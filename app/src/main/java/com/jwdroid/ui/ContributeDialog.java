package com.jwdroid.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.jwdroid.AppDbOpenHelper;
import com.jwdroid.R;
import com.jwdroid.Util;

public class ContributeDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(
                getActivity(), android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert);

        builder.setMessage(R.string.msg_contribute)
                .setPositiveButton(R.string.btn_learn_more, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(getActivity(), Contribute.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.btn_no_thanks, null);

        return builder.create();
    }

    static public void check(FragmentActivity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if(prefs.getBoolean("serviceJustEnded", false) && !prefs.getBoolean("contributeAsked", false)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("serviceJustEnded");
            SQLiteDatabase db = AppDbOpenHelper.getInstance(activity).getWritableDatabase();
            Integer count = Util.dbFetchInt(db, "SELECT SUM(minutes) FROM `session`", new String[]{});
            if(count > 600) {
                DialogFragment dialog = new ContributeDialog();
                dialog.show(activity.getSupportFragmentManager(), null);
                editor.putBoolean("contributeAsked", true);
            }
            editor.commit();
        }
    }
}