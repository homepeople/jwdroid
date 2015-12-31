package com.jwdroid;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;


abstract public class SimpleArrayRecyclerAdapter<T> extends RecyclerView.Adapter {

    protected LayoutInflater mInflater;
    protected List<T> mItems;
    protected Context mContext;

    public SimpleArrayRecyclerAdapter(Context context, List<T> items) {
        mInflater = LayoutInflater.from(context);
        mItems = items;
        if(items == null)
        	mItems = new ArrayList<T>();
        mContext = context;        
    }

    public T getItem(int position) {
        return mItems.get(position); 
    }
    
    public int getPositionById(long id) {
    	int count = getItemCount();
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

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}