package com.bird.smt_sw;



import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.media.AudioManager;
import android.media.MediaPlayer;

import android.util.Log;
import android.view.View;

public class Boot_receiver extends BroadcastReceiver {

	private static final String TAG = "Boot_receiver";
	private MediaPlayer mediaPlayer;
	private Context mContext;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		mContext = context;
		String action = intent.getAction();
		Log.d(TAG, "@ onreceive action ="+action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)){
			Log.d(TAG, "@ action in");

	        //final Intent intent_ser = new Intent();
	         Intent intent_ser = new Intent(context.getApplicationContext(),MMITestBootupService.class);
	        intent_ser.setAction("com.bird.smt_sw.MMITestBootupService");
	        context.startService(intent_ser);
			
		}
		
	}

}
