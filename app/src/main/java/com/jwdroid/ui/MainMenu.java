package com.jwdroid.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;

import java.io.File;

public class MainMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.main_menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        showRevisionNotes();


        File root = Environment.getExternalStorageDirectory();

        File dir = new File(root, "jwdroid");
        if (!dir.exists())
            dir.mkdir();

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

        }
        return super.onOptionsItemSelected(item);
    }



    private void showRevisionNotes() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("revision_notes_1_4_1", false)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("revision_notes_1_4_1", true);
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
                    .setMessage(R.string.msg_revision_notes_1_4_1)
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

            ((TextView)layout.findViewById(R.id.lbl_thanks)).setText("Anton Chivchalov, Ruben Reis, Luis Montes de Oca, Paul Kruk, Marcin Budziński, Rados Devadesátdva, Stanislav Kolesnik, Matthias Wirobski, Tiago Aguiar, Abel Puertas, Pablo Carrasco Jr, Andrea Isaza, Vytautas Virganavičius, Mrydka Śmigacz, Eugen Betke, Pedro Paulo");

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
