package com.google.android.gsf.checkin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import com.google.android.AndroidContext;
import com.google.checkin.CheckinClient;
import com.google.checkin.CheckinResponse;
import com.google.android.gsf.Gservices;
import com.google.android.gsf.PrivacyExtension;
import com.google.tools.Client;

public class CheckinTask extends AsyncTask<Context, Void, Integer> {

	private static final String TAG = "CheckinTask";

	@Override
	protected Integer doInBackground(Context... contexts) {
		if (contexts.length < 1) {
			return 0;
		}
		Log.d(TAG, "Starting checkin");
		Context context = contexts[0];
		SharedPreferences prefs = context.getSharedPreferences("CheckinService", 0);
		long securityToken = prefs.getLong("CheckinTask_securityToken", 0);
		AndroidContext info = PrivacyExtension.getAndroidContext(context);
		info.setSecurityToken(securityToken);
		AccountManager am = AccountManager.get(context);
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
			Gservices.setString(context.getContentResolver(), key, response.getSettings().get(key));
		}
		prefs.edit().putLong("CheckinTask_securityToken", response.getSecurityToken()).apply();
		if (prefs.getBoolean("CheckinService_notify", false)) {
			Notification.Builder n = new Notification.Builder(context);
			n.setAutoCancel(true);
			n.setSmallIcon(android.R.drawable.stat_sys_warning);
			n.setTicker("Checkin success (market: " + (response.isMarketEnabled() ? "enabled" : "disabled") + ")");
			n.setContentTitle("Checkin success");
			n.setSubText("Android ID: " + response.getAndroidIdHex());
			n.setContentText("Market: " + (response.isMarketEnabled() ? "enabled" : "disabled"));
			((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, fromBuilder(n));
			prefs.edit().remove("CheckinService_notify").apply();
		}
		return response.isMarketEnabled() ? 1 : 2;
	}

	@SuppressWarnings("deprecation")
	private static Notification fromBuilder(Notification.Builder n) {
		if (Build.VERSION.SDK_INT < 16) {
			return n.getNotification();
		} else {
			return n.build();
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		Log.d(TAG, "Stopped Checkin");
	}
}
