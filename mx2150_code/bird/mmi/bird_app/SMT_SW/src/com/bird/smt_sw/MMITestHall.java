package com.bird.smt_sw;

import android.R.color;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
/*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]begin*/
public class MMITestHall extends MMINewActivity {

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private ImageView mHall;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hall_layout);
        setTitle(R.string.hall);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mHall = (ImageView) findViewById(R.id.showkey1);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
    }

    @Override
    public void onAttachedToWindow() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);// FLAG_HOMEKEY_DISPATCHED
        super.onAttachedToWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_F2:
            case KeyEvent.KEYCODE_F1:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                mHall.setImageResource(R.drawable.hall);
                return true;
            default:
                break;
        }
        return true;
    }

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMITestHall.this.getString(R.string.hall), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMITestHall.this.getString(R.string.hall), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestHall.this, MMITestHall.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
		}
	};
}
/*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]end*/
