package com.jwdroid;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TableRow;

import com.jwdroid.ui.Door;

public class ColorPicker {
	private Context mContext;
	private AlertDialog mDialog;
	private OnOkListener mOkListener;
	private int mColor1, mColor2;
	
	private View mDlgColorLayout;
		
	public ColorPicker(Context context, Integer color1, Integer color2) {
		mContext = context;
		
		//new ArrayList<Integer>() 
		//mColorMap.put(R.id.dlg_color_row_1, new ArrayList<Integer>(1,2,3,4));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater factory = LayoutInflater.from(context);
		
		mDlgColorLayout = factory.inflate(R.layout.dlg_color, null);
		
		mColor1 = color1;
		mColor2 = color2;
		
	    for(final int rowId: new int[] {R.id.dlg_color_row_1, R.id.dlg_color_row_2, R.id.dlg_color_row_3, R.id.dlg_color_row_4}) {    	        	    
    	    TableRow tr = (TableRow)mDlgColorLayout.findViewById(rowId);
    		for(int i=0;i<tr.getChildCount();i++) { 
    			Button btn = (Button)( (RelativeLayout)tr.getChildAt(i) ).getChildAt(1); 
    			
    			final int color = Integer.parseInt( (String)btn.getTag() );
    			
    			btn.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {						
						if(rowId == R.id.dlg_color_row_1 || rowId == R.id.dlg_color_row_2)
							mColor1 = mColor2 = color; 
						else
							mColor2 = color;
						
						updateButtons();
					}
				});
    			
    			
    		}
	    }
	    
	    updateButtons();
	    
		
		builder.setCancelable(true)
		   .setView(mDlgColorLayout)
		   .setTitle(R.string.msg_choose_color)
		   .setPositiveButton(R.string.btn_ok, null)
		   .setNegativeButton(R.string.btn_cancel, null);
		mDialog = builder.create();
	}
	
	public Dialog getDialog() {
		return mDialog;
	}
	
	public void setOkListener(OnOkListener listener) {
		mOkListener = listener;
		
		mDialog.setButton(AlertDialog.BUTTON_POSITIVE, mContext.getResources().getString(R.string.btn_ok), new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mOkListener.onOk(mColor1, mColor2);				
			}
		});
	}
		
	public void setColors(int color1, int color2) {
		mColor1 = color1;
		mColor2 = color2;
		updateButtons();
	}
	
	private void updateButtons() {
		 
		for(int rowId: new int[] {R.id.dlg_color_row_1, R.id.dlg_color_row_2, R.id.dlg_color_row_3, R.id.dlg_color_row_4}) {    	        	    
		    TableRow tr = (TableRow)mDlgColorLayout.findViewById(rowId);
			for(int i=0;i<tr.getChildCount();i++) { 
				Button btn = (Button)( (RelativeLayout)tr.getChildAt(i) ).getChildAt(1); 
				
				int color = Integer.parseInt( (String)btn.getTag() );
				if(rowId == R.id.dlg_color_row_1 || rowId == R.id.dlg_color_row_2)
					( (RelativeLayout)tr.getChildAt(i) ).getChildAt(0).setVisibility( mColor1 == color && mColor2 == color ? View.VISIBLE : View.INVISIBLE );
				else {
					btn.setEnabled( color != mColor1 );
					btn.setClickable( color != mColor1 );
					( (RelativeLayout)tr.getChildAt(i) ).getChildAt(0).setVisibility( mColor1 != color && mColor2 == color ? View.VISIBLE : View.INVISIBLE );
					btn.setBackgroundResource(Door.DRAWABLES[mColor1]);					
				}
			}
		}
	}
	
	
	
	
	public interface OnOkListener {
		public void onOk(int newColor1, int newColor2);
	}
	
}
