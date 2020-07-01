package com.bird.smt_sw;

import java.io.IOException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;


public class MMIReceiverTest extends MMINewActivity {

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
	private MediaPlayer mediaPlayer;

	private final static String TAG = "MMITestReceiver";

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private WakeLock mWakeLock = null;
    
	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.receiver_test);
		setTitle(R.string.receiver);

		correctButton = (Button) findViewById(R.id.correct);
		errorButton = (Button) findViewById(R.id.error);
		resetButton = (Button) findViewById(R.id.reset);
		correctButton.setOnClickListener(mCorrectButtonHandler);
		errorButton.setOnClickListener(mErrorButtonHandler);
		resetButton.setOnClickListener(mRestdButtonHandler);

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
        mediaPlayer = new MediaPlayer();
		//mediaPlayer = MediaPlayer.create(this, R.raw.buzzer);//听筒放单频类 尖锐声音
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		////audioManager.setMode(AudioManager.MODE_IN_CALL);
		//audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
		//audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
		//AudioManager.FLAG_PLAY_SOUND);
		mediaPlayer.setLooping(true);
		Log.w(TAG, "onCreate MODE:" + audioManager.getMode());

		//playMP3();
		mHandler.removeCallbacks(mRunnable);
		mHandler.postDelayed(mRunnable, 1000);
	}
	private Handler mHandler = new Handler();
	private Runnable mRunnable = new Runnable(){

		@Override
		public void run() {
		    playMP3();
		}
		
	};
	
	@Override
	protected void onDestroy()
	{

		//mediaPlayer.release();

		//mediaPlayer = null;
//if(reset_token == 0)//xuzhenguo
		//recoverMode();

		super.onDestroy();

	}
        
    private void exitAction() {
        if (mediaPlayer != null) {
		    mediaPlayer.release();

		    mediaPlayer = null;
            mHandler.removeCallbacks(mRunnable);
		    recoverMode();
        }
    }

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }
        
	private void playMP3()
	{

		if (mediaPlayer.isPlaying())
		{

			mediaPlayer.reset();// ����Ϊ��ʼ״̬

		}
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);////
		mediaPlayer.setVolume(((AudioManager) getSystemService(AUDIO_SERVICE)).getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), ((AudioManager) getSystemService(AUDIO_SERVICE)).getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
		
        try {

        	setDataSourceFromResource(getResources(), mediaPlayer,
                    R.raw.buzzer);

        	mediaPlayer.prepareAsync();//   prepare
        	try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	mediaPlayer.start();

         } catch (IllegalArgumentException e) {

            e.printStackTrace();

         } catch (IllegalStateException e) {

            e.printStackTrace();

         } catch (IOException e) {

            e.printStackTrace();

         }
		/*
		try
		{
			mediaPlayer.prepare();

		} catch (IllegalStateException e)
		{

			// TODO Auto-generated catch block

			e.printStackTrace();

		} catch (IOException e)
		{

			// TODO Auto-generated catch block

			e.printStackTrace();

		}// ����

		mediaPlayer.start();// ��ʼ��ָ�����
        */
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
		{// ��������¼�

					@Override
					public void onCompletion(MediaPlayer arg0)
					{

						mediaPlayer.stop();

						mediaPlayer.release();
						correctButton.setVisibility(View.VISIBLE);
						errorButton.setVisibility(View.VISIBLE);
						//resetButton.setVisibility(View.VISIBLE);
					}

				});

		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener()
		{// �������¼�

					@Override
					public boolean onError(MediaPlayer player, int arg1,

					int arg2)
					{

						mediaPlayer.release();

						return false;

					}

				});
	}

	private void recoverMode()
	{

		Log.w(TAG, "recoverMode");

		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		audioManager.setMode(AudioManager.MODE_NORMAL);
	}
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIReceiverTest.this.getString(R.string.receiver), Test_launcherActivity.PASS);
			mEditor.commit();
			exitAction();
			finish();

		}
	};
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIReceiverTest.this.getString(R.string.receiver), Test_launcherActivity.FAIL);
			mEditor.commit();
			exitAction();
			finish();

		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
		        exitAction();
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIReceiverTest.this, MMIReceiverTest.class);
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

			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIReceiverTest.this.getString(R.string.receiver), Test_launcherActivity.PASS);
			mEditor.commit();
			exitAction();
			finish();
			return true;
		}
		    
		return super.onKeyDown(keyCode, event);
	}

}
