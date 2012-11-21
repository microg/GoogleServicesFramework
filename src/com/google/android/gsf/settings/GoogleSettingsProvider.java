package com.google.android.gsf.settings;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Settings;

import com.google.android.gsf.GoogleSettingsContract;

public class GoogleSettingsProvider extends ContentProvider {

	private static class SqlArguments {

		public String[] args;
		public String table;
		public String where;

		SqlArguments(final Uri uri) {
			if (uri.getPathSegments().size() != 1) {
				throw new IllegalArgumentException("Invalid URI: " + uri);
			}
			table = uri.getPathSegments().get(0);
			where = null;
			args = null;
		}

		SqlArguments(final Uri uri, final String selection,
				final String[] selectionArgs) {
			table = uri.getPathSegments().get(0);
			if (uri.getPathSegments().size() == 1) {
				where = selection;
				args = selectionArgs;
			} else {
				if (uri.getPathSegments().size() != 2) {
					throw new IllegalArgumentException("Invalid URI: " + uri);
				}
				if (selection != null && !selection.isEmpty()) {
					throw new UnsupportedOperationException(
							(new StringBuilder())
									.append("WHERE clause not supported: ")
									.append(uri).toString());
				}
				if (table.equals("partner")) {
					where = "name=?";
					args = new String[] { uri.getPathSegments().get(1) };
				} else {
					where = "_id=" + ContentUris.parseId(uri);
					args = null;
				}
			}
		}

	}

	private boolean linkAssistedGps = false;
	private DatabaseHelper openHelper;

	private void checkWritePermissions(final SqlArguments arguments) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GoogleSettingsProvider.checkWritePermissions");
	}

	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		final SqlArguments arguments = new SqlArguments(uri, selection,
				selectionArgs);
		checkWritePermissions(arguments);
		final int result = openHelper.getWritableDatabase().delete(
				arguments.table, arguments.where, arguments.args);
		if (result > 0) {
			sendNotify(uri);
		}
		return result;
	}

	@Override
	public String getType(final Uri uri) {
		final SqlArguments arguments = new SqlArguments(uri);
		if (arguments.where == null || arguments.where.isEmpty()) {
			return "vnd.android.cursor.dir/" + arguments.table;
		}
		return "vnd.android.cursor.item/" + arguments.table;
	}

	private Uri getUriFor(final Uri uri, final ContentValues values,
			final long l) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GoogleSettingsProvider.getUriFor");
	}

	@Override
	public Uri insert(Uri uri, final ContentValues values) {
		final SqlArguments arguments = new SqlArguments(uri);
		checkWritePermissions(arguments);
		final long l = openHelper.getWritableDatabase().insert(arguments.table,
				null, values);
		if (l > 0) {
			uri = getUriFor(uri, values, l);
			sendNotify(uri);
			return uri;
		}
		return null;
	}

	@Override
	public boolean onCreate() {
		openHelper = new DatabaseHelper(getContext());
		final String agps = SystemProperties.get("ro.gps.agps_provider", null);
		if (agps == null || agps.isEmpty()
				|| agps.toLowerCase().contains("google")
				|| agps.toLowerCase().contains("μg")) {
			linkAssistedGps = true;
		}
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		SqlArguments sqlargs = new SqlArguments(uri, selection, selectionArgs);
		SQLiteDatabase db = openHelper.getReadableDatabase();
		ContentResolver resolver = getContext().getContentResolver();
		if (openHelper.assistedGpsSettingNeedsUpdate() && linkAssistedGps) {
			boolean localSetting = (GoogleSettingsContract.Partner.getInt(
					resolver, "network_location_opt_in", 0) == 1);
			boolean secureSetting = (Settings.Secure.getInt(resolver,
					"assisted_gps_enabled", 1) != 0);
			if (localSetting != secureSetting) {
				Settings.Secure.putInt(resolver, "assisted_gps_enabled",
						localSetting ? 1 : 0);
			}
		}
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(sqlargs.table);
		Cursor cursor = queryBuilder.query(db, projection, sqlargs.where,
				sqlargs.args, null, null, sortOrder);
		cursor.setNotificationUri(resolver, uri);
		return cursor;
	}

	private void sendNotify(final Uri uri) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GoogleSettingsProvider.sendNotify");
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		final SqlArguments arguments = new SqlArguments(uri, selection,
				selectionArgs);
		checkWritePermissions(arguments);
		final int result = openHelper.getWritableDatabase().update(
				arguments.table, values, arguments.where, arguments.args);
		if (result > 0) {
			sendNotify(uri);
		}
		return result;
	}

}
