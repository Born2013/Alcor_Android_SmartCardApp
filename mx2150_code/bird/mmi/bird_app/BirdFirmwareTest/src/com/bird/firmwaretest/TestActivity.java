package com.bird.firmwaretest;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bird.flashlight.BirdFlashlightNative;
// caoyuangui add 20170324 begin
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
// caoyuangui add 20170324 end
import java.io.IOException;
import java.util.ArrayList;
/*[BIRD][闪光灯测试修改][luye][20170330]begin*/
import android.hardware.Camera.Parameters;
/*[BIRD][闪光灯测试修改][luye][20170330]end*/


public class TestActivity extends Activity implements TextureView.SurfaceTextureListener{
    private ArrayList<String> mSelectNameList = new ArrayList<String>();
    private int mSelectTime = 0;
    private boolean isHourFormat;

    private PowerManager.WakeLock mWakeLock;
    private static boolean isWakeUp = false;

    private Vibrator vibrater;

    private Camera mCamera;
    private TextureView camera_preview;
    private SurfaceTexture mSurfaceTexture;

    private MediaPlayer mSpeakerMediaPlayer;
    private MediaPlayer mReciverMediaPlayer;
    // caoyuangui add 20170324 begin
    private AudioManager mAudioManager;


    private boolean isRecever = false;
    private boolean isSperak = false;    
    // caoyuangui add 20170324 end

    private int frequency = 8000;
    private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    private int blockSize = 256;
    private RecordAudio recordTask;
    private TextView mMicTextView;
    private ImageView mMicView;
    private boolean started = false;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mPaint;

    private AlarmManager mAlarmManager;
    private AlarmReceiver mAlarmReceiver;

    private TestHandler mHandler = new TestHandler();

    private final static int SWITCH_CAMERA_GAP = 10;

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        android.util.Log.d("shujiang", "onSurfaceTextureAvailable");
        mSurfaceTexture = surfaceTexture;
        initCameraParameters();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        android.util.Log.d("shujiang", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        android.util.Log.d("shujiang", "onSurfaceTextureDestroyed");
        closeCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private class TestHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Util.SUB_CAMERA_TEST:
                    closeCamera();
                    openSubCamera();
                    break;
                case Util.MAIN_CAMERA_TEST:
                    closeCamera();
                    openMainCamera();
                /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
                    break;
                case Util.FLASHLIGHT_OPEN_TEST:
                    closeCamera();
                    //openSubCamera();
                    openFlashLight();
                    break;
                case Util.FLASHLIGHT_CLOSE_TEST:
                    closeCamera();
                    closeFlashLight();
                    break;
                /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Util.FINISH_TEST);

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mAlarmReceiver = new AlarmReceiver();
        registerReceiver(mAlarmReceiver, mIntentFilter);

        getOptionIntent();

