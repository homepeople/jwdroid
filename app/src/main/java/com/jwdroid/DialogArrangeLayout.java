package com.jwdroid;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class DialogArrangeLayout extends RelativeLayout{
	
	private boolean mRowsAutoChanged = false;
	private boolean mColsAutoChanged = false;
	private Context mContext;
	private TextWatcher mRowsTextWatcher = null, mColsTextWatcher = null;

	public DialogArrangeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	
	public void prepare(AppDbOpenHelper dbHelper, Long territoryId, Integer groupId) {
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		final EditText editRows = (EditText)findViewById(R.id.edit_arrange_rows);
		final EditText editCols = (EditText)findViewById(R.id.edit_arrange_cols);
		    		
		Cursor rs = db.rawQuery("SELECT COUNT(*) FROM door WHERE territory_id=? AND group_id=?", new String[] {territoryId.toString(), String.valueOf(groupId)});
		rs.moveToFirst();
		final Integer cntDoors = rs.getInt(0);
		rs.close();
		
		Integer cols = Util.dbFetchInt(db, "SELECT MAX(col)-MIN(col)+1 FROM door WHERE territory_id=? AND group_id=?", new String[] {territoryId.toString(), String.valueOf(groupId)});
		Integer rows = Util.dbFetchInt(db, "SELECT MAX(row)-MIN(row)+1 FROM door WHERE territory_id=? AND group_id=?", new String[] {territoryId.toString(), String.valueOf(groupId)});
		
		mRowsAutoChanged = mColsAutoChanged = false;
		if(mRowsTextWatcher != null)
			editRows.removeTextChangedListener(mRowsTextWatcher);
		if(mColsTextWatcher != null)
			editCols.removeTextChangedListener(mColsTextWatcher);
		
		editRows.setText(rows.toString());
		editCols.setText(cols.toString());
		
		mRowsTextWatcher = new TextWatcher() {					
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}					
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}					
			@Override
			public void afterTextChanged(Editable s) {
				if(mRowsAutoChanged) {
					mRowsAutoChanged = false;
					return;
				}
				mColsAutoChanged = true;
				try { 
					Integer rows = Integer.parseInt(s.toString());
					if(rows <= 0) {
						editCols.setText("");
					}
					else {								
						Integer cols = (int)Math.ceil((float)cntDoors/rows);
						editCols.setText(cols.toString());
					}							
				}
				catch(Exception e) {
					editCols.setText("");
				}				
			}
		};
		editRows.addTextChangedListener(mRowsTextWatcher);
		
		mColsTextWatcher = new TextWatcher() {					
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}					
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}					
			@Override
			public void afterTextChanged(Editable s) {
				if(mColsAutoChanged) {
					mColsAutoChanged = false;
					return;
				}
				mRowsAutoChanged = true;
				try { 
					Integer cols = Integer.parseInt(s.toString());
					if(cols <= 0) {
						editRows.setText("");
					}
					else {								
						Integer rows = (int)Math.ceil((float)cntDoors/cols);
						editRows.setText(rows.toString());
					}							
				}
				catch(Exception e) {
					editRows.setText("");
				}
				
			}
		};		
		editCols.addTextChangedListener(mColsTextWatcher);
		
	}
	
}