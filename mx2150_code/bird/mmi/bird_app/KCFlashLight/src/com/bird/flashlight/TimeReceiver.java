package com.bird.flashlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;


public class TimeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		   Log.i("maozexin","action:"+action);
        if(action.equals("com.bird.flashlight.ACTION_TIME_OFF")){
        		Intent intent1=new Intent(context,TimeService.class);
				context.stopService(intent1);
        }else if(action.equals("com.bird.flashlight.ACTION_TIME_ON")){
        		Intent intent1=new Intent(context,TimeService.class);
				context.startService(intent1);
        }
	}

}
