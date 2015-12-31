package com.jwdroid.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.jwdroid.BugSenseConfig;
import com.jwdroid.R;

public class Help extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseConfig.initAndStartSession(this);

        setContentView(R.layout.help);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_help);
        setSupportActionBar(toolbar);

        WebView mWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");

        String locale = getResources().getConfiguration().locale.getLanguage();
        if (locale.equals("ru"))
            mWebView.loadUrl("https://cdn.rawgit.com/jwdroid/jwdroid/master/.help/ru.html");
        else if (locale.equals("es"))
            mWebView.loadUrl("https://cdn.rawgit.com/jwdroid/jwdroid/master/.help/es.html");
        else
            mWebView.loadUrl("https://cdn.rawgit.com/jwdroid/jwdroid/master/.help/en.html");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
}
