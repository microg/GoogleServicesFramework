package com.google.android.gsf.gservices;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final int DB_VERSION = 3;
	private static final int DB_VERSION_OLD = 1;

	public DatabaseHelper(final Context context) {
		super(context, "gservices.db", null, DB_VERSION);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL("CREATE TABLE main (name TEXT PRIMARY KEY, value TEXT)");
		db.execSQL("CREATE TABLE overrides (name TEXT PRIMARY KEY, value TEXT)");
		db.execSQL("CREATE TABLE saved_system (name TEXT PRIMARY KEY, value TEXT)");
		db.execSQL("CREATE TABLE saved_secure (name TEXT PRIMARY KEY, value TEXT)");
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		if (oldVersion == DB_VERSION_OLD) {
			db.execSQL("DROP TABLE IF EXISTS main");
			db.execSQL("DROP TABLE IF EXISTS overrides");
			onCreate(db);
		}
		db.setVersion(newVersion);
	}
}