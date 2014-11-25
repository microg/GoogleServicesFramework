/*
 * Copyright (c) 2014 μg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gsf.deviceid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.android.AndroidContext;

import java.util.HashMap;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String META_FIRST_TIME_CONFIG = "first_time_config";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "device_ids";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE device_spec (id INT PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE);");
        db.execSQL("CREATE TABLE device_spec_data (spec_id INT FOREIGN KEY REFERENCES device_spec.id, name TEXT, value TEXT);");
        db.execSQL("CREATE TABLE device_uniq (id INT PRIMARY KEY AUTOINCREMENT, spec_id INT FOREIGN KEY REFERENCES device_spec.id, name TEXT UNIQUE);");
        db.execSQL("CREATE TABLE device_uniq_link (uniq_id INT FOREIGN KEY REFERENCES device_uniq.id, link TEXT UNIQUE);");
        db.execSQL("CREATE TABLE device_uniq_data (uniq_id INT FOREIGN KEY REFERENCES device_uniq.id, name TEXT, value TEXT)");
        db.execSQL("CREATE TABLE meta_data (name TEXT UNIQUE, value TEXT);");
        putDeviceSpec("Samsung Galaxy Nexus (4.1.1)", AndroidContext.baseGalaxyNexus_16().asMap());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // no upgrades yet
    }

    public String getMeta(String name, String def) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor data = db.query("meta_data", new String[]{"value"}, "name=?", new String[]{name}, null, null, null);
        String result = def;
        if (data != null) {
            if (data.moveToNext()) {
                result = data.getString(0);
            }
            data.close();
        }
        db.close();
        return result;
    }

    public void putMeta(String name, String value) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("value", value);
        SQLiteDatabase db = getWritableDatabase();
        db.insertWithOnConflict("meta_data", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public long putDeviceSpec(String name, Map<String, String> data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        long id = db.insertWithOnConflict("device_spec", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0) {
            throw new RuntimeException("Could not add device spec");
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            values.clear();
            values.put("id", id);
            values.put("name", entry.getKey());
            values.put("value", entry.getValue());
            db.insertWithOnConflict("device_spec_data", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.close();
        return id;
    }

    public Map<String, String> getDeviceData(String link) {
        HashMap<String, String> result = new HashMap<String, String>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT device_spec_data.name, device_spec_data.value " +
                "FROM device_uniq_link JOIN device_uniq ON device_uniq.id = device_uniq_link.uniq_id " +
                "JOIN device_spec_data ON device_spec_data.spec_id = device_uniq.spec_id " +
                "WHERE device_uniq_link.link = ?", new String[]{link});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getString(1));
            }
            cursor.close();
        }
        cursor = db.rawQuery("SELECT device_uniq_data.name, device_uniq_data.value " +
                "FROM device_uniq_link JOIN device_uniq_data ON device_uniq_data.uniq_id = device_uniq_link.uniq_id " +
                "WHERE device_uniq_link.link = ?", new String[]{link});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getString(1));
            }
            cursor.close();
        }
        db.close();
        return result;
    }

    public long putDeviceUniq(String name, long specId, Map<String, String> data) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("spec_id", specId);
        long id = db.insertWithOnConflict("device_uniq", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id < 0) {
            throw new RuntimeException("Could not add device uniq");
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            values.clear();
            values.put("id", id);
            values.put("name", entry.getKey());
            values.put("value", entry.getValue());
            db.insertWithOnConflict("device_uniq_data", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
        db.close();
        return id;
    }
}
