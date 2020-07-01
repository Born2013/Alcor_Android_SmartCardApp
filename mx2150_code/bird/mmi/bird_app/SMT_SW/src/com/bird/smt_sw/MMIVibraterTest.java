package com.bird.smt_sw;

import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class MMIVibraterTest extends MMINewActivity {

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
	private Vibrator vibrater;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vibrater_test);

		setTitle(R.string.vibrater);

		correctButton = (Button) findViewById(R.id.correct);
		errorButton = (Button) findViewById(R.id.error);
		resetButton = (Button) findViewById(R.id.reset);
		correctButton.setOnClickListener(mCorrectButtonHandler);
		errorButton.setOnClickListener(mErrorButtonHandler);
		resetButton.setOnClickListener(mRestdButtonHandler);
		Vibrator vibrater = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();

        long[] pattern = {1000,1000,1000,1000};

		vibrater.vibrate(pattern, -1);
		Timer timer = new Timer();
		timer.schedule(task, 4000);
	}


	private TimerTask task = new TimerTask()
	{
		@Override
		public void run()
		{

			Message message = new Message();

			message.what = 1;

			mHandler.sendMessage(message);

		}

	};

	private Handler mHandler = new Handler()
	{

		public void handleMessage(Message msg)
		{

			switch (msg.what)
			{

			case 1:
				correctButton.setVisibility(View.VISIBLE);
				errorButton.setVisibility(View.VISIBLE);
				resetButton.setVisibility(View.VISIBLE);
				break;

			}

			super.handleMessage(msg);
		}

	};
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIVibraterTest.this.getString(R.string.vibrater), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIVibraterTest.this.getString(R.string.vibrater), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIVibraterTest.this, MMIVibraterTest.class);
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
			ComponentName comp = new ComponentName(MMIVibraterTest.this, MMIVibraterTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIVibraterTest.this.getString(R.string.vibrater), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
