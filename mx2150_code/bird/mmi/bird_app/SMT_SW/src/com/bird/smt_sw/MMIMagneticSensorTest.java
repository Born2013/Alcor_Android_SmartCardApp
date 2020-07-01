package com.bird.smt_sw;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager.WakeLock;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MMIMagneticSensorTest extends MMINewActivity {

    private static final String TAG = "MMIMagneticSensorTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;
    private Sensor mSensor;
    private int mRate = SensorManager.SENSOR_DELAY_NORMAL;
    private TextView mLightSensor ,mStatus;
    private WakeLock mWakeLock = null;
    private float mOldvalue = -1f;
    private boolean mMagneticChanged = false;
    private TextView xValue;
    private TextView yValue;
    private TextView zValue;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {      

        super.onCreate(savedInstanceState);
        setContentView(R.layout.magnetic_sensor);
        setTitle(R.string.magnetic);

        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);       

        mLightSensor = (TextView) findViewById(R.id.magnetic_sensor);
        mStatus = (TextView) findViewById(R.id.status);

        xValue = (TextView) findViewById(R.id.x);
        yValue = (TextView) findViewById(R.id.y);
        zValue = (TextView) findViewById(R.id.z);
         
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        mSensorListener = new SensorEventListener() {  
            @Override  
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
            @Override  
            public void onSensorChanged(SensorEvent event) {

                Log.e(TAG, "onSensorChanged");
                if (event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) return;

                float new_value = event.values[SensorManager.DATA_X];
                Log.e(TAG, "new_value="+new_value);
                Log.e(TAG, "mOldvalue="+mOldvalue);

                //float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                xValue.setText("X = : "+new_value);
                yValue.setText("Y = : "+y);
                zValue.setText("Z = : "+z);

                if(mOldvalue==-1f) {
                    mOldvalue=new_value;
                } else {
                    if(new_value > mOldvalue && !mMagneticChanged) {
                    Log.e(TAG, "new_value > mOldvalue");
                        //mView.setBackgroundColor(0xFFFF0000);
                        //mStatus_tv.setText(R.string.light_bright);
                        //mHintView.setVisibility(View.INVISIBLE);
                        mMagneticChanged = true;
                        mStatus.setText(R.string.item_pass);
                        mLightSensor.setTextColor(getResources().getColor(R.drawable.GREEN));
                        mStatus.setTextColor(getResources().getColor(R.drawable.GREEN));
                        mEditor.putInt(MMIMagneticSensorTest.this.getString(R.string.magnetic), Test_launcherActivity.PASS);
                        mEditor.commit();
                        
                    } else if(new_value < mOldvalue && !mMagneticChanged) {
                    Log.e(TAG, "new_value < mOldvalue");
                        //mView.setBackgroundColor(0xFF00FF00);
                        //mStatus_tv.setText(R.string.light_dark);
                        //mHintView.setVisibility(View.INVISIBLE);
                        mMagneticChanged = true;
                        mStatus.setText(R.string.item_pass);
                        mLightSensor.setTextColor(getResources().getColor(R.drawable.GREEN));
                        mStatus.setTextColor(getResources().getColor(R.drawable.GREEN));
                        mEditor.putInt(MMIMagneticSensorTest.this.getString(R.string.magnetic), Test_launcherActivity.PASS);
                        mEditor.commit();
                    }
                }
            }
        };

        mSensorManager.registerListener(mSensorListener, mSensor, mRate);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(mSensorListener);
    }

    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {

        public void onClick(View v) {

            mEditor.putInt(MMIMagneticSensorTest.this.getString(R.string.magnetic), Test_launcherActivity.PASS);
            mEditor.commit();
            finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {

        public void onClick(View v) {
            mEditor.putInt(MMIMagneticSensorTest.this.getString(R.string.magnetic), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {

        public void onClick(View v) {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(MMIMagneticSensorTest.this, MMIMagneticSensorTest.class);
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
            ComponentName comp = new ComponentName(MMIMagneticSensorTest.this, MMIMagneticSensorTest.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            mEditor.putInt(MMIMagneticSensorTest.this.getString(R.string.magnetic), mMagneticChanged ? Test_launcherActivity.PASS : Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
}
