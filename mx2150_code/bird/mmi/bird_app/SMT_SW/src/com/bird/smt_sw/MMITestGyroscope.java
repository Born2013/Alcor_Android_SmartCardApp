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

public class MMITestGyroscope extends MMINewActivity implements SensorEventListener {
    private static final String TAG = "GyroscopeTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private TextView xValue;
    private TextView yValue;
    private TextView zValue;
    private TextView noteInf;
    private  SensorManager mSensorManager;
    private Sensor mGyroscope;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gyroscope_test);
        setTitle(R.string.gyroscope);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();    
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);      
        xValue = (TextView) findViewById(R.id.x);
        yValue = (TextView) findViewById(R.id.y);
        zValue = (TextView) findViewById(R.id.z);
        noteInf = (TextView) findViewById(R.id.note_phonelocation);     
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG,"onSensorChanged  event.sensor.getType() ="+event.sensor.getType());
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            xValue.setText("X = : "+x);
            yValue.setText("Y = : "+y);
            zValue.setText("Z = : "+z);
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    
    @Override 
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mGyroscope,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestGyroscope.this.getString(R.string.gyroscope), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			mEditor.putInt(MMITestGyroscope.this.getString(R.string.gyroscope), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestGyroscope.this, MMITestGyroscope.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
        }
    };
}

