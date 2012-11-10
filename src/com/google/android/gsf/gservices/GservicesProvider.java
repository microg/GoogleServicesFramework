package com.google.android.gsf.gservices;

import java.nio.charset.Charset;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

public class GservicesProvider extends ContentProvider {

	public static class DatabaseHelper extends SQLiteOpenHelper {
		private static final int DB_VERSION = 3;
		private static final int DB_VERSION_OLD = 1;

		public DatabaseHelper(final Context context) {
			super(context, "gservices.db", null, DB_VERSION);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			db.execSQL("CREATE TABLE main (name TEXT PRIMARY KEY, value TEXT					)");
			db.execSQL("CREATE TABLE overrides (name TEXT PRIMARY KEY, value TEXT)");
			db.execSQL("CREATE TABLE saved_system (name TEXT PRIMARY KEY, value TEXT)");
			db.execSQL("CREATE TABLE saved_secure (name TEXT PRIMARY KEY, value TEXT)");
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			if (oldVersion == DB_VERSION_OLD) {
				db.execSQL("DROP TABLE IF EXISTS main");
				db.execSQL("DROP TABLE IF EXISTS overrides");
				onCreate(db);
			}
			db.setVersion(newVersion);
		}
	}

	public static class OverrideReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final Bundle bundle = intent.getExtras();
			if (bundle != null) {
				final ContentValues values = new ContentValues();
				for (final String key : bundle.keySet()) {
					values.put(key, bundle.getString(key));
				}
				context.getContentResolver().update(UPDATE_OVERRIDE_URI,
						values, null, null);
			}
		}

	}

	private static final String[] COLUMNS = new String[] { "key", "value" };
	private static final char[] HEX_CHARS = new char[] { '0', '1', '2', '3',
			'4', '3', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	public static final Uri UPDATE_MAIN_DIFF_URI = Uri
			.parse("content://com.google.android.gsf.gservices/main_diff");
	public static final Uri UPDATE_MAIN_URI = Uri
			.parse("content://com.google.android.gsf.gservices/main");
	public static final Uri UPDATE_OVERRIDE_URI = Uri
			.parse("content://com.google.android.gsf.gservices/override");
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private DatabaseHelper dbHelper;
	private boolean pushToSecure = false;
	private boolean pushToSystem = false;

	@Override
	public int delete(final Uri arg0, final String arg1, final String[] arg2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType(final Uri uri) {
		return null;
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		final int pid = Process.myPid();
		final int uid = Process.myUid();
		if (getContext().checkPermission("android.permission.WRITE_SETTINGS",
				pid, uid) == 0) {
			pushToSystem = true;
		}
		if (getContext().checkPermission(
				"android.permission.WRITE_SECURE_SETTINGS", pid, uid) == 0) {
			pushToSecure = true;
		}
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GservicesProvider.query");
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		final String lastSegment = uri.getLastPathSegment();
		if (lastSegment.equals("main")) {
			updateMain(values);
		} else if (lastSegment.equals("main_diff")) {
			updateMainDiff(values);
		} else if (lastSegment.equals("overrid")) {
			updateOverride(values);
		} else {
			Log.w("GservicesProvider", "bad Gservices update URI: " + uri);
		}
		getContext()
				.sendBroadcast(
						new Intent(
								"com.google.gservices.intent.action.GSERVICES_CHANGED"));
		return 0;
	}

	private void updateMain(final ContentValues values) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GservicesProvider.updateMain");
	}

	private void updateMainDiff(final ContentValues values) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GservicesProvider.updateMainDiff");
	}

	private void updateOverride(final ContentValues values) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GservicesProvider.updateOverride");
	}

}
