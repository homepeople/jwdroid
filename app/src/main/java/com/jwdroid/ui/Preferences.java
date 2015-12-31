package com.jwdroid.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.dropbox.sync.android.DbxAccountManager;
import com.jwdroid.BugSenseConfig;
import com.jwdroid.DropboxConfig;
import com.jwdroid.R;

public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setTitle(R.string.title_preferences);

        addPreferencesFromResource(R.xml.preferences);

        final DbxAccountManager dbxMgr = DropboxConfig.getAccountManager(Preferences.this);

        Preference dropboxPref = (Preference) findPreference("dropbox_off");

        dropboxPref.setEnabled(dbxMgr.hasLinkedAccount());

        dropboxPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (dbxMgr.hasLinkedAccount()) {
                    dbxMgr.unlink();
                    preference.setEnabled(false);
                }

                return false;
            }
        });
    }
}
