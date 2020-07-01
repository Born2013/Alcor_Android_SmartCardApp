package com.sensortek.stkhealthcare2.provider;

import android.net.Uri;

public final class HeartContract {
	
    public static final String AUTHORITY = "com.sensortek.stkhealthcare2";
    public static final String HEART_TABLE_NAME = "Heart";
    public static final String _ID = "_id";
    public static final String BPM = "bpm";
    public static final String DATE_TIME = "date_time";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/heart");
}
