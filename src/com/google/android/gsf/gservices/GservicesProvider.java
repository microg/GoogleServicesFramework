package com.google.android.gsf.gservices;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import com.google.android.gsf.Gservices;

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

	private static final Charset CHAR_ENC = Charset.forName("UTF-8");
	private static final String[] COLUMNS = new String[] { "key", "value" };
	private static final BigInteger GOOGLE_SERIAL = new BigInteger("1228183678");
	private static final String HASH = "SHA-1";
	private static final char[] HEX_CHARS = new char[] { '0', '1', '2', '3',
			'4', '3', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static final int KEY_COLUMN = 0;
	public static final Uri UPDATE_MAIN_DIFF_URI = Uri
			.parse("content://com.google.android.gsf.gservices/main_diff");
	public static final Uri UPDATE_MAIN_URI = Uri
			.parse("content://com.google.android.gsf.gservices/main");
	public static final Uri UPDATE_OVERRIDE_URI = Uri
			.parse("content://com.google.android.gsf.gservices/override");
	private static final int VALUE_COLUMN = 1;
	private DatabaseHelper dbHelper;
	private boolean pushToSecure = false;
	private boolean pushToSystem = false;
	private TreeMap<String, String> values;
	private final Object valuesLock = new Object();

	private boolean checkPermission(final String permission) {
		return getContext().checkCallingPermission(permission) == 0;
	}

	private boolean checkPermissionOrSignature(final String permission,
			final BigInteger serial) {
		if (checkPermission("com.google.android.providers.gsf.permission.READ_GSERVICES")) {
			return true;
		}
		if (checkSignature(serial)) {
			return true;
		}
		return false;
	}

	private boolean checkReadPermission() {
		return checkPermissionOrSignature(
				"com.google.android.providers.gsf.permission.READ_GSERVICES",
				GOOGLE_SERIAL);
	}

	private boolean checkSignature(final BigInteger serial) {
		try {
			final PackageManager pm = getContext().getPackageManager();
			final String packageName = pm.getNameForUid(Binder.getCallingUid());
			final Signature[] sigs = pm.getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES).signatures;
			final CertificateFactory factory = CertificateFactory
					.getInstance("X509");
			for (final Signature sig : sigs) {
				try {
					final X509Certificate cert = (X509Certificate) factory
							.generateCertificate(new ByteArrayInputStream(sig
									.toByteArray()));
					Log.d("GoogleServicesProvider", packageName
							+ "::Signature: {Subject: '" + cert.getSubjectDN()
							+ "', Serial: '" + cert.getSerialNumber() + "'}");
					if (cert.getSerialNumber().equals(serial)) {
						Log.d("GoogleServicesProvider", packageName
								+ " has a valid Google Certificate.");
						return true;
					}
				} catch (final Throwable t) {
				}
			}
		} catch (final Throwable t) {
		}
		return false;
	}

	private boolean checkWritePermission() {
		return checkPermissionOrSignature(
				"com.google.android.providers.gsf.permission.WRITE_GSERVICES",
				GOOGLE_SERIAL);
	}

	private void computeLocalDigestAndUpdateValues(final DatabaseHelper dbHelper) {
		final SQLiteDatabase db = dbHelper.getWritableDatabase();
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(HASH);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
		final TreeMap<String, String> map = new TreeMap<String, String>();
		String oldDigest = null;
		db.beginTransaction();
		Cursor cursor = db.rawQuery(
				"SELECT name, value FROM main ORDER BY name", null);
		try {
			while (cursor.moveToNext()) {
				final String key = cursor.getString(KEY_COLUMN);
				final String value = cursor.getString(VALUE_COLUMN);
				if (!key.equals("digest")) {
					md.update(key.getBytes(CHAR_ENC));
					md.update((byte) 0);
					md.update(value.getBytes(CHAR_ENC));
					md.update((byte) 0);
				} else {
					oldDigest = value;
				}
				map.put(key, value);
			}
		} finally {
			cursor.close();
		}
		final StringBuilder sb = new StringBuilder("1-");
		final byte[] hash = md.digest();
		for (final byte element : hash) {
			sb.append(HEX_CHARS[0xf & element >> 4]);
			sb.append(HEX_CHARS[element & 0xf]);
		}
		final String digest = sb.toString();
		map.put("digest", digest);
		if (digest.equals(oldDigest)) {
			final SQLiteStatement statement = db
					.compileStatement("INSERT OR REPLACE INTO main (name, value) VALUES (?, ?)");
			statement.bindString(1, "digest");
			statement.bindString(2, digest);
			statement.execute();
			statement.close();
		}
		cursor = db.rawQuery("SELECT name, value FROM overrides", null);
		try {
			while (cursor.moveToNext()) {
				map.put(cursor.getString(KEY_COLUMN),
						cursor.getString(VALUE_COLUMN));
			}
		} finally {
			cursor.close();
		}
		synchronized (valuesLock) {
			values = map;
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

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
		computeLocalDigestAndUpdateValues(dbHelper);
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		if (!checkReadPermission()) {
			throw new UnsupportedOperationException();
		}
		final MatrixCursor cursor = new MatrixCursor(COLUMNS);
		if (selectionArgs != null) {
			final String lastSegment = uri.getLastPathSegment();
			if (lastSegment == null) {
				querySimple(cursor, selectionArgs);
			} else if (lastSegment.equals(Gservices.CONTENT_PREFIX_URI
					.getLastPathSegment())) {
				queryPrefix(cursor, selectionArgs);
			}
		}
		return cursor;
	}

	private void queryPrefix(final MatrixCursor cursor,
			final String[] selectionArgs) {
		String sa = "";
		for (String string : selectionArgs) {
			sa += string + " : ";
		}
		sa = sa.substring(0, sa.length() - 3);
		for (String arg : selectionArgs) {
			String limit = getPrefixLimit(arg);
			SortedMap<String, String> sortedmap;
			if (limit == null) {
				sortedmap = values.tailMap(arg);
			} else {
				sortedmap = values.subMap(arg, limit);
			}
			for (Entry<String, String> entry : sortedmap.entrySet()) {
				cursor.addRow(new String[] { entry.getKey(), entry.getValue() });
			}
		}
	}

	private String getPrefixLimit(String string) {
		for (int i = string.length() - 1; i > 0; i--) {
			char c = string.charAt(i);
			if (c < '\uFFFF') {
				return string.substring(0, i) + (char) (c + 1);
			}
		}
		return null;
	}

	private void querySimple(final MatrixCursor cursor, final String[] keys) {
		synchronized (valuesLock) {
			for (final String key : keys) {
				cursor.addRow(new String[] { key, values.get(key) });
			}
		}
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		if (!checkWritePermission()) {
			throw new UnsupportedOperationException();
		}
		final String lastSegment = uri.getLastPathSegment();
		if (lastSegment.equals("main")) {
			updateMain(values);
		} else if (lastSegment.equals("main_diff")) {
			updateMainDiff(values);
		} else if (lastSegment.equals("override")) {
			updateOverride(values);
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

	private void syncAllSettings() {
		if (pushToSystem) {
			syncSettings(android.provider.Settings.System.CONTENT_URI,
					"system:", "saved_system");
		}
		if (pushToSecure) {
			syncSettings(android.provider.Settings.Secure.CONTENT_URI,
					"secure:", "saved_secure");
		}
	}

	private void syncSettings(Uri uri, String prefix, String table) {
		// TODO Auto-generated method stub
		throw new RuntimeException(
				"Not yet implemented: GservicesProvider.syncSettings");
	}

}
