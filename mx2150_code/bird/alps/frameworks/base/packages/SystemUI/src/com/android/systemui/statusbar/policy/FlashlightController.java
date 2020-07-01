/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]begin
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.util.Size;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]end
//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]BEGIN
import com.bird.flashlight.FlashLightJni;
import com.android.systemui.FeatureOption;
//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]END
/**
 * Manages the flashlight.
 */
public class FlashlightController {

    private static final String TAG = "FlashlightController";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int DISPATCH_ERROR = 0;
    private static final int DISPATCH_CHANGED = 1;
    private static final int DISPATCH_AVAILABILITY_CHANGED = 2;
	//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]begin
	private static final int DISPATCH_OFF = 3;
    private static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";
	//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]end
    private final CameraManager mCameraManager;
    /** Call {@link #ensureHandler()} before using */
    private Handler mHandler;

    /** Lock on mListeners when accessing */
    private final ArrayList<WeakReference<FlashlightListener>> mListeners = new ArrayList<>(1);

    /** Lock on {@code this} when accessing */
    private boolean mFlashlightEnabled;

    private final String mCameraId;
    private boolean mTorchAvailable;
	//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]begin
	private boolean mCameraAvailable;
    private CameraDevice mCameraDevice;
    private CaptureRequest mFlashlightRequest;
    private CameraCaptureSession mSession;
	private SurfaceTexture mSurfaceTexture;
	private Surface mSurface;
	//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]end
    public FlashlightController(Context mContext) {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        String cameraId = null;
        try {
            cameraId = getCameraId();
        } catch (Throwable e) {
            //[BIRD][BUG 下拉通知栏，手电筒修改][chenguangxiang][20170209] BEGIN
            Log.e(TAG, "Couldn't initialize.", e);
            cameraId = "0";
            //return;
            //[BIRD][BUG 下拉通知栏，手电筒修改][chenguangxiang][20170209] END
        } finally {
            mCameraId = cameraId;
        }

        if (mCameraId != null) {
            ensureHandler();
            mCameraManager.registerTorchCallback(mTorchCallback, mHandler);
        }
		//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]begin
		// M: Turn off the flashlight when shutdown.
        initializeReceiver(mContext);
		//[BIRD][状态栏中手电筒不能使用][pangmeizhou][20160316]end
    }

    public void setFlashlight(boolean enabled) {
		//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][pangmeizhou][20160414]BEGIN
		if(FeatureOption.BIRD_IUI_FLASHLIGHT_SUPPORT) {
		    //[BIRD][BUG #7994 后置摄像头，相机预览界面下拉通知栏，手电筒修改][chenguangxiang][20161226] BEGIN
		    if(isAvailable()) {
                boolean isTurnOn = FlashLightJni.get_state_flashlight();
                if (!isTurnOn) {
	                FlashLightJni.turn_on_flashlight();
				    mFlashlightEnabled = true;
                } else {
	                FlashLightJni.turn_off_flashlight();
				    mFlashlightEnabled = false;
                }
            }
            //[BIRD][BUG #7994 后置摄像头，相机预览界面下拉通知栏，手电筒修改][chenguangxiang][20161226] END
		} else {
	        boolean pendingError = false;
	        synchronized (this) {
	            if (mFlashlightEnabled != enabled) {
	                mFlashlightEnabled = enabled;
	                try {
	                    mCameraManager.setTorchMode(mCameraId, enabled);
	                } catch (CameraAccessException e) {
	                    Log.e(TAG, "Couldn't set torch mode", e);
	                    mFlashlightEnabled = false;
	                    pendingError = true;
	                }
	            }
	        }
	        dispatchModeChanged(mFlashlightEnabled);
	        if (pendingError) {
	            dispatchError();
	        }
		}
		//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][pangmeizhou][20160414]END
    }


    public void killFlashlight() {
        Log.i(TAG, "[killFlashlight]mFlashlightEnabled=" + mFlashlightEnabled);
        boolean enabled;
        synchronized (this) {
            enabled = mFlashlightEnabled;
        }
        if (enabled) {
            mHandler.post(mKillFlashlightRunnable);
        }
    }
	
    public boolean hasFlashlight() {
        return true;//mCameraId != null;
    }
	
