package com.bird.flashlight;


import android.app.Service;
import com.bird.flashlight.FlashLightJni;
import java.util.Timer;
import java.util.TimerTask;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class TimeService extends Service {
    private Timer mTimer;
    private WakeLock wakeLock = null;
    private  class TimeTask extends TimerTask{
    	private int count = 0;
		
		public void run() {
			count++;
			switch (count) {
			case 600:
			  Log.i("maozexin","time to turn off");
			  FlashLightJni.turn_off_flashlight();
			  Intent intent = new Intent("com.bird.flashlight.ACTION_RETURN");
              sendBroadcast(intent);
			  stopSelf();
              break;
			default:
				break;
			}
			
		}
	};

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.i("maozexin","service oncreate");
		if(mTimer == null) {
		     acquireWakeLock();
			 mTimer = new Timer();
			 mTimer.schedule(new TimeTask(), 0,1000);
			 Log.i("maozexin","time to begin");
		 }
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	public void onDestroy(){
		 if(mTimer!=null){
          mTimer.cancel();
          releaseWakeLock();
          mTimer=null;
          }
        Log.i("maozexin","service destroy");
        super.onDestroy();
	}      
  // 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
  private void acquireWakeLock() {
  if (null == wakeLock) {
     PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
     wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());
     if (null != wakeLock) {
     Log.i("maozexin", "call acquireWakeLock");
     wakeLock.acquire();
      }
    }
  }
  // 释放设备电源锁
  private void releaseWakeLock() {
  if (null != wakeLock && wakeLock.isHeld()) {
    Log.i("maozexin", "call releaseWakeLock");
    wakeLock.release();
    wakeLock = null;
    }
  }
}
