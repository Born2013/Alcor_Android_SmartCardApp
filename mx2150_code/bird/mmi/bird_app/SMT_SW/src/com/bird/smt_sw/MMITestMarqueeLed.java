package com.bird.smt_sw;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.util.Log;
import com.bird.marqueen.MarqueenNative;
import android.widget.Toast;
import android.content.SharedPreferences;

public class MMITestMarqueeLed extends MMINewActivity {
    private static final String TAG = "MarqueeLedTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private Button mSwitchButton;
    private boolean mIsOn; 
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.marquee_led_test);
        setTitle(R.string.marquee_led);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();        
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);      
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mSwitchButton = (Button) findViewById(R.id.button1);           
        mSwitchButton.setOnClickListener(mSwitchButtonHandler);
    }



    @Override
    protected void onResume() {
        super.onResume();
        mSwitchButton.setEnabled(true);
    }

    @Override
    protected void onPause() {    
        super.onPause();
        if (mIsOn) {
            MarqueenNative.marqueenIoctl(1,0);/*关闭跑马灯芯片*/
            MarqueenNative.marqueenClose();/*关闭设备节点*/
            mIsOn  = false;
        }
    }
    
    private View.OnClickListener mSwitchButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            int ret = MarqueenNative.marqueenOpen();
            if(ret < 0){
                Toast.makeText(MMITestMarqueeLed.this,"open device is failed",Toast.LENGTH_SHORT).show();
            } else {
                MarqueenNative.marqueenIoctl(1,1);/*打开跑马灯芯片*/
                int value = MarqueenNative.marqueenIoctl(2,4);/*2:表示循环显示，4：全亮*/
                if (value < 0) {
                    Toast.makeText(MMITestMarqueeLed.this,"open device is failed",Toast.LENGTH_SHORT).show();
                } else {
                    mSwitchButton.setEnabled(false);
                    mIsOn = true;
                }
            }
        }
    };

    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestMarqueeLed.this.getString(R.string.marquee_led), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestMarqueeLed.this.getString(R.string.marquee_led), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestMarqueeLed.this, MMITestMarqueeLed.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
        }
    };
}

