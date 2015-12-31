package com.jwdroid;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class DividerView extends View {
	
	private Paint mPaint;
	
	public DividerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mPaint = new Paint();
		
		TypedArray styles = context.obtainStyledAttributes(attrs,
                R.styleable.DividerView);
       
        setColor(styles.getColor(R.styleable.DividerView_dividerColor, Color.argb(150, 255, 255, 255)));

       
        styles.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w=0, h=0;
		if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
			h = MeasureSpec.getSize(heightMeasureSpec);
		else
			h = 1;
		w = MeasureSpec.getSize(widthMeasureSpec);
		
		setMeasuredDimension(w,h);		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
	}
	
	public void setColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }
}
