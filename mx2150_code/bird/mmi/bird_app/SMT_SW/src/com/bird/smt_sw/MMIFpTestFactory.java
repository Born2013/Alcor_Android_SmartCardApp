package com.bird.smt_sw;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.util.Log;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.content.Intent;
import android.view.View;
import android.graphics.Color;
import android.view.WindowManager;

import android.content.ComponentName;
import android.content.Context;
import java.util.concurrent.Executors;
import android.os.Message;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.RemoteException;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.widget.Toast;
import android.os.CancellationSignal;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;

public class MMIFpTestFactory extends MMINewActivity {

    private final static String TAG = "MMIFpTestFactory";	
    private ImageView mImageView = null;
    private TextView mTextView = null;
    private TextView mResult;

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    protected Button mTest;
    
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private boolean mEnrolling;
    private boolean mIsHardwareDetected = false;
    protected FingerprintManager fpManager;
    private CancellationSignal mEnrollmentCancel;
    private byte[] mToken;
    FingerprintManager mFPManager;
    private int mUserId;
    
    private static final int ENROLL_TIMEOUT_MS = 10000;
    private static final int MSG_ENROLL_TIMEOUT = 100;
    private static final int MSG_ENROLL_ERROR = 101;
    private static final int MSG_ENROLL_SUCCESS = 102;

    private static final int FINGERPRINT_ACQUIRED_FINGER_DOWN = 1102;
    private static final int FINGERPRINT_ACQUIRED_FINGER_UP = 1103;
    private static final int FINGERPRINT_ACQUIRED_FINGER_EXISTED = 1105;
    private static final int FINGERPRINT_ACQUIRED_FINGER_DUPLICATE = 1005;
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENROLL_ERROR:
                    cancelEnrollment();
                    update(false);
                    break;
                case MSG_ENROLL_SUCCESS:
                    cancelEnrollment();
                    update(true);
                    break;
                case MSG_ENROLL_TIMEOUT:
                    cancelEnrollment();
                    update(false);
                    break;
            }
        }
    };

    private FingerprintManager.EnrollmentCallback mEnrollmentCallback = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            Log.d(TAG, "remaining=" + remaining + " getEnrollProcess=" + getEnrollProcess(remaining));
            Message msg = new Message();
            msg.what = MSG_ENROLL_SUCCESS;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            Log.d(TAG, "helpstring=" + helpString + "helpMsgId=" + helpMsgId);
            if (helpMsgId == FINGERPRINT_ACQUIRED_FINGER_DOWN){
                Log.d(TAG, "FINGERPRINT_ACQUIRED_FINGER_DOWN");
                mHandler.removeMessages(MSG_ENROLL_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(MSG_ENROLL_TIMEOUT, ENROLL_TIMEOUT_MS);
            } else if (helpMsgId == FINGERPRINT_ACQUIRED_FINGER_UP) {
                Log.d(TAG, "FINGERPRINT_ACQUIRED_FINGER_UP");
            } else if (helpMsgId == FINGERPRINT_ACQUIRED_FINGER_EXISTED || helpMsgId == FINGERPRINT_ACQUIRED_FINGER_DUPLICATE) {
                Message msg = new Message();
                msg.what = MSG_ENROLL_SUCCESS;
                mHandler.sendMessage(msg);
            }
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            Log.d(TAG, "errString=" + errString + "helpMsgId=" + errMsgId);
            Message msg = new Message();
            if (errMsgId != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                msg.what = MSG_ENROLL_ERROR;
                msg.obj = errString;
                mHandler.sendMessage(msg);
            }
        }

    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mmi_fp_test);
        setTitle(R.string.fp_sensor);
        mTextView = (TextView) findViewById(R.id.fp_test_guide);
        mResult = (TextView) findViewById(R.id.result);
        mImageView = (ImageView) findViewById(R.id.id_fp_image);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        mTest = (Button) findViewById(R.id.case_test);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mTest.setOnClickListener(mTestButtonListener);
        
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mToken = new byte[69];
        mFPManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        if (mFPManager != null) {
            mIsHardwareDetected = mFPManager.isHardwareDetected();
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override 
    protected void onResume() {
        super.onResume();
        if (mFPManager != null) {
            mFPManager.postEnroll();
        }
        mTest.setEnabled(true);
        mResult.setVisibility(View.INVISIBLE);
        mImageView.setVisibility(View.INVISIBLE);
        mTextView.setVisibility(View.INVISIBLE);
    }
    protected void onPause() {
        super.onPause();
        cancelEnrollment();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();  
        mFPManager.postEnroll();
    }

    private void startEnrollment() {
        long challenge = mFPManager.preEnroll();
        Log.d(TAG,"startEnrollment challenge = "+challenge);
        if (challenge > 0) {
            long temp = challenge;
            for (int i = 0; i < 8; i++) {
              mToken[i+1] = (byte)(temp & 0xff);
              temp = temp >> 8;  
            }
        }
        Log.d(TAG,"startEnrollment mToken = "+mToken);
        mTextView.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.INVISIBLE);
        mResult.setVisibility(View.INVISIBLE);
        mHandler.removeMessages(MSG_ENROLL_TIMEOUT);
        mHandler.sendEmptyMessageDelayed(MSG_ENROLL_TIMEOUT, ENROLL_TIMEOUT_MS);
        mEnrollmentCancel = new CancellationSignal();
        if (mUserId != UserHandle.USER_NULL) {
            mFPManager.setActiveUser(mUserId);
        }
        getSystemService(FingerprintManager.class).enroll(mToken, mEnrollmentCancel,
                0 /* flags */, mUserId, mEnrollmentCallback);
        mEnrolling = true;
    }
    
    private void cancelEnrollment() {
        mHandler.removeMessages(MSG_ENROLL_TIMEOUT);
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
        }
    }

    
    private int getEnrollProcess(int enrollmentRemaining) {
        int remainingProcess = enrollmentRemaining * 100 / 10;
        return (100 - remainingProcess);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                Intent intent = new Intent();
                ComponentName comp = new ComponentName(MMIFpTestFactory.this, MMIFpTestFactory.class);
                intent.setComponent(comp);
                startActivity(intent);
                finish();
            break;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mEditor.putInt(MMIFpTestFactory.this.getString(R.string.fp_sensor), Test_launcherActivity.PASS);
                mEditor.commit();
                finish();
            break;
        }
        return true;
    }

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIFpTestFactory.this.getString(R.string.fp_sensor), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
	
    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(MMIFpTestFactory.this.getString(R.string.fp_sensor), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };
	
    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(MMIFpTestFactory.this, MMIFpTestFactory.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
        }
    };

    private View.OnClickListener mTestButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (!mIsHardwareDetected) {
                Toast.makeText(MMIFpTestFactory.this,getResources().getString(R.string.have_no_hardware), Toast.LENGTH_SHORT).show();
            } else {
                if (!mEnrolling) {
                    startEnrollment();
                    mTest.setEnabled(false);
                }
            }
        }
    };
    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
    public void update(boolean result) {
        mTest.setEnabled(true);
        mResult.setVisibility(View.VISIBLE);
        mTextView.setVisibility(View.INVISIBLE);
        if (result) {
            mImageView.setVisibility(View.VISIBLE);
            mResult.setTextColor(0xFF00FF00);
            mResult.setText(R.string.pass);
        } else {
            mImageView.setVisibility(View.GONE);
            mResult.setTextColor(0xFFFF0000);
            mResult.setText(R.string.fail);
        }
    }

}
