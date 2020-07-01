package com.bird.reboottest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import com.bird.firmwaretest.R;
import com.bird.firmwaretest.OptionsActivity;

public class RebootTestActivity extends Activity {
    private static final String TAG = "RebootTestActivity";

    private int usedCount;
    private int unusedCount;
    private int testCount;
    private String lastTestTime;
    private String recentTestTime;
    private SharedPreferences preference;
    private String PREFS_NAME = "com.bird.reboot.test";

    private TextView mTotalTestTv;
    private TextView mUsedTestTv;
    private TextView mUnusedTestTv;
    private TextView mLastTestTv;
    private TextView mRecentTestTv;
    private Button mClearBtn;
    private Button mStopBtn;
    private TextView mResultTestTv;

    private LinearLayout mResultLayout;
    private static final int START_REBOOT = 1;
    private boolean isCancelFlag = false;
    private PowerManager.WakeLock mWakeLock;


    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START_REBOOT:
                    SharedPreferences.Editor editor = preference.edit();
                    editor.putBoolean("isRebootTest",true);
                    editor.commit();
                    updatePreference();
                    PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
                    mWakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK,TAG);
                    mWakeLock.acquire();
                    powerReboot();
                    break;
                default:
                    break;

            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reboot_test);

        initView();
        setData(getApplicationContext());

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCancelFlag){
            processReboot();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        //释放屏幕常亮锁
        if(null != mWakeLock) {
            mWakeLock.release();
        }
    }

    private void initView(){
        mTotalTestTv = (TextView)findViewById(R.id.tv_test_count);
        mUsedTestTv = (TextView)findViewById(R.id.tv_used_count);
        mUnusedTestTv = (TextView)findViewById(R.id.tv_unused_count);
        mLastTestTv = (TextView)findViewById(R.id.tv_last_reboot_time);
        mRecentTestTv = (TextView)findViewById(R.id.tv_current_reboot_time);

        mResultLayout = (LinearLayout)findViewById(R.id.show_result);
        mClearBtn = (Button)findViewById(R.id.btn_clear);
        mClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAndTest();
            }
        });

        mStopBtn = (Button)findViewById(R.id.btn_stop);
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isCancelFlag){
                    isCancelFlag = false;
                    startReboot();
                    mStopBtn.setText(getString(R.string.btn_stop_test_txt));

                } else {
                    isCancelFlag =true;
                    cancelReboot();
                    mStopBtn.setText(getString(R.string.btn_start_test_txt));
                }

            }
        });

    }

    private void setData(Context context){
        preference = context.getSharedPreferences(PREFS_NAME,0);

        testCount = preference.getInt("testCount", 0);
        usedCount = preference.getInt("usedCount", 0);
        unusedCount = testCount-usedCount;
        lastTestTime = preference.getString("lastTestTime","");
        recentTestTime = preference.getString("recentTestTime","");

        String totalText = getResources().getString(R.string.test_total_count,testCount);
        String usedText = getResources().getString(R.string.test_used_count,usedCount);
        String unUsedText = getResources().getString(R.string.test_unused_count,unusedCount);
        String lastTestText = getResources().getString(R.string.test_last_reboot_time,lastTestTime);
        String recentTestText = getResources().getString(R.string.test_recent_reboot_time,recentTestTime);

        mTotalTestTv.setText(totalText);
        mUsedTestTv.setText(usedText);
        mUnusedTestTv.setText(unUsedText);
        mLastTestTv.setText(lastTestText);
        mRecentTestTv.setText(recentTestText);
    }

    private void processReboot(){
        SharedPreferences.Editor editor = preference.edit();

        if (usedCount >= testCount){
            mResultLayout.setVisibility(View.VISIBLE);
            mClearBtn.setVisibility(View.VISIBLE);
            mStopBtn.setVisibility(View.GONE);
            String resultText =null;
            mResultTestTv = (TextView)findViewById(R.id.tv_success_result);
            if (usedCount>testCount){
                resultText = getResources().getString(R.string.result_error);
            } else {
                resultText = getResources().getString(R.string.test_result_tip,usedCount);
            }

            mResultTestTv.setText(resultText);
            cancelReboot();

        } else {
            if (mResultLayout.getVisibility()==View.VISIBLE){
                mResultLayout.setVisibility(View.GONE);
            }
            if(mClearBtn.getVisibility()==View.VISIBLE){
                mClearBtn.setVisibility(View.GONE);
            }

            if (mStopBtn.getVisibility()==View.GONE){
                mStopBtn.setVisibility(View.VISIBLE);
            }
            startReboot();
        }
    }

    private void startReboot(){
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean("isRebootTest",false);
        editor.commit();
        mHandler.removeMessages(START_REBOOT);
        mHandler.sendEmptyMessageDelayed(START_REBOOT,5000);
    }

    private void cancelReboot(){
        mHandler.removeMessages(START_REBOOT);
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean("isRebootTest",false);
        editor.commit();
    }

    private void updatePreference(){
        SharedPreferences.Editor editor = preference.edit();

        usedCount++;

        editor.putInt("usedCount", Math.min(usedCount,testCount));
        editor.putString("lastTestTime",recentTestTime);
        String recentTestText = mFormat.format(new Date(System.currentTimeMillis()));
        editor.putString("recentTestTime",recentTestText);
        editor.commit();
    }


    private void powerReboot() {
        PowerManager pManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
        pManager.reboot("重启");
        System.out.println("execute cmd--> reboot\n" + "recovery");
    }

    private void clearAndTest (){
        clearData();
        processReboot();
    }

    private void clearData(){
        SharedPreferences.Editor editor = preference.edit();
        usedCount = 0;

        editor.putInt("usedCount", usedCount);
        editor.putString("lastTestTime","");
        editor.putString("recentTestTime","");
        editor.commit();

        setData(getApplicationContext());
    }

    private void clearAllData(){
        SharedPreferences.Editor editor = preference.edit();
        usedCount = 0;
        testCount = 0;
        editor.putInt("usedCount", usedCount);
        editor.putInt("testCount", testCount);
        editor.putString("lastTestTime","");
        editor.putString("recentTestTime","");
        editor.commit();

        //setData(getApplicationContext());
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode()==KeyEvent.KEYCODE_HOME){
            Log.i(TAG+"luye","keycode home clicked");
            cancelReboot();
        } else if (event.getKeyCode()==KeyEvent.KEYCODE_BACK){
            Log.i(TAG+"luye","keycode back clicked");
            cancelReboot();
            clearAllData();
            finish();
            Intent intent = new Intent(RebootTestActivity.this,OptionsActivity.class);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }
}
