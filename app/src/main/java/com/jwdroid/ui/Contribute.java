package com.jwdroid.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;
import com.jwdroid.SimpleArrayItem;
import com.jwdroid.billing.IabHelper;
import com.jwdroid.billing.IabResult;
import com.jwdroid.billing.Inventory;
import com.jwdroid.billing.Purchase;

import java.util.List;

public class Contribute extends AppCompatActivity implements
        OnDonateListener,
        IabHelper.OnIabPurchaseFinishedListener,
        IabHelper.QueryInventoryFinishedListener,
        IabHelper.OnConsumeMultiFinishedListener {

    private static final String TAG = "Contribute";

    public static final String SKU[][] = {
            {"once_1","sub6_1","sub12_1"},
            {"once_5","sub6_5","sub12_5"},
            {"once_10","sub6_10","sub12_10"},
            {"once_20","sub6_20","sub12_20"}};

    IabHelper mIabHelper;
    Integer mSelectedDonation, mSelectedRecurring;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.contribute);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_contribute);


        findViewById(R.id.btn_donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DonateDialog dialog = new DonateDialog();
                dialog.setListener(Contribute.this);
                dialog.show(getSupportFragmentManager(), null);
            }
        });

        findViewById(R.id.btn_translate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://crowdin.com/project/jwdroid/invite"));
                startActivity(browserIntent);
            }
        });

        findViewById(R.id.btn_review).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.jwdroid"));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET|Intent.FLAG_ACTIVITY_MULTIPLE_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(marketIntent);
            }
        });


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mIabHelper = new IabHelper(this, "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvTWUYvLaSl1xs+dyctY+iqJ3f7Z/fx5fEERGEy4fZB6GVJ+D6lKWgZn4GPVRw6G7r5swWuxoSKda2ACJ9dwydk8j8dLAoxagMzFbxFqIZYtH6Nog1nBtrygQ8k7TxJpELNIV2satkqnO7KjqFEpZTdddvG03NqykzutmsFcLQtCb/8q7FCW63KpL/+Sgy13i4BAzNvWBDM4G9mch7buO9gmgDAEbvy8lv0vx5AQaI/WuS+dTLU2WMK6PAwftestPYbMoR2uEDjkpF2bh78pMu96bxEkV1X0Bgn9p+tOXHGzR+IgPFOuK1XVM23PTXfCAc70d5GA+o5i2ypgSw5oJJwIDAQAB");

        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "Problem setting up In-app Billing: " + result);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mIabHelper == null) return;

        if (!mIabHelper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIabHelper != null) mIabHelper.dispose();
        mIabHelper = null;
    }

    @Override
    public void onDonate(int donation, int recurring) {
        mSelectedDonation = donation;
        mSelectedRecurring = recurring;
        mIabHelper.queryInventoryAsync(this);
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        if (result.isSuccess()) {
            mIabHelper.consumeAsync(inv.getAllPurchases(), this);
        } else {
            onConsumeMultiFinished(null, null);
        }
    }

    @Override
    public void onConsumeMultiFinished(List<Purchase> purchases, List<IabResult> results) {
        mIabHelper.launchPurchaseFlow(this, SKU[mSelectedDonation][mSelectedRecurring], 0, this);
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
        if (result.isSuccess()) {
            mIabHelper.consumeAsync(info, null);
        }
    }

    public static class DonateDialog extends DialogFragment {

        OnDonateListener mListener;

        public void setListener(OnDonateListener mListener) {
            this.mListener = mListener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final View layout = getActivity().getLayoutInflater().inflate(R.layout.dlg_donate, null);

            ArrayAdapter adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_item, new SimpleArrayItem[]{
                    new SimpleArrayItem(0, "$1"),
                    new SimpleArrayItem(1, "$5"),
                    new SimpleArrayItem(2, "$10"),
                    new SimpleArrayItem(3, "$20"),
            });
            adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
            ((Spinner) layout.findViewById(R.id.spinner_donation)).setAdapter(adapter);

            adapter = new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_item, new SimpleArrayItem[]{
                    new SimpleArrayItem(0, getActivity().getResources().getStringArray(R.array.btn_recurring)[0]),
                    new SimpleArrayItem(1, getActivity().getResources().getStringArray(R.array.btn_recurring)[1]),
                    new SimpleArrayItem(2, getActivity().getResources().getStringArray(R.array.btn_recurring)[2])
            });
            adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
            ((Spinner) layout.findViewById(R.id.spinner_recurring)).setAdapter(adapter);

            AlertDialog dialog = new AlertDialog.Builder(getActivity(), android.support.v7.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
                    .setView(layout)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mListener != null) {
                                int donationItem = ((Spinner) layout.findViewById(R.id.spinner_donation)).getSelectedItemPosition();
                                int recurringItem = ((Spinner) layout.findViewById(R.id.spinner_recurring)).getSelectedItemPosition();
                                mListener.onDonate(donationItem, recurringItem);
                            }
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create();

            return dialog;
        }


    }
}

interface OnDonateListener {
    void onDonate(int donation, int recurring);
}
