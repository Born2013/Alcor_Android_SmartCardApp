/*
 * Copyright (C) 2006 The Android Open Source Project
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
/*[BIRD]xujing 20140829 added*/
package com.android.keyguard;

import java.util.Calendar;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews.RemoteView;


/**@hide*/
@RemoteView
public class BirdDigitalClock extends View {

    private final String TAG = "BirdDigitalClock";
    private Drawable mHourHigh;
    private Drawable mHourLow;
    private Drawable mMinuteHigh;
    private Drawable mMinuteLow;
    private Drawable mDivider;
    private String mAmPm;

    private boolean mAttached;
    private int mChildrenGap;
    private final Handler mHandler = new Handler();

    private Context mContext;
    private Paint mPaint;

    private Resources mResources;
    private int mDividerWidth;
    private int mNumberMinuteWidth;
    private int mNumberMinuteHeight;
    private int mNumberHourWidth;
    private int mNumberHourHeight;
    private int mWidth;
    private int mHeight;
    private String mDateFormat;
    private String mDate;
    private int mAmPmHeight;
    private int mAmPmWidth;
    private int mDateHeight;
    private int mDateMarginTop;
    private int mDateWidth;
    private TypedArray mTimeDrawableHourArray;
    private TypedArray mTimeDrawableMinuteArray;
    private int mDividerHeight;
    private int mTimeHeight;

    public BirdDigitalClock(Context context) {
        this(context, null);
    }

