package com.jwdroid.ui;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayAdapter;
import com.jwdroid.Util;

import java.util.ArrayList;

public class ReportList extends AppCompatActivity {

    private static final String TAG = "ReportList";

    static private final int DIALOG_DELETE = 1;

    private ReportListAdapter mListAdapter;
    private ListView mListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.report_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_reports);
        setSupportActionBar(toolbar);


        mListView = (ListView) findViewById(R.id.report_list);


        mListAdapter = new ReportListAdapter(this, null);

        mListView.setAdapter(mListAdapter);

        mListAdapter.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                findViewById(R.id.report_list_empty).setVisibility(mListAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                ListItem item = (ListItem) mListAdapter.getItem(position);

                if(item.type != ListItem.TYPE_ITEM) {
                    return;
                }

                Intent intent = new Intent(ReportList.this, Report.class);
                intent.putExtra("month", String.format("%04d%02d", ((MonthListItem)item).year, ((MonthListItem)item).month + 1));
                startActivityForResult(intent, 1);

            }
        });

        updateContent();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateContent();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void updateContent() {
        SQLiteDatabase db = AppDbOpenHelper.getInstance(ReportList.this).getReadableDatabase();
        Long firstDateVisits = Util.dbFetchLong(db, "SELECT MIN(strftime('%s',date)) FROM visit WHERE strftime('%Y',date,'localtime') > '2000'", new String[]{});
        Long firstDateSessions = Util.dbFetchLong(db, "SELECT MIN(strftime('%s',date)) FROM session WHERE strftime('%Y',date,'localtime') > '2000'", new String[]{});
        Time minDate = new Time();
        minDate.setToNow();
        if (firstDateVisits != null && firstDateVisits > 0)
            minDate.set(firstDateVisits * 1000);
        if (firstDateSessions != null && firstDateSessions > 0 && firstDateSessions * 1000 < minDate.toMillis(true)) {
            minDate.set(firstDateSessions * 1000);
        }

        Time now = new Time();
        now.setToNow();
        now.monthDay = 1;

        ArrayList<ListItem> items = new ArrayList<ListItem>();

        minDate.monthDay = 1;
        minDate.hour = 0;
        minDate.minute = 0;

        TotalListItem total = new TotalListItem();
        int startServiceYear = now.year;
        if(now.month < 8) {
            startServiceYear--;
        }
        total.year = startServiceYear+1;
        total.minutes = Util.dbFetchInt(db, "SELECT SUM(minutes) FROM session WHERE strftime('%Y%m',date,'localtime')>=?", new String[]{String.format("%04d09", startServiceYear)});
        items.add(total);

        while (now.toMillis(true) >= minDate.toMillis(true)) {
            MonthListItem i = new MonthListItem();
            i.month = now.month;
            i.year = now.year;
            i.minutes = Util.dbFetchInt(db, "SELECT SUM(minutes) FROM session WHERE strftime('%Y%m',date,'localtime')=?", new String[]{String.format("%04d%02d", now.year, now.month + 1)});
            items.add(i);

            if(now.month == 8) {
                total = new TotalListItem();
                total.year = now.year;
                total.minutes = Util.dbFetchInt(db, "SELECT SUM(minutes) FROM session WHERE strftime('%Y%m',date,'localtime')>=? AND strftime('%Y%m',date,'localtime')<=?", new String[]{String.format("%04d09", now.year-1), String.format("%04d08", now.year)});
                items.add(total);
            }

            now.month--;
            now.monthDay = 1;
            now.normalize(true);
        }

        mListAdapter.swapData(items);
    }

    private abstract class ListItem {
        static final int TYPE_TOTAL = 0;
        static final int TYPE_ITEM = 1;
        int type;
        abstract View getView(LayoutInflater inflater, View convertView, ViewGroup parent);

        class ViewHolder {
            TextView name, minutes;
        }
    }

    private class MonthListItem extends ListItem {
        Integer month, year, minutes;

        MonthListItem() {
            type = TYPE_ITEM;
        }

        @Override
        View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.report_list_item, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.report_item_name);
                holder.minutes = (TextView) convertView.findViewById(R.id.report_item_minutes);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(ReportList.this.getResources().getStringArray(R.array.months)[month] + " " + year);
            holder.minutes.setText(String.format("%d:%02d", minutes / 60, minutes % 60));

            return convertView;
        }
    }

    private class TotalListItem extends ListItem {
        Integer year, minutes;

        TotalListItem() {
            type = TYPE_TOTAL;
        }

        @Override
        View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.report_total_list_item, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.report_item_name);
                holder.minutes = (TextView) convertView.findViewById(R.id.report_item_minutes);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(String.format(ReportList.this.getString(R.string.lbl_service_year), year-1, year));
            holder.minutes.setText(String.format("%d:%02d", minutes / 60, minutes % 60));

            return convertView;
        }
    }


    private static class ReportListAdapter extends SimpleArrayAdapter<ListItem> {

        public ReportListAdapter(Context context, ArrayList<ListItem> items) {
            super(context, items);
        }

        public long getItemId(int position) {
            ListItem item = mItems.get(position);
            if(item.type == ListItem.TYPE_ITEM) {
                return ((MonthListItem)item).year*100 + ((MonthListItem)item).month;
            }
            else {
                return ((TotalListItem)item).year;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position).type;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return mItems.get(position).getView(mInflater, convertView, parent);
        }



    }


}
