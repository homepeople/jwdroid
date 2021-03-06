package com.jwdroid.export;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.jwdroid.R;

abstract public class Backuper {
	
	protected Context mContext;
	protected Runnable mCallback;
	
	public Backuper(Context context, Runnable runnable) {
		mContext = context;
		mCallback = runnable;
	}
	
	public void run() {
		new AsyncTask<Void,Void,Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				try {

					backup();

				    return true;
				}
				catch (Exception e) {

					Log.e("Backuper", e.toString());

					return false;
				}
			}
			protected void onPostExecute(Boolean result) {
				if(result) {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.msg_backup_created), Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(mContext, mContext.getResources().getString(R.string.msg_backup_failed), Toast.LENGTH_LONG).show();
				}
				if(mCallback != null) mCallback.run();

			};
		}.execute();
	}
	
	abstract void backup() throws Exception;

}
