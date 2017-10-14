package com.jwdroid.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.DropboxConfig;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayAdapter;
import com.jwdroid.export.DropboxBackuper;
import com.jwdroid.export.Importer;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupList extends AppCompatActivity /*implements ConnectionCallbacks, OnConnectionFailedListener */ {

    private static final String TAG = "BackupList";

    static private final int DIALOG_DELETE = 1;
    static private final int DIALOG_RESTORE = 2;
    static private final int DIALOG_CREATE = 3;

    static private final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    static private final int FOLDER_CHOOSE_REQUEST_CODE = 2;
    static private final int REQUEST_LINK_TO_DBX = 3;
    static private final int REQUEST_FILE = 4;

    private BackupListAdapter mListAdapter;
    private ListView mListView;

    private Long mDialogItemId;

    //private GoogleApiClient mGoogleApiClient;
    private DbxClientV2 mDbxClient = null;
    private boolean mDriveConnected = false, mDriveConnecting = false, mRequestDriveAccess = false;
    private List<BackupItem> mDriveContents = null;

    Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.title_drive);
        setSupportActionBar(mToolbar);

        BugSenseConfig.initAndStartSession(this);

        mListView = (ListView) findViewById(R.id.backup_list);

        mListAdapter = new BackupListAdapter(this, null);

        mListView.setAdapter(mListAdapter);


        final QuickAction listActions = new QuickAction(this);
        listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_backup_restore), getResources().getDrawable(R.drawable.ac_doc_export)));
        listActions.addActionItem(new ActionItem(getResources().getString(R.string.action_backup_delete), getResources().getDrawable(R.drawable.ac_trash)));
        listActions.animateTrack(false);
        listActions.setAnimStyle(QuickAction.ANIM_MOVE_FROM_RIGHT);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (((ListItem) mListView.getItemAtPosition(position)).type != ListItem.TYPE_ITEM)
                    return;
                listActions.show(view, id);
            }
        });

        listActions.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                Bundle args;
                SQLiteDatabase db;
                Cursor rs;
                switch (pos) {
                    case 0:    // Восстановить
                        mDialogItemId = listActions.getId();
                        showDialog(DIALOG_RESTORE);
                        break;
                    case 1:    // Удалить
                        mDialogItemId = listActions.getId();
                        showDialog(DIALOG_DELETE);
                        break;
                }
            }
        });



	    /*
        mGoogleApiClient = new GoogleApiClient.Builder(this)
	        .addApi(Drive.API)
	        .addScope(Drive.SCOPE_FILE)
	        .addScope(Drive.SCOPE_APPFOLDER)
	        .addConnectionCallbacks(this)
	        .addOnConnectionFailedListener(this)
	        .build();*/

        updateContent();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final String dbxAccessToken = DropboxConfig.getToken(BackupList.this);
        if (dbxAccessToken != null) {
            mDriveContents = null;
            mDbxClient = DropboxConfig.getDbxClient(dbxAccessToken);
            updateContent();
            loadDriveContents(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //mDriveConnecting = true;
        //mGoogleApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.backup_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_add:
                final ProgressDialog progressDialog = ProgressDialog.show(BackupList.this, "",
                        getResources().getString(R.string.lbl_please_wait), true);

                Runnable callback = new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.cancel();
                        updateContent();
                        loadDriveContents(false);
                    }
                };

                if (mDbxClient != null)
                    new DropboxBackuper(getApplicationContext(), callback).run();
                else {
                    progressDialog.cancel();
                    Auth.startOAuth2Authentication(BackupList.this, DropboxConfig.appKey);
                }
                break;

            case R.id.menu_refresh:
                mDriveContents = null;
                updateContent();
                loadDriveContents(true);
                break;

        }

        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        LayoutInflater factory = LayoutInflater.from(this);

        switch (id) {

            case DIALOG_RESTORE:
                dialog = new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_restore_backup)
                        .setPositiveButton(R.string.btn_yes, null)
                        .setNegativeButton(R.string.btn_no, null)
                        .create();
                break;

            case DIALOG_DELETE:
                dialog = new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_delete_backup)
                        .setPositiveButton(R.string.btn_yes, null)
                        .setNegativeButton(R.string.btn_no, null)
                        .create();
                break;
        }

        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_RESTORE: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        final ProgressDialog progressDialog = ProgressDialog.show(BackupList.this, "",
                                getResources().getString(R.string.lbl_please_wait), true);

                        new AsyncTask<BackupItem, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(BackupItem... params) {
                                boolean result = true;

                               try {
                                    Time time = new Time();
                                    time.setToNow();

                                    BackupItem backupItem = params[0];
                                    DbxDownloader downloader = mDbxClient.files().download("/" + backupItem.getFilename());
                                    try {
                                        new Importer(BackupList.this, downloader.getInputStream()).run();
                                    } catch (Exception e) {
                                        Log.e(TAG, e.toString());
                                        result = false;
                                    } finally {
                                        downloader.close();
                                    }

                                } catch (Exception e) {
                                    Log.e(TAG, e.toString());
                                    result = false;
                                }

                                return result;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {

                                progressDialog.cancel();

                                if (result) {
                                    Toast.makeText(BackupList.this, getResources().getString(R.string.msg_restore_backup_success), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(BackupList.this, getResources().getString(R.string.msg_restore_backup_failed), Toast.LENGTH_LONG).show();
                                }

                                super.onPostExecute(result);
                            }

                        }.execute((BackupItem) mListAdapter.getItemById(mDialogItemId));

                    }
                });

                break;
            }
            case DIALOG_DELETE: {
                AlertDialog alertDialog = (AlertDialog) dialog;
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, null, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BackupItem item = (BackupItem) mListAdapter.getItemById(mDialogItemId);

                        new AsyncTask<String,Void,Void>() {
                            @Override
                            protected Void doInBackground(String... params) {
                                try {
                                    mDbxClient.files().deleteV2("/" + params[0]);
                                } catch (Exception e) {
                                    Log.e(TAG, e.toString());
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void aVoid) {
                                loadDriveContents(false);
                                updateContent();
                            }
                        }.execute(item.getFilename());
                    }
                });
                break;
            }
        }
    }


    private void updateContent() {


        ArrayList<ListItem> items = new ArrayList<ListItem>();

        items.add(new HeadingItem(HeadingItem.HEADING_GOOGLE));
        if (mDriveConnecting)
            items.add(new LoadingItem());
        else if (mDbxClient == null)
            items.add(new DriveItem());
        else if (mDriveContents == null)
            items.add(new LoadingItem());
        else {
            if (mDriveContents.size() == 0)
                items.add(new LabelItem());
            else
                for (BackupItem item : mDriveContents) {
                    items.add(item);
                }
        }

        mListAdapter.swapData(items);
    }


    private class ListItem {
        static final int TYPE_HEADING = 0;
        static final int TYPE_ITEM = 1;
        static final int TYPE_DRIVE = 2;
        static final int TYPE_LOADING = 3;
        static final int TYPE_LABEL = 4;

        int type;
    }

    private class DriveItem extends ListItem {
        public DriveItem() {
            type = TYPE_DRIVE;
        }
    }

    private class LoadingItem extends ListItem {
        public LoadingItem() {
            type = TYPE_LOADING;
        }
    }

    private class LabelItem extends ListItem {
        public LabelItem() {
            type = TYPE_LABEL;
        }
    }

    private class HeadingItem extends ListItem {
        static final int HEADING_GOOGLE = 0;
        static final int HEADING_LOCAL = 1;

        int heading;

        public HeadingItem(int h) {
            type = TYPE_HEADING;
            heading = h;
        }
    }

    private class BackupItem extends ListItem {
        Time time;
        Boolean zip, drive;

        public BackupItem(Time t, Boolean z, Boolean d) {
            type = TYPE_ITEM;
            time = t;
            zip = z;
            drive = d;
        }

        public String getFilename() {
            return "backup_" + time.toMillis(true) + (zip ? ".zip" : "");
        }
    }

    public static class BackupFilenameFilter implements FilenameFilter {

        Pattern p;

        public BackupFilenameFilter() {
            p = Pattern.compile("^backup_\\d+(\\.zip)?$");
        }

        @Override
        public boolean accept(File dir, String name) {
            Matcher m = p.matcher(name);
            return m.matches();
        }

    }


    private class BackupListAdapter extends SimpleArrayAdapter<ListItem> {

        public BackupListAdapter(Context context, ArrayList<ListItem> items) {
            super(context, items);
        }

        public long getItemId(int position) {
            ListItem item = mItems.get(position);
            if (item.type != ListItem.TYPE_ITEM)
                return 0;
            return ((BackupItem) item).time.toMillis(true);
        }

        public int getItemViewType(int position) {
            return mItems.get(position).type;
        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            ListItem item = mItems.get(position);

            if (item.type == ListItem.TYPE_HEADING) {
                HeadingItem headingItem = (HeadingItem) item;
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.backup_list_heading, null);
                    holder = new ViewHolder();
                    holder.icon = (ImageView) convertView.findViewById(R.id.backup_item_heading_icon);
                    holder.name = (TextView) convertView.findViewById(R.id.backup_item_heading_label);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                if (headingItem.heading == HeadingItem.HEADING_GOOGLE) {
                    holder.name.setText(R.string.lbl_drive);
                    holder.icon.setImageResource(R.drawable.dropbox_small);
                }
                if (headingItem.heading == HeadingItem.HEADING_LOCAL) {
                    holder.name.setText(R.string.lbl_local_folder);
                    holder.icon.setImageResource(R.drawable.folder);
                }

                return convertView;
            }

            if (item.type == ListItem.TYPE_LABEL) {

                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.backup_list_label, null);
                }

                return convertView;
            }

            if (item.type == ListItem.TYPE_DRIVE) {

                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.backup_list_drive, null);
                    ((Button) convertView.findViewById(R.id.btn_drive_turn_on)).setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            //mRequestDriveAccess = true;
                            //mDriveConnecting = true;
                            //updateContent();
                            //mGoogleApiClient.connect();

                            Auth.startOAuth2Authentication(BackupList.this, DropboxConfig.appKey);
                        }
                    });
                }

                return convertView;
            }

            if (item.type == ListItem.TYPE_LOADING) {

                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.backup_list_loading, null);
                    Animation rotation = AnimationUtils.loadAnimation(BackupList.this, R.anim.counterclockwise_rotation);
                    rotation.setRepeatCount(Animation.INFINITE);
                    convertView.findViewById(R.id.img_loading).startAnimation(rotation);
                }

                return convertView;
            }


            BackupItem backupItem = (BackupItem) item;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.backup_list_item, null);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.backup_item_name);
                holder.size = (TextView) convertView.findViewById(R.id.backup_item_size);

                convertView.setTag(holder);
            } else {

                holder = (ViewHolder) convertView.getTag();
            }

            Date date = new Date(backupItem.time.toMillis(true));
            holder.name.setText(Html.fromHtml(DateFormat.getDateInstance(DateFormat.LONG).format(date) + ", " + android.text.format.DateFormat.getTimeFormat(mContext).format(date)));

            return convertView;
        }

        class ViewHolder {
            TextView name, size;
            ImageView icon;
        }

    }


