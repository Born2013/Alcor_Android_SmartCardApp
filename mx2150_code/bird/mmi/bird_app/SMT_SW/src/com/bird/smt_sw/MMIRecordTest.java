package com.bird.smt_sw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MMIRecordTest extends MMINewActivity {


	private MediaRecorder mediaRecorder;
	private MediaPlayer mediaPlayer;
	private Button mStartButton;
	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
	private TextView mNoteText;
	private ProgressBar progressbar;
	private boolean openflag = false;
	private static final String TAG = "DeviceTestRecord";
	private File record_file ;

	private int mPlayCompleteTimes = 0;
	
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private AudioManager mAudioManager;
    
	@Override
	public void onCreate(Bundle savedInstanceState){

		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_test);
		setTitle(R.string.headset_record);
		
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

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mCount = 0;
		isFirstClick = true;
		
		record_file = new File("/data/data/com.bird.smt_sw/test.amr");
	}

	private void initRecord()
	{

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mediaRecorder.setOutputFile(record_file.getAbsolutePath());// /sdcard/test.amr
		
	}

//	private TimerTask task = new TimerTask()
//	{
//		@Override
//		public void run()
//		{
//			Message message = new Message();
//			message.what = 1;
//			mHandler.sendMessage(message);
//		}
//
//	};

	private Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case 1:
				playRecord();
				break;
			}
			super.handleMessage(msg);
		}
	};

	private void playRecord()
	{
		closeRecord();
		mNoteText.setText(R.string.start_play);
		mediaPlayer = new MediaPlayer();
		if (mediaPlayer.isPlaying()){
			mediaPlayer.reset();// ����Ϊ��ʼ״̬
		}
		
		try{
			mediaPlayer.setDataSource(new FileInputStream(record_file).getFD());
		} catch (IllegalArgumentException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try{
			mediaPlayer.prepare();
		} catch (IllegalStateException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// ����

		mediaPlayer.start();// ��ʼ��ָ�����
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){// ��������¼�

					@Override
					public void onCompletion(MediaPlayer arg0){
						mPlayCompleteTimes++;
						mediaPlayer.stop();
						mediaPlayer.release();
						mNoteText.setText(R.string.stop_play);
						progressbar.setVisibility(View.GONE);
					    mStartButton.setVisibility(View.INVISIBLE);	
					    mCount++;
					    
					}
				});

		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener(){// �������¼�

					@Override
					public boolean onError(MediaPlayer player, int arg1,
					int arg2)
					{
						mediaPlayer.release();
						return false;
					}
				});
	}

	private void closeRecord(){

		if (openflag){
			try{

				mediaRecorder.stop();
				mediaRecorder.reset();
				mediaRecorder.release();
				mNoteText.setText(R.string.stop_record);
			} catch (IllegalStateException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			openflag = false;
		}
	}

	@Override
	protected void onDestroy()	{

		Log.d(TAG, "onDestroy");
		super.onDestroy();
		//File record_file = new File("/data/data/com.bird.smt_sw/test.amr");

		if (record_file.exists()){
			Log.d(TAG, "onDestroy,test.amr is killed");
			record_file.delete();
		}
	}

	private void startRecord(){


		if (mAudioManager.isWiredHeadsetOn()){//(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

			mStartButton.setVisibility(View.INVISIBLE);
			mNoteText.setText(R.string.start_record);
			progressbar.setVisibility(View.VISIBLE);
		} else{
			mNoteText.setText(R.string.plug_in_headset);
			isFirstClick = true;
			return;
		}

		// ��5��������prepare����
		try{
			initRecord();				
			mediaRecorder.prepare();
			// ��6��������start������ʼ¼��
			mediaRecorder.start();
			openflag = true;
		} catch (IllegalStateException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Timer timer = new Timer();
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				Message message = new Message();
				message.what = 1;
				mHandler.sendMessage(message);
			}
	
		};			
		timer.schedule(task, 2000);//5000 //2000:2s录音

	
	}
	private View.OnClickListener mStartButtonHandler = new View.OnClickListener(){

		public void onClick(View v)	{

			if (mAudioManager.isWiredHeadsetOn()){//(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

				mStartButton.setVisibility(View.INVISIBLE);
				mNoteText.setText(R.string.start_record);
				progressbar.setVisibility(View.VISIBLE);
			} else{
				mNoteText.setText(R.string.plug_in_headset);
				return;
			}

			// ��5��������prepare����
			try{
				initRecord();				
				mediaRecorder.prepare();
				// ��6��������start������ʼ¼��
				mediaRecorder.start();
				openflag = true;
			} catch (IllegalStateException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e)	{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Timer timer = new Timer();
			TimerTask task = new TimerTask()
			{
				@Override
				public void run()
				{
					Message message = new Message();
					message.what = 1;
					mHandler.sendMessage(message);
				}
		
			};			
			timer.schedule(task, 2000);//5000  //2000:2s录音

		}
	};

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener(){

		public void onClick(View v){
			mEditor.putInt(MMIRecordTest.this.getString(R.string.headset_record), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};

	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener(){

		public void onClick(View v){
			mEditor.putInt(MMIRecordTest.this.getString(R.string.headset_record), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};

	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener(){

		public void onClick(View v){

			//Intent intent = null;
			//intent = new Intent(DeviceTestRecord.this, DeviceTestRecord.class);
			//startActivityForResult(intent, 0);
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIRecordTest.this, MMIRecordTest.class);
			intent.setComponent(comp);

			startActivity(intent);
			finish();
		}
	};
	private int mCount;
	private boolean isFirstClick;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIRecordTest.this, MMIRecordTest.class);
			intent.setComponent(comp);

			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			if((mCount == 0) && isFirstClick){
				isFirstClick = false;
				startRecord();
			} else if((mCount > 0) && !isFirstClick) {
				mEditor.putInt(MMIRecordTest.this.getString(R.string.headset_record), Test_launcherActivity.PASS);
				mEditor.commit();
				finish();
			}	

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
