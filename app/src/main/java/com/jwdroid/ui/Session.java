package com.jwdroid.ui;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.RepeatListener;

import java.sql.Date;
import java.util.HashMap;

public class Session extends AppCompatActivity {

    private static final int DIALOG_DATE = 1;
    private static final int DIALOG_TIME = 2;
    private static final int DIALOG_DELETE = 3;

    public static final int NEW_REPORT_YEAR = 2016;

    private static final String MINUTES = "minutes";
    private static final String BOOKS = "books";
    private static final String BROCHURES = "brochures";
    private static final String MAGAZINES = "magazines";
    private static final String RETURNS = "returns";
    private static final String TRACTS = "tracts";
    private static final String PUBLICATIONS = "publications";
    private static final String VIDEOS = "videos";
    private static final String[] ALL_DATA = new String[]{MINUTES, BOOKS, BROCHURES, MAGAZINES, RETURNS, TRACTS, PUBLICATIONS, VIDEOS};

    Long mSessionId;

    String mDesc;
    Time mDate;
    Boolean mShowHoldTip = false;
    HashMap<String, Integer> mValues = new HashMap<String, Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.session);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(String.format(getResources().getString(R.string.title_session), ""));
        setSupportActionBar(toolbar);

        mSessionId = getIntent().getExtras().getLong("session");

        SQLiteDatabase db = AppDbOpenHelper.getInstance(Session.this).getWritableDatabase();

        mDate = new Time();
        mDate.setToNow();

        if (mSessionId != 0) {

            Cursor rs = db.rawQuery("SELECT strftime('%s',date),desc,minutes,books,brochures,magazines,returns,tracts,publications,videos FROM session WHERE ROWID=?", new String[]{mSessionId.toString()});
            rs.moveToFirst();

            mDate.set(rs.getLong(0) * 1000);

            mDesc = rs.getString(1);
            mValues.put(MINUTES, rs.getInt(2));
            mValues.put(BOOKS, rs.getInt(3));
            mValues.put(BROCHURES, rs.getInt(4));
            mValues.put(MAGAZINES, rs.getInt(5));
            mValues.put(RETURNS, rs.getInt(6));
            mValues.put(TRACTS, rs.getInt(7));
            mValues.put(PUBLICATIONS, rs.getInt(8));
            mValues.put(VIDEOS, rs.getInt(9));

            rs.close();
        } else {
            for (String name : ALL_DATA) {
                mValues.put(name, 0);
            }
        }

        updateYear();


        if (savedInstanceState != null) {
            for (String name : ALL_DATA) {
                mValues.put(name, savedInstanceState.getInt(name));
            }
        }

        Date date = new Date(mDate.toMillis(true));
        //((TextView)findViewById(R.id.title)).setText( String.format(getResources().getString(R.string.title_session), DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(date)));


        ((Button) findViewById(R.id.btn_date)).setText(android.text.format.DateFormat.getDateFormat(this).format(date));
        ((Button) findViewById(R.id.btn_date)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_DATE);
            }
        });

        ((Button) findViewById(R.id.btn_time)).setText(android.text.format.DateFormat.getTimeFormat(this).format(date));
        ((Button) findViewById(R.id.btn_time)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_TIME);
            }
        });

        ((TextView) findViewById(R.id.edit_desc)).setText(mDesc);

        ((TextView) findViewById(R.id.lbl_timer_hour)).setText(String.format("%d", mValues.get(MINUTES) / 60));
        ((TextView) findViewById(R.id.lbl_timer_minute)).setText(String.format("%02d", mValues.get(MINUTES) % 60));

        ((TextView) findViewById(R.id.text_magazines)).setText(String.valueOf(mValues.get(MAGAZINES)));
        ((TextView) findViewById(R.id.text_brochures)).setText(String.valueOf(mValues.get(BROCHURES)));
        ((TextView) findViewById(R.id.text_books)).setText(String.valueOf(mValues.get(BOOKS)));
        ((TextView) findViewById(R.id.text_tracts)).setText(String.valueOf(mValues.get(TRACTS)));
        ((TextView) findViewById(R.id.text_returns)).setText(String.valueOf(mValues.get(RETURNS)));
        ((TextView) findViewById(R.id.text_publications)).setText(String.valueOf(mValues.get(PUBLICATIONS)));
        ((TextView) findViewById(R.id.text_videos)).setText(String.valueOf(mValues.get(VIDEOS)));

        ((EditText) findViewById(R.id.edit_desc)).addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mDesc = s.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("tip_hold_plus", false)) {
            mShowHoldTip = true;
        }


        linkCounterControls(BOOKS, R.id.btn_books_less, R.id.btn_books_more, R.id.text_books);
        linkCounterControls(BROCHURES, R.id.btn_brochures_less, R.id.btn_brochures_more, R.id.text_brochures);
        linkCounterControls(MAGAZINES, R.id.btn_magazines_less, R.id.btn_magazines_more, R.id.text_magazines);
        linkCounterControls(TRACTS, R.id.btn_tracts_less, R.id.btn_tracts_more, R.id.text_tracts);
        linkCounterControls(RETURNS, R.id.btn_returns_less, R.id.btn_returns_more, R.id.text_returns);
        linkCounterControls(PUBLICATIONS, R.id.btn_publications_less, R.id.btn_publications_more, R.id.text_publications);
        linkCounterControls(VIDEOS, R.id.btn_videos_less, R.id.btn_videos_more, R.id.text_videos);


        ((ImageButton) findViewById(R.id.btn_timer_less)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Integer value = mValues.get(MINUTES);
                if (value > 1) mValues.put(MINUTES, --value);
                ((TextView) findViewById(R.id.lbl_timer_hour)).setText(String.format("%d", value / 60));
                ((TextView) findViewById(R.id.lbl_timer_minute)).setText(String.format("%02d", value % 60));
            }
        }));

        ((ImageButton) findViewById(R.id.btn_timer_more)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Integer value = mValues.get(MINUTES);
                mValues.put(MINUTES, ++value);
                ((TextView) findViewById(R.id.lbl_timer_hour)).setText(String.format("%d", value / 60));
                ((TextView) findViewById(R.id.lbl_timer_minute)).setText(String.format("%02d", value % 60));

                if (mShowHoldTip) {
                    Editor editor = prefs.edit();
                    editor.putBoolean("tip_hold_plus", true);
                    editor.commit();
                    mShowHoldTip = false;

                    Toast.makeText(Session.this, R.string.msg_tip_hold_plus, Toast.LENGTH_LONG).show();
                }
            }
        }));




        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }


    private void linkCounterControls(final String name, final int idBtnLess, final int idBtnMore, final int idText) {
        ((ImageButton) findViewById(idBtnLess)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Integer value = mValues.get(name);
                if (value > 0) mValues.put(name, --value);
                ((TextView) findViewById(idText)).setText(String.valueOf(value));
            }
        }));
        ((ImageButton) findViewById(idBtnMore)).setOnTouchListener(new RepeatListener(300, 100, new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Integer value = mValues.get(name);
                mValues.put(name, ++value);
                ((TextView) findViewById(idText)).setText(String.valueOf(value));
            }
        }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session, menu);
        menu.findItem(R.id.menu_delete).setVisible(mSessionId != 0);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                showDialog(DIALOG_DELETE);
                break;
            case R.id.menu_ok:
                SQLiteDatabase db = AppDbOpenHelper.getInstance(Session.this).getWritableDatabase();

                if (mSessionId != 0) {
                    db.execSQL("UPDATE session SET date=?,desc=?,minutes=?,magazines=?,brochures=?,books=?,returns=?,tracts=?,publications=?,videos=? WHERE ROWID=?",
                            new Object[]{mDate.format3339(false), mDesc, mValues.get(MINUTES), mValues.get(MAGAZINES), mValues.get(BROCHURES), mValues.get(BOOKS), mValues.get(RETURNS), mValues.get(TRACTS), mValues.get(PUBLICATIONS), mValues.get(VIDEOS), mSessionId});

                    Intent intent = new Intent(Session.this, Report.class);
                    intent.putExtra("month", mDate.format("%Y%m"));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    db.execSQL("INSERT INTO session (date,desc,minutes,magazines,books,brochures,returns,tracts,publications,videos) VALUES(?,?,?,?,?,?,?,?,?,?)",
                            new Object[]{mDate.format3339(false), mDesc, mValues.get(MINUTES), mValues.get(MAGAZINES), mValues.get(BOOKS), mValues.get(BROCHURES), mValues.get(RETURNS), mValues.get(TRACTS), mValues.get(PUBLICATIONS), mValues.get(VIDEOS)});
                    setResult(1);
                    finish();
                }
                break;
            case android.R.id.home:
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.putExtra("month", mDate.format("%Y%m"));
                NavUtils.navigateUpTo(this, intent);
                return true;
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_TIME:

                return new TimePickerDialog(this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                mDate.set(0, minute, hourOfDay, mDate.monthDay, mDate.month, mDate.year);
                                mDate.normalize(true);
                                Date date = new Date(mDate.toMillis(true));
                                //((TextView)findViewById(R.id.title)).setText( String.format(getResources().getString(R.string.title_session), DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(date)));
                                ((Button) findViewById(R.id.btn_time)).setText(android.text.format.DateFormat.getTimeFormat(Session.this).format(new Date(mDate.toMillis(true))));
                            }
                        },
                        mDate.hour, mDate.minute, android.text.format.DateFormat.is24HourFormat(this));
            case DIALOG_DATE:
                return new DatePickerDialog(this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                mDate.set(0, mDate.minute, mDate.hour, dayOfMonth, monthOfYear, year);
                                mDate.normalize(true);
                                Date date = new Date(mDate.toMillis(true));
                                //((TextView)findViewById(R.id.title)).setText( String.format(getResources().getString(R.string.title_session), DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(date)));
                                ((Button) findViewById(R.id.btn_date)).setText(android.text.format.DateFormat.getDateFormat(Session.this).format(new Date(mDate.toMillis(true))));
                                updateYear();
                            }
                        },
                        mDate.year, mDate.month, mDate.monthDay);
            case DIALOG_DELETE:
                return new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_delete_session)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create();
        }
        return null;
    }


    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_TIME:
                ((TimePickerDialog) dialog).updateTime(mDate.hour, mDate.minute);
                break;
            case DIALOG_DATE:
                ((DatePickerDialog) dialog).updateDate(mDate.year, mDate.month, mDate.monthDay);
                break;
            case DIALOG_DELETE: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = AppDbOpenHelper.getInstance(Session.this).getWritableDatabase();
                        db.execSQL("DELETE FROM `session` WHERE rowid=?", new Long[]{mSessionId});
                        Toast.makeText(Session.this, R.string.msg_session_deleted, Toast.LENGTH_SHORT).show();
                        setResult(1);
                        finish();
                    }
                });
                break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        for (String name : ALL_DATA) {
            outState.putInt(name, mValues.get(name));
        }

        super.onSaveInstanceState(outState);
    }

    private void updateYear() {
        findViewById(R.id.row_books).setVisibility(mDate.year < NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_brochures).setVisibility(mDate.year < NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_magazines).setVisibility(mDate.year < NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_tracts).setVisibility(mDate.year < NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_publications).setVisibility(mDate.year >= NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_videos).setVisibility(mDate.year >= NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
    }




}
