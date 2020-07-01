package com.bird.smt_sw;

import java.io.IOException;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.fmradio.FmNative;

public class MMIFMTest extends MMINewActivity {

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
	private Button mTuneButton;
	private TextView mInsertHeadset;
	static final String TAG = "FMTEST";
    private int FIXED_STATION_FREQ = 962;//918;
    private MediaPlayer mMP = null;
    public float mFreq = (float)88.0;
    
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    private HeadsetReceiver mHeadsetReceiver;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
        boolean open_ret = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fm);
    	setTitle(R.string.fm);
        Log.d(TAG, "$ oncreate");

        mInsertHeadset = (TextView) findViewById(R.id.headset_insert);
	    correctButton = (Button) findViewById(R.id.correct);
	    errorButton = (Button) findViewById(R.id.error);
	    resetButton = (Button) findViewById(R.id.reset);
        mTuneButton = (Button) findViewById(R.id.fm_tune);// 去掉搜索按钮

        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mTuneButton.setOnClickListener(mTuneButtonHandler);// 去掉搜索按钮

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
		registerHeadsetReceiver();
		
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
	
	//audioManager.setStreamVolume(AudioManager.STREAM_FM,
	//	audioManager.getStreamMaxVolume(AudioManager.STREAM_FM),
	//	AudioManager.FLAG_PLAY_SOUND);
		
        boolean open = FmNative.openDev();
        Log.d(TAG, "$ oncreate open=" +open);
        boolean powrUp =FmNative.powerUp((float)FIXED_STATION_FREQ/10);
        Log.d(TAG, "$ oncreate powrUp=" +powrUp);
        FmNative.setMute(false);
        mMP = new MediaPlayer();
		if (mMP.isPlaying()){
			mMP.reset();// ����Ϊ��ʼ״̬
		}
        try {
            mMP.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
            mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMP.prepare();
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
        Log.d(TAG, "$ oncreate mMP start=" );
        mMP.start();
	}
    
    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(TAG, "$ onResume");
	}


	@Override
    public void onStop() {

    	super.onStop();
    	Log.d(TAG, "$ onStop");
       // if(mMP.isPlaying()) {
       //     mMP.stop();
      //      mMP.release();
      //  }

      //  FmNative.powerDown(0);
      //  FmNative.closeDev();
    }

    
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG, "$ onDestroy");
        //if(mMP.isPlaying()) {
        //    mMP.stop();
        //    mMP.release();
        //}

        unregisterReceiver(mHeadsetReceiver);
        //FmNative.powerDown(0);
        //FmNative.closeDev();
	}

        private void exitAction() {
            if(mMP.isPlaying()) {
                mMP.stop();
                mMP.release();
            }
            FmNative.powerDown(0);
            FmNative.closeDev();
        }

	private View.OnClickListener mTuneButtonHandler=new View.OnClickListener() {

        public void onClick(View v) {

        	Slog.d(TAG, "mFreq=" +mFreq );
	        mFreq=FmNative.seek(mFreq, true);
	        Slog.d(TAG, "mFreq=" +mFreq );
           	FmNative.tune(mFreq);
           	
          	FmNative.setMute(false);
           	if (mMP.isPlaying()){
			mMP.reset();// ����Ϊ��ʼ״̬
		}
        try {
            mMP.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
            mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
            
            mMP.prepare();
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

        mMP.start();
        }
    };

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {
			mEditor.putInt(MMIFMTest.this.getString(R.string.fm), Test_launcherActivity.PASS);
			mEditor.commit();
			exitAction();
			finish();
		}
	};

	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {
			mEditor.putInt(MMIFMTest.this.getString(R.string.fm), Test_launcherActivity.FAIL);
			mEditor.commit();
			exitAction();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
		        exitAction();
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIFMTest.this, MMIFMTest.class);
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
		if(true){//去掉 搜索按钮
        	Slog.d(TAG, "mFreq=" +mFreq );
	        mFreq=FmNative.seek(mFreq, true);
	        Slog.d(TAG, "mFreq=" +mFreq );
           	FmNative.tune(mFreq);
           	FmNative.setMute(false);
           	
           	if (mMP.isPlaying()){
			mMP.reset();// ����Ϊ��ʼ״̬
		}
        try {
            mMP.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
            mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);
            
            mMP.prepare();
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

        mMP.start();
           	}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIFMTest.this.getString(R.string.fm), Test_launcherActivity.PASS);
			mEditor.commit();
			exitAction();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
	
	private class HeadsetReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(intent.hasExtra("state")){
				if(intent.getIntExtra("state", 0) == 1){
					Toast.makeText(context, getString(R.string.headset_connected), Toast.LENGTH_LONG).show();
					//mInsertHeadset.setVisibility(View.INVISIBLE);
					Message msg = new Message();
					msg.what = 1;
					mHandler.sendMessageDelayed(msg, 200);
				} else if(intent.getIntExtra("state", 0) == 0){
					//Toast.makeText(context, "not connected", Toast.LENGTH_LONG).show();
					//mInsertHeadset.setVisibility(View.VISIBLE);
					Message msg = new Message();
					msg.what = 2;
					mHandler.sendMessageDelayed(msg, 200);
				}
			}
		}
	}
	
    private void registerHeadsetReceiver() {
		// TODO Auto-generated method stub
    	mHeadsetReceiver = new HeadsetReceiver();
    	IntentFilter iFilter= new IntentFilter();
    	iFilter.addAction("android.intent.action.HEADSET_PLUG");
    	registerReceiver(mHeadsetReceiver, iFilter);
	}
	
    private Handler mHandler = new Handler(){
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	        case 1:
	            mInsertHeadset.setVisibility(View.INVISIBLE);
	            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
	
	            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
		          audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
		          AudioManager.FLAG_PLAY_SOUND);
	        break;
	        case 2:
	            //mInsertHeadset.setVisibility(View.VISIBLE);
	        break;
	    }
	    super.handleMessage(msg);
	}
    };
}
