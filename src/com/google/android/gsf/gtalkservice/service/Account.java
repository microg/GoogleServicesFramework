package com.google.android.gsf.gtalkservice.service;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gsf.talk.TalkProvider;

public class Account {

	private final static String TAG = "GoogleTalkAccount";

	public static Account getAccount(final String accountIdent, final Context context) {
		if (!accountIdent.contains(":")) {
			return null;
		}
		final String[] split = accountIdent.split(":");
		final String accountType = split[0];
		final String accountName = split[1];
		if (accountType.equals("com.google")) {
			return getGoogleAccount(accountName, context);
		} else {
			return null;
		}
	}

	public static long getAccountIdForUser(final String username, final Context context) {
		final Cursor c = context.getContentResolver()
								.query(TalkProvider.ACCOUNTS_URI, new String[]{"_id"}, "username=?",
									   new String[]{username}, null);
		if (c.moveToFirst()) {
			final long id = c.getLong(0);
			c.close();
			return id;
		}
		c.close();
		final ContentValues values = new ContentValues();
		values.put("name", username);
		values.put("username", username);
		return ContentUris.parseId(context.getContentResolver().insert(TalkProvider.ACCOUNTS_URI, values));
	}

	public static Account getGoogleAccount(final String username, final Context context) {
		Log.d(TAG, username + " seems to be a google account. search it in local database...");
		final AccountManager am = AccountManager.get(context);
		final android.accounts.Account[] as = am.getAccountsByType("com.google");
		String auth = null;
		final long id = getAccountIdForUser(username, context);
		for (final android.accounts.Account a : as) {
			if (a.name.equalsIgnoreCase(username)) {
				int waitForIntent = 0;
				do {
					if (waitForIntent > 0) {
						try {
							synchronized (context) {
								context.wait(1000);
							}
						} catch (final InterruptedException e) {
							Log.w(TAG, e);
						}
					}
					waitForIntent--;
					Log.d(TAG, "found " + username + " in AccountManager as " + a + ". Trying to get auth token");
					final AccountManagerFuture<Bundle> af = am.getAuthToken(a, "mail", null, null, null, null);
					Bundle b = null;
					try {
						b = af.getResult();
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
					if (b != null) {
						auth = b.getString(AccountManager.KEY_AUTHTOKEN);
						Log.d(TAG, "authToken is " + auth);
						if (auth != null) {
							break;
						} else {
							final Intent intent = b.getParcelable(AccountManager.KEY_INTENT);
							if (intent != null && waitForIntent == -1) {
								Log.d(TAG, "Starting intent: " + intent);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								context.startActivity(intent);
								waitForIntent = 10;
							}
						}
					}
				} while (waitForIntent > 0);
			}
		}
		return new Account("com.google", "talk.google.com", username.split("@")[1], username, auth, id, username);
	}

	private final String auth;

	private final long id;
	private final String jid;
	private final String serviceHost;
	private final String serviceName;
	private final String type;
	private final String username;

	public Account(final String type, final String serviceHost, final String serviceName, final String jid,
				   final String auth, final long id, final String username) {
		this.type = type;
		this.jid = jid;
		this.auth = auth;
		this.serviceName = serviceName;
		this.serviceHost = serviceHost;
		this.id = id;
		this.username = username;
	}

	public String getAuth() {
		return auth;
	}

	public long getId() {
		return id;
	}

	public String getJid() {
		return jid;
	}

	public String getServiceHost() {
		return serviceHost;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String getType() {
		return type;
	}

	public String getUsername() {
		return username;
	}
}
