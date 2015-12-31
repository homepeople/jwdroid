package com.jwdroid.ui;

import android.support.v7.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.RepeatListener;
import com.jwdroid.Util;

import java.util.Date;
import java.util.HashMap;

public class Visit extends AppCompatActivity {

    private static final String TAG = "Visit";

    public static final int TYPE_NA = 0;
    public static final int TYPE_FIRST_VISIT = 1;
    public static final int TYPE_RETURN_VISIT = 2;
    public static final int TYPE_STUDY = 3;

    static final int TIME_DIALOG_ID = 0;
    static final int DATE_DIALOG_ID = 1;
    static final int DIALOG_DELETE = 3;

    private static final String BOOKS = "books";
    private static final String BROCHURES = "brochures";
    private static final String MAGAZINES = "magazines";
    private static final String TRACTS = "tracts";
    private static final String PUBLICATIONS = "publications";
    private static final String VIDEOS = "videos";
    private static final String TYPE = "type";

    private static final String[] COUNTERS = {BOOKS, BROCHURES, MAGAZINES, TRACTS, PUBLICATIONS, VIDEOS};

    private static final int[] COUNTER_BUTTONS = {
            R.id.btn_visit_books_less,
            R.id.btn_visit_books_more,
            R.id.btn_visit_brochures_less,
            R.id.btn_visit_brochures_more,
            R.id.btn_visit_magazines_less,
            R.id.btn_visit_magazines_more,
            R.id.btn_visit_publications_less,
            R.id.btn_visit_publications_more,
            R.id.btn_visit_tracts_less,
            R.id.btn_visit_tracts_more,
            R.id.btn_visit_videos_less,
            R.id.btn_visit_videos_more};

    public static final int[] TYPE_ICONS = {R.drawable.visit_na, R.drawable.first_visit, R.drawable.revisit, R.drawable.study};

    private Long mTerritoryId, mDoorId, mPersonId, mVisitId;

    private String mDesc = "";
    private Integer mType = TYPE_FIRST_VISIT;
    private Time mDate = new Time();

    private HashMap<String, Integer> mValues = new HashMap<String, Integer>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.visit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        Cursor rs;
        SQLiteDatabase db = AppDbOpenHelper.getInstance(Visit.this).getWritableDatabase();

        mDoorId = getIntent().getExtras().getLong("door");
        mPersonId = getIntent().getExtras().getLong("person");


        rs = db.rawQuery("SELECT name,territory_id FROM door WHERE ROWID=?", new String[]{mDoorId.toString()});
        rs.moveToFirst();
        String doorName = rs.getString(0);
        mTerritoryId = rs.getLong(1);
        rs.close();


        rs = db.rawQuery("SELECT name FROM person WHERE ROWID=?", new String[]{mPersonId.toString()});
        rs.moveToFirst();
        toolbar.setTitle(rs.getString(0));
        rs.close();


        if (mTerritoryId != 0) {
            rs = db.rawQuery("SELECT name FROM territory WHERE ROWID=?", new String[]{mTerritoryId.toString()});
            rs.moveToFirst();
            toolbar.setSubtitle(doorName + "  •  " + rs.getString(0));
            rs.close();
        }
        else {
            toolbar.setSubtitle(R.string.title_people);
        }

        mDate.setToNow();

        mVisitId = getIntent().getExtras().getLong("visit");
        if (mVisitId != 0) {
            rs = db.rawQuery("SELECT calc_auto,desc,type,strftime('%s',date),magazines,brochures,books,tracts,publications,videos FROM visit WHERE ROWID=?", new String[]{mVisitId.toString()});
            rs.moveToFirst();

            mDesc = rs.getString(1);
            mType = rs.getInt(2);
            mDate.set(rs.getLong(3) * 1000);

            mValues.put(MAGAZINES, rs.getInt(4));
            mValues.put(BROCHURES, rs.getInt(5));
            mValues.put(BOOKS, rs.getInt(6));
            mValues.put(TRACTS, rs.getInt(7));
            mValues.put(PUBLICATIONS, rs.getInt(8));
            mValues.put(VIDEOS, rs.getInt(9));

            rs.close();
        } else {
            int visitsCnt = Util.dbFetchInt(db, "SELECT COUNT(*) FROM visit WHERE door_id=? AND person_id=? AND type!=?", new String[]{mDoorId.toString(), mPersonId.toString(), String.valueOf(TYPE_NA)});
            if (visitsCnt > 0) {
                int studiesCnt = Util.dbFetchInt(db, "SELECT COUNT(*) FROM visit WHERE door_id=? AND person_id=? AND type=?", new String[]{mDoorId.toString(), mPersonId.toString(), String.valueOf(TYPE_STUDY)});
                if (studiesCnt > 0)
                    mType = TYPE_STUDY;
                else
                    mType = TYPE_RETURN_VISIT;
            }

            for (String name : COUNTERS) {
                mValues.put(name, 0);
            }
        }

