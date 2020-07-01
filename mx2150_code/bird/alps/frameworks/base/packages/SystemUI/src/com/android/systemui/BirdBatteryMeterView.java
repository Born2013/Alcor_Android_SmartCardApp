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
package com.android.systemui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.tuner.TunerService;

//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
import com.android.systemui.FeatureOption;
import android.provider.Settings;
import android.database.ContentObserver;
//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.util.Log;

public class BirdBatteryMeterView extends ImageView implements
        BatteryController.BatteryStateChangeCallback, TunerService.Tunable {
        
    private BatteryController mBatteryController;
    LevelListDrawable mLevelListDrawable;
    AnimationDrawable mAnimationDrawable;

    public BirdBatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BirdBatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BirdBatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setImageResource(R.drawable.bird_stat_sys_battery);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController.addStateChangedCallback(this);
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
        if (FeatureOption.BIRD_POWER_MANAGER_V2) {
            getContext().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.BIRD_GENERAL_SAVING_STATUS),
                    false, mGeneralStatusChangeObserver);
        }
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBatteryController.removeStateChangedCallback(this);
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
        if (FeatureOption.BIRD_POWER_MANAGER_V2) {
            getContext().getContentResolver().unregisterContentObserver(mGeneralStatusChangeObserver);
        }
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        if (level >= 100) {
            setImageResource(R.drawable.bird_stat_sys_battery);
            setImageLevel(level);
        } else {
            if (charging) {
                setImageResource(R.drawable.bird_stat_sys_battery_charge);
                setImageLevel(level);
                if (level <= 99) {
                    mLevelListDrawable = (LevelListDrawable) getDrawable();
                    if(mLevelListDrawable.getCurrent() instanceof AnimationDrawable){
                        mAnimationDrawable = (AnimationDrawable) mLevelListDrawable.getCurrent();
                        mAnimationDrawable.start();
                    }
                }
            } else {
                setImageResource(R.drawable.bird_stat_sys_battery);
                setImageLevel(level);
            }
        }
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {

    }
    
    public void setBatteryController(BatteryController mBatteryController) {
        this.mBatteryController = mBatteryController;
    }
    
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    private ContentObserver mGeneralStatusChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (com.android.systemui.FeatureOption.BIRD_POWER_MANAGER_V2) {
                postInvalidate();
            }
        }
    };
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
}

