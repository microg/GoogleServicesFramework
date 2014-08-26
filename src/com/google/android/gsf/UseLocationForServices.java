package com.google.android.gsf;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class UseLocationForServices {

	private static final String[] GOOGLE_GEOLOCATION_ORIGINS =
			new String[]{"http://www.google.com", "http://www.google.co.uk"};

	private static String addGoogleOrigins(String origins) {
		HashSet<String> hashset = parseAllowGeolocationOrigins(origins);
		String as[] = GOOGLE_GEOLOCATION_ORIGINS;
		int i = as.length;
		for (int j = 0; j < i; j++)
			hashset.add(as[j]);

		return formatAllowGeolocationOrigins(hashset);
	}

	private static String formatAllowGeolocationOrigins(Collection collection) {
		StringBuilder stringbuilder = new StringBuilder();
		String s;
		for (Iterator iterator = collection.iterator(); iterator.hasNext(); stringbuilder.append(s)) {
			s = (String) iterator.next();
			if (stringbuilder.length() > 0)
				stringbuilder.append(' ');
		}

		return stringbuilder.toString();
	}

	private static HashSet<String> parseAllowGeolocationOrigins(String origins) {
		HashSet<String> hashset = new HashSet<String>();
		if (!TextUtils.isEmpty(origins)) {
			String as[] = origins.split("\\s+");
			int i = as.length;
			for (int j = 0; j < i; j++) {
				String s1 = as[j];
				if (!TextUtils.isEmpty(s1))
					hashset.add(s1);
			}

		}
		return hashset;
	}

	private static String removeGoogleOrigins(String origins) {
		HashSet<String> hashset = parseAllowGeolocationOrigins(origins);
		String as[] = GOOGLE_GEOLOCATION_ORIGINS;
		int i = as.length;
		for (int j = 0; j < i; j++)
			hashset.remove(as[j]);

		return formatAllowGeolocationOrigins(hashset);
	}

	private static void setGoogleBrowserGeolocation(Context context, boolean enable) {
		ContentResolver contentresolver = context.getContentResolver();
		String origins = android.provider.Settings.Secure.getString(contentresolver, "allowed_geolocation_origins");
		if (enable) {
			origins = addGoogleOrigins(origins);
		} else {
			origins = removeGoogleOrigins(origins);
		}
		android.provider.Settings.Secure.putString(contentresolver, "allowed_geolocation_origins", origins);
	}

	public static boolean setUseLocationForServices(Context context, boolean enable) {
		setGoogleBrowserGeolocation(context, enable);
		ContentResolver contentresolver = context.getContentResolver();
		boolean result =
				GoogleSettingsContract.Partner.putInt(contentresolver, "use_location_for_services", (enable ? 1 : 0));
		context.sendBroadcast(
				new Intent("com.google.android.gsf.settings.GoogleLocationSettings.UPDATE_LOCATION_SETTINGS"));
		return result;
	}

}
