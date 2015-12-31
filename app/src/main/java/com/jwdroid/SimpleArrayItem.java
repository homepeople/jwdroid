package com.jwdroid;

public class SimpleArrayItem {
	public Object data;
	public long id;
	public SimpleArrayItem(long _id, Object _data) {
		id = _id;
		data = _data;
	}
	
	public String toString() {
		return data.toString();
	}
}
