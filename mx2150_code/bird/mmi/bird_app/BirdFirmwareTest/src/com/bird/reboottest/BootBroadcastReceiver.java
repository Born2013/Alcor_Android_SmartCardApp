package com.bird.reboottest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG ="BootBroadcastReceiver" ;
    private static final String PREFS_NAME = "com.bird.reboot.test";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG+"luye","onReceive------->action:"+intent.getAction());
        if (com.bird.firmwaretest.FeatureOption.BIRD_ADD_REBOOT_TEST) {
            SharedPreferences preference = context.getSharedPreferences(PREFS_NAME,0);
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)&&preference.getBoolean("isRebootTest",false)){
                Intent ootStartIntent=new Intent(context,RebootTestActivity.class);
                ootStartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(ootStartIntent);
            }
        }
    }


 
}
