package com.aspartame.RemindMe;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RemindMe extends Activity {
	private static final String DEBUG_TAG = "RemindMe";
	public static final int NEW_SINGLE_REMINDER = 0;
	public static final int NEW_PERIODIC_REMINDER = 1;
	public static final int EDIT_SINGLE_REMINDER = 2;
	public static final int EDIT_PERIODIC_REMINDER = 3;
	private static final int MENU_ITEM_SETTINGS = Menu.FIRST;
	private static final int MENU_ITEM_HELP = Menu.FIRST + 1;
	private static final int MENU_ITEM_ABOUT = Menu.FIRST + 2;
	private SharedPreferences preferences;
	private Context context;
	private DBadapter dbAdapter;
	private AlarmManager am;
	private ArrayList<Reminder> activeReminders = new ArrayList<Reminder>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = getApplicationContext();
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		dbAdapter = new DBadapter(this);
		dbAdapter.open();

		// If started by notification
		if (ReminderService.nm != null) {
			Log.d(DEBUG_TAG, "in onCreate(), canceling notification");

			Intent intent = getIntent();
			int notificationId = intent.getIntExtra("notificationId", -1);
			Log.d(DEBUG_TAG, "in onCreate(), notificationId: " + notificationId);
			if (notificationId >= 0) { 
				ReminderService.nm.cancel(notificationId);

				showExpiredDialog(intent);
			}
		}

		setupView();
	}

	@Override
	public void onResume() {
		super.onResume();
		setupView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		int groupId = 0;
		int menuItemOrder = Menu.NONE;

		int menuItemId = MENU_ITEM_SETTINGS;
		int menuItemText = R.string.settings;
		MenuItem menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		menuItem.setIcon(R.drawable.settings_32);
		
		menuItemId = MENU_ITEM_HELP;
		menuItemText = R.string.help;
		menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		menuItem.setIcon(R.drawable.help_32);
		
		menuItemId = MENU_ITEM_ABOUT;
		menuItemText = R.string.about;
		menuItem = menu.add(groupId, menuItemId, menuItemOrder, menuItemText);
		menuItem.setIcon(R.drawable.about_32);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (item.getItemId() == MENU_ITEM_SETTINGS) {
			Intent intent = new Intent(context, SettingsActivity.class);
			startActivity(intent);
		}
		
		if (item.getItemId() == MENU_ITEM_HELP) {
			showHelpDialog();
		}

		if (item.getItemId() == MENU_ITEM_ABOUT) {
			showAboutDialog();
		}

		return true;
	}

	private void showAboutDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		final AlertDialog aboutDialog = ad.create();
		
		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View aboutView = li.inflate(R.layout.about, null);
		
		Window window = aboutDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		aboutDialog.setIcon(R.drawable.about_32);
		aboutDialog.setTitle("About RemindMe");
		aboutDialog.setView(aboutView);
		
		LinearLayout aboutLayout = (LinearLayout) aboutView.findViewById(R.id.about_layout);
		
		aboutLayout.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Log.d(DEBUG_TAG, "dismiss aboutDialog");
				aboutDialog.dismiss();
			}
		});
		
		aboutDialog.show();
	}
	
	private void showHelpDialog() {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);
		final AlertDialog helpDialog = ad.create();

		LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View helpView = li.inflate(R.layout.help, null);
		
		helpDialog.setIcon(R.drawable.help_32);
		helpDialog.setTitle("How to use RemindMe");
		helpDialog.setView(helpView);

		Window window = helpDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		LinearLayout helpLayout = (LinearLayout) helpView.findViewById(R.id.help_layout);
		
		helpLayout.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				helpDialog.dismiss();
			}
		});
		
		helpDialog.show();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	protected void onNewIntent(Intent intent) {
		if (ReminderService.nm != null) {	
			Log.d(DEBUG_TAG, "in onNewIntent, canceling notification");

			int notificationId = intent.getIntExtra("notificationId", -1);
			Log.d(DEBUG_TAG, "in onNewIntent, notificationId: " + notificationId);
			if (notificationId >= 0) { 
				ReminderService.nm.cancel(notificationId);

				showExpiredDialog(intent);
			}
		}

		setupView();
	}

	private void populateActiveRemindersList() {
		Cursor cursor = dbAdapter.getAllSingleRemindersCursor();
		startManagingCursor(cursor);

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
	}

	private void setupView() {	
		populateActiveRemindersList();

		LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View mainView = li.inflate(R.layout.main, null);

		View newAlertView = (View) mainView.findViewById(R.id.create_new_alert_view);

		// Set onClickListener on "New Reminder"-button
		newAlertView.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(context, SingleReminderActivity.class);
				intent.putExtra("time", 0l);
				intent.putExtra("desc", "");
				startActivityForResult(intent, NEW_SINGLE_REMINDER);
			}
		});

		// Set onLongClickListener on "New Reminder"-button
		newAlertView.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View view) {
				Intent intent = new Intent(context, PeriodicReminderActivity.class);
				intent.putExtra("time", 0l);
				intent.putExtra("desc", "");
				intent.putExtra("interval", 0l);
				startActivityForResult(intent, NEW_PERIODIC_REMINDER);

				return true;
			}
		});

		// Populate active alerts view
		LinearLayout activeRemindersLayout = (LinearLayout) mainView.findViewById(R.id.active_alerts_layout);

		TextView labelView = (TextView) mainView.findViewById(R.id.active_alerts_label_id);
		if (activeReminders.size() == 0) { labelView.setText("No pending reminders"); }

		for (int i = 0; i < activeReminders.size(); i++) {
			View reminderView = li.inflate(R.layout.active_alert_view, null);

			String dateTime = activeReminders.get(i).getTimeStr() + " (" + activeReminders.get(i).getDateStr() + ")";

			if (activeReminders.get(i).getInterval() != 0) {
				TextView intervalView = (TextView) reminderView.findViewById(R.id.single_event_datetime_interval);
				
				String interval = DateParser.parseInterval(activeReminders.get(i).getInterval());
				
				if (interval.equals("Weekdays")) { interval = DateParser.parseWeekdays(activeReminders.get(i).getWeekdays()); }
				else { interval = DateParser.parseInterval(activeReminders.get(i).getInterval()); }
				
				String text = "Interval: " + interval;
				
				intervalView.setText(text);
				intervalView.setVisibility(android.view.View.VISIBLE);
			}

			TextView dateTimeView = (TextView) reminderView.findViewById(R.id.single_event_datetime);
			dateTimeView.setText(dateTime);

			if (!activeReminders.get(i).getDesc().equals("")) {
				TextView descView = (TextView) reminderView.findViewById(R.id.single_event_description);
				descView.setText(activeReminders.get(i).getDesc());
				descView.setVisibility(android.view.View.VISIBLE);
			}

			if (activeReminders.get(i).getInterval() != 0) {
				ImageView imgView = (ImageView) reminderView.findViewById(R.id.single_event_icon);
				imgView.setImageDrawable(getResources().getDrawable(R.drawable.advertisment_periodic_32));
			}

			final int pos = i;

			reminderView.setOnClickListener(new OnClickListener() {
				public void onClick(View view) {
					// showEditDialog to choose edit/delete
					long time = activeReminders.get(pos).getTime();
					String desc = activeReminders.get(pos).getDesc();
					long interval = activeReminders.get(pos).getInterval();
					int weekdays = activeReminders.get(pos).getWeekdays();

					showEditReminderDialog(time, desc, interval, weekdays);
				}
			});

			activeRemindersLayout.addView(reminderView);

			View spaceView = li.inflate(R.layout.space, null);
			activeRemindersLayout.addView(spaceView);
		}

		setContentView(mainView);
	}

	private void showEditReminderDialog(final long currentTime, final String desc, final long interval, final int weekdays) {
		AlertDialog.Builder ad = new AlertDialog.Builder(this);

		ad.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {

				if (interval != 0) {
					Intent intent = new Intent(context, PeriodicReminderActivity.class);
					intent.putExtra("time", currentTime);
					intent.putExtra("desc", desc);
					intent.putExtra("interval", interval);
					intent.putExtra("weekdays", weekdays);
					startActivityForResult(intent, EDIT_PERIODIC_REMINDER);
				} else {
					Intent intent = new Intent(context, SingleReminderActivity.class);
					intent.putExtra("time", currentTime);
					intent.putExtra("desc", desc);
					startActivityForResult(intent, EDIT_SINGLE_REMINDER);
				}
			}
		});

		ad.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int arg1) {
				am = (AlarmManager) getSystemService(ALARM_SERVICE);

				PendingIntent pIntent = newPendingIntent(currentTime, desc, interval);
				am.cancel(pIntent);

				if (interval != 0) { dbAdapter.removePeriodicReminder(currentTime); } 
				else { dbAdapter.removeSingleReminder(currentTime); }

				setupView();
			}
		});

		final AlertDialog confirmDialog = ad.create();

		Window window = confirmDialog.getWindow();
		window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
						WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		
		confirmDialog.setIcon(R.drawable.advertisment_32);
		confirmDialog.setTitle(getTimeStr(currentTime) + " (" + getDateStr(currentTime) + ")");
		confirmDialog.setMessage(desc);
		confirmDialog.show();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch(requestCode) {

		case NEW_SINGLE_REMINDER: {
			if (resultCode == RESULT_OK) {
				Log.d(DEBUG_TAG, "New single reminder created");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(DEBUG_TAG, "New single reminder canceled");
			}
		} break;

		case EDIT_SINGLE_REMINDER: {
			if (resultCode == RESULT_OK) {
				Log.d(DEBUG_TAG, "Edited single reminder");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(DEBUG_TAG, "Canceled editing of single reminder");
			}
		} break;

		case NEW_PERIODIC_REMINDER: {
			if (resultCode == RESULT_OK) {
				Log.d(DEBUG_TAG, "New periodic reminder created");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(DEBUG_TAG, "New periodic reminder canceled");
			}
		} break;

		case EDIT_PERIODIC_REMINDER: {
			if (resultCode == RESULT_OK) {
				Log.d(DEBUG_TAG, "Edited periodic reminder");
			} else if (resultCode == RESULT_CANCELED) {
				Log.d(DEBUG_TAG, "Canceled editing of periodic reminder");
			}
		} break;
		}

	}

	/* Only for single reminders */
	private void showExpiredDialog(Intent intent) {
		Log.d(DEBUG_TAG, "showExpiredDialog()");
		AlertDialog.Builder ad = new AlertDialog.Builder(this);

		final Intent mIntent = intent;

		final String desc = intent.getStringExtra("desc");
		final long time = intent.getLongExtra("time", 0);

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);

		String dateStr = getDateStr(time);
		String timeStr = getTimeStr(time);

		if (time > 0) {

			ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int arg1) {
					am = (AlarmManager) getSystemService(ALARM_SERVICE);
					final PendingIntent pIntent = newSingleReminderPendingIntent(mIntent);
					am.cancel(pIntent);

					dbAdapter.removeSingleReminder(time);
					setupView();
				}
			});

			if (intent.getStringExtra("type").equals("single")) {
				ad.setNegativeButton("Reschedule", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
						am = (AlarmManager) getSystemService(ALARM_SERVICE);
						final PendingIntent pIntent = newSingleReminderPendingIntent(mIntent);
						am.cancel(pIntent);

						dbAdapter.removeSingleReminder(time);

						Intent intent = new Intent(context, SingleReminderActivity.class);
						intent.putExtra("time", time);
						intent.putExtra("desc", desc);
						startActivityForResult(intent, NEW_SINGLE_REMINDER);
					}
				});
			}

			final AlertDialog confirmDialog = ad.create();

			Window window = confirmDialog.getWindow();
			window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 
							WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			
			if (intent.getStringExtra("type").equals("single")) { confirmDialog.setIcon(R.drawable.advertisment_32); }
			else {confirmDialog.setIcon(R.drawable.advertisment_periodic_32); }
			confirmDialog.setTitle(timeStr + " (" + dateStr + ")");
			confirmDialog.setMessage(desc);
			confirmDialog.show();
		}
	}

	private PendingIntent newSingleReminderPendingIntent(Intent intent) {
		String soundUri = intent.getStringExtra("soundUri");
		boolean vibrate = intent.getBooleanExtra("vibrate", true);
		String desc = intent.getStringExtra("desc");
		long time = intent.getLongExtra("time", 0);
		boolean isRepeat = intent.getBooleanExtra("isRepeat", false);

		Intent newIntent = new Intent(ReminderReceiver.ACTION_SINGLE_REMINDER);
		newIntent.setDataAndType(Uri.parse("foo" + time), "com.aspartame.RemindMe/foo_type");

		newIntent.putExtra("soundUri", soundUri);
		newIntent.putExtra("vibrate", vibrate);
		newIntent.putExtra("desc", desc);
		newIntent.putExtra("time", time);
		newIntent.putExtra("isRepeat", isRepeat);

		PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pIntent;
	}

	private PendingIntent newPendingIntent(long time, String desc, long interval) {

		String action = (interval == 0) ? ReminderReceiver.ACTION_SINGLE_REMINDER : ReminderReceiver.ACTION_PERIODIC_REMINDER;
		Intent newIntent = new Intent(action);
		newIntent.setDataAndType(Uri.parse("foo" + time), "com.aspartame.RemindMe/foo_type");

		newIntent.putExtra("desc", desc);
		newIntent.putExtra("time", time);

		if (interval == 0) {
			String soundUri = preferences.getString(getResources().getString(R.string.preferences_notification_sound), "default");
			boolean vibrate = preferences.getBoolean(getResources().getString(R.string.preferences_vibration), true);

			newIntent.putExtra("soundUri", soundUri);
			newIntent.putExtra("vibrate", vibrate);
			newIntent.putExtra("isRepeat", false);
		} else {
			newIntent.putExtra("interval", interval);
		}

		PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		return pIntent;

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

	@Override
	public void onDestroy() {
		dbAdapter.close();
		super.onDestroy();
	}
}