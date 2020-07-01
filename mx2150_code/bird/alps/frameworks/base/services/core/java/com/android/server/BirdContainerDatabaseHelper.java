/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
package com.android.server;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.content.ContentValues;
import android.database.Cursor;

public class BirdContainerDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "BirdContainerDatabaseHelper";

    private static final String DATABASE_NAME = "birdcontainer.db";
    
    public static final String COLUMN_ID = "_id";
    
    public static final class PARAMETER {
        public static final String TABLE_NAME = "parameter";
        public static final String KEY = "key";
        public static final String VALUE = "value";
    }
    
    private static final String uxCreateString = "CREATE TABLE " + PARAMETER.TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY," 
                + PARAMETER.KEY + " TEXT," 
                + PARAMETER.VALUE + " TEXT)";

    private static final int DEFAULT_VERSION = 1;

    public BirdContainerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DEFAULT_VERSION);
        Log.d(TAG, "[BirdContainerDatabaseHelper]...");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "[onCreate]...");
        db.execSQL(uxCreateString);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "[onUpgrade] oldVersion : " + oldVersion + ", newVersion : " + newVersion);
    }
    
    public boolean isInDb(String key) {
        String selectQuery = "SELECT * FROM " + PARAMETER.TABLE_NAME
                    + " WHERE " + PARAMETER.KEY + "='" + key + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        try {
            if (c == null) {
                Log.w(TAG, "[isInDb] c is null,return.");
                return false;
            }
            if (c.getCount() == 0) {
                Log.i(TAG, "[isInDb] get count is 0.");
                return false;
            }
            Log.i(TAG, "[isInDb] device is alread in db.");
            return true;
        } finally {
            if (c != null) {
                c.close();
            }
            db.close();
        }
    }
    
    public boolean saveData(String key, String value) {
        synchronized(this) {
            if (isInDb(key)) {
                return updateData(key, value);
            } else {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(PARAMETER.KEY, key);
                values.put(PARAMETER.VALUE, value);
                try {
                    db.insert(PARAMETER.TABLE_NAME, null, values);
                    return true;
                } catch (Exception e) {
                    return false;
                } finally {
                    db.close();
                }
            }
        }
    }
    
    public String getData(String key) {
        synchronized(this) {
            String selectQuery = "SELECT " + PARAMETER.VALUE + " FROM " + PARAMETER.TABLE_NAME
                    + " WHERE " + PARAMETER.KEY + "='" + key + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getString(c.getColumnIndex(PARAMETER.VALUE));
                }
            } finally {
                if (c != null) {
                    c.close();
                }
                db.close();
            }
            return "";
            
        }
    }
    
    public boolean clearData(String key) {
        synchronized(this) {
            SQLiteDatabase db = getWritableDatabase();
            String clause = PARAMETER.KEY + "='" + key + "'";
            try {
                return db.delete(PARAMETER.TABLE_NAME, clause, null) != 0;
            } finally {
                db.close();
            }
        }
    }
    
    public void clearAllData() {
        synchronized(this) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.execSQL("DELETE FROM " + PARAMETER.TABLE_NAME +";");
            } finally {
                db.close();
            }
        }
    }
    
    private boolean updateData(String key, String value) {
        synchronized(this) {
            SQLiteDatabase db = getWritableDatabase();
            String selection = PARAMETER.KEY + "='" + key + "'";
            ContentValues values = new ContentValues();
            values.put(PARAMETER.VALUE, value);
            try {
                db.update(PARAMETER.TABLE_NAME, values, selection, null);
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                db.close();
            }
        }
    }

}
/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */
