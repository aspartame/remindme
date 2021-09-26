package com.aspartame.RemindMe;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

public class ReminderService extends Service{
	public static final String DEBUG_TAG = "ReminderService";
	public static NotificationManager nm = null;
	private static final long INTERVAL_MONTHLY = -1l;
	private SharedPreferences preferences;
	private DBadapter dbAdapter;

	@Override
	public void onCreate() {}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		dbAdapter = new DBadapter(this);
		dbAdapter.open();
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

		String soundUri = intent.getStringExtra("soundUri");
		boolean vibrate = intent.getBooleanExtra("vibrate", true);
		String desc = intent.getStringExtra("desc");
		long time = intent.getLongExtra("time", 0l);
		String type = intent.getStringExtra("type");
		boolean isRepeat = intent.getBooleanExtra("isRepeat", false);

		if (isRepeat) { 
			nm.cancel( (int) time ); 
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);

		String timeStr = DateParser.parseTime(hour, minute, is24HourClock);

		int icon = R.drawable.advertisment_16;
		String notificationText = desc.equals("") ? "Reminder expired at " + timeStr 
				: "Reminder: \"" + desc + "\" expired at " + timeStr;
		long when = System.currentTimeMillis();

		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, notificationText, when);

		// Convert long time to a positive int to use for identification
		int notificationId = (int) (time & 0x7FFFFFFF);

		Intent i = new Intent(this, RemindMe.class);
		i.putExtra("notificationId", notificationId);
		i.putExtra("type", type);
		i.putExtra("soundUri", soundUri);
		i.putExtra("vibrate", vibrate);
		i.putExtra("desc", desc);
		i.putExtra("time", time);
		i.putExtra("isRepeat", isRepeat);
		i.setData(Uri.parse("foo" + time));
		PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);

		// Set notification information
		String title = "Today at " + timeStr;
		notification.setLatestEventInfo(this, title, desc, pIntent);

		// Add vibration to notification
		if (vibrate) { notification.defaults |= Notification.DEFAULT_VIBRATE; }

		// Add sound to notification
		if ( soundUri.equals("default") ) {
			notification.sound = Settings.System.DEFAULT_NOTIFICATION_URI;
		} else if ( !soundUri.equals("") ) {
			notification.sound = Uri.parse(soundUri);
		}
		Log.d("AlertService", "soundUri: " + soundUri);

		// Add flashing of LED to notification
		notification.ledARGB = Color.YELLOW;
		notification.ledOffMS = 1;
		notification.ledOnMS = 1;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		nm.notify(notificationId, notification);

		Log.d(DEBUG_TAG, "in ReminderService, notificationId: " + notificationId);

		// Set repeating notification if set in preferences
		String repeatIntervalString = preferences.getString(getResources().getString(R.string.preferences_repeat), "0");
		int repeatInterval = Integer.parseInt(repeatIntervalString);

		if (repeatInterval > 0) {
			PendingIntent repeatIntent = newPendingIntent(time, desc, "single");

			// Intended use
			long repeatTime = System.currentTimeMillis() + (repeatInterval * 60 * 1000);
			am.set(AlarmManager.RTC_WAKEUP, repeatTime, repeatIntent);
		}

		if (type.equals("periodic")) {
			long interval = dbAdapter.getPeriodicReminderInterval(time);
			int weekdays = dbAdapter.getPeriodicReminderWeekdays(time);
			dbAdapter.removePeriodicReminder(time);
			long newTime = 0l;
			
			if (weekdays > 0) {
				// TODO: set new time for next valid day
			}
			else if (interval != INTERVAL_MONTHLY) {
				newTime = cal.getTimeInMillis() + interval;
			} else {
				Log.d(DEBUG_TAG, "Calculating new time");
				newTime = getNewTime(time);
				Log.d(DEBUG_TAG, "Calculated new time");
			}

			/* Check if a reminder with the same time already exists in the db.
			 * If so, add 1 ms to the time and try again. */
			while ( dbAdapter.periodicReminderExists(newTime) ) {
				Log.d(DEBUG_TAG, "in the loop: " + newTime);
				++newTime;
			}

			dbAdapter.insertPeriodicReminder(newTime, desc, interval, weekdays);

			PendingIntent newIntent = newPendingIntent(newTime, desc, "periodic");
			// Intended use
			am.set(AlarmManager.RTC_WAKEUP, newTime, newIntent);
		}

		onDestroy();
	}

	private PendingIntent newPendingIntent(long time, String desc, String type) {
		Log.d(DEBUG_TAG, "Type: " + type);
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

	private long getNewTime(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);

		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);

		if ( (month == Calendar.JANUARY) && (day >= 29) ) {
			day = isLeapYear(year) ? 29 : 28;
			++month;
		} else if ( (day == 31) && ( (month == Calendar.MARCH) || 
			    					 (month == Calendar.MAY) ||
			    					 (month == Calendar.AUGUST) ||
			    					 (month == Calendar.OCTOBER) ) ) {
			day = 30;
			++month;
		} else if (month == Calendar.DECEMBER) {
			++month;
			++year;
		} else {
			++month;
		}

		calendar.set(year, month, day, hour, minute);
		return calendar.getTimeInMillis();
	}

	/*	1) If a year is divisible by 4 it is a
	 *     leap year if #2 does not apply.
	 *  2) If a year is divisible by 100 it is 
	 *     not a leap year unless #3 applies.
	 *  3) If a year is divisible by 400 it is
	 *     a leap year.
	 */
	private boolean isLeapYear(int year) {
		if (year % 4 == 0) {
			if ( (year % 100 == 0) && (year % 400 != 0) ) { return false; }
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy() {
		if ( (ReminderReceiver.wakeLock != null) && (ReminderReceiver.keyLock != null) ) {
			ReminderReceiver.wakeLock.release();
			ReminderReceiver.keyLock.reenableKeyguard();
		}
		dbAdapter.close();
	}
}
