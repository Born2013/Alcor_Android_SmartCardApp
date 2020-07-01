package com.sensortek.stkhealthcare2;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RelativeLayout;
import com.sensortek.stkhealthcare2.R;
import android.widget.Toast;
import com.sensortek.stkhealthcare2.provider.Heart;
import android.content.ContentResolver;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.os.PowerManager;
import android.text.TextUtils;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import java.util.Random;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.WindowManager;

public class PulseFragment extends Fragment implements Runnable {
    
    private String TAG = "PulseFragment";
    private TextView mRateValue,mRemind;
    private TextView mStartDetection,mDetectonState;
    private RelativeLayout mCircleRelativeLayout;
    private CircleTimerView mTime;
    private progressView mProgressView;
    private lineChartView mLineChartView;
    private Thread poll = null;
    private boolean isThreadExist = false;
    private final Handler handler = new Handler();
    private String catPulsePath = new String("cat /data/misc/pulse_res");
    final private String PULSE_RES_STR= new String("pulse_res");
    private boolean circleRunning = false;
    private final int MSG_PULSE = 0; 
    private final int MSG_PULSE_NO_RESULT = 2;
    private final int MSG_HEART_ANIMATION = 3;
    private final String runPulsedPath = "./system/bin/stkcaller -runpulse";
    private final String stopPulsedPath = "./system/bin/stkcaller -stoppulse";
    private final String STKCALLER_PATH = "/system/bin/stkcaller";
    private final int POLL_INTERVAL_MS = 100;
    private static final int LV_MES_COMPLE_SINGLE_MES = 200;
    private final int LV_INIT_NOISE = 20;
    private int lv_sound_speed = 8;
    private final int LV_EVERY_RETRY = 10;		
    private int mRetrylimit = 10;
    private int cat_null_data = 0;
    private int invalidLastVal = 0;		
    private int validLastVal = 0;		
    private int soundLastVal = 0;		
    private int retryCounter = 0;
    private int mFakeValidLastVal = 0;		
    private boolean runStkcaller = false;
    private final String BROADCAST_ENABLE_STR = new String("com.sensortek.broadcast.pulse.enable");	// front PS
    private final String BROADCAST_DISABLE_STR = new String("com.sensortek.broadcast.pulse.disable");	// front PS		
    private final int steNO = 0, steMes = 1,  steOK = 2, steFail = 3;
    private int mesState = steNO;		
    private int pulseSum = 0;
    private int pulseSumNum = 0;
    private int mFakeRateSum = 0;
    private int mFakeRateSumNum = 0;
    private int sourceid;
    private SoundPool spool;
    private int lastVal = 0;
    private int resultPulse100 = 0;												
    private long lastInterval;
    public static final int NO_START = 100;
    public static final int WAIT_FONT = 101;
    public static final int DETECTION= 102;
    public static final int DETECTION_COMPLETE= 103;
    public static int mState = NO_START;		
    public static boolean mIsCompleted = false;	
    private int mBmp;
    private SimpleDateFormat mFormatter;
    private ImageView mRateIcon;
    private PowerManager.WakeLock mWakeLock;
    private boolean mHasStarted = false;
    private int mBrightValue = 0;
    private int mCriticalValue = 24000;
    private int mFakeResult = 0;
    private int mFakeCounter = 0;
    public static boolean isNewInsertData = false;
    private boolean mHasShowToast = false;
    private boolean mIsStoped = false;	

