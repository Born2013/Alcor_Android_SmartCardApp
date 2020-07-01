package com.bird.trafficmgr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.database.Cursor;
import android.telecom.TelecomManager;
import android.util.Log;
import android.app.ActivityThread;


import java.util.ArrayList;
import java.util.List;
import android.os.AsyncTask;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
public class CommondBroacastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommondBroacastReceiver MyFireWallService - gaowei";
    private static final boolean BIRD_AIMEI_STUDENT = android.os.SystemProperties.get("ro.bd_aimei_student").equals("1");
    private final String ACTION = "android.intent.action.COMMONDCODE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
         Log.d(TAG, "action = " +action);
        if (BIRD_AIMEI_STUDENT && ACTION.equals(action)) {
            Intent msg = new Intent(context, MyFireWallService.class);
            context.startService(msg);
            Log.d(TAG, "CommondBroacastReceiver here!!! BroadcastReceiver");
        }
    }
}
