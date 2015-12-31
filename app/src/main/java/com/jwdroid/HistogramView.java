package com.jwdroid;

import java.util.ArrayList;

import com.jwdroid.ui.Door;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class HistogramView extends View {
	
	private ArrayList<Integer> mColors = new ArrayList<Integer>();

	public HistogramView(Context context) {
		super(context);		
	}
	
	public HistogramView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {	
		super.onDraw(canvas);
		
		if(mColors.size() == 0)
			return;
		
		float w = getMeasuredWidth();
		float h = getMeasuredHeight();
		
		float left = 0;
		float widthOne = w / mColors.size();
		
		Paint paint = new Paint();		
				
		for(int i=0;i<mColors.size();i++) {
			//if(mColors.get(i) != 0) {
				paint.setColor(getContext().getResources().getColor(Door.COLORS_LIGHT[mColors.get(i)]));
				paint.setAlpha(50);
				canvas.drawRect(left, 0, left+widthOne, h, paint);				
			//}
			left += widthOne;
		}
	}
	
	public void setColors(ArrayList<Integer> colors) {
		mColors = colors;
		invalidate();
	}

}
