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

public class BatteryMeterView extends ImageView implements
        BatteryController.BatteryStateChangeCallback, TunerService.Tunable {

    private final BatteryMeterDrawable mDrawable;
    private final String mSlotBattery;
    private BatteryController mBatteryController;
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    private int mBatteryPaintColor;
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        final int frameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.batterymeter_frame_color));
        mDrawable = new BatteryMeterDrawable(context, new Handler(), frameColor);
        atts.recycle();

        mSlotBattery = context.getString(
                com.android.internal.R.string.status_bar_battery);
        setImageDrawable(mDrawable);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (StatusBarIconController.ICON_BLACKLIST.equals(key)) {
            ArraySet<String> icons = StatusBarIconController.getIconBlacklist(newValue);
			/*[BIRD][BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM][状态栏电量图标客制化][zhangaman][20170316]begin*/
            if (com.android.systemui.FeatureOption.BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM) {
                setVisibility(View.GONE);
            } else {
                setVisibility(icons.contains(mSlotBattery) ? View.GONE : View.VISIBLE);
            }
            /*[BIRD][BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM][状态栏电量图标客制化][zhangaman][20170316]end*/
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mBatteryController.addStateChangedCallback(this);
        mDrawable.startListening();
        TunerService.get(getContext()).addTunable(this, StatusBarIconController.ICON_BLACKLIST);
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
        mDrawable.stopListening();
        TunerService.get(getContext()).removeTunable(this);
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
        if (FeatureOption.BIRD_POWER_MANAGER_V2) {
            getContext().getContentResolver().unregisterContentObserver(mGeneralStatusChangeObserver);
        }
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        setContentDescription(
                getContext().getString(charging ? R.string.accessibility_battery_level_charging
                        : R.string.accessibility_battery_level, level));
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {

    }

    public void setBatteryController(BatteryController mBatteryController) {
        this.mBatteryController = mBatteryController;
        mDrawable.setBatteryController(mBatteryController);
    }

    public void setDarkIntensity(float f) {
        mDrawable.setDarkIntensity(f);
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

