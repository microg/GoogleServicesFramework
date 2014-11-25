package com.google.android.gsf.checkin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import com.google.android.AndroidContext;
import com.google.android.gsf.GservicesContract;
import com.google.android.gsf.PrivacyExtension;
import com.google.checkin.CheckinClient;
import com.google.checkin.CheckinResponse;
import com.google.tools.Client;

import java.util.concurrent.atomic.AtomicBoolean;

public class CheckinService extends Service {

    private static final String TAG = "CheckinService";
    private static final String PREFERENCES_NAME = "CheckinService";

    private static final int DEFAULT_INTERVAL = 43200;
    private static final int DEFAULT_MIN_TRIGGER = 30;

    public static final String KEY_LAST_FINGERPRINT = "checkin_last_build_fingerprint";
    public static final String KEY_CHECKIN_ENABLED = "checkin_enabled";
    public static final String KEY_LAST_TIME = "checkin_last_time_millis";
    public static final String KEY_MIN_TRIGGER = "checkin_min_trigger_interval";
    public static final String KEY_INTERVAL = "checkin_interval";
    public static final String KEY_SECURITY_TOKEN = "checkin_security_token";

    private static AtomicBoolean active = new AtomicBoolean(false);
    private static AtomicBoolean force = new AtomicBoolean(false);
    private static AtomicBoolean notify = new AtomicBoolean(false);

	public static class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (active.compareAndSet(false, true)) {
                Log.d(TAG, "Triggered by: " + intent);
                context.startService(new Intent(context, CheckinService.class));
            }
		}
	}

	public static class SecretCodeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            if (active.getAndSet(true) & force.getAndSet(true) & notify.getAndSet(true)) {
                // Nothing to do
            } else {
                // send checkin request
                Log.d(TAG, "Triggered by: " + intent);
                context.startService(new Intent(context, CheckinService.class));
            }
        }
    }

	public static class TriggerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            boolean doForce = intent.getBooleanExtra("force", false);
            if (doForce) {
                getPreferences(context).edit().putBoolean(KEY_CHECKIN_ENABLED, true).apply();
            }
            if (active.getAndSet(true) & (!doForce || force.getAndSet(true))) {
                // Nothing to do
            } else {
                // send checkin request
                Log.d(TAG, "Triggered by: " + intent);
                context.startService(new Intent(context, CheckinService.class));
            }
        }
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Incoming request on CheckinService");
        synchronized (CheckinService.class) {
            if (active.get()) {
                if (!executeCheckin()) {
                    active.set(false);
                }
            }
        }
        return START_NOT_STICKY;
    }

    private long calculateWaitMillis() {
        if (!getPreferences().getBoolean(KEY_CHECKIN_ENABLED, false)) {
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
        long lastCheckin = getPreferences().getLong(KEY_LAST_TIME, 0);
        long interval = 1000 * getPreferences().getLong(KEY_INTERVAL, DEFAULT_INTERVAL);
        long minTrigger = 1000 * getPreferences().getLong(KEY_MIN_TRIGGER, DEFAULT_MIN_TRIGGER);
        long time = System.currentTimeMillis();
        long regularWait = lastCheckin + interval - time;
        long forceWait = lastCheckin + minTrigger - time;
        if (force.getAndSet(forceWait > 0)) {
            return forceWait;
        }
        if (!Build.FINGERPRINT.equals(getPreferences().getString(KEY_LAST_FINGERPRINT, null))) {
            Log.d(TAG, "Fingerprint changed, forcing checkin");
            return forceWait;
        }
        return regularWait;
    }

    private boolean executeCheckin() {
        long waitTime = calculateWaitMillis();
        if (waitTime == Long.MAX_VALUE) {
            return false;
        } else if (waitTime > 0) {
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(this, Receiver.class), 0);
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + waitTime, pi);
            return false;
        } else {
            Log.d(TAG, "Starting checkin in new thread...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doCheckin();
                    getPreferences().edit()
                            .putString(KEY_LAST_FINGERPRINT, Build.FINGERPRINT)
                            .putLong(KEY_LAST_TIME, System.currentTimeMillis()).apply();
                    active.set(false);
                }
            }).start();
            return true;
        }
    }

    private void doCheckin() {
        Log.d(TAG, "Starting checkin");
        long securityToken = getPreferences().getLong(KEY_SECURITY_TOKEN, 0);
        AndroidContext info = PrivacyExtension.getAndroidContext(this);
        for (String s : info.toString().split("\r\n")) {
            Log.d(TAG, s);
        }
        if (securityToken != 0) {
            info.set(AndroidContext.KEY_CHECKIN_SECURITY_TOKEN, securityToken);
        } else {
            info.unset(AndroidContext.KEY_ANDROID_ID_HEX);
            info.unset(AndroidContext.KEY_ANDROID_ID_LONG);
        }
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");
        String token = null;
        if (accounts.length > 0) {
            Account a = accounts[0];
            try {
                token = am.blockingGetAuthToken(a, "ac2dm", true);
            } catch (Exception e) {
            }
            if (token == null || token.isEmpty()) {
                try {
                    token = am.blockingGetAuthToken(a, "SID", true);
                } catch (Exception e) {
                }
            }
            info.set(AndroidContext.KEY_EMAIL, a.name);
            info.set(AndroidContext.KEY_AUTHORIZATION_TOKEN, token);
        }
        Client.DEBUG = true;
        Client.DEBUG_HEADER = true;
        CheckinResponse response = new CheckinClient().checkin(info);
        Log.d(TAG, "Checkin success: id:" + response.getAndroidIdHex() + ", token:" + response.getSecurityToken() +
                ", digest:" + response.getDigest() + ", market:" + response.isMarketEnabled());
        for (String key : response.getSettings().keySet()) {
            GservicesContract.setString(getContentResolver(), key, response.getSettings().get(key));
        }
        getPreferences().edit().putLong(KEY_SECURITY_TOKEN, response.getSecurityToken()).apply();
        if (notify.getAndSet(false)) {
            Notification.Builder n = new Notification.Builder(this);
            n.setAutoCancel(true);
            n.setSmallIcon(android.R.drawable.stat_notify_sync);
            n.setContentTitle("Checkin success");
            n.setContentText("Android ID: " + response.getAndroidIdHex());
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, fromBuilder(n));
        }
    }

    private SharedPreferences getPreferences() {
        return getPreferences(this);
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
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
