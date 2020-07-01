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
/*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]begin*/
public class MMITestToggleSwitch extends MMINewActivity {

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private ImageView mImageView;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toggle_switch_layout);
        setTitle(R.string.toggle_switch);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mImageView = (ImageView) findViewById(R.id.showkey1);
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
            case KeyEvent.KEYCODE_F6:
            case KeyEvent.KEYCODE_F7:
                mImageView.setImageResource(R.drawable.hall);
                return true;
            default:
                break;
        }
        return true;
    }

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMITestToggleSwitch.this.getString(R.string.toggle_switch), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMITestToggleSwitch.this.getString(R.string.toggle_switch), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestToggleSwitch.this, MMITestToggleSwitch.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
		}
	};
}
/*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]end*/
