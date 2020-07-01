package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.TextView;
import android.widget.TextClock;
import java.util.Calendar;
import android.content.res.Resources;
import java.util.TimeZone;
import android.text.format.DateFormat;
import java.text.DateFormatSymbols;
import android.view.View;
import android.util.Log;
import android.widget.LinearLayout;
import android.os.AsyncTask;
import android.content.ContentResolver;
import android.content.ComponentName;

/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
public class BirdKeyguardLeatherStandByScreen extends RelativeLayout {
    private static final String TAG = "BirdKeyguardLeatherStandByScreen";
    private LeatherAnalogClock mAnalogClock;
    private TextClock mDateTextClock;
    private TextView mWeekTextView;
    private TextView mApmTextView;
    private CharSequence mWeekFormatString;
    private CharSequence mDateFormatString;
    private Calendar mTime;
    private String mTimeZone;
    private RelativeLayout mUnreadCallMms;
    private LinearLayout mUnreadMms;
    private LinearLayout mMissedCall;
    private TextView mUnreadMmsTextView;
    private TextView mMissedCallTextView;
    private int mSmsMmsCount;
    private int mMissedcallCount;
    private Resources res;
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";
    
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
				mTimeZone = intent.getStringExtra("time-zone");
			}
			createTime(mTimeZone);
			refreshDate();
		}
	};
    
	public BirdKeyguardLeatherStandByScreen(Context context) {
		this(context, null, 0);
		// TODO Auto-generated constructor stub
	}

	public BirdKeyguardLeatherStandByScreen(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BirdKeyguardLeatherStandByScreen(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
        super.onFinishInflate();
        res = getContext().getResources();
        mAnalogClock = (LeatherAnalogClock)findViewById(R.id.anagclock);
        mAnalogClock.enableSeconds(true);
        mDateTextClock = (TextClock) findViewById(R.id.zzzz_keyguard_bird_leather_date);
        mWeekTextView = (TextView) findViewById(R.id.zzzz_keyguard_bird_leather_week_day);
        mApmTextView = (TextView) findViewById(R.id.zzzz_keyguard_bird_leather_apm);
        mWeekFormatString = res
        		.getText(R.string.zzzz_keyguard_leather_week_day);
        mDateFormatString = res
        		.getText(R.string.zzzz_keyguard_leather_date_style1);
        mUnreadCallMms = (RelativeLayout) findViewById(R.id.zzzz_leather_call_mms);
        mUnreadMms = (LinearLayout) findViewById(R.id.zzzz_leather_mms);
        mMissedCall = (LinearLayout) findViewById(R.id.zzzz_leather_missed_call);
        mUnreadMmsTextView = (TextView) findViewById(R.id.zzzz_leather_mms_text);
        mMissedCallTextView = (TextView) findViewById(R.id.zzzz_leather_missed_call_text);
        mSmsMmsCount = mMissedcallCount = -1;
        createTime(mTimeZone);
        refreshDate();
	}

	private void createTime(String timeZone) {
		if (timeZone != null) {
			mTime = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
		} else {
			mTime = Calendar.getInstance();
		}
	}

	private void refreshDate() {
		mDateTextClock.setFormat24Hour(mDateFormatString);
		mDateTextClock.setFormat12Hour(mDateFormatString);
		CharSequence weekChSequence = DateFormat.format(mWeekFormatString,
				mTime);
		mWeekTextView.setText(weekChSequence);
		String mFormat = android.text.format.DateFormat
				.is24HourFormat(getContext()) ? M24 : M12;
		if (mFormat.equals(M12)) {
			mApmTextView.setVisibility(View.VISIBLE);
			String[] ampm = new DateFormatSymbols().getAmPmStrings();
			if (mTime.get(Calendar.AM_PM) == 0) {
				mApmTextView.setText(ampm[0]);
			} else {
				mApmTextView.setText(ampm[1]);
			}

		} else {
			mApmTextView.setVisibility(View.GONE);
		}

		Log.i(TAG, "week = " + weekChSequence);
	}
    
	@Override
	public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_UNREAD_CHANGED);
        getContext().registerReceiver(mUnreadLoader, filter);
        loadAndInitUnreadShortcuts();
	}

	@Override
	public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mIntentReceiver);
        getContext().unregisterReceiver(mUnreadLoader);
	}
	public void updateTime(){
		mAnalogClock.showAnalogClock(true);
	}


    /**
     * Load and initialize unread shortcuts.
     *
     * @param context
     */
    void loadAndInitUnreadShortcuts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... unused) {
                initUnreadNumberFromSystem();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
				refreshMissView();
            }
        }.execute();
    }

	void initUnreadNumberFromSystem() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            mMissedcallCount = android.provider.Settings.System.getInt(cr, "com_android_contacts_mtk_unread");
			mSmsMmsCount = android.provider.Settings.System.getInt(cr, "com_android_mms_mtk_unread");
        } catch (android.provider.Settings.SettingNotFoundException e) {
            Log.e("pangmeizhou", "e = " + e.getMessage());
        }
    }

	BroadcastReceiver mUnreadLoader = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			// TODO Auto-generated method stub
			if (Intent.ACTION_UNREAD_CHANGED.equals(action)) {
				final ComponentName componentName = (ComponentName) intent
				        .getExtra(Intent.EXTRA_UNREAD_COMPONENT);
				final int unreadNum = intent.getIntExtra(Intent.EXTRA_UNREAD_NUMBER, -1);
				Log.d("pangmeizhou", "Receive unread broadcast: componentName = " + componentName
				        + ", unreadNum = " + unreadNum);
				if (componentName != null && unreadNum != -1) {
					String className = componentName.getClassName();
					if(className.equals("com.android.dialer.DialtactsActivity")) {
						mMissedcallCount = unreadNum;
					}
					if(className.equals("com.android.mms.ui.BootActivity")) {
						mSmsMmsCount = unreadNum;
					}
					refreshMissView();
				}
			}
		}
	};

    void refreshMissView() {
        if(mMissedcallCount <= 0) {
            mMissedCall.setVisibility(View.GONE);
        } else {
            mMissedCallTextView.setText(""+mMissedcallCount);
            mMissedCall.setVisibility(View.VISIBLE);
        }

        if(mSmsMmsCount <= 0) {
            mUnreadMms.setVisibility(View.GONE);
        } else {
            mUnreadMmsTextView.setText(""+mSmsMmsCount);
            mUnreadMms.setVisibility(View.VISIBLE);
        }
    }

	
}
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
