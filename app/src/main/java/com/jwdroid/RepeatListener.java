package com.jwdroid;

import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

/**
 * A class, that can be used as a TouchListener on any view (e.g. a Button).
 * It cyclically runs a clickListener, emulating keyboard-like behaviour. First
 * click is fired immediately, next after initialInterval, and subsequent after
 * normalInterval.
 *
 * <p>Interval is scheduled after the onClick completes, so it has to run fast.
 * If it runs slow, it does not generate skipped onClicks.
 */
public class RepeatListener implements OnTouchListener {
	
	private static final int REPEATS_UNTIL_SPEED_UP = 20;

    private Handler handler = new Handler();

    private int initialInterval;
    private final int normalInterval;
    private final OnClickListener clickListener;
    private Rect rect;

    private Runnable handlerRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, normalInterval);
            
            if(repeats >= REPEATS_UNTIL_SPEED_UP) {
            	for(int i=0;i<10;i++)
            		clickListener.onClick(downView);
            }
            else
            	clickListener.onClick(downView);
            
            repeats++;
            
        }
    };

    private View downView;
    private int repeats = 0;

    /**
     * @param initialInterval The interval after first click event
     * @param normalInterval The interval after second and subsequent click 
     *       events
     * @param clickListener The OnClickListener, that will be called
     *       periodically
     */
    public RepeatListener(int initialInterval, int normalInterval, 
            OnClickListener clickListener) {
        if (clickListener == null)
            throw new IllegalArgumentException("null runnable");
        if (initialInterval < 0 || normalInterval < 0)
            throw new IllegalArgumentException("negative interval");

        this.initialInterval = initialInterval;
        this.normalInterval = normalInterval;
        this.clickListener = clickListener;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeCallbacks(handlerRunnable);
                handler.postDelayed(handlerRunnable, initialInterval);
                downView = view;
                rect = new Rect(view.getLeft(), view.getTop(), view.getRight(),
                        view.getBottom());
                clickListener.onClick(view);
                break;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(handlerRunnable);
                repeats = 0;
                downView = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!rect.contains(view.getLeft() + (int) motionEvent.getX(),
                        view.getTop() + (int) motionEvent.getY())) {
                    // User moved outside bounds
                    handler.removeCallbacks(handlerRunnable);
                    repeats = 0;
                    downView = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(handlerRunnable);
                repeats = 0;
                downView = null;
                break;
        }
        return false;
    }
    
    static public interface OnSpeedableClickListener {        
        void onClick(View v);
        void onSpeedClick(View v);
    }

}