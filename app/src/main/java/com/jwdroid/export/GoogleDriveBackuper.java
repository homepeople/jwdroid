package com.jwdroid.export;

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.text.format.Time;

import com.jwdroid.ui.BackupList;

public class GoogleDriveBackuper extends Backuper {
	
	public GoogleDriveBackuper(Context context, Runnable callback) {
		super(context, callback);
	}
	
	protected void backup() throws Exception {
		Time time = new Time();
		time.setToNow();
		
		/*GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
	        .addApi(Drive.API)
	        .addScope(Drive.SCOPE_FILE)
	        .addScope(Drive.SCOPE_APPFOLDER)
	        .build();
    	        
	    ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);
	    
	    if(!connectionResult.isSuccess())
	    	throw new Exception();
	
		DriveFolder folder = BackupList.getDriveFolder(mContext, googleApiClient);
		
		if(folder == null)
			throw new Exception();
	    		
		MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
	    	.setTitle("backup_"+time.toMillis(true)+".zip")
	    	.build();
		
		ContentsResult contentsResult = Drive.DriveApi.newContents(googleApiClient).await();
		
		if(!contentsResult.getStatus().isSuccess())
			throw new Exception();
		
		Contents contents = contentsResult.getContents();
		new Exporter(mContext, contents.getOutputStream()).run();			        		
	    		
		DriveFileResult fileResult = folder.createFile(googleApiClient, changeSet, contents).await();
		
		if(!fileResult.getStatus().isSuccess())
			throw new Exception();
	    	
		//Drive.DriveApi.requestSync(googleApiClient).await();*/
	}

}
