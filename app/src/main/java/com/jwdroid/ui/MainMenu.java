package com.jwdroid.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.export.Exporter;
import com.jwdroid.export.Importer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainMenu extends AppCompatActivity {

    private static final String TAG = "MainMenu";

    static private final int REQUEST_BACKUP = 1;
    static private final int REQUEST_RESTORE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.main_menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        showRevisionNotes();

		 /*try {
			AppDbOpenHelper.copyDataBase();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        findViewById(R.id.btn_territories).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, TerritoryList.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_people).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, PeopleList.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_chrono).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, Chrono.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_reports).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, ReportList.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_drive).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, BackupList.class);
                startActivity(intent);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_preferences:
                Intent intent = new Intent(this, Preferences.class);
                startActivity(intent);
                break;

            case R.id.menu_help:
                intent = new Intent(this, Help.class);
                startActivity(intent);
                break;

            case R.id.menu_contribute:
                intent = new Intent(this, Contribute.class);
                startActivity(intent);
                break;

            case R.id.menu_community:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/112371364498094492171")));
                break;

            case R.id.menu_about:
                DialogFragment dialog = new AboutDialog();
                dialog.show(getSupportFragmentManager(), null);
                break;

            case R.id.menu_backup:
                try {
                    Calendar calendar = new GregorianCalendar();
                    String filename = String.format("jwdroid_%d-%02d-%02d-%02d-%02d-%02d_%s_%s.zip",
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH)+1,
                            calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            calendar.get(Calendar.SECOND),
                            Build.MANUFACTURER,
                            Build.MODEL);

                    File file = new File(getCacheDir(), filename);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    new Exporter(getApplicationContext(), outputStream).run();
                    outputStream.flush();
                    outputStream.close();

                    Uri contentUri = FileProvider.getUriForFile(getApplicationContext(),
                            "com.jwdroid.fileprovider", file);

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.setType("application/zip");
                    shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(Intent.createChooser(shareIntent,
                            getApplication().getResources().getString(R.string.action_save_data)),
                            REQUEST_BACKUP);
                }
                catch(Exception e) {
                    Log.e(TAG, e.toString());
                    Toast.makeText(getApplicationContext(), R.string.msg_backup_failed, Toast.LENGTH_LONG)
                            .show();
                }
                break;

            case R.id.menu_restore:
                new AlertDialog.Builder(this)
                        .setCancelable(true)
                        .setMessage(R.string.msg_restore_backup)
                        .setNegativeButton(R.string.btn_no, null)
                        .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("application/zip");
                                startActivityForResult(Intent.createChooser(intent,
                                                getApplication().getResources().getString(R.string.action_restore_data)),
                                        REQUEST_RESTORE);
                            }
                        })
                        .create()
                        .show();


                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_BACKUP:
                if(resultCode == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), R.string.msg_backup_created, Toast.LENGTH_LONG).show();
                }
                break;

            case REQUEST_RESTORE:
                if(resultCode == RESULT_OK) {

                    final ProgressDialog progressDialog = ProgressDialog.show(MainMenu.this, "",
                            getResources().getString(R.string.lbl_please_wait), true);

                    new AsyncTask<Uri, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Uri... params) {
                            boolean result = true;
                            try {

                                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(params[0], "r");
                                FileDescriptor fd = pfd.getFileDescriptor();
                                FileInputStream is = new FileInputStream(fd);
                                new Importer(MainMenu.this, is).run();
                                is.close();
                                result = true;
                            }
                            catch(Exception e) {
                                result = false;
                                Log.e(TAG, e.toString());
                            }

                            return result;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {

                            progressDialog.cancel();

                            if (result) {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_restore_backup_success), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_restore_backup_failed), Toast.LENGTH_LONG).show();
                            }

                            super.onPostExecute(result);
                        }

                    }.execute(data.getData());


                }
                break;
        }
    }

    private void showRevisionNotes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("revision_notes_1_5_3", false)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("revision_notes_1_5_3", true);
            editor.commit();

            DialogFragment dialog = new ChangelogDialog();
            dialog.show(getSupportFragmentManager(), null);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContributeDialog.check(this);
    }

    static public class ChangelogDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.msg_revision_notes)
                    .setMessage(R.string.msg_revision_notes_1_5_3)
                    .setPositiveButton(R.string.btn_ok, null).create();
        }

        @Override
        public void onStart() {
            super.onStart();
            ((TextView)getDialog().findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    static public class AboutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            LayoutInflater factory = LayoutInflater.from(getActivity());
            View layout = factory.inflate(R.layout.dlg_about, null);

            try {
                ((TextView) layout.findViewById(R.id.lbl_header)).setText(
                        getActivity().getResources().getString(R.string.app_name) + " " +
                                getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            }
            catch(Exception e) {
            }

            ((TextView)layout.findViewById(R.id.lbl_thanks)).setText("Anton Chivchalov, Ruben Reis, Luis Montes de Oca, Paul Kruk, Marcin Budziński, Radek Choronží, Stanislav Kolesnik, Matthias Wirobski, Tiago Aguiar, Abel Puertas, Pablo Carrasco Jr, Andrea Isaza, Vytautas Virganavičius, Mrydka Śmigacz, Eugen Betke, Pedro Paulo, Erik van Brakel, Brendo Gabriel Meireles, Wiktoria Zawisza, Jacek Ziółkowski, Carl Johnson Hansen, Mihai Hedes");

            return new AlertDialog.Builder(getActivity())
                    .setView(layout)
                    //.setMessage(R.string.msg_revision_notes_1_4_1)
                    .setPositiveButton(R.string.btn_ok, null).create();
        }

        @Override
        public void onStart() {
            super.onStart();
            ((TextView)getDialog().findViewById(R.id.lbl_github_url)).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }


}
