package com.jwdroid.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.AsyncLoader;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayAdapter;
import com.jwdroid.Util;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Report extends AppCompatActivity implements LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    static private final int DIALOG_DELETE = 1;

    private SessionListAdapter mListAdapter;
    private ListView mListView;

    private String mMonth;

    private Long mDialogItemId;

    private QuickAction mSendTypeActions;

    Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.report);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mMonth = getIntent().getExtras().getString("month");

        final String monthName = getResources().getStringArray(R.array.months)[Integer.parseInt(mMonth.substring(4, 6)) - 1] + " " + mMonth.substring(0, 4);

        mToolbar.setTitle(monthName);


        // Set up territory list

        mListView = (ListView) findViewById(R.id.session_list);


        mListAdapter = new SessionListAdapter(this, new ArrayList<SessionItem>());
        mListAdapter.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                findViewById(R.id.session_list_empty).setVisibility(mListAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                SessionItem item = mListAdapter.getItem(position);

                Intent intent = new Intent(Report.this, Session.class);
                intent.putExtra("session", item.id);
                startActivityForResult(intent, 1);

            }
        });

        final QuickAction listActions = new QuickAction(this);
        listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_session_delete), getResources().getDrawable(R.drawable.ac_trash)));
        listActions.animateTrack(false);
        listActions.setAnimStyle(QuickAction.ANIM_MOVE_FROM_RIGHT);

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int pos, long id) {
                listActions.show(v, id);
                return true;
            }
        });

        listActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                Bundle args;
                SQLiteDatabase db;
                Cursor rs;
                switch (pos) {
                    case 0:    // Название
                        mDialogItemId = listActions.getId();
                        showDialog(DIALOG_DELETE);
                }
            }
        });


        mSendTypeActions = new QuickAction(this);
        mSendTypeActions.addActionItem(new ActionItem("SMS", getResources().getDrawable(R.drawable.ac_spechbubble_sq_line)));
        mSendTypeActions.addActionItem(new ActionItem("E-mail", getResources().getDrawable(R.drawable.ac_mail)));
        mSendTypeActions.animateTrack(false);

        mSendTypeActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                Bundle args;
                SQLiteDatabase db;
                Cursor rs;
                SummaryInfo data = getSummaryInfo();
                String text = getResources().getString(R.string.lbl_books) + " " + data.books + "\n" +
                        getResources().getString(R.string.lbl_brochures_tracts) + " " + (data.brochures + data.tracts) + "\n" +
                        getResources().getString(R.string.lbl_hours) + " " + (data.minutes / 60) + "\n" +
                        getResources().getString(R.string.lbl_magazines) + " " + data.magazines + "\n" +
                        getResources().getString(R.string.lbl_returns) + " " + data.returns + "\n" +
                        getResources().getString(R.string.lbl_studies) + " " + data.studies;

                if(data.publications > 0) {
                    text = getResources().getString(R.string.lbl_placements) + " " + data.publications + "\n" +
                            getResources().getString(R.string.lbl_videos) + " " + data.videos + "\n" +
                            getResources().getString(R.string.lbl_hours) + " " + (data.minutes / 60) + "\n" +
                            getResources().getString(R.string.lbl_returns) + " " + data.returns + "\n" +
                            getResources().getString(R.string.lbl_studies) + " " + data.studies;
                }

                switch (pos) {
                    case 0:    // SMS
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel"));
                        intent.setType("vnd.android-dir/mms-sms");
                        intent.putExtra("sms_body", String.format(getResources().getString(R.string.title_report), monthName) + "\n\n" + text);
                        startActivity(intent);
                        break;
                    case 1: // E-mail
                        intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_SUBJECT, String.format(getResources().getString(R.string.title_report), monthName));
                        intent.putExtra(Intent.EXTRA_TEXT, text);
                        startActivity(Intent.createChooser(intent, null));
                        break;
                }
            }
        });




        final QuickAction summaryTypeActions = new QuickAction(this);
        summaryTypeActions.addActionItem(new ActionItem(getResources().getString(R.string.action_calc_type_sessions), getResources().getDrawable(R.drawable.ac_list_bullets)));
        summaryTypeActions.addActionItem(new ActionItem(getResources().getString(R.string.action_calc_type_visits), getResources().getDrawable(R.drawable.ac_users)));
        summaryTypeActions.animateTrack(false);

        summaryTypeActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                Bundle args;
                SQLiteDatabase db;
                Cursor rs;
                SharedPreferences.Editor editor;
                switch (pos) {
                    case 0:    // По списку служений
                        editor = prefs.edit();
                        editor.putString("report_calc_type", "1");
                        editor.commit();
                        break;
                    case 1:
                        editor = prefs.edit();
                        editor.putString("report_calc_type", "2");
                        editor.commit();
                        break;
                }
            }
        });

        findViewById(R.id.btn_summary_type).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                summaryTypeActions.show(v);
            }
        });

        calcSummary();

        getSupportLoaderManager().restartLoader(0, null, this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_send:
                mSendTypeActions.show(mToolbar);
                break;

            case R.id.menu_add:
                Intent intent = new Intent(Report.this, Session.class);
                intent.putExtra("session", 0);
                startActivityForResult(intent, 1);
                break;
        }

        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        SessionListLoader loader = new SessionListLoader(this, mMonth);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ArrayList<SessionItem> data = new ArrayList<SessionItem>();
        while (cursor.moveToNext()) {
            SessionItem item = new SessionItem();
            item.id = cursor.getLong(0);
            item.date = new Time();
            item.date.set(cursor.getLong(1) * 1000);
            item.minutes = cursor.getInt(2);
            item.books = cursor.getInt(3);
            item.brochures = cursor.getInt(4);
            item.magazines = cursor.getInt(5);
            item.tracts = cursor.getInt(6);
            item.returns = cursor.getInt(7);
            item.desc = cursor.getString(8);
            item.publications = cursor.getInt(9);
            item.videos = cursor.getInt(10);

            data.add(item);
        }
        mListAdapter.swapData(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mListAdapter.swapData(new ArrayList<SessionItem>());
    }

    @Override
    protected void onActivityResult(int arg0, int arg1, Intent arg2) {
        getSupportLoaderManager().getLoader(0).forceLoad();
        calcSummary();
    }



    private class SummaryInfo {
        int magazines, brochures, books, tracts, returns, studies, minutes, publications, videos;
    }

    private SummaryInfo getSummaryInfo() {

        SummaryInfo data = new SummaryInfo();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int calcType = Integer.parseInt(prefs.getString("report_calc_type", "1"));

        SQLiteDatabase db = AppDbOpenHelper.getInstance(Report.this).getReadableDatabase();

        if (calcType == 1) { // По хронометру
            Cursor rs = db.rawQuery("SELECT SUM(magazines),SUM(brochures),SUM(books),SUM(returns),SUM(tracts),SUM(publications),SUM(videos) FROM session WHERE strftime('%Y%m',date,'localtime')=?", new String[]{mMonth});
            rs.moveToFirst();
            data.magazines = rs.getInt(0);
            data.brochures = rs.getInt(1);
            data.books = rs.getInt(2);
            data.returns = rs.getInt(3);
            data.tracts = rs.getInt(4);
            data.publications = rs.getInt(5);
            data.videos = rs.getInt(6);
            rs.close();
        } else {              // По посещениям
            Cursor rs = db.rawQuery("SELECT SUM(magazines),SUM(brochures),SUM(books),SUM(tracts),SUM(publications),SUM(videos) FROM visit WHERE strftime('%Y%m',date,'localtime')=?", new String[]{mMonth});
            rs.moveToFirst();
            data.magazines = rs.getInt(0);
            data.brochures = rs.getInt(1);
            data.books = rs.getInt(2);
            data.tracts = rs.getInt(3);
            data.publications = rs.getInt(4);
            data.videos = rs.getInt(5);
            rs.close();
            data.returns = Util.dbFetchInt(db, "SELECT COUNT(*) FROM visit WHERE type>? AND strftime('%Y%m',date,'localtime')=?", new String[]{String.valueOf(Visit.TYPE_FIRST_VISIT), mMonth});
        }
        data.studies = Util.dbFetchInt(db, "SELECT COUNT(DISTINCT person_id) FROM visit WHERE type=? AND strftime('%Y%m',date,'localtime')=?", new String[]{String.valueOf(Visit.TYPE_STUDY), mMonth});
        data.minutes = Util.dbFetchInt(db, "SELECT SUM(minutes) FROM session WHERE strftime('%Y%m',date,'localtime')=?", new String[]{mMonth});

        return data;
    }

    private void calcSummary() {
        SummaryInfo data = getSummaryInfo();

        String info = "";
        if (data.magazines > 0)
            info += data.magazines + " " + Util.pluralForm(this, data.magazines, getResources().getStringArray(R.array.plural_magazines));
        if (data.brochures > 0 || data.tracts > 0) {
            if (info.length() > 0)
                info += ", ";
            info += (data.brochures + data.tracts) + " " + Util.pluralForm(this, data.brochures + data.tracts, getResources().getStringArray(R.array.plural_brochures_tracts));
        }
        if (data.books > 0) {
            if (info.length() > 0)
                info += ", ";
            info += data.books + " " + Util.pluralForm(this, data.books, getResources().getStringArray(R.array.plural_books));
        }
        if (data.publications > 0) {
            if (info.length() > 0)
                info += ", ";
            info += data.publications + " " + Util.pluralForm(this, data.publications, getResources().getStringArray(R.array.plural_placements));
        }
        if (data.videos > 0) {
            if (info.length() > 0)
                info += ", ";
            info += data.videos + " " + Util.pluralForm(this, data.videos, getResources().getStringArray(R.array.plural_videos));
        }
        if (data.returns > 0) {
            if (info.length() > 0)
                info += ", ";
            info += data.returns + " " + Util.pluralForm(this, data.returns, getResources().getStringArray(R.array.plural_returns));
        }
        if (data.studies > 0) {
            if (info.length() > 0)
                info += ", ";
            info += data.studies + " " + Util.pluralForm(this, data.studies, getResources().getStringArray(R.array.plural_studies));
        }
        if (info.length() == 0)
            info = getResources().getString(R.string.lbl_report_no_data);
        ((TextView) findViewById(R.id.summary_info)).setText(info);

        ((TextView) findViewById(R.id.summary_minutes)).setText(String.format("%d:%02d", data.minutes / 60, data.minutes % 60));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
        if (key.equals("report_calc_type")) {
            calcSummary();
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        LayoutInflater factory = LayoutInflater.from(this);
        final View dlgEditLayout = factory.inflate(R.layout.dlg_edit, null);

        switch (id) {

            case DIALOG_DELETE:
                dialog = new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_delete_session)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create();
                break;
        }

        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_DELETE: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = AppDbOpenHelper.getInstance(Report.this).getWritableDatabase();
                        db.execSQL("DELETE FROM `session` WHERE rowid=?", new Long[]{mDialogItemId});
                        Toast.makeText(Report.this, R.string.msg_session_deleted, Toast.LENGTH_SHORT).show();
                        getSupportLoaderManager().getLoader(0).forceLoad();
                        calcSummary();
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


    private class SessionItem {
        Long id;
        Integer minutes, books, brochures, tracts, magazines, returns, publications, videos;
        String desc;
        Time date;
    }


    static public class SessionListLoader extends AsyncLoader<Cursor> {

        private String mMonth;

        public SessionListLoader(Context context, String month) {
            super(context);
            mMonth = month;
        }

        @Override
        public Cursor loadInBackground() {
            SQLiteDatabase db = AppDbOpenHelper.getInstance(getContext()).getWritableDatabase();
            Cursor rs = db.rawQuery("SELECT ROWID,strftime('%s',date),minutes,books,brochures,magazines,tracts,returns,desc,publications,videos FROM session WHERE strftime('%Y%m',date,'localtime')=? ORDER BY date DESC", new String[]{mMonth});
            return rs;
        }
    }


    private static class SessionListAdapter extends SimpleArrayAdapter<SessionItem> {
        SimpleDateFormat mDayFormat;

        public SessionListAdapter(Context context, ArrayList<SessionItem> items) {
            super(context, items);
            mDayFormat = new SimpleDateFormat("EEEE");
        }

        public long getItemId(int position) {
            return mItems.get(position).id;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            SessionItem item = mItems.get(position);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.session_list_item, null);

                holder = new ViewHolder();
                holder.date = (TextView) convertView.findViewById(R.id.session_item_date);
                holder.desc = (TextView) convertView.findViewById(R.id.session_item_desc);
                holder.minutes = (TextView) convertView.findViewById(R.id.session_item_minutes);
                holder.info = (TextView) convertView.findViewById(R.id.session_item_info);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }
            Date date = new Date(item.date.toMillis(true));
            holder.date.setText(Html.fromHtml("<b>"+android.text.format.DateFormat.getDateFormat(mContext).format(date) + "</b>, " + mDayFormat.format(date) + ", " + android.text.format.DateFormat.getTimeFormat(mContext).format(date)));

            if (item.desc != null && item.desc.length() > 0) {
                holder.desc.setVisibility(View.VISIBLE);
                holder.desc.setText(item.desc);
            } else
                holder.desc.setVisibility(View.GONE);

            holder.minutes.setText(String.format("%d:%02d", item.minutes / 60, item.minutes % 60));

            String info = "";
            if (item.magazines > 0)
                info += item.magazines + " " + Util.pluralForm(mContext, item.magazines, mContext.getResources().getStringArray(R.array.plural_magazines));
            if (item.brochures > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.brochures + " " + Util.pluralForm(mContext, item.brochures, mContext.getResources().getStringArray(R.array.plural_brochures));
            }
            if (item.books > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.books + " " + Util.pluralForm(mContext, item.books, mContext.getResources().getStringArray(R.array.plural_books));
            }
            if (item.tracts > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.tracts + " " + Util.pluralForm(mContext, item.tracts, mContext.getResources().getStringArray(R.array.plural_tracts));
            }
            if (item.publications > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.publications + " " + Util.pluralForm(mContext, item.publications, mContext.getResources().getStringArray(R.array.plural_placements));
            }
            if (item.videos > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.videos + " " + Util.pluralForm(mContext, item.videos, mContext.getResources().getStringArray(R.array.plural_videos));
            }
            if (item.returns > 0) {
                if (info.length() > 0)
                    info += ", ";
                info += item.returns + " " + Util.pluralForm(mContext, item.returns, mContext.getResources().getStringArray(R.array.plural_returns));
            }
            if (info.length() > 0) {
                holder.info.setVisibility(View.VISIBLE);
                holder.info.setText(info);
            } else
                holder.info.setVisibility(View.GONE);

            return convertView;
        }

        static class ViewHolder {
            TextView date, desc, minutes, info;
        }

    }




}
