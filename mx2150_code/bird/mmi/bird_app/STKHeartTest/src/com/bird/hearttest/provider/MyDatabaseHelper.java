package com.sensortek.stkhealthcare2.provider;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "heart.db";
    public static final String TAG = "MyDatabaseHelper";
    public static final int DATABASE_VERSION = 7;
    
	private Context mContext;
	private static MyDatabaseHelper mMyDatabaseHelper = null;

    /**
     * Access function to get the singleton instance of DialerDatabaseHelper.
     */
    public static synchronized MyDatabaseHelper getInstance(Context context) {

        if (mMyDatabaseHelper == null) {
            // Use application context instead of activity context because this is a singleton,
            // and we don't want to leak the activity if the activity is not running but the
            // dialer database helper is still doing work.
        	mMyDatabaseHelper = new MyDatabaseHelper(context.getApplicationContext());
        }
        return mMyDatabaseHelper;
    }


    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG,"MyDatabaseHelper");
        mContext = context;
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) {
Log.d(TAG,"onCreate");
		createHeartTable(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		Log.d(TAG,"createHeartTable"+",oldVersion = "+oldVersion);
	}

    private static void createHeartTable(SQLiteDatabase db) {
        Log.d(TAG,"createHeartTable");
        db.execSQL("CREATE TABLE " + HeartContract.HEART_TABLE_NAME + " (" +
                HeartContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                HeartContract.BPM + " INTEGER NOT NULL, " +
                HeartContract.DATE_TIME + " TEXT NOT NULL);");
    }
    
}
