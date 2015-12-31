package com.jwdroid.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.jwdroid.AlphanumComparator;
import com.jwdroid.ui.BackupList;

public class LocalBackuper extends Backuper {
	
	public LocalBackuper(Context context, Runnable callback) {
		super(context, callback);
	}
	
	protected void backup() throws Exception {
		Time time = new Time();
		time.setToNow();
		
		File root = Environment.getExternalStorageDirectory(); 
		
		String filename = root.getAbsolutePath()+"/jwdroid/backup_"+time.toMillis(true)+".zip";
		
		OutputStream os = new FileOutputStream(filename);
		
		new Exporter(mContext, os).run();
		
		os.flush();
        os.close();
        
        // Удаляем старые
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	    int numBackups = Integer.parseInt(prefs.getString("num_backups", "20"));
	    if(numBackups > 0) {	        
	        FilenameFilter filter = new BackupList.BackupFilenameFilter();
		    ArrayList<String> items = new ArrayList<String>();
		    
		    File dir = new File(root, "jwdroid");
		    File[] files = dir.listFiles(filter);
		    for(File file : files) {
		    	items.add(file.getName());
		    }
		    
		    Collections.sort(items, new AlphanumComparator());
		    
		    for(int i=0; i<items.size()-numBackups; i++) {
		    	new File(dir, items.get(i)).delete();
		    }
	    }
	}

}