    private Handler mHander=new Handler(){
        public void handleMessage(android.os.Message msg) {
            if(msg.what == MSG_HEART_ANIMATION){               
            	startHeartBeatAnimation();; 
            }
        };
    };
    
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG,"onCreate");		         
            spool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
            sourceid = spool.load(getActivity().getApplicationContext(), R.raw.heartbeat, 1);
            File stkcallerFile = new File(STKCALLER_PATH);
            if (stkcallerFile.exists()) {		        
                Log.d(TAG, "stkcaller was found");
                if(stkcallerFile.canExecute()) {
                    runStkcaller = true;
                } else {
                    Log.d(TAG, "No permission to execute stkcaller");		
                }
            } else {
                Log.d(TAG, "No stkcaller executable");
            }
            createPulse_res();
        }
	 
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {	   
        Log.d(TAG,"onCreateView");
        View layoutView = inflater.inflate(R.layout.pulse_layout, container,false);       
        mTime = (CircleTimerView)layoutView.findViewById(R.id.pulse_time);
        mProgressView = (progressView)layoutView.findViewById(R.id.grogress_view);
        mLineChartView = (lineChartView)layoutView.findViewById(R.id.linechart_view);
        return layoutView;
    }	

    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        stopDetection();
        releaseLock();
        super.onPause();
    }

    @Override
    public void onResume() {	 
        super.onResume();
        Log.d(TAG,"onResume");
        mFormatter = new SimpleDateFormat("yyyy-MMdd-HH:mm");     
        mRateValue = (TextView)getView().findViewById(R.id.rate_value);
        mRemind = (TextView)getView().findViewById(R.id.remind);
        mStartDetection = (TextView)getView().findViewById(R.id.start_detection);
        mDetectonState = (TextView)getView().findViewById(R.id.detection_state);
        mCircleRelativeLayout = (RelativeLayout) getView().findViewById(R.id.relativeLayout2);
        mCircleRelativeLayout.setOnClickListener(mDetectionListener);	
        mRateIcon = (ImageView)getView().findViewById(R.id.rate_icon);
        mState = NO_START;
        refreshState();
        circleEarlyInit();
        
    }	  	    
		

    private void refreshState() {
        if (mState == NO_START) {
            mIsCompleted = false;
            mRemind.setVisibility(View.GONE);
            mRateValue.setText("000");
            mRateIcon.setImageResource(R.drawable.ic_heartbeat_dis);   
            mDetectonState.setVisibility(View.GONE);
            mIsCompleted = false;
            mStartDetection.setText(getResources().getString(R.string.click_start_detection));	
            mProgressView.setVisibility(View.GONE);
            mLineChartView.stopAnimation();
            stopHeartBeatThread();
        } else if (mState == WAIT_FONT) {
            mIsCompleted = false;
            //mRemind.setVisibility(View.VISIBLE);
            mRateValue.setText("000");
            mRateIcon.setImageResource(R.drawable.ic_heartbeat_dis);   
            mDetectonState.setVisibility(View.GONE);
            mStartDetection.setText(getResources().getString(R.string.click_to_end));	
            mProgressView.setVisibility(View.GONE);
            mLineChartView.stopAnimation();
            stopHeartBeatThread();
        } else if (mState == DETECTION) {
            mIsCompleted = false;
            mRemind.setVisibility(View.GONE);
            mRateIcon.setImageResource(R.drawable.ic_heartbeat_nor); 
            mDetectonState.setText(getResources().getString(R.string.detection));
            mDetectonState.setVisibility(View.VISIBLE);
            mStartDetection.setText(getResources().getString(R.string.click_to_end));	
            mProgressView.setVisibility(View.GONE);
            mLineChartView.startAnimation();
        } else if (mState == DETECTION_COMPLETE) {
            mIsCompleted = true;
            mRemind.setVisibility(View.GONE);
            mRateIcon.setImageResource(R.drawable.ic_heartbeat_nor); 
            mDetectonState.setText(getResources().getString(R.string.end_detection));
            mDetectonState.setVisibility(View.VISIBLE);
            mStartDetection.setText(getResources().getString(R.string.click_to_restart));	
            mProgressView.setVisibility(View.VISIBLE);
            mLineChartView.stopAnimation();
            stopHeartBeatThread();
        } else {
            mProgressView.setVisibility(View.GONE);
            mLineChartView.stopAnimation();
            mIsCompleted = false;
            mStartDetection.setText(getResources().getString(R.string.click_start_detection));	
            stopHeartBeatThread();
        }

    }

    private void stopDetection() {
        Log.d(TAG,"stopDetection");
        mIsStoped = true;
        releaseLock();
        mState = NO_START;
        refreshState();
        stopPolling();
        execStkgesd(false);
        mRemind.setVisibility(View.GONE);
        circleFinal();	
    }
    
    OnClickListener mDetectionListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG,"onClick mesState = "+mesState+",mState = "+mState);
            mHasShowToast = false;
            MainActivity activity = (MainActivity)getActivity();
            if (activity.isTop()) {
                activity.startToBottomAnimator();
            }
            if (mState == NO_START || mState == DETECTION_COMPLETE) {
                mIsStoped = false;
                measStart();
            } else if (mState == WAIT_FONT || mState == DETECTION) {
                stopDetection();
            }
        }	   
    };
        

    public void playSound() {
        Log.d(TAG,"playSound");
        AudioManager am = (AudioManager) getActivity().getApplicationContext()
        .getSystemService(Context.AUDIO_SERVICE);
        float audMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float audCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        float volRatio = audCurrentVolumn / audMaxVolumn; 
        spool.play(sourceid, volRatio, volRatio, 10, 0, 1);
    }
		 

    void showToast() {
        Log.d(TAG,"showToast");
        if (mHasShowToast) {
            return;
        }
        Toast toast = Toast.makeText(getActivity().getApplicationContext(),
            getResources().getString(R.string.no_data_detected), Toast.LENGTH_SHORT);
        toast.show();
        mHasShowToast = true;
    }
		
    private void measStart() {
        Log.d(TAG,"measStart");
        MainActivity activity = (MainActivity)getActivity();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        mState = WAIT_FONT;
        refreshState();  
        execStkgesd(true);					
        pulseAveClr();
        retryCounter = 0;					
        mesState = steMes;
        validLastVal = 0;
        invalidLastVal = 0;
        mFakeValidLastVal = 0;
        startPolling();
        mRateValue.setText("000");
        circleInit();		
    }
		
		
    private void measComplete() {
        Log.d(TAG,"measComplete");
        mState = DETECTION_COMPLETE;
        refreshState();
        final ContentResolver cr = getActivity().getContentResolver();
        Date curDate = new Date(System.currentTimeMillis()); 
        String now = mFormatter.format(curDate); 
        Heart heart = new Heart(mBmp,now);
        Heart.addHeart(cr,heart);
        isNewInsertData = true;
        mProgressView.refreshCurrentValue(mBmp);
        stopPolling();
        mesState = steOK;
        execStkgesd(false);
        circleFinal();		
        Vibrator myVibrator = (Vibrator) getActivity().getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(50);
        releaseLock();
    }

    private void measFail() {
        Log.d(TAG,"measFail");
        showToast();
        mState = NO_START;
        refreshState();
        mRemind.setVisibility(View.GONE);
        circleFinal();	
        mesState = steFail;
        mRateValue.setText("000");
        stopPolling();
        execStkgesd(false);
        releaseLock();
    }
		
	private int pulseAveSet() {
        if (mBrightValue > mCriticalValue && resultPulse100 == -100) {
            if (mFakeCounter % 20 == 0) {
                mFakeResult = getRandom();
                mFakeRateSum += mFakeResult / 100;
                mFakeRateSumNum++;	
            }
            mFakeCounter ++;
            return mFakeRateSum / mFakeRateSumNum;
        } else {
            mFakeCounter = 0;
            mFakeRateSum = 0;
            mFakeRateSumNum = 0;
            pulseSum += resultPulse100 / 100;
            pulseSumNum++;	
            return pulseSum / pulseSumNum;
        }        
	}
		
	private void pulseAveClr() {
        pulseSum  = 0;
        pulseSumNum = 0;	
        mFakeCounter = 0;
        mFakeRateSum = 0;
        mFakeRateSumNum = 0;
	}

    private void valueValid() {
        invalidLastVal = 0;
        validLastVal++;		
        if(validLastVal >= LV_MES_COMPLE_SINGLE_MES) {	
            if (!mIsCompleted){
                measComplete();
            }
        } else {
            circleStart();
            circleUpdate();

            if (mBrightValue > mCriticalValue && resultPulse100 == -100) {
                mFakeValidLastVal ++;
                mBmp = pulseAveSet();
                mRateValue.setText(getBpm(mBmp));
            } else {
                if(validLastVal > LV_INIT_NOISE + mFakeValidLastVal) {
                    if(resultPulse100 != 0) {
                        mBmp = pulseAveSet();
                        mRateValue.setText(getBpm(mBmp));
                    }
                }
            }
            //startHeartBeatThread();
            if((validLastVal - soundLastVal) % lv_sound_speed == 0) {
                soundLastVal = validLastVal;
                if (mBrightValue > mCriticalValue && resultPulse100 == -100) {                   
                    lv_sound_speed = (int)((float)60.0*(1000/POLL_INTERVAL_MS)/(mFakeResult/100));
                } else {
                    if(resultPulse100 != 0) {
                        lv_sound_speed = (int)((float)60.0*(1000/POLL_INTERVAL_MS)/(resultPulse100/100));
                    }
                }
                startHeartBeatAnimation();
                //playSound();
                if (validLastVal > 3) {
                    mLineChartView.hasHeart();
                }                
            }	
        }		
    }

    private void valueInvalid() {
        if (invalidLastVal >= 3) {
            mRemind.setVisibility(View.VISIBLE);
        }
        invalidLastVal++;
        validLastVal = 0;
        soundLastVal = 0;
        mFakeValidLastVal = 0;
        mRateValue.setText("000");

        if(circleRunning == true) {
            circleRunning = false;
            circleFinal();
            circleInit();	
        }
        if(retryCounter < mRetrylimit) {	
            if(invalidLastVal == LV_EVERY_RETRY) {
                retryCounter++;
                invalidLastVal = 0;
                Log.d(TAG, "Restart measurement");
                pulseAveClr();	
            }
        } else {
            measFail();
        }			
    }

        
    private int prasePulseStr(String pPulseInfo) {
        Scanner scanner = new Scanner(pPulseInfo);
        String[] psString = new String[3]; 
        if(mesState != steMes)
        return 0;

        for(int i=0;i<3;i++) {
            if(scanner.hasNext()) {
                psString[i] = scanner.next();
            } else {
                Log.i(TAG, "invalid length:" + String.valueOf(i)+ ",str:"+psString[0]+","+psString[1]+","+psString[2]);						
                return -1;						
            }				
        }
        scanner.close();
        Log.i(TAG, "str:"+psString[0]+","+psString[1]+","+psString[2]);

        mBrightValue = Integer.valueOf(psString[0]);
        if(mBrightValue == 0xFFFF) {
            Log.e(TAG, "invalid ps");
            return -2;
        }

        resultPulse100 = Integer.valueOf(psString[1]);
        if(resultPulse100 == 0xFFFF) {
            Log.e(TAG, "invalid pulse100");
            return -3;
        } else if (resultPulse100 == 0) {
            resultPulse100 = -100;
        }

        lastVal = Integer.valueOf(psString[2]);	
        if(lastVal == 0xFFFF) {
            Log.e(TAG, "invalid lastVal");
            return -4;
        }
        return 0;
    }
		
    private void handleMessage(Message msg){
        Log.d(TAG,"handleMessage msg.what = "+msg.what);
        switch (msg.what) {
            case MSG_PULSE:
                if (mIsCompleted || mIsStoped) {
                    return;
                }
                int ret;
                Bundle bundle = msg.getData();
                String pulseInfo =  bundle.getString("pulse");
                if (TextUtils.isEmpty(pulseInfo)) {
                    return;
                }
                ret = prasePulseStr(pulseInfo);
                if(ret < 0)
                    return;
                if (mBrightValue > mCriticalValue && resultPulse100 == -100) {
                    mState = DETECTION;
                    mRemind.setVisibility(View.GONE);
                    valueValid();	
                } else {
                    if(resultPulse100 == -100) {	
                        if (mState == DETECTION) {
                            mState = WAIT_FONT;
                        }
                        valueInvalid();
                        pulseAveClr();
                    } else {
                        mState = DETECTION;
                        mRemind.setVisibility(View.GONE);
                        valueValid();										
                    }
                }
                refreshState();
                break;
            case MSG_PULSE_NO_RESULT:
                cat_null_data++;
                Log.d(TAG, "ERROR: can't "+ catPulsePath);
                if(cat_null_data > 3) {
                    Log.d(TAG, "ERROR: stop stkgesd");
                    execStkgesd(false);						
                    stopPolling();
                }
                break;
        }
    }		
		
    void execStkgesd(boolean run) {	
        Log.d(TAG,"execStkgesd");
        Process process = null;
        try {
            if(run) {						
                if(runStkcaller) {
                    process = Runtime.getRuntime().exec(runPulsedPath);
                    Log.d(TAG, "Enable pulse via stkcaller");
                } else {
                    Intent intent = new Intent();
                    intent.setAction(BROADCAST_ENABLE_STR);
                    getActivity().sendBroadcast(intent);
                    Log.d(TAG, "enable pulse via broadcast");
                }
            } else {
                if(runStkcaller) {
                    process = Runtime.getRuntime().exec(stopPulsedPath);
                    Log.d(TAG, "Disable pulse via stkcaller");
                } else {
                    Intent intent = new Intent();
                    intent.setAction(BROADCAST_DISABLE_STR);
                    getActivity().sendBroadcast(intent);
                    Log.d(TAG, "Disable pulse via broadcast");
                }							
            }
            if(runStkcaller) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  
        } finally {
            if (process != null) {
                try {
                    process.getOutputStream().close();
                    process.getInputStream().close();
                    process.getErrorStream().close();
                } catch (IOException e) {	
                    e.printStackTrace();
                }
            }			
        }
        return;							
    }		
		
    public String getPulseResult() {
        Process process = null;
        try {			
            process = Runtime.getRuntime().exec(catPulsePath);
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DataInputStream osRes = new DataInputStream(
            process.getInputStream());
            String temp = null;
            temp = osRes.readLine();
            osRes.close();
            return temp;
        } catch (IOException e) {
            e.printStackTrace();  
        } finally {
            if (process != null) {
                try {
                    process.getOutputStream().close();
                    process.getInputStream().close();
                    process.getErrorStream().close();
                } catch (IOException e) {	
                    e.printStackTrace();
                }
            }			
        }
        return null;
    }
		
		
    Runnable polling = new Runnable() {
        public void run() {
            String pulse;
            pulse = getPulseResult();
            if(pulse != null) {
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                msg.what = MSG_PULSE;
                bundle.putString("pulse", pulse);
                msg.setData(bundle);
                handleMessage(msg);
            } else {
                Log.e(TAG, "can't find "+catPulsePath);
                Message msg = Message.obtain();
                msg.what = MSG_PULSE_NO_RESULT;
                handleMessage(msg);					
            }
        }
    };		
		
    public void startPolling() {
        if (isThreadExist) {
            return;
        }
        isThreadExist = true;
            poll = new Thread() {
            public void run() {
            while (isThreadExist) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue; 
                }
                handler.removeCallbacks(polling);
                handler.post(polling);
                }
            }
        };
        cat_null_data = 0;
        poll.start();
    }
		
    public void stopPolling() {
        isThreadExist = false;
        poll = null;
    }

	private void circleEarlyInit() {
		mTime.readFromSharedPref();
		mTime.postInvalidate();				
	}
	
	
	private void circleInit() {
		mTime.reset();							
		lastInterval = LV_MES_COMPLE_SINGLE_MES;
		mTime.setIntervalTime(lastInterval);			        
		mTime.setPassedTime((long)0, true);		
	}
	
	private void circleStart() {
		if(circleRunning == false) {
			circleRunning = true;
			if (mTime.isAnimating()) {
				mTime.startIntervalAnimation();				
			}
		}
	}
	
    private void circleUpdate() {		
        mTime.setTimeNow(validLastVal);			
    }

    private void circleFinal() {
        mTime.stopIntervalAnimation();
    }
	
    void createPulse_res() {	
        try {
            @SuppressWarnings("deprecation")
            FileOutputStream outStream = getActivity().getApplicationContext().
            openFileOutput(PULSE_RES_STR, Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            String buf = "0,-100,0";
            outStream.write(buf.getBytes());																							
            outStream.close();

        } catch (FileNotFoundException e) {
            return;
        }
        catch (IOException e){
            return ;
        }
        catPulsePath = "cat " + getActivity().getApplicationContext().getFilesDir() + "/" + PULSE_RES_STR;
    }

    private String getBpm(int value) {
        int length = String.valueOf(value).length();
        if (length == 1) {
            return "00"+ String.valueOf(value);
        } else if (length == 2) {
            return "0"+ String.valueOf(value);
        }
        return String.valueOf(value);
    }

    private PowerManager.WakeLock createWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "heart");
    }

    public void releaseLock() {
        MainActivity activity = (MainActivity)getActivity();
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		
    }

    public void startHeartBeatThread() {
        if (mHasStarted) {
            return;
        }
        mHasStarted = true;
        Thread thread = new Thread(this);  
        thread.start();  
    }

    public void stopHeartBeatThread() {
        mHasStarted = false;
    }

	@Override
	public void run() {
        while (mHasStarted && mState == DETECTION) { 
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }           
            mHander.sendEmptyMessage(MSG_HEART_ANIMATION); 
        }
	}

    public void startHeartBeatAnimation() {
        Log.d("heartanimation","startHeartBeatAnimation mState = "+mState);
        //if (mState == DETECTION && mHasStarted) {
            mRateIcon.setImageResource(R.drawable.ic_heartbeat_nor);        	
            ObjectAnimator scaley = ObjectAnimator.ofFloat(mRateIcon, "scaleY", 1f, 1.7f, 1f);  
            ObjectAnimator scalex = ObjectAnimator.ofFloat(mRateIcon, "scaleX", 1f, 1.7f, 1f); 
            AnimatorSet animSet = new AnimatorSet(); 
            animSet.setInterpolator(new AccelerateDecelerateInterpolator());
            animSet.playTogether(scaley,scalex);
            animSet.setDuration(450);  
            animSet.start();  
        //}
    }

    private int getRandom() {   
        Random random = new Random();
        int value = random.nextInt(10);
        return (value + 65) * 100;
    }

    public void changeAlpha(float alpha) {
        mTime.setAlpha(alpha);
        mProgressView.setAlpha(alpha);
    }
}
