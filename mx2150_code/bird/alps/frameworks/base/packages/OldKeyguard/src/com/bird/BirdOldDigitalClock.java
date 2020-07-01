/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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

package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Paint;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Calendar;
import android.view.View;
import android.util.Log;
import android.text.format.DateFormat;
import com.bird.calendarUtil.CalendarUtil;

/**
 * Displays the time
 */
public class BirdOldDigitalClock extends RelativeLayout {

    private static final String TAG= "BirdOldDigitalClock";
    private Context mContext;
    private Calendar mCalendar;
    private String mFormat;
    
    private ImageView mHour01;
    private ImageView mHour02;
    private ImageView mColon;
    private ImageView mMinute01;
    private ImageView mMinute02;
    private ImageView mWeekDay;

    private TextView mNongliText;
    
    private AmPm mAmPm;


    private ContentObserver mFormatChangeObserver;
    private boolean mLive = true;
    private boolean mAttached;
    
    private final static int[] TIME_NUM = {
            R.drawable.oldphone_clock_numbers_0,
            R.drawable.oldphone_clock_numbers_1, 
            R.drawable.oldphone_clock_numbers_2,
            R.drawable.oldphone_clock_numbers_3, 
            R.drawable.oldphone_clock_numbers_4,
            R.drawable.oldphone_clock_numbers_5, 
            R.drawable.oldphone_clock_numbers_6,
            R.drawable.oldphone_clock_numbers_7, 
            R.drawable.oldphone_clock_numbers_8,
            R.drawable.oldphone_clock_numbers_9
    };

    private final static int[] DATE_NUM = {
        
            R.drawable.oldphone_week_sunday_,
            R.drawable.oldphone_week_monday_ , 
            R.drawable.oldphone_week_tuesday_,
            R.drawable.oldphone_week_wednesday_, 
            R.drawable.oldphone_week_thursday_,
            R.drawable.oldphone_week_friday_, 
            R.drawable.oldphone_week_saturday_
            
    };

    
    private final Handler mHandler = new Handler();
    
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (mLive && Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {        
                    mCalendar = Calendar.getInstance();
                } 
                mHandler.post(new Runnable() {
                        public void run() {
                            updateTime();
                        }
                });
            }
        };

    private class FormatChangeObserver extends ContentObserver {
        public FormatChangeObserver() {
            super(new Handler());
        }
        @Override
        public void onChange(boolean selfChange) {
            setDateFormat();
            updateTime();
        }
    }

    public BirdOldDigitalClock(Context context) {
        this(context, null);
    }

    public BirdOldDigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAmPm = new AmPm(this);
        
        mHour01 = (ImageView) findViewById(R.id.zzzzz_default_clock_hour01);
        mHour02 = (ImageView) findViewById(R.id.zzzzz_default_clock_hour02);
        mColon = (ImageView) findViewById(R.id.zzzzz_default_clock_colon);
        mMinute01 = (ImageView) findViewById(R.id.zzzzz_default_clock_minute01);
        mMinute02 = (ImageView) findViewById(R.id.zzzzz_default_clock_minute02);
        
        mWeekDay = (ImageView)findViewById(R.id.zzzzz_default_week);
        mNongliText = (TextView)findViewById(R.id.date_nongli);
        
        mCalendar = Calendar.getInstance();

        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached) 
            return;
        mAttached = true;
        
        if (mLive) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter);
        }
        
        mFormatChangeObserver = new FormatChangeObserver();
        mContext.getContentResolver().registerContentObserver( Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached) 
            return;
        mAttached = false;
        if (mLive) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        mContext.getContentResolver().unregisterContentObserver(mFormatChangeObserver);
    }


    private void drawHour(Calendar calendar, boolean is24Format) {
        Log.d(TAG, " drawHour ");
        int i = calendar.get(Calendar.HOUR_OF_DAY);
        int j; 
        int k; 
        if (is24Format)  {
            j = i / 10;
            k = i % 10;
        } else { 
            if ((i == 0) || (i == 12)) {
                j = 1;
                k = 2;
            } else if ((i > 0) && (i < 12)) {
                j = i / 10;
                k = i % 10;
            } else {
                int l = i - 12;
                j = l / 10;
                k = l % 10;
            }
            Log.d(TAG, " drawHour  is24Format: " +is24Format);
        }
        Log.d(TAG, " drawHour 111");
        mHour01.setImageResource(TIME_NUM[j]);
        if (j == 0 && !is24Format) {
            mHour01.setVisibility(View.GONE);
        } else {
            mHour01.setVisibility(View.VISIBLE);
        }
        mHour02.setImageResource(TIME_NUM[k]);
    }

    private void drawMinute(Calendar calendar) {
        int i = calendar.get(Calendar.MINUTE);
        int j = i / 10;
        int k = i % 10;
        mMinute01.setImageResource(TIME_NUM[j]);
        mMinute02.setImageResource(TIME_NUM[k]);
    }

    private void drawWeek() {
        int i = mCalendar.get(Calendar.DAY_OF_WEEK)-1;
        Log.d(TAG, " drawWeek i ="+i );        
        mWeekDay.setImageResource(DATE_NUM[i]);
        String lunay =     getResources().getString(R.string.lunar,CalendarUtil.getLunarMonth(mCalendar), CalendarUtil.getLunarDay(mCalendar));
        Log.d(TAG, " drawWeek lunay ="+lunay );                
        mNongliText.setText(lunay);
        
    }
    
    void updateTime(Calendar c) {
        mCalendar = c;
        updateTime();

        
    }

    public void updateTime() {
        if (mLive) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }
        boolean is24Format = DateFormat.is24HourFormat(getContext());
        
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);
        drawHour(mCalendar, is24Format);
        mColon.setImageResource(R.drawable.oldphone_clock_numbers_colon);
        drawMinute(mCalendar);
        drawWeek();

    }

    private void setDateFormat() {
        boolean is24Format = DateFormat.is24HourFormat(getContext());
        String format = is24Format? "HH:mm":"hh:mm a";
        mAmPm.setShowAmPm(!is24Format);

        int a = -1;
        boolean quoted = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '\'') {
                quoted = !quoted;
            }
            if (!quoted && c == 'a') {
                a = i;
                break;
            }
        }
        if (a == 0) {
            format = format.substring(1);
        } else if (a > 0) {
            format = format.substring(0, a-1) + format.substring(a+1);
        }
        format = format.trim();
        mFormat = format;
    }

    

    void setLive(boolean live) {
        mLive = live;
    }

    static class AmPm {
        private ImageView mAmPm;
        private String mAmString, mPmString;
        
        AmPm(View parent) {
            mAmPm = (ImageView) parent.findViewById(R.id.zzzzz_default_am_pm);
        }
        
        void setShowAmPm(boolean show) {
            mAmPm.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        void setIsMorning(boolean isMorning) {
            mAmPm.setImageResource(isMorning ? R.drawable.oldphone_clock_am : R.drawable.oldphone_clock_pm);
        }
        
    }


}

