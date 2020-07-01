package com.bird.smt_sw;




import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class MMIBreathLedTest extends MMINewActivity {

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;


        private int req_token;
    
        private SharedPreferences mPreferences;
        private SharedPreferences.Editor mEditor;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.common_test_mode_breathled);
		
		setTitle(R.string.breathled);
		
		//req_token = getIntent().getIntExtra(TOKEN, 0);
		correctButton = (Button) findViewById(R.id.correct);
		errorButton = (Button) findViewById(R.id.error);
		//resetButton = (Button) findViewById(R.id.reset);
		correctButton.setOnClickListener(mCorrectButtonHandler);
		errorButton.setOnClickListener(mErrorButtonHandler);
		//resetButton.setOnClickListener(mRestdButtonHandler);
		
		mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
                mEditor = mPreferences.edit();
                
		MMINativeLib.Breathled1();
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		//MMINativeLib.BreathledStop();
	}
	
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {

			mEditor.putInt(MMIBreathLedTest.this.getString(R.string.breathled), Test_launcherActivity.PASS);
			mEditor.commit();
			
			MMINativeLib.BreathledStop();
			finish();
		}
	};
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIBreathLedTest.this.getString(R.string.breathled), Test_launcherActivity.FAIL);
			mEditor.commit();
			
			MMINativeLib.BreathledStop();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIBreathLedTest.this, MMIBreathLedTest.class);
			intent.setComponent(comp);
			//intent.putExtra(TOKEN, req_token);
			startActivity(intent);
			finish();
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}
	
        @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIBreathLedTest.this, MMIBreathLedTest.class);
			intent.setComponent(comp);
			//intent.putExtra(TOKEN, 1);
			MMINativeLib.BreathledStop();
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIBreathLedTest.this.getString(R.string.breathled), Test_launcherActivity.PASS);
			mEditor.commit();
			MMINativeLib.BreathledStop();
			finish();
			return true;
		}
		    
		return super.onKeyDown(keyCode, event);
	}
}
