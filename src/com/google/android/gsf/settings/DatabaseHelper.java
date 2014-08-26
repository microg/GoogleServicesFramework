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
	private final Context context;

	public DatabaseHelper(final Context context) {
		super(context, "googlesettings.db", null, DATABASE_VERSION);
		this.context = context;
	}

	final boolean assistedGpsSettingNeedsUpdate() {
		return assistedGpsSettingNeedsUpdate;
	}

	private void insertDefaultPartnerSettings(final SQLiteDatabase db) {
		final SQLiteStatement statement = db.compileStatement("INSERT OR IGNORE INTO partner(name,value) VALUES(?,?);");
		context.getResources();
		loadStringSetting(statement, "client_id", R.string.def_client_id);
		final String s = android.provider.Settings.Secure.getString(context.getContentResolver(), "logging_id2");
		if (s != null) {
			loadSetting(statement, "logging_id2", s);
		}
		statement.close();
	}

	private void loadSetting(final SQLiteStatement statement, final String name, final Object value) {
		statement.bindString(1, name);
		statement.bindString(2, value.toString());
		statement.execute();
	}

	private void loadStringSetting(final SQLiteStatement statement, final String name, final int resourceId) {
		loadSetting(statement, name, context.getResources().getString(resourceId));
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		db.execSQL(
				"CREATE TABLE partner (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
		db.execSQL("CREATE INDEX partnerIndex1 ON partner (name);");
		insertDefaultPartnerSettings(db);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		if (oldVersion < NEEDS_UPDATE_VERSION_BELOW) {
			assistedGpsSettingNeedsUpdate = true;
		}
	}
}
