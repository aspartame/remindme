package com.aspartame.RemindMe;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class ReminderReceiver extends BroadcastReceiver {
	public static final String DEBUG_TAG = "ReminderReceiver";
	public static final String TAG = "WAKE_LOCK_TAG";
	public static final String ACTION_SINGLE_REMINDER = "com.aspartame.RemindMe.ACTION_SINGLE_REMINDER";
	public static final String ACTION_PERIODIC_REMINDER = "com.aspartame.RemindMe.ACTION_PERIODIC_REMINDER";
	public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	public static PowerManager.WakeLock wakeLock = null;
	public static KeyguardManager.KeyguardLock keyLock = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Intent received");
		Log.d(DEBUG_TAG, "Action: " + intent.getAction());
		
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		
		if ( intent.getAction().equals(ACTION_SINGLE_REMINDER) ||
			 intent.getAction().equals(ACTION_PERIODIC_REMINDER) ) {
			
			wakeLock = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE), TAG);
			wakeLock.acquire();
			
			KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
			keyLock = km.newKeyguardLock(TAG);
			keyLock.disableKeyguard();
			
			String soundUri = intent.getStringExtra("soundUri");
			boolean vibrate = intent.getBooleanExtra("vibrate", true);
			String desc = intent.getStringExtra("desc");
			long time = intent.getLongExtra("time", 0);
			boolean isRepeat = intent.getBooleanExtra("isRepeat", false);

			Intent i = new Intent(context, ReminderService.class);
			i.putExtra("soundUri", soundUri);
			i.putExtra("vibrate", vibrate);
			i.putExtra("desc", desc);
			i.putExtra("time", time);
			i.putExtra("isRepeat", isRepeat);
			
			String type = (intent.getAction().equals(ACTION_SINGLE_REMINDER)) ? "single" : "periodic";
			i.putExtra("type", type);
			
			String preDesc = desc.equals("") ? "" : ": ";
			Toast.makeText(context, "RemindMe reminds you" + preDesc + desc, Toast.LENGTH_LONG).show();
			context.startService(i);
		} else if (intent.getAction().equals(BOOT_COMPLETED)) {
			Intent i = new Intent(context, OnBootService.class);
			context.startService(i);
		}
	}
}









