package com.bird.smt_sw;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MMIMicRecordTest extends MMINewActivity {

	private MediaRecorder mediaRecorder;
	private MediaPlayer mediaPlayer;
	private Button mStartButton;
	private Button correctButton;
	private Button errorButton;
	//private Button resetButton;
	private TextView mNoteText;
	private ProgressBar progressbar;
	private boolean openflag = false;
	private static final String TAG = "MMIRecordMainBoardMic";

	private int mPlayCompleteTimes = 0;
	
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private AudioManager mAudioManager;
    
	boolean isRecording = false;//是否录放的标记
	static final int frequency = 44100;//44100;
	static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	int recBufSize,playBufSize;
	AudioRecord audioRecord;
	AudioTrack audioTrack;
	
	@Override
	public void onCreate(Bundle savedInstanceState){

		Log.d(TAG, "! onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.record_test_amend);
		setTitle(R.string.board_record);
		
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
		mStartButton = (Button) findViewById(R.id.start_record);
		correctButton = (Button) findViewById(R.id.correct);
		errorButton = (Button) findViewById(R.id.error);
		//resetButton = (Button) findViewById(R.id.reset);
		mNoteText = (TextView) findViewById(R.id.note_title);
		progressbar = (ProgressBar) findViewById(R.id.progressbar);
		mStartButton.setOnClickListener(mStartButtonHandler);
		correctButton.setOnClickListener(mCorrectButtonHandler);
		errorButton.setOnClickListener(mErrorButtonHandler);
		//resetButton.setOnClickListener(mRestdButtonHandler);

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//mAudioManager.setMode(AudioManager.MODE_IN_CALL);
		//mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
		//		mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
		//        AudioManager.FLAG_PLAY_SOUND);
		mCount = 0;
		isFirstClick = true;
		
		recBufSize = AudioRecord.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);

		playBufSize=AudioTrack.getMinBufferSize(frequency,
				channelConfiguration, audioEncoding);
		// -----------------------------------------
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
				channelConfiguration, audioEncoding, recBufSize);

		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,  //STREAM_MUSIC
				channelConfiguration, audioEncoding,
				playBufSize, AudioTrack.MODE_STREAM);
		audioTrack.setStereoVolume(0.3f, 0.3f);//设置当前音量大小
		//setReceiverMode();//set receiver
	}
	
	private void setReceiverMode(){
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_CALL);
		audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
		audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
		AudioManager.FLAG_PLAY_SOUND);
		//mediaPlayer.setLooping(true);
		Log.d(TAG, "onCreate MODE:" + audioManager.getMode());
	}
	
	private void recoverMode(){

		Log.d(TAG, "recoverMode");

		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		audioManager.setMode(AudioManager.MODE_NORMAL);
	}

	@Override
	protected void onDestroy()	{

		Log.d(TAG, "onDestroy");
		super.onDestroy();
		//recoverMode();//set receiver
		//audioTrack.release();
		//audioRecord.release();
		//android.os.Process.killProcess(android.os.Process.myPid());
		if(mRecordPlayThread != null){
			mRecordPlayThread.interrupt();
			mRecordPlayThread = null;
		}
	}

	private void startRecord(){


		if (!mAudioManager.isWiredHeadsetOn()){//(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

			mStartButton.setVisibility(View.INVISIBLE);
			mNoteText.setText(R.string.start_record);
			progressbar.setVisibility(View.VISIBLE);
			isFirstClick = false;
			isRecording = true;
			
			if(mRecordPlayThread == null)
			mRecordPlayThread = new RecordPlayThread();
			mRecordPlayThread.start();// 开一条线程边录边放
		} else{
			mNoteText.setText(R.string.plug_out_headset);
			isFirstClick = true;
			return;
		}
	
	}
	private View.OnClickListener mStartButtonHandler = new View.OnClickListener(){

		public void onClick(View v)	{

			if (!mAudioManager.isWiredHeadsetOn()){//(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

				mStartButton.setVisibility(View.INVISIBLE);
				mNoteText.setText(R.string.start_record);
				progressbar.setVisibility(View.VISIBLE);
				isFirstClick = false;
				isRecording = true;
				Log.d(TAG, "mRecordPlayThread ="+mRecordPlayThread);
				if(mRecordPlayThread == null)
				mRecordPlayThread = new RecordPlayThread();
				mRecordPlayThread.start();// 开一条线程边录边放
			} else{
				mNoteText.setText(R.string.plug_out_headset);
				
				return;
			}

		}
	};

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener(){

		public void onClick(View v){
			isRecording = false;
			mEditor.putInt(MMIMicRecordTest.this.getString(R.string.board_record), 1);
			mEditor.commit();
			MMIMicRecordTest.this.finish();
		}
	};

	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener(){

		public void onClick(View v){
			isRecording = false;
			mEditor.putInt(MMIMicRecordTest.this.getString(R.string.board_record), 2);
			mEditor.commit();
			MMIMicRecordTest.this.finish();
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

			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			if(isFirstClick){
				startRecord();

			} else if(!isFirstClick) {
				isRecording = false;
				mEditor.putInt(MMIMicRecordTest.this.getString(R.string.board_record), 1);
				mEditor.commit();
				MMIMicRecordTest.this.finish();
			}	

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
	private RecordPlayThread mRecordPlayThread = null;
	class RecordPlayThread extends Thread {
		public void run() {
			try {
				byte[] buffer = new byte[recBufSize];
				audioRecord.startRecording();//开始录制
				audioTrack.play();//开始播放
				Log.d(TAG, "run  isRecording="+isRecording);
				while (isRecording) {
					//从MIC保存数据到缓冲区
					int bufferReadResult = audioRecord.read(buffer, 0,
							recBufSize);

					byte[] tmpBuf = new byte[bufferReadResult];
					System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
					//写入数据即播放
					audioTrack.write(tmpBuf, 0, tmpBuf.length);
				}
				Log.d(TAG, "!RecordPlayThread   before stop  isRecording ="+isRecording);
				audioTrack.stop();
				audioRecord.stop();
				audioTrack.release();
				audioRecord.release();
				audioTrack = null;
				audioRecord = null;
			} catch (Throwable t) {
				//Toast.makeText(MMIRecordMainBoardMic.this, t.getMessage(), 1000);
			}
		}
	};

}
