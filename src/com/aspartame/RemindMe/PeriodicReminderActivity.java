package com.aspartame.RemindMe;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PeriodicReminderActivity extends Activity {
	public static final long INTERVAL_MONTHLY = -1l;
	public static final long INTERVAL_WEEKDAYS = -2l;
	private static final String DEBUG_TAG = "PeriodicReminderActivity";
	private long interval = 0;
	private int weekdays = 0;
	private SharedPreferences preferences;
	private DBadapter dbAdapter;
	private Context context;
	AlarmManager am;

	public void onCreate(Bundle savedInstanceState) {
		Log.d(DEBUG_TAG, "Starting PeriodicReminderActivity");
		super.onCreate(savedInstanceState);

		context = getApplicationContext();
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		dbAdapter = new DBadapter(this);
		dbAdapter.open();

		long time = getIntent().getLongExtra("time", 0l);
		String desc = getIntent().getStringExtra("desc");
		interval = getIntent().getLongExtra("interval", 0l);
		weekdays = getIntent().getIntExtra("weekdays", 0);

		if (time > 0) { this.setTitle("Edit periodic reminder"); }

		createReminder(time, desc, interval);
	}

	private void createReminder(final long oldTime, String desc, long interval) {
		Log.d(DEBUG_TAG, "Starting createReminder");

		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View createView = li.inflate(R.layout.new_periodic_reminder, null);

		/* Create a calendar and set its values */
		final Calendar cal = Calendar.getInstance();

		if (oldTime > 0 ) { cal.setTimeInMillis(oldTime); } 
		else { cal.set(Calendar.SECOND, 0); }

		/* Set date, time and description fields */
		final boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);

		final TextView dateTextView = (TextView) createView.findViewById(R.id.new_periodic_reminder_date_label);
		final TextView timeTextView = (TextView) createView.findViewById(R.id.new_periodic_reminder_time_label);
		final TextView intervalTextView = (TextView) createView.findViewById(R.id.new_periodic_reminder_interval_label);
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);

		String dateLabel = DateParser.formatDate(cal);
		String timeLabel = DateParser.parseTime(hour, minute, is24HourClock);

		String intervalLabel = "Set interval";

		if (weekdays > 0) {
			intervalLabel = DateParser.parseWeekdays(weekdays);
		} else if (interval > 0) {
			intervalLabel = DateParser.parseInterval(interval);
		}

		dateTextView.setText(dateLabel);
		timeTextView.setText(timeLabel);
		intervalTextView.setText(intervalLabel);

		EditText descView = (EditText) createView.findViewById(R.id.new_periodic_reminder_description);
		descView.setText(desc);

		/* Handle result from date- and time picker dialogs */
		final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int mYear, int mMonth, int mDay) {
				cal.set(mYear, mMonth, mDay);
				String date = DateParser.formatDate(mYear, mMonth, mDay);
				dateTextView.setText(date);
			}
		};

		final TimePickerDialog.OnTimeSetListener timeSetListener =
			new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int mHour, int mMinute) {	
				cal.set(Calendar.HOUR_OF_DAY, mHour);
				cal.set(Calendar.MINUTE, mMinute);
				String formattedTime = DateParser.parseTime(mHour, mMinute, is24HourClock);
				timeTextView.setText(formattedTime);
			}
		};

		// Set onClickListener for Date view
		View dateView = (View) createView.findViewById(R.id.new_periodic_reminder_date_view);
		dateView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				DatePickerDialog dateDialog = new DatePickerDialog(PeriodicReminderActivity.this, dateSetListener, 
						cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

				dateDialog.setTitle("Choose date");
				
				Window window = dateDialog.getWindow();
				window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
								WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
				
				dateDialog.show();
			}
		});

		// Set onClickListener for Time view
		View timeView = (View) createView.findViewById(R.id.new_periodic_reminder_time_view);
		timeView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				TimePickerDialog timeDialog = new TimePickerDialog(PeriodicReminderActivity.this, timeSetListener, 
						cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);

				Window window = timeDialog.getWindow();
				window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
								WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
				
				timeDialog.setTitle("Choose time");
				timeDialog.show();
			}
		});

		// Set onClickListener for Interval view
		View intervalView = (View) createView.findViewById(R.id.new_periodic_reminder_interval_view);
		intervalView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showIntervalPickerDialog(intervalTextView);
			}
		});

		// Set onClickListener for create alert button
		Button submitButton = (Button) createView.findViewById(R.id.new_periodic_reminder_submit_button);
		submitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Log.d(DEBUG_TAG, "Clicked Set reminder");
				checkIntervalSelected(createView, cal, oldTime);
				
			}
		});

		// Set onClickListener for cancel button
		Button cancelButton = (Button) createView.findViewById(R.id.new_periodic_reminder_cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Log.d(DEBUG_TAG, "Cancelling new single reminder");
				setResult(RESULT_CANCELED);
				finish();
			}
		});


		setContentView(createView);
	}
	
	private void checkIntervalSelected(View createView, Calendar cal, long oldTime) {
		if ( (interval == 0) || (interval == INTERVAL_WEEKDAYS && weekdays == 0) ) { showNoIntervalDialog(); }
		else {
			EditText descView = (EditText) createView.findViewById(R.id.new_periodic_reminder_description);
			String desc = descView.getText().toString();
			registerReminder(cal, desc, oldTime, interval);

			Log.d(DEBUG_TAG, "Reminder added to database");
			setResult(RESULT_OK);
			finish();
		}
	}
	
	private void showNoIntervalDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		
		ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {}
		});
		
		final AlertDialog warningDialog = ad.create();
		
		Window window = warningDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		warningDialog.setIcon(R.drawable.warning_32);
		warningDialog.setTitle("No interval set");
		warningDialog.setMessage("You must set an interval for the periodic reminder");
		warningDialog.show();
	}

	private void showIntervalPickerDialog(final TextView intervalTextView) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);

		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View intervalView = li.inflate(R.layout.set_interval_view, null);

		final RadioGroup radioGroup = (RadioGroup) intervalView.findViewById(R.id.set_interval_radiogroup);
		final RadioButton weekdaysButton = (RadioButton) intervalView.findViewById(R.id.set_interval_option_weekdays);
		final RadioButton customButton = (RadioButton) intervalView.findViewById(R.id.set_interval_option_custom);

		// Set the correct radio button to "checked" if an interval is given
		if (interval != 0) {
			String intervalStr = DateParser.parseInterval(interval);
			int id = 0;

			if (intervalStr.equals("Daily")) { id = R.id.set_interval_option_daily; }
			else if (intervalStr.equals("Weekly")) { id = R.id.set_interval_option_weekly; }
			else if (intervalStr.equals("Monthly")) { id = R.id.set_interval_option_monthly; }
			else if (intervalStr.equals("Weekdays")) { 
				id = R.id.set_interval_option_weekdays; 
				weekdaysButton.setText(DateParser.parseWeekdays(weekdays));
			}
			else { 
				id = R.id.set_interval_option_custom;
				customButton.setText(DateParser.parseInterval(interval));
			}

			RadioButton checkedButton = (RadioButton) intervalView.findViewById(id);
			checkedButton.setChecked(true);
		}
		
		// Set onClickListener for weekdays option and open weekdays view
		weekdaysButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showWeekdaysDialog(weekdaysButton);
			}
		});		

		// Set onClickListener for custom option and open custom view
		customButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showCustomIntervalDialog(customButton);
			}
		});

		// Set onClickListener for set button
		ad.setPositiveButton("Set", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				int choice = radioGroup.getCheckedRadioButtonId();

				setInterval(choice);
				
				String text = "";
				
				if (weekdays > 0) { text = DateParser.parseWeekdays(weekdays); }
				else { text = DateParser.parseInterval(interval); }
				
				intervalTextView.setText(text);
			}
		});

		// Set onClickListener for cancel button
		ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {

			}
		});

		final AlertDialog intervalDialog = ad.create();
		
		Window window = intervalDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		intervalDialog.setIcon(R.drawable.timer2_32);
		intervalDialog.setTitle("Set interval");
		intervalDialog.setView(intervalView);
		intervalDialog.show();
	}

	private void showWeekdaysDialog(final RadioButton weekdaysButton) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		
		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View weekdaysView = li.inflate(R.layout.weekdays_interval_view, null);
		
		final CheckBox monday = (CheckBox) weekdaysView.findViewById(R.id.monday);
		final CheckBox tuesday = (CheckBox) weekdaysView.findViewById(R.id.tuesday);
		final CheckBox wednesday = (CheckBox) weekdaysView.findViewById(R.id.wednesday);
		final CheckBox thursday = (CheckBox) weekdaysView.findViewById(R.id.thursday);
		final CheckBox friday = (CheckBox) weekdaysView.findViewById(R.id.friday);
		final CheckBox saturday = (CheckBox) weekdaysView.findViewById(R.id.saturday);
		final CheckBox sunday = (CheckBox) weekdaysView.findViewById(R.id.sunday);
		
		if (weekdays != 0) {
			if ((weekdays & DateParser.MONDAY) == 0x1) { monday.setChecked(true); }
			if ((weekdays & DateParser.TUESDAY) == 0x2) { tuesday.setChecked(true); }
			if ((weekdays & DateParser.WEDNESDAY) == 0x4) { wednesday.setChecked(true); }
			if ((weekdays & DateParser.THURSDAY) == 0x8) { thursday.setChecked(true); }
			if ((weekdays & DateParser.FRIDAY) == 0x10) { friday.setChecked(true); }
			if ((weekdays & DateParser.SATURDAY) == 0x20) { saturday.setChecked(true); }
			if ((weekdays & DateParser.SUNDAY) == 0x40) { sunday.setChecked(true); }
		}
		
		// Set onClickListener for set button
		ad.setPositiveButton("Set", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				weekdays = 0;

				if (monday.isChecked()) { weekdays |= DateParser.MONDAY; }
				if (tuesday.isChecked()) { weekdays |= DateParser.TUESDAY; }
				if (wednesday.isChecked()) { weekdays |= DateParser.WEDNESDAY; }
				if (thursday.isChecked()) { weekdays |= DateParser.THURSDAY; }
				if (friday.isChecked()) { weekdays |= DateParser.FRIDAY; }
				if (saturday.isChecked()) { weekdays |= DateParser.SATURDAY; }
				if (sunday.isChecked()) { weekdays |= DateParser.SUNDAY; }
				
				if (Integer.bitCount(weekdays) == 0) { interval = 0; }

				Log.d(DEBUG_TAG, "weekdays: " + Integer.toBinaryString(weekdays));
				weekdaysButton.setText(DateParser.parseWeekdays(weekdays));
			}
		});

		// Set onClickListener for cancel button
		ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {}
		});

		final AlertDialog weekdaysDialog = ad.create();	
		weekdaysDialog.setIcon(R.drawable.timer2_32);
		weekdaysDialog.setTitle("Select days");
		weekdaysDialog.setView(weekdaysView);
		weekdaysDialog.show();
	}
	
	private void showCustomIntervalDialog(final RadioButton customButton) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);

		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View customView = li.inflate(R.layout.custom_interval_view, null);

		SeekBar dayBar = (SeekBar) customView.findViewById(R.id.custom_interval_day_seekbar);
		SeekBar hourBar = (SeekBar) customView.findViewById(R.id.custom_interval_hour_seekbar);

		final TextView dayValue = (TextView) customView.findViewById(R.id.custom_interval_day_label);
		final TextView hourValue = (TextView) customView.findViewById(R.id.custom_interval_hour_label);

		int[] values = DateParser.parseIntInterval(interval);

		if ( (values[0] < 0) || (values[1] < 0) ) {
			dayBar.setProgress(0);
			hourBar.setProgress(0);

			dayValue.setText(Integer.toString(0));
			hourValue.setText(Integer.toString(0));
		} else {
			dayBar.setProgress(values[0]);
			hourBar.setProgress(values[1]);

			dayValue.setText(Integer.toString(values[0]));
			hourValue.setText(Integer.toString(values[1]));
		}

		final TextView dayLabel = (TextView) customView.findViewById(R.id.custom_interval_day_label_text);
		final TextView hourLabel = (TextView) customView.findViewById(R.id.custom_interval_hour_label_text);

		String text = (values[0] != 1) ? " days" : " day";
		dayLabel.setText(text);

		text = (values[1] != 1) ? " hours" : " hour";
		hourLabel.setText(text);

		OnSeekBarChangeListener onSeekBarProgress = new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar s, int progress, boolean touch) {
				if (touch) {					
					if (s.getId() == R.id.custom_interval_day_seekbar) {
						String text = (progress != 1) ? " days" : " day";
						dayValue.setText(Integer.toString(progress));
						dayLabel.setText(text);
					}

					if (s.getId() == R.id.custom_interval_hour_seekbar) {
						String text = (progress != 1) ? " hours" : " hour";
						hourValue.setText(Integer.toString(progress));
						hourLabel.setText(text);
					}
				}
			}

			public void onStartTrackingTouch(SeekBar s) {}
			public void onStopTrackingTouch(SeekBar s) {}
		};

		dayBar.setOnSeekBarChangeListener(onSeekBarProgress);
		hourBar.setOnSeekBarChangeListener(onSeekBarProgress);

		// Set onClickListener for set button
		ad.setPositiveButton("Set", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				int days = Integer.valueOf(dayValue.getText().toString());
				int hours = Integer.valueOf(hourValue.getText().toString());

				interval = (long) ( (days * AlarmManager.INTERVAL_DAY) + (hours * AlarmManager.INTERVAL_HOUR) );
				customButton.setText(DateParser.parseInterval(interval));
			}
		});

		// Set onClickListener for cancel button
		ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {}
		});

		final AlertDialog customDialog = ad.create();	
		customDialog.setIcon(R.drawable.timer2_32);
		customDialog.setTitle("Set timer");
		customDialog.setView(customView);
		customDialog.show();

	}

	private void setInterval(int choice) {
		switch (choice) {
		case R.id.set_interval_option_daily:	interval = AlarmManager.INTERVAL_DAY;			break;
		case R.id.set_interval_option_weekly:	interval = (AlarmManager.INTERVAL_DAY * 7);		break;
		case R.id.set_interval_option_monthly:	interval = INTERVAL_MONTHLY;					break;
		case R.id.set_interval_option_weekdays:	interval = INTERVAL_WEEKDAYS;					break;
		}
	}

	private void registerReminder(Calendar cal, String desc, long oldTime, long interval) {
		am = (AlarmManager) getSystemService(ALARM_SERVICE);
		
		/* If rescheduling old reminder, remove it from database */
		if (oldTime > 0) {
			PendingIntent oldIntent = newPendingIntent(oldTime, desc);
			am.cancel(oldIntent);
			int dbResult = dbAdapter.removePeriodicReminder(oldTime);
			
			Log.d(DEBUG_TAG, "Rescheduling reminder, deleting " + dbResult + " rows from database");
		}
		
		long newTime = cal.getTimeInMillis();
		
		/* Check if a reminder with the same time already exists in the db.
		 * If so, add 1 ms to the time and try again. */
		while ( dbAdapter.periodicReminderExists(newTime) ) {
			Log.d(DEBUG_TAG, "in the loop: " + newTime);
			++newTime;
		}

		dbAdapter.insertPeriodicReminder(newTime, desc, interval, weekdays);
		
		PendingIntent newIntent = newPendingIntent(newTime, desc);
		// Intended use
		am.set(AlarmManager.RTC_WAKEUP, newTime, newIntent);
		
		// Testing/debugging purpose, set reminder to 10 seconds from now
		//am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * 60), newIntent);
		//Log.d(DEBUG_TAG, "Alarm set for 60 seconds");
		
		/* Display a toast notifying the user */ 
		boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);
		String timeStr = DateParser.parseTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);
		String dateStr = DateParser.formatDate(cal);
		String text = "";
		
		if (oldTime == 0) { text = "New periodic reminder scheduled for " + timeStr + " on " + dateStr + "."; } 
		else { text = "Periodic reminder rescheduled for " + timeStr + " on " + dateStr + ".";}

		Toast toast = Toast.makeText(PeriodicReminderActivity.this, text, Toast.LENGTH_LONG);
		toast.show();
	}
	
	private PendingIntent newPendingIntent(long time, String desc) {
		Intent intent = new Intent(ReminderReceiver.ACTION_PERIODIC_REMINDER);
		
		// Append some pseudo-random data, this is a hack to be able to schedule 2+ alarms
		intent.setDataAndType(Uri.parse("foo" + time), "com.aspartame.RemindMe/foo_type");
		
		// Append useful data
		String soundUri = preferences.getString(getResources().getString(R.string.preferences_notification_sound), "default");
		boolean vibrate = preferences.getBoolean(getResources().getString(R.string.preferences_vibration), true);

		intent.putExtra("soundUri", soundUri);
		intent.putExtra("vibrate", vibrate);
		intent.putExtra("desc", desc);
		intent.putExtra("time", time);
		intent.putExtra("isRepeat", false);

		PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pIntent;
	}

	@Override
	public void onDestroy() {
		dbAdapter.close();
		super.onDestroy();
	}
}