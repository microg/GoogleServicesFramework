package com.google.android.gsf.settings;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.google.android.gsf.R;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static int DATABASE_VERSION = 2;
	private static int NEEDS_UPDATE_VERSION_BELOW = 2;

	private boolean assistedGpsSettingNeedsUpdate;
	private Context context;

	public DatabaseHelper(Context context) {
		super(context, "googlesettings.db", null, DATABASE_VERSION);
		this.context = context;
	}

	private void insertDefaultPartnerSettings(SQLiteDatabase db) {
		SQLiteStatement statement = db
				.compileStatement("INSERT OR IGNORE INTO partner(name,value) VALUES(?,?);");
		context.getResources();
		loadStringSetting(statement, "client_id", R.string.def_client_id);
		String s = android.provider.Settings.Secure.getString(
				context.getContentResolver(), "logging_id2");
		if (s != null) {
			loadSetting(statement, "logging_id2", s);
		}
		statement.close();
	}

	private void loadSetting(SQLiteStatement statement, String name,
			Object value) {
		statement.bindString(1, name);
		statement.bindString(2, value.toString());
		statement.execute();
	}

	private void loadStringSetting(SQLiteStatement statement, String name,
			int resourceId) {
		loadSetting(statement, name,
				context.getResources().getString(resourceId));
	}

	final boolean assistedGpsSettingNeedsUpdate() {
		return assistedGpsSettingNeedsUpdate;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE partner (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
		db.execSQL("CREATE INDEX partnerIndex1 ON partner (name);");
		insertDefaultPartnerSettings(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < NEEDS_UPDATE_VERSION_BELOW) {
			assistedGpsSettingNeedsUpdate = true;
		}
	}
}
