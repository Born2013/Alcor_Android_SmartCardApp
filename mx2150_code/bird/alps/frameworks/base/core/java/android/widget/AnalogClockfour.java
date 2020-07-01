package android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.internal.R;
import java.util.TimeZone;

public class AnalogClockfour extends View {

	private Context mContext;
	private final Paint mPaint = new Paint();
	private float width;
	private float height;

	private Time mCalendar;
	private int mDate;
	private float mHour;
	private float mMinutes;
	private float mSecond;

	private Handler myhandler;

	private boolean mAttached;

	public AnalogClockfour(Context context) {
		this(context, null);
	}

	public AnalogClockfour(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnalogClockfour(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		mCalendar = new Time();
		mCalendar.setToNow();
		int date = mCalendar.monthDay;
		int hour = mCalendar.hour;
		int minute = mCalendar.minute;
		int second = mCalendar.second;

		mSecond = second;
		mMinutes = minute + second / 60.0f;
		mHour = hour + mMinutes / 60.0f;
		mDate = date;

		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);

		myhandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (msg.what == 0x123) {
					sendMessageDelayed(obtainMessage(0x123), 1000);
					onTimeChanged();
					invalidate();
				}
			}
		};
	}

	@Override
	protected void onDraw(Canvas canvas) {
		width = getWidth();
		height = getHeight();

		super.onDraw(canvas);
		Bitmap dial = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_04_dial, width, width);
		Bitmap date = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_04_date, width, width);
		Bitmap hour = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_04_hour, width, width);
		Bitmap minute = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_04_minute, width, width);
		Bitmap second = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_04_second, width, width);

		canvas.drawBitmap(dial, 0, (height - width) / 2, mPaint);
		canvas.drawBitmap(date, 0, (height - width) / 2, mPaint);

		canvas.save();
		canvas.rotate(mHour / 12.0f * 360.0f, width / 2, height / 2);
		canvas.drawBitmap(hour, 0, (height - width) / 2, mPaint);
		canvas.restore();

		canvas.save();
		canvas.rotate(mMinutes / 60.0f * 360.0f, width / 2, height / 2);
		canvas.drawBitmap(minute, 0, (height - width) / 2, mPaint);
		canvas.restore();

		canvas.save();
		canvas.rotate(mSecond / 60.0f * 360.0f, width / 2, height / 2);
		canvas.drawBitmap(second, 0, (height - width) / 2, mPaint);
		canvas.restore();
	}


	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (!mAttached) {
			myhandler.sendEmptyMessage(0x123);
			mAttached = true;
			final IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			filter.addAction(Intent.ACTION_DATE_CHANGED);
			filter.addAction(Intent.ACTION_LOCALE_CHANGED);
			filter.addAction(Intent.ACTION_SCREEN_ON);
			getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
		}

		mCalendar = new Time();
		onTimeChanged();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			myhandler.removeMessages(0x123);
			mAttached = false;
		}
	}

	private void onTimeChanged() {
		mCalendar.setToNow();

		int date = mCalendar.monthDay;
		int hour = mCalendar.hour;
		int minute = mCalendar.minute;
		int second = mCalendar.second;

		mSecond = second;
		mMinutes = minute + second / 60.0f;
		mHour = hour + mMinutes / 60.0f;
		mDate = date;

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

	public Bitmap getBmpScale(Context mContext, int id, float cell_width, float cell_height) {
		Bitmap viewBg = BitmapFactory.decodeResource(mContext.getResources(), id);
		Matrix matrix = new Matrix();
		int width = viewBg.getWidth();//获取资源位图的宽
		int height = viewBg.getHeight();//获取资源位图的高
		float w = cell_width / viewBg.getWidth();
		float h = cell_height / viewBg.getHeight();
		matrix.postScale(w, h);//获取缩放比例
		Bitmap dstbmp = Bitmap.createBitmap(viewBg, 0, 0, width, height, matrix, true); //根据缩放比例获取新的位图
		return dstbmp;
	}
}

