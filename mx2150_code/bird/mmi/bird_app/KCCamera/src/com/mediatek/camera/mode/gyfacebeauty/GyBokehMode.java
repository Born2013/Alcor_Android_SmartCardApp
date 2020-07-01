/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.mode.gyfacebeauty;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.Size;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import com.android.camera.R;

import com.mediatek.camera.AdditionManager;
import com.mediatek.camera.ICameraAddition;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.mode.CameraMode;
//import com.mediatek.camera.mode.facebeauty.FaceBeautyParametersHelper.ParameterListener;
import com.mediatek.camera.platform.ICameraAppUi.SpecViewType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.AutoFocusMvCallback;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.cFbOriginalCallback;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.IFocusManager;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.platform.IFileSaver.OnFileSavedListener;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;
import android.content.Context;
import android.app.Activity;
import junit.framework.Assert;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import com.android.camera.CameraActivity;
import com.mediatek.camera.mode.gyfacebeauty.GyBokehHelper;

import com.gangyun.camera.LibVGestureDetect;
import com.gangyun.camera.GYConfig;
//[BIRD][双摄引导只出现一次][yangheng][20170301] BEGIN
import android.content.SharedPreferences;
//[BIRD][双摄引导只出现一次][yangheng][20170301] END

//[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
import com.android.camera.FeatureSwitcher;
//[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN

