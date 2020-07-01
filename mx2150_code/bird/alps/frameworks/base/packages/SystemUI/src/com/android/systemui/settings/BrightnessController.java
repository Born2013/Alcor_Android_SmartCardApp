/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.systemui.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.ImageView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]begin
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import android.util.Log;
import android.view.View;
import com.android.systemui.statusbar.phone.FeatureOption;
//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]end
//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
import android.content.Intent;
//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END

public class BrightnessController implements ToggleSlider.Listener {
    private static final String TAG = "StatusBar.BrightnessController";
    private static final boolean SHOW_AUTOMATIC_ICON = false;

    /**
     * {@link android.provider.Settings.System#SCREEN_AUTO_BRIGHTNESS_ADJ} uses the range [-1, 1].
     * Using this factor, it is converted to [0, BRIGHTNESS_ADJ_RESOLUTION] for the SeekBar.
     */
    private static final float BRIGHTNESS_ADJ_RESOLUTION = 2048;

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;

    private final Context mContext;
    private final ImageView mIcon;
    private final ToggleSlider mControl;
    private final boolean mAutomaticAvailable;
    private final IPowerManager mPower;
    private final CurrentUserTracker mUserTracker;
    private final Handler mHandler;
    private final BrightnessObserver mBrightnessObserver;

    private ArrayList<BrightnessStateChangeCallback> mChangeCallbacks =
            new ArrayList<BrightnessStateChangeCallback>();

    private boolean mAutomatic;
    private boolean mListening;
    private boolean mExternalChange;
	//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]begin
	private CheckBox mAutoBrightnessToggle;
	//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]end

    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    final static String ACTION_GENERAL_SAVING_CLOSE = "com.bird.powermanager.ACTION_GENERAL_SAVING_CLOSE";
    private final static int GENERAL_SCREEN_BRIGHTNESS_VALUE = 100;//100
    private final static int GENERAL_SCREEN_BRIGHTNESS_MODE_VALUE = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    private final static int GENERAL_SCREEN_OFF_TIME_OUT_VALUE = 30000;//30s
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
    
    public interface BrightnessStateChangeCallback {
        public void onBrightnessLevelChanged();
    }

    /** ContentObserver to watch brightness **/
    private class BrightnessObserver extends ContentObserver {