        updateYear();

        if (savedInstanceState != null) {
            for (String name : COUNTERS) {
                mValues.put(name, savedInstanceState.getInt(name));
            }
            mType = savedInstanceState.getInt(TYPE);
        }

        ((EditText) findViewById(R.id.edit_visit_desc)).setText(mDesc);
        ((EditText) findViewById(R.id.edit_visit_desc)).addTextChangedListener(new TextWatcher() {

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

        ((Spinner) findViewById(R.id.list_visit_type)).setAdapter(new VisitTypeAdapter(this));

        ((Spinner) findViewById(R.id.list_visit_type)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View arg1,
                                       int position, long arg3) {
                mType = (Integer) adapterView.getItemAtPosition(position);
                findViewById(R.id.visit_desc_block).setVisibility(mType == TYPE_NA ? View.GONE : View.VISIBLE);
                if (mType == TYPE_NA) {
                    ((EditText) findViewById(R.id.edit_visit_desc)).setText("");
                    for (String name : COUNTERS) {
                        mValues.put(name, 0);
                    }
                }

                updateYear();

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }

        });
        ((Spinner) findViewById(R.id.list_visit_type)).setSelection(mType);

        linkCounterControls(BOOKS, R.id.btn_visit_books_less, R.id.btn_visit_books_more, R.id.text_visit_books);
        linkCounterControls(BROCHURES, R.id.btn_visit_brochures_less, R.id.btn_visit_brochures_more, R.id.text_visit_brochures);
        linkCounterControls(MAGAZINES, R.id.btn_visit_magazines_less, R.id.btn_visit_magazines_more, R.id.text_visit_magazines);
        linkCounterControls(TRACTS, R.id.btn_visit_tracts_less, R.id.btn_visit_tracts_more, R.id.text_visit_tracts);
        linkCounterControls(PUBLICATIONS, R.id.btn_visit_publications_less, R.id.btn_visit_publications_more, R.id.text_visit_publications);
        linkCounterControls(VIDEOS, R.id.btn_visit_videos_less, R.id.btn_visit_videos_more, R.id.text_visit_videos);

