package com.jwdroid.ui;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.ChronoService;
import com.jwdroid.DropboxConfig;
import com.jwdroid.R;
import com.jwdroid.RepeatListener;
import com.jwdroid.Util;
import com.jwdroid.export.DropboxBackuper;
import com.jwdroid.export.LocalBackuper;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Chrono extends AppCompatActivity {

    private static final int DIALOG_FINISH = 1;
    private static final int DIALOG_START = 2;

    private static final String PUBLICATIONS = "publications";
    private static final String VIDEOS = "videos";
    private static final String RETURNS = "returns";

    private ChronoService mService;
    private boolean mBound;

    private Timer mTimer = null;
    private boolean mShowColon = false;

    private Long mDialogItemId;

    private Boolean mShowHoldTip = false;

    private HashMap<String, Integer> mVisitValues = new HashMap<String, Integer>();

    private ServiceConnection mConnection = new ServiceConnection() {


        public void onServiceConnected(ComponentName className, IBinder service) {

            final ChronoService.LocalBinder binder = (ChronoService.LocalBinder) service;
            mService = binder.getService();


            recalcVisits();
            initUI();

            updateUI();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            initUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.chrono);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_chrono);
        setSupportActionBar(toolbar);

        initUI();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = new Intent(Chrono.this, ChronoService.class);
        if (prefs.getLong("chronoStartTime", -1) != -1)
            startService(intent);
        bindService(intent, mConnection, 0);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (mService != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                }
            }
        }, 0, 500);

        if (!prefs.getBoolean("tip_hold_plus", false)) {
            mShowHoldTip = true;
        }


        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_START);
            }
        });

        findViewById(R.id.btn_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_FINISH);
            }
        });

        findViewById(R.id.btn_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService == null)
                    return;
                mService.setPaused(true);
                v.setVisibility(View.GONE);
                findViewById(R.id.btn_resume).setVisibility(View.VISIBLE);
                updateUI();
            }
        });

        findViewById(R.id.btn_resume).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService == null)
                    return;
                mService.setPaused(false);
                v.setVisibility(View.GONE);
                findViewById(R.id.btn_pause).setVisibility(View.VISIBLE);
                updateUI();
            }
        });

        ((CheckBox) findViewById(R.id.chk_calc_auto)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mService == null)
                    return;
                prefs.edit().putBoolean("chronoCalcAuto", isChecked).commit();
                recalcVisits();
                initUI();
            }
        });

        linkCounterControls(prefs, "chronoPublications", R.id.btn_publications_less, R.id.btn_publications_more, R.id.text_publications);
        linkCounterControls(prefs, "chronoVideos", R.id.btn_videos_less, R.id.btn_videos_more, R.id.text_videos);
        linkCounterControls(prefs, "chronoReturns", R.id.btn_returns_less, R.id.btn_returns_more, R.id.text_returns);


        ((ImageButton) findViewById(R.id.btn_timer_less)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mService == null) return;
                int minutes = prefs.getInt("chronoMinutes", 0);
                if (ChronoService.getCurrentMinutes(Chrono.this) > 1)
                    mService.setMinutes(minutes - 1);
                updateUI();
            }
        }));

        ((ImageButton) findViewById(R.id.btn_timer_more)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mService == null) return;
                mService.setMinutes(prefs.getInt("chronoMinutes", 0) + 1);
                updateUI();

                if (mShowHoldTip) {
                    Editor editor = prefs.edit();
                    editor.putBoolean("tip_hold_plus", true);
                    editor.commit();
                    mShowHoldTip = false;

                    Toast.makeText(Chrono.this, R.string.msg_tip_hold_plus, Toast.LENGTH_LONG).show();
                }
            }
        }));

        ((CheckBox) findViewById(R.id.chk_calc_auto)).setChecked(prefs.getBoolean("chronoCalcAuto", true));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void linkCounterControls(final SharedPreferences prefs, final String name, final int idBtnLess, final int idBtnMore, final int idText) {
        ((ImageButton) findViewById(idBtnLess)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mService == null) return;
                prefs.edit().putInt(name, Math.max(prefs.getInt(name, 0) - 1, 0)).commit();
                ((TextView) findViewById(idText)).setText(String.valueOf(prefs.getInt(name, 0)));
            }
        }));
        ((ImageButton) findViewById(idBtnMore)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (mService == null) return;
                prefs.edit().putInt(name, prefs.getInt(name, 0) + 1).commit();
                ((TextView) findViewById(idText)).setText(String.valueOf(prefs.getInt(name, 0)));
            }
        }));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Chrono.this);
        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {
            case DIALOG_FINISH:
                dialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.msg_chrono_finish)
                        .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mService != null) {

                                    recalcVisits();

                                    Time beginTime = new Time();
                                    beginTime.set(prefs.getLong("chronoBeginTime", 0));

                                    SQLiteDatabase db = AppDbOpenHelper.getInstance(Chrono.this).getWritableDatabase();
                                    db.execSQL("INSERT INTO session (date, minutes,publications,videos,returns,books,brochures,magazines,tracts) VALUES(?,?,?,?,?,0,0,0,0)",
                                            new Object[]{
                                                    beginTime.format3339(false),
                                                    ChronoService.getCurrentMinutes(Chrono.this),
                                                    prefs.getInt("chronoPublications", 0) + mVisitValues.get(PUBLICATIONS),
                                                    prefs.getInt("chronoVideos", 0) + mVisitValues.get(VIDEOS),
                                                    prefs.getInt("chronoReturns", 0) + mVisitValues.get(RETURNS)});

                                    long sessionId = Util.dbFetchLong(db, "SELECT last_insert_rowid()", new String[]{});

                                    mService.stop();

                                    prefs.edit()
                                            .remove("chronoStartTime")
                                            .remove("chronoBeginTime")
                                            .remove("chronoMinutes")
                                            .remove("chronoPublications")
                                            .remove("chronoVideos")
                                            .remove("chronoReturns")
                                            .putBoolean("serviceJustEnded", true)
                                            .commit();

                                    if (prefs.getBoolean("autobackup", true)) {

                                        if (DropboxConfig.getAccountManager(Chrono.this).hasLinkedAccount())
                                            new DropboxBackuper(Chrono.this, null).run();
                                        else
                                            new LocalBackuper(Chrono.this, null).run();
                                    }

                                    Intent intent = new Intent(Chrono.this, Session.class);
                                    intent.putExtra("session", sessionId);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        })
                        .setNegativeButton(R.string.btn_no, null).create();
                break;

            case DIALOG_START:
                dialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.msg_chrono_start)
                        .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                prefs.edit()
                                        .remove("chronoStartTime")
                                        .putLong("chronoBeginTime", System.currentTimeMillis())
                                        .putInt("chronoPublications", 0)
                                        .putInt("chronoVideos", 0)
                                        .putInt("chronoReturns", 0)
                                        .commit();

                                Intent intent = new Intent(Chrono.this, ChronoService.class);
                                startService(intent);
                                bindService(new Intent(Chrono.this, ChronoService.class), mConnection, 0);
                            }
                        })
                        .setNegativeButton(R.string.btn_no, null).create();
                break;
        }


        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }

    private void recalcVisits() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mVisitValues.put(PUBLICATIONS, 0);
        mVisitValues.put(VIDEOS, 0);
        mVisitValues.put(RETURNS, 0);

        if (mService == null || !prefs.getBoolean("chronoCalcAuto", true))
            return;

        Time now = new Time();
        now.setToNow();

        SQLiteDatabase db = AppDbOpenHelper.getInstance(Chrono.this).getReadableDatabase();

        Cursor rs = db.rawQuery("SELECT SUM(publications),SUM(videos) FROM visit WHERE strftime('%s',date) >= ? AND strftime('%s',date) <= ?", new String[]{String.valueOf(prefs.getLong("chronoBeginTime", 0)), String.valueOf(now.toMillis(true))});
        rs.moveToFirst();
        mVisitValues.put(PUBLICATIONS, rs.getInt(0));
        mVisitValues.put(VIDEOS, rs.getInt(1));
        rs.close();

        rs = db.rawQuery("SELECT COUNT(*) FROM visit WHERE type > 1 AND strftime('%s',date) >= ? AND strftime('%s',date) <= ?", new String[]{String.valueOf(prefs.getLong("chronoBeginTime", 0)), String.valueOf(now.toMillis(true))});
        rs.moveToFirst();
        mVisitValues.put(RETURNS, rs.getInt(0));
        rs.close();

        ((TextView) findViewById(R.id.text_visit_publications)).setText("+" + mVisitValues.get(PUBLICATIONS));
        ((TextView) findViewById(R.id.text_visit_videos)).setText("+" + mVisitValues.get(VIDEOS));
        ((TextView) findViewById(R.id.text_visit_returns)).setText("+" + mVisitValues.get(RETURNS));

    }

    private void updateUI() {
        if (mService == null)
            return;

        int minutes = ChronoService.getCurrentMinutes(this);

        mShowColon = !mShowColon;
        if (mService.getPaused())
            mShowColon = true;
        findViewById(R.id.lbl_timer_colon).setVisibility(mShowColon ? View.VISIBLE : View.INVISIBLE);
        ((TextView) findViewById(R.id.lbl_timer_hour)).setText(String.format("%d", minutes / 60));
        ((TextView) findViewById(R.id.lbl_timer_minute)).setText(String.format("%02d", minutes % 60));
    }

    private void initUI() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Chrono.this);

        findViewById(R.id.lbl_timer_colon).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_start).setVisibility(mService == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_pause).setVisibility(mService == null || mService.getPaused() ? View.GONE : View.VISIBLE);
        findViewById(R.id.btn_resume).setVisibility(mService == null || !mService.getPaused() ? View.GONE : View.VISIBLE);
        findViewById(R.id.btn_finish).setVisibility(mService == null ? View.GONE : View.VISIBLE);

        //findViewById(R.id.lbl_started_time).setVisibility(mService == null ? View.INVISIBLE : View.VISIBLE);
        if (mService == null)
            ((TextView) findViewById(R.id.lbl_started_time)).setText(R.string.lbl_chrono_stopped);
        else
            ((TextView) findViewById(R.id.lbl_started_time)).setText(String.format(getResources().getString(R.string.lbl_chrono_started_time), android.text.format.DateFormat.getTimeFormat(this).format(new Date(prefs.getLong("chronoBeginTime", 0)))));

        boolean calcAuto = prefs.getBoolean("chronoCalcAuto", true);

        findViewById(R.id.text_visit_publications).setVisibility(mService != null && calcAuto ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.text_visit_videos).setVisibility(mService != null && calcAuto ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.text_visit_returns).setVisibility(mService != null && calcAuto ? View.VISIBLE : View.INVISIBLE);

        int[] buttons = {R.id.btn_timer_less, R.id.btn_timer_more, R.id.btn_publications_less,
                R.id.btn_publications_more, R.id.btn_videos_less, R.id.btn_videos_more,
                R.id.btn_returns_less, R.id.btn_returns_more, R.id.chk_calc_auto};
        for (int id : buttons) {
            findViewById(id).setEnabled(mService != null);
            findViewById(id).setClickable(mService != null);
        }

        ((TextView) findViewById(R.id.text_publications)).setText(String.valueOf(
                prefs.getInt("chronoPublications", 0)));

        ((TextView) findViewById(R.id.text_videos)).setText(String.valueOf(
                prefs.getInt("chronoVideos", 0)));

        ((TextView) findViewById(R.id.text_returns)).setText(String.valueOf(
                prefs.getInt("chronoReturns", 0)));
    }
}
