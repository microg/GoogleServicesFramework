package com.google.android.gsf.settings;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;

import com.google.android.gsf.gservices.GservicesProvider.DatabaseHelper;

public class GoogleSettingsProvider extends ContentProvider {

	private static class SqlArguments {

		public String[] args;
		public String table;
		public String where;

		public SqlArguments(final Uri uri) {
			this(uri, null, null);
		}

		public SqlArguments(final Uri uri, final String selection,
				final String[] selectionArgs) {
			// TODO Auto-generated constructor stub
			throw new RuntimeException("Not yet implemented: SqlArguments.ctor");
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
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GoogleSettingsProvider.query");
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