        initView();
        init();
    }

    private void getOptionIntent() {
        Intent intent = getIntent();
        mSelectNameList.clear();
        mSelectNameList = intent.getStringArrayListExtra(Util.SELECT_TEST_OPTIONS);
        mSelectTime = intent.getIntExtra(Util.SELECT_TEST_TIMES, 0);
        String mTimeFormat = intent.getStringExtra(Util.TIME_FORMAT);
        if (mTimeFormat.equals(getString(R.string.hour_format))) {
            isHourFormat = true;
        } else {
            isHourFormat = false;
        }
    }


    private void init() {

        setAlarmIntent(Util.FINISH_TEST, mSelectTime);
        android.util.Log.d("shujiang1111", "init");
        initLcd();

        if (mSelectNameList.indexOf(getString(R.string.motor_test)) != -1) {
            initVibrator();
        }

        if (mSelectNameList.indexOf(getString(R.string.flashlight_test)) != -1) {
            initFlashLight();
        }

        if (mSelectNameList.indexOf(getString(R.string.camera_test)) != -1) {
            initCamera();
        }

        if (mSelectNameList.indexOf(getString(R.string.speaker_test)) != -1) {
            isSperak = true;
            initSpeaker();
        }

        if (mSelectNameList.indexOf(getString(R.string.mic_test)) != -1) {
            initMic();
        }

        if (mSelectNameList.indexOf(getString(R.string.handset_test)) != -1) {
            isRecever = true;
            initReciver();
        }

    }

    private void setAlarmIntent(String action, int duration) {
        Intent intent = new Intent(action);
        PendingIntent mPendingIntent = PendingIntent
                .getBroadcast(TestActivity.this, 0, intent, 0);
        long testDuration = 0;
        if (isHourFormat) {
            testDuration = (long)duration * 60 * 60 * 1000;
        } else {
            testDuration = (long)duration * 60 * 1000;
        }

        long triggerTime = SystemClock.elapsedRealtime() + testDuration;

        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, mPendingIntent);
    }




    private void initMic() {
        mMicTextView.setVisibility(View.VISIBLE);
        transformer = new RealDoubleFFT(blockSize);

        mBitmap = Bitmap.createBitmap(150, 100,
                Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mMicView.setImageBitmap(mBitmap);

        started = true;
        recordTask = new RecordAudio();
        recordTask.execute();

    }

    private void releaseMic() {
        if (recordTask != null) {
            started = false;
            recordTask.cancel(true);
        }
    }

    private void initSpeaker() {
        mSpeakerMediaPlayer = MediaPlayer.create(this, R.raw.test);

        AudioManager am = getAudioManager();
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am
                        .getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                AudioManager.FLAG_PLAY_SOUND);
        
        playMP3();

    }

    private void initReciver() {
        mReciverMediaPlayer = MediaPlayer.create(this, R.raw.testlife);

        AudioManager audioManager = getAudioManager();
        //audioManager.setMode(AudioManager.MODE_IN_CALL);
        audioManager.setMode( AudioManager.MODE_IN_COMMUNICATION);    
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                AudioManager.FLAG_PLAY_SOUND);  

        if (isRecever && !isSperak) {
            playSound();
        }

    }


    private void releaseMedia() {
        if (mSpeakerMediaPlayer != null) {
            mSpeakerMediaPlayer.stop();
            mSpeakerMediaPlayer.release();
            mSpeakerMediaPlayer = null;
        }
        if (mReciverMediaPlayer != null) {
            mReciverMediaPlayer.stop();
            mReciverMediaPlayer.release();
            mReciverMediaPlayer = null;
        }
    }

    private void playMP3() {
        getAudioManager().setSpeakerphoneOn(true);
        if (mSpeakerMediaPlayer != null && mSpeakerMediaPlayer.isPlaying()) {
            return;
        }
        try {
            mSpeakerMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                        android.util.Log.d("CAOYUANGUI","mSpeakerMediaPlayer isSperak:"+isSperak);       
                        if (isRecever) {
                            playSound();
                        } else {
                            playMP3();
                        }
                }
            });

            mSpeakerMediaPlayer.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        android.util.Log.d("CAOYUANGUI","mReciverMediaPlayer isRecever:"+isRecever);  
        mSpeakerMediaPlayer.setLooping(false);
        mSpeakerMediaPlayer.start();

    }

    private void playSound() {
        getAudioManager().setSpeakerphoneOn(false);
        if (mReciverMediaPlayer != null && mReciverMediaPlayer.isPlaying()) {
             return;
            //mReciverMediaPlayer.reset();
        }

        try {
            mReciverMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                        android.util.Log.d("CAOYUANGUI","mReciverMediaPlayer isSperak:"+isSperak);                        
                        if (isSperak) {
                            playMP3();      
                        } else {
                            playSound();
                        }                
                }
            });
            mReciverMediaPlayer.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        android.util.Log.d("CAOYUANGUI","mReciverMediaPlayer isSperak:"+isSperak);    
        mReciverMediaPlayer.setLooping(false);       
        mReciverMediaPlayer.start();


    }


    private void initCamera(){
        openMainCamera();
    }

    private void openMainCamera() {
        if (mCamera == null) {
            mCamera = Camera.open(0);
        }
        initCameraParameters();
        mHandler.sendEmptyMessageDelayed(Util.SUB_CAMERA_TEST, SWITCH_CAMERA_GAP*1000);
    }

    private void initCameraParameters() {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();

            parameters.setPreviewFpsRange(5000, 30000);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            
            if (FeatureOption.BIRD_TEST_CAMERA_NORMAL) {
                mCamera.setDisplayOrientation(90);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException t) {
            }
        }
    }

    private void openSubCamera() {
        if (mCamera == null) {
            mCamera = Camera.open(1);
        }
        initCameraParameters();
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        mHandler.sendEmptyMessageDelayed(Util.FLASHLIGHT_OPEN_TEST, SWITCH_CAMERA_GAP*1000);
        //mHandler.sendEmptyMessageDelayed(Util.MAIN_CAMERA_TEST, SWITCH_CAMERA_GAP*1000);
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void initView() {
        camera_preview = (TextureView) findViewById(R.id.camera_texture_view);
        camera_preview.setSurfaceTextureListener(this);
        mMicTextView = (TextView) findViewById(R.id.mic_txt);
        mMicView = (ImageView) findViewById(R.id.mic_view);

    }


    private void initLcd() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "TAG");
        if (!isWakeUp) {
            mWakeLock.acquire();
            isWakeUp = true;
        }
    }

    private void initVibrator() {
        vibrater = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {1000,2000,1000,2000};
        vibrater.vibrate(pattern, 0);
    }

    private void initFlashLight() {
        /*[BIRD][闪光灯测试修改][luye][20170330]begin*/
        /*if (!BirdFlashlightNative.isFlashLightOn()) {
            BirdFlashlightNative.opentFlashLight();
        }*/
        //openFlashLight();///*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        /*[BIRD][闪光灯测试修改][luye][20170330]end*/
    }
    /*[BIRD][闪光灯测试修改][luye][20170330]begin*/
    private Camera.Parameters parameters = null;
    private static final String TAG="TestActivity";
    private void openFlashLight() {
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        if (mCamera == null) {
            mCamera = Camera.open();
        }   
        parameters = mCamera.getParameters();
        parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mHandler.sendEmptyMessageDelayed(Util.FLASHLIGHT_CLOSE_TEST, SWITCH_CAMERA_GAP*1000);
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
    }
    
    private void closeFlashLight() {
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        if (mCamera == null) {
            mCamera = Camera.open();
        } 
        parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
        //mCamera.release();
        mHandler.sendEmptyMessageDelayed(Util.MAIN_CAMERA_TEST, SWITCH_CAMERA_GAP*1000);
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
    }
    /*[BIRD][闪光灯测试修改][luye][20170330]end*/

    private void removeAllMessage() {
        mHandler.removeMessages(Util.SUB_CAMERA_TEST);
        mHandler.removeMessages(Util.MAIN_CAMERA_TEST);
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        mHandler.removeMessages(Util.FLASHLIGHT_OPEN_TEST);
        mHandler.removeMessages(Util.FLASHLIGHT_CLOSE_TEST);
        /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mCamera != null) {
            camera_preview.setSurfaceTextureListener(null);
            closeCamera();
        }

        if (isWakeUp && mWakeLock != null) {
            mWakeLock.release();
            isWakeUp = false;
        }

        if (vibrater != null) {
            vibrater.cancel();
        }
        //*[BIRD][闪光灯测试修改][luye][20170330]begin*/
        /*if (BirdFlashlightNative.isFlashLightOn()) {
            BirdFlashlightNative.closeFlashLight();
        }*/
        //closeFlashLight();///*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
        /*[BIRD][闪光灯测试修改][luye][20170330]end*/

        releaseMedia();
        releaseMic();

        if (mAlarmReceiver != null) {
            unregisterReceiver(mAlarmReceiver);
            mAlarmReceiver = null;
        }
        removeAllMessage();
         
        // caoyuangui add 20170324 beging  
        recoverMode();
        isRecever = false;
        isSperak = false;
        // caoyuangui add 20170324 end          

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed
                        // 16
                        // bit
                    }

                    transformer.ft(toTransform);
                    publishProgress(toTransform);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }

            return null;
        }

        protected void onProgressUpdate(double[]... toTransform) {
            mCanvas.drawColor(Color.BLACK);

            for (int i = 0; i < toTransform[0].length; i++) {
                int x = i;
                int downy = (int) (100 - (toTransform[0][i] * 10));
                int upy = 100;

                mCanvas.drawLine(x, downy, x, upy, mPaint);
            }
            mMicView.invalidate();
        }
    }



    private class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Util.FINISH_TEST)) {
                Intent startIntent = new Intent(TestActivity.this, TestResultActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                android.util.Log.d("shujiang1111", "AlarmReceiver:FINISH_TEST");
                startActivity(startIntent);
                finish();
            }
        }
    }

    private void recoverMode() {
        getAudioManager().setMode(AudioManager.MODE_NORMAL);
    }

    private AudioManager getAudioManager() {
          if (mAudioManager == null) {
              mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
          }
          return mAudioManager;

    }

}
