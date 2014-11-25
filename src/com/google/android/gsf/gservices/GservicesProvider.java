package com.google.android.gsf.gservices;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

public class GservicesProvider extends ContentProvider {

	private static final BigInteger GOOGLE_SERIAL = new BigInteger("1228183678");
	private static final char[] HEX_CHARS =
			new char[]{'0', '1', '2', '3', '4', '3', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	private static final int KEY_COLUMN = 0;
	private static final String TAG = "GoogleServicesProvider";
	public static final Uri UPDATE_MAIN_DIFF_URI = Uri.parse("content://com.google.android.gsf.gservices/main_diff");
	public static final Uri UPDATE_MAIN_URI = Uri.parse("content://com.google.android.gsf.gservices/main");
	public static final Uri UPDATE_OVERRIDE_URI = Uri.parse("content://com.google.android.gsf.gservices/override");
	private static final int VALUE_COLUMN = 1;

	private DatabaseHelper dbHelper;
	private boolean pushToSecure = false;
	private boolean pushToSystem = false;
	private TreeMap<String, String> values;

	private final Object valuesLock = new Object();
    private MessageDigest md;

    private boolean checkCallingPermission(final String permission) {
        return getContext().checkCallingPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

	private boolean checkPermissionOrSignature(final String permission, final BigInteger... serials) {
        if (checkCallingPermission(permission)) {
            return true;
        }
        for (BigInteger serial : serials) {
			if (checkSignature(serial)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkReadPermission() {
        return checkPermissionOrSignature("com.google.android.providers.gsf.permission.READ_GSERVICES",
                getSignatureSerials("com.google.android.gsf", GOOGLE_SERIAL));
    }

    private BigInteger[] getSignatureSerials(String packageName, BigInteger additionalSerial) {
        ArrayList<BigInteger> serials = new ArrayList<BigInteger>();
        try {
            final PackageManager pm = getContext().getPackageManager();
            final Signature[] sigs = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
			final CertificateFactory factory = CertificateFactory.getInstance("X509");
			for (final Signature sig : sigs) {
				try {
					final X509Certificate cert =
							(X509Certificate) factory.generateCertificate(new ByteArrayInputStream(sig.toByteArray()));
					if (cert.getSerialNumber() != null) {
						serials.add(cert.getSerialNumber());
					}
				} catch (Exception e) {
					// Try next
				}
			}
		} catch (Exception e) {
			// Ignore
		}
        if (additionalSerial == null) {
            return serials.toArray(new BigInteger[serials.size()]);
        } else {
            BigInteger[] result = serials.toArray(new BigInteger[serials.size() + 1]);
            result[serials.size()] = additionalSerial;
            return result;
        }
    }

	private boolean checkSignature(final BigInteger serial) {
		try {
			final PackageManager pm = getContext().getPackageManager();
			final String packageName = pm.getNameForUid(Binder.getCallingUid());
            final BigInteger[] serials = getSignatureSerials(packageName, null);
            for (BigInteger s : serials) {
                if (s.equals(serial)) {
                    return true;
				}
			}
		} catch (final Throwable t) {
		}
		return false;
	}

	private boolean checkWritePermission() {
        return checkPermissionOrSignature("com.google.android.providers.gsf.permission.WRITE_GSERVICES",
                getSignatureSerials("com.google.android.gsf", GOOGLE_SERIAL));
    }

    private boolean computeLocalDigestAndUpdateValues() {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final TreeMap<String, String> map = new TreeMap<String, String>();
        md.reset();
        String oldDigest = null;
		db.beginTransaction();
		Cursor cursor = db.rawQuery("SELECT name, value FROM main ORDER BY name", null);
		try {
			while (cursor.moveToNext()) {
				final String key = cursor.getString(KEY_COLUMN);
				final String value = cursor.getString(VALUE_COLUMN);
				if (!key.equals("digest")) {
					md.update(key.getBytes());
					md.update((byte) 0);
					md.update(value.getBytes());
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

		if (!digest.equals(oldDigest)) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", "digest");
            contentValues.put("value", digest);
            db.insertWithOnConflict("main", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
        cursor = db.rawQuery("SELECT name, value FROM overrides", null);
        try {
			while (cursor.moveToNext()) {
				map.put(cursor.getString(KEY_COLUMN), cursor.getString(VALUE_COLUMN));
			}
		} finally {
			cursor.close();
		}
		synchronized (valuesLock) {
			values = map;
		}
		db.setTransactionSuccessful();
		db.endTransaction();
        return !digest.equals(oldDigest);
    }

	@Override
	public int delete(final Uri arg0, final String arg1, final String[] arg2) {
		throw new UnsupportedOperationException();
	}

	private String getPrefixLimit(final String string) {
		for (int i = string.length() - 1; i > 0; i--) {
			final char c = string.charAt(i);
			if (c < '\uFFFF') {
				return string.substring(0, i) + (char) (c + 1);
			}
		}
		return null;
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
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            Log.w(TAG, "Can't hash digest, this will cause problems!", e);
        }
		dbHelper = new DatabaseHelper(getContext());
		final int pid = Process.myPid();
		final int uid = Process.myUid();
        if (getContext().checkPermission("android.permission.WRITE_SETTINGS", pid, uid) == PackageManager.PERMISSION_GRANTED) {
            pushToSystem = true;
        }
        if (getContext().checkPermission("android.permission.WRITE_SECURE_SETTINGS", pid, uid) == PackageManager.PERMISSION_GRANTED) {
            pushToSecure = true;
        }
        computeLocalDigestAndUpdateValues();
        return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
						final String sortOrder) {
		if (!checkReadPermission()) {
            Log.d(TAG, "no permission to read during query(" + uri + ", " + projection + ", " + selection + ", " +
                    selectionArgs + ", " + sortOrder + ")");
            throw new UnsupportedOperationException();
        }
        final MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
		if (selectionArgs != null) {
			final String lastSegment = uri.getLastPathSegment();
			if (lastSegment == null) {
				querySimple(cursor, selectionArgs);
			} else if (lastSegment.equals("prefix")) {
				queryPrefix(cursor, selectionArgs);
			}
		}
		return cursor;
	}

	private void queryPrefix(final MatrixCursor cursor, final String... selectionArgs) {
		for (final String arg : selectionArgs) {
			final String limit = getPrefixLimit(arg);
			SortedMap<String, String> sortedmap;
			if (limit == null) {
				sortedmap = values.tailMap(arg);
			} else {
				sortedmap = values.subMap(arg, limit);
			}
			for (final Entry<String, String> entry : sortedmap.entrySet()) {
				cursor.addRow(new String[]{entry.getKey(), entry.getValue()});
			}
		}
	}

	private void querySimple(final MatrixCursor cursor, final String[] keys) {
		synchronized (valuesLock) {
			for (final String key : keys) {
				cursor.addRow(new String[]{key, values.get(key)});
			}
		}
	}

	private void syncAllSettings() {
		if (pushToSystem) {
			syncSettings(android.provider.Settings.System.CONTENT_URI, "system:", "saved_system");
		}
		if (pushToSecure) {
			syncSettings(android.provider.Settings.Secure.CONTENT_URI, "secure:", "saved_secure");
        }
	}

	private void syncSettings(final Uri uri, final String prefix, final String table) {
        final MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        queryPrefix(cursor, prefix);
        // TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GservicesProvider.syncSettings");
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		if (!checkWritePermission()) {
            Log.d(TAG, "no permission to write during update(" + uri + ", " + values + ", " + selection + ", " +
                    selectionArgs + ")");
            throw new UnsupportedOperationException();
        }
        final String lastSegment = uri.getLastPathSegment();
        if (lastSegment.equals("main") && updateMain(values) ||
                lastSegment.equals("main_diff") && updateMainDiff(values) ||
                lastSegment.equals("override") && updateOverride(values)) {
            getContext().sendBroadcast(new Intent("com.google.gservices.intent.action.GSERVICES_CHANGED"));
            return 1;
        }
        return 0;
    }

    private boolean updateMain(final ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insertWithOnConflict("main", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
        if (computeLocalDigestAndUpdateValues()) {
            Log.d(TAG, "changed " + values.get("name") + " to " + values.get("value") + " and digest is now " + this.values.get("digest"));
            return true;
        }
        return false;
    }

    private boolean updateMainDiff(final ContentValues values) {
        Log.w(TAG, "Not yet implemented: GservicesProvider.updateMainDiff: " + values);
        return false;
    }

    private boolean updateOverride(final ContentValues values) {
        Log.w(TAG, "Not yet implemented: GservicesProvider.updateOverride: " + values);
        return false;
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
                context.getContentResolver().update(UPDATE_OVERRIDE_URI, values, null, null);
            }
        }

    }
}