/*
    @Override
	public void onConnected(Bundle arg0) {
		mDriveConnecting = false;
		mDriveConnected = true;
		
		
		
		//loadDriveContents();		
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		
		if(!mRequestDriveAccess) {
			mDriveConnecting = false;
			updateContent();
			return;
		}
		
		mRequestDriveAccess = false;
		
		if (connectionResult.hasResolution()) {
	        try {
	            connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
	        } catch (IntentSender.SendIntentException e) {
	            // Unable to resolve, message user appropriately
	        }
	    } else {
	        GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
	    }
		
		
		
	}*/

    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mDriveConnecting = true;
                    //mGoogleApiClient.connect();
                    updateContent();
                }
                break;

            case REQUEST_LINK_TO_DBX:
                if (resultCode == RESULT_OK) {
                    mDriveConnecting = false;
                    new DropboxBackuper(this, new Runnable() {
                        @Override
                        public void run() {
                            updateContent();
                        }
                    }).run();
                    updateContent();
                    loadDriveContents(false);
                }
                break;

            case REQUEST_FILE:

                break;
        }
    }

    private void loadDriveContents(final boolean force) {

        if(mDbxClient == null) {
            return;
        }

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params) {
                List<BackupItem> contents = new ArrayList<BackupItem>();

                try {

                    ListFolderResult result = mDbxClient.files().listFolder("");
                    Pattern p = Pattern.compile("^backup_(\\d+)\\.zip$");
                    while (true) {
                        for (Metadata metadata : result.getEntries()) {
                            String name = metadata.getName();
                            Log.w(TAG, name);

                            Matcher m = p.matcher(name);
                            if (m.matches()) {
                                Time time = new Time();
                                time.set(Long.parseLong(m.group(1)));
                                contents.add(new BackupItem(time, true, true));
                            }
                        }

                        if (!result.getHasMore()) {
                            break;
                        }

                        result = mDbxClient.files().listFolderContinue(result.getCursor());
                    }

                    Collections.reverse(contents);

                } catch (Exception e) {

                }
				



                return contents;
            }

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);

                mDriveContents = (List<BackupItem>) result;
                updateContent();
            }
        }.execute();


    }

	/*static public DriveFolder getDriveFolder(Context context, GoogleApiClient client) {


		DriveFolder rootFolder = Drive.DriveApi.getRootFolder(client);
		DriveFolder folder;

		Query query = new Query.Builder()
			.addFilter(Filters.eq(SearchableField.TITLE, "jwdroid2"))
			.build();
		MetadataBufferResult metadataBufferResult = rootFolder.queryChildren(client, query).await();

		if(!metadataBufferResult.getStatus().isSuccess())
			return null;

		MetadataBuffer buffer = metadataBufferResult.getMetadataBuffer();
		if(buffer.getCount() == 0) {
			MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
            	.setTitle("jwdroid2")
            	.build();
			DriveFolderResult driveFolderResult = rootFolder.createFolder(client, changeSet).await();
			if(!driveFolderResult.getStatus().isSuccess())
				return null;

			folder = driveFolderResult.getDriveFolder();
		}
		else {
			DriveId id = buffer.get(0).getDriveId();
			folder = Drive.DriveApi.getFolder(client, id);
		}

		buffer.close();

		return folder;
	}*/

}
