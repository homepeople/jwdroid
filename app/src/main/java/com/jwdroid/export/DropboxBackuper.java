package com.jwdroid.export;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadUploader;
import com.jwdroid.AlphanumComparator;
import com.jwdroid.DropboxConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropboxBackuper extends Backuper {
	
	public DropboxBackuper(Context context, Runnable callback) {
		super(context, callback);
	}
	
	protected void backup() throws Exception {
		Time time = new Time();
		time.setToNow();

		String accessToken = DropboxConfig.getToken(mContext);
		if(accessToken == null) {
			return;
		}
		DbxClientV2 client = DropboxConfig.getDbxClient(accessToken);
		if(client == null) {
			return;
		}

		UploadUploader uploader = client.files().upload("/backup_"+time.toMillis(true)+".zip");
		new Exporter(mContext, uploader.getOutputStream()).run();
		uploader.finish();

		// Удаляем старые

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		int numBackups = Integer.parseInt(prefs.getString("num_backups", "20"));
		if(numBackups > 0) {

			ArrayList<String> items = new ArrayList<String>();
			ListFolderResult result = client.files().listFolder("");
			Pattern p = Pattern.compile("^backup_(\\d+)\\.zip$");
			while (true) {
				for (Metadata metadata : result.getEntries()) {
					Matcher m = p.matcher(metadata.getName());
					if(m.matches())
						items.add(metadata.getName());
				}

				if (!result.getHasMore()) {
					break;
				}

				result = client.files().listFolderContinue(result.getCursor());
			}

			Collections.sort(items, new AlphanumComparator());

			for(int i=0; i<items.size()-numBackups; i++) {
				client.files().deleteV2("/"+items.get(i));
			}
		}
	}

}
