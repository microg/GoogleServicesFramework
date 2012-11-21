package com.google.android.gsf;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class GoogleSettingsContract {

	public static class NameValueTable implements BaseColumns {
		protected static boolean putString(ContentResolver resolver, Uri uri,
				String name, String value) {
			throw new RuntimeException(
					"Not yet implemented: NameValueTable.putString");
		}
	}

	public static final class Partner extends NameValueTable {
		public static int getInt(ContentResolver resolver, String name,
				int defaultValue) {
			String value = getString(resolver, name);
			if (value != null) {
				try {
					return Integer.parseInt(value);
				} catch (Throwable t) {
					return defaultValue;
				}
			}
			return defaultValue;
		}

		public static String getString(ContentResolver resolver, String name) {
			// TODO Auto-generated method stub
			throw new RuntimeException("Not yet implemented: Partner.getString");
		}
	}
}
