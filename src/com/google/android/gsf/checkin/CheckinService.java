package com.google.android.gsf.checkin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.android.common.OperationScheduler;
import com.android.common.OperationScheduler.Options;
import com.google.android.gsf.Gservices;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckinService extends Service {

	private final static String TAG = "CheckinService";
	private CheckinTask task;
	private static AtomicBoolean active = new AtomicBoolean(false);

	public static class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (active.compareAndSet(false, true)) {
				context.startService(new Intent(context, CheckinService.class));
			}
		}
	}

	public static class SecretCodeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences prefs = context.getSharedPreferences("CheckinService", 0);
			prefs.edit().putBoolean("CheckinService_notify", true).apply();
			OperationScheduler os = new OperationScheduler(prefs);
			os.setTriggerTimeMillis(0);
			os.resetTransientError();
			active.set(true);
			context.startService(new Intent(context, CheckinService.class));
		}
	}

	public static class TriggerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences prefs = context.getSharedPreferences("CheckinService", 0);
			OperationScheduler os = new OperationScheduler(prefs);
			os.setTriggerTimeMillis(0);
			if (intent.getBooleanExtra("force", false)) {
				os.resetTransientError();
			}
			active.set(true);
			context.startService(new Intent(context, CheckinService.class));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!Gservices.getBoolean(getContentResolver(), "checkin_enabled", false)) {
			return 0;
		}
		synchronized (CheckinService.class) {
			if (task == null) {
				task = new CheckinTask();
			} else {
				return 0;
			}
		}
		SharedPreferences prefs = getSharedPreferences("CheckinService", 0);
		OperationScheduler os = new OperationScheduler(prefs);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info != null && info.isConnected()) {
				os.setEnabledState(true);
			} else {
				os.setEnabledState(false);
			}
		}
		String build = Build.FINGERPRINT + "\n" + Build.RADIO + "\n" + Build.BOOTLOADER;
		if (!build.equals(prefs.getString("CheckinService_lastBuild", null))) {
			prefs.edit().putString("CheckinService_lastBuild", build).apply();
			os.setTriggerTimeMillis(0);
		}
		Options options = new Options();
		options.minTriggerMillis = 30000;
		options.periodicIntervalMillis = 1000 * Gservices.getLong(getContentResolver(), "checkin_interval", 43200);
		long nextCheckin = os.getNextTimeMillis(options);
		long current = System.currentTimeMillis();
		if (nextCheckin < current) {
			task.execute(this);
			os.onSuccess();
		} else {
			PendingIntent pi = PendingIntent.getBroadcast(this, startId, new Intent(this, Receiver.class), flags);
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			if (options.periodicIntervalMillis > 0) {
				am.setRepeating(AlarmManager.RTC, nextCheckin, options.periodicIntervalMillis, pi);
			} else {
				am.set(AlarmManager.RTC, nextCheckin, pi);
			}
		}
		task = null;
		active.set(false);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "CheckinService.onBind: huh, we shouldn't get called!");
		return null;
	}

}
