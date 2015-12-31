package com.jwdroid;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TriangleView extends View {
	
	static public final int TOP_LEFT = 0;
	static public final int TOP_RIGHT = 1;
	static public final int BOTTOM_RIGHT = 2;
	static public final int BOTTOM_LEFT = 3;
	static public final int RIGHT = 4;
	static public final int LEFT = 5;
		
	private int mOrientation = TOP_RIGHT;

	private int mRadius;
	
	private Paint mPaint, mPaintStroke;
	
	public TriangleView(Context context) {
		super(context);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);	
		
		mPaintStroke = new Paint();
		mPaintStroke.setAntiAlias(true);	
		mPaintStroke.setColor(0);
		mPaintStroke.setStyle(Paint.Style.STROKE);
	}
	
	public TriangleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.TriangleView);
		int color = styles.getColor(R.styleable.TriangleView_fillColor, Color.BLACK);		
		int colorStroke = styles.getColor(R.styleable.TriangleView_strokeColor, color);
		String orientation = styles.getString(R.styleable.TriangleView_orientation);
		if(orientation != null) {
			if(orientation.equals("top_left")) mOrientation = TOP_LEFT;		
			if(orientation.equals("top_right")) mOrientation = TOP_RIGHT;
			if(orientation.equals("bottom_left")) mOrientation = BOTTOM_LEFT;
			if(orientation.equals("bottom_right")) mOrientation = BOTTOM_RIGHT;
			if(orientation.equals("right")) mOrientation = RIGHT;
			if(orientation.equals("left")) mOrientation = LEFT;
		}
		mRadius = styles.getInt(R.styleable.TriangleView_radius, 8);
		mRadius *= context.getResources().getDisplayMetrics().density;
		
		styles.recycle();
		
		mPaint = new Paint();
		mPaint.setColor(color);
		mPaint.setAntiAlias(true);
		
		mPaintStroke = new Paint();
		mPaintStroke.setColor(colorStroke);
		mPaintStroke.setStyle(Paint.Style.STROKE);
		mPaintStroke.setAntiAlias(true);		
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int width = getWidth();
		int height = getHeight();
		
		Path path = new Path();
		switch(mOrientation) {
		case TOP_LEFT:
			path.moveTo(0, 0);
			path.lineTo(width, 0);
			path.lineTo(0,height);
			break;
			
		case TOP_RIGHT:
			if(mRadius == 0) {
				path.moveTo(0, 0);
				path.lineTo(width, 0);
				path.lineTo(width,height);
			}
			else {
				path.moveTo(0, 0);
				path.lineTo(width-mRadius, 0);
				path.arcTo(new RectF(width-mRadius,0,width,mRadius), 270, 90);
				path.lineTo(width,height);
			}
			break;
			
		case BOTTOM_RIGHT:
			path.moveTo(width,0);
			path.lineTo(width, height);
			path.lineTo(0,height);
			break;
			
		case BOTTOM_LEFT:
			path.moveTo(0, 0);
			path.lineTo(width, height);
			path.lineTo(0,height);
			break;
			
		case RIGHT:
			path.moveTo(0, 0);
			path.lineTo(width, height/2);
			path.lineTo(0,height);
			break;
			
		case LEFT:
			path.moveTo(width, 0);
			path.lineTo(0, height/2);
			path.lineTo(width,height);
			break;
		}
				
		Path pathStroke = new Path(path);
		path.close();
		canvas.drawPath(path, mPaint);
		if(mPaint.getColor() != mPaintStroke.getColor() && mPaintStroke.getColor() != 0)
			canvas.drawPath(pathStroke, mPaintStroke);
		
	
	}
	
	public void setColor(int color) {
		mPaint.setColor(color);
		mPaintStroke.setColor(color);
		invalidate();
	}
	
	public void setColor(String color) {
		mPaint.setColor(Color.parseColor(color));
		mPaintStroke.setColor(Color.parseColor(color));
		invalidate();
	}
	
	public void setColorStroke(int color) {
		mPaintStroke.setColor(color);
		invalidate();
	}
	
	public void setColorStroke(String color) {
		mPaintStroke.setColor(Color.parseColor(color));
		invalidate();
	}
	
	public int getOrientation() {
		return mOrientation;
	}

	public void setOrientation(int orientation) {
		this.mOrientation = orientation;
	}

	public int getRadius() {
		return mRadius;
	}

	public void setRadius(int radius) {
		this.mRadius = radius;
	}

}