        private final Uri BRIGHTNESS_MODE_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
        private final Uri BRIGHTNESS_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
        private final Uri BRIGHTNESS_ADJ_URI =
                Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) return;
            try {
                mExternalChange = true;
                if (BRIGHTNESS_MODE_URI.equals(uri)) {
                    updateMode();
                    updateSlider();
                } else if (BRIGHTNESS_URI.equals(uri) && !mAutomatic) {
                    updateSlider();
                } else if (BRIGHTNESS_ADJ_URI.equals(uri) && mAutomatic) {
                    updateSlider();
                } else {
                    updateMode();
                    updateSlider();
                }
                for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
                    cb.onBrightnessLevelChanged();
                }
                Log.i(TAG, "BIRD_POWER_MANAGER_V2, ACTION_GENERAL_SAVING_CLOSE ???");
                //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
                if (com.android.systemui.FeatureOption.BIRD_POWER_MANAGER_V2
                        && Settings.System.getInt(mContext.getContentResolver(), Settings.System.BIRD_GENERAL_SAVING_STATUS, 0) == 1
                        && Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 60000) != GENERAL_SCREEN_OFF_TIME_OUT_VALUE) {
    		        Intent intent = new Intent(ACTION_GENERAL_SAVING_CLOSE);
                    mContext.sendBroadcast(intent);
                    Log.i(TAG, "BIRD_POWER_MANAGER_V2, ACTION_GENERAL_SAVING_CLOSE");
                }
                //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
            } finally {
                mExternalChange = false;
            }
        }

        public void startObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(
                    BRIGHTNESS_MODE_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_URI,
                    false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(
                    BRIGHTNESS_ADJ_URI,
                    false, this, UserHandle.USER_ALL);
        }

        public void stopObserving() {
            final ContentResolver cr = mContext.getContentResolver();
            cr.unregisterContentObserver(this);
        }

    }

    public BrightnessController(Context context, ImageView icon, ToggleSlider control) {
        mContext = context;
        mIcon = icon;
        mControl = control;
        mHandler = new Handler();
        mUserTracker = new CurrentUserTracker(mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                updateMode();
                updateSlider();
            }
        };
        mBrightnessObserver = new BrightnessObserver(mHandler);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting() + 10;//[BIRD][TASK #5898][省电模式下将亮度调至最低，屏幕基本接近灭屏状态][chenguangxiang]
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
		//[BIRD][BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS][自动调节亮度可配置显示/隐藏][dingjiayuan][20160520]begin
        mAutomaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available)
				 && !FeatureOption.BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS;
		//[BIRD][BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS][自动调节亮度可配置显示/隐藏][dingjiayuan][20160520]end
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
    }

    public void addStateChangedCallback(BrightnessStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public boolean removeStateChangedCallback(BrightnessStateChangeCallback cb) {
        return mChangeCallbacks.remove(cb);
    }

    @Override
    public void onInit(ToggleSlider control) {
        // Do nothing
    }

    public void registerCallbacks() {
        if (mListening) {
            return;
        }

        mBrightnessObserver.startObserving();
        mUserTracker.startTracking();

        // Update the slider and mode before attaching the listener so we don't
        // receive the onChanged notifications for the initial values.
        updateMode();
        updateSlider();

        mControl.setOnChangedListener(this);
        mListening = true;
    }

    /** Unregister all call backs, both to and from the controller */
    public void unregisterCallbacks() {
        if (!mListening) {
            return;
        }

        mBrightnessObserver.stopObserving();
        mUserTracker.stopTracking();
        mControl.setOnChangedListener(null);
        mListening = false;
    }

    @Override
    public void onChanged(ToggleSlider view, boolean tracking, boolean automatic, int value,
            boolean stopTracking) {
        updateIcon(mAutomatic);
        if (mExternalChange) return;

        if (!mAutomatic) {
            final int val = value + mMinimumBacklight;
            if (stopTracking) {
                MetricsLogger.action(mContext, MetricsEvent.ACTION_BRIGHTNESS, val);
            }
            setBrightness(val);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, val,
                                    UserHandle.USER_CURRENT);
                        }
                    });
            }
        } else {
            final float adj = value / (BRIGHTNESS_ADJ_RESOLUTION / 2f) - 1;
            if (stopTracking) {
                MetricsLogger.action(mContext, MetricsEvent.ACTION_BRIGHTNESS_AUTO, value);
            }
            setBrightnessAdj(adj);
            if (!tracking) {
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        Settings.System.putFloatForUser(mContext.getContentResolver(),
                                Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj,
                                UserHandle.USER_CURRENT);
                    }
                });
            }
        }

        for (BrightnessStateChangeCallback cb : mChangeCallbacks) {
            cb.onBrightnessLevelChanged();
        }
    }

    private void setMode(int mode) {
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode,
                mUserTracker.getCurrentUserId());
    }

    private void setBrightness(int brightness) {
        try {
            mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
        } catch (RemoteException ex) {
        }
    }

    private void setBrightnessAdj(float adj) {
        try {
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj);
        } catch (RemoteException ex) {
        }
    }
	//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]begin
	public void onStartTrackingTouch(){
		setMode(SCREEN_BRIGHTNESS_MODE_MANUAL);
		updateMode();
		updateSlider();		
	}
	
	public void setAutoBrightnessIcon(CheckBox toggle){
		mAutoBrightnessToggle = toggle;
		mAutoBrightnessToggle.setOnCheckedChangeListener(mCheckListener);
		if(mAutomaticAvailable){
			mAutoBrightnessToggle.setVisibility(View.VISIBLE);
		}
	}

    private final OnCheckedChangeListener mCheckListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton toggle, boolean checked) {
			Log.i("adadfadafd","onCheckedChanged checked = "+checked);
			Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE,
                    checked ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : SCREEN_BRIGHTNESS_MODE_MANUAL);
        }
    };
	//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]end
    private void updateIcon(boolean automatic) {
        if (mIcon != null) {
            mIcon.setImageResource(automatic && SHOW_AUTOMATIC_ICON ?
                    com.android.systemui.R.drawable.ic_qs_brightness_auto_on :
                    com.android.systemui.R.drawable.ic_qs_brightness_auto_off);
        }
		//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]begin
		Log.i("adadfadafd","updateIcon automatic = "+automatic+" mAutoBrightnessToggle = "+mAutoBrightnessToggle);
		if(mAutoBrightnessToggle != null) {
			mAutoBrightnessToggle.setChecked(automatic);
		}
		//[BIRD][BIRD_ADD_AUTO_BRIGHTNESS_TOOGLE][状态栏中增加自动调节亮度按钮][dingjiayuan][20160519]end
    }

    /** Fetch the brightness mode from the system settings and update the icon */
    private void updateMode() {
        if (mAutomaticAvailable) {
            int automatic;
            automatic = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    UserHandle.USER_CURRENT);
            mAutomatic = automatic != Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
            updateIcon(mAutomatic);
        } else {
            mControl.setChecked(false);
            updateIcon(false /*automatic*/);
        }
    }

    /** Fetch the brightness from the system settings and update the slider */
    private void updateSlider() {
        if (mAutomatic) {
            float value = Settings.System.getFloatForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 0,
                    UserHandle.USER_CURRENT);
            mControl.setMax((int) BRIGHTNESS_ADJ_RESOLUTION);
            mControl.setValue((int) ((value + 1) * BRIGHTNESS_ADJ_RESOLUTION / 2f));
        } else {
            int value;
            value = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                    UserHandle.USER_CURRENT);
            mControl.setMax(mMaximumBacklight - mMinimumBacklight);
            mControl.setValue(value - mMinimumBacklight);
        }
    }

}
