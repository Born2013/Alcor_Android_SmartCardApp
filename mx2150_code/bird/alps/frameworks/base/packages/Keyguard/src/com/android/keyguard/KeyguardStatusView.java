/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextClock;
import android.widget.TextView;
//[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] BEGIN
import android.icu.util.Calendar;
//[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] END

import com.android.internal.widget.LockPatternUtils;

import java.util.Locale;
/*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.lang.reflect.Method;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemProperties;
/*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */

public class KeyguardStatusView extends GridLayout {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    /// M: add for mock testcase
    TextView mAlarmStatusView;
    TextClock mDateView;
    TextClock mClockView;
    TextView mOwnerInfo;
    //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] BEGIN
    TextView mAmPm;
    //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] END
    /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]begin */
    TextClock mMinutesView;
    /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]end */

    /// M: add for mock testcase
    KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            refresh();
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        mDateView = (TextClock) findViewById(R.id.date_view);
        mClockView = (TextClock) findViewById(R.id.clock_view);
        mDateView.setShowCurrentUserTime(true);
        mClockView.setShowCurrentUserTime(true);
        mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] BEGIN
        mAmPm = (TextView) findViewById(R.id.am_pm);
        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] END
        /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]begin */
        if (SystemProperties.get("ro.bd_customed_keyguard").equals("1")) {
            mMinutesView = (TextClock) findViewById(R.id.minute_view);
        }
        /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]end */

        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]begin*/
        if (!FeatureOption.BIRD_KUSAI_KEYGUARD) {
            mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
            mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        }
        /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]end*/
        mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] BEGIN
        mAmPm.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] END
    }

    public void refreshTime() {
        /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]begin*/
        if (FeatureOption.BIRD_KUSAI_KEYGUARD) {
            String dateFormat = getResources().getString(R.string.bird_kusai_dateformat);
            mDateView.setFormat24Hour(dateFormat);
            mDateView.setFormat12Hour(dateFormat);
        } else {
            mDateView.setFormat24Hour(Patterns.dateView);
            mDateView.setFormat12Hour(Patterns.dateView);
        }
        /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]end*/

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
        /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]begin */
        if (SystemProperties.get("ro.bd_customed_keyguard").equals("1")) {
            mClockView.setFormat12Hour("hh");
            mClockView.setFormat24Hour("HH");
            mMinutesView.setFormat12Hour("mm");
            mMinutesView.setFormat24Hour("mm");
        }
        /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]end */

        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] BEGIN
        if(android.text.format.DateFormat.is24HourFormat(mContext)) {
            mAmPm.setVisibility(View.GONE);
        } else {
            mAmPm.setVisibility(View.VISIBLE);
            Calendar c = Calendar.getInstance(); 
            if(c.get(Calendar.AM_PM) == 0) {
                mAmPm.setText(R.string.new_am);
            } else {
                mAmPm.setText(R.string.new_pm);
            }
        }
        //[BIRD][BUG #7885 锁屏界面时间没有上午、下午][chenguangxiang][20161229] END
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            mAlarmStatusView.setVisibility(View.VISIBLE);
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
        if (FeatureOptions.BIRD_SETTINGS_ADD_DATE_FORMAT) {
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.DATE_FORMAT), false, mContentObserver);
        }
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
        if (FeatureOptions.BIRD_SETTINGS_ADD_DATE_FORMAT) {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        }
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            
            
            /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
            String dateFormat = "";
            final String key;
            if (FeatureOptions.BIRD_SETTINGS_ADD_DATE_FORMAT) {
                dateFormat = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
                if (TextUtils.isEmpty(dateFormat)) {
                    dateFormat = getLocaleDateFormatString();
                }
                key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel + dateFormat;
            } else {
                key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            }
            
            
            if (key.equals(cacheKey)) return;

            /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
            if (FeatureOptions.BIRD_SETTINGS_ADD_DATE_FORMAT) {
                dateView = dateFormat;
            } else {
                dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
            }
            /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }
    
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
    private static String getLocaleDateFormatString() {
        Class<?> aClass = null;
        try {
            aClass = Class.forName("android.text.format.DateFormat");
            Method getDateFormatStringMethod = aClass.getDeclaredMethod("getDateFormatString");
            getDateFormatStringMethod.setAccessible(true);
            return (String) getDateFormatStringMethod.invoke(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "M/d/yy";
    }
    
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            refresh();
        }
    };
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
}
