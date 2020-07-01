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

public class MMITestTemperature extends MMINewActivity implements SensorEventListener {
    private static final String TAG = "TemperatureTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private TextView xValue;
    private SensorManager mSensorManager;
    private Sensor mTemperature;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature_test);
        setTitle(R.string.temperature);
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
        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG,"onSensorChanged  event.sensor.getType() ="+event.sensor.getType());
        if (event.sensor.getType() == Sensor.TYPE_TEMPERATURE) {
            float temp = event.values[0]/10000;
            int temp1 = (int) (temp*10);
            float temperature =(float)temp1/10;
            xValue.setText(String.valueOf(temperature) + "°C");
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    
    @Override 
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mTemperature,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestTemperature.this.getString(R.string.temperature), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestTemperature.this.getString(R.string.temperature), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestTemperature.this, MMITestTemperature.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
        }
    };
}