        ((Button) findViewById(R.id.btn_visit_date)).setText(android.text.format.DateFormat.getDateFormat(this).format(new Date(mDate.toMillis(true))));
        ((Button) findViewById(R.id.btn_visit_time)).setText(android.text.format.DateFormat.getTimeFormat(this).format(new Date(mDate.toMillis(true))));
        ((Button) findViewById(R.id.btn_visit_date)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });
        ((Button) findViewById(R.id.btn_visit_time)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID);
            }
        });


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void linkCounterControls(final String name, final int idBtnLess, final int idBtnMore, final int idText) {

        ((TextView) findViewById(idText)).setText(mValues.get(name).toString());

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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                mDate.set(0, minute, hourOfDay, mDate.monthDay, mDate.month, mDate.year);
                                mDate.normalize(true);
                                ((Button) findViewById(R.id.btn_visit_time)).setText(android.text.format.DateFormat.getTimeFormat(Visit.this).format(new Date(mDate.toMillis(true))));
                            }
                        },
                        mDate.hour, mDate.minute, android.text.format.DateFormat.is24HourFormat(this));
            case DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                mDate.set(0, mDate.minute, mDate.hour, dayOfMonth, monthOfYear, year);
                                mDate.normalize(true);
                                ((Button) findViewById(R.id.btn_visit_date)).setText(android.text.format.DateFormat.getDateFormat(Visit.this).format(new Date(mDate.toMillis(true))));
                                updateYear();
                            }
                        },
                        mDate.year, mDate.month, mDate.monthDay);


            case DIALOG_DELETE:
                return new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_delete_visit)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create();
        }
        return null;
    }


    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case TIME_DIALOG_ID:
                ((TimePickerDialog) dialog).updateTime(mDate.hour, mDate.minute);
                break;
            case DATE_DIALOG_ID:
                ((DatePickerDialog) dialog).updateDate(mDate.year, mDate.month, mDate.monthDay);
                break;
            case DIALOG_DELETE:
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = AppDbOpenHelper.getInstance(Visit.this).getWritableDatabase();
                        db.execSQL("DELETE FROM `visit` WHERE rowid=?", new Long[]{mVisitId});
                        Door.updateVisits(Visit.this, mDoorId);
                        Toast.makeText(Visit.this, R.string.msg_visit_deleted, Toast.LENGTH_SHORT).show();
                        setResult(1);
                        finish();
                    }
                });
                alertDialog.setButton(alertDialog.BUTTON_NEGATIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.visit, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_delete).setVisible(mVisitId != 0);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                showDialog(DIALOG_DELETE);
                break;
            case R.id.menu_ok:
                mDate.switchTimezone("UTC");

                SQLiteDatabase db = AppDbOpenHelper.getInstance(Visit.this).getWritableDatabase();
                if (mVisitId == 0) {
                    db.execSQL("INSERT INTO visit (territory_id,door_id,person_id,desc,calc_auto,type,date,magazines,brochures,books,tracts,publications,videos)" +
                                    "VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?)",
                            new Object[]{mTerritoryId, mDoorId, mPersonId, mDesc, mType,
                                    mDate.format3339(false), mValues.get(MAGAZINES), mValues.get(BROCHURES),
                                    mValues.get(BOOKS), mValues.get(TRACTS), mValues.get(PUBLICATIONS),
                                    mValues.get(VIDEOS)});
                } else {
                    db.execSQL("UPDATE visit SET desc=?,type=?,`date`=?,magazines=?,brochures=?,books=?,tracts=?,publications=?,videos=? WHERE ROWID=?",
                            new Object[]{mDesc, mType, mDate.format3339(false),
                                    mValues.get(MAGAZINES), mValues.get(BROCHURES), mValues.get(BOOKS),
                                    mValues.get(TRACTS), mValues.get(PUBLICATIONS), mValues.get(VIDEOS),
                                    mVisitId});
                }

                Door.updateVisits(Visit.this, mDoorId);

                setResult(1);
                finish();
                break;
            case android.R.id.home:
                Intent intent = new Intent(this, Door.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("door", mDoorId);
                startActivity(intent);
                return true;
        }

        return false;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {

        for(String name : COUNTERS) {
            outState.putInt(name, mValues.get(name));
        }
        outState.putInt(TYPE, mType);

        super.onSaveInstanceState(outState);
    }

    private void updateYear() {
        findViewById(R.id.row_books).setVisibility(mType != TYPE_NA && mDate.year < Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_brochures).setVisibility(mType != TYPE_NA && mDate.year < Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_magazines).setVisibility(mType != TYPE_NA && mDate.year < Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_tracts).setVisibility(mType != TYPE_NA && mDate.year < Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_publications).setVisibility(mType != TYPE_NA && mDate.year >= Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
        findViewById(R.id.row_videos).setVisibility(mType != TYPE_NA && mDate.year >= Session.NEW_REPORT_YEAR ? View.VISIBLE : View.GONE);
    }


    private class VisitTypeAdapter extends BaseAdapter implements SpinnerAdapter {

        private int[] icons = {R.drawable.visit_na, R.drawable.first_visit, R.drawable.revisit, R.drawable.study};


        private LayoutInflater mInflater;
        private Context mContext;

        public VisitTypeAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
            mContext = context;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.spinner_visit_type, null);
            }
            ((ImageView) convertView.findViewById(R.id.spinner_visit_type_icon)).setImageResource(icons[position]);
            ((TextView) convertView.findViewById(R.id.spinner_visit_type_text)).setText(mContext.getResources().getStringArray(R.array.visit_types)[position]);
            convertView.setPadding(0, 0, 0, 0);
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.spinner_visit_type, null);
            }
            ((ImageView) convertView.findViewById(R.id.spinner_visit_type_icon)).setImageResource(icons[position]);
            ((TextView) convertView.findViewById(R.id.spinner_visit_type_text)).setText(mContext.getResources().getStringArray(R.array.visit_types)[position]);
            return convertView;
        }



    }


}
