package com.bird.flashlight;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import com.bird.flashlight.FlashLightJni;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Message;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.PowerManager.WakeLock;


public class FlashLightActivity extends Activity {

	private ImageButton imgBtn;
	private ImageButton sButton;
	private View mLight;
	private Vibrator mv;
	private int status = 0;
	private int sos = 0;
	public static final String TAG = "FlashLightActivity";
	private SharedPreferences sp;
	private WakeLock wakeLock = null;

     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.bird.flashlight.ACTION_RETURN");//[BIRD][常亮10分钟自动关闭手电筒][maozexin][20150309]
        filter.addAction("com.bird.flashlight.ACTION_SOS_OFF");
        registerReceiver(mReceiver, filter);
        sp=getSharedPreferences("sos_status", 0);
		imgBtn = (ImageButton) findViewById(R.id.IButton);
		sButton = (ImageButton) findViewById(R.id.SButton);
		mLight =(View)findViewById(R.id.flashlight_light);
		if(!FeatureOption.BIRD_FLASHLIGHT_SOS_ON){
		    sButton.setVisibility(View.GONE);
		    //mLight.setVisibility(View.GONE);
		}
		mv = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		sButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (sos == 0) {
				    sendTimeOffBroadcast();
					sos = 1;
					//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]begin
					if (FeatureOption.BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT) {
					    status = 1;
					}
					//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]end
					sendSosBroadcast();
					sp.edit().putInt("sos",sos).commit();
					imgBtn.setBackgroundResource(R.drawable.torch_on_btn);
					sButton.setBackgroundResource(R.drawable.flashlight_sos_on);
					setLightOff();
					FlashLightJni.turn_off_flashlight();
					sos();
				} else {
				    sendTimeOnBroadcast();
					sos = 0;
					sendSosBroadcast();
					sp.edit().putInt("sos",sos).commit();
					timerCancel();
					if(status==1){
					FlashLightJni.turn_on_flashlight();
					}
					sButton.setBackgroundResource(R.drawable.flashlight_sos_off);
					setLightOn();
				}
			}
		});
		imgBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				mv.vibrate(30);
				if (status == 0) {
				   sendTimeOnBroadcast();
					status = 1;
					setLightOn();
					imgBtn.setBackgroundResource(R.drawable.torch_on_btn);
					FlashLightJni.turn_on_flashlight();
				} else {
					statusOff();
					sendTimeOffBroadcast();
					//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]begin
					if (!FeatureOption.BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT) {
					    finish();
					}
					//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]end
					return;
				}
			}
		});
		
	}
	private void sendTimeOnBroadcast(){
	    Intent intent = new Intent("com.bird.flashlight.ACTION_TIME_ON");
        sendBroadcast(intent);
	}
	private void sendTimeOffBroadcast(){
	    Intent intent = new Intent("com.bird.flashlight.ACTION_TIME_OFF");
        sendBroadcast(intent);
	}
	private void sendSosBroadcast(){
	    Intent intent = new Intent("com.bird.flashlight.ACTION_SOS");
	    Bundle bundle=new Bundle();
		bundle.putInt("sos",sos);
        intent.putExtras(bundle);
        sendBroadcast(intent);
	}
	private void statusOff(){
		status = 0;
		setLightOff();
		imgBtn.setBackgroundResource(R.drawable.torch_off_btn);
		FlashLightJni.turn_off_flashlight();
		sos = 0;
		sendSosBroadcast();
	    sp.edit().putInt("sos",sos).commit();
		timerCancel();
		sButton.setBackgroundResource(R.drawable.flashlight_sos_off);
	}
    private Timer mTimer;
    private  class SosTask extends TimerTask{
    	private int count = -1;
		
		public void run() {
			count++;
			switch (count) {
			case 1:
			case 3:
			case 5:
		    case 7:
		    case 11:
		    case 15:
		    case 19:
		    case 21:
		    case 23:
				mHandler.sendEmptyMessage(1);
				break;
		   case 2:
		   case 4:
		   case 6:
		   case 10:
		   case 14:
		   case 18:
		   case 20:
		   case 22:
		   case 24:
				mHandler.sendEmptyMessage(0);
				break;
		   case 33:
		        count=0;
				break;	

			default:
				break;
			}
			
		}
	};
	    private Handler mHandler = new Handler() {
    	  @Override
    	  public void handleMessage(Message msg) {
    	   // TODO Auto-generated method stub
    	          switch (msg.what) {
                  case 1:
                         FlashLightJni.turn_on_flashlight();
                         setLightOn();
                         break;
                  case 0:
                         FlashLightJni.turn_off_flashlight();
                         setLightOff();
                         break;
                  default:
                	  break;
    	          }
                 
                	  
    	 }
    };
  // 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
  private void acquireWakeLock() {
  if (null == wakeLock) {
     PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
    private void setLightOn(){
      //if(FeatureOption.BIRD_FLASHLIGHT_SOS_ON){
      mLight.setVisibility(View.VISIBLE);
      //}
    }
    private void setLightOff(){
      //if(FeatureOption.BIRD_FLASHLIGHT_SOS_ON){
      mLight.setVisibility(View.GONE);
      //}
    }
	private void sos(){
		   FlashLightJni.turn_on_flashlight();
		 if(mTimer == null) {
		     acquireWakeLock();
			 mTimer = new Timer();
			 mTimer.schedule(new SosTask(), 0, 800);
		 }
		 }
   public void timerCancel(){
		  if(mTimer!=null){
          mTimer.cancel();
          releaseWakeLock();
          mTimer=null;
          }
		 }
    @Override 
    public void onBackPressed() { 
        super.onBackPressed(); 
        statusOff();
    } 
     
	@Override
	protected void onResume() {
		super.onResume();
		/*boolean isTurnOn=FlashLightJni.get_state_flashlight();
		if(!isTurnOn&&sos!=1){
		sendTimeOnBroadcast();
		FlashLightJni.turn_on_flashlight();
		setLightOn();
		}
		status = 1;
		imgBtn.setBackgroundResource(R.drawable.torch_on_btn);*/
	}

	@Override
	protected void onPause() {
		super.onPause();
		mv.cancel();
	}
	
	@Override
	protected void onDestroy(){
	   FlashLightJni.turn_off_flashlight();
		Log.i("maozexin","destroy");
        statusOff();
		unregisterReceiver(mReceiver);
        super.onDestroy();
	}
	
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	// TODO Auto-generated method stub
		boolean isTurnOn=FlashLightJni.get_state_flashlight();
		if(sos!=1){
		if(!isTurnOn){
			status=0;
			imgBtn.setBackgroundResource(R.drawable.torch_off_btn);
			setLightOff();
		}else{
			status=1;
			imgBtn.setBackgroundResource(R.drawable.torch_on_btn);
			setLightOn();
		}
    	super.onWindowFocusChanged(hasFocus);
    	}
    }

	BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
        	final String action = intent.getAction();
        	//[BIRD][常亮10分钟自动关闭手电筒][maozexin][20150309]begin
            if ("com.bird.flashlight.ACTION_RETURN".equals(action)) {  
				status = 0;
				imgBtn.setBackgroundResource(R.drawable.torch_off_btn);
				setLightOff();
				FlashLightJni.turn_off_flashlight();
				finish();
				return;
            }
            if ("com.bird.flashlight.ACTION_SOS_OFF".equals(action)){
			    statusOff();
            }
           //[BIRD][常亮10分钟自动关闭手电筒][maozexin][20150309]end
        }		
	};
	
}