    public BirdDigitalClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BirdDigitalClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mResources = mContext.getResources();
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BirdDigitalClock, defStyle, 0);

        int res_id = a.getResourceId(
                R.styleable.BirdDigitalClock_time_hour_res, 0);
        mTimeDrawableHourArray = mResources.obtainTypedArray(res_id);

        res_id = a.getResourceId(R.styleable.BirdDigitalClock_time_minute_res,
                0);
        mTimeDrawableMinuteArray = mResources.obtainTypedArray(res_id);

        mDivider = mTimeDrawableMinuteArray
                .getDrawable(mTimeDrawableMinuteArray.length() - 1);

        mChildrenGap = (int) a.getDimension(
                R.styleable.BirdDigitalClock_children_gap, 0);

        mMinuteLow = mTimeDrawableMinuteArray.getDrawable(0);
        mHourLow = mTimeDrawableHourArray.getDrawable(0);

        int textColor = (int) a.getColor(
                R.styleable.BirdDigitalClock_text_color, 0xffffffff);

        mAmPmWidth = (int) a.getDimension(
                R.styleable.BirdDigitalClock_ampm_width, 0);

        mAmPmHeight = (int) a.getDimension(
                R.styleable.BirdDigitalClock_ampm_height, 0);

        mDateHeight = (int) a.getDimension(
                R.styleable.BirdDigitalClock_date_height, 0);

        mDateMarginTop = (int) a.getDimension(
                R.styleable.BirdDigitalClock_date_margin_top, 0);

        mDateWidth = (int) a.getDimension(
                R.styleable.BirdDigitalClock_date_width, 0);

        int id = a.getResourceId(R.styleable.BirdDigitalClock_date_format, 0);
        if (id != 0) {
            mDateFormat = mResources.getString(id);
        }

        mPaint = new Paint();
        mPaint.setColor(textColor);

        a.recycle();
        setMySize();

    }

    private void setMySize() {
        int ampm_width = mAmPmWidth;
        //~ if (DateFormat.is24HourFormat(mContext)) {
            //~ ampm_width = 0;
        //~ }
        mDividerWidth = mDivider.getIntrinsicWidth();
        mDividerHeight = mDivider.getIntrinsicHeight();
        mNumberMinuteWidth = mMinuteLow.getIntrinsicWidth();
        mNumberMinuteHeight = mMinuteLow.getIntrinsicHeight();
        mNumberHourWidth = mHourLow.getIntrinsicWidth();
        mNumberHourHeight = mHourLow.getIntrinsicHeight();
        mWidth = mNumberHourWidth * 2 + mNumberMinuteWidth * 2 + mDividerWidth
                + mChildrenGap * 4 + ampm_width;
        mTimeHeight = mNumberMinuteHeight;
        if (mNumberMinuteHeight < mNumberHourHeight) {
            mTimeHeight = mNumberHourHeight;
        }
        mHeight = mTimeHeight + mDateMarginTop + mDateHeight;
        Log.i(TAG, "setMySize-  mWidth" + mWidth + "; mHeight: " + mHeight);
        requestLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getContext().registerReceiver(mIntentReceiver, filter, null,
                    mHandler);
        }
        onTimeChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "onMeasure-  mWidth" + mWidth + "; mHeight: " + mHeight);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int left = 0;

   if (mAmPm != null && mAmPm.length() > 0) {
            mPaint.setTextSize(mAmPmHeight);
            canvas.drawText(mAmPm, left, mAmPmHeight - 5, mPaint);
            left = left + mAmPmWidth + mChildrenGap;
        }

        mHourHigh.setBounds(left, 0, left + mNumberHourWidth, mNumberHourHeight);
        mHourHigh.draw(canvas);

        left = left + mNumberHourWidth + mChildrenGap;
        mHourLow.setBounds(left, 0, left + mNumberHourWidth, mNumberHourHeight);
        mHourLow.draw(canvas);

        left = left + mNumberHourWidth + mChildrenGap;
        mDivider.setBounds(left, 0, left + mDividerWidth, mDividerHeight);
        mDivider.draw(canvas);

        left = left + mDividerWidth + mChildrenGap;
        mMinuteHigh.setBounds(left, 0, left + mNumberMinuteWidth, mNumberMinuteHeight);
        mMinuteHigh.draw(canvas);

        left = left + mNumberMinuteWidth + mChildrenGap;
        mMinuteLow.setBounds(left, 0, left + mNumberMinuteWidth, mNumberMinuteHeight);
        mMinuteLow.draw(canvas);

        if (mDate != null && mDate.length() > 0) {
            left = left + mNumberMinuteWidth;
            mPaint.setTextSize(mDateHeight);
            canvas.drawText(mDate, left - mDateWidth, mTimeHeight
                    + mDateMarginTop + mDateHeight - 5, mPaint);
        }
    }

    private void onTimeChanged() {
        long dateTaken = System.currentTimeMillis();
        if (mDateFormat != null &&  mDateFormat.length() > 0) {
            mDate = DateFormat.format(mDateFormat, dateTaken).toString();
        }
        final Calendar myCalendar = Calendar.getInstance(TimeZone.getDefault());
        myCalendar.setTimeInMillis(dateTaken);

        int my_Hour = myCalendar.get(Calendar.HOUR_OF_DAY);
        int my_Minute = myCalendar.get(Calendar.MINUTE);

        if (DateFormat.is24HourFormat(mContext)) {
            mAmPm = null;
            if (my_Hour == 24) {
                my_Hour = 0;
            }
        } else {
            mAmPm = DateUtils.getAMPMString(myCalendar.get(Calendar.AM_PM));
            Log.i(TAG, "mAmPm" + mAmPm);
            if (my_Hour == 0) {
                my_Hour = 12;
            } else if (my_Hour > 12) {
                my_Hour -= 12;
            }
        }

        mHourHigh = mTimeDrawableHourArray.getDrawable(my_Hour / 10);
        mHourLow = mTimeDrawableHourArray.getDrawable(my_Hour % 10);
        mMinuteHigh = mTimeDrawableMinuteArray.getDrawable(my_Minute / 10);
        mMinuteLow = mTimeDrawableMinuteArray.getDrawable(my_Minute % 10);
        invalidate();
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onTimeChanged();
        }
    };
}
