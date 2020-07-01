package com.bird.smt_sw;

import android.R.color;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.hardware.Camera;
import android.view.Window;
import com.mediatek.camera.mode.gyfacebeauty.BirdDualCameraNative;

public class DeviceTestSubBackCameraCover extends MMINewActivity {

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private Camera mCamera;
    DeviceTestCameraPreview camera_preview;   
    private MainHandler mHandler;
    private TextView mTips;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	/*[BIRD][BIRD_FACTORY_CAM_FULL_DISPLAY][测试模式下相机测试全屏显示][luye][20160704]begin*/
    	if(FeatureOption.BIRD_FACTORY_CAM_FULL_DISPLAY) {
    	    requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
    	/*[BIRD][BIRD_FACTORY_CAM_FULL_DISPLAY][测试模式下相机测试全屏显示][luye][20160704]end*/    
        setContentView(R.layout.sub_back_camera_cover);
        setTitle(R.string.sub_back_camera_cover);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mTips = (TextView) findViewById(R.id.dual_tips);
        camera_preview = (DeviceTestCameraPreview )findViewById(R.id.camera_surface_view);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mHandler = new MainHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();  /*main camera*/
        camera_preview.setCamera(mCamera);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BirdDualCameraNative.openSmain();
        mHandler.sendEmptyMessageDelayed(1,1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            camera_preview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
        if (true) {
            BirdDualCameraNative.closeSmain();
        }
        mHandler.removeMessages(1);
    }

    @Override
    public void onAttachedToWindow() {
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);// FLAG_HOMEKEY_DISPATCHED
        super.onAttachedToWindow();
    }
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(DeviceTestSubBackCameraCover.this.getString(R.string.sub_back_camera_cover), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(DeviceTestSubBackCameraCover.this.getString(R.string.sub_back_camera_cover), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
    
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(DeviceTestSubBackCameraCover.this, DeviceTestSubBackCameraCover.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
		}
	};

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            mHandler.sendEmptyMessageDelayed(1,1000);
            if (BirdDualCameraNative.readSubBackCameraState()) {
                mTips.setText(R.string.camera_blocked);
            } else {
                mTips.setText(R.string.camera_blocked_no);
            }
        }
    }    
}

