package com.android.keyguard;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
public class LeatherAnalogClock extends View {
	private boolean mAttached;
	private Time mCalendar;
	private boolean mChanged;
	private final Context mContext;
	private final Drawable mDial;
	private final int mDialHeight;
	private final int mDialWidth;
	private final Handler mHandler = new Handler();
	private float mHour;
	private final Drawable mHourHand;
	private final Drawable mMinuteHand;
	private float mMinutes;
	private final Drawable mSecondHand;
	private final Drawable mDot;
	private float mSeconds;
	private String mTimeZoneId;
	private boolean mNoSeconds = false;
	private final Runnable mClockTick = new Runnable() {
		public void run() {
			onTimeChanged();
			invalidate();
			postDelayed(mClockTick, 1000L);
		}
	};
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context paramContext, Intent paramIntent) {
			if (paramIntent.getAction().equals(
					"android.intent.action.TIMEZONE_CHANGED")) {
				String str = paramIntent.getStringExtra("time-zone");
				mCalendar = new Time(TimeZone.getTimeZone(str).getID());
			}
			onTimeChanged();
			invalidate();
		}
	};

	public LeatherAnalogClock(Context paramContext) {
		this(paramContext, null);
	}

	public LeatherAnalogClock(Context paramContext,
			AttributeSet paramAttributeSet) {
		this(paramContext, paramAttributeSet, 0);
	}

	public LeatherAnalogClock(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		mContext = context;
		Resources r = mContext.getResources();
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.my_analog_clock, defStyleAttr, 0);
		mDial = a.getDrawable(R.styleable.my_analog_clock_dial);
		mHourHand = a.getDrawable(R.styleable.my_analog_clock_hand_hour);
		mMinuteHand = a.getDrawable(R.styleable.my_analog_clock_hand_minute);
		mSecondHand = a.getDrawable(R.styleable.my_analog_clock_hand_second);
		mDot = a.getDrawable(R.styleable.my_analog_clock_dot);
		mCalendar = new Time();
		mDialWidth = mDial.getIntrinsicWidth();
		mDialHeight = mDial.getIntrinsicHeight();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		float hScale = 1.0f;
		float vScale = 1.0f;

		if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
			hScale = (float) widthSize / (float) mDialWidth;
		}

		if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
			vScale = (float) heightSize / (float) mDialHeight;
		}

		float scale = Math.min(hScale, vScale);

		setMeasuredDimension(
				resolveSizeAndState((int) (mDialWidth * scale),
						widthMeasureSpec, 0),
				resolveSizeAndState((int) (mDialHeight * scale),
						heightMeasureSpec, 0));
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mChanged = true;
	}

	private void drawHand(Canvas canvas, Drawable drawable, int x, int y,
			float angle, boolean change) {
		canvas.save();
		canvas.rotate(angle, x, y);
		if (change) {
			int w = drawable.getIntrinsicWidth();
			int h = drawable.getIntrinsicHeight();
			drawable.setBounds(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
		}
		drawable.draw(canvas);
		canvas.restore();
	}

	protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean scaled = false;
        boolean change = mChanged;
        if (change) {
        	mChanged = false;
        }
        int availableWidth = getWidth();
        int availableHeight = getHeight();
        int x = availableWidth / 2;
        int y = availableHeight / 2;
        Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();
        if (availableWidth < w || availableHeight < h) {
        	scaled = true;
        	float scale = Math.min((float) availableWidth / (float) w,
        			(float) availableHeight / (float) h);
        	canvas.save();
        	canvas.scale(scale, scale, x, y);
        }
        if (change) {
        	dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);
        drawHand(canvas, mHourHand, x, y, 360.0F * (mHour / 12.0F), change);
        drawHand(canvas, mMinuteHand, x, y, 360.0F * (mMinutes / 60.0F), change);
        if(!mNoSeconds){
        	drawHand(canvas, mSecondHand, x, y, 360.0F * (mSeconds / 60.0F), change);
        }
        if (mDot != null) {
            w = mDot.getIntrinsicWidth();
            h = mDot.getIntrinsicHeight();
            mDot.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            mDot.draw(canvas);
        }

		if (scaled) {
			canvas.restore();
		}
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
			getContext().registerReceiverAsUser(mIntentReceiver,
					android.os.Process.myUserHandle(), filter, null, mHandler);
		}
		mCalendar = new Time();
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

	private void onTimeChanged() {
		mCalendar.setToNow();
		if (mTimeZoneId != null)
			mCalendar.switchTimezone(mTimeZoneId);
		int i = mCalendar.hour;
		int j = mCalendar.minute;
		int k = mCalendar.second;
		mSeconds = k;
		mMinutes = (j + k / 60.0F);
		mHour = (i + this.mMinutes / 60.0F);
		mChanged = true;
		updateContentDescription(mCalendar);
	}

	private void updateContentDescription(Time time) {
		final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
		String contentDescription = DateUtils.formatDateTime(mContext,
				time.toMillis(false), flags);
		setContentDescription(contentDescription);
	}

	public void enableSeconds(boolean enable) {
		mNoSeconds = !enable;
	}

	public void showAnalogClock(boolean show) {
		if(show){
			onTimeChanged();
			if(!mNoSeconds){
				removeCallbacks(mClockTick);
				post(mClockTick);
			}
		}
	}
	
}
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
