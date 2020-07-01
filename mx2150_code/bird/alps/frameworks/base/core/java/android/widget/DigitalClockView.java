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
     
package android.widget;    

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews.RemoteView;

import java.util.TimeZone;
import java.util.Calendar;


/**
 * This widget display an analogic clock with two hands for hours and
 * minutes.
 *
 * @attr ref android.R.styleable#AnalogClock_dial
 * @attr ref android.R.styleable#AnalogClock_hand_hour
 * @attr ref android.R.styleable#AnalogClock_hand_minute
 */
/**@hide*/
@RemoteView
public class DigitalClockView extends View {
    private Time mCalendar;

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mDial;

    private Drawable mHourHigh;
    private Drawable mHourLow;
    private Drawable mMinuteHigh;
    private Drawable mMinuteLow;
    private Drawable mDivider;

    private TypedArray mTimeDrawableArray;

    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final Handler mHandler = new Handler();
    private float mMinutes;
    private float mHour;
    private boolean mChanged;

    public DigitalClockView(Context context) {
        this(context, null);
    }

    public DigitalClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DigitalClockView(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        Resources r = mContext.getResources();
        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, com.android.internal.R.styleable.DigitalClockView, defStyle, 0);

        //int res_id = a.getResourceId(com.android.internal.R.styleable.DigitalClockView_time_res, 0);
        mTimeDrawableArray  = r.obtainTypedArray(com.android.internal.R.array.clock_widget_drawables);
        int size = mTimeDrawableArray.length();
        mHourHigh = mTimeDrawableArray.getDrawable(0);
        mHourLow = mTimeDrawableArray.getDrawable(1);
        mMinuteHigh = mTimeDrawableArray.getDrawable(2);
        mMinuteLow = mTimeDrawableArray.getDrawable(3);
        mDivider = mTimeDrawableArray.getDrawable(size - 1);

        /*mDial = a.getDrawable(com.android.internal.R.styleable.AnalogClock_dial);
        if (mDial == null) {
            mDial = r.getDrawable(com.android.internal.R.drawable.clock_dial);
        }

        mHourHand = a.getDrawable(com.android.internal.R.styleable.AnalogClock_hand_hour);
        if (mHourHand == null) {
            mHourHand = r.getDrawable(com.android.internal.R.drawable.clock_hand_hour);
        }

        mMinuteHand = a.getDrawable(com.android.internal.R.styleable.AnalogClock_hand_minute);
        if (mMinuteHand == null) {
            mMinuteHand = r.getDrawable(com.android.internal.R.drawable.clock_hand_minute);
        }

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();*/
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
            filter.addAction(Intent.ACTION_SCREEN_ON);//[110527]xujing 20161019 modify

            getContext().registerReceiver(mIntentReceiver, filter, null, mHandler);
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
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

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        int availableWidth = mRight - mLeft;
        int availableHeight = mBottom - mTop;

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        final Drawable dial = mDial;
        int w = 0;//dial.getIntrinsicWidth();
        int h = 0;//dial.getIntrinsicHeight();
        //HourHigh
        final Drawable hourHigh = mHourHigh;
        int mHourHighWidth = hourHigh.getIntrinsicWidth();
        int mHourHighHeight = hourHigh.getIntrinsicHeight();
        
        //HourLow
        final Drawable hourLow = mHourLow;
        int mHourLowWidth = hourLow.getIntrinsicWidth();
        int mHourLowHeight  = hourLow.getIntrinsicHeight();

        //dot
        final Drawable divider = mDivider;
        int mDividerWidth = divider.getIntrinsicWidth();


        //MinuteHigh
        final Drawable minuteHigh = mMinuteHigh;
        int mMinuteHighWidth = minuteHigh.getIntrinsicWidth();
        int mMinuteHighHeight = minuteHigh.getIntrinsicHeight();

        //MinuteLow
        final Drawable minuteLow = mMinuteLow;
        int mMinuteLowWidth = minuteLow.getIntrinsicWidth();
        int mMinuteLowHeight = minuteLow.getIntrinsicHeight();

        //because all drawables's height is same 
        int height = mMinuteLowHeight;
        boolean scaled = false;

