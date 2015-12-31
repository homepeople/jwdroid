package com.jwdroid;

import android.content.Context;

import com.splunk.mint.Mint;

public class BugSenseConfig {
	
	final static public String APIKEY = "bdd93776";
	
	static public void initAndStartSession(Context context) {
		Mint.initAndStartSession(context, APIKEY);
	}

}
