/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.systemui.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
import android.text.TextUtils;
import android.provider.Settings;
import java.lang.reflect.Method;
import com.android.systemui.FeatureOption;
import android.database.ContentObserver;
import android.os.Handler;
/*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */

public class DateView extends TextView {
    private static final String TAG = "DateView";

    private final Date mCurrentTime = new Date();

    private DateFormat mDateFormat;
    private String mLastText;
    private String mDatePattern;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                    || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                if (Intent.ACTION_LOCALE_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    // need to get a fresh date format
                    mDateFormat = null;
                }
                updateClock();
            }
        }
    };

    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DateView,
                0, 0);

        try {
            mDatePattern = a.getString(R.styleable.DateView_datePattern);
        } finally {
            a.recycle();
        }
        if (mDatePattern == null) {
            mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter, null, null);

        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
        if (FeatureOption.BIRD_SETTINGS_ADD_DATE_FORMAT) {
            getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.DATE_FORMAT), false, mContentObserver);
        }
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
        
        updateClock();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mDateFormat = null; // reload the locale next time
        getContext().unregisterReceiver(mIntentReceiver);
        
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
        if (FeatureOption.BIRD_SETTINGS_ADD_DATE_FORMAT) {
            getContext().getContentResolver().unregisterContentObserver(mContentObserver);
        }
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
    }

    protected void updateClock() {
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
        mCurrentTime.setTime(System.currentTimeMillis());
        final String text;
        if (FeatureOption.BIRD_SETTINGS_ADD_DATE_FORMAT) {
            String dateFormat = Settings.System.getString(getContext().getContentResolver(), Settings.System.DATE_FORMAT);
            if (TextUtils.isEmpty(dateFormat)) {
                dateFormat = getLocaleDateFormatString();
            }
            text = android.text.format.DateFormat.format(dateFormat, mCurrentTime).toString();
        } else {
            if (mDateFormat == null) {
                final Locale l = Locale.getDefault();
                DateFormat format = DateFormat.getInstanceForSkeleton(mDatePattern, l);
                format.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
                mDateFormat = format;
            }
            text = mDateFormat.format(mCurrentTime);
        }
        /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
        if (!text.equals(mLastText)) {
            setText(text);
            mLastText = text;
        }
    }
    
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
    private String getLocaleDateFormatString() {
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
            updateClock();
        }
    };
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
}
