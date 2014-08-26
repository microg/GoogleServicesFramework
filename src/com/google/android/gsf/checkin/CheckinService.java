package com.google.android.gsf.checkin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.google.android.AndroidContext;
import com.google.android.gsf.Gservices;
import com.google.android.gsf.PrivacyExtension;
import com.google.checkin.CheckinClient;
import com.google.checkin.CheckinResponse;
import com.google.tools.Client;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckinService extends Service {

    private static final String TAG = "CheckinService";

    private static final int DEFAULT_INTERVAL = 43200;
    private static final int DEFAULT_MIN_TRIGGER = 30;

    private static final String KEY_LAST_FINGERPRINT = "checkin_last_build_fingerprint";
    private static final String KEY_CHECKIN_ENABLED = "checkin_enabled";
    private static final String KEY_LAST_TIME = "checkin_last_time_millis";
    private static final String KEY_MIN_TRIGGER = "checkin_min_trigger_interval";
    private static final String KEY_INTERVAL = "checkin_interval";
    private static final String KEY_SECURITY_TOKEN = "checkin_security_token";

    private static AtomicBoolean active = new AtomicBoolean(false);
    private static AtomicBoolean force = new AtomicBoolean(false);
    private static AtomicBoolean notify = new AtomicBoolean(false);

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
            if (active.getAndSet(true) && force.getAndSet(true) && notify.getAndSet(true)) {
                // Nothing to do
            } else {
                // send checkin request
                context.startService(new Intent(context, CheckinService.class));
            }
        }
    }

	public static class TriggerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            boolean doForce = intent.getBooleanExtra("force", false);
            if (active.getAndSet(true) && (!doForce || force.getAndSet(true))) {
                // Nothing to do
            } else {
                // send checkin request
                context.startService(new Intent(context, CheckinService.class));
            }
        }
    }

	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Incoming request on CheckinService");
        synchronized (CheckinService.class) {
            if (!active.get()) {
                try {
                    executeCheckin();
                } finally {
                    active.set(false);
                }
            }
        }
        return START_NOT_STICKY;
    }

    private long calculateWaitMillis() {
        if (!Gservices.getBoolean(getContentResolver(), KEY_CHECKIN_ENABLED, false)) {
            Log.d(TAG, "Ignoring request on CheckinService, checkin is disabled!");
            return Long.MAX_VALUE;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) {
            Log.d(TAG, "ConnectivityManager not accessible, stop checkin!");
            return Long.MAX_VALUE;
        }
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            Log.d(TAG, "No active network connection, stop checkin!");
            return Long.MAX_VALUE;
        }
        long lastCheckin = Gservices.getLong(getContentResolver(), KEY_LAST_TIME, 0);
        long interval = 1000 * Gservices.getLong(getContentResolver(), KEY_INTERVAL, DEFAULT_INTERVAL);
        long minTrigger = 1000 * Gservices.getLong(getContentResolver(), KEY_MIN_TRIGGER, DEFAULT_MIN_TRIGGER);
        long time = System.currentTimeMillis();
        long regularWait = lastCheckin + interval - time;
        long forceWait = lastCheckin + minTrigger - time;
        if (force.getAndSet(forceWait > 0)) {
            return forceWait;
        }
        if (Gservices.getString(getContentResolver(), KEY_LAST_FINGERPRINT).equals(Build.FINGERPRINT)) {
            Log.d(TAG, "Fingerprint changed, forcing checkin");
            return forceWait;
        }
        return regularWait;
    }

    private void executeCheckin() {
        long waitTime = calculateWaitMillis();
        if (waitTime > 0) {
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(this, Receiver.class), 0);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + waitTime, pi);
        } else {
            try {
                doCheckin();
                Gservices.setString(getContentResolver(), KEY_LAST_FINGERPRINT, Build.FINGERPRINT);
                Gservices.setString(getContentResolver(), KEY_LAST_TIME, Long.toString(System.currentTimeMillis()));
            } catch (Throwable t) {
                Log.w(TAG, "Checkin failed", t);
            }
        }
    }

    private void doCheckin() {
        Log.d(TAG, "Starting checkin");
        long securityToken = Gservices.getLong(getContentResolver(), KEY_SECURITY_TOKEN, 0);
        AndroidContext info = PrivacyExtension.getAndroidContext(this);
        info.setSecurityToken(securityToken);
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");
        String token = null;
        if (accounts.length > 0) {
            Account a = accounts[0];
            try {
                token = am.blockingGetAuthToken(a, "ac2dm", true);
            } catch (Exception e) {
            }
            if (token == null) {
                try {
                    token = am.blockingGetAuthToken(a, "SID", true);
                } catch (Exception e) {
                }
            }
            if (token != null) {
                info.setEmail(a.name);
            }
        }
        Client.DEBUG = true;
        Client.DEBUG_HEADER = true;
        CheckinResponse response = new CheckinClient().checkin(info, token);
        Log.d(TAG, "Checkin success: id:" + response.getAndroidIdHex() + ", token:" + response.getSecurityToken() +
                ", digest:" + response.getDigest() + ", market:" + response.isMarketEnabled());
        for (String key : response.getSettings().keySet()) {
            Gservices.setString(getContentResolver(), key, response.getSettings().get(key));
        }
        Gservices.setString(getContentResolver(), KEY_SECURITY_TOKEN, Long.toString(response.getSecurityToken()));
        if (notify.getAndSet(false)) {
            Notification.Builder n = new Notification.Builder(this);
            n.setAutoCancel(true);
            n.setSmallIcon(android.R.drawable.stat_notify_sync);
            n.setContentTitle("Checkin success");
            n.setContentText("Android ID: " + response.getAndroidIdHex());
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, fromBuilder(n));
        }
    }

    private static Notification fromBuilder(Notification.Builder n) {
        if (Build.VERSION.SDK_INT < 16) {
            return n.getNotification();
        } else {
            return n.build();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "CheckinService.onBind: huh, we shouldn't get called!");
        return null;
    }

}
