package com.sensortek.stkhealthcare2.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class HeartProvider extends ContentProvider {
    
	
	public static final int HEART_DIR = 1;
	public static final int HEART_ITEM = 2;
	
    private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final String TAG = "HeartProvider";
    static {
        sURLMatcher.addURI(HeartContract.AUTHORITY, "heart", HEART_DIR);
        sURLMatcher.addURI(HeartContract.AUTHORITY, "heart/#", HEART_ITEM);
    }
    private MyDatabaseHelper mdbHelper;
    
    public HeartProvider() {
    }

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
        int count;
        String primaryKey;
        SQLiteDatabase db = mdbHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case HEART_DIR:
                count = db.delete(HeartContract.HEART_TABLE_NAME, where, whereArgs);
                break;
            case HEART_ITEM:
                primaryKey = uri.getLastPathSegment();
                if (TextUtils.isEmpty(where)) {
                    where = HeartContract._ID + "=" + primaryKey;
                } else {
                    where = HeartContract._ID + "=" + primaryKey +
                            " AND (" + where + ")";
                }
                count = db.delete(HeartContract.HEART_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}

	@Override
	public String getType(Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
            case HEART_DIR:
                return "vnd.android.cursor.dir/heart";
            case HEART_ITEM:
                return "vnd.android.cursor.item/heart";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = mdbHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case HEART_DIR:
            case HEART_ITEM:
            	rowId = db.insert(HeartContract.HEART_TABLE_NAME, null, initialValues);               
                break;
            default:
                throw new IllegalArgumentException("Cannot insert from URL: " + uri);
        }

        Uri uriResult = ContentUris.withAppendedId(HeartContract.CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(uriResult, null);
        return uriResult;
	}

	@Override
	public boolean onCreate() {
                Log.d(TAG,"onCreate");
		mdbHelper = new MyDatabaseHelper(getContext());
       mdbHelper.getWritableDatabase();
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projectionIn, String selection,
			String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query
        int match = sURLMatcher.match(uri);
        switch (match) {
            case HEART_DIR:
                qb.setTables(HeartContract.HEART_TABLE_NAME);
                break;
            case HEART_ITEM:
                qb.setTables(HeartContract.HEART_TABLE_NAME);
                qb.appendWhere(HeartContract._ID + "=");
                qb.appendWhere(uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        SQLiteDatabase db = mdbHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, sort);

        if (ret == null) {
            Log.e(TAG,"Heart.query: failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return ret;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
        int count;
        SQLiteDatabase db = mdbHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case HEART_DIR:
                count = db.update(HeartContract.HEART_TABLE_NAME, values,
                		selection,
                		selectionArgs);
                break;
            case HEART_ITEM:
            	String heartId = uri.getLastPathSegment();
                count = db.update(HeartContract.HEART_TABLE_NAME, values,
                		HeartContract._ID + "=" + heartId,
                        null);
                break;
            default: {
                throw new UnsupportedOperationException(
                        "Cannot update URL: " + uri);
            }
        }
        Log.v(TAG,"*** notifyChange() url: " + uri);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
}
