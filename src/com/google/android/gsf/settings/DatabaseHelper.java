package com.google.android.gsf.settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.Settings;
import com.google.android.gsf.R;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static int DATABASE_VERSION = 2;

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
        {
            ContentValues cv = new ContentValues();
            cv.put("name", "client_id");
            cv.put("value", "android-google");
            db.insertWithOnConflict("partner", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        final String s = Settings.Secure.getString(context.getContentResolver(), "logging_id2");
        if (s != null) {
            ContentValues cv = new ContentValues();
            cv.put("name", "logging_id2");
            cv.put("value", s);
            db.insertWithOnConflict("partner", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE partner (_id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE ON CONFLICT REPLACE,value TEXT);");
        insertDefaultPartnerSettings(db);
	}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
