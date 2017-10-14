package com.jwdroid.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

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

        final Preference dropboxPref = (Preference) findPreference("dropbox_off");

        Boolean dropboxEnabled = DropboxConfig.getToken(this) != null;
        dropboxPref.setEnabled(dropboxEnabled);
        ((Preference) findPreference("autobackup")).setEnabled(dropboxEnabled);
        ((Preference) findPreference("num_backups")).setEnabled(dropboxEnabled);

        dropboxPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                DropboxConfig.clearToken(Preferences.this);
                preference.setEnabled(false);
                ((Preference) findPreference("autobackup")).setEnabled(false);
                ((Preference) findPreference("num_backups")).setEnabled(false);
                return false;
            }
        });
    }
}
