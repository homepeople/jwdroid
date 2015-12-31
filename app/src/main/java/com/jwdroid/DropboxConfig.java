package com.jwdroid;

import android.content.Context;

import com.dropbox.sync.android.DbxAccountManager;

public class DropboxConfig {
    public static final String appKey = "nll1o6m22dxqt40";
    public static final String appSecret = "y2txp5u77g90fuf";

    public static DbxAccountManager getAccountManager(Context context)
    {
        return DbxAccountManager.getInstance(context.getApplicationContext(), appKey, appSecret);
    }
}