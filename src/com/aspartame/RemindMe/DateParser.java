package com.aspartame.RemindMe;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlarmManager;
import android.util.Log;

public class DateParser {
	private static final String DEBUG_TAG = "DateParser";

	/*
	 * Constants used in calculation of selected weekdays. 
	 * The weekdays variable is bitmasked with these constants to 
	 * determine which days have been selected.
	 */
	public static final int MONDAY = 0x1;
	public static final int TUESDAY = 0x2;
	public static final int WEDNESDAY = 0x4;
	public static final int THURSDAY = 0x8;
	public static final int FRIDAY = 0x10;
	public static final int SATURDAY = 0x20;
	public static final int SUNDAY = 0x40;

	public static String formatDate(Calendar c) {
		int m = c.get(Calendar.MONTH);
		String month = getStrMonth(m);

		return month + " " + c.get(Calendar.DAY_OF_MONTH) + ", " + c.get(Calendar.YEAR);
	}

	public static String formatDate(int y, int m, int d) {
		String month = getStrMonth(m);
		String day = (d < 10) ? "0" + d : Integer.toString(d);

		return month + " " + day + ", " + y;
	}

	private static String getStrMonth(int m) {
		String month = "";

		switch (m) {
		case Calendar.JANUARY:		month = "Jan";	break;
		case Calendar.FEBRUARY:		month = "Feb";	break;
		case Calendar.MARCH:		month = "Mar";	break;
		case Calendar.APRIL:		month = "Apr";	break;
		case Calendar.MAY:			month = "May";	break;
		case Calendar.JUNE:			month = "Jun";	break;
		case Calendar.JULY:			month = "Jul";	break;
		case Calendar.AUGUST:		month = "Aug";	break;
		case Calendar.SEPTEMBER:	month = "Sep";	break;
		case Calendar.OCTOBER:		month = "Oct";	break;
		case Calendar.NOVEMBER:		month = "Nov";	break;
		case Calendar.DECEMBER:		month = "Dec";	break;
		}

		return month;
	}

	public static String parseTime(int hour, int minute, boolean is24HourClock) {
		String result = "";
		String strMinute = (minute < 10) ? "0" + minute : Integer.toString(minute);

		if ( is24HourClock) {
			String strHour = (hour < 10) ? "0" + hour : Integer.toString(hour);
			result = strHour + ":" + strMinute;
		} else {
			String amPm = (hour < 12) ? "AM" : "PM";
			String strHour = ( (hour > 12) || (hour == 0) ) ? Integer.toString( (hour + 12) % 24 ) : Integer.toString(hour); 
			result = strHour + ":" + strMinute + " " + amPm;
		}

		return result;
	}

	public static String parseTimer(int year, int month, int day, int hour, int min) {
		int [] time = getDiff(year, month, day, hour, min); 
		return parseTimer(time[0], time[1]);
	}

	public static int[] parseIntTimer(int year, int month, int day, int hour, int min) {
		return getDiff(year, month, day, hour, min);
	}

	private static int[] getDiff(int year, int month, int day, int hour, int min) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, hour, min);

		long current = System.currentTimeMillis();
		long millis = cal.getTimeInMillis();
		double diff = millis - current;

		int diffMinutes = (int) Math.ceil( (diff / (1000 * 60)) );
		int hours = (int) (diffMinutes / 60);
		int minutes = diffMinutes % 60;

		return new int[] { hours, minutes };
	}

	public static String parseTimer(int h, int m) {
		String result = "";

		if (h <= 0) {
			if (m <= 0) { result = "Set timer"; } 
			else { result = m + " min"; }
		} else if (h == 1) {
			if (m <= 0) { result = "1 h"; }
			else { result = "1 h, " + m + " min"; }
		} else {
			if (m <= 0) { result = h + " h"; }
			else { result = h + " h, " + m + " min"; }
		}

		return result;
	}

	public static int[] parseIntInterval(long interval) {
		int diffHours = (int) (interval / AlarmManager.INTERVAL_HOUR);
		int days = (diffHours / 24);
		int hours = (diffHours % 24);

		return new int[] { days, hours };
	}

	public static String parseInterval(long interval) {
		Log.d(DEBUG_TAG, "interval: " + interval);
		String result = "";

		if (interval == AlarmManager.INTERVAL_DAY) { return "Daily"; }
		else if (interval == (AlarmManager.INTERVAL_DAY * 7)) { return "Weekly"; }
		else if (interval == PeriodicReminderActivity.INTERVAL_MONTHLY) { return "Monthly"; }
		else if (interval == PeriodicReminderActivity.INTERVAL_WEEKDAYS) { return "Weekdays"; }
		else {
			int diffHours = (int) (interval / AlarmManager.INTERVAL_HOUR);
			int days = (diffHours / 24);
			int hours = (diffHours % 24);

			if (days == 0) {
				if (hours == 0) { result = "Set interval"; }
				else if (hours == 1) { result = hours + " hour"; }
				else { result = hours + " hours"; }
			} else if (days == 1) {
				if (hours == 0) { result = days + " day"; }
				else if (hours == 1) { result = days + " day, " + hours + " hour"; }
				else { result = days + " day, " + hours + " hours"; }
			} else {
				if (hours == 0) { result = days + " days"; }
				else if (hours == 1) { result = days + " days, " + hours + "hour"; }
				else { result = days + " days, " + hours + " hours"; }
			}
		}

		return result;
	}

	// TODO: Implement
	public static String parseWeekdays(int weekdays) {
		String res = "";

		ArrayList<String> days = new ArrayList<String>();

		if ((weekdays & MONDAY) == 0x1) { days.add("Mon"); }
		if ((weekdays & TUESDAY) == 0x2) { days.add("Tue"); }
		if ((weekdays & WEDNESDAY) == 0x4) { days.add("Wed"); }
		if ((weekdays & THURSDAY) == 0x8) { days.add("Thu"); }
		if ((weekdays & FRIDAY) == 0x10) { days.add("Fri"); }
		if ((weekdays & SATURDAY) == 0x20) { days.add("Sat"); }
		if ((weekdays & SUNDAY) == 0x40) { days.add("Sun"); }

		if (days.size() == 0) { return "Specific weekdays"; }
		else if (days.size() == 1) { res = days.get(0); }
		else {
			int i = 0;
			while (i < (days.size() - 1)) {
				res += days.get(i) + ", ";
				i++;
			}
			
			res += days.get(i);
		}
		
		return res;
	}
}











