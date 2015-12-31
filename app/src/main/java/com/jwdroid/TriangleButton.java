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
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Button;

public class TriangleButton extends Button {
	
	static public final int TOP_LEFT = 0;
	static public final int TOP_RIGHT = 1;
	static public final int BOTTOM_RIGHT = 2;
	static public final int BOTTOM_LEFT = 3;
	
	private Path mPath;
	
	private int mOrientation = TOP_RIGHT;
	
	private Paint mPaint, mPaintStroke;
	
	private int mRadius = 16;
	
	public TriangleButton(Context context) {
		super(context);
		
		mPaint = new Paint();
		mPaint.setAntiAlias(true);	
		mPaint.setColor(0);
		
		mPaintStroke = new Paint();
		mPaintStroke.setColor(0);
		mPaintStroke.setStyle(Paint.Style.STROKE);
		mPaintStroke.setAntiAlias(true);	
		
		mRadius *= context.getResources().getDisplayMetrics().density;
	}
	
	public TriangleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.TriangleButton);
		int color = styles.getColor(R.styleable.TriangleButton_triangleFillColor, Color.BLACK);		
		int colorStroke = styles.getColor(R.styleable.TriangleButton_triangleStrokeColor, color);
		String orientation = styles.getString(R.styleable.TriangleButton_orientation);
		if(orientation != null) {
			if(orientation.equals("top_left")) mOrientation = TOP_LEFT;		
			if(orientation.equals("top_right")) mOrientation = TOP_RIGHT;
			if(orientation.equals("bottom_left")) mOrientation = BOTTOM_LEFT;
			if(orientation.equals("bottom_right")) mOrientation = BOTTOM_RIGHT;
		}
		
		styles.recycle();
		
		mRadius *= context.getResources().getDisplayMetrics().density;
		
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
		
		if(!isEnabled())
			return;
		
		if(mPaint.getColor() == 0)
			return;
		
		int width = getWidth();
		int height = getHeight();
		
		int triangleWidth = (int)(width * 0.4);
		int triangleHeight = (int)(height * 0.4);
		
		Path path = new Path();
		switch(mOrientation) {
		case TOP_LEFT:
			path.moveTo(0, 0);
			path.lineTo(width, 0);
			path.lineTo(0,height);
			break;
			
		case TOP_RIGHT:
			path.moveTo(width-triangleWidth, 0);
			path.lineTo(width-mRadius, 0);
			path.arcTo(new RectF(width-mRadius,0,width,mRadius), 270, 90);
			path.lineTo(width,triangleHeight);
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
			
		}
		
		Path pathStroke = new Path(path);
		path.close();
		canvas.drawPath(path, mPaint);
		if(mPaint.getColor() != mPaintStroke.getColor() && mPaintStroke.getColor() != 0)
			canvas.drawPath(pathStroke, mPaintStroke);
	
		
	}
	
	public void setColor(int color) {
		mPaint.setColor(color);
		invalidate();
	}
	
	public void setColor(String color) {
		mPaint.setColor(Color.parseColor(color));
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
	
	@Override
	protected ContextMenuInfo getContextMenuInfo() {	
		TriangleButtonContextMenuInfo info = new TriangleButtonContextMenuInfo();
		info.setView(this);
		return info;
	}
	
	
	static public class TriangleButtonContextMenuInfo implements ContextMenuInfo {
		private View mView;

		public View getView() {
			return mView;
		}

		public void setView(View view) {
			this.mView = view;
		}
	}


}
