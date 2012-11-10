package com.google.android.gsf;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class Gservices {

	private static HashMap<String, String> cache;
	public static final Uri CONTENT_PREFIX_URI = Uri
			.parse("content://com.google.android.gsf.gservices/prefix");
	public static final Uri CONTENT_URI = Uri
			.parse("content://com.google.android.gsf.gservices");
	public static final Pattern FALSE_PATTERN = Pattern.compile(
			"^(0|false|f|off|no|n)$", 2);
	private static String[] preloadedPrefixes = new String[0];
	private static ContentResolver resolver;
	public static final Pattern TRUE_PATTERN = Pattern.compile(
			"^(1|true|t|on|yes|y)$", 2);
	private static Object versionToken;

	public static void bulkCacheByPrefix(final ContentResolver resolver,
			final String... prefixes) {
		final Map<String, String> map = getStringsByPrefix(resolver, prefixes);
		synchronized (Gservices.class) {
			ensureCacheInitializedLocked(resolver);
			preloadedPrefixes = prefixes;
			for (final String key : map.keySet()) {
				cache.put(key, map.get(key));
			}
		}
	}

	private static void ensureCacheInitializedLocked(
			final ContentResolver resolver) {
		if (cache == null) {
			cache = new HashMap<String, String>();
			versionToken = new Object();
			Gservices.resolver = resolver;
			new Thread(new Runnable() {

				@Override
				public void run() {
					Looper.prepare();
					resolver.registerContentObserver(
							CONTENT_URI,
							true,
							new ContentObserver(new Handler(Looper.myLooper())) {
								@Override
								public void onChange(final boolean selfChange) {
									synchronized (Gservices.class) {
										Gservices.cache.clear();
										Gservices.versionToken = new Object();
									}
									if (Gservices.preloadedPrefixes.length > 0) {
										Gservices.bulkCacheByPrefix(resolver,
												preloadedPrefixes);
									}
								}
							});
					Looper.loop();
				}
			}).start();
		}
	}

	public static boolean getBoolean(final ContentResolver resolver,
			final String key, boolean defaultValue) {
		final String string = getString(resolver, key);
		if (string != null && !string.equals("")) {
			if (TRUE_PATTERN.matcher(string).matches()) {
				defaultValue = true;
			} else if (FALSE_PATTERN.matcher(string).matches()) {
				defaultValue = false;
			} else {
				Log.w("Gservices", "attempt to read " + key + " = \"" + string
						+ "\" as boolean");
			}
		}
		return defaultValue;
	}

	public static long getLong(final ContentResolver resolver,
			final String key, long defaultValue) {
		final String string = getString(resolver, key);
		if (string != null && !string.equals("")) {
			try {
				defaultValue = Long.parseLong(string);
			} catch (final Throwable t) {
				Log.w("Gservices", "attempt to read " + key + " = \"" + string
						+ "\" as long");
			}
		}
		return defaultValue;
	}

	public static String getString(final ContentResolver resolver,
			final String key) {
		return getString(resolver, key, null);
	}

	public static String getString(final ContentResolver resolver,
			final String key, final String defaultValue) {
		Object versionToken;
		synchronized (Gservices.class) {
			ensureCacheInitializedLocked(resolver);
			versionToken = Gservices.versionToken;
			if (cache.containsKey(key)) {
				return cache.get(key);
			}
		}
		for (final String prefix : preloadedPrefixes) {
			if (key.startsWith(prefix)) {
				return null;
			}
		}
		final Cursor cursor = Gservices.resolver.query(CONTENT_URI, null, null,
				new String[] { key }, null);
		if (cursor == null) {
			cache.put(key, null);
			return null;
		}
		String result = null;
		try {
			cursor.moveToFirst();
			result = cursor.getString(1);
			cursor.close();
		} catch (final Throwable t) {
			if (!cursor.isClosed()) {
				cursor.close();
			}
		}
		synchronized (Gservices.class) {
			if (versionToken == Gservices.versionToken) {
				cache.put(key, result);
			}
		}
		return result;
	}

	public static Map<String, String> getStringsByPrefix(
			final ContentResolver resolver, final String... prefixes) {
		final Map<String, String> map = new TreeMap<String, String>();
		final Cursor cursor = resolver.query(CONTENT_PREFIX_URI, null, null,
				prefixes, null);
		if (cursor == null) {
			return map;
		}
		try {
			while (cursor.moveToNext()) {
				map.put(cursor.getString(0), cursor.getString(1));
			}
			cursor.close();
		} catch (final Throwable t) {
			if (!cursor.isClosed()) {
				cursor.close();
			}
		}
		return map;
	}

	public static Object getVersionToken(final ContentResolver resolver) {
		synchronized (Gservices.class) {
			ensureCacheInitializedLocked(resolver);
			return Gservices.versionToken;
		}
	}
}
