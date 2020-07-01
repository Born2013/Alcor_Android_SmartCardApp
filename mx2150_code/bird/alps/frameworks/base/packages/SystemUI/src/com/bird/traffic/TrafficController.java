package com.bird.traffic;

import java.math.BigDecimal;

import com.android.systemui.statusbar.phone.PhoneStatusBar;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.telephony.TelephonyIntents;
import android.provider.Settings;
import android.util.Log;

public class TrafficController extends BroadcastReceiver {

    private static final String TAG = "TrafficController";

    private Context mContext;
    private TextView mView;
    private PhoneStatusBar mStatusBar;
    private SharedPreferences mSharedPreferences;
    private IntentFilter mIntentFilter;
    private static final boolean DEBUG = false;
    private static final int UPDATE_TIME_SECONDS = 1;
    private static final int UPDATE_TIME_MILI_SECONDS = UPDATE_TIME_SECONDS * 1000;

    private static final String UPDATE_MOBILE_DATA_STATE_ACTION = "com.bird.systemui.action.UPDATE_MOBILE_DATA_STATE";
    private static final String TOGGOLE_TRAFFIC_ACTION = "com.bird.systemui.action.TOGGOLE_TRAFFIC";

    private static final String TOGGOLE_TRAFFIC_EXTRA = "toggole_traffic";

    private static final int ONE_M = 1024 * 1024;
    private static final int TEN_K = 10 * 1024;
    private static final int ONE_K = 1024;

    private long mRecvBytesOld = 0;
    private double mNetworkSpeedRecv = 0;
    private PendingIntent mPendingIntent;
    boolean mMobileDataOpened, mWifiOpened;
    boolean mDataDisconnected = false;
    boolean mCanBeVisible = true;
    boolean mToggole = false;
    boolean mScreenOn = true;

    private static final int UPDATE_TRAFFIC = 2;
    private boolean mStarted = false;
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TRAFFIC:
                    updateTrafficNum();
                    break;
                default:
                    break;
            }
        }
    };

    public TrafficController(Context context) {
        mContext = context;
        // check traffic function open or close
        mToggole = Settings.System.getInt(mContext.getContentResolver(), Settings.System.BIRD_DEFAULT_THE_INSTANT_SPEED,0) == 1;
        // create wifi, mobile data open close intent filter
        mIntentFilter = new IntentFilter();

        mIntentFilter.addAction(TOGGOLE_TRAFFIC_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(this, mIntentFilter);

        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiOpened = wifiManager.isWifiEnabled();

        Log.d(TAG,"mToggole = "+mToggole);

        if (mToggole) {
            mCanBeVisible = true;
            startMonitor();
        }

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionString = intent.getAction();
        Log.d(TAG,"actionString = "+actionString);
        if (mView == null) {
            return;
        }
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(actionString)) {
            final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            mWifiOpened = networkInfo != null && networkInfo.isConnected();
            updateTrafficVisibility();
            if (mWifiOpened) {
                startMonitor();
            }

        } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(actionString)
                || ConnectivityManager.CONNECTIVITY_ACTION.equals(actionString)
                || Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(actionString)
                || TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED.equals(actionString)) {

            TelephonyManager telMgr = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            mMobileDataOpened = telMgr.getDataEnabled() && isSimStateOk(mContext);
            updateTrafficVisibility();

            if (mMobileDataOpened) {
                startMonitor();
            }

        } else if (TOGGOLE_TRAFFIC_ACTION.equals(actionString)) {
            mToggole = intent.getBooleanExtra(TOGGOLE_TRAFFIC_EXTRA, false);
            if (mToggole) {
                startMonitor();
            } else {
                stopMonitor();
            }
            updateTrafficVisibility();
        } else if (Intent.ACTION_SCREEN_ON.equals(actionString)) {
            mScreenOn = true;
            startMonitor();
        } else if (Intent.ACTION_SCREEN_OFF.equals(actionString)) {
            mScreenOn = false;
            stopMonitor();
        }
    }

    public final void startMonitor() {

        if ((mMobileDataOpened || mWifiOpened) && mToggole && mScreenOn && !mStarted) {
            mRecvBytesOld = TrafficStats.getTotalRxBytes();
            mHandler.sendEmptyMessageDelayed(UPDATE_TRAFFIC, UPDATE_TIME_MILI_SECONDS);
            mStarted = true;
        }

    }

    public void stopMonitor() {
        mStarted = false;
        if (mHandler.hasMessages(UPDATE_TRAFFIC)) {
            mHandler.removeMessages(UPDATE_TRAFFIC);
        }
    }

    public void addTrafficView(TextView view) {
        mView = view;
    }

    public boolean getViewVisibility() {
        return (mView.getVisibility() == View.VISIBLE);
    }

    // show or hide the traffic view
    public void setTrafficCanBeVisible(boolean show) {
        mCanBeVisible = show;
        updateTrafficVisibility();
    }

    private void updateTrafficVisibility() {
        if ((mMobileDataOpened || mWifiOpened) && mCanBeVisible && mToggole) {
            mView.setVisibility(View.VISIBLE);
        } else {
            mView.setVisibility(View.GONE);
        }
    }

    private void updateTrafficNum() {
        if (mView == null) {
            return;
        }
        if (DEBUG)
            Log.d(TAG, "updateTrafficNum");
        long recvBytes = TrafficStats.getTotalRxBytes();
        mNetworkSpeedRecv = ((double) (recvBytes - mRecvBytesOld) / UPDATE_TIME_SECONDS);
        if (mNetworkSpeedRecv < ONE_M && mNetworkSpeedRecv >= TEN_K) {
            mNetworkSpeedRecv = mNetworkSpeedRecv / ONE_K;
            BigDecimal b = new BigDecimal(mNetworkSpeedRecv);
            mNetworkSpeedRecv = b.setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue();
            int speed = (int) mNetworkSpeedRecv;
            mView.setText(speed + "K/s");
        } else if (mNetworkSpeedRecv < TEN_K) {
            mNetworkSpeedRecv = mNetworkSpeedRecv / ONE_K;
            BigDecimal b = new BigDecimal(mNetworkSpeedRecv);
            mNetworkSpeedRecv = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            mView.setText(mNetworkSpeedRecv + "K/s");
        } else {
            mNetworkSpeedRecv = mNetworkSpeedRecv / ONE_M;
            BigDecimal b = new BigDecimal(mNetworkSpeedRecv);
            mNetworkSpeedRecv = b.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
            mView.setText(mNetworkSpeedRecv + "M/s");
        }
        mRecvBytesOld = recvBytes;
        mHandler.sendEmptyMessageDelayed(UPDATE_TRAFFIC, UPDATE_TIME_MILI_SECONDS);
    }

    public static boolean isSimStateOk(Context context) {
        TelephonyManager telMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);;
        if (telMgr == null) {
            return false;
        }
        final int simsCount = telMgr.getPhoneCount();
        int[] simsSate = new int[simsCount];
        boolean absentOrNotReady = true;
        for (int i = 0; i < simsCount; i++) {
            simsSate[i] = telMgr.getSimState(i);
            absentOrNotReady = absentOrNotReady
                    && (simsSate[i] != TelephonyManager.SIM_STATE_READY);
        }
        boolean isFlyMode = Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        boolean disabled = absentOrNotReady || isFlyMode;
        return !disabled;
    }


}
