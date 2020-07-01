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
import android.content.SharedPreferences;

public class MMITestPressure extends MMINewActivity implements SensorEventListener {
    private static final String TAG = "PressureTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private TextView xValue;
    private  SensorManager mSensorManager;
    private Sensor mPressure;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pressure_test);
        setTitle(R.string.pressure);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();    
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);      
        xValue = (TextView) findViewById(R.id.x);    
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG,"onSensorChanged  event.sensor.getType() ="+event.sensor.getType());
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            float x = event.values[0];
            xValue.setText("X = : "+x);
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    
    @Override 
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mPressure,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestPressure.this.getString(R.string.pressure), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestPressure.this.getString(R.string.pressure), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestPressure.this, MMITestPressure.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
        }
    };
}

