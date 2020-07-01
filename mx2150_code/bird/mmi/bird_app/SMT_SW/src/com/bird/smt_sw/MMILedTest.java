package com.bird.smt_sw;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MMILedTest extends MMINewActivity {

    private int led_status=0;
    Handler mHandler;
    Runnable mRunnable;
    private TextView mStatus_tv;
    private WakeLock mWakeLock = null;
    private static final String TAG = "DEVICE_TEST_LED";
	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
	
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	       
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.led_test);
	        setTitle(R.string.led);
            //Log.d(TAG, "MTK_FAN5405_SUPPORT = "+com.mediatek.common.featureoption.FeatureOption.MTK_FAN5405_SUPPORT);
			
	        correctButton = (Button) findViewById(R.id.correct);
	        errorButton = (Button) findViewById(R.id.error);
	        resetButton = (Button) findViewById(R.id.reset);
	        correctButton.setOnClickListener(mCorrectButtonHandler);
	        errorButton.setOnClickListener(mErrorButtonHandler);
	        resetButton.setOnClickListener(mRestdButtonHandler);      
	        mStatus_tv = (TextView) findViewById(R.id.common_test_mode_status);

	        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
	        mEditor = mPreferences.edit();
	        
	        MMINativeLib.device_test_led_r_on_jni();
	        //MMINativeLib.device_test_keypad_bk_turn_on_jni();
			
	        mStatus_tv.setText(R.string.led_red);
			led_status=1;
			
	        mHandler=new Handler();
	        mRunnable=new Runnable() {

	            @Override
	            public void run() {
	            /*
	                if(led_status==1) {
	                    led_status=0;
	                    boolean flag = MMINativeLib.device_test_led_r_off_jni();
	                    //MMINativeLib.device_test_keypad_bk_turn_off_jni();
	                    mStatus_tv.setText(R.string.close);
	                    System.out.println("flag==>"+flag);
	                } else { 
	                    led_status=1;
	                    boolean flag1 = MMINativeLib.device_test_led_r_on_jni();
	                    System.out.println("flag1==>"+flag1);
	                    //MMINativeLib.device_test_keypad_bk_turn_on_jni();
	                    mStatus_tv.setText(R.string.open);
	                }
	            */
	                if(led_status==1) {
	                    led_status=2;
	                    boolean flag = MMINativeLib.device_test_led_r_off_jni();
	                    MMINativeLib.device_test_led_g_on_jni();
	                    //MMINativeLib.device_test_keypad_bk_turn_off_jni();
	                    mStatus_tv.setText(R.string.led_green);
	                    System.out.println("1flag==>"+flag);
	                } else if(led_status==2) {
	                    led_status=3;
	                    boolean flag = MMINativeLib.device_test_led_g_off_jni();
	                    MMINativeLib.device_test_led_b_on_jni();
	                    mStatus_tv.setText(R.string.led_blue);
	                    System.out.println("2flag==>"+flag);
	                } 
	                else if (led_status==3){ 
	                    led_status=1;
	                    boolean flag = MMINativeLib.device_test_led_b_off_jni();
	                    MMINativeLib.device_test_led_r_on_jni();
	                    System.out.println("3flag==>"+flag);
	                    //MMINativeLib.device_test_keypad_bk_turn_on_jni();
	                    mStatus_tv.setText(R.string.led_red);
	                }
	                mHandler.postDelayed(this, 1000);
	            }
	        };

	        mHandler.postDelayed(mRunnable, 1000);
	    }

	    @Override
	    protected void onResume() {

	        super.onResume();
	        if (mWakeLock == null) {
	            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
	            mWakeLock.setReferenceCounted(false);
	            mWakeLock.acquire();
	        }
	    }

	    @Override
	    protected void onPause() {

	        super.onPause();
	        if (mWakeLock != null && mWakeLock.isHeld()) {
	            mWakeLock.release();
	            mWakeLock = null;
	        }
	    }

	    @Override
	    public void onStop() { 
	    	
	        super.onStop();
	        mHandler.removeCallbacks(mRunnable);
	        if(led_status==1) {
	            led_status=0;
	            MMINativeLib.device_test_led_r_off_jni();
	            //MMINativeLib.device_test_keypad_bk_turn_off_jni();
	        } else if(led_status==2){
	            led_status=0;
	            MMINativeLib.device_test_led_g_off_jni();
	        } else if(led_status == 3){
	            led_status=0;
	            MMINativeLib.device_test_led_b_off_jni();
	        }
	    }

	    

	    



		private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {

			public void onClick(View v) {
				mEditor.putInt(MMILedTest.this.getString(R.string.led), Test_launcherActivity.PASS);
				mEditor.commit();
				finish();
			}
		};

		private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {

			public void onClick(View v) {

				mEditor.putInt(MMILedTest.this.getString(R.string.led), Test_launcherActivity.FAIL);
				mEditor.commit();
				finish();
			}
		};

		private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {

			public void onClick(View v) {
				//Intent intent = null;	
				//intent = new Intent(DeviceTestLED.this,DeviceTestLED.class);
				//startActivityForResult(intent, 0);
				Intent intent = new Intent();
				ComponentName comp = new ComponentName(MMILedTest.this, MMILedTest.class);
				intent.setComponent(comp);
				
				startActivity(intent);
				finish();
			}
		};
		
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			if (keyCode == KeyEvent.KEYCODE_HOME){
				
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_MENU){
				
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
				Intent intent = new Intent();
				ComponentName comp = new ComponentName(MMILedTest.this, MMILedTest.class);
				intent.setComponent(comp);
				
				startActivity(intent);
				finish();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
				mEditor.putInt(MMILedTest.this.getString(R.string.led), Test_launcherActivity.PASS);
				mEditor.commit();
				finish();
				return true;
			}

			return super.onKeyDown(keyCode, event);
		}
    
}
