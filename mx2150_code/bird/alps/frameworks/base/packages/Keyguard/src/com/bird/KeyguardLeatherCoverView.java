package com.android.keyguard;

import fm.xiami.aidl.IXIAMIMusicCallbackForThird;
import fm.xiami.aidl.IXIAMIPlayService;
import fm.xiami.aidl.SongInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.util.AttributeSet;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.text.format.DateFormat;
import android.widget.TextView;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

import java.util.Locale;
import android.widget.TextClock;
import android.text.TextUtils;

import com.google.android.mms.pdu.PduHeaders;

public class KeyguardLeatherCoverView extends RelativeLayout {
	private static final String TAG = "KeyguardLeatherCoverView";
	private final static Uri CONTENT_URI = Uri
			.parse("content://com.yunos.weather.provider/location");
	private static final int MSG_SMS_QUERY = 1;
	private static final int MSG_MMS_QUERY = 2;
	private static final int MSG_CALL_QUERY = 3;
	private final static String M12 = "h:mm";
	private final static String M24 = "kk:mm";
	private TextClock mDateTextClock;
	private TextView mWeekTextView;
	private TextView mDateTextView;
	private TextView mApmTextView;
	private RelativeLayout mWeather;
	private LinearLayout mWeatherTemLayout;
	private ImageView mWeatherIcon;
	private TextView mWeatherText;
	private RelativeLayout mUnreadCallMms;
	private LinearLayout mUnreadMms;
	private LinearLayout mMissedCall;
	private TextView mUnreadMmsTextView;
	private TextView mMissedCallTextView;
	private RelativeLayout mMusic;
	public TextView mMusicSong;
	public TextView mMusicArtist;
	public ImageButton mMusicPre;
	public ImageButton mMusicPlay;
	public ImageButton mMusicNext;
	public TextView mMusicTime;
	private int mSmsMmsCount;
	private int mMissedcallCount;
	private TextView mChargeText;
	private CharSequence mWeekFormatString;
	private CharSequence mDateFormatString;
	private Calendar mTime;
	private String mTimeZone;
	private Resources res;
	private final int MSG_SMS_MMS_QUERY_END = 1;
	private final int MSG_MISSED_CALL_QUERY_END = 2;
	// 20160127
	protected static final int UPDATE_INTERNAL = 1000;
	protected static final int UPDATE_PROGRESS = 1;
	protected static final int CALLBACK_PLAY_SONG_CHANGED = 10;
	protected static final int CALLBACK_PLAY_STATE_CHANGED = 11;
	public static final int XIAMI_SONG_PREPARE = 1001;
	public static final int XIAMI_SONG_PLAY = 1002;
	private int mPlayState;
	private boolean isCoverClosed = false;
	UpdateHandler mUHandler = new UpdateHandler();

