package com.jwdroid.export;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.jwdroid.AppDbOpenHelper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Importer {
	
	private Context mContext;
	private InputStream mInput, mEntryInput;
	
	public Importer(Context context, InputStream is) {
		mContext = context;
		mInput = is;
	}
	
	public void run() throws IOException, Exception {
		Pattern p = Pattern.compile("^(.*)\\.xml$");
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(mInput));
		try {
			
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        byte[] buffer = new byte[1024];
		        int count;
		        while ((count = zis.read(buffer)) != -1) {
		            baos.write(buffer, 0, count);
		        }
		        
		        mEntryInput = new ByteArrayInputStream(baos.toByteArray());
		        
		        String filename = entry.getName();
				Matcher m = p.matcher(filename);
				
				if(m.matches()) {
					
					if(m.group(1).equals("visit"))
						importTable(m.group(1), new String[]{"territory_id","door_id","person_id","date","desc","type","calc_auto","brochures","books","magazines","tracts"});
					
					if(m.group(1).equals("door")) {
						importTable(m.group(1), new String[]{"territory_id", "group_id", "col", "row", "name", "color1", "color2", "visits_num", "last_date", "last_person_name", "last_desc", "last_person_reject", "order_num", "manual_color", "last_modified_date"});
					}
					
					if(m.group(1).equals("person"))
						importTable(m.group(1), new String[]{"door_id","name","reject"});
					
					if(m.group(1).equals("territory"))
						importTable(m.group(1), new String[]{"name","notes","created","started","finished","modified"});
					
					if(m.group(1).equals("session"))
						importTable(m.group(1), new String[]{"date","minutes","books","brochures","magazines","tracts","returns","desc"});										
				}
				
				mEntryInput.close();
				baos.close();
			}
		}
		finally {
			zis.close();
		}
	}
	
	public void importTable(String tableName, String[] columns) throws Exception {
		List<Map<String,String>> list = XMLParser.parse(tableName, mEntryInput);

        Log.i("Importer","import "+tableName);
		
		if(list == null) {
            throw new Exception("Could not parse "+tableName+" XML file");
        }
		
		SQLiteDatabase db = AppDbOpenHelper.getInstance(mContext).getWritableDatabase();

        db.execSQL("DELETE FROM `"+tableName+"`");
		
		for(Map<String,String> item : list) {
			StringBuilder sql1 = new StringBuilder(), sql2 = new StringBuilder();
			List<String> values = new ArrayList<String>();
			values.add(item.get("ROWID"));

			for(String column : columns) {				
				sql1.append(","+column);
				sql2.append(",?");
				
				if(item.get(column).equals("null"))
					values.add(null);
				else
					values.add(item.get(column));
            }
            db.execSQL("INSERT INTO `" + tableName + "` (ROWID" + sql1.toString() + ") VALUES(?" + sql2.toString() + ")", values.toArray());
		}
		
	}

}
