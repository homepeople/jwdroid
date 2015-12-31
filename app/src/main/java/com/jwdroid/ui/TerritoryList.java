package com.jwdroid.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.format.Time;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.AsyncLoader;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.HistogramView;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayRecyclerAdapter;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


public class TerritoryList extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final String TAG = "JWDroidTerritoryListActivity";

    private static final int MENU_DELETE = Menu.FIRST + 1;

    private static final int DIALOG_ADD = 1;

    private TerritoryListAdapter mListAdapter;
    private long mTerritoryForDelete;
    private RecyclerView mListView;

    private Long mDialogItemId;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.territory_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_territory_list);
        setSupportActionBar(toolbar);

        // Set up territory list

        mListView = (RecyclerView) findViewById(R.id.territory_list);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        registerForContextMenu(mListView);


        mListAdapter = new TerritoryListAdapter(this, new ArrayList<TerritoryItem>());
        mListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                findViewById(R.id.territory_list_empty).setVisibility(mListAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mListView.setAdapter(mListAdapter);




        /*mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                Intent intent = new Intent(TerritoryList.this, Territory.class);
                intent.putExtra("territory", id);
                startActivity(intent);
            }
        });*/

        getSupportLoaderManager().initLoader(0, null, this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.territory_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_add:
                showDialog(DIALOG_ADD);
                break;
        }

        return false;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        switch (v.getId()) {
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        }

        return true;

    }

    @Override
    protected void onResume() {
        super.onResume();
        TerritoryList.this.getSupportLoaderManager().getLoader(0).forceLoad();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        LayoutInflater factory = LayoutInflater.from(this);
        SQLiteDatabase db = AppDbOpenHelper.getInstance(TerritoryList.this).getWritableDatabase();
        final View dlgEditLayout = factory.inflate(R.layout.dlg_edit, null);
        switch (id) {

            case DIALOG_ADD:

                ((TextView) dlgEditLayout.findViewById(R.id.lbl_dlgedit_note)).setText(R.string.dlg_territory_add_note);
                ((TextView) dlgEditLayout.findViewById(R.id.lbl_dlgedit_note)).setVisibility(View.VISIBLE);



                dialog = new AlertDialog.Builder(this, android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
                        .setTitle(R.string.dlg_territory_add_label)
                        .setView(dlgEditLayout)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                int error = 0;

                                Editable editable = ((EditText) dlgEditLayout.findViewById(R.id.edit_dlgedit_text)).getText();

                                if (editable.length() == 0)
                                    error = R.string.err_empty_name;

                                if (error > 0) {
                                    Toast.makeText(dlgEditLayout.getContext(), error, Toast.LENGTH_SHORT).show();
                                    return;
                                }


                                SQLiteDatabase db = AppDbOpenHelper.getInstance(TerritoryList.this).getWritableDatabase();
                                db.execSQL("INSERT INTO territory (name,created,started) VALUES(?,datetime('now'),datetime('now'))", new String[]{editable.toString()});

                                Toast.makeText(dlgEditLayout.getContext(), R.string.msg_territory_added, Toast.LENGTH_SHORT).show();

                                getSupportLoaderManager().getLoader(0).forceLoad();

                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null).create();

                break;

        }

        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Loader loader = null;
        if (id == 0)
            loader = new TerritoryListLoader(this);
        else if (id == 1)
            loader = new TerritoryDoorsLoader(this);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == 0) {
            ArrayList<TerritoryItem> data = new ArrayList<TerritoryItem>();
            while (cursor.moveToNext()) {
                TerritoryItem item = new TerritoryItem();
                item.id = cursor.getLong(0);
                item.name = cursor.getString(1);
                item.started = new Time();
                item.started.set(cursor.getLong(2) * 1000);

                if (!cursor.isNull(3)) {
                    item.finished = new Time();
                    item.finished.set(cursor.getLong(3) * 1000);
                }
                item.doorColors = new ArrayList<Integer>();

                data.add(item);
            }
            mListAdapter.swapData(data);
            getSupportLoaderManager().restartLoader(1, null, this);
        } else if (loader.getId() == 1) {
            HashMap<Long, ArrayList<Integer>> territoryColors = new HashMap<Long, ArrayList<Integer>>();
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                Long territoryId = cursor.getLong(0);
                Integer color = cursor.getInt(1);
                if (!territoryColors.containsKey(territoryId))
                    territoryColors.put(territoryId, new ArrayList<Integer>());
                territoryColors.get(territoryId).add(color);
            }
            for (Long territoryId : territoryColors.keySet()) {
                TerritoryItem item = mListAdapter.getItemById(territoryId);
                if (item != null)
                    item.doorColors = territoryColors.get(territoryId);
            }
            mListAdapter.notifyDataSetChanged();
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.swapData(new ArrayList<TerritoryItem>());
    }

    private class TerritoryItem {
        long id;
        String name;
        Time started, finished = null;
        ArrayList<Integer> doorColors;
    }


    static public class TerritoryListLoader extends AsyncLoader<Cursor> {


        public TerritoryListLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            SQLiteDatabase db = AppDbOpenHelper.getInstance(getContext()).getWritableDatabase();
            Cursor rs = db.rawQuery("SELECT rowid _id,name,strftime('%s',started), strftime('%s', finished) FROM territory ORDER BY started DESC", new String[]{});
            return rs;
        }
    }

    static public class TerritoryDoorsLoader extends AsyncLoader<Cursor> {


        public TerritoryDoorsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            SQLiteDatabase db = AppDbOpenHelper.getInstance(getContext()).getWritableDatabase();
            Cursor rs = db.rawQuery("SELECT territory_id,color1 FROM door ORDER BY territory_id,group_id,order_num ASC", new String[]{});

            return rs;
        }
    }


    private class TerritoryListAdapter extends SimpleArrayRecyclerAdapter<TerritoryItem> {

        public TerritoryListAdapter(Context context, ArrayList<TerritoryItem> items) {
            super(context, items);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View convertView = mInflater.inflate(R.layout.territory_list_item, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            holder.name = (TextView) convertView.findViewById(R.id.territory_list_item_name);
            holder.started = (TextView) convertView.findViewById(R.id.territory_list_item_started);
            holder.finished = (TextView) convertView.findViewById(R.id.territory_list_item_finished);
            holder.histogram = (HistogramView) convertView.findViewById(R.id.territory_list_item_histogram);
            return holder;
        }


        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
            TerritoryItem item = mItems.get(position);

            ViewHolder holder = (ViewHolder)h;

            holder.pos = position;
            holder.name.setText(item.name);
            holder.started.setText(android.text.format.DateFormat.getDateFormat(mContext).format(new Date(item.started.toMillis(true))));
            if (item.finished == null)
                holder.finished.setVisibility(View.GONE);
            else {
                holder.finished.setVisibility(View.VISIBLE);
                holder.finished.setText(android.text.format.DateFormat.getDateFormat(mContext).format(new Date(item.finished.toMillis(true))));
            }
            holder.histogram.setColors(item.doorColors);
        }

        public long getItemId(int position) {
            return mItems.get(position).id;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
            TextView name, started, finished;
            HistogramView histogram;
            int pos;
            View mView;

            public ViewHolder(View itemView) {
                super(itemView);
                mView = itemView;
                mView.setOnClickListener(this);
                mView.setOnLongClickListener(this);
            }

            public void onClick(View v) {
                Intent intent = new Intent(TerritoryList.this, Territory.class);
                intent.putExtra("territory", mItems.get(pos).id);
                startActivity(intent);
            }

            @Override
            public boolean onLongClick(View v) {
                final QuickAction listActions = new QuickAction(TerritoryList.this);
                listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_territory_change_name), getResources().getDrawable(R.drawable.ac_pencil)));
                listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_territory_info), getResources().getDrawable(R.drawable.ac_info)));
                listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_territory_delete), getResources().getDrawable(R.drawable.ac_trash)));
                listActions.animateTrack(false);
                listActions.setAnimStyle(QuickAction.ANIM_MOVE_FROM_RIGHT);


                listActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
                    @Override
                    public void onItemClick(int pos) {
                        Bundle args;
                        SQLiteDatabase db;
                        Cursor rs;
                        switch (pos) {
                            case 0: {        // Название
                                mDialogItemId = listActions.getId();
                                Territory.ChangeNameDialog dialog = new Territory.ChangeNameDialog();
                                args = new Bundle();
                                args.putLong("id", mDialogItemId);
                                dialog.setArguments(args);
                                dialog.setListener(new Territory.ChangeNameDialog.OnChangeNameListener() {
                                    @Override
                                    public void onDone(String newName) {
                                        mListAdapter.getItemById(mDialogItemId).name = newName;
                                        mListAdapter.notifyDataSetChanged();
                                    }
                                });
                                dialog.show(getSupportFragmentManager(), null);

                                break;
                            }
                            case 1: // Информация
                                Intent intent = new Intent(TerritoryList.this, TerritoryInfo.class);
                                intent.putExtra("territory", listActions.getId());
                                startActivity(intent);
                                break;
                            case 2: {   // Удалить
                                mTerritoryForDelete = listActions.getId();
                                Territory.DeleteDialog dialog = new Territory.DeleteDialog();
                                args = new Bundle();
                                args.putLong("id", mTerritoryForDelete);
                                dialog.setArguments(args);
                                dialog.setListener(new Territory.DeleteDialog.OnDeleteListener() {
                                    @Override
                                    public void onDone() {
                                        getSupportLoaderManager().getLoader(0).forceLoad();
                                    }
                                });
                                dialog.show(getSupportFragmentManager(), null);
                                break;
                            }
                        }
                    }
                });

                listActions.show(v, mItems.get(pos).id);
                return true;
            }
        }

    }


}