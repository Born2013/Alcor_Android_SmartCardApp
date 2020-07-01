/*[BIRD][BIRD_FINGER_PRINT][指纹功能拓展][yangbo][20160930]BEGIN */
package com.android.server.fingerprint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.content.ContentValues;
import android.database.Cursor;

public class BirdFingerprintDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "BirdFingerprintDatabaseHelper";

    private static final String DATABASE_NAME = "birdfingerprint.db";
    
    public static final String COLUMN_ID = "_id";
    
    public static final class FEATURES {
        public static final String TABLE_NAME = "features";
        public static final String KEY_FINGERID = "fingerId";
        public static final String KEY_GROUPID = "groupId";
        public static final String KEY_MAKE_CALL = "make_call";
        public static final String KEY_ANSWER_CALL = "answer_call";
        public static final String KEY_QUICK_START = "quick_start";
        public static final String KEY_TAKE_PHOTO = "take_photo";
        public static final String KEY_CALL_RECORDING = "call_recording";
    }
    
    private static final String uxCreateString = "CREATE TABLE " + FEATURES.TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY," 
                + FEATURES.KEY_FINGERID + " INTEGER,"
                + FEATURES.KEY_GROUPID + " INTEGER,"
                + FEATURES.KEY_MAKE_CALL + " TEXT," 
                + FEATURES.KEY_ANSWER_CALL + " INTEGER,"
                + FEATURES.KEY_QUICK_START + " TEXT,"
                + FEATURES.KEY_TAKE_PHOTO + " INTEGER,"
                + FEATURES.KEY_CALL_RECORDING + " INTEGER)";

    private static final int DEFAULT_VERSION = 1;

    public BirdFingerprintDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DEFAULT_VERSION);
        Log.d(TAG, "[BirdFingerprintDatabaseHelper]...");
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
    
    public boolean isInDb(int fingerId, int groupId) {
		String selectQuery = "SELECT * FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
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
    
    public long insert(int fingerId, int groupId) {
        synchronized(this) {
            if (isInDb(fingerId, groupId)) {
				Log.i(TAG, "insert already in db");
				return -1;
			}
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(FEATURES.KEY_FINGERID, fingerId);
            values.put(FEATURES.KEY_GROUPID, groupId);
            values.put(FEATURES.KEY_MAKE_CALL, "");
            values.put(FEATURES.KEY_ANSWER_CALL, 0);
            values.put(FEATURES.KEY_QUICK_START, "");
            values.put(FEATURES.KEY_TAKE_PHOTO, 0);
            values.put(FEATURES.KEY_CALL_RECORDING, 0);
            try {
                return db.insert(FEATURES.TABLE_NAME, null, values);
            } finally {
                db.close();
            }
        }
    }
    
    public boolean delete(int fingerId, int groupId) {
        synchronized(this) {
            SQLiteDatabase db = getWritableDatabase();
            String clause = FEATURES.KEY_FINGERID
                    + "='" + fingerId + "' and " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            try {
                return db.delete(FEATURES.TABLE_NAME, clause, null) != 0;
            } finally {
                db.close();
            }
        }
    }
    
    public boolean update(int fingerId, int groupId, ContentValues values) {
        synchronized(this) {
            if (!isInDb(fingerId, groupId)) {
				Log.i(TAG, "update is not is db");
				return false;
			}
			SQLiteDatabase db = getWritableDatabase();
			String selection = FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
			try {
			    db.update(FEATURES.TABLE_NAME, values, selection, null);
			} finally {
                db.close();
            }
            return true;
        }
    }
    
    public boolean isAnswerCallEnable(int fingerId, int groupId) {
        synchronized(this) {
            // Find the corresponding sound model ID for the keyphrase.
            String selectQuery = "SELECT " + FEATURES.KEY_ANSWER_CALL + " FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getInt(c.getColumnIndex(FEATURES.KEY_ANSWER_CALL)) == 1;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
                db.close();
            }
            return false;
        }
    }
    
    public String getMakeCallNumber(int fingerId, int groupId) {
        synchronized(this) {
            // Find the corresponding sound model ID for the keyphrase.
            String selectQuery = "SELECT " + FEATURES.KEY_MAKE_CALL + " FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getString(c.getColumnIndex(FEATURES.KEY_MAKE_CALL));
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
    
    public String getQuickStartPackageName(int fingerId, int groupId) {
        synchronized(this) {
            // Find the corresponding sound model ID for the keyphrase.
            String selectQuery = "SELECT " + FEATURES.KEY_QUICK_START + " FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getString(c.getColumnIndex(FEATURES.KEY_QUICK_START));
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
    
    public boolean isTakePhotoEnable(int fingerId, int groupId) {
        synchronized(this) {
            // Find the corresponding sound model ID for the keyphrase.
            String selectQuery = "SELECT " + FEATURES.KEY_TAKE_PHOTO + " FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getInt(c.getColumnIndex(FEATURES.KEY_TAKE_PHOTO)) == 1;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
                db.close();
            }
            return false;
        }
    }
    
    public boolean isCallRecordingEnable(int fingerId, int groupId) {
        synchronized(this) {
            // Find the corresponding sound model ID for the keyphrase.
            String selectQuery = "SELECT " + FEATURES.KEY_CALL_RECORDING + " FROM " + FEATURES.TABLE_NAME
                    + " WHERE " + FEATURES.KEY_FINGERID + "='" + fingerId
                    + "' AND " + FEATURES.KEY_GROUPID + "='" + groupId + "'";
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(selectQuery, null);
            try {
                if (c.moveToFirst()) {
                    return c.getInt(c.getColumnIndex(FEATURES.KEY_CALL_RECORDING)) == 1;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
                db.close();
            }
            return false;
        }
    }
    
    public boolean setAnswerCallEnable(int fingerId, int groupId, boolean enable) {
        ContentValues values = new ContentValues();
        values.put(FEATURES.KEY_ANSWER_CALL, enable ? 1 : 0);
        return update(fingerId, groupId, values);
    }
    
    public boolean setMakeCallNumber(int fingerId, int groupId, String number) {
        ContentValues values = new ContentValues();
        values.put(FEATURES.KEY_MAKE_CALL, number == null ? "" : number);
        return update(fingerId, groupId, values);
    }
    
    public boolean setQuickStartPackageName(int fingerId, int groupId, String packageName) {
        ContentValues values = new ContentValues();
        values.put(FEATURES.KEY_QUICK_START, packageName == null ? "" : packageName);
        return update(fingerId, groupId, values);
    }
    
    public boolean setTakePhotoEnable(int fingerId, int groupId, boolean enable) {
        ContentValues values = new ContentValues();
        values.put(FEATURES.KEY_TAKE_PHOTO, enable ? 1 : 0);
        return update(fingerId, groupId, values);
    }
    
    public boolean setCallRecordingEnable(int fingerId, int groupId, boolean enable) {
        ContentValues values = new ContentValues();
        values.put(FEATURES.KEY_CALL_RECORDING, enable ? 1 : 0);
        return update(fingerId, groupId, values);
    }

}
/*[BIRD][BIRD_FINGER_PRINT][指纹功能拓展][yangbo][20160930]END */
