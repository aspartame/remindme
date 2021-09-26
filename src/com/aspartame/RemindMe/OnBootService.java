package com.aspartame.RemindMe;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnBootService extends Service {
	public static final String DEBUG_TAG = "OnBootService";
	private ArrayList<Reminder> activeReminders = new ArrayList<Reminder>();
	private DBadapter dbAdapter;
	private SharedPreferences preferences;
	private String desc, type;
	private long time;
	
	@Override
	public void onCreate() {
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		dbAdapter = new DBadapter(this);
		dbAdapter.open();
		
		Log.d(DEBUG_TAG, "OnBootService.onStart()");
		
		populateActiveAlertsList();
		Log.d(DEBUG_TAG, "activeReminders.size(): " + activeReminders.size());
		
		for (int i = 0; i < activeReminders.size(); i++) {
			time = activeReminders.get(i).getTime();
			desc = activeReminders.get(i).getDesc();
			type = (activeReminders.get(i).getInterval() == 0) ? "single" : "periodic";
			
			PendingIntent pIntent = newPendingIntent(time, desc, type);
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, time, pIntent);
			Log.d(DEBUG_TAG, "Alarm set for " + time);
		}
		
		onDestroy();
	}
	
	private PendingIntent newPendingIntent(long time, String desc, String type) {
		String actionType = (type.equals("single")) ? ReminderReceiver.ACTION_SINGLE_REMINDER : ReminderReceiver.ACTION_PERIODIC_REMINDER;
		Intent intent = new Intent(actionType);
		
		// Append some pseudo-random data, this is a hack to be able to schedule 2+ alarms
		intent.setDataAndType(Uri.parse("foo" + time), "com.aspartame.RemindMe/foo_type");
		
		Log.d(DEBUG_TAG, intent.getDataString());
		
		// Append useful data
		String soundUri = preferences.getString(getResources().getString(R.string.preferences_notification_sound), "default");
		boolean vibrate = preferences.getBoolean(getResources().getString(R.string.preferences_vibration), true);

		intent.putExtra("soundUri", soundUri);
		intent.putExtra("vibrate", vibrate);
		intent.putExtra("desc", desc);
		intent.putExtra("time", time);
		intent.putExtra("isRepeat", true);

		PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pIntent;
	}
	
	private void populateActiveAlertsList() {
		Cursor cursor = dbAdapter.getAllSingleRemindersCursor();
		cursor.requery();
		activeReminders.clear();

		/* Add all single reminders */
		if (cursor.moveToFirst()) {
			do {
				String desc = cursor.getString(DBadapter.DESCRIPTION_COLUMN);
				long time = cursor.getLong(DBadapter.TIME_COLUMN);

				String dateStr = getDateStr(time);
				String timeStr = getTimeStr(time);

				Reminder reminder = new Reminder(desc, time, 0, dateStr, timeStr, 0);
				activeReminders.add(reminder);
			} while (cursor.moveToNext());
		}

		/* Add all periodic reminders */
		cursor = dbAdapter.getAllPeriodicRemindersCursor();
		cursor.requery();

		if (cursor.moveToFirst()) {
			do {
				String desc = cursor.getString(DBadapter.DESCRIPTION_COLUMN);
				long time = cursor.getLong(DBadapter.TIME_COLUMN);
				long interval = cursor.getLong(DBadapter.INTERVAL_COLUMN);
				int weekdays = cursor.getInt(DBadapter.WEEKDAYS_COLUMN);

				String dateStr = getDateStr(time);
				String timeStr = getTimeStr(time);

				Reminder reminder = new Reminder(desc, time, interval, dateStr, timeStr, weekdays);
				activeReminders.add(reminder);
			} while (cursor.moveToNext());
		}
		
		cursor.close();
	}
	
	private String getDateStr(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);

		return DateParser.formatDate(cal);
	}

	private String getTimeStr(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);

		return DateParser.parseTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);
	}
	
	
	public void onDestroy() {
		if (ReminderReceiver.wakeLock != null) {
			ReminderReceiver.wakeLock.release();
		}
		
		dbAdapter.close();
	}
}