public class GyBokehMode extends CameraMode implements ICameraAddition.Listener,
        IFocusManager.FocusListener {
    private static final String TAG = "GYLog GyBokehMode";
    
    protected static final int SUPPORTED_FB_PROPERTIES_MAX_NUMBER = 4;
    
    protected static final int ON_CAMERA_PARAMETERS_READY = 1;
    protected static final int INFO_FACE_DETECTED = 2;
    protected static final int ORIENTATION_CHANGED = 3;
    protected static final int ON_BACK_PRESSED = 4;
    protected static final int HIDE_EFFECTS_ITEM = 5;
    protected static final int ON_FULL_SCREEN_CHANGED = 6;
    protected static final int ON_CAMERA_CLOSED = 7;
    protected static final int ON_SETTING_BUTTON_CLICK = 8;
    protected static final int ON_LEAVE_FACE_BEAUTY_MODE = 9;
    private static final int ON_CONFIGURATION_CHANGED = 10;
    protected static final int ON_BOKEN_CHANGED = 11;
    
    private static final int CHECK_CAMERA_COVER = 6666;
    
    private CfbCallback mCfbCallback = new CfbCallback();
    private Handler mHandler;
    private GyPreviewSize mBokehPreviewSize;
   // private FaceBeautyParametersHelper mFaceBeautyParametersHelper;
  //  private ParameterListener mParameterListener;
    
    private ICameraView mICameraView;
    private ArrayList<Integer> mVfbFacesPoint = new ArrayList<Integer>();
    private AdditionManager mAdditionManager;
    
    private boolean mIsAutoFocusCallback = false;
    private CameraActivity mContext;
   	private GyBokehHelper mGyBokehHelper;

	private Parameters mParameters;
	private int monSingleTapUpX=0,monSingleTapUpY=0;
	private int gyPreviewW=0,gyPreviewH=0;
		
    public GyBokehMode(ICameraContext cameraContext) {
        super(cameraContext);
        Log.i(TAG, "[GyBeautyMode]constructor...");
        // first check the camera device
		
        mContext = (CameraActivity)cameraContext.getActivity();       
        mHandler = new MainHandler(mActivity.getMainLooper());
        mAdditionManager = cameraContext.getAdditionManager();
		mGyBokehHelper = GyBokehHelper.getInstance(mContext);
		setPreviewSizeRule();
		//[BIRD][双摄引导只出现一次][yangheng][20170301] BEGIN
		SharedPreferences sp = mContext.getSharedPreferences("default_show_dual_guide", Context.MODE_PRIVATE);
        boolean hasSet = sp.getBoolean("has_set", true);
        if (hasSet) {
            mGyBokehHelper.showGuide();
            sp.edit().putBoolean("has_set", false).commit();
        }
		//[BIRD][双摄引导只出现一次][yangheng][20170301] END
    }

    private android.widget.SeekBar mSeekBar;
    private android.widget.SeekBar mSeekBar2;
    private static int mSmoothValue = -1;
    private static int mWhiteningValue = 0;
    private int  mWhitening = 6;
    private boolean isInPopupSetting = false;

    
    @Override
    public boolean isNeedDualCamera() {
        Log.i(TAG, "isNeedDualCamera");
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
        if (FeatureSwitcher.BIRD_GANGYUN_NO_REAL) {
            return false;
        }
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] END
        return true;
    }
    
    @Override
    public boolean isRestartCamera() {
        return true;
    }
    
    @Override
    public void pause() {
        super.pause();
        Log.i(TAG, "[pause()] mICameraView = " + mICameraView);
        // Need hide the view when activity is onPause
        if (mICameraView != null) {
            mICameraView.hide();
        }
    }

    @Override
    public boolean close() {
        Log.i(TAG, "[closeMode]NextMode = " + mIModuleCtrl.getNextMode());
        if (mIModuleCtrl.getNextMode() != null) {
            // VFB photo mode to Video VR,not need set the face-beauty= false
            // 1:because video not know before mode is VFB;
            // 2:if set face-beauty= false,will found the video former parts not
            // have the face beauty effects
            if (mIFeatureConfig.isVfbEnable()) {
                if (CameraModeType.EXT_MODE_VIDEO == mIModuleCtrl.getNextMode()) {
                    //first need update the effects item if current effects is expanded
                   // mICameraView.update(HIDE_EFFECTS_ITEM);
                } else {
                  //  setVFBPs(false);
                }
            }
            
            if (mHandler != null) {
                mHandler.sendEmptyMessage(ON_LEAVE_FACE_BEAUTY_MODE);
            }
            
            // when close the mode ,need remove all the Msg when not execute
            removeAllMsg();
            // when leave out face beauty mode,need change the face view beauty
            // tag; because into this mode ,you have set true;
       //     changeFaceBeautyStatues(false);

				        
			 if(CameraModeType.EXT_MODE_PHOTO_PIP == mIModuleCtrl.getNextMode()){
				 mGyBokehHelper.gyBokehclose(false);
			 }
			 else{
				 mGyBokehHelper.gyBokehclose(true);
			 }

        }
		else{
		//	GyBokehHelper.gyBokehrelease();
		}
        mAdditionManager.close(true);
        return true;
    }

    @Override
    public boolean execute(ActionType type, Object... arg) {
        boolean returnValue = true;
        switch (type) {
        case ACTION_ON_CAMERA_OPEN:
	     Log.i(TAG, "[execute]set CameraModeType.ACTION_ON_CAMERA_OPEN");
            super.updateDevice();
            mAdditionManager.execute(type, true); 
            break;

        case ACTION_ON_CAMERA_CLOSE:				
 	         mGyBokehHelper.gyBokehclose(false);
             setModeState(ModeState.STATE_CLOSED);
            if (mIFeatureConfig.isVfbEnable()) {
                //mICameraView.update(ON_CAMERA_CLOSED);
            }		
	        Log.i(TAG, "[execute]set CameraModeType.ACTION_ON_CAMERA_CLOSE");
            break;
            
        case ACTION_ON_START_PREVIEW:
             Assert.assertTrue(arg.length == 1);
		Log.i(TAG, "[execute]set CameraModeType.ACTION_ON_START_PREVIEW");
		try {
			Size mgyPreviewSize  = mContext.getCameraDevice().getCamera().getParameters().getPreviewSize();
			if(gyPreviewW > 0 && gyPreviewH>0  && (gyPreviewW!= mgyPreviewSize.width || gyPreviewH != mgyPreviewSize.height)){
				startPreview_bokeh((Boolean) arg[0]);
				mHandler.sendEmptyMessage(ON_BOKEN_CHANGED);
				//mHandler.sendEmptyMessageDelayed(ON_BOKEN_CHANGED,50); 
			}
			else{
				startPreview((Boolean) arg[0]);
			}
			gyPreviewW = mgyPreviewSize.width;
			gyPreviewH = mgyPreviewSize.height;
		} catch (Exception e) {
		// TODO: handle exception
			Log.e(TAG, "Exception: =" + e.getMessage());
		}
            break;
            
        case ACTION_ON_CAMERA_PARAMETERS_READY:
            super.updateDevice();
            super.updateFocusManager();
            if (mIFocusManager != null) {
                mIFocusManager.setListener(this);
            }
             mHandler.sendEmptyMessage(ON_CAMERA_PARAMETERS_READY);
			mGyBokehHelper.updateParameters(mICameraDevice);
	        Log.i(TAG, "[execute]set CameraModeType.ACTION_ON_CAMERA_PARAMETERS_READY");			
            setModeState(ModeState.STATE_IDLE);
            
		
            // when user change the face beauty value to :Off need back to
            // PhotoMode

            // just Vfb need stopFD,because when native will get the preview
            // buffer itself will
            // get the FD Client,if we not stopFD,means the FD client is catched
            // by us;
            if (mIFeatureConfig.isVfbEnable()) {
                mIModuleCtrl.stopFaceDetection();
            }
           // mFaceBeautyParametersHelper.updateParameters(mICameraDevice);
           
            mAdditionManager.onCameraParameterReady(true);       
	     
			
            break;

        case ACTION_FACE_DETECTED:
            Face[] faces = (Face[]) arg;
            if (mIFeatureConfig.isVfbEnable() && faces != null && mICameraView != null) {
                mICameraView.update(INFO_FACE_DETECTED, faces.length);
                // update the face beauty point
              //  storeFaceBeautyLocation(faces);
                // PhotoActor/3rd party not set the face, because you have call
                // stopFD
            //    mIModuleCtrl.setFaces(faces);
            }
            break;

        case ACTION_ON_COMPENSATION_CHANGED:
            if (mICameraView != null) {
                mICameraView.update(ORIENTATION_CHANGED, mIModuleCtrl.getOrientationCompensation());
            }
	    
            break;

        case ACTION_ON_FULL_SCREEN_CHANGED:
	    
            if (mICameraView != null) {
                mICameraView.update(ON_FULL_SCREEN_CHANGED, (Boolean) arg[0]);
            }
            break;
            
        case ACTION_SHUTTER_BUTTON_FOCUS:
            //when focus is detected, not need to do AF
            break;
            
        case ACTION_PHOTO_SHUTTER_BUTTON_CLICK:
	     	    Log.i(TAG, "[execute]set CameraModeType.ACTION_PHOTO_SHUTTER_BUTTON_CLICK");	
				int size[] ={0,0,0,0,0,0,0,0,0,0,0};
	         	LibVGestureDetect.CurState(0,size);
   				mParameters =mContext.getCameraDevice().getCamera().getParameters();
				mParameters.set("gy-bokeh-enable",  1);
				mParameters.set("gy-bokeh-x",  size[2]);
				mParameters.set("gy-bokeh-y", size[3]);
				mParameters.set("gy-bokeh-radius",  size[4]);
				mParameters.set("gy-bokeh-level",  size[5]);
				mParameters.set("gy-bokeh-transition",  size[6]); 
				Log.i(TAG, "gycapture x:"+size[2] + " y:"+size[3]+" radius:"+size[4]+" level:"+size[5]+" trasition:"+size[6]);	
	            mContext.getCameraDevice().getCamera().setParameters(mParameters);
				
            //first need hide the effects item if the effects have expanded
            if (mICameraView != null) {
                mICameraView.update(HIDE_EFFECTS_ITEM);
            }
            if (mIFocusManager != null) {
                mIFocusManager.focusAndCapture();
            }
            break;

        case ACTION_SHUTTER_BUTTON_LONG_PRESS:
	        Log.i(TAG, "[execute]set CameraModeType.ACTION_SHUTTER_BUTTON_LONG_PRESS");				
            mICameraAppUi.showInfo(mActivity.getString(R.string.gy_accessibility_switch_to_bokeh)
                    + mActivity.getString(R.string.camera_continuous_not_supported));
            break;

        case ACTION_ON_SINGLE_TAP_UP:
            Assert.assertTrue(arg.length == 3);
            onSinlgeTapUp((View) arg[0], (Integer) arg[1], (Integer) arg[2]);
            break;

        case ACTION_ON_BACK_KEY_PRESS:
            // need callback ,if true means not need activity action on-back
            // pressed
            // when just supported Cfb, so need the supper onbackpressed
             Log.i(TAG, "ACTION_ON_BACK_KEY_PRESS");				
            GyBokehHelper.gyBokehrelease();
            if (mICameraView != null && !mICameraView.update(ON_BACK_PRESSED)
                    || !mIFeatureConfig.isVfbEnable()) {
                // returnValue is false means need super action the
                // onBackPressed
                returnValue = false;
            }
 	    returnValue = false;
            break;
        
        case ACTION_ON_SETTING_BUTTON_CLICK:
            // when user go to setting turn off the face beauty,before change
            // the setting we have hide the VFB UI, when the setting have
            // changed,this time not need show the UI
            //if (mICameraView != null && !isVfbOff()) {
             //   mICameraView.update(ON_SETTING_BUTTON_CLICK, arg[0]);
         //   }
         
            Log.i(TAG, "[execute]ACTION_ON_SETTING_BUTTON_CLICK =,arg[0] = " + arg[0]);
		   if((Boolean)arg[0])
                 stopFaceDetection();
            break;
        
        case ACTION_ON_CONFIGURATION_CHANGED:
            if (mHandler != null) {
                mHandler.sendEmptyMessage(ON_CONFIGURATION_CHANGED);
            }
            break;

         case ACTION_ON_PREVIEW_DISPLAY_SIZE_CHANGED:
		 Log.i(TAG,
				 "[execute] ActionType ACTION_ON_PREVIEW_DISPLAY_SIZE_CHANGED arg[0]"
						 + (Integer) arg[0] + ", arg[1]" + (Integer) arg[1]);
		   //mGyBokehHelper.gyPreviewSizeChange();
		    break;
            
        default:
            return false;
        }

        if (ActionType.ACTION_FACE_DETECTED != type) {
            Log.i(TAG, "[execute]type =" + type + ",returnValue = " + returnValue);
        }
        return returnValue;
    }

    @Override
    public boolean open() {
        mAdditionManager.setListener(this);
        mAdditionManager.open(true);
        super.open();
        mHandler.sendEmptyMessageDelayed(CHECK_CAMERA_COVER, 1000);
        return true;
    }

    //------> ICameraAddition.Listener
    @Override
    public boolean capture() {
        Log.i(TAG, "[capture]...");
        startCapture();
        return true;
    }

    @Override
    public boolean restartPreview(boolean needStop) {
      Log.i(TAG, "[restartPreview]...");
        return false;
    }
	
	@Override
    public void onFileSaveing() {
       // setModeState(ModeState.STATE_SAVING);
    }
    
	
    //<------ICameraAddition.Listener

    //------> IFocusManager.FocusListener
    @Override
    public void autoFocus() {
        Log.i(TAG, "[autoFocus]...");
        mICameraDevice.autoFocus(mAutoFocusCallback);
        mICameraAppUi.setViewState(ViewState.VIEW_STATE_FOCUSING);
        setModeState(ModeState.STATE_FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        Log.i(TAG, "[cancelAutoFocus]...");
        mICameraDevice.cancelAutoFocus();
        setFocusParameters();
        if (ModeState.STATE_CAPTURING != getModeState()) {
            mICameraAppUi.restoreViewState();
            setModeState(ModeState.STATE_IDLE);
        }
    }

    @Override
    public void startFaceDetection() {
        Log.i(TAG, "[startFaceDetection]...");
         mIModuleCtrl.startFaceDetection();
      //   mContext.getCameraDevice().startFaceDetection();
    }

    @Override
    public void stopFaceDetection() {
        Log.i(TAG, "[stopFaceDetection]...");
        mIModuleCtrl.stopFaceDetection();
    }

    @Override
    public void setFocusParameters() {
      //  Log.i(TAG, "[setFocusParameters]mIsAutoFocusCallback = " + mIsAutoFocusCallback);
        mIModuleCtrl.applyFocusParameters(!mIsAutoFocusCallback);
        mIsAutoFocusCallback = false;
    }

    @Override
    public void playSound(int soundId) {
        mCameraSound.play(soundId);
    }
    //<------IFocusManager.FocusListener
    
    private void removeAllMsg() {
        if (mHandler != null) {
            mHandler.removeMessages(ON_CAMERA_PARAMETERS_READY);
            mHandler.removeMessages(INFO_FACE_DETECTED);
            mHandler.removeMessages(ORIENTATION_CHANGED);
	        mHandler.removeMessages(ORIENTATION_CHANGED);
	        mHandler.removeMessages(ON_BOKEN_CHANGED);
	        /*[BIRD_PIP_DUAL_CAMERA_SUPPORT][chengshujiang][20161027]begin*/
            mHandler.removeMessages(CHECK_CAMERA_COVER);
            /*[BIRD_PIP_DUAL_CAMERA_SUPPORT][chengshujiang][20161027]end*/
        }
    }

    private class MainHandler extends Handler {

        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[handleMessage],msg = " + msg.what);

            switch (msg.what) {
            // this msg just used for VFB,so if you want use cFB,please be
            // careful
            case ON_CAMERA_PARAMETERS_READY:
                if (mICameraView != null) {
                    mICameraView.update(ON_CAMERA_PARAMETERS_READY);
                    mICameraView.show();
                }
                // have clear the face view
                // so you need initialize yourself
                mIModuleCtrl.initializeFrameView(false);
				
				if(mGyBokehHelper.isBokenOpen()){
				    Log.i(TAG, "[execute]set CameraModeType.ACTION_ON_CAMERA_PARAMETERS_READY isBokenOpen is true");	   
			     	mGyBokehHelper.gyBokehSetPreviewCallback(false);
				}                
                break;

            case ON_LEAVE_FACE_BEAUTY_MODE:
                if (mICameraView != null) {
                    mICameraView.update(ON_LEAVE_FACE_BEAUTY_MODE);
                }
                break;
                
            case ON_CONFIGURATION_CHANGED:
                // because configuration change,so need re-inflate the view
                // layout
                if (mICameraView != null) {
                    mICameraView.uninit();
                    mICameraView.init(mActivity, mICameraAppUi, mIModuleCtrl);
                }
                break;
                
                
            case ON_BOKEN_CHANGED:
                 mGyBokehHelper.gyBokehclose(false);   
                 mGyBokehHelper.gyBokehShow();               
                break;

            case CHECK_CAMERA_COVER: {
                mHandler.sendEmptyMessageDelayed(CHECK_CAMERA_COVER, 500);
                //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
                if (!FeatureSwitcher.BIRD_GANGYUN_NO_REAL) {
                    if (mGyBokehHelper != null) {
                        if(BirdDualCameraNative.readSubCameraState()) {
                            mGyBokehHelper.gyShowCoverTag();
                        } else {
                            mGyBokehHelper.gyHideCoverTag();
                        }
                    }
                }
                //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN 
                break;
            }

            default:
                break;
            }
        }
    }



    private void changeFaceBeautyStatues(boolean enable) {
        /**
         * true : means will set face beauty view to true, so the UI will show a
         * face beauty frame false: means will not show the full color face
         * frame,just is white color
         */
        Log.i(TAG, "[changeFaceBeautyStatues] enable = " + enable);
        mIModuleCtrl.setFaceBeautyEnalbe(enable);
    }

  

    private void setVFBPs(boolean isStart) {
        String value = isStart ? Util.VIDEO_FACE_BEAUTY_ENABLE : Util.VIDEO_FACE_BEAUTY_DISABLE;
        Log.i(TAG, "[setVFBPs] isStart = " + isStart + ",value = " + value);
        mICameraDevice.setParameter(Util.KEY_VIDEO_FACE_BEAUTY, value);
        // before set the ps,need update the ps
        mICameraDevice.applyParameters();
    }

    private void setPreviewSizeRule() {
		
        mBokehPreviewSize = new GyPreviewSize(mICameraContext);
        mISettingCtrl.addRule(SettingConstants.KEY_GYBOKEH_MODE, SettingConstants.KEY_PICTURE_RATIO,
                mBokehPreviewSize);
        mBokehPreviewSize.addLimitation("on", null, null);
    }

    private void onShutterButtonLongPress() {
        mICameraAppUi.showInfo(mActivity.getString(R.string.gy_accessibility_switch_to_bokeh)
                + mActivity.getString(R.string.camera_continuous_not_supported));
    }

    private void onSinlgeTapUp(View view, int x, int y) {
        Log.i(TAG, "[onSingleTapUp] mIFocusManager = " + mIFocusManager);
        if (ModeState.STATE_IDLE != getModeState()) {
            Log.i(TAG, "[onSingleTapUp] current state is = " + getModeState() + " ,so returen");
            return;
        }
        String focusMode = null;
        if (mIFocusManager != null) {
            focusMode = mIFocusManager.getFocusMode();
            Log.i(TAG, "[onSingleTapUp] current focusMode = " + focusMode);
        }
        if (mICameraDevice == null || focusMode == null
                || (Parameters.FOCUS_MODE_INFINITY.equals(focusMode))) {
            Log.i(TAG, "[onSinlgeTapUp]mICameraDevice = " + mICameraDevice + ",focusMode = "
                    + focusMode);
            return;
        }
		monSingleTapUpX = x;
		monSingleTapUpY = y;
		if(mContext.getCameraId() == 1){
		   mGyBokehHelper.showApertureView(monSingleTapUpX,monSingleTapUpY);
		}
        if (!mIFocusManager.getFocusAreaSupported()) {
            Log.i(TAG, "[onSinlgeTapUp]this project not supported Touch AF");
            return;
        }
        if (mICameraView != null) {
            mICameraView.update(HIDE_EFFECTS_ITEM);
        }
        mIFocusManager.onSingleTapUp(x, y);
		
        Log.i(TAG, "[onSingleTapUp] end getCameraId:"+mContext.getCameraId());
    }

    private boolean startCapture() {
        if ((ModeState.STATE_IDLE != getModeState()) || !isEnoughSpace()) {
            Log.i(TAG, "[startCapture],invalid state, return!");
            return false;
        }
        Log.i(TAG, "[startCapture]...");
        // need set FB original capture callback and here
        mICameraAppUi.setSwipeEnabled(false);
        mICameraAppUi.setViewState(ViewState.VIEW_STATE_CAPTURE);

        mICameraDevice.setcFBOrignalCallback(mCfbCallback);
        if (mIFeatureConfig.isVfbEnable()) {
            // because native not know the face beauty location when take
            // picture,
            // so need set the beauty location
           // setvFBFacePoints();
        }
        setModeState(ModeState.STATE_CAPTURING);
        mICameraDevice.takePicture(mShutterCallback, mRawPictureCallback, mPostViewPictureCallback,
                mJpegPictureCallback);

        return true;
    }

    private ShutterCallback mShutterCallback = new ShutterCallback() {

        @Override
        public void onShutter() {
            Log.d(TAG, "[mShutterCallback], time = " + System.currentTimeMillis());

        }
    };

    private PictureCallback mRawPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "[mRawPictureCallback], time = " + System.currentTimeMillis());

        }
    };

    private PictureCallback mPostViewPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "[mPostViewPictureCallback], time = " + System.currentTimeMillis());

        }
    };

    private PictureCallback mJpegPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "VFBCallback[mJpegPictureCallback], time = " + System.currentTimeMillis()
                    + ",data = " + data);
            
            if (ModeState.STATE_CLOSED == getModeState()) {
                Log.i(TAG, "[onPictureTaken] Camera is Closed");
                mICameraAppUi.restoreViewState();
                mICameraAppUi.setSwipeEnabled(true);
                return;
            }
            
            if (data != null) {
                // prepare the save request
                mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
                long time = System.currentTimeMillis();
                mIFileSaver.savePhotoFile(data, null, time, mIModuleCtrl.getLocation(), 0,
                        mFileSavedListener);
            }
            
            // Ensure focus indicator
            mIFocusManager.updateFocusUI();
            // Need Restart preview ,synchronize Normal photo
            // ZSD don't call stop preview when Capture done.
            boolean needRestartPreivew = !"on".equals(mISettingCtrl
                    .getSettingValue(SettingConstants.KEY_CAMERA_ZSD));
            startPreview(false);
            mIModuleCtrl.startFaceDetection();
            mICameraAppUi.setSwipeEnabled(true);
            mICameraAppUi.restoreViewState();
        }
    };

    private void storeFaceBeautyLocation(Face[] faces) {
        int index = 0;
        // First :clear last time stored values
        if (mVfbFacesPoint != null && mVfbFacesPoint.size() != 0) {
            mVfbFacesPoint.clear();
        }
        // Seconded: store the new values to the list
        if (faces != null) {
            for (int i = 0; i < faces.length; i++) {
                if (100 == faces[i].score) {
                    int x = faces[i].rect.left + (faces[i].rect.right - faces[i].rect.left) / 2;
                    int y = faces[i].rect.top + (faces[i].rect.bottom - faces[i].rect.top) / 2;
                    mVfbFacesPoint.add(index++, x);
                    mVfbFacesPoint.add(index, y);
                }
            }
        }
    }

    private void setvFBFacePoints() {
        if (mVfbFacesPoint == null) {
            Log.i(TAG, "[vFB] mVfbFacesPoint,current points is null,return");
            return;
        }
        String value = setFacePose();
        if (value != null) {
          //  mICameraDevice.setParameter(FaceBeautyParametersHelper.KEY_VIDED_FACE_BEAUTY_FACE,value);
          //  mICameraDevice.applyParameters();
        }
    }

    private String setFacePose() {
        String value = "";
        for (int i = 0; i < mVfbFacesPoint.size(); i++) {
            value += mVfbFacesPoint.get(i);
            // why need (i +1) != mvFBFacesPoint.size() ?
            // because at the end of value,not need any symbol
            // the value format is: xxx:yyy,x1:y1
            if ((i + 1) != mVfbFacesPoint.size()) {
                if (i % 2 != 0) {
                    value += ",";
                } else {
                    value += ":";
                }
            }
        }
        Log.i(TAG, "[vFB] setFacePose,vaue = " + value);
        return value;
    }

    private OnFileSavedListener mFileSavedListener = new OnFileSavedListener() {
        @Override
        public void onFileSaved(Uri uri) {
            Log.i(TAG, "[onFileSaved]uri = " + uri);
        }
    };

    // CFB
    public class CfbCallback implements cFbOriginalCallback {

        @Override
        public void onOriginalCallback(byte[] data) {
            Log.d(TAG, "cFBCallback,[onOriginalCallback],data.length = " + data.length);
            if (!mIFeatureConfig.isVfbEnable()) {
                mIFileSaver.init(FILE_TYPE.JPEG, 0, null, -1);
                long time = System.currentTimeMillis();
                mIFileSaver.savePhotoFile(data, null, time, mIModuleCtrl.getLocation(), 0,
                        mFileSavedListener);
            }
        }
    }
    


    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback() {
        
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            Log.i(TAG, "[onAutoFocus] success = " + success +",current state = " + getModeState());
            if (ModeState.STATE_CLOSED == getModeState()) {
                Log.i(TAG, "[onAutoFocus]camera is closed,so return");
                return;
            }
			if(getModeState() == ModeState.STATE_IDLE){
				mGyBokehHelper.showApertureView(monSingleTapUpX,monSingleTapUpY);
			}
            if (ModeState.STATE_FOCUSING == getModeState()) {
                mICameraAppUi.restoreViewState();
                setModeState(ModeState.STATE_IDLE);
            }
            mIFocusManager.onAutoFocus(success);
            mIsAutoFocusCallback = true;
        }
    };
    
    private final AutoFocusMvCallback mAutoFocusMvCallback = new AutoFocusMvCallback() {

        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
           Log.i(TAG, "[onAutoFocusMoving]moving = " + start);
            mIFocusManager.onAutoFocusMoving(start);
			if(!start && GYConfig.BOKEH_FOCUS_MOVING_SHOW_APRTURE){
				mGyBokehHelper.showApertureView(-1,-1);
			}
        }
        
    };
    
    private void startPreview(boolean needStop) {
        Log.i(TAG, "[startPreview]needStop = " + needStop);
        
        mIsAutoFocusCallback = false;
        
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mIFocusManager.resetTouchFocus();
            }
        });
        if (needStop) {
            stopPreview();
        }
		//mICameraDevice.applyParameters();
        mIFocusManager.setAeLock(false); // Unlock AE and AWB.
        mIFocusManager.setAwbLock(false);
		
        mIModuleCtrl.applyFocusParameters(false);
        Log.i(TAG, "set setFocusParameters normal");
        
        mICameraDevice.startPreview();
        mICameraAppUi.restoreViewState();
        mICameraDevice.setAutoFocusMoveCallback(mAutoFocusMvCallback);
        mIFocusManager.onPreviewStarted();
        setModeState(ModeState.STATE_IDLE);
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
        if (!FeatureSwitcher.BIRD_GANGYUN_NO_REAL) {
            mICameraDeviceManager.getCameraDevice(getTopCameraId()).startPreview();
        }
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] END
    }
    
    private int getTopCameraId() {
        return mICameraDeviceManager.getCurrentCameraId() == mICameraDeviceManager
                .getBackCameraId() ? mICameraDeviceManager.getFrontCameraId()
                : mICameraDeviceManager.getBackCameraId();
    }
    
     private void startPreview_bokeh(boolean needStop) {   
     		Size mgyPreviewSize = mContext.getCameraDevice().getCamera().getParameters().getPreviewSize();   
        mIsAutoFocusCallback = false;

        Log.i(TAG, "[startPreview1] b" );
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mIFocusManager.resetTouchFocus();
            }
        });
        if (needStop) {
          //  stopPreview();
        }
        mIFocusManager.setAeLock(false); // Unlock AE and AWB.
        mIFocusManager.setAwbLock(false);
        
        mIModuleCtrl.applyFocusParameters(false);
        Log.i(TAG, "set setFocusParameters normal");
        
       // mICameraDevice.startPreview();
        mICameraAppUi.restoreViewState();
        mICameraDevice.setAutoFocusMoveCallback(mAutoFocusMvCallback);
       // mIFocusManager.onPreviewStarted();
        setModeState(ModeState.STATE_IDLE);
    
	  gyPreviewW = mgyPreviewSize.width;
	   gyPreviewH = mgyPreviewSize.height;
	    //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
        if (!FeatureSwitcher.BIRD_GANGYUN_NO_REAL) {
            mICameraDeviceManager.getCameraDevice(getTopCameraId()).startPreview();
        }
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] END
    }
    private void stopPreview() {
        Log.i(TAG, "[stopPreview]mCurrentState = " + getModeState());
        if (ModeState.STATE_CLOSED == getModeState()) {
            Log.i(TAG, "[stopPreview]Preview is stopped.");
            return;
        }
	    mICameraDevice.cancelAutoFocus(); // Reset the focus.
	    mICameraDevice.stopPreview();
	    if (mIFocusManager != null) {
		    mActivity.runOnUiThread(new Runnable() {
		        public void run() {
			        mIFocusManager.onPreviewStopped();			
		        }
	        });
        }
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] BEGIN
        if (!FeatureSwitcher.BIRD_GANGYUN_NO_REAL) {
            mICameraDeviceManager.getCameraDevice(getTopCameraId()).stopPreview();
        }
        //[BIRD][BIRD_GANGYUN_NO_REAL][假双摄][yangheng][20170314] END
    }


}