        if (changed) {
            //dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        canvas.save();
        //MinuteLow
        if(changed){
            w = minuteLow.getIntrinsicWidth();
            h = minuteLow.getIntrinsicHeight();
             
            minuteLow.setBounds(availableWidth - w, 0, 
                    availableWidth , height);
        }
        Log.v("hubing ","hubing mRight-->"+mRight+"w-->"+w+"availWidth-->"+availableWidth);
        minuteLow.draw(canvas);
        canvas.restore();

        canvas.save();
        
        //MinuteHigh
        if(changed){
            w = minuteHigh.getIntrinsicWidth();
            h = minuteHigh.getIntrinsicHeight();
            
            minuteHigh.setBounds(availableWidth - w - mMinuteLowWidth, 0, availableWidth - mMinuteLowWidth ,height);
            Log.v("hubing","hubing mRight ->"+mRight+"-w->"+w+"mMinuteLowWidth-->"+mMinuteLowWidth);
        }
        minuteHigh.draw(canvas);
        canvas.restore();

        canvas.save();
        
        //dot
        if(changed){
            w = divider.getIntrinsicWidth();
            h = divider.getIntrinsicHeight();

            divider.setBounds(availableWidth - w - mMinuteLowWidth - mMinuteHighWidth ,0,  availableWidth - mMinuteLowWidth - mMinuteHighWidth, height);
        }
        divider.draw(canvas);
        canvas.restore();
         
        canvas.save();
        
        //hourLow
        if (changed) {
            w = hourLow.getIntrinsicWidth();
            h = hourLow.getIntrinsicHeight();

            hourLow.setBounds( availableWidth - w - mMinuteLowWidth - mMinuteHighWidth - mDividerWidth , 0,
                    availableWidth - mMinuteLowWidth - mMinuteHighWidth - mDividerWidth, height);
        }
        hourLow.draw(canvas);
        canvas.restore();
        
        canvas.save();

        //HourHigh
        if (changed) {
            w = hourHigh.getIntrinsicWidth();
            h = hourHigh.getIntrinsicHeight();
            hourHigh.setBounds(availableWidth - w - mMinuteLowWidth - mMinuteHighWidth - mDividerWidth - mHourLowWidth, 0,
                    availableWidth - mMinuteLowWidth - mMinuteHighWidth - mDividerWidth - mHourLowWidth, height);
        }
        hourHigh.draw(canvas);
        canvas.restore();

        canvas.save();

    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;

        updateTime();
        updateContentDescription(mCalendar);
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }

            onTimeChanged();
            
            invalidate();
        }
    };

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);

        setContentDescription(contentDescription);
    }

    private void updateTime() {

        final Calendar myCalendar = Calendar.getInstance(TimeZone.getDefault());
        myCalendar.setTimeInMillis(System.currentTimeMillis());

        //int my_Month = myCalendar.get(Calendar.MONTH);
        //int my_Day = myCalendar.get(Calendar.DAY_OF_MONTH);
        //int my_Week = myCalendar.get(Calendar.DAY_OF_WEEK);
        int my_hour_high = 0;
        int my_hour_low = 0;
        int my_min_high = 0;
        int my_min_low = 0;

        int my_Hour = myCalendar.get(Calendar.HOUR_OF_DAY);
        int my_Minute = myCalendar.get(Calendar.MINUTE);

        if (DateFormat.is24HourFormat(getContext())) {
            if(my_Hour==24){
                my_Hour=0;
            }
            my_hour_high = my_Hour/10;
            my_hour_low = my_Hour%10;
        } else {
            int am_pm = myCalendar.get(Calendar.AM_PM);
            if (my_Hour <= 12) {
                if (my_Hour == 0) {
                    my_Hour = 12;
                    my_hour_high = my_Hour/10;
                    my_hour_low = my_Hour%10;
                }else{
                    my_hour_high = my_Hour/10;
                    my_hour_low = my_Hour%10;
                }
            }else {
                my_Hour = my_Hour - 12;
                my_hour_high = my_Hour/10;
                my_hour_low = my_Hour%10;
            }
        }
        my_min_high = my_Minute/10;
        my_min_low = my_Minute%10;
        
        Log.v("hubing"," hubing hour-high->"+my_hour_high+"low ->"+my_hour_low+"-minute high->"+my_min_high+"low -->"+my_min_low);
        mHourHigh = mTimeDrawableArray.getDrawable(my_hour_high);
        mHourLow = mTimeDrawableArray.getDrawable(my_hour_low);
        mMinuteHigh = mTimeDrawableArray.getDrawable(my_min_high);
        mMinuteLow = mTimeDrawableArray.getDrawable(my_min_low);

    }


}
