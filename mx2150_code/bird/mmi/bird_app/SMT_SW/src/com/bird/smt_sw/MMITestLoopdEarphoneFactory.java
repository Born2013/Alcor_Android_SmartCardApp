package com.bird.smt_sw;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.media.AudioManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import android.view.WindowManager; 
import android.content.ComponentName;

/*[BIRD]add by zhangaman 20150717 begin */
public class MMITestLoopdEarphoneFactory extends Activity {

       private boolean gEarphoneMicSpeekLoogCheckResult = false;
       private final int RECORDTIMELONG = 2000;	
	private MediaRecorder mediaRecorder;
	private MediaPlayer mediaPlayer;
	private Button mStartButton;

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;

	private TextView mNoteText;
	private ProgressBar progressbar;
	private boolean openflag = false;
	private TextView mRecord_test_infor;
        private final String TESTFILE = "test.amr";	
       private boolean playFinishFlag = false;
       private SharedPreferences.Editor mEditor;
       private SharedPreferences mPreferences;
	   
	@Override
	public void onCreate(Bundle savedInstanceState) {
              gEarphoneMicSpeekLoogCheckResult = false;
			  
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sound_loop_test);
		setTitle(R.string.EarphoneMicLoop);
             mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
		mEditor = mPreferences.edit();
		mStartButton = (Button) findViewById(R.id.start_record);
              correctButton = (Button) findViewById(R.id.correct);
              errorButton = (Button) findViewById(R.id.error);
              resetButton = (Button) findViewById(R.id.reset);
			  
		mNoteText = (TextView) findViewById(R.id.note_title);
		progressbar = (ProgressBar) findViewById(R.id.progressbar);
		mStartButton.setOnClickListener(mStartButtonHandler);
              correctButton.setOnClickListener(mCorrectButtonHandler);
              errorButton.setOnClickListener(mErrorButtonHandler);
              resetButton.setOnClickListener(mRestdButtonHandler);
			  
              mRecord_test_infor = (TextView) findViewById(R.id.record_test_infor);
              mRecord_test_infor.setText(R.string.factoryrecord_test_infor);

		mediaPlayer = new MediaPlayer();
		initRecord();

	}
	
	private void initRecord() {
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mediaRecorder.setOutputFile(getFilesDir() + TESTFILE);

	}

	private TimerTask task = new TimerTask() {

		@Override
		public void run() {
			Message message = new Message();
			message.what = 1;
			mHandler.sendMessage(message);

		}
	};
	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				playRecord();
				break;
			}
			super.handleMessage(msg);
		}

	};

	private void playRecord() {

		closeRecord();
		mNoteText.setText(R.string.start_play);
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.reset();
		}
		try {
			//mediaPlayer.setDataSource(getFilesDir()  + TESTFILE);
			File file= new File(getFilesDir() + TESTFILE);
			FileInputStream fis = new FileInputStream(file);
			mediaPlayer.setDataSource(fis.getFD());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			mediaPlayer.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mediaPlayer.start();
		mediaPlayer
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer arg0) {
						mediaPlayer.stop();
						mediaPlayer.release();
						mNoteText.setText(R.string.stop_play);
						progressbar.setVisibility(View.GONE);
						playFinishFlag = true;
					}
				});
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					@Override
					public boolean onError(MediaPlayer player, int arg1,
							int arg2) {
						mediaPlayer.release();
						return false;
					}
				});

	}

	private void closeRecord() {
		if(openflag){
		try {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mNoteText.setText(R.string.stop_record);
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		openflag = false;
		}
		
	}

	private void mStartKeyButtonHandle(){

			       if(!isEarphonePlugged()){
   					mNoteText.setText(R.string.plusinEarphone);
				   	return;
				 }				
				mStartButton.setVisibility(View.INVISIBLE);
				mNoteText.setText(R.string.start_record);


			
			try {
				mediaRecorder.prepare();
				
				mediaRecorder.start();
				openflag = true;
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Timer timer = new Timer();
			timer.schedule(task, RECORDTIMELONG);
				
	}

	private View.OnClickListener mStartButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mStartKeyButtonHandle();
		}

	};

	@Override
	public void onAttachedToWindow() {
	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);// caoyuangui change FLAG_HOMEKEY_DISPATCHED to FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
	super.onAttachedToWindow();
	}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        boolean handleResult = false;
    
        switch(keyCode){
            case KeyEvent.KEYCODE_VOLUME_UP:
			
			Intent intent = null;	
			intent = new Intent(MMITestLoopdEarphoneFactory.this,MMITestLoopdEarphoneFactory.class);
			startActivityForResult(intent, 0);
			finish();
			
                     handleResult = true;
			break;
			
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if(playFinishFlag){
                     gEarphoneMicSpeekLoogCheckResult = true;
			SharedPreferences userInfo =	getSharedPreferences("MMICheckResult",0);
			userInfo.edit().putBoolean("gEarphoneMicSpeekLoogCheckResult", gEarphoneMicSpeekLoogCheckResult).commit();
				
			mEditor.putInt(MMITestLoopdEarphoneFactory.this.getString(R.string.EarphoneMicLoop), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();					 
			}else  if(!openflag){
					 	mStartKeyButtonHandle();
			}
                     handleResult = true;			
			break;
	}
		
        return handleResult;
    }

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {

			mEditor.putInt(MMITestLoopdEarphoneFactory.this.getString(R.string.EarphoneMicLoop), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMITestLoopdEarphoneFactory.this.getString(R.string.EarphoneMicLoop), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMITestLoopdEarphoneFactory.this, MMITestLoopdEarphoneFactory.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
		}
	};




	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
	}

	private boolean isEarphonePlugged() {

		boolean bRet = true;
		AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		if (null != am) {
			bRet = am.isWiredHeadsetOn();
		}
		return bRet;
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		return;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if (event.getRepeatCount() > 0 && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
	        return true;
	    }
	    return super.dispatchKeyEvent(event);
	}
}
/*[BIRD]add by zhangaman 20150717 end */