    public synchronized boolean isEnabled() {
		//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]BEGIN
		if(FeatureOption.BIRD_IUI_FLASHLIGHT_SUPPORT) {
			boolean isFlashlightOn = FlashLightJni.get_state_flashlight();
			return isFlashlightOn;
		} else {
        	return mFlashlightEnabled;
		}
		//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]END
    }

    public synchronized boolean isAvailable() {
        return mTorchAvailable;
    }

    public void addListener(FlashlightListener l) {
        synchronized (mListeners) {
            cleanUpListenersLocked(l);
            mListeners.add(new WeakReference<>(l));
        }
    }

    public void removeListener(FlashlightListener l) {
        synchronized (mListeners) {
            cleanUpListenersLocked(l);
        }
    }

    private synchronized void ensureHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mHandler = new Handler(thread.getLooper());
        }
    }

    private void startDevice() throws CameraAccessException {
        mCameraManager.openCamera(getCameraId(), mCameraListener, mHandler);
    }

    private void startSession() throws CameraAccessException {
        mSurfaceTexture = new SurfaceTexture(false);
        Size size = getSmallestSize(mCameraDevice.getId());
        mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        mSurface = new Surface(mSurfaceTexture);
        ArrayList<Surface> outputs = new ArrayList<>(1);
        outputs.add(mSurface);
        mCameraDevice.createCaptureSession(outputs, mSessionListener, mHandler);
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = mCameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    private void postUpdateFlashlight() {
        ensureHandler();
        mHandler.post(mUpdateFlashlightRunnable);
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable
                    && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return id;
            }
        }
        return null;
    }

    private void dispatchModeChanged(boolean enabled) {
        dispatchListeners(DISPATCH_CHANGED, enabled);
    }
	
    private void updateFlashlight(boolean forceDisable) {
        try {
            boolean enabled;
            synchronized (this) {
                enabled = mFlashlightEnabled && !forceDisable;
            }
            if (enabled) {
                if (mCameraDevice == null) {
                    startDevice();
                    return;
                }
                if (mSession == null) {
                    startSession();
                    return;
                }
                /// M: [ALPS01753229]JE @{
                if (mSurface == null) {
                    Log.e(TAG, "Error in updateFlashlight: mSurface is null");
                    handleError();
                    return;
                }
                /// M: [ALPS01753229]JE @}
                if (mFlashlightRequest == null) {
                    CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    builder.addTarget(mSurface);
                    CaptureRequest request = builder.build();
                    mSession.capture(request, null, mHandler);
                    mFlashlightRequest = request;
                }
            } else {
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    teardown();
                }
            }

        } catch (CameraAccessException|IllegalStateException|UnsupportedOperationException e) {
            Log.e(TAG, "Error in updateFlashlight", e);
            handleError();
        }
    }

    private void teardown() {
        /// M: [ALPS01753229]JE @{
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        if (mSession != null) {
            mSession.close();
        }
        /// M: [ALPS01753229]JE @}
        mCameraDevice = null;
        mSession = null;
        mFlashlightRequest = null;
        if (mSurface != null) {
            mSurface.release();
            mSurfaceTexture.release();
        }
        mSurface = null;
        mSurfaceTexture = null;
    }

    private void handleError() {
        synchronized (this) {
            mFlashlightEnabled = false;
        }
        dispatchError();
        dispatchOff();
        updateFlashlight(true /* forceDisable */);
    }

    private void dispatchOff() {
        dispatchListeners(DISPATCH_OFF, false /* argument (ignored) */);
    }

    private void dispatchError() {
        dispatchListeners(DISPATCH_CHANGED, false /* argument (ignored) */);
    }

    private void dispatchAvailabilityChanged(boolean available) {
        dispatchListeners(DISPATCH_AVAILABILITY_CHANGED, available);
    }

    private void dispatchListeners(int message, boolean argument) {
        synchronized (mListeners) {
            final int N = mListeners.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                FlashlightListener l = mListeners.get(i).get();
                if (l != null) {
                    if (message == DISPATCH_ERROR) {
                        l.onFlashlightError();
                    } else if (message == DISPATCH_CHANGED) {
                        l.onFlashlightChanged(argument);
                    } else if (message == DISPATCH_AVAILABILITY_CHANGED) {
                        l.onFlashlightAvailabilityChanged(argument);
                    } else if (message == DISPATCH_OFF) {
                        l.onFlashlightOff();
                    }
                } else {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(FlashlightListener listener) {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            FlashlightListener found = mListeners.get(i).get();
            if (found == null || found == listener) {
                mListeners.remove(i);
            }
        }
    }

    private final CameraDevice.StateListener mCameraListener = new CameraDevice.StateListener() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            postUpdateFlashlight();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            if (mCameraDevice == camera) {
                dispatchOff();
                teardown();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: camera=" + camera + " error=" + error);
            if (camera == mCameraDevice || mCameraDevice == null) {
                handleError();
            }
        }
    };

    private final CameraCaptureSession.StateListener mSessionListener =
            new CameraCaptureSession.StateListener() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (session.getDevice() == mCameraDevice) {
                mSession = session;
            } else {
                session.close();
            }
            postUpdateFlashlight();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "Configure failed.");
            if (mSession == null || mSession == session) {
                handleError();
            }
        }
    };

    private final Runnable mUpdateFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            updateFlashlight(false /* forceDisable */);
        }
    };

    private final Runnable mKillFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                mFlashlightEnabled = false;
            }
            updateFlashlight(true /* forceDisable */);
            dispatchOff();
        }
    };


    private final CameraManager.AvailabilityCallback mAvailabilityCallback =
            new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            if (DEBUG) Log.d(TAG, "onCameraAvailable(" + cameraId + ")");
            if (cameraId.equals(mCameraId)) {
                setCameraAvailable(true);
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            if (DEBUG) Log.d(TAG, "onCameraUnavailable(" + cameraId + ")");
            if (cameraId.equals(mCameraId)) {
                setCameraAvailable(false);
            }
        }

        private void setCameraAvailable(boolean available) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = mCameraAvailable != available;
                mCameraAvailable = available;
            }
            if (changed) {
                if (DEBUG) Log.d(TAG, "dispatchAvailabilityChanged(" + available + ")");
                dispatchAvailabilityChanged(available);
            }
        }
    };
	
    private final CameraManager.TorchCallback mTorchCallback =
            new CameraManager.TorchCallback() {

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (TextUtils.equals(cameraId, mCameraId)) {
                setCameraAvailable(false);
            }
        }

        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (TextUtils.equals(cameraId, mCameraId)) {
                setCameraAvailable(true);
				//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]BEGIN
				if(!FeatureOption.BIRD_IUI_FLASHLIGHT_SUPPORT) {
                	setTorchMode(enabled);
				}
				//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]END
            }
        }

        private void setCameraAvailable(boolean available) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = mTorchAvailable != available;
                mTorchAvailable = available;
            }
            if (changed) {
                if (DEBUG) Log.d(TAG, "dispatchAvailabilityChanged(" + available + ")");
				//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]BEGIN
				if(!FeatureOption.BIRD_IUI_FLASHLIGHT_SUPPORT) {
                	dispatchAvailabilityChanged(available);
				}
				//[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]END
            }
        }

        private void setTorchMode(boolean enabled) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = mFlashlightEnabled != enabled;
                mFlashlightEnabled = enabled;
            }
            if (changed) {
                if (DEBUG) Log.d(TAG, "dispatchModeChanged(" + enabled + ")");
                dispatchModeChanged(enabled);
            }
        }
    };

    public interface FlashlightListener {

        /**
         * Called when the flashlight was turned off or on.
         * @param enabled true if the flashlight is currently turned on.
         */
        void onFlashlightChanged(boolean enabled);


        /**
         * Called when there is an error that turns the flashlight off.
         */
        void onFlashlightError();

        /**
         * Called when there is a change in availability of the flashlight functionality
         * @param available true if the flashlight is currently available.
         */
        void onFlashlightAvailabilityChanged(boolean available);

        /**
         * Called when the flashlight turns off unexpectedly.
         */
        void onFlashlightOff();
    }
	
    // M: Turn off the flashlight when shutdown.
    private void initializeReceiver(Context context) {
        Log.i(TAG, "[initializeReceiver]");
        if (mCameraId == null) {
            return;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_SHUTDOWN);
        filter.addAction(ACTION_SHUTDOWN_IPO);
        context.registerReceiver(mShutdownReceiver, filter);
    }

    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "[onReceive]intent=" + intent);
            killFlashlight();
        }
    };
}
