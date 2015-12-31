package com.jwdroid.ui;

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.AsyncLoader;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.ColorPicker;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayAdapter;
import com.jwdroid.Util;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.util.ArrayList;
import java.util.Date;

public class PeopleList extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final int DIALOG_EDIT_PERSON = 1;
    private static final int DIALOG_DELETE_PERSON = 2;
    private static final int DIALOG_COLOR = 3;

    private PeopleListAdapter mListAdapter;
    private ListView mListView;

    private Long mDialogItemId;

    private ColorPicker mColorPicker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.people_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_people);
        setSupportActionBar(toolbar);

        // Set up territory list

        mListView = (ListView) findViewById(R.id.people_list);

        mColorPicker = new ColorPicker(this, 0, 0);


        mListAdapter = new PeopleListAdapter(this, new ArrayList<PeopleItem>());
        mListAdapter.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                findViewById(R.id.people_list_empty).setVisibility(mListAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                PeopleItem item = mListAdapter.getItem(position);

                Intent intent = new Intent(PeopleList.this, Door.class);
                intent.putExtra("territory", item.territoryId);
                intent.putExtra("door", item.doorId);
                intent.putExtra("person", item.id);
                startActivityForResult(intent, 1);

            }
        });

        final QuickAction personActions = new QuickAction(this);
        personActions.addActionItem(new ActionItem(getResources().getString(R.string.action_person_change), getResources().getDrawable(R.drawable.ac_pencil)));
        personActions.addActionItem(new ActionItem(getResources().getString(R.string.action_object_change_color), getResources().getDrawable(R.drawable.ac_color)));
        personActions.addActionItem(new ActionItem(getResources().getString(R.string.action_person_delete), getResources().getDrawable(R.drawable.ac_trash)));
        personActions.animateTrack(false);
        personActions.setAnimStyle(QuickAction.ANIM_MOVE_FROM_RIGHT);
        personActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                switch (pos) {
                    case 0:    // Изменить
                        mDialogItemId = personActions.getId();
                        showDialog(DIALOG_EDIT_PERSON);
                        break;
                    case 1:    // Цвет
                        mDialogItemId = personActions.getId();
                        showDialog(DIALOG_COLOR);
                        break;
                    case 2: // Удалить
                        mDialogItemId = personActions.getId();
                        showDialog(DIALOG_DELETE_PERSON);
                        break;
                }
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int arg2, long id) {
                personActions.show(v, id);
                return true;
            }
        });


        getSupportLoaderManager().restartLoader(0, null, this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.people_list, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                mDialogItemId = null;
                showDialog(DIALOG_EDIT_PERSON);
                break;
        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder;
        LayoutInflater factory = LayoutInflater.from(this);
        switch (id) {
            case DIALOG_COLOR:
                dialog = mColorPicker.getDialog();
                break;

            case DIALOG_EDIT_PERSON:

                final View view = factory.inflate(R.layout.dlg_edit, null);
                ((EditText) view.findViewById(R.id.edit_dlgedit_text)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.msg_person_desc)
                        .setView(view)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Editable editable = ((EditText) view.findViewById(R.id.edit_dlgedit_text)).getText();
                                SQLiteDatabase db = AppDbOpenHelper.getInstance(PeopleList.this).getWritableDatabase();

                                if (mDialogItemId == null) {
                                    db.execSQL("INSERT INTO door (territory_id,group_id,col,row,name,color1,color2,visits_num,order_num) VALUES(0,0,0,0,\"\",0,0,0,0)", new Object[]{});
                                    Long doorId = Util.dbFetchLong(db, "SELECT last_insert_rowid()", new String[]{});
                                    db.execSQL("INSERT INTO person (door_id,name) VALUES(?,?)", new Object[]{doorId.toString(), editable.toString()});
                                } else {
                                    db.execSQL("UPDATE person SET name=? WHERE ROWID=?", new Object[]{editable.toString(), mDialogItemId.toString()});
                                }

                                getSupportLoaderManager().getLoader(0).forceLoad();


                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null).create();
                break;


            case DIALOG_DELETE_PERSON:
                builder = new AlertDialog.Builder(this);
                builder.setCancelable(true)
                        .setMessage(R.string.msg_delete_person)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_cancel, null);
                dialog = builder.create();
                break;
        }

        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_COLOR: {
                SQLiteDatabase db = AppDbOpenHelper.getInstance(PeopleList.this).getWritableDatabase();
                Cursor rs = db.rawQuery("SELECT door.ROWID,door.color1,door.color2 FROM person LEFT JOIN door ON door.ROWID=person.door_id WHERE person.ROWID=?", new String[]{mDialogItemId.toString()});
                rs.moveToFirst();
                Integer color1 = rs.getInt(1);
                Integer color2 = rs.getInt(2);
                final Long doorId = rs.getLong(0);
                rs.close();

                mColorPicker.setColors(color1, color2);

                mColorPicker.setOkListener(new ColorPicker.OnOkListener() {
                    @Override
                    public void onOk(int newColor1, int newColor2) {
                        SQLiteDatabase db = AppDbOpenHelper.getInstance(PeopleList.this).getWritableDatabase();
                        db.execSQL("UPDATE `door` SET color1=?,color2=?,manual_color=1 WHERE ROWID=?", new Object[]{new Integer(newColor1), new Integer(newColor2), doorId});


                        int pos = mListAdapter.getPositionById(mDialogItemId);
                        ((PeopleItem) mListAdapter.getItem(pos)).doorColor1 = newColor1;
                        ((PeopleItem) mListAdapter.getItem(pos)).doorColor2 = newColor2;
                        mListAdapter.notifyDataSetChanged();


                    }
                });
                break;
            }

            case DIALOG_EDIT_PERSON:

                if (mDialogItemId == null) {
                    ((TextView) dialog.findViewById(R.id.lbl_dlgedit_note)).setText(R.string.lbl_person_add_from_people);
                    ((TextView) dialog.findViewById(R.id.lbl_dlgedit_note)).setVisibility(View.VISIBLE);
                    ((EditText) dialog.findViewById(R.id.edit_dlgedit_text)).setText("");
                } else {
                    ((TextView) dialog.findViewById(R.id.lbl_dlgedit_note)).setVisibility(View.GONE);
                    SQLiteDatabase db = AppDbOpenHelper.getInstance(PeopleList.this).getWritableDatabase();
                    Cursor rs = db.rawQuery("SELECT name FROM person WHERE ROWID=?", new String[]{mDialogItemId.toString()});
                    rs.moveToNext();
                    ((EditText) dialog.findViewById(R.id.edit_dlgedit_text)).setText(rs.getString(0));
                    rs.close();
                }

                break;


            case DIALOG_DELETE_PERSON:

                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SQLiteDatabase db = AppDbOpenHelper.getInstance(PeopleList.this).getWritableDatabase();
                        Long doorId = Util.dbFetchLong(db, "SELECT door_id FROM person WHERE ROWID=?", new String[]{mDialogItemId.toString()});
                        db.execSQL("DELETE FROM `visit` WHERE person_id=?", new Long[]{mDialogItemId});
                        db.execSQL("DELETE FROM `person` WHERE ROWID=?", new Long[]{mDialogItemId});
                        Door.updateVisits(PeopleList.this, doorId);
                        Toast.makeText(PeopleList.this, R.string.msg_person_deleted, Toast.LENGTH_SHORT).show();

                        getSupportLoaderManager().getLoader(0).forceLoad();
                    }
                });

                break;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        PeopleListLoader loader = new PeopleListLoader(this);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        ArrayList<PeopleItem> data = new ArrayList<PeopleItem>();
        while (cursor.moveToNext()) {
            PeopleItem item = new PeopleItem();
            item.id = cursor.getLong(0);
            item.doorId = cursor.getLong(1);
            item.doorName = cursor.getString(2);
            item.territoryId = cursor.getLong(3);
            item.territoryName = cursor.getString(4);
            item.personName = cursor.getString(5);
            item.visitDate = new Time();
            item.visitDate.set(cursor.getLong(6) * 1000);
            item.visitType = cursor.getInt(7);
            item.visitDesc = cursor.getString(8);
            item.doorColor1 = cursor.getInt(9);
            item.doorColor2 = cursor.getInt(10);
            item.visitsNum = cursor.getInt(11);

            data.add(item);
        }
        mListAdapter.swapData(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mListAdapter.swapData(new ArrayList<PeopleItem>());
    }

    @Override
    protected void onActivityResult(int arg0, int arg1, Intent arg2) {
        getSupportLoaderManager().getLoader(0).forceLoad();
    }


    private class PeopleItem {
        Long id, doorId, territoryId;
        String personName, doorName, territoryName, visitDesc;
        Time visitDate;
        Integer visitsNum, visitType, doorColor1, doorColor2;
    }


    static public class PeopleListLoader extends AsyncLoader<Cursor> {


        public PeopleListLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            SQLiteDatabase db = AppDbOpenHelper.getInstance(getContext()).getWritableDatabase();
            Cursor rs = db.rawQuery("SELECT person.rowid _id, " +
                    "		person.door_id, " +
                    "		door.name, " +
                    "		door.territory_id, " +
                    "		territory.name, " +
                    "		person.name, " +
                    "		strftime('%s',visit.date), " +
                    "		visit.type, " +
                    "		visit.desc, " +
                    "		door.color1, " +
                    "		door.color2, " +
                    "		(SELECT COUNT(*) FROM visit WHERE person_id=person.ROWID AND door_id=person.door_id AND type != ?) " +
                    "FROM person " +
                    "LEFT JOIN door ON person.door_id=door.ROWID " +
                    "LEFT JOIN visit ON visit.ROWID IN (SELECT ROWID FROM visit WHERE person_id=person.ROWID AND door_id=person.door_id AND type!=? ORDER BY date DESC LIMIT 1) " +
                    "LEFT JOIN territory ON door.territory_id=territory.ROWID " +
                    "WHERE (reject=0 AND visit.ROWID IS NOT NULL) OR door.territory_id=0 " +
                    "ORDER BY visit.date ASC", new String[]{String.valueOf(Visit.TYPE_NA), String.valueOf(Visit.TYPE_NA)});
            return rs;
        }
    }


    private static class PeopleListAdapter extends SimpleArrayAdapter<PeopleItem> {

        public PeopleListAdapter(Context context, ArrayList<PeopleItem> items) {
            super(context, items);
        }

        public long getItemId(int position) {
            return mItems.get(position).id;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            PeopleItem item = mItems.get(position);

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.people_list_item, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.people_item_name);
                holder.door_name = (TextView) convertView.findViewById(R.id.people_item_door_name);
                holder.desc = (TextView) convertView.findViewById(R.id.people_item_desc);
                holder.visit_type = (ImageView) convertView.findViewById(R.id.people_item_visit_type_icon);
                holder.color1 = convertView.findViewById(R.id.people_item_color1);
                holder.color2 = convertView.findViewById(R.id.people_item_color2);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(item.personName);

            if (item.visitsNum > 0) {
                String snippet = "<s><b>" + android.text.format.DateFormat.getDateFormat(mContext).format(new Date(item.visitDate.toMillis(true))) + "</b></s>: " + item.visitDesc;
                holder.desc.setText(Html.fromHtml(snippet));
                holder.desc.setVisibility(View.VISIBLE);

                holder.visit_type.setImageResource(Visit.TYPE_ICONS[item.visitType]);
                holder.visit_type.setVisibility(View.VISIBLE);
            } else {
                holder.desc.setVisibility(View.GONE);
                holder.visit_type.setVisibility(View.GONE);
            }


            String doorName = item.visitsNum + " " + Util.pluralForm(mContext, item.visitsNum, mContext.getResources().getStringArray(R.array.plural_visits));
            if (item.territoryId != 0)
                doorName += " &bull; " + item.territoryName + ", " + item.doorName;
            holder.door_name.setText(Html.fromHtml(doorName));

            holder.color1.setBackgroundColor(mContext.getResources().getColor(Door.COLORS[item.doorColor1]));
            holder.color2.setBackgroundColor(mContext.getResources().getColor(Door.COLORS[item.doorColor2]));


            return convertView;
        }

        static class ViewHolder {
            ImageView visit_type;
            TextView name, door_name, desc;
            View color1, color2;
        }

    }


}
