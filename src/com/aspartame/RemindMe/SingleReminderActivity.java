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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SingleReminderActivity extends Activity {
	private static final String DEBUG_TAG = "SingleReminderActivity";
	private SharedPreferences preferences;
	private DBadapter dbAdapter;
	private Context context;
	private AlarmManager am;
	
	public void onCreate(Bundle savedInstanceState) {
		Log.d(DEBUG_TAG, "Starting SingleReminderActivity");
		super.onCreate(savedInstanceState);
		
		context = getApplicationContext();
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		dbAdapter = new DBadapter(this);
		dbAdapter.open();
		
		long time = getIntent().getLongExtra("time", 0l);
		String desc = getIntent().getStringExtra("desc");
		
		if (time > 0) { this.setTitle("Edit reminder"); }
		
		createReminder(time, desc);
	}

	/* Create a reminder. If oldTime == RemindMe.NEW_REMINDER a new reminder is created, 
	 * otherwise the reminder identified by oldTime is rescheduled. */
	private void createReminder(final long oldTime, String desc) {
		Log.d(DEBUG_TAG, "Starting createReminder");
		
		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View createView = li.inflate(R.layout.new_single_reminder, null);
		
		/* Create a calendar and set its values */
		final Calendar cal = Calendar.getInstance();
		
		if (oldTime > 0 ) { cal.setTimeInMillis(oldTime); } 
		else { cal.set(Calendar.SECOND, 0); }
		
		/* Set date, time and description fields */
		final boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);
		
		final TextView timerTextView = (TextView) createView.findViewById(R.id.new_reminder_timer_label);
		final TextView dateTextView = (TextView) createView.findViewById(R.id.new_reminder_date_label);
		final TextView timeTextView = (TextView) createView.findViewById(R.id.new_reminder_time_label);
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);

		String dateLabel = DateParser.formatDate(cal);
		String timeLabel = DateParser.parseTime(hour, minute, is24HourClock);
		String timerLabel = "Set Timer";
		
		if (oldTime > 0) {
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			
			timerLabel = DateParser.parseTimer(year, month, day, hour, minute);
		}
		
		timerTextView.setText(timerLabel);
		dateTextView.setText(dateLabel);
		timeTextView.setText(timeLabel);
		
		EditText descView = (EditText) createView.findViewById(R.id.new_reminder_description);
		descView.setText(desc);
		
		/* Handle result from date- and time picker dialogs */
		final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int mYear, int mMonth, int mDay) {
				cal.set(mYear, mMonth, mDay);
				
				int mHour = cal.get(Calendar.HOUR_OF_DAY);
				int mMinute = cal.get(Calendar.MINUTE);

				String date = DateParser.formatDate(mYear, mMonth, mDay);
				String timerLabelText = DateParser.parseTimer(mYear, mMonth, mDay, mHour, mMinute);

				dateTextView.setText(date);
				timerTextView.setText(timerLabelText);
			}
		};

		final TimePickerDialog.OnTimeSetListener timeSetListener =
			new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int mHour, int mMinute) {
				int mYear = cal.get(Calendar.YEAR);
				int mMonth = cal.get(Calendar.MONTH);
				int mDay = cal.get(Calendar.DAY_OF_MONTH);
				
				cal.set(Calendar.HOUR_OF_DAY, mHour);
				cal.set(Calendar.MINUTE, mMinute);

				String formattedTime = DateParser.parseTime(mHour, mMinute, is24HourClock);
				String timerLabelText = DateParser.parseTimer(mYear, mMonth, mDay, mHour, mMinute);

				timeTextView.setText(formattedTime);
				timerTextView.setText(timerLabelText);
			}
		};
		
		// Set onClickListener for Timer view
		View timerView = (View) createView.findViewById(R.id.new_reminder_timer_view);
		timerView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				showTimerPickerDialog(cal, is24HourClock, dateTextView, timeTextView, timerTextView);
			}
		});

		// Set onClickListener for Date view
		View dateView = (View) createView.findViewById(R.id.new_reminder_date_view);
		dateView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// Show datePicker dialog
				DatePickerDialog dateDialog = new DatePickerDialog(SingleReminderActivity.this, dateSetListener, 
					cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
				
				Window window = dateDialog.getWindow();
				window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
								WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
				
				dateDialog.setTitle("Choose date");
				dateDialog.show();
			}
		});

		// Set onClickListener for Time view
		View timeView = (View) createView.findViewById(R.id.new_reminder_time_view);
		timeView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				TimePickerDialog timeDialog = new TimePickerDialog(SingleReminderActivity.this, timeSetListener, 
					cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);
				
				Window window = timeDialog.getWindow();
				window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
								WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
				
				timeDialog.setTitle("Choose time");
				timeDialog.show();
			}
		});
		
		// Set onClickListener for create alert button
		Button submitButton = (Button) createView.findViewById(R.id.new_reminder_submit_button);
		submitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Log.d(DEBUG_TAG, "Clicked Set reminder");
				
				EditText descView = (EditText) createView.findViewById(R.id.new_reminder_description);
				String desc = descView.getText().toString();

				registerReminder(cal, desc, oldTime);
				
				Log.d(DEBUG_TAG, "Reminder added to database");
				setResult(RESULT_OK);
				finish();
			}
		});

		// Set onClickListener for cancel button
		Button cancelButton = (Button) createView.findViewById(R.id.new_reminder_cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Log.d(DEBUG_TAG, "Cancelling new single reminder");
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		setContentView(createView);
	}
	
	private void showTimerPickerDialog(final Calendar cal, final boolean is24HourClock, final TextView dateTextView, 
			final TextView timeTextView, final TextView timerLabel) {
		
		AlertDialog.Builder ad = new AlertDialog.Builder(this);

		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		
		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View timerView = li.inflate(R.layout.timer_view, null);

		SeekBar hourBar = (SeekBar) timerView.findViewById(R.id.timer_hour_seekbar);
		SeekBar minuteBar = (SeekBar) timerView.findViewById(R.id.timer_minute_seekbar);

		int [] time = DateParser.parseIntTimer(year, month, day, hour, minute);
		final TextView hourValue = (TextView) timerView.findViewById(R.id.timer_hour_label);
		final TextView minuteValue = (TextView) timerView.findViewById(R.id.timer_minute_label);

		if ( (time[0] < 0) || (time[1] < 0) ) {
			hourBar.setProgress(0);
			minuteBar.setProgress(0);

			hourValue.setText(Integer.toString(0));
			minuteValue.setText(Integer.toString(0));
		} else {
			hourBar.setProgress(time[0]);
			minuteBar.setProgress(time[1]);

			hourValue.setText(Integer.toString(time[0]));
			minuteValue.setText(Integer.toString(time[1]));
		}

		final TextView hourLabel = (TextView) timerView.findViewById(R.id.timer_hour_label_text);
		final TextView minuteLabel = (TextView) timerView.findViewById(R.id.timer_minute_label_text);

		String text = (time[0] != 1) ? " hours" : " hour";
		hourLabel.setText(text);

		text = (time[1] != 1) ? " minutes" : " minute";
		minuteLabel.setText(text);


		OnSeekBarChangeListener onSeekBarProgress = new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar s, int progress, boolean touch) {
				if (touch) {					
					if (s.getId() == R.id.timer_hour_seekbar) {
						String text = (progress != 1) ? " hours" : " hour";
						hourValue.setText(Integer.toString(progress));
						hourLabel.setText(text);
					}

					if (s.getId() == R.id.timer_minute_seekbar) {
						String text = (progress != 1) ? " minutes" : " minute";
						minuteValue.setText(Integer.toString(progress));
						minuteLabel.setText(text);
					}
				}
			}

			public void onStartTrackingTouch(SeekBar s) {}
			public void onStopTrackingTouch(SeekBar s) {}
		};

		hourBar.setOnSeekBarChangeListener(onSeekBarProgress);
		minuteBar.setOnSeekBarChangeListener(onSeekBarProgress);


		// Set onClickListener for set button
		ad.setPositiveButton("Set", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				int timerHours = Integer.valueOf(hourValue.getText().toString());
				int timerMinutes = Integer.valueOf(minuteValue.getText().toString());

				long millis = System.currentTimeMillis() + (timerHours * 60 * 60 * 1000) + (timerMinutes * 60 * 1000);
				cal.setTimeInMillis(millis);

				String date = DateParser.formatDate(cal);
				String time = DateParser.parseTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);

				dateTextView.setText(date);
				timeTextView.setText(time);

				timerLabel.setText(DateParser.parseTimer(timerHours, timerMinutes));
			}
		});

		// Set onClickListener for cancel button
		ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				// Update the timer-label in case the time has changed, 
				// but only if a previous time has been set
				if (!timerLabel.getText().toString().equals("Set timer")) {
					int timerHours = Integer.valueOf(hourValue.getText().toString());
					int timerMinutes = Integer.valueOf(minuteValue.getText().toString());
					timerLabel.setText(DateParser.parseTimer(timerHours, timerMinutes));
				}
			}
		});

		final AlertDialog timerDialog = ad.create();
		
		Window window = timerDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		timerDialog.setIcon(R.drawable.timer2_32);
		timerDialog.setTitle("Set timer");
		timerDialog.setView(timerView);
		timerDialog.show();
	}
	
	private void registerReminder(Calendar cal, String desc, long oldTime) {
		am = (AlarmManager) getSystemService(ALARM_SERVICE);
		
		/* If rescheduling old reminder, remove it from database */
		if (oldTime > 0) {
			PendingIntent oldIntent = newPendingIntent(oldTime, desc);
			am.cancel(oldIntent);
			int dbResult = dbAdapter.removeSingleReminder(oldTime);
			
			Log.d(DEBUG_TAG, "Rescheduling reminder, deleting " + dbResult + " rows from database");
		}
		
		long newTime = cal.getTimeInMillis();
		
		/* Check if a reminder with the same time already exists in the db.
		 * If so, add 1 ms to the time and try again. */
		while ( dbAdapter.singleReminderExists(newTime) ) {
			Log.d(DEBUG_TAG, "in the loop: " + newTime);
			++newTime;
		}

		dbAdapter.insertSingleReminder(newTime, desc);
		
		PendingIntent newIntent = newPendingIntent(newTime, desc);
		// Intended use
		am.set(AlarmManager.RTC_WAKEUP, newTime, newIntent);
		
		/* Display a toast notifying the user */ 
		boolean is24HourClock = preferences.getBoolean(getResources().getString(R.string.preferences_clock_format), false);
		String timeStr = DateParser.parseTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24HourClock);
		String dateStr = DateParser.formatDate(cal);
		String text = "";
		
		if (oldTime == 0) { text = "New reminder scheduled for " + timeStr + " on " + dateStr + "."; } 
		else { text = "Reminder rescheduled for " + timeStr + " on " + dateStr + ".";}

		Toast toast = Toast.makeText(SingleReminderActivity.this, text, Toast.LENGTH_LONG);
		toast.show();
		
	}
	
	private PendingIntent newPendingIntent(long time, String desc) {
		Intent intent = new Intent(ReminderReceiver.ACTION_SINGLE_REMINDER);
		
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