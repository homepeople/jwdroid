package com.jwdroid.export;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jwdroid.AppDbOpenHelper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Exporter {
	
	private OutputStream mOutputStream;
	private ZipOutputStream mZip;
	private Context mContext;
	
	public Exporter(Context context, OutputStream os) {
		mOutputStream = os;		
		mContext = context;
	}
	
	public void run() throws IOException {
		mZip = new ZipOutputStream(new BufferedOutputStream(mOutputStream));
		
		try {
			exportTable("door", new String[]{"territory_id","group_id","col","row","name","color1","color2","visits_num","last_date","last_person_name","last_desc","last_person_reject","order_num","manual_color","last_modified_date"});
			exportTable("person", new String[]{"door_id","name","reject"});
			exportTable("territory", new String[]{"name","notes","created","started","finished","modified"});
			exportTable("visit", new String[]{"territory_id","door_id","person_id","date","desc","type","calc_auto","brochures","books","magazines","tracts","videos","publications"});
			exportTable("session", new String[]{"date","minutes","books","brochures","magazines","tracts","returns","desc","videos","publications"});
		}
		finally {
			mZip.close();
		}
	}
	
	private void exportTable(String tableName, String[] columns) throws IOException {
		StringBuilder out = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<document>\n");
		SQLiteDatabase db = AppDbOpenHelper.getInstance(mContext).getWritableDatabase();
		
		StringBuilder sql = new StringBuilder();
		for(String i : columns) {
			sql.append(","+i);
		}
		Cursor rs = db.rawQuery("SELECT ROWID"+sql.toString()+" FROM "+tableName, new String[]{});
		while(rs.moveToNext()) {
			out.append("\t<"+tableName+">\n");
			int idx = 0;
			out.append("\t\t<ROWID>" + rs.getString(idx++) + "</ROWID>\n");
			for(String column : columns) {
				out.append("\t\t<"+column+"><![CDATA[" + rs.getString(idx++) + "]]></"+column+">\n");
			}
			out.append("\t</"+tableName+">\n");
		}
		rs.close();
		
		out.append("</document>\n");
		
		ZipEntry entry = new ZipEntry(tableName+".xml");
		mZip.putNextEntry(entry);
		mZip.write(out.toString().getBytes());
		mZip.closeEntry();
	}

}
