package com.jwdroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.BaseAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class HorizontalPanelsView extends HorizontalScrollView {
	
	private static final String TAG = "JWHorizontalGroupsView";
	
    private static final int SWIPE_MIN_DISTANCE = 3;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;    
    private static final int SCROLL_MIN_DISTANCE_X = 40;
    private static final int SCROLL_MIN_DISTANCE_Y = 20;
    
    private static final int NONE = 0;
    private static final int VERTICAL = 1;
    private static final int HORIZONTAL = 2;
    
 
    private GestureDetector mGestureDetector;
    private int mActiveScroll = 0;
    private float mDownX = 0, mDownY = 0, mLastX = 0, mLastY = 0;
    private int mLastScrollX = 0, mLastScrollY = 0;
    
    private int mScrollingDirection = 0;
    
    private LinearLayout mInternalWrapper;
    
    private OnActiveChangedListener mActiveChangedListener;
 
    public HorizontalPanelsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        initInternalWrapper();
        mGestureDetector = new GestureDetector(new MyGestureDetector());
    }
 
    public HorizontalPanelsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        initInternalWrapper();
        mGestureDetector = new GestureDetector(new MyGestureDetector());
    }
 
    public HorizontalPanelsView(Context context) {
        super(context);
        
        initInternalWrapper();
        mGestureDetector = new GestureDetector(new MyGestureDetector());
    }
    
    
    public void setOnActiveChangedListener(OnActiveChangedListener listener) {
    	mActiveChangedListener = listener;
    }
    
    public int getActivePos() {
    	return mActiveScroll;
    }
    
    public ViewGroup getActiveViewGroup() {
    	return (ViewGroup)mInternalWrapper.getChildAt(mActiveScroll);
    }
    
    public void setActiveViewGroup(int n) {
    	
    	post(new Runnable() {
    	    @Override
    	    public void run() {
    	        int featureWidth = getMeasuredWidth();
    	    	scrollTo(featureWidth*mActiveScroll, 0);
    	    } 
    	});
    	
    	mActiveScroll = n;
    }    
    
    public void initInternalWrapper() {
    	 mInternalWrapper = new LinearLayout(getContext());
         mInternalWrapper.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
         mInternalWrapper.setOrientation(LinearLayout.HORIZONTAL);
         addView(mInternalWrapper);
    }
    
    public void addViewGroup(ViewGroup viewGroup) {
    	mInternalWrapper.addView(viewGroup);
    }
    
    public void removeViewGroups() {
    	mInternalWrapper.removeAllViews();
    }
    
    public int getViewGroupsCount() {
    	return mInternalWrapper.getChildCount();
    }
    
    public ViewGroup getViewGroupAt(int index) {
    	return (ViewGroup)mInternalWrapper.getChildAt(index);
    }    
    

    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	Log.w(TAG, String.format("touch action=%d x=%f y=%f dir=%d",event.getAction(),event.getX(),event.getY(),mScrollingDirection));
    	
    	return processTouchEvent(event);
    }   
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	Log.w(TAG, String.format("intercept action=%d x=%f y=%f dir=%d",event.getAction(),event.getX(),event.getY(),mScrollingDirection));
    	
    	processTouchEvent(event);
    	
    	return mScrollingDirection == HORIZONTAL;
    }
    
    

    private boolean processTouchEvent(MotionEvent event) {
    	
    	float density = getResources().getDisplayMetrics().density;
    	
    	
    	if (mGestureDetector.onTouchEvent(event)) {
            return false;
        }
    	else if(event.getAction() == MotionEvent.ACTION_DOWN) {    		 
    		 mDownX = mLastX = event.getX();
    		 mDownY = mLastY = event.getY();
    		 mLastScrollX = getScrollX();
    		 mScrollingDirection = NONE;
    		 return true;
    	 }    	 
    	 else if(event.getAction() == MotionEvent.ACTION_MOVE ) {    		 
    		 float curX = event.getX();
    		 float curY = event.getY();
    		 
    		 if(mDownX == 0) {
    			 mDownX = curX;
    			 mDownY = curY;
    			 mLastScrollX = getScrollX();
    		 }
    		 
    		 if(mScrollingDirection == NONE && Math.abs(Math.round(mDownX-curX)) > SCROLL_MIN_DISTANCE_X*density || mScrollingDirection == HORIZONTAL ) {
	    		 mScrollingDirection = HORIZONTAL;	    		       	 
	    		 scrollTo(Math.round(getScrollX() - curX + mLastX), 0);
    		 }
    		 if(mScrollingDirection == NONE && Math.abs(Math.round(mDownY-curY)) > SCROLL_MIN_DISTANCE_Y*density || mScrollingDirection == VERTICAL ) {
	    		 mScrollingDirection = VERTICAL;	    
	    	}
    		 
    		 mLastX = curX;
    		 mLastY = curY;
    		 return true;
    	 }
         else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ){
        	 if(mScrollingDirection == HORIZONTAL) {
	             int scrollX = getScrollX();
	             Display display = ((Activity)getContext()).getWindowManager().getDefaultDisplay();
	         	 int width = display.getWidth();
	             int featureWidth = getMeasuredWidth();
	             mActiveScroll = (scrollX + featureWidth/2)/featureWidth;
	             int scrollTo = mActiveScroll*featureWidth;             
	             smoothScrollTo(scrollTo, 0);
	             if(mActiveChangedListener != null)
	            	 mActiveChangedListener.onActiveChanged(mActiveScroll);
        	 }
        	 mDownX = mDownY = 0;
             mScrollingDirection = NONE;
             return true;
         }
         else{
             return false;
         }
    }
    
    
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {            	
            	
            	float density = getResources().getDisplayMetrics().density;
            	
            	if( mScrollingDirection != VERTICAL ) {
	            	Log.w(TAG, String.format("onFling x1:%f (%f), x2:%f", e1.getX(), mDownX, e2.getX()));
	                
	                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE*density && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY*density) {
	                	int featureWidth = getMeasuredWidth();
	                    mActiveScroll = (mActiveScroll < (getViewGroupsCount() - 1))? mActiveScroll + 1:getViewGroupsCount() -1;
	                    smoothScrollTo(mActiveScroll*featureWidth, 0);
	                    if(mActiveChangedListener != null)
	   	            	 	mActiveChangedListener.onActiveChanged(mActiveScroll);
	                    mScrollingDirection = NONE;
	                    return true;
	                }
	                
	                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE*density && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY*density) {
	                	int featureWidth = getMeasuredWidth();
	                    mActiveScroll = (mActiveScroll > 0)? mActiveScroll - 1:0;
	                    smoothScrollTo(mActiveScroll*featureWidth, 0);
	                    if(mActiveChangedListener != null)
	   	            	 	mActiveChangedListener.onActiveChanged(mActiveScroll);
	                    mScrollingDirection = NONE;
	                    return true;
	                }
            	}
            	else {
            		mScrollingDirection = NONE;
            	}
            	
            	
            } catch (Exception e) {
                    Log.e("Fling", "There was an error processing the Fling event:" + e.getMessage());
            }
            return false;
        }
    }
    
    
    public interface OnActiveChangedListener {
    	public void  onActiveChanged(int newActive);
    };
   
   
}