	class UpdateHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (UPDATE_PROGRESS == msg.what) {
				Log.d("mandy","UPDATE_PROGRESS");
				updateProgressInfo();
			} else if (CALLBACK_PLAY_SONG_CHANGED == msg.what) {
				Log.d("mandy","CALLBACK_PLAY_SONG_CHANGED");
				updateProgressInfo();
				updateSongNameAndArtist();
			} else if (CALLBACK_PLAY_STATE_CHANGED == msg.what) {
				Log.d("mandy","CALLBACK_PLAY_STATE_CHANGED");
				updatePlayState(msg.arg1);
			}
		}

	}

	IXIAMIPlayService mService = null;

	IXIAMIMusicCallbackForThird mCallback = new IXIAMIMusicCallbackForThird.Stub() {

		@Override
		public void onServiceDestory() throws RemoteException {
			Log.e(TAG, " onServiceDestory ");
			mService = null;
		}

		@Override
		public void onPlaySongChanged() throws RemoteException {
			Log.e(TAG, " onPlaySongChanged ");
			Message msg = mUHandler.obtainMessage();
			msg.what = CALLBACK_PLAY_SONG_CHANGED;
			mUHandler.sendMessage(msg);
		}

		@Override
		public void onPlayStateChanged(long songId, int state)
				throws RemoteException {
			Log.e(TAG, " onPlayStateChanged ");
			Message msg = mUHandler.obtainMessage();
			msg.what = CALLBACK_PLAY_STATE_CHANGED;
			msg.arg1 = state;
			mUHandler.sendMessage(msg);
		}

		@Override
		public void onAlbumCoverChanged(Bitmap bitmap) throws RemoteException {

		}

		@Override
		public void onLyricChanged(String lyricPath, String text)
				throws RemoteException {

		}

	};
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.e(TAG, " onServiceConnected className " + className
					+ " service " + service);
			mService = IXIAMIPlayService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
				Log.e(TAG,
						" onServiceConnected mService.registerCallback(mCallback)");
				updateAllInfo();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			Log.e(TAG, " onServiceDisconnected className " + className);
		}
	};

	public void onScreenTurnedOff() {

	}

	private void setOnClick() {
		mMusicPre.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d("mandy", "mService" + mService);
				if (mService != null) {
					try {
						mService.prev();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
		mMusicPlay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d("mandy", "mMusicPlay mService" + mService);
				if (mService != null) {
					try {
						if (isPlaying()) {
							mService.pause();
						} else {
							mService.resume();
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
		mMusicNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mService != null) {
					try {
						mService.next();
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});

	}

	private void updateSongNameAndArtist() {
		// TODO Auto-generated method stub
		Log.e(TAG, " updateSongNameAndArtist");
		if (mService != null) {
			try {
				SongInfo info = mService.getPlaySongInfo();
				Log.e(TAG, " mService.getPlaySongInfo()");
				if (info != null) {
					Log.e(TAG, " updateSongNameAndArtist " + info.getSongName()
							+ " " + info.getArtistName());
					updateSongNameAndArtist(info.getSongName(),
							info.getArtistName());
				}
			} catch (RemoteException e) {
				e.printStackTrace();
				Log.e(TAG,
						" updateSongNameAndArtist e.printStackTrace() "
								+ e.toString());
			}
		}
	}

	private void updatePlayState(int staus) {
		// TODO Auto-generated method stub
		Log.d("mandy","updatePlayState");
		mPlayState = staus;
		if (mMusicPlay != null) {
			if ((XIAMI_SONG_PREPARE == mPlayState)
					|| (XIAMI_SONG_PLAY == mPlayState)) {
				mMusicPlay.setImageResource(R.drawable.music_drawable_pause);
			} else {
				mMusicPlay.setImageResource(R.drawable.music_drawable_play);
			}
		}

		if (XIAMI_SONG_PLAY == mPlayState && mMusic.getVisibility() == View.GONE) {
			mMusic.setVisibility(View.VISIBLE);
			updateAllInfo();
			mWeather.setVisibility(View.GONE);
			mUnreadCallMms.setVisibility(View.GONE);
		}
	}

	private void updateProgressInfo() {
		if (mService != null) {
			try {
				updateProgressInfo(mService.getCurrTime(),
						mService.getDuration());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateProgressInfo(long current, long total) {
		Log.d("mandy","updateProgressInfo");
		if (mMusicTime != null) {
			mMusicTime.setText(formatToPlayTime(current) + " | "
					+ formatToPlayTime(total));
		}
		if(mMusic.getVisibility() == View.VISIBLE && isCoverClosed){
			mUHandler.removeMessages(UPDATE_PROGRESS);
			mUHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, UPDATE_INTERNAL);
		}
	}

	private boolean isPlaying() {
		if ((XIAMI_SONG_PREPARE == mPlayState || XIAMI_SONG_PLAY == mPlayState)
				&& mService == null) {
			Intent intent = new Intent("fm.xiami.walkman.third_remote_binder");
			intent.setClassName("com.xiami.walkman",
					"com.xiami.walkman.service.PlayService");
			if (getContext().getPackageManager().resolveService(intent,
					PackageManager.GET_SERVICES) != null) {
				Log.v(TAG, "bindXiamiService_01");
				getContext().bindService(intent, mConnection,
						Context.BIND_AUTO_CREATE);
			} else {
				Log.v(TAG, "bindXiamiService_02");
				intent = new Intent("fm.xiami.yunos.third_remote_binder");
				intent.setClassName("fm.xiami.yunos",
						"fm.xiami.bmamba.PlayService");
				getContext().bindService(intent, mConnection,
						Context.BIND_AUTO_CREATE);
			}
		} else if ((XIAMI_SONG_PREPARE == mPlayState || XIAMI_SONG_PLAY == mPlayState)
				&& mService != null) {
			return true;
		} else if (mService == null) {
			Intent intent = new Intent("fm.xiami.walkman.third_remote_binder");
			intent.setClassName("com.xiami.walkman",
					"com.xiami.walkman.service.PlayService");
			if (getContext().getPackageManager().resolveService(intent,
					PackageManager.GET_SERVICES) != null) {
				Log.v(TAG, "bindXiamiService_01");
				getContext().bindService(intent, mConnection,
						Context.BIND_AUTO_CREATE);
			} else {
				Log.v(TAG, "bindXiamiService_02");
				intent = new Intent("fm.xiami.yunos.third_remote_binder");
				intent.setClassName("fm.xiami.yunos",
						"fm.xiami.bmamba.PlayService");
				getContext().bindService(intent, mConnection,
						Context.BIND_AUTO_CREATE);
			}
		}
		return false;

	}

	private void updateAllInfo() {
		// TODO Auto-generated method stub
		updateProgressInfo();
		updateSongNameAndArtist();
		updatePlayState();
	}

	private void updatePlayState() {
		if (mService != null) {
			try {
				updatePlayState(mService.getPlayState());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	// 20160127
	private static final Uri MMS_QUERY_URI = Uri.parse("content://mms/inbox");
	private static final String NEW_INCOMING_MM_CONSTRAINT = "(" + Mms.READ
			+ " = 0 " + " AND " + Mms.MESSAGE_TYPE + " <> "
			+ PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + " AND "
			+ Mms.MESSAGE_TYPE + " <> " + PduHeaders.MESSAGE_TYPE_READ_ORIG_IND
			+ ") ";

	private final BroadcastReceiver mlanguageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mWeekFormatString = res
					.getText(R.string.zzzz_keyguard_leather_week_day);
			mDateFormatString = res
					.getText(R.string.zzzz_keyguard_leather_date_day);
			createTime(mTimeZone);
			refreshDate();
		}
	};
	private BroadcastReceiver InfoChangedReceiver = new BroadcastReceiver() {
		private Context mContext;

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			// Post a runnable to avoid blocking the broadcast.
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				final int pluggedInStatus = intent.getIntExtra("status",
						BatteryManager.BATTERY_STATUS_UNKNOWN);
				int batteryLevel = intent.getIntExtra("level", 0);
				updateBatteryInfo(pluggedInStatus, batteryLevel);
			}
		}
	};
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
	private final BroadcastReceiver mWeatherReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateWeather();
		}
	};

	public KeyguardLeatherCoverView(Context context) {
		this(context, null, 0);
		// TODO Auto-generated constructor stub
	}

	public KeyguardLeatherCoverView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public KeyguardLeatherCoverView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		res = getContext().getResources();
		mDateTextClock = (TextClock) findViewById(R.id.zzzz_keyguard_bird_leather_date);

		mWeekTextView = (TextView) findViewById(R.id.zzzz_keyguard_bird_leather_week_day);
		mApmTextView = (TextView) findViewById(R.id.zzzz_keyguard_bird_leather_apm);
		mWeekFormatString = res
				.getText(R.string.zzzz_keyguard_leather_week_day);
		mDateFormatString = res
				.getText(R.string.zzzz_keyguard_leather_date_day);
		mUnreadCallMms = (RelativeLayout) findViewById(R.id.zzzz_leather_call_mms);
		mUnreadMms = (LinearLayout) findViewById(R.id.zzzz_leather_mms);
		mMissedCall = (LinearLayout) findViewById(R.id.zzzz_leather_missed_call);
		mUnreadMmsTextView = (TextView) findViewById(R.id.zzzz_leather_mms_text);
		mMissedCallTextView = (TextView) findViewById(R.id.zzzz_leather_missed_call_text);
		mSmsMmsCount = mMissedcallCount = -1;
		mWeather = (RelativeLayout) findViewById(R.id.zzzz_leather_weather);
		mWeatherTemLayout = (LinearLayout) findViewById(R.id.zzzz_leather_weather_tem_view);
		mWeatherIcon = (ImageView) findViewById(R.id.zzzz_leather_weather_icon);
		mWeatherText = (TextView) findViewById(R.id.zzzz_leather_weather_tem);
		mChargeText = (TextView) findViewById(R.id.zzzz_leather_charge);
		mMusic = (RelativeLayout) findViewById(R.id.music_view);
		mMusicSong = (TextView) findViewById(R.id.music_song);
		mMusicArtist = (TextView) findViewById(R.id.music_artist);
		mMusicPre = (ImageButton) findViewById(R.id.music_prev);
		mMusicPlay = (ImageButton) findViewById(R.id.music_play);
		mMusicNext = (ImageButton) findViewById(R.id.music_next);
		mMusicTime = (TextView) findViewById(R.id.time);
		createTime(mTimeZone);
		refresh();
		setOnClick();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		getContext().registerReceiver(mIntentReceiver, filter);
		IntentFilter languagefilter = new IntentFilter();
		languagefilter.addAction(Intent.ACTION_LOCALE_CHANGED);
		languagefilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		getContext().registerReceiver(mlanguageReceiver, languagefilter);
		IntentFilter chargefilter = new IntentFilter();
		chargefilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		getContext().registerReceiver(InfoChangedReceiver, chargefilter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_UNREAD_CHANGED);
        getContext().registerReceiver(mUnreadLoader, filter);

		loadAndInitUnreadShortcuts();
        
	}

	@Override
	public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getContext().unregisterReceiver(mIntentReceiver);
		getContext().unregisterReceiver(mlanguageReceiver);
		getContext().unregisterReceiver(InfoChangedReceiver);
		getContext().unregisterReceiver(mUnreadLoader);
        
		if (mService != null) {
			try {
				mService.unRegisterCallback(mCallback);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		getContext().unbindService(mConnection);
	}

	protected void refresh() {
		refreshDate();
		// updateWeather();
	}

	private boolean isBatteryLow(int batteryLevel) {
		return batteryLevel < 16;
	}

	private boolean isPluggedIn(int status) {
		return status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL;
	}

	private void updateBatteryInfo(int battery_status, int batteryLevel) {
		// TODO Auto-generated method stub
		final boolean isPluggedIn = isPluggedIn(battery_status);
		final boolean batteryIsLow = isBatteryLow(batteryLevel);
		final boolean showingBatteryInfo = isPluggedIn || batteryIsLow;
		final boolean charging = isPluggedIn;
		final boolean batteryCharged = (battery_status == BatteryManager.BATTERY_STATUS_FULL);
		CharSequence string = null;
		if (showingBatteryInfo) {
			// Battery status
			if (charging) {
				// Charging, charged or waiting to charge.
				string = batteryCharged ? getContext().getString(
						R.string.keyguard_charged) : getContext().getString(
						R.string.keyguard_plugged_in)
						+ ":" + batteryLevel + "%";
			} else if (batteryIsLow) {
				// Battery is low
				string = getContext().getString(
						R.string.zz_keyguard_low_battery);
			}
		}
		if (mChargeText != null) {
			mChargeText.setText(string);
		}
	}

	private void updateWeather() {
		mWeatherIcon.setImageResource(R.drawable.weather_unknow);
		mWeatherTemLayout.setVisibility(View.GONE);
		int mConditionCode = 901;
		ContentResolver contentResolver = getContext().getContentResolver();
		Cursor cursor = null;
		String mTemp = null;
		try {
			cursor = contentResolver.query(CONTENT_URI, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndex("condition_code");
				if (index == -1) {
					return;
				}
				mConditionCode = cursor.getInt(index);
				if (mConditionCode == 901) {
					return;
				}
				index = cursor.getColumnIndex("current_temperature");
				if (index == -1) {
					return;
				}
				mTemp = cursor.getString(index);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		if (mTemp != null) {
			mWeatherTemLayout.setVisibility(View.VISIBLE);
			mWeatherText.setText(mTemp);
			mWeather.setVisibility(View.GONE);
			mWeatherIcon
					.setImageResource(conditionTranslateImageRes(mConditionCode));
		}
	}

	private int conditionTranslateImageRes(int weather) {
		int iconId = -1;
		Log.v(TAG, "conditionTranslate orgStr=" + weather);
		switch (weather) {
		case 0:
			iconId = R.drawable.weather_0;
			break;
		case 100:
			iconId = R.drawable.weather_1;
			break;
		case 101:
			iconId = R.drawable.weather_2;
			break;
		case 201:
		case 202:
			iconId = R.drawable.weather_3;
			break;
		case 203:
		case 204:
			iconId = R.drawable.weather_4;
			break;
		case 205:
		case 206:
		case 207:
		case 208:
		case 209:
		case 211:
			iconId = R.drawable.weather_5;
			break;
		case 212:
			iconId = R.drawable.weather_6;
			break;
		case 213:
			iconId = R.drawable.weather_7;
			break;
		case 214:
		case 215:
			iconId = R.drawable.weather_8;
			break;
		case 301:
			iconId = R.drawable.weather_9;
			break;
		case 302:
			iconId = R.drawable.weather_10;
			break;
		case 303:
		case 304:
			iconId = R.drawable.weather_11;
			break;
		case 305:
		case 306:
			iconId = R.drawable.weather_12;
			break;
		case 307:
		case 308:
		case 309:
			iconId = R.drawable.weather_13;
			break;
		case 400:
			iconId = R.drawable.weather_14;
			break;
		case 500:
		case 501:
		case 502:
		case 503:
		case 504:
			iconId = R.drawable.weather_15;
			break;
		default:
			iconId = -1;
			break;
		}

		return iconId;
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

	private void freshmessage() {
		int sms = 0, mms = 0;
		Cursor curSMS = null;
		ContentResolver cr = getContext().getContentResolver();
		try {
			curSMS = cr.query(Uri.parse("content://sms"), null,
					"type = 1 and read = 0", null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Cursor curMMS = null;
		try {
			curMMS = cr.query(MMS_QUERY_URI, null, NEW_INCOMING_MM_CONSTRAINT,
					null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (curSMS != null) {
			sms = curSMS.getCount();
		}
		if (curMMS != null) {
			mms = curMMS.getCount();
		}
		final int msgNum = sms + mms;
		if (msgNum <= 0) {
			mUnreadMms.setVisibility(View.GONE);
		} else {
			mUnreadMms.setVisibility(View.VISIBLE);
			mUnreadMmsTextView.setText(String.valueOf(msgNum));
		}
		if (curSMS != null) {
			curSMS.close();
		}
		if (curMMS != null) {
			curMMS.close();
		}
	}

	private void freshmisscall() {
       Log.d("aaman","freshmisscall");
		int call = 0;
		Cursor curCall = null;
		ContentResolver cr = getContext().getContentResolver();

		String where = new String(CallLog.Calls.TYPE + "="
				+ CallLog.Calls.MISSED_TYPE + " and new=1");
		try {
			curCall = cr.query(CallLog.Calls.CONTENT_URI, null, where, null,
					CallLog.Calls.DEFAULT_SORT_ORDER);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (curCall != null) {
			call = curCall.getCount();
		}
		if (call > 0) {
			mMissedCall.setVisibility(View.VISIBLE);
			mMissedCallTextView.setText(String.valueOf(call));
		} else {
			mMissedCall.setVisibility(View.GONE);
		}
        Log.d("aaman","freshmisscall call = "+call);
		if (curCall != null) {
			curCall.close();
		}
	}

	private void updateSongNameAndArtist(String name, String artist) {
		Log.e(TAG, " updateSongNameAndArtist name " + name + " artist "
				+ artist);
		if (mMusicSong != null) {
			mMusicSong.setText(name);
		}
		if (mMusicArtist != null) {
			mMusicArtist.setText(artist);
		}
	}

	private static String formatToPlayTime(long mills) {
		int secs = (int) (mills / 1000);
		int leftSecs = secs % 60;
		int mins = secs / 60;

		return String.format("%s:%s",
				mins < 10 ? ("0" + mins) : String.valueOf(mins),
				leftSecs < 10 ? ("0" + leftSecs) : String.valueOf(leftSecs));
	}
	private boolean isButton(int x, int y){
		Rect frame = new Rect();
        boolean contain = false;
        mMusicPre.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        mMusicPlay.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        mMusicNext.getHitRect(frame);
        contain = frame.contains(x, y);
        if (contain) {
            return true;
        }
        return false;
	}
	public void onCoverClosed() {
		isCoverClosed = true;
		if (isPlaying()) {
			mMusic.setVisibility(View.VISIBLE);
			updateAllInfo();
			mWeather.setVisibility(View.GONE);
			mUnreadCallMms.setVisibility(View.GONE);
		} else {
			mMusic.setVisibility(View.GONE);
			mUHandler.removeMessages(UPDATE_PROGRESS);
			mWeather.setVisibility(View.GONE);
			mUnreadCallMms.setVisibility(View.VISIBLE);
			updateWeather();

		}
	}
	public void onCoverOpen(){
		isCoverClosed = false;
		if(mMusic.getVisibility() == View.VISIBLE){
			mUHandler.removeMessages(UPDATE_PROGRESS);
		}
	}
	public boolean onTouchEvent(MotionEvent event) {
		if(mMusic.getVisibility() == View.VISIBLE){
			float tempX = event.getRawX();
	        float tempY = event.getRawY();
			if(!isButton((int) tempX, (int) tempY)){
				return true;
			}
		}
		return true;
    }

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
}
