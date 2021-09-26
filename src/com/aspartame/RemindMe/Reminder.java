package com.aspartame.RemindMe;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Reminder {
	private String desc, dateStr, timeStr;
	private long time;
	private long interval = 0;
	private int weekdays = 0;
	
	public Reminder(String desc, long time, long interval, String dateStr, String timeStr, int weekdays) {
		this.time = time;
		this.desc = desc;
		this.interval = interval;	
		this.dateStr = dateStr;
		this.timeStr = timeStr;
		this.weekdays = weekdays;
	}

	public long getTime() { return time; }
	
	public String getDesc() { return desc; }

	public void setInterval(long interval) { this.interval = interval; }
	public long getInterval() { return interval; }	
	
	public void setDateStr(String dateStr) { this.dateStr = dateStr; }
	public String getDateStr() { return dateStr; }
	
	public void setTimeStr(String timeStr) { this.timeStr = timeStr; }
	public String getTimeStr() { return timeStr; }
	
	public void setWeekdays(int weekdays) { this.weekdays = weekdays; }
	public int getWeekdays() { return weekdays; }
	
}
