package com.sensortek.stkhealthcare2;

import android.content.Context;
import com.sensortek.stkhealthcare2.provider.MyDatabaseHelper;


public class DatabaseHelperManager {
    public static MyDatabaseHelper getDatabaseHelper(Context context) {
        return MyDatabaseHelper.getInstance(context);
    }
}
