package com.jwdroid.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.jwdroid.AlphanumComparator;
import com.jwdroid.DropboxConfig;

public class DropboxBackuper extends Backuper {
	
	public DropboxBackuper(Context context, Runnable callback) {
		super(context, callback);
	}
	
	protected void backup() throws Exception {
		Time time = new Time();
		time.setToNow();
		
		DbxAccountManager dbxMgr = DropboxConfig.getAccountManager(mContext);
		
		if(dbxMgr.hasLinkedAccount()) {
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(dbxMgr.getLinkedAccount());
			
			DbxFile file = dbxFs.create(new DbxPath("/backup_"+time.toMillis(true)+".zip"));
			new Exporter(mContext, file.getWriteStream()).run();
			file.close();
			
			// Удаляем старые
	        
	        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		    int numBackups = Integer.parseInt(prefs.getString("num_backups", "20"));
		    if(numBackups > 0) {	        
		        List<DbxFileInfo> list = dbxFs.listFolder(new DbxPath("/"));
		        ArrayList<String> items = new ArrayList<String>();
		        Pattern p = Pattern.compile("^backup_\\d+\\.zip$");
			    			    
			    for(DbxFileInfo info : list) {
			    	Matcher m = p.matcher(info.path.getName());
			    	if(m.matches())
			    		items.add(info.path.getName());
			    }
			    
			    Collections.sort(items, new AlphanumComparator());
			    
			    for(int i=0; i<items.size()-numBackups; i++) {
			    	dbxFs.delete(new DbxPath("/"+items.get(i)));
			    }
		    }
		}
	}

}
