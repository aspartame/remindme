package com.aspartame.RemindMe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DBadapter {
	private static final String DEBUG_TAG = "DBadapter";
	private static final String DATABASE_NAME = "RemindMe20.db";
	private static final String TABLE_SINGLE_REMINDER = "singleReminder";
	private static final String TABLE_PERIODIC_REMINDER = "periodicReminder";
	private static final int DATABASE_VERSION = 2;
	public static final int SINGLE_REMINDER = 0;
	public static final int PERIODIC_REMINDER = 1;
	private SQLiteDatabase db;
	private final Context context;
	
	public static final String KEY_ID = "_id";

	public static final String KEY_TIME = "time";
	public static final int TIME_COLUMN = 1;
	
	public static final String KEY_DESCRIPTION = "description";
	public static final int DESCRIPTION_COLUMN = 2;
	
	public static final String KEY_INTERVAL = "interval";
	public static final int INTERVAL_COLUMN = 3;
	
	public static final String KEY_WEEKDAYS = "weekdays";
	public static final int WEEKDAYS_COLUMN = 4;
	

	
	private DBOpenHelper dbHelper;
	
	public DBadapter(Context context) {
		this.context = context;
		dbHelper = new DBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void open() throws SQLiteException {
		try { db = dbHelper.getWritableDatabase(); }
		catch (SQLiteException e) { db = dbHelper.getReadableDatabase(); }
	}
	
	public void close() {
		db.close();
	}
	
	public long insertSingleReminder(Reminder singleReminder) {
		
		ContentValues cv = new ContentValues();
		cv.put(KEY_TIME, singleReminder.getTime());
		cv.put(KEY_DESCRIPTION, singleReminder.getDesc());
		return db.insert(TABLE_SINGLE_REMINDER, null, cv);
	}
	
	public long insertSingleReminder(long newTime, String desc) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_TIME, newTime);
		cv.put(KEY_DESCRIPTION, desc);
		return db.insert(TABLE_SINGLE_REMINDER, null, cv);
	}
	
	public boolean singleReminderExists(long time) {
		Cursor cursor = db.query(TABLE_SINGLE_REMINDER, new String[] { KEY_TIME }, KEY_TIME + "=" + time, null, null, null, null);
		boolean exists = (cursor.getCount() > 0) ? true : false;
		cursor.close();
		
		return exists;
	}
	
	public int removeSingleReminder(long time) {
		return db.delete(TABLE_SINGLE_REMINDER, KEY_TIME + "=" + time , null);
	}
	
	// Not used, might also be wrong
	public String getReminderDescription(long time, int type) {
		String desc = "";
		String table = (type == SINGLE_REMINDER) ? TABLE_SINGLE_REMINDER : TABLE_PERIODIC_REMINDER;
		
		Cursor cursor = db.query(table, new String[] { KEY_DESCRIPTION }, KEY_TIME + "=" + time, null, null, null, null);
		
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			desc = cursor.getString(DESCRIPTION_COLUMN);
			Log.d(DEBUG_TAG, "getSingleReminderDescription returns: " + desc);
		} else {
			Log.d(DEBUG_TAG, "Error: getSingleReminderDescription returned more than one row");
		}
		
		cursor.close();
		
		return desc;
	}

	public Cursor getAllSingleRemindersCursor() {
		return db.query(TABLE_SINGLE_REMINDER, new String[] { KEY_ID, KEY_TIME, KEY_DESCRIPTION }, 
										null, null, null, null, KEY_TIME);
	}
	/*
	public long insertPeriodicReminder(Reminder periodicReminder) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_TIME, periodicReminder.getTime());
		cv.put(KEY_DESCRIPTION, periodicReminder.getDesc());
		cv.put(KEY_INTERVAL, periodicReminder.getInterval());
		return db.insert(TABLE_PERIODIC_REMINDER, null, cv);
	}
	*/
	public long insertPeriodicReminder(long newTime, String desc, long interval, int weekdays) {
		ContentValues cv = new ContentValues();
		cv.put(KEY_TIME, newTime);
		cv.put(KEY_DESCRIPTION, desc);
		cv.put(KEY_INTERVAL, interval);
		cv.put(KEY_WEEKDAYS, weekdays);
		return db.insert(TABLE_PERIODIC_REMINDER, null, cv);
	}
	
	public boolean periodicReminderExists(long time) {
		Cursor cursor = db.query(TABLE_PERIODIC_REMINDER, new String[] { KEY_TIME }, KEY_TIME + "=" + time, null, null, null, null);
		boolean exists = (cursor.getCount() > 0) ? true : false;
		cursor.close();
		
		return exists;
	}
	
	public int removePeriodicReminder(long time) {
		return db.delete(TABLE_PERIODIC_REMINDER, KEY_TIME + "=" + time , null);
	}
	
	public long getPeriodicReminderInterval(long time) {
		Cursor cursor = db.query(TABLE_PERIODIC_REMINDER, new String[] { KEY_INTERVAL }, KEY_TIME + "=" + time, null, null, null, null);
		
		long interval = 0l;
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			interval = cursor.getLong(0);
		} else {
			Log.d(DEBUG_TAG, "Error: getPeriodicReminderInterval returned more than one row");
		}
		
		cursor.close();
		
		return interval;
	}
	
	public int getPeriodicReminderWeekdays(long time) {
		Cursor cursor = db.query(TABLE_PERIODIC_REMINDER, new String[] { KEY_WEEKDAYS }, KEY_TIME + "=" + time, null, null, null, null);
		
		int weekdays = 0;
		if (cursor.getCount() == 1) {
			cursor.moveToFirst();
			weekdays = cursor.getInt(0);
		} else {
			Log.d(DEBUG_TAG, "Error: getPeriodicReminderInterval returned more than one row");
		}
		
		cursor.close();
		
		return weekdays;
	}
	
	public Cursor getAllPeriodicRemindersCursor() {
		return db.query(TABLE_PERIODIC_REMINDER, new String[] { KEY_ID, KEY_TIME, KEY_DESCRIPTION, KEY_INTERVAL, KEY_WEEKDAYS }, 
										null, null, null, null, KEY_TIME);
	}
	
	private static class DBOpenHelper extends SQLiteOpenHelper {
		public DBOpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		// SQL-statements for creating tables
		private static final String CREATE_TABLE_SINGLE_REMINDER = "create table " + TABLE_SINGLE_REMINDER + 
										" (" + KEY_ID  + " integer primary key autoincrement, " + KEY_TIME + 
										" long, " + KEY_DESCRIPTION + " text not null);";
		
		private static final String CREATE_TABLE_PERIODIC_REMINDER = "create table " + TABLE_PERIODIC_REMINDER + 
										" (" + KEY_ID  + " integer primary key autoincrement, " + KEY_TIME + 
										" long, " + KEY_DESCRIPTION + " text not null, " + 
										KEY_INTERVAL + " long, " + KEY_WEEKDAYS + " integer);";
		
		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(CREATE_TABLE_SINGLE_REMINDER);
			_db.execSQL(CREATE_TABLE_PERIODIC_REMINDER);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion, int _newVersion) {
			if ( (_oldVersion == 1) && (_newVersion == 2) ) {
				final String ALTER_TABLE_WEEKDAYS_REMINDER = "alter table " + TABLE_PERIODIC_REMINDER + 
								" add column " + KEY_WEEKDAYS + " integer;";
				
				_db.execSQL(ALTER_TABLE_WEEKDAYS_REMINDER);
			}
		}
	}
}






