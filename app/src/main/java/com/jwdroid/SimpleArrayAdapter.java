package com.jwdroid;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;


abstract public class SimpleArrayAdapter<T> extends BaseAdapter {
	
    protected LayoutInflater mInflater;
    protected List<T> mItems;
    protected Context mContext;
   
    public SimpleArrayAdapter(Context context, List<T> items) {
        mInflater = LayoutInflater.from(context);
        mItems = items;
        if(items == null)
        	mItems = new ArrayList<T>();
        mContext = context;        
    }

    public int getCount() {
        return mItems.size();
    }

    public T getItem(int position) {
        return mItems.get(position); 
    }
    
    public int getPositionById(long id) {
    	int count = getCount();
    	for (int i = 0; i < count; i++)
        {
            if (getItemId(i) == id)
                return i;
        }
        return -1;
    }
    
    public T getItemById(long id) {
    	int pos = getPositionById(id);
    	if(pos == -1)
    		return null;
    	else
    		return getItem(pos);
    }
    
    public void swapData(List<T> data) {
    	mItems = data;
    	notifyDataSetChanged();
    }
}