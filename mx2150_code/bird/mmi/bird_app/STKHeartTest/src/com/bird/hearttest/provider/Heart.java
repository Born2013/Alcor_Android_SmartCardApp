package com.sensortek.stkhealthcare2.provider;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import java.util.List;
import java.util.LinkedList;
import android.util.Log;

public final class Heart implements Parcelable{

    public static final String TAG = "Heart";
    private static final String[] QUERY_COLUMNS = {
        HeartContract._ID,
        HeartContract.BPM,
        HeartContract.DATE_TIME
    };
    
    private static final String DEFAULT_SORT_ORDER =
            HeartContract._ID + " DESC";
    
    private static final int ID_INDEX = 0;
    private static final int BPM_INDEX = 1;
    private static final int DATE_TIME_INDEX = 2;
    private static final int COLUMN_COUNT = DATE_TIME_INDEX + 1;
    public long id;
    public int bpm;
    public String dateAndTime;
    
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
	
    public static CursorLoader getHeartCursorLoader(Context context) {
        return new CursorLoader(context, HeartContract.CONTENT_URI,
                null, null, null, DEFAULT_SORT_ORDER);
    }

    public static void addHeart(ContentResolver contentResolver, Heart heart) {
        ContentValues values = createContentValues(heart);
        Uri uri = contentResolver.insert(HeartContract.CONTENT_URI, values);
    }


    public static List<Heart> getHearts(ContentResolver contentResolver,
            String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(HeartContract.CONTENT_URI, null,
                selection, selectionArgs, DEFAULT_SORT_ORDER);
        List<Heart> result = new LinkedList<Heart>();
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    result.add(new Heart(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static List<Heart> getLastHearts(ContentResolver contentResolver,
            String selection, String ... selectionArgs) {
        Cursor cursor  = contentResolver.query(HeartContract.CONTENT_URI, null,
                selection, selectionArgs, null);
        List<Heart> result = new LinkedList<Heart>();
        Log.d(TAG,"cursor = "+cursor);
        if (cursor == null) {
            return result;
        }

        try {
            if (cursor.moveToLast()) {
                do {
                    result.add(new Heart(cursor));
                } while (cursor.moveToPrevious() && result.size() < 10);
            }
        } finally {
            cursor.close();
        }

        return result;
    }
    

    public static boolean isEmpty(ContentResolver contentResolver) {
        Cursor cursor  = contentResolver.query(HeartContract.CONTENT_URI, null,null, null, null);
        if (cursor == null) {
            return true;
        }

        try {
            if (cursor.moveToFirst()) {
                return false;
            } else {
                return true;
            }
        } finally {
            cursor.close();
        }
    }
    
    public static void deleteHearts(ContentResolver contentResolver) {
        contentResolver.delete(HeartContract.CONTENT_URI,null, null);
    }
    
    public static ContentValues createContentValues(Heart heart) {
        ContentValues values = new ContentValues();
        values.put(HeartContract.BPM, heart.bpm);
        values.put(HeartContract.DATE_TIME, heart.dateAndTime);
        return values;
    }


    public Heart(int bpm, String dataAndTime) {
        this.bpm = bpm;
        this.dateAndTime = dataAndTime;
    }
    
    public Heart(Cursor c) {
        id = c.getLong(ID_INDEX);
        bpm = c.getInt(BPM_INDEX);
        dateAndTime = c.getString(DATE_TIME_INDEX);
    }
}
