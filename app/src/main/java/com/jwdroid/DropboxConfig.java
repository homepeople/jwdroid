package com.jwdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;

public class DropboxConfig {
    public static final String appKey = "nll1o6m22dxqt40";

    public static DbxClientV2 getDbxClient(String accessToken) {
        DbxRequestConfig config = new DbxRequestConfig("JWDroid");

        return new DbxClientV2(config, accessToken);
    }

    public static String getToken(Context  context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String accessToken = prefs.getString("dropbox-access-token", null);
        if (accessToken != null) {
            return accessToken;
        }
        accessToken = Auth.getOAuth2Token();
        if (accessToken != null) {
            prefs.edit().putString("dropbox-access-token", accessToken).commit();
        }
        return accessToken;
    }

    public static void clearToken(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().remove("dropbox-access-token").commit();
    }
}