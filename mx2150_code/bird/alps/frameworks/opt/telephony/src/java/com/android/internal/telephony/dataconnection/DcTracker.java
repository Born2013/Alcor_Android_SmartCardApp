/*
 * Copyright (C) 2016 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 *
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

package com.android.internal.telephony.dataconnection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.view.WindowManager;
import android.telephony.Rlog;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.dataconnection.DataConnection.ConnectionParams;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.ArrayUtils;

/** M: start */
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IGsmDCTExt;
import com.mediatek.common.telephony.ITelephonyExt;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.dataconnection.DataSubSelector;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;
import com.mediatek.internal.telephony.dataconnection.FdManager;
import com.mediatek.internal.telephony.dataconnection.IaExtendParam;
import com.mediatek.internal.telephony.dataconnection.DataConnectionHelper;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
/** M: end */

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.StringBuilder;

import com.android.internal.telephony.ServiceStateTracker;
/**
 * {@hide}
 */
public class DcTracker extends Handler {
    private static final String LOG_TAG = "DCT";
    private static final boolean DBG = true;
    private static final boolean VDBG = SystemProperties.get("ro.build.type").
            equals("eng") ? true : false; // STOPSHIP if true
    private static final boolean VDBG_STALL = SystemProperties.get("ro.build.type").
            equals("eng") ? true : false; // STOPSHIP if true
    private static final boolean EVDBG = SystemProperties.get("persist.debug.mtklog.dct.evdbg").
            equals("1") ? true : false; // default false
    private static final boolean RADIO_TESTS = false;

    public AtomicBoolean isCleanupRequired = new AtomicBoolean(false);

    private final AlarmManager mAlarmManager;

    private Object mDataEnabledLock = new Object();

    // responds to the setInternalDataEnabled call - used internally to turn off data
    // for example during emergency calls
    private boolean mInternalDataEnabled = true;

    // responds to public (user) API to enable/disable data use
    // independent of mInternalDataEnabled and requests for APN access
    // persisted
    private boolean mUserDataEnabled = true;

    // TODO: move away from static state once 5587429 is fixed.
    private static boolean sPolicyDataEnabled = true;

    /* Currently requested APN type (TODO: This should probably be a parameter not a member) */
    private String mRequestedApnType = PhoneConstants.APN_TYPE_DEFAULT;

    /**
     * After detecting a potential connection problem, this is the max number
     * of subsequent polls before attempting recovery.
     */
    // 1 sec. default polling interval when screen is on.
    private static final int POLL_NETSTAT_MILLIS = 1000;
    // 10 min. default polling interval when screen is off.
    private static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 1000*60*10;
    // Default sent packets without ack which triggers initial recovery steps
    private static final int NUMBER_SENT_PACKETS_OF_HANG = 10;

    // Default for the data stall alarm while non-aggressive stall detection
    private static final int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 1000 * 60 * 6;
    // Default for the data stall alarm for aggressive stall detection
    private static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 1000 * 60;
    // Tag for tracking stale alarms
    private static final String DATA_STALL_ALARM_TAG_EXTRA = "data.stall.alram.tag";

    // M: Check data stall alarm if is needed to skip for testing
    private static final String SKIP_DATA_STALL_ALARM = "persist.skip.data.stall.alarm";

    private static final boolean DATA_STALL_SUSPECTED = true;
    private static final boolean DATA_STALL_NOT_SUSPECTED = false;

    private String RADIO_RESET_PROPERTY = "gsm.radioreset";

    private static final String INTENT_RECONNECT_ALARM =
            "com.android.internal.telephony.data-reconnect";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_REASON =
            "reconnect_alarm_extra_reason";

    private static final String INTENT_DATA_STALL_ALARM =
            "com.android.internal.telephony.data-stall";

    private static final String REDIRECTION_URL_KEY = "redirectionUrl";
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String APN_TYPE_KEY = "apnType";

    @VisibleForTesting
    public static class DataAllowFailReason {
        private HashSet<DataAllowFailReasonType> mDataAllowFailReasonSet = new HashSet<>();

        public void addDataAllowFailReason(DataAllowFailReasonType type) {
            mDataAllowFailReasonSet.add(type);
        }

        public String getDataAllowFailReason() {
            StringBuilder failureReason = new StringBuilder();
            failureReason.append("isDataAllowed: No");
            for(DataAllowFailReasonType reason : mDataAllowFailReasonSet) {
                failureReason.append(reason.mFailReasonStr);
            }
            return failureReason.toString();
        }

        public boolean isFailForSingleReason(DataAllowFailReasonType failReasonType) {
            return (mDataAllowFailReasonSet.size() == 1) &&
                    (mDataAllowFailReasonSet.contains(failReasonType));
        }

        public boolean isFailForReason(DataAllowFailReasonType failReasonType) {
            return mDataAllowFailReasonSet.contains(failReasonType);
        }

        public void clearAllReasons() {
            mDataAllowFailReasonSet.clear();
        }

        public boolean isFailed() {
            return mDataAllowFailReasonSet.size() > 0;
        }

        public int getSizeOfFailReason() {
            return mDataAllowFailReasonSet.size();
        }
    }

    @VisibleForTesting
    public enum DataAllowFailReasonType {
        NOT_ATTACHED(" - Not attached"),
        RECORD_NOT_LOADED(" - SIM not loaded"),
        ROAMING_DISABLED(" - Roaming and data roaming not enabled"),
        INVALID_PHONE_STATE(" - PhoneState is not idle"),
        CONCURRENT_VOICE_DATA_NOT_ALLOWED(" - Concurrent voice and data not allowed"),
        PS_RESTRICTED(" - mIsPsRestricted= true"),
        UNDESIRED_POWER_STATE(" - desiredPowerState= false"),
        INTERNAL_DATA_DISABLED(" - mInternalDataEnabled= false"),
        DEFAULT_DATA_UNSELECTED(" - defaultDataSelected= false"),
        FDN_ENABLED(" - FDN enabled");

        public String mFailReasonStr;

        DataAllowFailReasonType(String reason) {
            mFailReasonStr = reason;
        }
    }

    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcController mDcc;

    /** kept in sync with mApnContexts
     * Higher numbers are higher priority and sorted so highest priority is first */
   /*ALPS01555724: The implementation of PriorityQueue is incorrect, use arraylist to sort priority.
    private final PriorityQueue<ApnContext>mPrioritySortedApnContexts =
            new PriorityQueue<ApnContext>(5,
            new Comparator<ApnContext>() {
                public int compare(ApnContext c1, ApnContext c2) {
                    return c2.priority - c1.priority;
                }
            } );
     */
    ArrayList <ApnContext> mPrioritySortedApnContexts = new ArrayList<ApnContext>();


    /** allApns holds all apns */
    private ArrayList<ApnSetting> mAllApnSettings = null;

    /** preferred apn */
    private ApnSetting mPreferredApn = null;

    /** Is packet service restricted by network */
    private boolean mIsPsRestricted = false;

    /** emergency apn Setting*/
    private ApnSetting mEmergencyApn = null;

    /* Once disposed dont handle any messages */
    private boolean mIsDisposed = false;

    private ContentResolver mResolver;

    /* Set to true with CMD_ENABLE_MOBILE_PROVISIONING */
    private boolean mIsProvisioning = false;

    /* The Url passed as object parameter in CMD_ENABLE_MOBILE_PROVISIONING */
    private String mProvisioningUrl = null;

    /* Intent for the provisioning apn alarm */
    private static final String INTENT_PROVISIONING_APN_ALARM =
            "com.android.internal.telephony.provisioning_apn_alarm";

    /* Tag for tracking stale alarms */
    private static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";

    /* Debug property for overriding the PROVISIONING_APN_ALARM_DELAY_IN_MS */
    private static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";

    /* Default for the provisioning apn alarm timeout */
    private static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 1000 * 60 * 15;

    /* The provision apn alarm intent used to disable the provisioning apn */
    private PendingIntent mProvisioningApnAlarmIntent = null;

    /* Used to track stale provisioning apn alarms */
    private int mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();

    // VOLTE [start]
    private static final boolean MTK_IMS_SUPPORT = SystemProperties.get("persist.mtk_ims_support")
                                                            .equals("1") ? true : false;
    private static final String VOLTE_EMERGENCY_PDN_APN_NAME = "volte.emergency.pdn.name";
    private static final String VOLTE_EMERGENCY_PDN_PROTOCOL = "volte.emergency.pdn.protocol";
    private static final String VOLTE_DEFAULT_EMERGENCY_PDN_APN_NAME = "";
    private static final String VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL = "IPV4V6";  //IP, IPV6, IPV4V6
    private Handler mWorkerHandler;
    // VOLTE [end]

    // VZW feature suppport
    private static final boolean VZW_FEATURE = SystemProperties.get("persist.operator.optr")
                                                            .equals("OP12") ? true : false;

    ///M: ePDG feature support
    private static final boolean EPDG_FEATURE = SystemProperties.get("persist.mtk_epdg_support")
                                                            .equals("1") ? true : false;

    private AsyncChannel mReplyAc = new AsyncChannel();

    /** M: start */
    protected static final String PROPERTY_MOBILE_DATA_ENABLE = "persist.radio.mobile.data";
    protected static final boolean MTK_DUALTALK_SPPORT =
            SystemProperties.getInt("ro.mtk_dt_support", 0) == 1;

    /// M: Default data customization.
    private static final String PROPERTY_OPERATOR = "persist.operator.optr";
    private static final String OPERATOR_OM = "OM";

    protected ApnSetting mInitialAttachApnSetting;
    private static final String NO_SIM_VALUE = "N/A";
    private String[] PROPERTY_ICCID = {
        "ril.iccid.sim1",
        "ril.iccid.sim2",
        "ril.iccid.sim3",
        "ril.iccid.sim4",
    };
    /** M: end */

    // M: OP12 hplmn support
    private static final String PLMN_OP12 = "311480";

    private static final boolean MTK_APNSYNC_TEST_SUPPORT =
            SystemProperties.getInt("persist.apnsync.test.support", 0) == 1;

    protected static boolean MTK_CC33_SUPPORT =
            SystemProperties.getInt("persist.data.cc33.support", 0) == 1 ? true : false;

    private static final boolean MTK_DUAL_APN_SUPPORT =
            SystemProperties.get("ro.mtk_dtag_dual_apn_support").equals("1") ? true : false;

    /**
     * M: IA- for IMS test mode and change attach APN for OP12.
     *    Enable : Set Attach PDN to VZWINTERNET
     *    Disable: Set Attach PDN to VZWIMS (Default)
     */
    protected static final boolean MTK_IMS_TESTMODE_SUPPORT =
            SystemProperties.getInt("persist.imstestmode.support", 0) == 1;

    /* Set to true if IMS pdn handover to WIFI(EPDG) and used for change attach APN */
    private boolean mIsImsHandover = false;

    // M: IA-change attach APN from modem.
    private ApnSetting mMdChangedAttachApn = null;
    private ApnSetting mManualChangedAttachApn = null;
    private static final int APN_CLASS_0 = 0;
    private static final int APN_CLASS_1 = 1;
    private static final int APN_CLASS_2 = 2;
    private static final int APN_CLASS_3 = 3;
    private static final int APN_CLASS_4 = 4;
    private static final int APN_CLASS_5 = 5;
    private static final String VZW_EMERGENCY_NI = "VZWEMERGENCY";
    private static final String VZW_IMS_NI = "VZWIMS";
    private static final String VZW_ADMIN_NI = "VZWADMIN";
    private static final String VZW_INTERNET_NI = "VZWINTERNET";
    private static final String VZW_APP_NI = "VZWAPP";
    private static final String VZW_800_NI = "VZW800";
    private static final String PROP_APN_CLASS_ICCID = "ril.md_changed_apn_class.iccid";
    private static final String PROP_APN_CLASS = "ril.md_changed_apn_class";

    // M: For IMS pdn handover to WIFI
    private static final String NETWORK_TYPE_WIFI = "WIFI";
    private static final String NETWORK_TYPE_MOBILE_IMS = "MOBILEIMS";

    // M: For sync APN table
    private static final String PROPERTY_THROTTLING_TIME = "persist.radio.throttling_time";
    private static final int THROTTLING_TIME_DEFAULT = 900;

    // M: [LTE][Low Power][UL traffic shaping] Start
    private String mLteAccessStratumDataState = PhoneConstants.LTE_ACCESS_STRATUM_STATE_UNKNOWN;
    private static final int LTE_AS_CONNECTED = 1;
    private int mNetworkType = -1;
    private boolean mIsLte = false;
    private boolean mIsSharedDefaultApn = false;
    private int mDefaultRefCount = 0;
    // M: [LTE][Low Power][UL traffic shaping] End

    // M: JPN IA Start
    protected String[] mPlmnStrings;
    protected int mSuspendId = 0;
    protected static final String[] MCC_TABLE_TEST = {
        "001"
    };
    protected static final String[] MCC_TABLE_DOMESTIC = {
        "440"
    };
    protected static final int REGION_UNKNOWN  = 0;
    protected static final int REGION_DOMESTIC = 1;
    protected static final int REGION_FOREIGN  = 2;
    protected int mRegion = REGION_UNKNOWN;
    protected Object mNeedsResumeModemLock = new Object();
    protected boolean mNeedsResumeModem = false;
    // M: JPN IA End

    // M: Attach APN is assigned empty but need to raise P-CSCF discovery flag
    // 26201 DTAG D1(T-Mobile)
    // 44010 DOCOMO
    private String[] PLMN_EMPTY_APN_PCSCF_SET = {
        "26201",
        "44010"
    };

    private String[] MCCMNC_OP18 = {
        "405840", "405854", "405855", "405856", "405857",
        "405858", "405859", "405860", "405861", "405862",
        "405863", "405864", "405865", "405866", "405867",
        "405868", "405869", "405870", "405871", "405872",
        "405873", "405874"
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver () {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (DBG) log("screen on");
                mIsScreenOn = true;
                stopNetStatPoll();
                startNetStatPoll();
                restartDataStallAlarm();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (DBG) log("screen off");
                mIsScreenOn = false;
                stopNetStatPoll();
                startNetStatPoll();
                restartDataStallAlarm();
            } else if (action.startsWith(INTENT_RECONNECT_ALARM)) {
                if (DBG) log("Reconnect alarm. Previous state was " + mState);
                onActionIntentReconnectAlarm(intent);
            } else if (action.equals(INTENT_DATA_STALL_ALARM)) {
                if (DBG) log("Data stall alarm");
                onActionIntentDataStallAlarm(intent);
            } else if (action.equals(INTENT_PROVISIONING_APN_ALARM)) {
                if (DBG) log("Provisioning apn alarm");
                onActionIntentProvisioningApnAlarm(intent);
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                final android.net.NetworkInfo networkInfo = (NetworkInfo)
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mIsWifiConnected = (networkInfo != null && networkInfo.isConnected());
                if (DBG) log("NETWORK_STATE_CHANGED_ACTION: mIsWifiConnected=" + mIsWifiConnected);
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                if (DBG) log("Wifi state changed");
                final boolean enabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                        WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
                if (!enabled) {
                    // when WiFi got disabled, the NETWORK_STATE_CHANGED_ACTION
                    // quit and won't report disconnected until next enabling.
                    mIsWifiConnected = false;
                }
                if (DBG) {
                    log("WIFI_STATE_CHANGED_ACTION: enabled=" + enabled
                            + " mIsWifiConnected=" + mIsWifiConnected);
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                // M:For OP12, in EPDG handover case to change initial attach APN.
                final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                        ConnectivityManager.EXTRA_NETWORK_INFO);
                int apnType = networkInfo.getType();
                String typeName = networkInfo.getTypeName();
                if (VDBG) log("onReceive: ConnectivityService action change apnType = " +
                        apnType + " typename =" + typeName);

                // The case of IMS handover to WIFI
                // Note: EPDG is implemented on CS framework
                if (apnType == ConnectivityManager.TYPE_MOBILE_IMS
                        && typeName.equals(NETWORK_TYPE_WIFI)) {
                    mIsImsHandover = true;
                    log("onReceive: mIsImsHandover = " + mIsImsHandover);
                    setInitialAttachApn();
                } else if (apnType == ConnectivityManager.TYPE_MOBILE_IMS &&
                        typeName.equals(NETWORK_TYPE_MOBILE_IMS)) {
                    mIsImsHandover = false;
                    // Currently, we dont change the attach APN back to avoid re-attach behaviour.
                    // If trigger re-attach, it might resulting lab test fail for unexpected detach.
                    log("onReceive: mIsImsHandover = " + mIsImsHandover);
                    //setInitialAttachApn();
                }
            } else {
                if (DBG) log("onReceive: Unknown action=" + action);
            }
        }
    };

    private final Runnable mPollNetStat = new Runnable() {
        @Override
        public void run() {
            updateDataActivity();

            if (mIsScreenOn) {
                mNetStatPollPeriod = Settings.Global.getInt(mResolver,
                        Settings.Global.PDP_WATCHDOG_POLL_INTERVAL_MS, POLL_NETSTAT_MILLIS);
            } else {
                mNetStatPollPeriod = Settings.Global.getInt(mResolver,
                        Settings.Global.PDP_WATCHDOG_LONG_POLL_INTERVAL_MS,
                        POLL_NETSTAT_SCREEN_OFF_MILLIS);
            }

            if (mNetStatPollEnabled) {
                mDataConnectionTracker.postDelayed(this, mNetStatPollPeriod);
            }
        }
    };

    private SubscriptionManager mSubscriptionManager;
    private final OnSubscriptionsChangedListener mOnSubscriptionsChangedListener =
            new OnSubscriptionsChangedListener() {
                public final AtomicInteger mPreviousSubId =
                        new AtomicInteger(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

                /**
                 * Callback invoked when there is any change to any SubscriptionInfo. Typically
                 * this method invokes {@link SubscriptionManager#getActiveSubscriptionInfoList}
                 */
                @Override
                public void onSubscriptionsChanged() {
                    if (DBG) log("SubscriptionListener.onSubscriptionInfoChanged start");
                    // Set the network type, in case the radio does not restore it.
                    int subId = mPhone.getSubId();
                    if (SubscriptionManager.isValidSubscriptionId(subId)) {
                        registerSettingsObserver();
                        /* check if sim is un-provisioned */
                        applyUnProvisionedSimDetected();
                    }
                    IccRecords r = mIccRecords.get();
                    String operatorNumeric = (r != null) ? r.getOperatorNumeric() : "";
                    if (mPreviousSubId.getAndSet(subId) != subId &&
                            SubscriptionManager.isValidSubscriptionId(subId) &&
                            !TextUtils.isEmpty(operatorNumeric)) {
                        onRecordsLoadedOrSubIdChanged();
                    }
                }
            };

    private static class SettingsObserver extends ContentObserver {
        final private HashMap<Uri, Integer> mUriEventMap;
        final private Context mContext;
        final private Handler mHandler;
        final private static String TAG = "DcTracker.SettingsObserver";

        SettingsObserver(Context context, Handler handler) {
            super(null);
            mUriEventMap = new HashMap<Uri, Integer>();
            mContext = context;
            mHandler = handler;
        }

        void observe(Uri uri, int what) {
            mUriEventMap.put(uri, what);
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(uri, false, this);
        }

        void unobserve() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            Rlog.e(TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            final Integer what = mUriEventMap.get(uri);
            if (what != null) {
                mHandler.obtainMessage(what.intValue()).sendToTarget();
            } else {
                Rlog.e(TAG, "No matching event to send for URI=" + uri);
            }

        }
    }

    private final SettingsObserver mSettingsObserver;

    private void registerSettingsObserver() {
        mSettingsObserver.unobserve();
        String simSuffix = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            simSuffix = Integer.toString(mPhone.getSubId());
        }

        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DATA_ROAMING + simSuffix),
                DctConstants.EVENT_ROAMING_ON);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED),
                DctConstants.EVENT_DEVICE_PROVISIONED_CHANGE);
        mSettingsObserver.observe(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED),
                DctConstants.EVENT_DEVICE_PROVISIONED_CHANGE);

        registerFdnContentObserver();
    }

    /**
     * Maintain the sum of transmit and receive packets.
     *
     * The packet counts are initialized and reset to -1 and
     * remain -1 until they can be updated.
     */
    public static class TxRxSum {
        public long txPkts;
        public long rxPkts;

        public TxRxSum() {
            reset();
        }

        public TxRxSum(long txPkts, long rxPkts) {
            this.txPkts = txPkts;
            this.rxPkts = rxPkts;
        }

        public TxRxSum(TxRxSum sum) {
            txPkts = sum.txPkts;
            rxPkts = sum.rxPkts;
        }

        public void reset() {
            txPkts = -1;
            rxPkts = -1;
        }

        @Override
        public String toString() {
            return "{txSum=" + txPkts + " rxSum=" + rxPkts + "}";
        }

        public void updateTxRxSum() {
            this.txPkts = TrafficStats.getMobileTxPackets();
            this.rxPkts = TrafficStats.getMobileRxPackets();
        }

        public void updateTcpTxRxSum() {
            this.txPkts = TrafficStats.getMobileTcpTxPackets();
            this.rxPkts = TrafficStats.getMobileTcpRxPackets();
        }
    }

    private void onActionIntentReconnectAlarm(Intent intent) {
        String reason = intent.getStringExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON);
        String apnType = intent.getStringExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE);

        int phoneSubId = mPhone.getSubId();
        int currSubId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        log("onActionIntentReconnectAlarm: currSubId = " + currSubId + " phoneSubId=" + phoneSubId);

        // Stop reconnect if not current subId is not correct.
        // FIXME STOPSHIP - phoneSubId is coming up as -1 way after boot and failing this?
        if (!SubscriptionManager.isValidSubscriptionId(currSubId) || (currSubId != phoneSubId)) {
            log("receive ReconnectAlarm but subId incorrect, ignore");
            return;
        }

        ApnContext apnContext = mApnContexts.get(apnType);

        if (DBG) {
            log("onActionIntentReconnectAlarm: mState=" + mState + " reason=" + reason +
                    " apnType=" + apnType + " apnContext=" + apnContext +
                    " mDataConnectionAsyncChannels=" + mDataConnectionAcHashMap);
        }

        if ((apnContext != null) && (apnContext.isEnabled())) {
            apnContext.setReason(reason);
            DctConstants.State apnContextState = apnContext.getState();
            if (DBG) {
                log("onActionIntentReconnectAlarm: apnContext state=" + apnContextState);
            }
            if ((apnContextState == DctConstants.State.FAILED)
                    || (apnContextState == DctConstants.State.IDLE)) {
                if (DBG) {
                    log("onActionIntentReconnectAlarm: state is FAILED|IDLE, disassociate");
                }
                DcAsyncChannel dcac = apnContext.getDcAc();
                if (dcac != null) {
                    if (DBG) {
                        log("onActionIntentReconnectAlarm: tearDown apnContext=" + apnContext);
                    }
                    dcac.tearDown(apnContext, "", null);
                }
                apnContext.setDataConnectionAc(null);
                apnContext.setState(DctConstants.State.IDLE);
            } else {
                if (DBG) log("onActionIntentReconnectAlarm: keep associated");
            }
            // TODO: IF already associated should we send the EVENT_TRY_SETUP_DATA???
            sendMessage(obtainMessage(DctConstants.EVENT_TRY_SETUP_DATA, apnContext));

            apnContext.setReconnectIntent(null);
        }
    }

    private void onActionIntentDataStallAlarm(Intent intent) {
        if (VDBG_STALL) log("onActionIntentDataStallAlarm: action=" + intent.getAction());
        Message msg = obtainMessage(DctConstants.EVENT_DATA_STALL_ALARM,
                intent.getAction());
        msg.arg1 = intent.getIntExtra(DATA_STALL_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private final ConnectivityManager mCm;

    /**
     * List of messages that are waiting to be posted, when data call disconnect
     * is complete
     */
    private ArrayList<Message> mDisconnectAllCompleteMsgList = new ArrayList<Message>();

    private RegistrantList mAllDataDisconnectedRegistrants = new RegistrantList();

    // member variables
    private final Phone mPhone;
    private final UiccController mUiccController;
    private final AtomicReference<IccRecords> mIccRecords = new AtomicReference<IccRecords>();
    protected AtomicReference<UiccCardApplication> mUiccCardApplication
            = new AtomicReference<UiccCardApplication>();
    private DctConstants.Activity mActivity = DctConstants.Activity.NONE;
    private DctConstants.State mState = DctConstants.State.IDLE;
    private final Handler mDataConnectionTracker;

    private long mTxPkts;
    private long mRxPkts;
    private int mNetStatPollPeriod;
    private boolean mNetStatPollEnabled = false;

    private TxRxSum mDataStallTxRxSum = new TxRxSum(0, 0);
    // Used to track stale data stall alarms.
    private int mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
    // The current data stall alarm intent
    private PendingIntent mDataStallAlarmIntent = null;
    // Number of packets sent since the last received packet
    private long mSentSinceLastRecv;
    // Controls when a simple recovery attempt it to be tried
    private int mNoRecvPollCount = 0;
    // Reference counter for enabling fail fast
    private static int sEnableFailFastRefCounter = 0;
    // True if data stall detection is enabled
    private volatile boolean mDataStallDetectionEnabled = true;

    private volatile boolean mFailFast = false;

    // True when in voice call
    private boolean mInVoiceCall = false;

    // wifi connection status will be updated by sticky intent
    private boolean mIsWifiConnected = false;

    /** Intent sent when the reconnect alarm fires. */
    private PendingIntent mReconnectIntent = null;

    // When false we will not auto attach and manually attaching is required.
    private boolean mAutoAttachOnCreationConfig = false;
    private AtomicBoolean mAutoAttachOnCreation = new AtomicBoolean(false);

    // State of screen
    // (TODO: Reconsider tying directly to screen, maybe this is
    //        really a lower power mode")
    private boolean mIsScreenOn = true;

    // Indicates if we found mvno-specific APNs in the full APN list.
    // used to determine if we can accept mno-specific APN for tethering.
    private boolean mMvnoMatched = false;

    /** Allows the generation of unique Id's for DataConnection objects */
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(0);

    /** The data connections. */
    private HashMap<Integer, DataConnection> mDataConnections =
            new HashMap<Integer, DataConnection>();

    /** The data connection async channels */
    private HashMap<Integer, DcAsyncChannel> mDataConnectionAcHashMap =
            new HashMap<Integer, DcAsyncChannel>();

    /** Convert an ApnType string to Id (TODO: Use "enumeration" instead of String for ApnType) */
    private HashMap<String, Integer> mApnToDataConnectionId = new HashMap<String, Integer>();

    /** Phone.APN_TYPE_* ===> ApnContext */
    private final ConcurrentHashMap<String, ApnContext> mApnContexts =
            new ConcurrentHashMap<String, ApnContext>();

    private final SparseArray<ApnContext> mApnContextsById = new SparseArray<ApnContext>();

    private int mDisconnectPendingCount = 0;

    /** mRedirectUrl is set when we got the validation failure with the redirection URL
     * based on which we start the Carrier App to check the sim state */
    private String mRedirectUrl = null;

    /** mColdSimDetected is set to true when we received SubInfoChanged &&
     * SubscriptionInfo.simProvisioningStatus equals to SIM_UNPROVISIONED_COLD */
    private boolean mColdSimDetected = false;

    /** mmOutOfCreditSimDetected is set to true when we received SubInfoChanged &&
     * SubscriptionInfo.simProvisioningStatus equals to SIM_UNPROVISIONED_OUT_OF_CREDIT */
    private boolean mOutOfCreditSimDetected = false;

    /** HashSet of ApnContext associated with redirected data-connection.
     * those apn contexts tear down upon redirection and re-establish upon non-cold sim detection */
    private HashSet<ApnContext> redirectApnContextSet = new HashSet<>();

    /**
     * Handles changes to the APN db.
     */
    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver () {
            super(mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            removeMessages(DctConstants.EVENT_APN_CHANGED);
            // M: De-bound the onApnChanged in threads trigger in the same time
            sendMessageDelayed(obtainMessage(DctConstants.EVENT_APN_CHANGED), APN_CHANGE_MILLIS);
            //sendMessage(obtainMessage(DctConstants.EVENT_APN_CHANGED));
        }
    }

    // M: JPN IA Start
    /**
    * Handles changes to the settings of IMS switch db.
    */
    private ContentObserver mImsSwitchChangeObserver  = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (DBG) {
                log("mImsSwitchChangeObserver: onChange=" + selfChange);
            }
            if (isOp17IaSupport()) {
                log("IA : OP17, set IA");
                setInitialAttachApn();
            }
        }
    };
    // M: JPN IA End

    //***** Instance Variables

    private boolean mReregisterOnReconnectFailure = false;


    //***** Constants

    // Used by puppetmaster/*/radio_stress.py
    private static final String PUPPET_MASTER_RADIO_STRESS_TEST = "gsm.defaultpdpcontext.active";

    private static final int POLL_PDP_MILLIS = 5 * 1000;
    private static final int APN_CHANGE_MILLIS = 1 * 1000;

    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120 * 1000;

    static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID =
                        Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
    static final String APN_ID = "apn_id";

    private boolean mCanSetPreferApn = false;

    private AtomicBoolean mAttached = new AtomicBoolean(false);

    /** Watches for changes to the APN db. */
    private ApnChangeObserver mApnObserver;

    private final String mProvisionActionName;
    private BroadcastReceiver mProvisionBroadcastReceiver;
    private ProgressDialog mProvisioningSpinner;

    public boolean mImsRegistrationState = false;

    // M: Data connection fail cause manager
    protected DcFailCauseManager mDcFcMgr;

    // M: Fast Dormancy
    protected FdManager mFdMgr;

    // M: For Plug in
    private static final boolean BSP_PACKAGE =
            SystemProperties.getBoolean("ro.mtk_bsp_package", false);
    private IGsmDCTExt mGsmDctExt;
    private ITelephonyExt mTelephonyExt;

    // M: Vsim
    private static final String PROPERTY_VSIM_ENABLE =
            TelephonyProperties.PROPERTY_EXTERNAL_SIM_INSERTED;

    private static final String PROPERTY_FORCE_APN_CHANGE = "ril.force_apn_change";

    // M: start of throttling APN
    private static final boolean THROTTLING_APN_ENABLED =
            SystemProperties.get("persist.mtk_volte_support").equals("1");
    private static final String PROPERTY_THROTTLING_APN_ENABLED = "ril.throttling.enabled";
    private static final String HIGH_THROUGHPUT_APN[] = {
        PhoneConstants.APN_TYPE_ALL,
        PhoneConstants.APN_TYPE_DEFAULT,
        PhoneConstants.APN_TYPE_DUN,
        PhoneConstants.APN_TYPE_HIPRI,
        PhoneConstants.APN_TYPE_TETHERING
    };

    // VOLTE
    private static final String IMS_APN[] = {
        PhoneConstants.APN_TYPE_IMS,
        PhoneConstants.APN_TYPE_EMERGENCY,
    };

    private static final int PDP_CONNECTION_POOL_SIZE = 3;
    private static final int THROTTLING_MAX_PDP_SIZE = 8;

    private static final int MIN_ID_HIGH_TROUGHPUT = 0;
    private static final int MAX_ID_HIGH_TROUGHPUT = 1;
    private static final int MIN_ID_OTHERS_TROUGHPUT = 2;
    private static final int MAX_ID_OTHERS_TROUGHPUT = 3;
    private static final int MIN_ID_IMS_TROUGHPUT = 4;
    private static final int MAX_ID_IMS_TROUGHPUT = 6;

    private AtomicInteger mHighThroughputIdGenerator = new AtomicInteger(0);
    private AtomicInteger mOthersUniqueIdGenerator = new AtomicInteger(2);
    private AtomicInteger mImsUniqueIdGenerator = new AtomicInteger(4);
    // M: end of throttling APN

    // M: for prevent onApnChanged and onRecordLoaded happened at the same time
    private Integer mCreateApnLock = new Integer(0);

    // M: Google issue, this thread should quit when DcTracker dispose,
    //    otherwise memory leak will happen.
    private HandlerThread mDcHandlerThread;

    //***** Constructor
    public DcTracker(Phone phone) {
        super();
        mPhone = phone;

        if (DBG) log("DCT.constructor");

        mResolver = mPhone.getContext().getContentResolver();
        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, DctConstants.EVENT_ICC_CHANGED, null);
        mAlarmManager =
                (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        mCm = (ConnectivityManager) mPhone.getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(INTENT_DATA_STALL_ALARM);
        filter.addAction(INTENT_PROVISIONING_APN_ALARM);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // TODO - redundent with update call below?
        mUserDataEnabled = getDataEnabled();

        notifyMobileDataChange(mUserDataEnabled ? 1 : 0);

        mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        mAutoAttachOnCreation.set(sp.getBoolean(Phone.DATA_DISABLED_ON_BOOT_KEY, false));

        mSubscriptionManager = SubscriptionManager.from(mPhone.getContext());
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);

        mDcHandlerThread = new HandlerThread("DcHandlerThread");
        mDcHandlerThread.start();
        Handler dcHandler = new Handler(mDcHandlerThread.getLooper());
        mDcc = DcController.makeDcc(mPhone, this, dcHandler);
        mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(mPhone, dcHandler);

        if (DBG) log("DualApnSupport = " + MTK_DUAL_APN_SUPPORT);

        mDataConnectionTracker = this;
        registerForAllEvents();
        update();

        /** M: create worker handler to handle DB/IO access */
        createWorkerHandler();

        mApnObserver = new ApnChangeObserver();
        phone.getContext().getContentResolver().registerContentObserver(
                Telephony.Carriers.CONTENT_URI, true, mApnObserver);

        phone.getContext().getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ENHANCED_4G_MODE_ENABLED), true,
                mImsSwitchChangeObserver);

        initApnContexts();

        for (ApnContext apnContext : mApnContexts.values()) {
            // Register the reconnect and restart actions.
            filter = new IntentFilter();
            filter.addAction(INTENT_RECONNECT_ALARM + '.' + apnContext.getApnType());
            mPhone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone);
        }

        // M: Initialize data connection fail cause manager
        mDcFcMgr = DcFailCauseManager.getInstance(mPhone);

        // Add Emergency APN to APN setting list by default to support EPDN in sim absent cases
        initEmergencyApnSetting();
        addEmergencyApnSetting();

        // M: Fast Dormancy init
        mFdMgr = FdManager.getInstance(phone);

        //MTK START: Add Plug in
        if (!BSP_PACKAGE) {
            try {
                mGsmDctExt =
                    MPlugin.createInstance(IGsmDCTExt.class.getName(), mPhone.getContext());

                mTelephonyExt =
                    MPlugin.createInstance(ITelephonyExt.class.getName(), mPhone.getContext());
                mTelephonyExt.init(mPhone.getContext());
                mTelephonyExt.startDataRoamingStrategy(mPhone);
            } catch (Exception e) {
                if (DBG) {
                    log("mGsmDctExt or mTelephonyExt init fail");
                }
                e.printStackTrace();
            }
        }
        //MTK END

        mProvisionActionName = "com.android.internal.telephony.PROVISION" + phone.getPhoneId();

        mSettingsObserver = new SettingsObserver(mPhone.getContext(), this);
        registerSettingsObserver();
    }

    @VisibleForTesting
    public DcTracker() {
        mAlarmManager = null;
        mCm = null;
        mPhone = null;
        mUiccController = null;
        mDataConnectionTracker = null;
        mProvisionActionName = null;
        mSettingsObserver = new SettingsObserver(null, this);
    }

    public void registerServiceStateTrackerEvents() {
        mPhone.getServiceStateTracker().registerForDataConnectionAttached(this,
                DctConstants.EVENT_DATA_CONNECTION_ATTACHED, null);
        mPhone.getServiceStateTracker().registerForDataConnectionDetached(this,
                DctConstants.EVENT_DATA_CONNECTION_DETACHED, null);
        mPhone.getServiceStateTracker().registerForDataRoamingOn(this,
                DctConstants.EVENT_ROAMING_ON, null);
        mPhone.getServiceStateTracker().registerForDataRoamingOff(this,
                DctConstants.EVENT_ROAMING_OFF, null);
        mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this,
                DctConstants.EVENT_PS_RESTRICT_ENABLED, null);
        mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this,
                DctConstants.EVENT_PS_RESTRICT_DISABLED, null);
        mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this,
                DctConstants.EVENT_DATA_RAT_CHANGED, null);
    }

    public void unregisterServiceStateTrackerEvents() {
        mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        mPhone.getServiceStateTracker().unregisterForDataRoamingOn(this);
        mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
        mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(this);
    }

    private void registerForAllEvents() {
        if (DBG) {
            log("registerForAllEvents: mPhone = " + mPhone);
        }
        mPhone.mCi.registerForAvailable(this, DctConstants.EVENT_RADIO_AVAILABLE, null);
        mPhone.mCi.registerForOffOrNotAvailable(this,
                DctConstants.EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        mPhone.mCi.registerForDataNetworkStateChanged(this,
                DctConstants.EVENT_DATA_STATE_CHANGED, null);
        // Note, this is fragile - the Phone is now presenting a merged picture
        // of PS (volte) & CS and by diving into its internals you're just seeing
        // the CS data.  This works well for the purposes this is currently used for
        // but that may not always be the case.  Should probably be redesigned to
        // accurately reflect what we're really interested in (registerForCSVoiceCallEnded).

        // M: Remove below section for the reason that new PS/CS design has been applied.
        //mPhone.getCallTracker().registerForVoiceCallEnded(this,
        //        DctConstants.EVENT_VOICE_CALL_ENDED, null);
        //mPhone.getCallTracker().registerForVoiceCallStarted(this,
        //        DctConstants.EVENT_VOICE_CALL_STARTED, null);
        // M: End
        registerServiceStateTrackerEvents();
     //   SubscriptionManager.registerForDdsSwitch(this,
     //          DctConstants.EVENT_CLEAN_UP_ALL_CONNECTIONS, null);

        // M: cc33
        mPhone.mCi.registerForRemoveRestrictEutran(this, DctConstants.EVENT_REMOVE_RESTRICT_EUTRAN
                ,null);

        // M: JPN IA
        // FIXME: Better refactoring to use OP plugin instead of wrapper optr.
        if (!WorldPhoneUtil.isWorldPhoneSupport()) {
            mPhone.mCi.setOnPlmnChangeNotification(this, DctConstants.EVENT_REG_PLMN_CHANGED, null);
            mPhone.mCi.setOnRegistrationSuspended(this, DctConstants.EVENT_REG_SUSPENDED, null);
        }
        // M: JPN IA End

        //M: Reset Attach Apn
        mPhone.mCi.registerForResetAttachApn(this, DctConstants.EVENT_RESET_ATTACH_APN, null);

        // M: IA-change attach APN
        mPhone.mCi.registerForAttachApnChanged(this, DctConstants.EVENT_ATTACH_APN_CHANGED, null);

        mPhone.mCi.registerForPcoStatus(this, DctConstants.EVENT_PCO_STATUS, null);

        // M: [LTE][Low Power][UL traffic shaping]
        // TODO: Should this move to NW frameworks to handle?
        mPhone.mCi.registerForLteAccessStratumState(this,
                DctConstants.EVENT_LTE_ACCESS_STRATUM_STATE, null);
    }

    public void dispose() {
        if (DBG) log("DCT.dispose");

        /// M: To stop data customization strategy @{
        if (mTelephonyExt != null) {
            mTelephonyExt.stopDataRoamingStrategy();
        }

        if (mProvisionBroadcastReceiver != null) {
            mPhone.getContext().unregisterReceiver(mProvisionBroadcastReceiver);
            mProvisionBroadcastReceiver = null;
        }
        if (mProvisioningSpinner != null) {
            mProvisioningSpinner.dismiss();
            mProvisioningSpinner = null;
        }

        /// M: When IMS/EIMS pdn is connecting, after dispose, dc stateMachine will quit,
        /// there is no way to notify DataDispatcher the result, add below logic to avoid this. @{
        for (ApnContext apnContext : mApnContexts.values()) {
            if ((PhoneConstants.APN_TYPE_IMS.equals(apnContext.getApnType()) ||
                 PhoneConstants.APN_TYPE_EMERGENCY.equals(apnContext.getApnType()))
                    && (apnContext.getState() == DctConstants.State.CONNECTING ||
                        apnContext.getState() == DctConstants.State.RETRYING)) {
                if (DBG) log("notify dc fail for " + apnContext.getApnType());
                ApnSetting apn = apnContext.getApnSetting();
                mPhone.notifyPreciseDataConnectionFailed(null,
                        apnContext.getApnType(), apn != null ? apn.apn : "unknown", "");
            }
        }
        /// @}
        cleanUpAllConnections(true, null);

        for (DcAsyncChannel dcac : mDataConnectionAcHashMap.values()) {
            dcac.disconnect();
        }
        mDataConnectionAcHashMap.clear();
        mIsDisposed = true;
        mPhone.getContext().unregisterReceiver(mIntentReceiver);
        mUiccController.unregisterForIccChanged(this);
        mSettingsObserver.unobserve();

        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
        mDcc.dispose();
        mDcTesterFailBringUpAll.dispose();

        mPhone.getContext().getContentResolver().unregisterContentObserver(mApnObserver);

        mPhone.getContext().getContentResolver().unregisterContentObserver(
                mImsSwitchChangeObserver);

        unregisterForAllEvents();

        mApnContexts.clear();
        mApnContextsById.clear();
        mPrioritySortedApnContexts.clear();
        unregisterForAllEvents();

        destroyDataConnections();

        /** M: exit worker thread */
        if (mWorkerHandler != null) {
            Looper looper = mWorkerHandler.getLooper();
            looper.quit();
        }

        if (mDcHandlerThread != null) {
            mDcHandlerThread.quit();
            mDcHandlerThread = null;
        }

        // M: dispose data connection fail cause manager
        mDcFcMgr.dispose();

        // M: Fdn flags reset
        mIsFdnChecked = false;
        mIsMatchFdnForAllowData = false;
        mIsPhbStateChangedIntentRegistered = false;
        mPhone.getContext().unregisterReceiver(mPhbStateChangedIntentReceiver);
    }

    private void unregisterForAllEvents() {
        if (DBG) log("unregisterForAllEvents: mPhone = " + mPhone);
         //Unregister for all events
        mPhone.mCi.unregisterForAvailable(this);
        mPhone.mCi.unregisterForOffOrNotAvailable(this);
        IccRecords r = mIccRecords.get();
        if (r != null) {
            r.unregisterForRecordsLoaded(this);
            mIccRecords.set(null);
        }
        mPhone.mCi.unregisterForDataNetworkStateChanged(this);
        // M: Remove below section for the reason that new PS/CS design has been applied.
        //mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
        //mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        // M: End
        unregisterServiceStateTrackerEvents();
        //SubscriptionManager.unregisterForDdsSwitch(this);
        // M: cc33
        mPhone.mCi.unregisterForRemoveRestrictEutran(this);


        // M: JPN IA Start
        if (!WorldPhoneUtil.isWorldPhoneSupport()) {
            mPhone.mCi.unSetOnPlmnChangeNotification(this);
            mPhone.mCi.unSetOnRegistrationSuspended(this);
        }
        // M: JPN IA End

        // M: Reset Attach Apn
        mPhone.mCi.unregisterForResetAttachApn(this);

        // M: IA-change attach APN from modem.
        mPhone.mCi.unregisterForAttachApnChanged(this);

        mPhone.mCi.unregisterForPcoStatus(this);

        // M: [LTE][Low Power][UL traffic shaping]
        // TODO: Should this move to NW frameworks to handle?
        mPhone.mCi.unregisterForLteAccessStratumState(this);
    }

    /**
     * Called when EVENT_RESET_DONE is received so goto
     * IDLE state and send notifications to those interested.
     *
     * TODO - currently unused.  Needs to be hooked into DataConnection cleanup
     * TODO - needs to pass some notion of which connection is reset..
     */
    private void onResetDone(AsyncResult ar) {
        if (DBG) log("EVENT_RESET_DONE");
        String reason = null;
        if (ar.userObj instanceof String) {
            reason = (String) ar.userObj;
        }
        gotoIdleAndNotifyDataConnection(reason);
    }

    /**
     * Modify {@link android.provider.Settings.Global#MOBILE_DATA} value.
     */
    public void setDataEnabled(boolean enable) {
        Message msg = obtainMessage(DctConstants.CMD_SET_USER_DATA_ENABLE);
        msg.arg1 = enable ? 1 : 0;
        if (DBG) log("setDataEnabled: sendMessage: enable=" + enable);
        sendMessage(msg);

        // M: cc33 notify modem the data on/off state
        if (MTK_CC33_SUPPORT) {
            mPhone.mCi.setDataOnToMD(enable, null);
        }
    }

    private void onSetUserDataEnabled(boolean enabled) {
        synchronized (mDataEnabledLock) {
            if (mUserDataEnabled != enabled) {
                mUserDataEnabled = enabled;

                // For single SIM phones, this is a per phone property.
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    Settings.Global.putInt(mResolver, Settings.Global.MOBILE_DATA, enabled ? 1 : 0);
                } else {
                    int phoneSubId = mPhone.getSubId();
                    Settings.Global.putInt(mResolver, Settings.Global.MOBILE_DATA + phoneSubId,
                            enabled ? 1 : 0);
                }

                // M:
                setUserDataProperty(enabled);
                notifyMobileDataChange(enabled ? 1 : 0);
                syncDataSettingsToMd(enabled, getDataOnRoamingEnabled());
                // M: }@

                if (getDataOnRoamingEnabled() == false &&
                        mPhone.getServiceState().getDataRoaming() == true) {
                    if (enabled) {
                        notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
                    } else {
                        notifyOffApnsOfAvailability(Phone.REASON_DATA_DISABLED);
                    }
                }

                if (enabled) {
                    onTrySetupData(Phone.REASON_DATA_ENABLED);
                } else {
                    if (BSP_PACKAGE) {
                        onCleanUpAllConnections(Phone.REASON_DATA_SPECIFIC_DISABLED);
                    } else {
                         for (ApnContext apnContext : mApnContexts.values()) {
                            if (!isDataAllowedAsOff(apnContext.getApnType())) {
                                apnContext.setReason(Phone.REASON_DATA_SPECIFIC_DISABLED);
                                onCleanUpConnection(true
                                        , apnContext.apnIdForApnName(apnContext.getApnType())
                                        , Phone.REASON_DATA_SPECIFIC_DISABLED);
                            }
                        }
                    }
                }
            }
        }
    }

    private void onDeviceProvisionedChange() {
        if (getDataEnabled()) {
            mUserDataEnabled = true;
            onTrySetupData(Phone.REASON_DATA_ENABLED);
        } else {
            mUserDataEnabled = false;
            onCleanUpAllConnections(Phone.REASON_DATA_SPECIFIC_DISABLED);
        }
    }


    public long getSubId() {
        return mPhone.getSubId();
    }

    public DctConstants.Activity getActivity() {
        return mActivity;
    }

    private void setActivity(DctConstants.Activity activity) {
        log("setActivity = " + activity);
        mActivity = activity;
        mPhone.notifyDataActivity();
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog log) {
        final int apnId = ApnContext.apnIdForNetworkRequest(networkRequest);
        final ApnContext apnContext = mApnContextsById.get(apnId);
        log.log("DcTracker.requestNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) apnContext.incRefCount(log);
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog log) {
        final int apnId = ApnContext.apnIdForNetworkRequest(networkRequest);
        final ApnContext apnContext = mApnContextsById.get(apnId);
        log.log("DcTracker.releaseNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) apnContext.decRefCount(log);
    }

    public boolean isApnSupported(String name) {
        if (name == null) {
            loge("isApnSupported: name=null");
            return false;
        }
        ApnContext apnContext = mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
            return false;
        }
        return true;
    }

    /**
     * Called when there is any change to any SubscriptionInfo Typically
     * this method invokes {@link SubscriptionManager#getActiveSubscriptionInfoList}
     */
    private boolean isColdSimDetected() {
        int subId = mPhone.getSubId();
        if(SubscriptionManager.isValidSubscriptionId(subId)) {
            final SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (subInfo != null) {
                final int simProvisioningStatus = subInfo.getSimProvisioningStatus();
                if (simProvisioningStatus == SubscriptionManager.SIM_UNPROVISIONED_COLD) {
                    log("Cold Sim Detected on SubId: " + subId);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when there is any change to any SubscriptionInfo Typically
     * this method invokes {@link SubscriptionManager#getActiveSubscriptionInfoList}
     */
    private boolean isOutOfCreditSimDetected() {
        int subId = mPhone.getSubId();
        if(SubscriptionManager.isValidSubscriptionId(subId)) {
            final SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(subId);
            if (subInfo != null) {
                final int simProvisioningStatus = subInfo.getSimProvisioningStatus();
                if (simProvisioningStatus == SubscriptionManager.SIM_UNPROVISIONED_OUT_OF_CREDIT) {
                    log("Out Of Credit Sim Detected on SubId: " + subId);
                    return true;
                }
            }
        }
        return false;
    }

    public int getApnPriority(String name) {
        ApnContext apnContext = mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
        }
        return apnContext.priority;
    }

    // Turn telephony radio on or off.
    private void setRadio(boolean on) {
        final ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        try {
            phone.setRadio(on);
        } catch (Exception e) {
            // Ignore.
        }
    }

    // Class to handle Intent dispatched with user selects the "Sign-in to network"
    // notification.
    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        // Mobile provisioning URL.  Valid while provisioning notification is up.
        // Set prior to notification being posted as URL contains ICCID which
        // disappears when radio is off (which is the case when notification is up).
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String provisionUrl, String networkOperator) {
            mNetworkOperator = networkOperator;
            mProvisionUrl = provisionUrl;
        }

        private void setEnableFailFastMobileData(int enabled) {
            sendMessage(obtainMessage(DctConstants.CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA, enabled, 0));
        }

        private void enableMobileProvisioning() {
            final Message msg = obtainMessage(DctConstants.CMD_ENABLE_MOBILE_PROVISIONING);
            msg.setData(Bundle.forPair(DctConstants.PROVISIONING_URL_KEY, mProvisionUrl));
            sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Turning back on the radio can take time on the order of a minute, so show user a
            // spinner so they know something is going on.
            mProvisioningSpinner = new ProgressDialog(context);
            mProvisioningSpinner.setTitle(mNetworkOperator);
            mProvisioningSpinner.setMessage(
                    // TODO: Don't borrow "Connecting..." i18n string; give Telephony a version.
                    context.getText(com.android.internal.R.string.media_route_status_connecting));
            mProvisioningSpinner.setIndeterminate(true);
            mProvisioningSpinner.setCancelable(true);
            // Allow non-Activity Service Context to create a View.
            mProvisioningSpinner.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            mProvisioningSpinner.show();
            // After timeout, hide spinner so user can at least use their device.
            // TODO: Indicate to user that it is taking an unusually long time to connect?
            sendMessageDelayed(obtainMessage(DctConstants.CMD_CLEAR_PROVISIONING_SPINNER,
                    mProvisioningSpinner), PROVISIONING_SPINNER_TIMEOUT_MILLIS);
            // This code is almost identical to the old
            // ConnectivityService.handleMobileProvisioningAction code.
            setRadio(true);
            setEnableFailFastMobileData(DctConstants.ENABLED);
            enableMobileProvisioning();
        }
    }

    public boolean isDataPossible(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        boolean apnContextIsEnabled = apnContext.isEnabled();
        DctConstants.State apnContextState = apnContext.getState();
        boolean apnTypePossible = !(apnContextIsEnabled &&
                (apnContextState == DctConstants.State.FAILED));
        boolean isEmergencyApn = apnContext.getApnType().equals(PhoneConstants.APN_TYPE_EMERGENCY);
        // Set the emergency APN availability status as TRUE irrespective of conditions checked in
        // isDataAllowed() like IN_SERVICE, MOBILE DATA status etc.
        boolean dataAllowed = isEmergencyApn || isDataAllowed(null);
        boolean possible = dataAllowed && apnTypePossible;

        if ((apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)
                    || apnContext.getApnType().equals(PhoneConstants.APN_TYPE_IA))
                && (mPhone.getServiceState().getRilDataRadioTechnology()
                == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)) {
            log("Default data call activation not possible in iwlan.");
            possible = false;
        }

        if (VDBG) {
            log(String.format("isDataPossible(%s): possible=%b isDataAllowed=%b " +
                            "apnTypePossible=%b apnContextisEnabled=%b apnContextState()=%s",
                    apnType, possible, dataAllowed, apnTypePossible,
                    apnContextIsEnabled, apnContextState));
        }
        return possible;
    }

    @Override
    protected void finalize() {
        if(DBG) log("finalize");
    }

    private ApnContext addApnContext(String type, NetworkConfig networkConfig) {
        ApnContext apnContext = new ApnContext(mPhone, type, LOG_TAG, networkConfig, this);
        mApnContexts.put(type, apnContext);
        mApnContextsById.put(ApnContext.apnIdForApnName(type), apnContext);
        mPrioritySortedApnContexts.add(apnContext);
        return apnContext;
    }

    private void initApnContexts() {
        log("initApnContexts: E");
        // Load device network attributes from resources
        String[] networkConfigStrings = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.networkAttributes);
        for (String networkConfigString : networkConfigStrings) {
            NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
            ApnContext apnContext = null;

            switch (networkConfig.type) {
            case ConnectivityManager.TYPE_MOBILE:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DEFAULT, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_MMS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_MMS, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_SUPL, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_DUN:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DUN, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_HIPRI, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_FOTA:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_FOTA, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_IMS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_IMS, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_CBS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_CBS, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_IA:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_IA, networkConfig);
                break;
            /** M: start */
            case ConnectivityManager.TYPE_MOBILE_DM:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_DM, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_NET:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_NET, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_WAP:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_WAP, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_CMMAIL:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_CMMAIL, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_RCSE:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_RCSE, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_XCAP:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_XCAP, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_RCS:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_RCS, networkConfig);
                break;
            case ConnectivityManager.TYPE_MOBILE_BIP:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_BIP, networkConfig);
                break;
            /** M: end*/
            case ConnectivityManager.TYPE_MOBILE_EMERGENCY:
                apnContext = addApnContext(PhoneConstants.APN_TYPE_EMERGENCY, networkConfig);
                break;
            default:
                log("initApnContexts: skipping unknown type=" + networkConfig.type);
                continue;
            }
            log("initApnContexts: apnContext=" + apnContext);
        }

        //The implement of priorityQueue class is incorrect, we sort the list by ourself
        Collections.sort(mPrioritySortedApnContexts, new Comparator<ApnContext>() {
            public int compare(ApnContext c1, ApnContext c2) {
                return c2.priority - c1.priority;
            }
        });
        if (VDBG) log("initApnContexts: mPrioritySortedApnContexts=" + mPrioritySortedApnContexts);
        if (VDBG) log("initApnContexts: X mApnContexts=" + mApnContexts);
    }

    public LinkProperties getLinkProperties(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac != null) {
                if (DBG) log("return link properites for " + apnType);
                return dcac.getLinkPropertiesSync();
            }
        }
        if (DBG) log("return new LinkProperties");
        return new LinkProperties();
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext!=null) {
            DcAsyncChannel dataConnectionAc = apnContext.getDcAc();
            if (dataConnectionAc != null) {
                if (DBG) {
                    log("get active pdp is not null, return NetworkCapabilities for " + apnType);
                }
                return dataConnectionAc.getNetworkCapabilitiesSync();
            }
        }
        if (DBG) log("return new NetworkCapabilities");
        return new NetworkCapabilities();
    }

    // Return all active apn types
    public String[] getActiveApnTypes() {
        if (DBG) log("get all active apn types");
        ArrayList<String> result = new ArrayList<String>();

        for (ApnContext apnContext : mApnContexts.values()) {
            if (mAttached.get() && apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }

        return result.toArray(new String[0]);
    }

    // Return active apn of specific apn type
    public String getActiveApnString(String apnType) {
        if (VDBG && EVDBG) log( "get active apn string for type:" + apnType);
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            ApnSetting apnSetting = apnContext.getApnSetting();
            if (apnSetting != null) {
                return apnSetting.apn;
            }
        }
        return null;
    }

    // Return state of specific apn type
    public DctConstants.State getState(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.getState();
        }
        return DctConstants.State.FAILED;
    }

    // Return if apn type is a provisioning apn.
    private boolean isProvisioningApn(String apnType) {
        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    // Return state of overall
    public DctConstants.State getOverallState() {
        boolean isConnecting = false;
        boolean isFailed = true; // All enabled Apns should be FAILED.
        boolean isAnyEnabled = false;

        //M: For debug, dump overall state.
        StringBuilder builder = new StringBuilder();
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext != null) {
                builder.append(apnContext.toString() + ", ");
            }
        }
        if (VDBG) log("overall state is " + builder);

        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.isEnabled()) {
                isAnyEnabled = true;
                switch (apnContext.getState()) {
                case CONNECTED:
                case DISCONNECTING:
                    if (VDBG) log("overall state is CONNECTED");
                    return DctConstants.State.CONNECTED;
                case RETRYING:
                case CONNECTING:
                    isConnecting = true;
                    isFailed = false;
                    break;
                case IDLE:
                case SCANNING:
                    isFailed = false;
                    break;
                default:
                    isAnyEnabled = true;
                    break;
                }
            }
        }

        if (!isAnyEnabled) { // Nothing enabled. return IDLE.
            if (VDBG) log( "overall state is IDLE");
            return DctConstants.State.IDLE;
        }

        if (isConnecting) {
            if (VDBG) log( "overall state is CONNECTING");
            return DctConstants.State.CONNECTING;
        } else if (!isFailed) {
            if (VDBG) log( "overall state is IDLE");
            return DctConstants.State.IDLE;
        } else {
            if (VDBG) log( "overall state is FAILED");
            return DctConstants.State.FAILED;
        }
    }

    public boolean isApnTypeAvailable(String type) {
        if ((type.equals(PhoneConstants.APN_TYPE_DUN) && fetchDunApn() != null) ||
             type.equals(PhoneConstants.APN_TYPE_EMERGENCY)) {
            log("isApnTypeAvaiable, apn: " + type);
            return true;
        }

        if (mAllApnSettings != null) {
            for (ApnSetting apn : mAllApnSettings) {
                if (apn.canHandleType(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Report on whether data connectivity is enabled for any APN.
     * @return {@code false} if data connectivity has been explicitly disabled,
     * {@code true} otherwise.
     */
    public boolean getAnyDataEnabled() {
        if (!isDataEnabled(true)) return false;
        DataAllowFailReason failureReason = new DataAllowFailReason();
        if (!isDataAllowed(failureReason)) {
            if (DBG) log(failureReason.getDataAllowFailReason());
            return false;
        }
        for (ApnContext apnContext : mApnContexts.values()) {
            // Make sure we don't have a context that is going down
            // and is explicitly disabled.
            if (isDataAllowedForApn(apnContext)) {
                if (DBG) {
                    log("getAnyDataEnabled1 return true, apn=" + apnContext.getApnType());
                }
                return true;
            }
        }
        if (DBG) log("getAnyDataEnabled1 return false");
        return false;
    }

    public boolean getAnyDataEnabled(boolean checkUserDataEnabled) {
        if (!isDataEnabled(checkUserDataEnabled)) return false;

        DataAllowFailReason failureReason = new DataAllowFailReason();
        if (!isDataAllowed(failureReason)) {
            if (DBG) log(failureReason.getDataAllowFailReason());
            return false;
        }
        for (ApnContext apnContext : mApnContexts.values()) {
            // Make sure we dont have a context that going down
            // and is explicitly disabled.
            if (isDataAllowedForApn(apnContext)) {
                if (DBG) {
                    log("getAnyDataEnabled2 return true, apn = " + apnContext.getApnType());
                }
                return true;
            }
        }
        if (DBG) log("getAnyDataEnabled2 return false");
        return false;
    }

    private boolean isDataEnabled(boolean checkUserDataEnabled) {
        synchronized (mDataEnabledLock) {
            if (checkUserDataEnabled) {
                mUserDataEnabled = getDataEnabled();
            }

            if (!(mInternalDataEnabled && (!checkUserDataEnabled || mUserDataEnabled)
                    && (!checkUserDataEnabled || sPolicyDataEnabled))) {
                if (DBG) {
                    log("isDataEnabled return false. mInternalDataEnabled = "
                            + mInternalDataEnabled + ", checkUserDataEnabled = "
                            + checkUserDataEnabled + ", mUserDataEnabled = "
                            + mUserDataEnabled + ", sPolicyDataEnabled = " + sPolicyDataEnabled);
                }
                return false;
            }
        }
        return true;
    }

    private boolean isDataAllowedForApn(ApnContext apnContext) {
        //If RAT is iwlan then dont allow default/IA PDP at all.
        //Rest of APN types can be evaluated for remaining conditions.
        if ((apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)
                    || apnContext.getApnType().equals(PhoneConstants.APN_TYPE_IA))
                && (mPhone.getServiceState().getRilDataRadioTechnology()
                == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)) {
            log("Default data call activation not allowed in iwlan.");
            return false;
        }

        return apnContext.isReady();
    }

    //****** Called from ServiceStateTracker
    /**
     * Invoked when ServiceStateTracker observes a transition from GPRS
     * attach to detach.
     */
    private void onDataConnectionDetached() {
        /*
         * We presently believe it is unnecessary to tear down the PDP context
         * when GPRS detaches, but we should stop the network polling.
         */
        if (DBG) log ("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();
        notifyDataConnection(Phone.REASON_DATA_DETACHED);
        mAttached.set(false);

        // M: To avoid trying setup data call before PS attach.
        if (mAutoAttachOnCreationConfig) {
            mAutoAttachOnCreation.set(false);
        }
    }

    private void onDataConnectionAttached() {
        if (DBG) log("onDataConnectionAttached");
        mAttached.set(true);
        if (getOverallState() == DctConstants.State.CONNECTED) {
            if (DBG) log("onDataConnectionAttached: start polling notify attached");
            startNetStatPoll();
            startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
            notifyDataConnection(Phone.REASON_DATA_ATTACHED);
        } else {
            // update APN availability so that APN can be enabled.
            notifyOffApnsOfAvailability(Phone.REASON_DATA_ATTACHED);
        }
        if (mAutoAttachOnCreationConfig) {
            mAutoAttachOnCreation.set(true);
        }
        setupDataOnConnectableApns(Phone.REASON_DATA_ATTACHED);
    }

    private boolean isDataAllowed(DataAllowFailReason failureReason) {
        final boolean internalDataEnabled;
        synchronized (mDataEnabledLock) {
            internalDataEnabled = mInternalDataEnabled;
        }

        boolean attachedState = mAttached.get();
        boolean desiredPowerState = mPhone.getServiceStateTracker().getDesiredPowerState();
        int radioTech = mPhone.getServiceState().getRilDataRadioTechnology();
        if (radioTech == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN) {
            desiredPowerState = true;
        }

        IccRecords r = mIccRecords.get();
        boolean recordsLoaded = false;
        if (r != null) {
            recordsLoaded = r.getRecordsLoaded();
            if (DBG && !recordsLoaded) log("isDataAllowed getRecordsLoaded=" + recordsLoaded);
        }

        boolean bIsFdnEnabled = isFdnEnabled();

        int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean defaultDataSelected = SubscriptionManager.isValidSubscriptionId(dataSub);

        PhoneConstants.State state = PhoneConstants.State.IDLE;
        // Note this is explicitly not using mPhone.getState.  See b/19090488.
        // mPhone.getState reports the merge of CS and PS (volte) voice call state
        // but we only care about CS calls here for data/voice concurrency issues.
        // Calling getCallTracker currently gives you just the CS side where the
        // ImsCallTracker is held internally where applicable.
        // This should be redesigned to ask explicitly what we want:
        // voiceCallStateAllowDataCall, or dataCallAllowed or something similar.
        if (mPhone.getCallTracker() != null) {
            state = mPhone.getCallTracker().getState();
        }

        DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();

        if (failureReason != null) failureReason.clearAllReasons();
        if (!(attachedState || mAutoAttachOnCreation.get())) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.NOT_ATTACHED);
        }
        if (!recordsLoaded) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.RECORD_NOT_LOADED);
        }
        if (!dcHelper.isAllCallingStateIdle() &&
                !dcHelper.isDataSupportConcurrent(mPhone.getPhoneId())) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.INVALID_PHONE_STATE);
            failureReason.addDataAllowFailReason(
                    DataAllowFailReasonType.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }
        if (!internalDataEnabled) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.INTERNAL_DATA_DISABLED);
        }
        if (!defaultDataSelected) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(
                    DataAllowFailReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if ((mPhone.getServiceState().getDataRoaming() ||
                mPhone.getServiceStateTracker().isPsRegStateRoamByUnsol()) &&
                !getDataOnRoamingEnabled()) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.ROAMING_DISABLED);
        }
        if (mIsPsRestricted) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.UNDESIRED_POWER_STATE);
        }
        if (bIsFdnEnabled) {
            if(failureReason == null) return false;
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.FDN_ENABLED);
        }

        return failureReason == null || !failureReason.isFailed();
    }

    private boolean isDataAllowedExt(DataAllowFailReason failureReason, String apnType) {
        int nFailReasonSize = failureReason.getSizeOfFailReason();

        if (1 == nFailReasonSize) {
            return (failureReason.isFailForSingleReason(
                    DataAllowFailReasonType.DEFAULT_DATA_UNSELECTED)
                    && ignoreDefaultDataUnselected(apnType))
                    || (failureReason.isFailForSingleReason(
                    DataAllowFailReasonType.ROAMING_DISABLED)
                    && ignoreDataRoaming(apnType));
        } else if (2 == nFailReasonSize) {
            return (failureReason.isFailForReason(DataAllowFailReasonType.DEFAULT_DATA_UNSELECTED)
                    && ignoreDefaultDataUnselected(apnType))
                    && (failureReason.isFailForReason(DataAllowFailReasonType.ROAMING_DISABLED)
                    && ignoreDataRoaming(apnType));
        }

        return false;
    }

    // arg for setupDataOnConnectableApns
    private enum RetryFailures {
        // retry failed networks always (the old default)
        ALWAYS,
        // retry only when a substantial change has occurred.  Either:
        // 1) we were restricted by voice/data concurrency and aren't anymore
        // 2) our apn list has change
        ONLY_ON_CHANGE
    };

    private void setupDataOnConnectableApns(String reason) {
        setupDataOnConnectableApns(reason, RetryFailures.ALWAYS);
    }

    private void setupDataOnConnectableApns(String reason, RetryFailures retryFailures) {
        if (VDBG) log("setupDataOnConnectableApns: " + reason);

        if (DBG && !VDBG) {
            StringBuilder sb = new StringBuilder(120);
            for (ApnContext apnContext : mPrioritySortedApnContexts) {
                sb.append(apnContext.getApnType());
                sb.append(":[state=");
                sb.append(apnContext.getState());
                sb.append(",enabled=");
                sb.append(apnContext.isEnabled());
                sb.append("] ");
            }
            log("setupDataOnConnectableApns: " + reason + " " + sb);
        }

        ArrayList<ApnContext> aryApnContext = new ArrayList<ApnContext>();
        String strTempIA = SystemProperties.get("ril.radio.ia-apn");

        for (ApnContext tmpApnContext : mPrioritySortedApnContexts) {
            if ((TextUtils.equals(strTempIA, "VZWIMS")
                    && TextUtils.equals(tmpApnContext.getApnType(), PhoneConstants.APN_TYPE_IMS))
                    || (TextUtils.equals(strTempIA, "VZWINTERNET")
                    && TextUtils.equals(tmpApnContext.getApnType(),
                        PhoneConstants.APN_TYPE_DEFAULT))) {
                aryApnContext.add(0, tmpApnContext);
            } else {
                aryApnContext.add(tmpApnContext);
            }
        }

        for (ApnContext apnContext : aryApnContext) {
            ArrayList<ApnSetting> waitingApns = null;

            if (PhoneConstants.APN_TYPE_IMS.equals(apnContext.getApnType())
                || PhoneConstants.APN_TYPE_EMERGENCY.equals(apnContext.getApnType())) {
                //for VoLTE, framework should not trigger IMS/Emergency bearer activation
                if (VDBG && EVDBG) {
                    log("setupDataOnConnectableApns: ignore apnContext " + apnContext);
                }
            } else {
                if (VDBG && EVDBG) log("setupDataOnConnectableApns: apnContext " + apnContext);

                if (apnContext.getState() == DctConstants.State.FAILED
                        || apnContext.getState() == DctConstants.State.SCANNING) {
                    if (retryFailures == RetryFailures.ALWAYS) {
                        apnContext.releaseDataConnection(reason);
                    } else if (apnContext.isConcurrentVoiceAndDataAllowed() == false &&
                            mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                        // RetryFailures.ONLY_ON_CHANGE - check if voice concurrency has changed
                        apnContext.releaseDataConnection(reason);
                    } else {
                        // RetryFailures.ONLY_ON_CHANGE - check if the apns have changed
                        int radioTech = mPhone.getServiceState().getRilDataRadioTechnology();
                        ArrayList<ApnSetting> originalApns = apnContext.getWaitingApns();
                        if (originalApns != null && originalApns.isEmpty() == false) {
                            waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                            if (originalApns.size() != waitingApns.size() ||
                                    originalApns.containsAll(waitingApns) == false) {
                                apnContext.releaseDataConnection(reason);
                            } else {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (TextUtils.equals(apnContext.getApnType(), PhoneConstants.APN_TYPE_DEFAULT)
                        && TextUtils.equals(strTempIA, "VZWIMS")) {
                    ApnContext apnContextIms = mApnContexts.get(PhoneConstants.APN_TYPE_IMS);
                    if (apnContextIms != null && !apnContextIms.isEnabled()
                            && !TextUtils.equals(reason, Phone.REASON_DATA_ATTACHED)
                                && !TextUtils.equals(reason, Phone.REASON_DATA_ENABLED)
                            && !TextUtils.equals(reason, Phone.REASON_APN_CHANGED)
                            && !TextUtils.equals(reason, Phone.REASON_VOICE_CALL_ENDED)
                            && !TextUtils.equals(reason, Phone.REASON_SIM_LOADED)) {
                        log("setupDataOnConnectableApns: ignore default pdn setup");
                        continue;
                    }
                }
                if (apnContext.isConnectable()) {
                    log("setupDataOnConnectableApns: isConnectable() call trySetupData");
                    apnContext.setReason(reason);
                    trySetupData(apnContext, waitingApns);
                }
            }
        }
    }

    boolean isEmergency() {
        final boolean result;
        synchronized (mDataEnabledLock) {
            result = mPhone.isInEcm() || mPhone.isInEmergencyCall();
        }
        log("isEmergency: result=" + result);
        return result;
    }

    private boolean trySetupData(ApnContext apnContext) {
        return trySetupData(apnContext, null);
    }

    private boolean trySetupData(ApnContext apnContext, ArrayList<ApnSetting> waitingApns) {
        if (DBG) {
            log("trySetupData for type:" + apnContext.getApnType() +
                    " due to " + apnContext.getReason() + ", mIsPsRestricted=" + mIsPsRestricted);
        }
        apnContext.requestLog("trySetupData due to " + apnContext.getReason());

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            apnContext.setState(DctConstants.State.CONNECTED);
            mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());

            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return true;
        }

        // Allow SETUP_DATA request for E-APN to be completed during emergency call
        // and MOBILE DATA On/Off cases as well.
        boolean isEmergencyApn = apnContext.getApnType().equals(PhoneConstants.APN_TYPE_EMERGENCY);
        final ServiceStateTracker sst = mPhone.getServiceStateTracker();

        // set to false if apn type is non-metered.
        boolean checkUserDataEnabled =
                (ApnSetting.isMeteredApnType(apnContext.getApnType(), mPhone.getContext(),
                        mPhone.getSubId(), mPhone.getServiceState().getDataRoaming()) &&
                        /** M: enable MMS and SUPL even if data is disabled */
                        !isDataAllowedAsOff(apnContext.getApnType()));

        DataAllowFailReason failureReason = new DataAllowFailReason();

        // allow data if currently in roaming service, roaming setting disabled
        // and requested apn type is non-metered for roaming.
        boolean isDataAllowed = isDataAllowed(failureReason) ||
                (failureReason.isFailForSingleReason(DataAllowFailReasonType.ROAMING_DISABLED) &&
                !(ApnSetting.isMeteredApnType(apnContext.getApnType(), mPhone.getContext(),
                mPhone.getSubId(), mPhone.getServiceState().getDataRoaming()))) ||
                // M: extend the logics of isDataAllowed()
                isDataAllowedExt(failureReason, apnContext.getApnType());

        if (apnContext.isConnectable() && (isEmergencyApn ||
                (isDataAllowed && isDataAllowedForApn(apnContext) &&
                isDataEnabled(checkUserDataEnabled) && !isEmergency())) && !mColdSimDetected ) {
            if (apnContext.getState() == DctConstants.State.FAILED) {
                String str = "trySetupData: make a FAILED ApnContext IDLE so its reusable";
                if (DBG) log(str);
                apnContext.requestLog(str);
                apnContext.setState(DctConstants.State.IDLE);
            }
            int radioTech = mPhone.getServiceState().getRilDataRadioTechnology();
            apnContext.setConcurrentVoiceAndDataAllowed(sst.isConcurrentVoiceAndDataAllowed());
            if (apnContext.getState() == DctConstants.State.IDLE ||
                (apnContext.getApnType().equals(PhoneConstants.APN_TYPE_MMS) &&
                 apnContext.getState() == DctConstants.State.RETRYING)) {

                if (waitingApns == null) {
                    // M: ECC w/o SIM {
                    if (TextUtils.equals(apnContext.getApnType(),
                        PhoneConstants.APN_TYPE_EMERGENCY)) {
                        if (mAllApnSettings == null) {
                            log("mAllApnSettings is null, create first and add emergency one");
                            createAllApnList();
                        } else if (mAllApnSettings.isEmpty()) {
                            log("add mEmergencyApn: " + mEmergencyApn + " to mAllApnSettings");
                            addEmergencyApnSetting();
                        }
                    }
                    // M: ECC w/o SIM }
                    waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                }
                if (waitingApns.isEmpty()) {
                    notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    String str = "trySetupData: X No APN found retValue=false";
                    if (DBG) log(str);
                    apnContext.requestLog(str);
                    return false;
                } else {
                    apnContext.setWaitingApns(waitingApns);
                    // M: VDF MMS over ePDG @{
                    apnContext.setWifiApns(buildWifiApns(apnContext.getApnType()));
                    /// @}
                    if (DBG) {
                        log ("trySetupData: Create from mAllApnSettings : "
                                    + apnListToString(mAllApnSettings));
                    }
                }
            }

            if (DBG) {
                log("trySetupData: call setupData, waitingApns : "
                        + apnListToString(apnContext.getWaitingApns()));
                // M: VDF MMS over ePDG @{
                log("trySetupData: call setupData, wifiApns : "
                        + apnListToString(apnContext.getWifiApns()));
                /// @}
            }
            boolean retValue = setupData(apnContext, radioTech);
            notifyOffApnsOfAvailability(apnContext.getReason());

            if (DBG) log("trySetupData: X retValue=" + retValue);
            return retValue;
        } else {
            if (!apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT)
                    && apnContext.isConnectable()) {
                if (apnContext.getApnType().equals(PhoneConstants.APN_TYPE_MMS)
                        && TelephonyManager.getDefault().isMultiSimEnabled() && !mAttached.get()) {
                    if (DBG) {
                        log("Wait for attach");
                    }
                    if (apnContext.getState() == DctConstants.State.IDLE) {
                        apnContext.setState(DctConstants.State.RETRYING);
                    }
                    return true;
                } else {
                    mPhone.notifyDataConnectionFailed(apnContext.getReason(),
                            apnContext.getApnType());
                }
            }
            notifyOffApnsOfAvailability(apnContext.getReason());

            StringBuilder str = new StringBuilder();

            str.append("trySetupData failed. apnContext = [type=" + apnContext.getApnType() +
                    ", mState=" + apnContext.getState() + ", mDataEnabled=" +
                    apnContext.isEnabled() + ", mDependencyMet=" +
                    apnContext.getDependencyMet() + "] ");

            if (!apnContext.isConnectable()) {
                str.append("isConnectable = false. ");
            }
            if (!isDataAllowed) {
                str.append("data not allowed: " + failureReason.getDataAllowFailReason() + ". ");
            }
            if (!isDataAllowedForApn(apnContext)) {
                str.append("isDataAllowedForApn = false. RAT = " +
                        mPhone.getServiceState().getRilDataRadioTechnology());
            }
            if (!isDataEnabled(checkUserDataEnabled)) {
                str.append("isDataEnabled(" + checkUserDataEnabled + ") = false. " +
                        "mInternalDataEnabled = " + mInternalDataEnabled + " , mUserDataEnabled = "
                        + mUserDataEnabled + ", sPolicyDataEnabled = " + sPolicyDataEnabled + " ");
            }
            if (isEmergency()) {
                str.append("emergency = true");
            }
            if(mColdSimDetected) {
                str.append("coldSimDetected = true");
            }

            if (DBG) log(str.toString());
            apnContext.requestLog(str.toString());

            return false;
        }
    }

    // Disabled apn's still need avail/unavail notifications - send them out
    private void notifyOffApnsOfAvailability(String reason) {
        if (DBG) {
            DataAllowFailReason failureReason = new DataAllowFailReason();
            if (!isDataAllowed(failureReason)) {
                log(failureReason.getDataAllowFailReason());
            }
        }
        for (ApnContext apnContext : mApnContexts.values()) {
            if ((!mAttached.get() || !apnContext.isReady()) && apnContext.isNeedNotify()) {
                String apnType = apnContext.getApnType();
                if (VDBG && EVDBG) {
                    log("notifyOffApnOfAvailability type:" + apnType + " reason: " + reason);
                }
                PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;
                if (apnType== PhoneConstants.APN_TYPE_IMS) {
                    switch (getState(apnType)) {
                    case RETRYING:
                        if (DBG) log("getDataConnectionState: apnType: " + apnType
                               + " is in retrying state!! return connecting state");
                       ret = PhoneConstants.DataState.CONNECTING;
                       break;
                    case CONNECTED:
                       ret = PhoneConstants.DataState.CONNECTED;
                       break;
                    case CONNECTING:
                    case SCANNING:
                       ret = PhoneConstants.DataState.CONNECTING;
                       break;
                    case FAILED:
                    case IDLE:
                    default:
                       break;
                    }
                }
                mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(),
                                            apnType, ret);
            } else {
                if (VDBG && EVDBG) {
                    log("notifyOffApnsOfAvailability skipped apn due to attached && isReady " +
                            apnContext.toString());
                }
            }
        }
    }

    /**
     * If tearDown is true, this only tears down a CONNECTED session. Presently,
     * there is no mechanism for abandoning an CONNECTING session,
     * but would likely involve cancelling pending async requests or
     * setting a flag or new state to ignore them when they came in
     * @param tearDown true if the underlying DataConnection should be
     * disconnected.
     * @param reason reason for the clean up.
     * @return boolean - true if we did cleanup any connections, false if they
     *                   were already all disconnected.
     */
    private boolean cleanUpAllConnections(boolean tearDown, String reason) {
        if (DBG) log("cleanUpAllConnections: tearDown=" + tearDown + " reason=" + reason);
        boolean didDisconnect = false;
        boolean specificDisable = false;

        // Either user disable mobile data or under roaming service and user disabled roaming
        if (!TextUtils.isEmpty(reason)) {
            specificDisable = reason.equals(Phone.REASON_DATA_SPECIFIC_DISABLED) ||
                    reason.equals(Phone.REASON_ROAMING_ON);
            // /Ignore IMS PDN deactivation when Radio turned off or PDP Recovery @{
            specificDisable = specificDisable || reason.equals(Phone.REASON_RADIO_TURNED_OFF);
            specificDisable = specificDisable || reason.equals(Phone.REASON_PDP_RESET);
            // /@}
        }

        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.isDisconnected() == false) didDisconnect = true;
            if (specificDisable) {
                // Use ApnSetting to decide metered or non-metered.
                // Tear down all metered data connections.
                ApnSetting apnSetting = apnContext.getApnSetting();
                if (apnSetting != null && apnSetting.isMetered(mPhone.getContext(),
                        mPhone.getSubId(), mPhone.getServiceState().getDataRoaming())) {
                    if (DBG) log("clean up metered ApnContext Type: " + apnContext.getApnType());
                    apnContext.setReason(reason);
                    cleanUpConnection(tearDown, apnContext);
                }
            } else {
                if (reason != null && reason.equals(Phone.REASON_ROAMING_ON)
                        && ignoreDataRoaming(apnContext.getApnType())) {
                    if (DBG) {
                        log("cleanUpConnection: Ignore Data Roaming for apnType = "
                                + apnContext.getApnType());
                    }
                } else {
                    // TODO - only do cleanup if not disconnected
                    apnContext.setReason(reason);
                    cleanUpConnection(tearDown, apnContext);
                }
            }
        }

        stopNetStatPoll();
        stopDataStallAlarm();

        // TODO: Do we need mRequestedApnType?
        mRequestedApnType = PhoneConstants.APN_TYPE_DEFAULT;

        log("cleanUpConnection: mDisconnectPendingCount = " + mDisconnectPendingCount);
        if (tearDown && mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }

        return didDisconnect;
    }

    /**
     * Cleanup all connections.
     *
     * TODO: Cleanup only a specified connection passed as a parameter.
     *       Also, make sure when you clean up a conn, if it is last apply
     *       logic as though it is cleanupAllConnections
     *
     * @param cause for the clean up.
     */
    private void onCleanUpAllConnections(String cause) {
        cleanUpAllConnections(true, cause);
    }

    void sendCleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (DBG) log("sendCleanUpConnection: tearDown=" + tearDown + " apnContext=" + apnContext);
        Message msg = obtainMessage(DctConstants.EVENT_CLEAN_UP_CONNECTION);
        msg.arg1 = tearDown ? 1 : 0;
        msg.arg2 = 0;
        msg.obj = apnContext;
        sendMessage(msg);
    }

    private void cleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (apnContext == null) {
            if (DBG) log("cleanUpConnection: apn context is null");
            return;
        }

        DcAsyncChannel dcac = apnContext.getDcAc();
        String str = "cleanUpConnection: tearDown=" + tearDown + " reason=" +
                apnContext.getReason();
        if (VDBG) log(str + " apnContext=" + apnContext);
        apnContext.requestLog(str);
        if (tearDown) {
            if (apnContext.isDisconnected()) {
                // The request is tearDown and but ApnContext is not connected.
                // If apnContext is not enabled anymore, break the linkage to the DCAC/DC.
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcac != null) {
                        str = "cleanUpConnection: teardown, disconnected, !ready";
                        if (DBG) log(str + " apnContext=" + apnContext);
                        apnContext.requestLog(str);
                        dcac.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                }
            } else {
                // Connection is still there. Try to clean up.
                if (dcac != null) {
                    if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                        boolean disconnectAll = false;
                        if (PhoneConstants.APN_TYPE_DUN.equals(apnContext.getApnType())) {
                            // CAF_MSIM is this below condition required.
                            // if (PhoneConstants.APN_TYPE_DUN.equals(PhoneConstants.APN_TYPE_DEFAULT)) {
                            if (teardownForDun()) {
                                if (DBG) {
                                    log("cleanUpConnection: disconnectAll DUN connection");
                                }
                                // we need to tear it down - we brought it up just for dun and
                                // other people are camped on it and now dun is done.  We need
                                // to stop using it and let the normal apn list get used to find
                                // connections for the remaining desired connections
                                disconnectAll = true;
                            }
                        }
                        final int generation = apnContext.getConnectionGeneration();
                        str = "cleanUpConnection: tearing down" + (disconnectAll ? " all" : "") +
                                " using gen#" + generation;
                        if (DBG) log(str + "apnContext=" + apnContext);
                        apnContext.requestLog(str);
                        Pair<ApnContext, Integer> pair =
                                new Pair<ApnContext, Integer>(apnContext, generation);
                        Message msg = obtainMessage(DctConstants.EVENT_DISCONNECT_DONE, pair);
                        if (disconnectAll) {
                            apnContext.getDcAc().tearDownAll(apnContext.getReason(), msg);
                        } else {
                            apnContext.getDcAc()
                                .tearDown(apnContext, apnContext.getReason(), msg);
                        }
                        apnContext.setState(DctConstants.State.DISCONNECTING);
                        mDisconnectPendingCount++;
                    }
                } else {
                    // apn is connected but no reference to dcac.
                    // Should not be happen, but reset the state in case.
                    apnContext.setState(DctConstants.State.IDLE);
                    apnContext.requestLog("cleanUpConnection: connected, bug no DCAC");
                    if (apnContext.isNeedNotify()) {
                        mPhone.notifyDataConnection(apnContext.getReason(),
                                apnContext.getApnType());
                    }
                }
            }
        } else {
            boolean needNotify = true;
            //TODO: remove phone count.
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            if (apnContext.isDisconnected() && phoneCount > 2) {
                needNotify = false;
            }
            // force clean up the data connection.
            if (dcac != null) dcac.reqReset();
            apnContext.setState(DctConstants.State.IDLE);
            if (apnContext.isNeedNotify() && needNotify) {
                mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
            apnContext.setDataConnectionAc(null);
        }

        // Make sure reconnection alarm is cleaned up if there is no ApnContext
        // associated to the connection.
        if (dcac != null) {
            cancelReconnectAlarm(apnContext);
        }
        str = "cleanUpConnection: X tearDown=" + tearDown + " reason=" + apnContext.getReason();
        if (DBG && apnContext.isNeedNotify()) {
            log(str + " apnContext=" + apnContext + " dcac=" + apnContext.getDcAc());
        }
        apnContext.requestLog(str);
    }

    ApnSetting fetchDunApn() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            log("fetchDunApn: net.tethering.noprovisioning=true ret: null");
            return null;
        }
        int bearer = mPhone.getServiceState().getRilDataRadioTechnology();
        ApnSetting retDunSetting = null;
        String apnData = Settings.Global.getString(mResolver, Settings.Global.TETHER_DUN_APN);
        List<ApnSetting> dunSettings = ApnSetting.arrayFromString(apnData);
        IccRecords r = mIccRecords.get();
        for (ApnSetting dunSetting : dunSettings) {
            String operator = (r != null) ? r.getOperatorNumeric() : "";
            if (!ServiceState.bitmaskHasTech(dunSetting.bearerBitmask, bearer)) continue;
            if (dunSetting.numeric.equals(operator)) {
                if (dunSetting.hasMvnoParams()) {
                    if (r != null && ApnSetting.mvnoMatches(r, dunSetting.mvnoType,
                            dunSetting.mvnoMatchData)) {
                        if (VDBG) {
                            log("fetchDunApn: global TETHER_DUN_APN dunSetting=" + dunSetting);
                        }
                        return dunSetting;
                    }
                } else if (mMvnoMatched == false) {
                    if (VDBG) log("fetchDunApn: global TETHER_DUN_APN dunSetting=" + dunSetting);
                    return dunSetting;
                }
            }
        }

        Context c = mPhone.getContext();
        //String[] apnArrayData = c.getResources().getStringArray(R.array.config_tether_apndata);
        String[] apnArrayData = getDunApnByMccMnc(c);
        for (String apn : apnArrayData) {
            ApnSetting dunSetting = ApnSetting.fromString(apn);
            if (dunSetting != null) {
                if (!ServiceState.bitmaskHasTech(dunSetting.bearerBitmask, bearer)) continue;
                if (dunSetting.hasMvnoParams()) {
                    if (r != null && ApnSetting.mvnoMatches(r, dunSetting.mvnoType,
                            dunSetting.mvnoMatchData)) {
                        if (VDBG) {
                            log("fetchDunApn: config_tether_apndata mvno dunSetting=" + dunSetting);
                        }
                        return dunSetting;
                    }
                } else if (mMvnoMatched == false) {
                    retDunSetting = dunSetting;
                }
            }
        }

        if (VDBG) log("fetchDunApn: config_tether_apndata dunSetting=" + retDunSetting);
        return retDunSetting;
    }

    public boolean hasMatchedTetherApnSetting() {
        ApnSetting matched = fetchDunApn();
        log("hasMatchedTetherApnSetting: APN=" + matched);
        return matched != null;
    }

    // M: Fixed google DUN only one resource problem
    private String[] getDunApnByMccMnc(Context context){
        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        int mcc = 0;
        int mnc = 0;
        if (operator != null && operator.length() > 3) {
            mcc = Integer.parseInt(operator.substring(0, 3));
            mnc = Integer.parseInt(operator.substring(3, operator.length()));
        }

        Resources sysResource = context.getResources();
        int sysMcc = sysResource.getConfiguration().mcc;
        int sysMnc = sysResource.getConfiguration().mnc;
        if (VDBG) log("fetchDunApn: Resource mccmnc=" + sysMcc + "," + sysMnc +
                "; OperatorNumeric mccmnc=" + mcc + "," + mnc);
        Resources resource = null;
        try {
            Configuration configuration = new Configuration();
            configuration = context.getResources().getConfiguration();
            configuration.mcc = mcc;
            configuration.mnc = mnc;
            Context resc = context.createConfigurationContext(configuration);
            resource = resc.getResources();
        } catch (Exception e) {
            e.printStackTrace();
            log("getResourcesUsingMccMnc fail");
        }

        // If single sim, configuration numeric == sysNumeric or resourse
        if ((TelephonyManager.getDefault().getSimCount() == 1)
                || (mcc == sysMcc && mnc == sysMnc) || resource == null) {
            return sysResource.getStringArray(R.array.config_tether_apndata);
        } else {
            if (VDBG) log("fetchDunApn: get resource from mcc=" + mcc + ", mnc=" + mnc);
            return resource.getStringArray(R.array.config_tether_apndata);
        }
    }

    /**
     * Determine if DUN connection is special and we need to teardown on start/stop
     */
    private boolean teardownForDun() {
        // CDMA always needs to do this the profile id is correct
        final int rilRat = mPhone.getServiceState().getRilDataRadioTechnology();
        if (ServiceState.isCdma(rilRat)) return true;

        return (fetchDunApn() != null);
    }

    /**
     * Cancels the alarm associated with apnContext.
     *
     * @param apnContext on which the alarm should be stopped.
     */
    private void cancelReconnectAlarm(ApnContext apnContext) {
        if (apnContext == null) return;

        PendingIntent intent = apnContext.getReconnectIntent();

        if (intent != null) {
                AlarmManager am =
                    (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
                am.cancel(intent);
                apnContext.setReconnectIntent(null);
        }
    }

    /**
     * @param types comma delimited list of APN types
     * @return array of APN types
     */
    private String[] parseTypes(String types) {
        String[] result;
        // If unset, set to DEFAULT.
        if (types == null || types.equals("")) {
            result = new String[1];
            result[0] = PhoneConstants.APN_TYPE_ALL;
        } else {
            result = types.split(",");
        }
        return result;
    }

    boolean isPermanentFail(DcFailCause dcFailCause) {
        boolean isPermanent = dcFailCause.isPermanentFail();

        // For OP129
        if (129 == DataConnectionHelper.getInstance().getSbpIdFromNetworkOperator(
                mPhone.getPhoneId())) {
            return (dcFailCause.isPermanentFail()
                    || dcFailCause == DcFailCause.TCM_ESM_TIMER_TIMEOUT)
                    && (mAttached.get() == false || dcFailCause != DcFailCause.SIGNAL_LOST);
        }

        // M: Check if it is permanent fail by operator
        if (mDcFcMgr == null) {
            loge("mDcFcMgr should not be null, something wrong");
        } else {
            isPermanent = mDcFcMgr.isPermanentFailByOp(dcFailCause);
        }

        return (isPermanent &&
                (mAttached.get() == false || dcFailCause != DcFailCause.SIGNAL_LOST));
    }

    private ApnSetting makeApnSetting(Cursor cursor) {
        // M: Inactive timer for Sprint
        int inactiveTimer = 0;
        try {
            inactiveTimer = cursor.getInt(
                    cursor.getColumnIndexOrThrow(Telephony.Carriers.INACTIVE_TIMER));
        } catch (IllegalArgumentException e) {
            loge("makeApnSetting: parsing inactive timer failed. " + e);
        }

        String[] types = parseTypes(
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
        ApnSetting apn = new ApnSetting(
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                NetworkUtils.trimV4AddrZeros(
                        cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                types,
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.ROAMING_PROTOCOL)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.CARRIER_ENABLED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER_BITMASK)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MODEM_COGNITIVE)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.WAIT_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MTU)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_MATCH_DATA)),
                inactiveTimer);
        return apn;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> mnoApns = new ArrayList<ApnSetting>();
        ArrayList<ApnSetting> mvnoApns = new ArrayList<ApnSetting>();
        IccRecords r = mIccRecords.get();

        if (cursor.moveToFirst()) {
            do {
                ApnSetting apn = makeApnSetting(cursor);
                if (apn == null) {
                    continue;
                }

                if (apn.hasMvnoParams()) {
                    if (r != null && ApnSetting.mvnoMatches(r, apn.mvnoType, apn.mvnoMatchData)) {
                        mvnoApns.add(apn);
                    }
                } else {
                    mnoApns.add(apn);
                }
            } while (cursor.moveToNext());
        }

        ArrayList<ApnSetting> result;
        if (mvnoApns.isEmpty()) {
            result = mnoApns;
            mMvnoMatched = false;
        } else {
            result = mvnoApns;
            mMvnoMatched = true;
        }
        if (DBG) log("createApnList: X result=" + result);
        return result;
    }

    private boolean dataConnectionNotInUse(DcAsyncChannel dcac) {
        if (DBG) log("dataConnectionNotInUse: check if dcac is inuse dcac=" + dcac);
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getDcAc() == dcac) {
                if (DBG) log("dataConnectionNotInUse: in use by apnContext=" + apnContext);
                return false;
            }
        }
        /* To prevent that DataConnection is going to disconnect
        /* and we still need its information, not to do teardown here
        // TODO: Fix retry handling so free DataConnections have empty apnlists.
        // Probably move retry handling into DataConnections and reduce complexity
        // of DCT.
        if (DBG) log("dataConnectionNotInUse: tearDownAll");
        dcac.tearDownAll("No connection", null);
        */
        if (DBG) log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    private DcAsyncChannel findFreeDataConnection(String reqApnType, ApnSetting apnSetting) {
        for (DcAsyncChannel dcac : mDataConnectionAcHashMap.values()) {
            if (dcac.isInactiveSync() && dataConnectionNotInUse(dcac)) {
                DcAsyncChannel dcacForTeardown = dcac;
                if (isSupportThrottlingApn()) {
                    for (String apn : HIGH_THROUGHPUT_APN) {
                        if (apnSetting != null && apnSetting.canHandleType(apn)
                                && !PhoneConstants.APN_TYPE_EMERGENCY.equals(reqApnType)
                                && !apnSetting.canHandleType(PhoneConstants.APN_TYPE_IMS)
                                && dcac != null) {
                            int id = dcac.getDataConnectionIdSync();
                            if (id < MIN_ID_HIGH_TROUGHPUT || id > MAX_ID_HIGH_TROUGHPUT) {
                                dcac = null;
                            } else if (PhoneConstants.APN_TYPE_DEFAULT.equals(reqApnType)
                                    && id != MIN_ID_HIGH_TROUGHPUT) {
                                log("free dcac for default apn without interface id 0");
                                dcac = null;
                            }
                        }
                    }
                    if(Arrays.asList(IMS_APN).indexOf(reqApnType) > -1){
                        if (apnSetting != null && apnSetting.canHandleType(reqApnType)
                                && dcac != null) {
                            int id = dcac.getDataConnectionIdSync();
                            log("Data connection's interface is: " + id);
                            if ((id) == MIN_ID_IMS_TROUGHPUT
                                    && PhoneConstants.APN_TYPE_IMS.equals(reqApnType) ||
                                    id == (MAX_ID_IMS_TROUGHPUT - 1)
                                    && PhoneConstants.APN_TYPE_EMERGENCY.equals(reqApnType)) {
                                log("findFreeDataConnection: find connection to handle: "
                                        + reqApnType);
                            } else {
                                dcac = null;
                            }
                        }
                    }
                    if (!PhoneConstants.APN_TYPE_EMERGENCY.equals(reqApnType)
                            && !PhoneConstants.APN_TYPE_IMS.equals(reqApnType)
                            && dcac != null) {
                        int id = dcac.getDataConnectionIdSync();
                        if (id >= MIN_ID_IMS_TROUGHPUT && id <= MAX_ID_IMS_TROUGHPUT) {
                            log("findFreeDataConnection: free dcac for non-IMS APN");
                            dcac = null;
                        }
                    }
                    if (Arrays.asList(HIGH_THROUGHPUT_APN).indexOf(reqApnType) == -1
                            && dcac != null) {
                        int id = dcac.getDataConnectionIdSync();
                        if (id == MIN_ID_HIGH_TROUGHPUT) {
                            log("free dcac for non high throughput apn with interface id 0");
                            dcac = null;
                        }
                    }
                }

                if (dcac != null) {
                    if (DBG) {
                        log("findFreeDataConnection: found free DataConnection="
                                + " dcac=" + dcac);
                    }
                    return dcac;
                }
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    private boolean setupData(ApnContext apnContext, int radioTech) {
        if (DBG) log("setupData: apnContext=" + apnContext);
        apnContext.requestLog("setupData");
        ApnSetting apnSetting;
        DcAsyncChannel dcac = null;

        apnSetting = apnContext.getNextApnSetting();

        if (apnSetting == null &&
                !apnContext.getApnType().equals(PhoneConstants.APN_TYPE_EMERGENCY)) {
            if (DBG) log("setupData: return for no apn found!");
            return false;
        }

        int profileId = apnSetting.profileId;
        // M: VDF MMS over ePDG @{
        //if (profileId == 0) {
            profileId = getApnProfileID(apnContext.getApnType());
        //}
        /// @}
        // On CDMA, if we're explicitly asking for DUN, we need have
        // a dun-profiled connection so we can't share an existing one
        // On GSM/LTE we can share existing apn connections provided they support
        // this type.
        if (apnContext.getApnType() != PhoneConstants.APN_TYPE_DUN ||
                teardownForDun() == false) {
            dcac = checkForCompatibleConnectedApnContext(apnContext);
            if (dcac != null) {
                // Get the dcacApnSetting for the connection we want to share.
                ApnSetting dcacApnSetting = dcac.getApnSettingSync();
                if (dcacApnSetting != null) {
                    // Setting is good, so use it.
                    apnSetting = dcacApnSetting;
                }
            }
        }
        if (dcac == null) {
            if (isOnlySingleDcAllowed(radioTech)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    if (DBG) {
                        log("setupData: Higher priority ApnContext active.  Ignoring call");
                    }
                    return false;
                }

                // Only lower priority calls left.  Disconnect them all in this single PDP case
                // so that we can bring up the requested higher priority call (once we receive
                // response for deactivate request for the calls we are about to disconnect
                if (cleanUpAllConnections(true, Phone.REASON_SINGLE_PDN_ARBITRATION)) {
                    // If any call actually requested to be disconnected, means we can't
                    // bring up this connection yet as we need to wait for those data calls
                    // to be disconnected.
                    if (DBG) log("setupData: Some calls are disconnecting first.  Wait and retry");
                    return false;
                }

                // No other calls are active, so proceed
                if (DBG) log("setupData: Single pdp. Continue setting up data call.");
            }

            /** M: throttling/high throughput APN start **/
            if (!isSupportThrottlingApn() && !isOnlySingleDcAllowed(radioTech)) {
                boolean isHighThroughputApn = false;
                for (String apn : HIGH_THROUGHPUT_APN) {
                    if (apnSetting.canHandleType(apn)) {
                        isHighThroughputApn = true;
                        break;
                    }
                }

                if (!isHighThroughputApn) {
                    boolean lastDcAlreadyInUse = false;
                    for (DcAsyncChannel asyncChannel : mDataConnectionAcHashMap.values()) {
                        if (asyncChannel.getDataConnectionIdSync() == getPdpConnectionPoolSize()) {
                            if (asyncChannel.isInactiveSync() &&
                                    dataConnectionNotInUse(asyncChannel)) {
                                if (DBG) {
                                    log("find the last dc for non-high-throughput apn");
                                }
                                dcac = asyncChannel;

                                if (DBG) {
                                    log("setupData: tearDownAll is executed on un-used dcac");
                                }
                                asyncChannel.tearDownAll("No connection", null);
                            } else {
                                log("the last data connection is already in-use");
                                lastDcAlreadyInUse = true;
                            }
                        }
                    }
                    if (dcac == null && !lastDcAlreadyInUse) {
                        DataConnection conn = DataConnection.makeDataConnection(mPhone,
                                getPdpConnectionPoolSize(), this, mDcTesterFailBringUpAll, mDcc);
                        mDataConnections.put(getPdpConnectionPoolSize(), conn);
                        dcac = new DcAsyncChannel(conn, LOG_TAG);
                        int status = dcac.fullyConnectSync(mPhone.getContext(),
                                this, conn.getHandler());
                        if (status == AsyncChannel.STATUS_SUCCESSFUL) {
                            log("create the last data connection");
                            mDataConnectionAcHashMap.put(dcac.getDataConnectionIdSync(), dcac);
                        } else {
                            loge("createDataConnection (last): Could not connect to dcac="
                                    + dcac + " status=" + status);
                        }
                    }
                }
            }
            /** M: throttling/high throughput APN end start **/

            if (dcac == null) {
                if (DBG) log("setupData: No ready DataConnection found!");
                // TODO: When allocating you are mapping type to id. If more than 1 free,
                // then could findFreeDataConnection get the wrong one??
                dcac = findFreeDataConnection(apnContext.getApnType(), apnSetting);

            }

            // M: Reuse DCAC if there is remain DCAC for the ApnContext.
            if (dcac == null) {
                if (apnContext.getApnType() == PhoneConstants.APN_TYPE_DEFAULT
                        || apnContext.getApnType() == PhoneConstants.APN_TYPE_MMS) {
                    DcAsyncChannel prevDcac = apnContext.getDcAc();
                    // There is already an inactive dcac, try to reuse it.
                    if (prevDcac != null && prevDcac.isInactiveSync()) {
                        dcac = prevDcac;
                        ApnSetting dcacApnSetting = dcac.getApnSettingSync();
                        log("setupData: reuse previous DCAC: dcacApnSetting = "
                                + dcacApnSetting);
                        if (dcacApnSetting != null) {
                            // Setting is good, so use it.
                            apnSetting = dcacApnSetting;
                        }
                    }
                }
            }

            if (dcac == null) {
                dcac = createDataConnection(apnContext.getApnType(), apnSetting);
            }

            if (dcac == null) {
                if (DBG) log("setupData: No free DataConnection and couldn't create one, WEIRD");
                return false;
            }
        }
        final int generation = apnContext.incAndGetConnectionGeneration();
        if (DBG) {
            log("setupData: dcac=" + dcac + " apnSetting=" + apnSetting + " gen#=" + generation);
        }

        apnContext.setDataConnectionAc(dcac);
        apnContext.setApnSetting(apnSetting);
        apnContext.setState(DctConstants.State.CONNECTING);
        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());

        Message msg = obtainMessage();
        msg.what = DctConstants.EVENT_DATA_SETUP_COMPLETE;
        msg.obj = new Pair<ApnContext, Integer>(apnContext, generation);
        dcac.bringUp(apnContext, profileId, radioTech, msg, generation);

        if (DBG) log("setupData: initing!");
        return true;
    }

    // M: IA-change attach APN
    private void onMdChangedAttachApn(AsyncResult ar) {
        log("onMdChangedAttachApn");
        int[] ints = (int[]) ar.result;
        int apnId = ints[0];

        if (apnId != APN_CLASS_1 && apnId != APN_CLASS_3) {
            log("onMdChangedAttachApn: Not handle APN Class:" + apnId);
            return;
        }

        // Save MD requested APN class in property, for cases that DCT object disposed.
        int phoneId = mPhone.getPhoneId();
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            String iccId = SystemProperties.get(PROPERTY_ICCID[phoneId], "");
            SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, iccId);
            SystemProperties.set(PROP_APN_CLASS + phoneId, String.valueOf(apnId));
            log("onMdChangedAttachApn, set " + iccId + ", " + apnId);
        }

        updateMdChangedAttachApn(apnId);

        if (mMdChangedAttachApn != null) {
            setInitialAttachApn();
        } else {
            // Before createAllApnList, the mMdChangedAttachApn will be null
            // after updateMdChangedAttachApn(), it will be set in
            // onRecordsLoaded->setInitialAttachApn()
            log("onMdChangedAttachApn: MdChangedAttachApn is null, not found APN");
        }
    }

    // M: IA-change attach APN
    private void updateMdChangedAttachApn(int apnId) {
        if (mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
            for (ApnSetting apn : mAllApnSettings) {
                if (apnId == APN_CLASS_1 &&
                        ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_IMS)) {
                    mMdChangedAttachApn = apn;
                    log("onMdChangedAttachApn: MdChangedAttachApn=" + apn);
                    break;
                } else if (apnId == APN_CLASS_3 &&
                        ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_DEFAULT)) {
                    mMdChangedAttachApn = apn;
                    log("onMdChangedAttachApn: MdChangedAttachApn=" + apn);
                    break;
                }
            }
        }
    }

    // M: IA-change attach APN
    private boolean isMdChangedAttachApnEnabled() {
        if (mMdChangedAttachApn != null && mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
            for (ApnSetting apn : mAllApnSettings) {
                if (TextUtils.equals(mMdChangedAttachApn.apn, apn.apn)) {
                    log("isMdChangedAttachApnEnabled: " + apn);
                    return apn.carrierEnabled;
                }
            }
        }
        return false;
    }

    private void setInitialAttachApn() {
        // M: JPN IA Start
        boolean needsResumeModem = false;
        String currentMcc;
        // M: JPN IA End

        boolean isIaApn = false;
        ApnSetting previousAttachApn = mInitialAttachApnSetting;
        IccRecords r = mIccRecords.get();
        String operatorNumeric = (r != null) ? r.getOperatorNumeric() : "";
        if (operatorNumeric == null || operatorNumeric.length() == 0) {
            log("setInitialApn: but no operator numeric");
            return;
        } else {
            // M: JPN IA Start
            synchronized (mNeedsResumeModemLock) {
                if (mNeedsResumeModem) {
                    mNeedsResumeModem = false;
                    needsResumeModem = true;
                }
            }
            currentMcc = operatorNumeric.substring(0, 3);
            log("setInitialApn: currentMcc = " + currentMcc + ", needsResumeModem = "
                    + needsResumeModem);
            // M: JPN IA End
        }

        String[] dualApnPlmnList = null;
        if (MTK_DUAL_APN_SUPPORT == true) {
            dualApnPlmnList = mPhone.getContext().getResources()
                        .getStringArray(com.mediatek.internal.R.array.dtag_dual_apn_plmn_list);
        }

        log("setInitialApn: current attach Apn [" + mInitialAttachApnSetting + "]");

        ApnSetting iaApnSetting = null;
        ApnSetting defaultApnSetting = null;
        ApnSetting firstApnSetting = null;

        log("setInitialApn: E mPreferredApn=" + mPreferredApn);

        // M: change attach APN for MD changed APN and handover to WIFI
        if (mIsImsHandover || MTK_IMS_TESTMODE_SUPPORT) {
            // In those case should change attach APN to  class3 APN (VZWINTERNET)
            // The use of getClassTypeApn will return the ApnSetting of specify class APN.
            // Need to make sure the class number is valid (e.g. class1~4) for OP12 APN.
            mManualChangedAttachApn = getClassTypeApn(APN_CLASS_3);

            log("setInitialAttachApn: mIsImsHandover = " + mIsImsHandover +
                " MTK_IMS_TESTMODE_SUPPORT = " + MTK_IMS_TESTMODE_SUPPORT);

            if (mManualChangedAttachApn != null) {
                log("setInitialAttachApn: mManualChangedAttachApn = " + mManualChangedAttachApn);
            }
        }

        if (mMdChangedAttachApn == null) {
            // Restore MD requested APN class from property, for cases that DCT object disposed.
            // Don't restore if card changed.
            int phoneId = mPhone.getPhoneId();
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                int apnClass = SystemProperties.getInt(PROP_APN_CLASS + phoneId, -1);
                if (apnClass >= 0) {
                    String iccId = SystemProperties.get(PROPERTY_ICCID[phoneId], "");
                    String apnClassIccId = SystemProperties.get(PROP_APN_CLASS_ICCID + phoneId, "");
                    log("setInitialAttachApn: " + iccId + " , " + apnClassIccId + ", " + apnClass);
                    if (TextUtils.equals(iccId, apnClassIccId)) {
                        updateMdChangedAttachApn(apnClass);
                    } else {
                        SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, "");
                        SystemProperties.set(PROP_APN_CLASS + phoneId, "");
                    }
                }
            }
        }

        // M: IA-change attach APN
        // VZW required to detach when disabling VZWIMS. So when VZWIMS is MD changed APN
        // but disabling VZWIMS, follow AOSP logic to change IA.
        ApnSetting mdChangedAttachApn = mMdChangedAttachApn;
        if (mMdChangedAttachApn != null && getClassType(mMdChangedAttachApn) == APN_CLASS_1
                && !isMdChangedAttachApnEnabled()) {
            mdChangedAttachApn = null;
        }

        if (mdChangedAttachApn == null && mManualChangedAttachApn == null &&
                mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
            firstApnSetting = mAllApnSettings.get(0);
            log("setInitialApn: firstApnSetting=" + firstApnSetting);

            // Search for Initial APN setting and the first apn that can handle default
            for (ApnSetting apn : mAllApnSettings) {
                // Can't use apn.canHandleType(), as that returns true for APNs that have no type.
                if (ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_IA) &&
                        apn.carrierEnabled && checkIfDomesticInitialAttachApn(currentMcc)) {
                    // The Initial Attach APN is highest priority so use it if there is one
                    log("setInitialApn: iaApnSetting=" + apn);
                    iaApnSetting = apn;
                    if (ArrayUtils.contains(PLMN_EMPTY_APN_PCSCF_SET, operatorNumeric)) {
                        isIaApn = true;
                    }
                    break;
                } else if ((defaultApnSetting == null)
                        && (apn.canHandleType(PhoneConstants.APN_TYPE_DEFAULT))) {
                    // Use the first default apn if no better choice
                    log("setInitialApn: defaultApnSetting=" + apn);
                    defaultApnSetting = apn;
                }
            }
        }
        // M: end of change attach APN

        // The priority of apn candidates from highest to lowest is:
        //   1) APN_TYPE_IA (Initial Attach)
        //   2) mPreferredApn, i.e. the current preferred apn
        //   3) The first apn that than handle APN_TYPE_DEFAULT
        //   4) The first APN we can find.

        mInitialAttachApnSetting = null;
        // M: change attach APN for MD changed APN and handover to WIFI
        if (mManualChangedAttachApn != null) {
            log("setInitialAttachApn: using mManualChangedAttachApn");
            mInitialAttachApnSetting = mManualChangedAttachApn;
        } else if (mdChangedAttachApn != null) {
            log("setInitialAttachApn: using mMdChangedAttachApn");
            mInitialAttachApnSetting = mdChangedAttachApn;
        } else if (iaApnSetting != null) {
            if (DBG) log("setInitialAttachApn: using iaApnSetting");
            mInitialAttachApnSetting = iaApnSetting;
        } else if (mPreferredApn != null) {
            if (DBG) log("setInitialAttachApn: using mPreferredApn");
            mInitialAttachApnSetting = mPreferredApn;
        } else if (defaultApnSetting != null) {
            if (DBG) log("setInitialAttachApn: using defaultApnSetting");
            mInitialAttachApnSetting = defaultApnSetting;
        } else if (firstApnSetting != null) {
            if (DBG) log("setInitialAttachApn: using firstApnSetting");
            mInitialAttachApnSetting = firstApnSetting;
        }

        if (mInitialAttachApnSetting == null) {
            if (DBG) log("setInitialAttachApn: X There in no available apn, use empty");
            IaExtendParam param = new IaExtendParam(operatorNumeric, dualApnPlmnList,
                    RILConstants.SETUP_DATA_PROTOCOL_IPV4V6);
            mPhone.mCi.setInitialAttachApn("", RILConstants.SETUP_DATA_PROTOCOL_IPV4V6,
                    -1, "", "", (Object) param, null);
        } else {
            if (DBG) log("setInitialAttachApn: X selected Apn=" + mInitialAttachApnSetting);
            String iaApn = mInitialAttachApnSetting.apn;
            if (isIaApn) {
                if (DBG) log("setInitialAttachApn: ESM flag false, change IA APN to empty");
                iaApn = "";
            }

            Message msg = null;
            // M: JPN IA Start
            if (needsResumeModem) {
                if (DBG) log("setInitialAttachApn: DCM IA support");
                msg = obtainMessage(DctConstants.EVENT_SET_RESUME);
            }
            // M: JPN IA End
            String iaApnProtocol = mInitialAttachApnSetting.protocol;
            if (isOp18Sim()) {
                if (mPhone.getServiceState().getDataRoaming()) {
                    iaApnProtocol = mInitialAttachApnSetting.roamingProtocol;
                }
            }
            IaExtendParam param = new IaExtendParam(operatorNumeric,
                    mInitialAttachApnSetting.canHandleType(PhoneConstants.APN_TYPE_IMS),
                    dualApnPlmnList, mInitialAttachApnSetting.roamingProtocol);

            mPhone.mCi.setInitialAttachApn(iaApn, iaApnProtocol,
                    mInitialAttachApnSetting.authType, mInitialAttachApnSetting.user,
                    mInitialAttachApnSetting.password, (Object) param, msg);
        }
        if (DBG) log("setInitialAttachApn: new attach Apn [" + mInitialAttachApnSetting + "]");
    }

    /**
     * Handles changes to the APN database.
     */
    private void onApnChanged() {
        if (mPhone instanceof GsmCdmaPhone) {
            // The "current" may no longer be valid.  MMS depends on this to send properly. TBD
            ((GsmCdmaPhone)mPhone).updateCurrentCarrierInProvider();
        }

        /** M: onApnChanged optimization
         *  keep current settings before create new apn list
         */
        ArrayList<ApnSetting> prevAllApns = mAllApnSettings;
        ApnSetting prevPreferredApn = mPreferredApn;
        if (DBG) log("onApnChanged: createAllApnList and set initial attach APN");
        createAllApnList();

        ApnSetting previousAttachApn = mInitialAttachApnSetting;

        // check if EM force update APN
        if (SystemProperties.getInt(PROPERTY_FORCE_APN_CHANGE, 0) == 0) {
            /// M: we will do nothing if the apn is not changed or only the APN name
            /// is changed. Generally speaking, if PreferredApn and AttachApns are
            /// both not changed, it will be considered that APN not changed. But if both
            /// of them are not changed but any of them is null, then we double confirm it
            /// by compare preAllApns and curAllApns.
            /// VZW test case [SuppSig][02.17]: change APN name should trigger reattach
            boolean ignoreName = !VZW_FEATURE;

            final String prevPreferredApnString = (prevPreferredApn == null) ?
                    "" : prevPreferredApn.toStringIgnoreName(ignoreName);
            final String curPreferredApnString = (mPreferredApn == null) ?
                    "" : mPreferredApn.toStringIgnoreName(ignoreName);
            final String prevAttachApnSettingString = (previousAttachApn == null) ?
                    "" : previousAttachApn.toStringIgnoreName(ignoreName);
            final String curAttachApnSettingString = (mInitialAttachApnSetting == null) ?
                    "" : mInitialAttachApnSetting.toStringIgnoreName(ignoreName);
            if (TextUtils.equals(prevPreferredApnString, curPreferredApnString)
                    && TextUtils.equals(prevAttachApnSettingString, curAttachApnSettingString)) {
                // If preferred APN or preferred initial APN is null, we need to check all APNs.
                if ((prevPreferredApn == null || previousAttachApn == null) && !TextUtils.equals(
                        ApnSetting.toStringIgnoreNameForList(prevAllApns, ignoreName),
                        ApnSetting.toStringIgnoreNameForList(mAllApnSettings, ignoreName))) {
                    log("onApnChanged: all APN setting changed.");
                } else {
                    if (MTK_IMS_SUPPORT) {
                        if (isIMSApnSettingChanged(prevAllApns, mAllApnSettings)) {
                            sendOnApnChangedDone(true);
                            log("onApnChanged: IMS apn setting changed!!");
                            return;
                        }
                    }
                    log("onApnChanged: not changed");
                    return;
                }
            }
        }

        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        if (operator != null && operator.length() > 0) {
            // M: update initial attach APN for SVLTE since SVLTE use specific
            // APN for initial attach.
            setInitialAttachApn();
        } else {
            if (DBG) {
                log("onApnChanged: but no operator numeric");
            }
        }

        if (DBG) log("onApnChanged: cleanUpAllConnections and setup connectable APN");
        sendOnApnChangedDone(false);
    }

    private void sendOnApnChangedDone(boolean bImsApnChanged) {
        Message msg = obtainMessage(DctConstants.EVENT_APN_CHANGED_DONE);
        msg.arg1 = bImsApnChanged ? 1 : 0;
        sendMessage(msg);
    }

    private void onApnChangedDone() {
        //Fixed:[ALPS01670132] Data iocn cannot shows and data service cannot work
        //after change default APN some times.
        DctConstants.State overallState = getOverallState();
        boolean isDisconnected = (overallState == DctConstants.State.IDLE ||
                overallState == DctConstants.State.FAILED);

        // match the current operator.
        if (DBG) {
            log("onApnChanged: createAllApnList and cleanUpAllConnections: isDisconnected = "
                    + isDisconnected);
        }

        cleanUpConnectionsOnUpdatedApns(!isDisconnected);

        if (DBG) {
            log("onApnChanged: phone.getsubId=" + mPhone.getSubId()
                    + "getDefaultDataSubscriptionId()" +
                    + SubscriptionManager.getDefaultDataSubscriptionId());
        }
        // FIXME: See bug 17426028 maybe no conditional is needed.
        if (mPhone.getSubId() == SubscriptionManager.getDefaultDataSubscriptionId()) {
            setupDataOnConnectableApns(Phone.REASON_APN_CHANGED);
        }
    }

    /**
     * @param cid Connection id provided from RIL.
     * @return DataConnectionAc associated with specified cid.
     */
    private DcAsyncChannel findDataConnectionAcByCid(int cid) {
        for (DcAsyncChannel dcac : mDataConnectionAcHashMap.values()) {
            if (dcac.getCidSync() == cid) {
                return dcac;
            }
        }
        return null;
    }

    // TODO: For multiple Active APNs not exactly sure how to do this.
    private void gotoIdleAndNotifyDataConnection(String reason) {
        if (DBG) log("gotoIdleAndNotifyDataConnection: reason=" + reason);
        notifyDataConnection(reason);
    }

    /**
     * "Active" here means ApnContext isEnabled() and not in FAILED state
     * @param apnContext to compare with
     * @return true if higher priority active apn found
     */
    private boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        for (ApnContext otherContext : mPrioritySortedApnContexts) {
            if (apnContext.getApnType().equalsIgnoreCase(otherContext.getApnType())) return false;
            if (otherContext.isEnabled() && otherContext.getState() != DctConstants.State.FAILED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports if we support multiple connections or not.
     * This is a combination of factors, based on carrier and RAT.
     * @param rilRadioTech the RIL Radio Tech currently in use
     * @return true if only single DataConnection is allowed
     */
    private boolean isOnlySingleDcAllowed(int rilRadioTech) {
        int[] singleDcRats = mPhone.getContext().getResources().getIntArray(
                com.android.internal.R.array.config_onlySingleDcAllowed);
        boolean onlySingleDcAllowed = false;

        // MTK START [ALPS01540105]
        if (!BSP_PACKAGE && mTelephonyExt != null) {
            try {
                onlySingleDcAllowed = mTelephonyExt.isOnlySingleDcAllowed(); // default is false
                if (onlySingleDcAllowed == true) {
                    if (DBG) log("isOnlySingleDcAllowed: " + onlySingleDcAllowed);
                    return true;
                }
            } catch (Exception ex) {
                loge("Fail to create or use plug-in");
                ex.printStackTrace();
            }
        }
        // MTK END [ALPS01540105]

        if (MTK_DUALTALK_SPPORT) {
            if ((SystemProperties.getInt("ril.external.md", 0) - 1) == mPhone.getPhoneId()) {
                if (DBG) {
                    log("isOnlySingleDcAllowed: external modem");
                }
                return true;
            }
        }

        if (Build.IS_DEBUGGABLE &&
                SystemProperties.getBoolean("persist.telephony.test.singleDc", false)) {
            onlySingleDcAllowed = true;
        }
        if (singleDcRats != null) {
            for (int i=0; i < singleDcRats.length && onlySingleDcAllowed == false; i++) {
                if (rilRadioTech == singleDcRats[i]) onlySingleDcAllowed = true;
            }
        }

        if (DBG) log("isOnlySingleDcAllowed(" + rilRadioTech + "): " + onlySingleDcAllowed);
        return onlySingleDcAllowed;
    }

    void sendRestartRadio() {
        if (DBG)log("sendRestartRadio:");
        Message msg = obtainMessage(DctConstants.EVENT_RESTART_RADIO);
        sendMessage(msg);
    }

    private void restartRadio() {
        if (DBG) log("restartRadio: ************TURN OFF RADIO**************");
        cleanUpAllConnections(true, Phone.REASON_RADIO_TURNED_OFF);
        mPhone.getServiceStateTracker().powerOffRadioSafely(this);
        /* Note: no need to call setRadioPower(true).  Assuming the desired
         * radio power state is still ON (as tracked by ServiceStateTracker),
         * ServiceStateTracker will call setRadioPower when it receives the
         * RADIO_STATE_CHANGED notification for the power off.  And if the
         * desired power state has changed in the interim, we don't want to
         * override it with an unconditional power on.
         */

        int reset = Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", "0"));
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(reset + 1));
    }

    /**
     * Return true if data connection need to be setup after disconnected due to
     * reason.
     *
     * @param apnContext APN context
     * @return true if try setup data connection is need for this reason
     */
    private boolean retryAfterDisconnected(ApnContext apnContext) {
        boolean retry = true;
        String reason = apnContext.getReason();

        if ( Phone.REASON_RADIO_TURNED_OFF.equals(reason) ||
                Phone.REASON_FDN_ENABLED.equals(reason) ||
                (isOnlySingleDcAllowed(mPhone.getServiceState().getRilDataRadioTechnology())
                 && isHigherPriorityApnContextActive(apnContext))) {
            retry = false;
        }
        return retry;
    }

    private void startAlarmForReconnect(long delay, ApnContext apnContext) {
        String apnType = apnContext.getApnType();

        Intent intent = new Intent(INTENT_RECONNECT_ALARM + "." + apnType);
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, apnContext.getReason());
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, apnType);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        // Get current sub id.
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);

        if (DBG) {
            log("startAlarmForReconnect: delay=" + delay + " action=" + intent.getAction()
                    + " apn=" + apnContext);
        }

        PendingIntent alarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0,
                                        intent, PendingIntent.FLAG_UPDATE_CURRENT);
        apnContext.setReconnectIntent(alarmIntent);

        // Use the exact timer instead of the inexact one to provide better user experience.
        // In some extreme cases, we saw the retry was delayed for few minutes.
        // Note that if the stated trigger time is in the past, the alarm will be triggered
        // immediately.
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, alarmIntent);
    }

    private void notifyNoData(DcFailCause lastFailCauseCode,
                              ApnContext apnContext) {
        if (DBG) log( "notifyNoData: type=" + apnContext.getApnType());
        if (isPermanentFail(lastFailCauseCode)
            && (!apnContext.getApnType().equals(PhoneConstants.APN_TYPE_DEFAULT))) {
            mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
    }

    public boolean getAutoAttachOnCreation() {
        return mAutoAttachOnCreation.get();
    }

    private void onRecordsLoadedOrSubIdChanged() {
        if (DBG) log("onRecordsLoadedOrSubIdChanged: createAllApnList");
        mAutoAttachOnCreationConfig = mPhone.getContext().getResources()
                .getBoolean(com.android.internal.R.bool.config_auto_attach_data_on_creation);

        //M: resolve tethering on and after flight mode off, rild initial will auto-start FD
        if (mFdMgr != null) {
            mFdMgr.disableFdWhenTethering();
        }
        // M: cc33.
        if (MTK_CC33_SUPPORT) {
            mPhone.mCi.setRemoveRestrictEutranMode(true, null);
            mPhone.mCi.setDataOnToMD(mUserDataEnabled, null);
        }
        // M: VzW
        syncDataSettingsToMd(getDataEnabled(), getDataOnRoamingEnabled());

        // reset FDN check flag
        mIsFdnChecked = false;

        createAllApnList();
        setInitialAttachApn();
        if (mPhone.mCi.getRadioState().isOn()) {
            if (DBG) log("onRecordsLoadedOrSubIdChanged: notifying data availability");
            notifyOffApnsOfAvailability(Phone.REASON_SIM_LOADED);
        }

        boolean bGetDataCallList = true;
        if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            bGetDataCallList = false;
        }

        if (bGetDataCallList) {
            mDcc.getDataCallListForSimLoaded();
        } else {
            sendMessage(obtainMessage(DctConstants.EVENT_SETUP_DATA_WHEN_LOADED));
        }
    }

    //MTK START: FDN Support
    private static final String FDN_CONTENT_URI = "content://icc/fdn";
    private static final String FDN_CONTENT_URI_WITH_SUB_ID = "content://icc/fdn/subId/";
    private static final String FDN_FOR_ALLOW_DATA = "*99#";
    private boolean mIsFdnChecked = false;
    private boolean mIsMatchFdnForAllowData = false;
    private boolean mIsPhbStateChangedIntentRegistered = false;
    private BroadcastReceiver mPhbStateChangedIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) {
                log("onReceive: action=" + action);
            }
            if (action.equals(TelephonyIntents.ACTION_PHB_STATE_CHANGED)) {
                boolean bPhbReady = intent.getBooleanExtra("ready", false);
                if (DBG) {
                    log("bPhbReady: " + bPhbReady);
                }
                if (bPhbReady) {
                    onFdnChanged();
                }
            }
        }
    };

    private void registerFdnContentObserver() {
        Uri fdnContentUri;
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            fdnContentUri = Uri.parse(FDN_CONTENT_URI);
        } else {
            fdnContentUri = Uri.parse(FDN_CONTENT_URI_WITH_SUB_ID + mPhone.getSubId());
        }
        mSettingsObserver.observe(fdnContentUri, DctConstants.EVENT_FDN_CHANGED);
    }

    private boolean isFdnEnableSupport() {
        boolean isFdnEnableSupport = false;
        if (!BSP_PACKAGE && mGsmDctExt != null) {
            isFdnEnableSupport = mGsmDctExt.isFdnEnableSupport();
        }
        return isFdnEnableSupport;
    }

    private boolean isFdnEnabled() {
        boolean bFdnEnabled = false;
        if (isFdnEnableSupport()) {
            ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            if (telephonyEx != null) {
                try {
                    bFdnEnabled = telephonyEx.isFdnEnabled(mPhone.getSubId());
                    log("isFdnEnabled(), bFdnEnabled = " + bFdnEnabled);
                    if (bFdnEnabled) {
                        if (mIsFdnChecked) {
                            log("isFdnEnabled(), match FDN for allow data = "
                                    + mIsMatchFdnForAllowData);
                            return !mIsMatchFdnForAllowData;
                        } else {
                            boolean bPhbReady = telephonyEx.isPhbReady(mPhone.getSubId());
                            log("isFdnEnabled(), bPhbReady = " + bPhbReady);
                            if (bPhbReady) {
                                mWorkerHandler.sendEmptyMessage(DctConstants.EVENT_CHECK_FDN_LIST);
                            } else if (!mIsPhbStateChangedIntentRegistered) {
                                IntentFilter filter = new IntentFilter();
                                filter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
                                mPhone.getContext().registerReceiver(
                                        mPhbStateChangedIntentReceiver, filter);
                                mIsPhbStateChangedIntentRegistered = true;
                            }
                        }
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            } else {
                loge("isFdnEnabled(), get telephonyEx failed!!");
            }
        }
        return bFdnEnabled;
    }

    private void onFdnChanged() {
        if (isFdnEnableSupport()) {
            log("onFdnChanged()");
            boolean bFdnEnabled = false;
            boolean bPhbReady = false;

            ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            if (telephonyEx != null) {
                try {
                    bFdnEnabled = telephonyEx.isFdnEnabled(mPhone.getSubId());
                    bPhbReady = telephonyEx.isPhbReady(mPhone.getSubId());
                    log("onFdnChanged(), bFdnEnabled = " + bFdnEnabled
                            + ", bPhbReady = " + bPhbReady);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            } else {
                loge("onFdnChanged(), get telephonyEx failed!!");
            }

            if (bPhbReady) {
                if (bFdnEnabled) {
                    log("fdn enabled, check fdn list");
                    mWorkerHandler.sendEmptyMessage(DctConstants.EVENT_CHECK_FDN_LIST);
                } else {
                    log("fdn disabled, call setupDataOnConnectableApns()");
                    setupDataOnConnectableApns(Phone.REASON_FDN_DISABLED);
                }
            } else if (!mIsPhbStateChangedIntentRegistered) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
                mPhone.getContext().registerReceiver(
                        mPhbStateChangedIntentReceiver, filter);
                mIsPhbStateChangedIntentRegistered = true;
            }
        } else {
            log("not support fdn enabled, skip onFdnChanged");
        }
    }

    private void cleanOrSetupDataConnByCheckFdn() {
        log("cleanOrSetupDataConnByCheckFdn()");

        Uri uriFdn;
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            uriFdn = Uri.parse(FDN_CONTENT_URI);
        } else {
            uriFdn = Uri.parse(FDN_CONTENT_URI_WITH_SUB_ID + mPhone.getSubId());
        }
        ContentResolver cr = mPhone.getContext().getContentResolver();
        Cursor cursor = cr.query(uriFdn, new String[] { "number" }, null, null, null);

        mIsMatchFdnForAllowData = false;
        if (cursor != null) {
            mIsFdnChecked = true;
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        String strFdnNumber = cursor.getString(
                                cursor.getColumnIndexOrThrow("number"));
                        log("strFdnNumber = " + strFdnNumber);
                        if (strFdnNumber.equals(FDN_FOR_ALLOW_DATA)) {
                            mIsMatchFdnForAllowData = true;
                            break;
                        }
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        if (mIsMatchFdnForAllowData) {
            log("match FDN for allow data, call setupDataOnConnectableApns(REASON_FDN_DISABLED)");
            setupDataOnConnectableApns(Phone.REASON_FDN_DISABLED);
        } else {
            log("not match FDN for allow data, call cleanUpAllConnections(REASON_FDN_ENABLED)");
            cleanUpAllConnections(true, Phone.REASON_FDN_ENABLED);
        }
    }
    //MTK END: Support FDN

    private void applyUnProvisionedSimDetected() {
        if(isColdSimDetected()) {
            if(!mColdSimDetected) {
                if(DBG) {
                    log("onColdSimDetected: cleanUpAllDataConnections");
                }
                cleanUpAllConnections(null);
                //send otasp_sim_unprovisioned so that SuW is able to proceed and notify users
                mPhone.notifyOtaspChanged(ServiceStateTracker.OTASP_SIM_UNPROVISIONED);
                mColdSimDetected = true;
            }
        } else if (isOutOfCreditSimDetected()) {
            if(!mOutOfCreditSimDetected) {
                if(DBG) {
                    log("onOutOfCreditSimDetected on subId: re-establish data connection");
                }
                for (ApnContext context : redirectApnContextSet) {
                    onTrySetupData(context);
                    redirectApnContextSet.remove(context);
                }
                mOutOfCreditSimDetected = true;
            }
        } else {
            if (DBG) log("Provisioned Sim Detected on subId: " + mPhone.getSubId() );
            mColdSimDetected = false;
            mOutOfCreditSimDetected = false;
        }
    }

    private void onSimNotReady() {
        if (DBG) log("onSimNotReady");

        cleanUpAllConnections(true, Phone.REASON_SIM_NOT_READY);
        if (mAllApnSettings != null) {
            mAllApnSettings.clear();
        }
        mAutoAttachOnCreationConfig = false;
    }

    private void onSetDependencyMet(String apnType, boolean met) {
        // don't allow users to tweak hipri to work around default dependency not met
        if (PhoneConstants.APN_TYPE_HIPRI.equals(apnType)) return;

        ApnContext apnContext = mApnContexts.get(apnType);
        if (apnContext == null) {
            loge("onSetDependencyMet: ApnContext not found in onSetDependencyMet(" +
                    apnType + ", " + met + ")");
            return;
        }
        applyNewState(apnContext, apnContext.isEnabled(), met);
        if (PhoneConstants.APN_TYPE_DEFAULT.equals(apnType)) {
            // tie actions on default to similar actions on HIPRI regarding dependencyMet
            apnContext = mApnContexts.get(PhoneConstants.APN_TYPE_HIPRI);
            if (apnContext != null) applyNewState(apnContext, apnContext.isEnabled(), met);
        }
    }

    private void onSetPolicyDataEnabled(boolean enabled) {
        synchronized (mDataEnabledLock) {
            final boolean prevEnabled = getAnyDataEnabled();
            if (sPolicyDataEnabled != enabled) {
                sPolicyDataEnabled = enabled;
                if (prevEnabled != getAnyDataEnabled()) {
                    if (!prevEnabled) {
                        onTrySetupData(Phone.REASON_DATA_ENABLED);
                    } else {
                        onCleanUpAllConnections(Phone.REASON_DATA_SPECIFIC_DISABLED);
                    }
                }
            }
        }
    }


    private void applyNewState(ApnContext apnContext, boolean enabled, boolean met) {
        boolean cleanup = false;
        boolean trySetup = false;
        String str ="applyNewState(" + apnContext.getApnType() + ", " + enabled +
                "(" + apnContext.isEnabled() + "), " + met + "(" +
                apnContext.getDependencyMet() +"))";
        if (DBG) log(str);
        apnContext.requestLog(str);

        if (apnContext.isReady()) {
            cleanup = true;
            if (enabled && met) {
                DctConstants.State state = apnContext.getState();
                switch(state) {
                    case CONNECTING:
                    case SCANNING:
                    case CONNECTED:
                    case DISCONNECTING:
                        // We're "READY" and active so just return
                        if (DBG) log("applyNewState: 'ready' so return");
                        apnContext.requestLog("applyNewState state=" + state + ", so return");
                        return;
                    case IDLE:
                        // fall through: this is unexpected but if it happens cleanup and try setup
                    case FAILED:
                    case RETRYING: {
                        // We're "READY" but not active so disconnect (cleanup = true) and
                        // connect (trySetup = true) to be sure we retry the connection.
                        trySetup = true;
                        apnContext.setReason(Phone.REASON_DATA_ENABLED);
                        break;
                    }
                }
            //TODO: Need handle dependency met and data not enable case
            } else if (!enabled) {
                cleanup = true;
                apnContext.setReason(Phone.REASON_DATA_DISABLED);
/*
            } else if (met) {
                apnContext.setReason(Phone.REASON_DATA_DISABLED);
                // If ConnectivityService has disabled this network, stop trying to bring
                // it up, but do not tear it down - ConnectivityService will do that
                // directly by talking with the DataConnection.
                //
                // This doesn't apply to DUN, however.  Those connections have special
                // requirements from carriers and we need stop using them when the dun
                // request goes away.  This applies to both CDMA and GSM because they both
                // can declare the DUN APN sharable by default traffic, thus still satisfying
                // those requests and not torn down organically.
                if (apnContext.getApnType() == PhoneConstants.APN_TYPE_DUN && teardownForDun()) {
                    cleanup = true;
                } else {
                    cleanup = false;
                }
*/
            } else {
                apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_UNMET);
            }
        } else {
            if (enabled && met) {
                if (apnContext.isEnabled()) {
                    apnContext.setReason(Phone.REASON_DATA_DEPENDENCY_MET);
                } else {
                    apnContext.setReason(Phone.REASON_DATA_ENABLED);
                }
                if (apnContext.getState() == DctConstants.State.FAILED) {
                    apnContext.setState(DctConstants.State.IDLE);
                }
                trySetup = true;
            }
        }
        apnContext.setEnabled(enabled);
        apnContext.setDependencyMet(met);
        if (cleanup) cleanUpConnection(true, apnContext);
        if (trySetup) {
            apnContext.resetErrorCodeRetries();
            trySetupData(apnContext);
        }
    }

    private DcAsyncChannel checkForCompatibleConnectedApnContext(ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        ApnSetting dunSetting = null;
        boolean bIsRequestApnTypeEmergency =
                (PhoneConstants.APN_TYPE_EMERGENCY.equals(apnType)) ? true : false;

        if (PhoneConstants.APN_TYPE_DUN.equals(apnType)) {
            dunSetting = fetchDunApn();
        }
        if (DBG) {
            log("checkForCompatibleConnectedApnContext: apnContext=" + apnContext );
        }

        DcAsyncChannel potentialDcac = null;
        ApnContext potentialApnCtx = null;
        for (ApnContext curApnCtx : mApnContexts.values()) {
            DcAsyncChannel curDcac = curApnCtx.getDcAc();
            if (curDcac != null) {
                ApnSetting apnSetting = curApnCtx.getApnSetting();
                log("apnSetting: " + apnSetting);
                if (dunSetting != null) {
                    if (dunSetting.equals(apnSetting)) {
                        switch (curApnCtx.getState()) {
                            case CONNECTED:
                                if (DBG) {
                                    log("checkForCompatibleConnectedApnContext:"
                                            + " found dun conn=" + curDcac
                                            + " curApnCtx=" + curApnCtx);
                                }
                                return curDcac;
                            case RETRYING:
                            case CONNECTING:
                            case SCANNING:
                                potentialDcac = curDcac;
                                potentialApnCtx = curApnCtx;
                            default:
                                // Not connected, potential unchanged
                                break;
                        }
                    }
                } else if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                    boolean bIsSkip = false;
                    if (bIsRequestApnTypeEmergency) {
                        if (!PhoneConstants.APN_TYPE_EMERGENCY.equals(curApnCtx.getApnType())) {
                            if (DBG) {
                                log("checkForCompatibleConnectedApnContext:"
                                        + " found canHandle conn=" + curDcac
                                        + " curApnCtx=" + curApnCtx
                                        + ", but not emergency type (skip)");
                            }
                            bIsSkip = true;
                        }
                    }
                    switch (curApnCtx.getState()) {
                        case CONNECTED:
                            if (bIsSkip) break;
                            if (DBG) {
                                log("checkForCompatibleConnectedApnContext:"
                                        + " found canHandle conn=" + curDcac
                                        + " curApnCtx=" + curApnCtx);
                            }
                            return curDcac;
                        case RETRYING:
                        case CONNECTING:
                        case SCANNING:
                            if (bIsSkip) break;
                            potentialDcac = curDcac;
                            potentialApnCtx = curApnCtx;
                        default:
                            // Not connected, potential unchanged
                            break;
                    }
                }
            } else {
                if (VDBG) {
                    log("checkForCompatibleConnectedApnContext: not conn curApnCtx=" + curApnCtx);
                }
            }
        }
        if (potentialDcac != null) {
            if (DBG) {
                log("checkForCompatibleConnectedApnContext: found potential conn=" + potentialDcac
                        + " curApnCtx=" + potentialApnCtx);
            }
            return potentialDcac;
        }

        if (DBG) log("checkForCompatibleConnectedApnContext: NO conn apnContext=" + apnContext);
        return null;
    }

    public void setEnabled(int id, boolean enable) {
        Message msg = obtainMessage(DctConstants.EVENT_ENABLE_NEW_APN);
        msg.arg1 = id;
        msg.arg2 = (enable ? DctConstants.ENABLED : DctConstants.DISABLED);
        sendMessage(msg);
    }

    private void onEnableApn(int apnId, int enabled) {
        ApnContext apnContext = mApnContextsById.get(apnId);
        if (apnContext == null) {
            loge("onEnableApn(" + apnId + ", " + enabled + "): NO ApnContext");
            return;
        }
        // TODO change our retry manager to use the appropriate numbers for the new APN
        if (DBG) log("onEnableApn: apnContext=" + apnContext + " call applyNewState");
        applyNewState(apnContext, enabled == DctConstants.ENABLED, apnContext.getDependencyMet());
    }

    // TODO: We shouldnt need this.
    private boolean onTrySetupData(String reason) {
        if (DBG) log("onTrySetupData: reason=" + reason);
        setupDataOnConnectableApns(reason);
        return true;
    }

    private boolean onTrySetupData(ApnContext apnContext) {
        if (DBG) log("onTrySetupData: apnContext=" + apnContext);
        return trySetupData(apnContext);
    }

    /**
     * Return current {@link android.provider.Settings.Global#MOBILE_DATA} value.
     */
    public boolean getDataEnabled() {
        final int device_provisioned =
                Settings.Global.getInt(mResolver, Settings.Global.DEVICE_PROVISIONED, 0);

        boolean retVal = "true".equalsIgnoreCase(SystemProperties.get(
                "ro.com.android.mobiledata", "true"));
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            retVal = Settings.Global.getInt(mResolver, Settings.Global.MOBILE_DATA,
                    retVal ? 1 : 0) != 0;
        } else {
            int phoneSubId = mPhone.getSubId();
            if (VDBG) log("getDataEnabled: phoneSubId = " + phoneSubId);

            try {
                retVal = TelephonyManager.getIntWithSubId(mResolver,
                        Settings.Global.MOBILE_DATA, phoneSubId) != 0;
            } catch (SettingNotFoundException e) {
                if (OPERATOR_OM.equals(SystemProperties.get(PROPERTY_OPERATOR, OPERATOR_OM))) {
                    // Follow AOSP, use existing retVal
                } else {
                    if (!SubscriptionManager.isValidSubscriptionId(phoneSubId)
                            && !(TelephonyManager.getDefault().getSimCount() == 1)) {
                        if (DBG) {
                            log("invalid sub id, return data disabled");
                        }
                        return false;
                    }
                    // Not found the 'MOBILE_DATA+phoneSubId' setting, we should initialize it.
                    retVal = handleMobileDataSettingNotFound(retVal);
                }
            }
        }
        if (VDBG) log("getDataEnabled: retVal=" + retVal);
        if (device_provisioned == 0) {
            // device is still getting provisioned - use whatever setting they
            // want during this process
            //
            // use the normal data_enabled setting (retVal, determined above)
            // as the default if nothing else is set
            final String prov_property = SystemProperties.get("ro.com.android.prov_mobiledata",
                  retVal ? "true" : "false");
            retVal = "true".equalsIgnoreCase(prov_property);

            final int prov_mobile_data = Settings.Global.getInt(mResolver,
                    Settings.Global.DEVICE_PROVISIONING_MOBILE_DATA_ENABLED,
                    retVal ? 1 : 0);
            retVal = prov_mobile_data != 0;
            log("getDataEnabled during provisioning retVal=" + retVal + " - (" + prov_property +
                    ", " + prov_mobile_data + ")");
        }

        return retVal;
    }

    private boolean handleMobileDataSettingNotFound(boolean retVal) {
        log("handleMobileDataSettingNotFound: initial retVal=" + retVal);

        int phoneSubId = mPhone.getSubId();
        if (!SubscriptionManager.isValidSubscriptionId(phoneSubId)) {
            log("invalid sub id, return data disabled");
            return false;
        }

        int insertedStatus = 0;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (!NO_SIM_VALUE.equals(SystemProperties.get(PROPERTY_ICCID[i]))) {
                insertedStatus = insertedStatus | (1 << i);
            }
        }
        log("insertedStatus = " + insertedStatus);

        //retVal = Settings.Global.getInt(mResolver, Settings.Global.MOBILE_DATA,
        //        retVal ? 1 : 0) != 0;

        if (!retVal) {
            setUserDataProperty(false);
            Settings.Global.putInt(mResolver, Settings.Global.MOBILE_DATA + phoneSubId, 0);
        } else { // OP02 will have default value of MOBILE_DATA as true
            int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
            log("defaultDataSubId = " + defaultDataSubId);
            if (defaultDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                // 'MOTA upgrade' will go this way
                if (phoneSubId == defaultDataSubId) {
                    setUserDataProperty(true);
                    Settings.Global.putInt(mResolver,
                            Settings.Global.MOBILE_DATA + phoneSubId, 1);
                    retVal = true;
                } else {
                    setUserDataProperty(false);
                    Settings.Global.putInt(mResolver,
                            Settings.Global.MOBILE_DATA + phoneSubId, 0);
                    retVal = false;
                }
            } else {
                if (insertedStatus == 1 || insertedStatus == 3) {
                    if (mPhone.getPhoneId() == 0) {
                        setUserDataProperty(true);
                        Settings.Global.putInt(mResolver,
                                Settings.Global.MOBILE_DATA + phoneSubId, 1);
                        retVal = true;
                    } else {
                        setUserDataProperty(false);
                        Settings.Global.putInt(mResolver,
                                Settings.Global.MOBILE_DATA + phoneSubId, 0);
                        retVal = false;
                    }
                } else if (insertedStatus == 2) {
                    if (mPhone.getPhoneId() == 1) {
                        setUserDataProperty(true);
                        Settings.Global.putInt(mResolver,
                                Settings.Global.MOBILE_DATA + phoneSubId, 1);
                        retVal = true;
                    } else {
                        setUserDataProperty(false);
                        Settings.Global.putInt(mResolver,
                                Settings.Global.MOBILE_DATA + phoneSubId, 0);
                        retVal = false;
                    }
                }
            }
        }

        log("handleMobileDataSettingNotFound: after retVal=" + retVal);
        return retVal;
    }

    /**
     * Modify {@link android.provider.Settings.Global#DATA_ROAMING} value.
     */
    public void setDataOnRoamingEnabled(boolean enabled) {
        final int phoneSubId = mPhone.getSubId();
        if (getDataOnRoamingEnabled() != enabled) {
            int roaming = enabled ? 1 : 0;

            // For single SIM phones, this is a per phone property.
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                Settings.Global.putInt(mResolver, Settings.Global.DATA_ROAMING, roaming);
            } else {
                Settings.Global.putInt(mResolver, Settings.Global.DATA_ROAMING +
                         phoneSubId, roaming);
            }

            // M: VzW
            syncDataSettingsToMd(getDataEnabled(), enabled);

            mSubscriptionManager.setDataRoaming(roaming, phoneSubId);
            // will trigger handleDataOnRoamingChange() through observer
            if (DBG) {
               log("setDataOnRoamingEnabled: set phoneSubId=" + phoneSubId
                       + " isRoaming=" + enabled);
            }
        } else {
            if (DBG) {
                log("setDataOnRoamingEnabled: unchanged phoneSubId=" + phoneSubId
                        + " isRoaming=" + enabled);
             }
        }
    }

    /**
     * Return current {@link android.provider.Settings.Global#DATA_ROAMING} value.
     */
    public boolean getDataOnRoamingEnabled() {
        boolean isDataRoamingEnabled = "true".equalsIgnoreCase(SystemProperties.get(
                "ro.com.android.dataroaming", "false"));
        final int phoneSubId = mPhone.getSubId();

        try {
            // For single SIM phones, this is a per phone property.
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                isDataRoamingEnabled = Settings.Global.getInt(mResolver,
                        Settings.Global.DATA_ROAMING, isDataRoamingEnabled ? 1 : 0) != 0;
            } else {
                isDataRoamingEnabled = TelephonyManager.getIntWithSubId(mResolver,
                        Settings.Global.DATA_ROAMING, phoneSubId) != 0;
            }
        } catch (SettingNotFoundException snfe) {
            if (DBG) log("getDataOnRoamingEnabled: SettingNofFoundException snfe=" + snfe);
        }
        if (VDBG) {
            log("getDataOnRoamingEnabled: phoneSubId=" + phoneSubId +
                    " isDataRoamingEnabled=" + isDataRoamingEnabled);
        }
        return isDataRoamingEnabled;
    }

    private boolean ignoreDataRoaming(String apnType) {
        boolean ignoreDataRoaming = false;
        try {
            ignoreDataRoaming = mTelephonyExt.ignoreDataRoaming(apnType);
        } catch (Exception e) {
            loge("get ignoreDataRoaming fail!");
            e.printStackTrace();
        }

        // Telenor requirement: MMS/XCAP should over ePDG in roaming state even roaming data is off.
        // Prop gsm.wfc.status is set by NW (based on +CGREG event)
        if (mDcFcMgr != null && mDcFcMgr.isSpecificSimOperator(DcFailCauseManager.Operator.OP156)) {
            boolean isOverEpdg = SystemProperties.get("gsm.wfc.status").equals("99") ? true : false;
            log("ignoreDataRoaming: OP156 check apnType = " + apnType + ", Epdg=" + isOverEpdg);
            if (isOverEpdg && (apnType.equals(PhoneConstants.APN_TYPE_MMS) ||
                    apnType.equals(PhoneConstants.APN_TYPE_XCAP))) {
                ignoreDataRoaming = true;
            }
        }

        if (ignoreDataRoaming) {
            log("ignoreDataRoaming: " + ignoreDataRoaming + ", apnType = " + apnType);
        }
        return ignoreDataRoaming;
    }

    private boolean ignoreDefaultDataUnselected(String apnType) {
        boolean ignoreDefaultDataUnselected = false;

        try {
            ignoreDefaultDataUnselected = mTelephonyExt.ignoreDefaultDataUnselected(apnType);
        } catch (Exception e) {
            loge("get ignoreDefaultDataUnselected fail!");
            e.printStackTrace();
        }

        // M: Vsim
        if (!ignoreDefaultDataUnselected
                && TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DEFAULT)
                && isVsimActive(mPhone.getPhoneId())) {
            if (DBG) {
                log("Vsim is enabled, set ignoreDefaultDataUnselected as true");
            }
            ignoreDefaultDataUnselected = true;
        }

        if (ignoreDefaultDataUnselected) {
            log("ignoreDefaultDataUnselected: " + ignoreDefaultDataUnselected
                    + ", apnType = " + apnType);
        }
        return ignoreDefaultDataUnselected;
    }

    private void onRoamingOff() {
        boolean bDataOnRoamingEnabled = getDataOnRoamingEnabled();

        if (DBG) {
            log("onRoamingOff bDataOnRoamingEnabled=" + bDataOnRoamingEnabled
                    + ", mUserDataEnabled=" + mUserDataEnabled);
        }
        if (!mUserDataEnabled) return;
        if (isOp18Sim()) {
            setInitialAttachApn();
        }
        if (!bDataOnRoamingEnabled) {
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_OFF);
            setupDataOnConnectableApns(Phone.REASON_ROAMING_OFF);
        } else {
            notifyDataConnection(Phone.REASON_ROAMING_OFF);
        }
    }

    private void onRoamingOn() {
        boolean bDataOnRoamingEnabled = getDataOnRoamingEnabled();

        if (DBG) {
            log("onRoamingOn bDataOnRoamingEnabled=" + bDataOnRoamingEnabled
                    + ", mUserDataEnabled=" + mUserDataEnabled);
        }

        if (!mUserDataEnabled) {
            if (DBG) log("data not enabled by user");
            return;
        }
        if (isOp18Sim()) {
            setInitialAttachApn();
        }
        // Check if the device is actually data roaming
        if (!mPhone.getServiceState().getDataRoaming()) {
            if (DBG) log("device is not roaming. ignored the request.");
            return;
        }

        if (bDataOnRoamingEnabled) {
            if (DBG) log("onRoamingOn: setup data on roaming");
            setupDataOnConnectableApns(Phone.REASON_ROAMING_ON);
            notifyDataConnection(Phone.REASON_ROAMING_ON);
        } else {
            if (DBG) log("onRoamingOn: Tear down data connection on roaming.");
            cleanUpAllConnections(true, Phone.REASON_ROAMING_ON);
            notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
        }
    }

    private void onRadioAvailable() {
        if (DBG) log("onRadioAvailable");
        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            // setState(DctConstants.State.CONNECTED);
            notifyDataConnection(null);

            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }

        IccRecords r = mIccRecords.get();
        if (r != null && r.getRecordsLoaded()) {
            notifyOffApnsOfAvailability(null);
        }

        if (getOverallState() != DctConstants.State.IDLE) {
            cleanUpConnection(true, null);
        }
    }

    private void onRadioOffOrNotAvailable() {
        // Make sure our reconnect delay starts at the initial value
        // next time the radio comes on

        mReregisterOnReconnectFailure = false;

        // M: To avoid trying setup data call before PS attach.
        mAutoAttachOnCreation.set(false);

        if (mPhone.getSimulatedRadioControl() != null) {
            // Assume data is connected on the simulator
            // FIXME  this can be improved
            log("We're on the simulator; assuming radio off is meaningless");
            notifyOffApnsOfAvailability(null);
        } else {
            if (DBG) log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, Phone.REASON_RADIO_TURNED_OFF);
        }
        //ALPS01769896: We don't notify off twice.
        //notifyOffApnsOfAvailability(null);
    }

    private void completeConnection(ApnContext apnContext) {

        if (DBG) log("completeConnection: successful, notify the world apnContext=" + apnContext);

        if (mIsProvisioning && !TextUtils.isEmpty(mProvisioningUrl)) {
            if (DBG) {
                log("completeConnection: MOBILE_PROVISIONING_ACTION url="
                        + mProvisioningUrl);
            }
            Intent newIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_BROWSER);
            newIntent.setData(Uri.parse(mProvisioningUrl));
            newIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mPhone.getContext().startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                loge("completeConnection: startActivityAsUser failed" + e);
            }
        }
        mIsProvisioning = false;
        mProvisioningUrl = null;
        if (mProvisioningSpinner != null) {
            sendMessage(obtainMessage(DctConstants.CMD_CLEAR_PROVISIONING_SPINNER,
                    mProvisioningSpinner));
        }

        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        startNetStatPoll();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
    }

    /**
     * A SETUP (aka bringUp) has completed, possibly with an error. If
     * there is an error this method will call {@link #onDataSetupCompleteError}.
     */
    private void onDataSetupComplete(AsyncResult ar) {

        DcFailCause cause = DcFailCause.UNKNOWN;
        boolean handleError = false;
        ApnContext apnContext = getValidApnContext(ar, "onDataSetupComplete");

        if (apnContext == null) return;

        if (ar.exception == null) {
            DcAsyncChannel dcac = apnContext.getDcAc();

            if (RADIO_TESTS) {
                // Note: To change radio.test.onDSC.null.dcac from command line you need to
                // adb root and adb remount and from the command line you can only change the
                // value to 1 once. To change it a second time you can reboot or execute
                // adb shell stop and then adb shell start. The command line to set the value is:
                // adb shell sqlite3 /data/data/com.android.providers.settings/databases/settings.db "insert into system (name,value) values ('radio.test.onDSC.null.dcac', '1');"
                ContentResolver cr = mPhone.getContext().getContentResolver();
                String radioTestProperty = "radio.test.onDSC.null.dcac";
                if (Settings.System.getInt(cr, radioTestProperty, 0) == 1) {
                    log("onDataSetupComplete: " + radioTestProperty +
                            " is true, set dcac to null and reset property to false");
                    dcac = null;
                    Settings.System.putInt(cr, radioTestProperty, 0);
                    log("onDataSetupComplete: " + radioTestProperty + "=" +
                            Settings.System.getInt(mPhone.getContext().getContentResolver(),
                                    radioTestProperty, -1));
                }
            }
            if (dcac == null) {
                log("onDataSetupComplete: no connection to DC, handle as error");
                cause = DcFailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
                // M: Bug fixed.
                apnContext.setState(DctConstants.State.FAILED);
                handleError = true;
            } else {
                ApnSetting apn = apnContext.getApnSetting();
                if (DBG) {
                    log("onDataSetupComplete: success apn=" + (apn == null ? "unknown" : apn.apn));
                }
                if (apn != null && apn.proxy != null && apn.proxy.length() != 0) {
                    try {
                        String port = apn.port;
                        if (TextUtils.isEmpty(port)) port = "8080";
                        ProxyInfo proxy = new ProxyInfo(apn.proxy,
                                Integer.parseInt(port), null);
                        dcac.setLinkPropertiesHttpProxySync(proxy);
                    } catch (NumberFormatException e) {
                        loge("onDataSetupComplete: NumberFormatException making ProxyProperties (" +
                                apn.port + "): " + e);
                    }
                }

                // everything is setup
                if(TextUtils.equals(apnContext.getApnType(),PhoneConstants.APN_TYPE_DEFAULT)) {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "true");
                    } catch (RuntimeException ex) {
                        log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to true");
                    }
                    if (mCanSetPreferApn && mPreferredApn == null) {
                        if (DBG) log("onDataSetupComplete: PREFERRED APN is null");
                        mPreferredApn = apn;
                        if (mPreferredApn != null) {
                            setPreferredApn(mPreferredApn.id);
                        }
                    }
                } else {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                    } catch (RuntimeException ex) {
                        log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                    }
                }

                // A connection is setup
                apnContext.setState(DctConstants.State.CONNECTED);

                boolean isProvApn = apnContext.isProvisioningApn();
                final ConnectivityManager cm = ConnectivityManager.from(mPhone.getContext());
                if (mProvisionBroadcastReceiver != null) {
                    mPhone.getContext().unregisterReceiver(mProvisionBroadcastReceiver);
                    mProvisionBroadcastReceiver = null;
                }
                if ((!isProvApn) || mIsProvisioning) {
                    // Hide any provisioning notification.
                    cm.setProvisioningNotificationVisible(false, ConnectivityManager.TYPE_MOBILE,
                            mProvisionActionName);
                    // Complete the connection normally notifying the world we're connected.
                    // We do this if this isn't a special provisioning apn or if we've been
                    // told its time to provision.
                    completeConnection(apnContext);
                } else {
                    // This is a provisioning APN that we're reporting as connected. Later
                    // when the user desires to upgrade this to a "default" connection,
                    // mIsProvisioning == true, we'll go through the code path above.
                    // mIsProvisioning becomes true when CMD_ENABLE_MOBILE_PROVISIONING
                    // is sent to the DCT.
                    if (DBG) {
                        log("onDataSetupComplete: successful, BUT send connected to prov apn as"
                                + " mIsProvisioning:" + mIsProvisioning + " == false"
                                + " && (isProvisioningApn:" + isProvApn + " == true");
                    }

                    // While radio is up, grab provisioning URL.  The URL contains ICCID which
                    // disappears when radio is off.
                    mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(
                            cm.getMobileProvisioningUrl(),
                            TelephonyManager.getDefault().getNetworkOperatorName());
                    mPhone.getContext().registerReceiver(mProvisionBroadcastReceiver,
                            new IntentFilter(mProvisionActionName));
                    // Put up user notification that sign-in is required.
                    cm.setProvisioningNotificationVisible(true, ConnectivityManager.TYPE_MOBILE,
                            mProvisionActionName);
                    // Turn off radio to save battery and avoid wasting carrier resources.
                    // The network isn't usable and network validation will just fail anyhow.
                    setRadio(false);
                }
                if (DBG) {
                    log("onDataSetupComplete: SETUP complete type=" + apnContext.getApnType()
                        + ", reason:" + apnContext.getReason());
                }
            }
        } else {
            cause = (DcFailCause) (ar.result);
            if (DBG) {
                ApnSetting apn = apnContext.getApnSetting();
                log(String.format("onDataSetupComplete: error apn=%s cause=%s",
                        (apn == null ? "unknown" : apn.apn), cause));
            }
            if (cause.isEventLoggable()) {
                // Log this failure to the Event Logs.
                int cid = getCellLocationId();
                EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL,
                        cause.ordinal(), cid, TelephonyManager.getDefault().getNetworkType());
            }
            ApnSetting apn = apnContext.getApnSetting();
            mPhone.notifyPreciseDataConnectionFailed(apnContext.getReason(),
                    apnContext.getApnType(), apn != null ? apn.apn : "unknown", cause.toString());

            //compose broadcast intent send to the specific carrier apps
            Intent intent = new Intent(TelephonyIntents.ACTION_REQUEST_NETWORK_FAILED);
            intent.putExtra(ERROR_CODE_KEY, cause.getErrorCode());
            intent.putExtra(APN_TYPE_KEY, apnContext.getApnType());
            notifyCarrierAppWithIntent(intent);

            if (cause.isRestartRadioFail() || apnContext.restartOnError(cause.getErrorCode())) {
                if (DBG) log("Modem restarted.");
                sendRestartRadio();
            }

            // If the data call failure cause is a permanent failure, we mark the APN as permanent
            // failed.
            if (isPermanentFail(cause) ||
                    (mGsmDctExt != null && mGsmDctExt.isIgnoredCause(cause))) {
                log("cause = " + cause + ", mark apn as permanent failed. apn = " + apn);
                apnContext.markApnPermanentFailed(apn);
            }

            handleError = true;
        }

        if (handleError) {
            onDataSetupCompleteError(ar);
        }

        /* If flag is set to false after SETUP_DATA_CALL is invoked, we need
         * to clean data connections.
         */
        if (!mInternalDataEnabled) {
            cleanUpAllConnections(null);
        }

    }

    /**
     * check for obsolete messages.  Return ApnContext if valid, null if not
     */
    private ApnContext getValidApnContext(AsyncResult ar, String logString) {
        if (ar != null && ar.userObj instanceof Pair) {
            Pair<ApnContext, Integer>pair = (Pair<ApnContext, Integer>)ar.userObj;
            ApnContext apnContext = pair.first;
            if (apnContext != null) {
                final int generation = apnContext.getConnectionGeneration();
                if (DBG) {
                    log("getValidApnContext (" + logString + ") on " + apnContext + " got " +
                            generation + " vs " + pair.second);
                }
                if (generation == pair.second) {
                    return apnContext;
                } else {
                    log("ignoring obsolete " + logString);
                    return null;
                }
            }
        }
        throw new RuntimeException(logString + ": No apnContext");
    }

    /**
     * Error has occurred during the SETUP {aka bringUP} request and the DCT
     * should either try the next waiting APN or start over from the
     * beginning if the list is empty. Between each SETUP request there will
     * be a delay defined by {@link #getApnDelay()}.
     */
    private void onDataSetupCompleteError(AsyncResult ar) {

        ApnContext apnContext = getValidApnContext(ar, "onDataSetupCompleteError");

        if (apnContext == null) return;

        long delay = apnContext.getDelayForNextApn(mFailFast);

        // Check if we need to retry or not.
        if (delay >= 0) {
            if (DBG) log("onDataSetupCompleteError: Try next APN. delay = " + delay);
            apnContext.setState(DctConstants.State.SCANNING);
            // Wait a bit before trying the next APN, so that
            // we're not tying up the RIL command channel
            startAlarmForReconnect(delay, apnContext);
        } else {
            // If we are not going to retry any APN, set this APN context to failed state.
            // This would be the final state of a data connection.
            apnContext.setState(DctConstants.State.FAILED);
            mPhone.notifyDataConnection(Phone.REASON_APN_FAILED, apnContext.getApnType());
            apnContext.setDataConnectionAc(null);
            log("onDataSetupCompleteError: Stop retrying APNs.");
        }
    }

    /**
     * Read Carrier App name from CarrierConfig
     * @return String[0] Package name, String[1] Activity name
     */
    private String[] getActivationAppName() {
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = null;
        String[] activationApp;

       if (configManager != null) {
            b = configManager.getConfig();
        }
        if (b != null) {
            activationApp = b.getStringArray(CarrierConfigManager
                    .KEY_SIM_PROVISIONING_STATUS_DETECTION_CARRIER_APP_STRING_ARRAY);
        } else {
            // Return static default defined in CarrierConfigManager.
            activationApp = CarrierConfigManager.getDefaultConfig().getStringArray
                    (CarrierConfigManager
                            .KEY_SIM_PROVISIONING_STATUS_DETECTION_CARRIER_APP_STRING_ARRAY);
        }
        return activationApp;
    }

    /**
     * Called when EVENT_REDIRECTION_DETECTED is received.
     */
    private void onDataConnectionRedirected(String redirectUrl,
                                            HashMap<ApnContext, ConnectionParams> apnContextMap) {
        if (!TextUtils.isEmpty(redirectUrl)) {
            mRedirectUrl = redirectUrl;
            Intent intent = new Intent(TelephonyIntents.ACTION_DATA_CONNECTION_REDIRECTED);
            intent.putExtra(REDIRECTION_URL_KEY, redirectUrl);
            if(!isColdSimDetected() && !isOutOfCreditSimDetected()
                    && checkCarrierAppAvailable(intent)) {
                log("Starting Activation Carrier app with redirectUrl : " + redirectUrl);

                // Tear down data connections for all apn types
                for(ApnContext context : apnContextMap.keySet()) {
                    cleanUpConnection(true, context);
                    redirectApnContextSet.add(context);
                }
            }
        }
    }

    /**
     * Called when EVENT_DISCONNECT_DONE is received.
     */
    private void onDisconnectDone(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDone");
        if (apnContext == null) return;

        if(DBG) log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + apnContext);
        apnContext.setState(DctConstants.State.IDLE);

        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());

        // if all data connection are gone, check whether Airplane mode request was
        // pending. (ignore only IMS or EIMS is connected)
        if (isDisconnected() || isOnlyIMSorEIMSPdnConnected()) {
            if (mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
                if (DBG) log("onDisconnectDone: radio will be turned off, no retries");
                // Radio will be turned off. No need to retry data setup
                apnContext.setApnSetting(null);
                apnContext.setDataConnectionAc(null);

                // Need to notify disconnect as well, in the case of switching Airplane mode.
                // Otherwise, it would cause 30s delayed to turn on Airplane mode.
                if (mDisconnectPendingCount > 0) {
                    mDisconnectPendingCount--;
                }

                if (mDisconnectPendingCount == 0) {
                    notifyDataDisconnectComplete();
                    notifyAllDataDisconnected();
                    // Notify carrier app with redirection when there is no pending disconnect req
                    notifyCarrierAppForRedirection();
                }
                return;
            }
        }
        // If APN is still enabled, try to bring it back up automatically
        if (mAttached.get() && apnContext.isReady() && retryAfterDisconnected(apnContext)) {
            if (PhoneConstants.APN_TYPE_IMS.equals(apnContext.getApnType()) ||
                PhoneConstants.APN_TYPE_EMERGENCY.equals(apnContext.getApnType())) {
                if (DBG) {
                    log("onDisconnectDone: not to retry for " + apnContext.getApnType() + " PDN");
                }
            } else {
                try {
                    SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                } catch (RuntimeException ex) {
                    log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                }
                // Wait a bit before trying the next APN, so that
                // we're not tying up the RIL command channel.
                // This also helps in any external dependency to turn off the context.
                if (DBG) log("onDisconnectDone: attached, ready and retry after disconnect");
                long delay = apnContext.getInterApnDelay(mFailFast);
                delay = getDisconnectDoneRetryTimer(apnContext.getReason(), delay);
                if (delay > 0) {
                    // Data connection is in IDLE state, so when we reconnect later, we'll rebuild
                    // the waiting APN list, which will also reset/reconfigure the retry manager.
                    startAlarmForReconnect(delay, apnContext);
                }
            }
        } else {
            boolean restartRadioAfterProvisioning = mPhone.getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_restartRadioAfterProvisioning);

            if (apnContext.isProvisioningApn() && restartRadioAfterProvisioning) {
                log("onDisconnectDone: restartRadio after provisioning");
                restartRadio();
            }
            apnContext.setApnSetting(null);
            apnContext.setDataConnectionAc(null);
            if (isOnlySingleDcAllowed(mPhone.getServiceState().getRilDataRadioTechnology())) {
                if(DBG) log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                setupDataOnConnectableApns(Phone.REASON_SINGLE_PDN_ARBITRATION);
            } else {
                if(DBG) log("onDisconnectDone: not retrying");
            }
        }

        if (mDisconnectPendingCount > 0)
            mDisconnectPendingCount--;

        if (mDisconnectPendingCount == 0) {
            apnContext.setConcurrentVoiceAndDataAllowed(
                    mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
            // Notify carrier app with redirection when there is no pending disconnect req
            notifyCarrierAppForRedirection();
        }
    }

    /**
     * M: Called when EVENT_DISCONNECT_DONE is received.
     * Get retry timer for onDisconnectDone.
     */
    private long getDisconnectDoneRetryTimer(String reason, long delay) {
        long timer = delay;
        if (Phone.REASON_APN_CHANGED.equals(reason)) {
            // M: onApnChanged need retry quickly
            timer = 3000;
        } else if (!BSP_PACKAGE && mGsmDctExt != null) {
            // M: for other specific reason
            try {
                timer = mGsmDctExt.getDisconnectDoneRetryTimer(reason, timer);
            } catch (Exception e) {
                loge("GsmDCTExt.getDisconnectDoneRetryTimer fail!");
                e.printStackTrace();
            }
        }

        return timer;
    }

    /**
     * Called when EVENT_DISCONNECT_DC_RETRYING is received.
     */
    private void onDisconnectDcRetrying(AsyncResult ar) {
        // We could just do this in DC!!!
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDcRetrying");
        if (apnContext == null) return;

        apnContext.setState(DctConstants.State.RETRYING);
        if(DBG) log("onDisconnectDcRetrying: apnContext=" + apnContext);

        mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
    }

    public void onVoiceCallStarted() {
        mInVoiceCall = true;

        boolean isSupportConcurrent =
                DataConnectionHelper.getInstance().isDataSupportConcurrent(mPhone.getPhoneId());
        log("onVoiceCallStarted:isDataSupportConcurrent = " + isSupportConcurrent);

        if (isConnected() && !isSupportConcurrent) {
            if (DBG) log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(Phone.REASON_VOICE_CALL_STARTED);
        }
        notifyVoiceCallEventToDataConnection(mInVoiceCall, isSupportConcurrent);
    }

    public void onVoiceCallEnded() {
        mInVoiceCall = false;

        boolean isSupportConcurrent =
                DataConnectionHelper.getInstance().isDataSupportConcurrent(mPhone.getPhoneId());
        log("onVoiceCallEnded:isDataSupportConcurrent = " + isSupportConcurrent);

        if (!getDataEnabled()) {
            if (DBG) {
                log("onVoiceCallEnded: default data disable, cleanup default apn.");
            }
            onCleanUpConnection(true, DctConstants.APN_DEFAULT_ID, Phone.REASON_DATA_DISABLED);
        }

        if (isConnected()) {
            if (!isSupportConcurrent) {
                startNetStatPoll();
                startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                notifyDataConnection(Phone.REASON_VOICE_CALL_ENDED);
            } else {
                // clean slate after call end.
                resetPollStats();
            }
        }
        // reset reconnect timer
        setupDataOnConnectableApns(Phone.REASON_VOICE_CALL_ENDED);

        notifyVoiceCallEventToDataConnection(mInVoiceCall, isSupportConcurrent);
    }

    private void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        if (DBG) log("onCleanUpConnection");
        ApnContext apnContext = mApnContextsById.get(apnId);
        if (apnContext != null) {
            apnContext.setReason(reason);
            cleanUpConnection(tearDown, apnContext);
        }
    }

    private boolean isConnected() {
        for (ApnContext apnContext : mApnContexts.values()) {
            if (apnContext.getState() == DctConstants.State.CONNECTED) {
                // At least one context is connected, return true
                return true;
            }
        }
        // There are not any contexts connected, return false
        return false;
    }

    public boolean isDisconnected() {
        for (ApnContext apnContext : mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                // At least one context was not disconnected return false
                return false;
            }
        }
        // All contexts were disconnected so return true
        return true;
    }

    private void notifyDataConnection(String reason) {
        if (DBG) log("notifyDataConnection: reason=" + reason);
        for (ApnContext apnContext : mApnContexts.values()) {
            if (mAttached.get() && apnContext.isReady() && apnContext.isNeedNotify()) {
                // M: Check need notify or not in order to avoid ANR issue
                if (DBG) log("notifyDataConnection: type:" + apnContext.getApnType());
                mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(),
                        apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    private void setDataProfilesAsNeeded() {
        if (DBG) log("setDataProfilesAsNeeded");
        if (mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
            ArrayList<DataProfile> dps = new ArrayList<DataProfile>();
            for (ApnSetting apn : mAllApnSettings) {
                if (apn.modemCognitive) {
                    DataProfile dp = new DataProfile(apn,
                            mPhone.getServiceState().getDataRoaming());
                    boolean isDup = false;
                    for(DataProfile dpIn : dps) {
                        if (dp.equals(dpIn)) {
                            isDup = true;
                            break;
                        }
                    }
                    if (!isDup) {
                        dps.add(dp);
                    }
                }
            }
            if(dps.size() > 0) {
                mPhone.mCi.setDataProfile(dps.toArray(new DataProfile[0]), null);
            }
        }
    }

    /**
     * Based on the sim operator numeric, create a list for all possible
     * Data Connections and setup the preferredApn.
     */
    private void createAllApnList() {
        mMvnoMatched = false;
        mAllApnSettings = new ArrayList<ApnSetting>();
        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";
        if (operator != null) {
            String selection = "numeric = '" + operator + "'";
            String orderBy = "_id";
            // query only enabled apn.
            // carrier_enabled : 1 means enabled apn, 0 disabled apn.
            // selection += " and carrier_enabled = 1";
            if (DBG) log("createAllApnList: selection=" + selection);

            Cursor cursor = mPhone.getContext().getContentResolver().query(
                    Telephony.Carriers.CONTENT_URI, null, selection, null, orderBy);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    mAllApnSettings = createApnList(cursor);
                }
                cursor.close();
            }
        }

        addEmergencyApnSetting();

        dedupeApnSettings();

        if (mAllApnSettings.isEmpty()) {
            if (DBG) log("createAllApnList: No APN found for carrier: " + operator);
            mPreferredApn = null;
            // TODO: What is the right behavior?
            //notifyNoData(DataConnection.FailCause.MISSING_UNKNOWN_APN);
        } else {
            mPreferredApn = getPreferredApn();
            if (mPreferredApn != null && !mPreferredApn.numeric.equals(operator)) {
                mPreferredApn = null;
                setPreferredApn(-1);
            }
            if (DBG) log("createAllApnList: mPreferredApn=" + mPreferredApn);
        }
        if (DBG) log("createAllApnList: X mAllApnSettings=" + mAllApnSettings);

        setDataProfilesAsNeeded();

        // M: sync apn table to md
        syncApnToMd();

        // M: VDF MMS over ePDG @{
        syncApnTableToRds(mAllApnSettings);
        /// @}
    }

    private void dedupeApnSettings() {
        ArrayList<ApnSetting> resultApns = new ArrayList<ApnSetting>();

        // coalesce APNs if they are similar enough to prevent
        // us from bringing up two data calls with the same interface
        int i = 0;
        while (i < mAllApnSettings.size() - 1) {
            ApnSetting first = mAllApnSettings.get(i);
            ApnSetting second = null;
            int j = i + 1;
            while (j < mAllApnSettings.size()) {
                second = mAllApnSettings.get(j);
                if (apnsSimilar(first, second)) {
                    ApnSetting newApn = mergeApns(first, second);
                    mAllApnSettings.set(i, newApn);
                    first = newApn;
                    mAllApnSettings.remove(j);
                } else {
                    j++;
                }
            }
            i++;
        }
    }

    //check whether the types of two APN same (even only one type of each APN is same)
    private boolean apnTypeSameAny(ApnSetting first, ApnSetting second) {
        if(VDBG) {
            StringBuilder apnType1 = new StringBuilder(first.apn + ": ");
            for(int index1 = 0; index1 < first.types.length; index1++) {
                apnType1.append(first.types[index1]);
                apnType1.append(",");
            }

            StringBuilder apnType2 = new StringBuilder(second.apn + ": ");
            for(int index1 = 0; index1 < second.types.length; index1++) {
                apnType2.append(second.types[index1]);
                apnType2.append(",");
            }
            log("APN1: is " + apnType1);
            log("APN2: is " + apnType2);
        }

        for(int index1 = 0; index1 < first.types.length; index1++) {
            for(int index2 = 0; index2 < second.types.length; index2++) {
                if(first.types[index1].equals(PhoneConstants.APN_TYPE_ALL) ||
                        second.types[index2].equals(PhoneConstants.APN_TYPE_ALL) ||
                        first.types[index1].equals(second.types[index2])) {
                    if(VDBG)log("apnTypeSameAny: return true");
                    return true;
                }
            }
        }

        if(VDBG)log("apnTypeSameAny: return false");
        return false;
    }

    // Check if neither mention DUN and are substantially similar
    private boolean apnsSimilar(ApnSetting first, ApnSetting second) {
        return (first.canHandleType(PhoneConstants.APN_TYPE_DUN) == false &&
                second.canHandleType(PhoneConstants.APN_TYPE_DUN) == false &&
                Objects.equals(first.apn, second.apn) &&
                !apnTypeSameAny(first, second) &&
                xorEquals(first.proxy, second.proxy) &&
                xorEquals(first.port, second.port) &&
                first.carrierEnabled == second.carrierEnabled &&
                first.bearerBitmask == second.bearerBitmask &&
                first.profileId == second.profileId &&
                Objects.equals(first.mvnoType, second.mvnoType) &&
                Objects.equals(first.mvnoMatchData, second.mvnoMatchData) &&
                xorEquals(first.mmsc, second.mmsc) &&
                xorEquals(first.mmsProxy, second.mmsProxy) &&
                xorEquals(first.mmsPort, second.mmsPort));
    }

    // equal or one is not specified
    private boolean xorEquals(String first, String second) {
        return (Objects.equals(first, second) ||
                TextUtils.isEmpty(first) ||
                TextUtils.isEmpty(second));
    }

    private ApnSetting mergeApns(ApnSetting dest, ApnSetting src) {
        int id = dest.id;
        ArrayList<String> resultTypes = new ArrayList<String>();
        resultTypes.addAll(Arrays.asList(dest.types));
        for (String srcType : src.types) {
            if (resultTypes.contains(srcType) == false) resultTypes.add(srcType);
            if (srcType.equals(PhoneConstants.APN_TYPE_DEFAULT)) id = src.id;
        }
        String mmsc = (TextUtils.isEmpty(dest.mmsc) ? src.mmsc : dest.mmsc);
        String mmsProxy = (TextUtils.isEmpty(dest.mmsProxy) ? src.mmsProxy : dest.mmsProxy);
        String mmsPort = (TextUtils.isEmpty(dest.mmsPort) ? src.mmsPort : dest.mmsPort);
        String proxy = (TextUtils.isEmpty(dest.proxy) ? src.proxy : dest.proxy);
        String port = (TextUtils.isEmpty(dest.port) ? src.port : dest.port);
        String protocol = src.protocol.equals("IPV4V6") ? src.protocol : dest.protocol;
        String roamingProtocol = src.roamingProtocol.equals("IPV4V6") ? src.roamingProtocol :
                dest.roamingProtocol;
        int bearerBitmask = (dest.bearerBitmask == 0 || src.bearerBitmask == 0) ?
                0 : (dest.bearerBitmask | src.bearerBitmask);

        return new ApnSetting(id, dest.numeric, dest.carrier, dest.apn,
                proxy, port, mmsc, mmsProxy, mmsPort, dest.user, dest.password,
                dest.authType, resultTypes.toArray(new String[0]), protocol,
                roamingProtocol, dest.carrierEnabled, 0, bearerBitmask, dest.profileId,
                (dest.modemCognitive || src.modemCognitive), dest.maxConns, dest.waitTime,
                dest.maxConnsTime, dest.mtu, dest.mvnoType, dest.mvnoMatchData, dest.inactiveTimer);
    }

    /** Return the DC AsyncChannel for the new data connection */
    private DcAsyncChannel createDataConnection(String reqApnType, ApnSetting apnSetting) {
        if (DBG) log("createDataConnection E");

        int id = 0;
        if (isSupportThrottlingApn()) {
            id = generateDataConnectionId(reqApnType, apnSetting);
            if (id < 0) {
                return null;
            }
        } else {
            id = mUniqueIdGenerator.getAndIncrement();
            if (id >= getPdpConnectionPoolSize()) {
                loge("Max PDP count is " + getPdpConnectionPoolSize() + ",but request " + (id + 1));
                mUniqueIdGenerator.getAndDecrement();
                return null;
            }
        }

        DataConnection conn = DataConnection.makeDataConnection(mPhone, id,
                                                this, mDcTesterFailBringUpAll, mDcc);
        mDataConnections.put(id, conn);
        DcAsyncChannel dcac = new DcAsyncChannel(conn, LOG_TAG);
        int status = dcac.fullyConnectSync(mPhone.getContext(), this, conn.getHandler());
        if (status == AsyncChannel.STATUS_SUCCESSFUL) {
            mDataConnectionAcHashMap.put(dcac.getDataConnectionIdSync(), dcac);
        } else {
            loge("createDataConnection: Could not connect to dcac=" + dcac + " status=" + status);
        }

        if (DBG) log("createDataConnection() X id=" + id + " dc=" + conn);
        return dcac;
    }

    private void destroyDataConnections() {
        if(mDataConnections != null) {
            if (DBG) log("destroyDataConnections: clear mDataConnectionList");
            mDataConnections.clear();
        } else {
            if (DBG) log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    /**
     * Build a list of APNs to be used to create PDP's.
     *
     * @param requestedApnType
     * @return waitingApns list to be used to create PDP
     *          error when waitingApns.isEmpty()
     */
    private ArrayList<ApnSetting> buildWaitingApns(String requestedApnType, int radioTech) {
        if (DBG) log("buildWaitingApns: E requestedApnType=" + requestedApnType);
        ArrayList<ApnSetting> apnList = new ArrayList<ApnSetting>();

        if (requestedApnType.equals(PhoneConstants.APN_TYPE_DUN)) {
            ApnSetting dun = fetchDunApn();
            if (dun != null) {
                apnList.add(dun);
                if (DBG) log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + apnList);
                return apnList;
            }
        }

        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";

        // This is a workaround for a bug (7305641) where we don't failover to other
        // suitable APNs if our preferred APN fails.  On prepaid ATT sims we need to
        // failover to a provisioning APN, but once we've used their default data
        // connection we are locked to it for life.  This change allows ATT devices
        // to say they don't want to use preferred at all.
        boolean usePreferred = true;
        try {
            usePreferred = ! mPhone.getContext().getResources().getBoolean(com.android.
                    internal.R.bool.config_dontPreferApn);
        } catch (Resources.NotFoundException e) {
            if (DBG) log("buildWaitingApns: usePreferred NotFoundException set to true");
            usePreferred = true;
        }
        if (usePreferred) {
            mPreferredApn = getPreferredApn();
        }
        if (DBG) {
            log("buildWaitingApns: usePreferred=" + usePreferred
                    + " canSetPreferApn=" + mCanSetPreferApn
                    + " mPreferredApn=" + mPreferredApn
                    + " operator=" + operator + " radioTech=" + radioTech
                    + " IccRecords r=" + r);
        }

        if (usePreferred && mCanSetPreferApn && mPreferredApn != null &&
                mPreferredApn.canHandleType(requestedApnType)) {
            if (DBG) {
                log("buildWaitingApns: Preferred APN:" + operator + ":"
                        + mPreferredApn.numeric + ":" + mPreferredApn);
            }
            if (mPreferredApn.numeric.equals(operator)) {
                if (ServiceState.bitmaskHasTech(mPreferredApn.bearerBitmask, radioTech)) {
                    apnList.add(mPreferredApn);
                    if (DBG) log("buildWaitingApns: X added preferred apnList=" + apnList);
                    return apnList;
                } else {
                    if (DBG) log("buildWaitingApns: no preferred APN");
                    setPreferredApn(-1);
                    mPreferredApn = null;
                }
            } else {
                if (DBG) log("buildWaitingApns: no preferred APN");
                setPreferredApn(-1);
                mPreferredApn = null;
            }
        }
        if (mAllApnSettings != null) {
            if (DBG) log("buildWaitingApns: mAllApnSettings=" + mAllApnSettings);
            for (ApnSetting apn : mAllApnSettings) {
                if (apn.canHandleType(requestedApnType)) {
                    if (ServiceState.bitmaskHasTech(apn.bearerBitmask, radioTech)) {
                        if (apn.canHandleType(PhoneConstants.APN_TYPE_EMERGENCY)) {
                            // Read EIMS protocol type from EM system property
                            String eimsProtocol = SystemProperties.get(VOLTE_EMERGENCY_PDN_PROTOCOL,
                                    VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL);
                            log("initEmergencyApnSetting: eimsProtocol = " + eimsProtocol);
                            if (!eimsProtocol.equals(apn.protocol)) {
                                apn = new ApnSetting(apn.id, apn.numeric,
                                        apn.carrier, apn.apn, apn.proxy,
                                        apn.port, apn.mmsc, apn.mmsProxy,
                                        apn.mmsPort, apn.user, apn.password,
                                        apn.authType, apn.types,
                                        eimsProtocol, eimsProtocol, apn.carrierEnabled,
                                        0, apn.bearerBitmask,
                                        apn.profileId, apn.modemCognitive,
                                        apn.maxConns, apn.waitTime,
                                        apn.maxConnsTime, apn.mtu,
                                        apn.mvnoType, apn.mvnoMatchData, apn.inactiveTimer);
                            }
                        }
                        if (DBG) log("buildWaitingApns: adding apn=" + apn);
                        apnList.add(apn);
                    } else {
                        if (DBG) {
                            log("buildWaitingApns: bearerBitmask:" + apn.bearerBitmask + " does " +
                                    "not include radioTech:" + radioTech);
                        }
                    }
                } else if (DBG) {
                    log("buildWaitingApns: couldn't handle requested ApnType="
                            + requestedApnType);
                }
            }
        } else {
            loge("mAllApnSettings is null!");
        }
        if (DBG) log("buildWaitingApns: " + apnList.size() + " APNs in the list: " + apnList);
        return apnList;
    }

    // M: VDF MMS over ePDG @{
    private ArrayList<ApnSetting> buildWifiApns(String requestedApnType) {
        if (DBG) log("buildWifiApns: E requestedApnType=" + requestedApnType);
        ArrayList<ApnSetting> apnList = new ArrayList<ApnSetting>();

        if (mAllApnSettings != null) {
            if (DBG) log("buildWaitingApns: mAllApnSettings=" + mAllApnSettings);
            for (ApnSetting apn : mAllApnSettings) {
                if (apn.canHandleType(requestedApnType)) {
                    if (isWifiOnlyApn(apn.bearerBitmask)) {
                        apnList.add(apn);
                    }
                }
            }
        }
        if (DBG) log("buildWifiApns: X apnList=" + apnList);
        return apnList;
    }
    /// @}
    private String apnListToString (ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        try {
            for (int i = 0, size = apns.size(); i < size; i++) {
                result.append('[')
                  .append(apns.get(i).toString())
                  .append(']');
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
        return result.toString();
    }

    private void setPreferredApn(int pos) {
        if (!mCanSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }

        String subId = Long.toString(mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        log("setPreferredApn: delete subId=" + subId);
        ContentResolver resolver = mPhone.getContext().getContentResolver();
        resolver.delete(uri, null, null);

        if (pos >= 0) {
            log("setPreferredApn: insert pos=" + pos + ", subId=" + subId);
            ContentValues values = new ContentValues();
            values.put(APN_ID, pos);
            resolver.insert(uri, values);
        }
    }

    private ApnSetting getPreferredApn() {
        if (mAllApnSettings == null || mAllApnSettings.isEmpty()) {
            log("getPreferredApn: mAllApnSettings is " + ((mAllApnSettings == null)?"null":"empty"));
            return null;
        }

        String subId = Long.toString(mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                uri, new String[] { "_id", "name", "apn" },
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            mCanSetPreferApn = true;
        } else {
            mCanSetPreferApn = false;
        }
        log("getPreferredApn: mRequestedApnType=" + mRequestedApnType + " cursor=" + cursor
                + " cursor.count=" + ((cursor != null) ? cursor.getCount() : 0)
                + " subId=" + subId);

        if (mCanSetPreferApn && cursor.getCount() > 0) {
            int pos;
            cursor.moveToFirst();
            pos = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID));
            for(ApnSetting p : mAllApnSettings) {
                log("getPreferredApn: apnSetting=" + p + ", pos=" + pos + ", subId=" + subId);
                if (p.id == pos && p.canHandleType(mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + p);
                    cursor.close();
                    return p;
                }
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage (Message msg) {
        if (VDBG) log("handleMessage msg=" + msg);
        AsyncResult ar;

        switch (msg.what) {
            case DctConstants.EVENT_RECORDS_LOADED:
                // If onRecordsLoadedOrSubIdChanged() is not called here, it should be called on
                // onSubscriptionsChanged() when a valid subId is available.
                int subId = mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    onRecordsLoadedOrSubIdChanged();
                } else {
                    log("Ignoring EVENT_RECORDS_LOADED as subId is not valid: " + subId);
                }
                break;

            case DctConstants.EVENT_SETUP_DATA_WHEN_LOADED:
                setupDataOnConnectableApns(Phone.REASON_SIM_LOADED);
                break;

            case DctConstants.EVENT_DATA_CONNECTION_DETACHED:
                onDataConnectionDetached();
                break;

            case DctConstants.EVENT_DATA_CONNECTION_ATTACHED:
                onDataConnectionAttached();
                break;

            case DctConstants.EVENT_DO_RECOVERY:
                doRecovery();
                break;

            case DctConstants.EVENT_APN_CHANGED:
                new Thread(new Runnable() {
                    public void run() {
                        synchronized (mCreateApnLock) {
                            onApnChanged();
                        }
                    }
                }).start();
                break;

            case DctConstants.EVENT_APN_CHANGED_DONE:
                boolean bImsApnChanged = (msg.arg1 == 0) ? false : true;
                if (DBG) {
                    log("EVENT_APN_CHANGED_DONE");
                }
                if (bImsApnChanged) {
                    log("ims apn changed");
                    ApnContext apnContext = mApnContexts.get(PhoneConstants.APN_TYPE_IMS);
                    cleanUpConnection(true, apnContext);
                } else {
                    // default changed
                    onApnChangedDone();
                }
                break;

            case DctConstants.EVENT_PS_RESTRICT_ENABLED:
                /**
                 * We don't need to explicitly to tear down the PDP context
                 * when PS restricted is enabled. The base band will deactive
                 * PDP context and notify us with PDP_CONTEXT_CHANGED.
                 * But we should stop the network polling and prevent reset PDP.
                 */
                if (DBG) log("EVENT_PS_RESTRICT_ENABLED " + mIsPsRestricted);
                stopNetStatPoll();
                stopDataStallAlarm();
                mIsPsRestricted = true;
                break;

            case DctConstants.EVENT_PS_RESTRICT_DISABLED:
                /**
                 * When PS restrict is removed, we need setup PDP connection if
                 * PDP connection is down.
                 */
                // M: Wifi only
                ConnectivityManager cnnm = (ConnectivityManager) mPhone.getContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

                if (DBG) log("EVENT_PS_RESTRICT_DISABLED " + mIsPsRestricted);
                mIsPsRestricted  = false;
                if (isConnected()) {
                    startNetStatPoll();
                    startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                } else {
                    // TODO: Should all PDN states be checked to fail?
                    if (mState == DctConstants.State.FAILED) {
                        cleanUpAllConnections(false, Phone.REASON_PS_RESTRICT_ENABLED);
                        mReregisterOnReconnectFailure = false;
                    }
                    ApnContext apnContext = mApnContextsById.get(DctConstants.APN_DEFAULT_ID);
                    if (apnContext != null) {
                        // M: Fix dual DataConnection issue. For the case that PS is detached but
                        //    the EVENT_DATA_CONNECTION_DETACHED haven't received yet. In this case,
                        //    isDataAllow returns true and will try to setup data. Then, the detach
                        //    message received and set mAttached as false. After this,
                        //    onDisconnectDone() called and will set ApnContext idle and DCAC null.
                        //    It will make DCAC can not re-use when setup data at the second time.
                        if (mPhone.getServiceStateTracker().getCurrentDataConnectionState()
                                == ServiceState.STATE_IN_SERVICE) {
                            apnContext.setReason(Phone.REASON_PS_RESTRICT_ENABLED);
                            trySetupData(apnContext);
                        } else {
                            if (DBG) log("EVENT_PS_RESTRICT_DISABLED, data not attached, skip.");
                        }
                    } else {
                        loge("**** Default ApnContext not found ****");
                        // M: Wifi only
                        if (Build.IS_DEBUGGABLE && cnnm.isNetworkSupported(
                                ConnectivityManager.TYPE_MOBILE)) {
                            throw new RuntimeException("Default ApnContext not found");
                        }
                    }
                }
                break;

            case DctConstants.EVENT_TRY_SETUP_DATA:
                if (msg.obj instanceof ApnContext) {
                    onTrySetupData((ApnContext)msg.obj);
                } else if (msg.obj instanceof String) {
                    onTrySetupData((String)msg.obj);
                } else {
                    loge("EVENT_TRY_SETUP request w/o apnContext or String");
                }
                break;

            case DctConstants.EVENT_CLEAN_UP_CONNECTION:
                boolean tearDown = (msg.arg1 == 0) ? false : true;
                if (DBG) log("EVENT_CLEAN_UP_CONNECTION tearDown=" + tearDown);
                if (msg.obj instanceof ApnContext) {
                    cleanUpConnection(tearDown, (ApnContext)msg.obj);
                } else {
                    onCleanUpConnection(tearDown, msg.arg2, (String) msg.obj);
                }
                break;
            case DctConstants.EVENT_SET_INTERNAL_DATA_ENABLE: {
                final boolean enabled = (msg.arg1 == DctConstants.ENABLED) ? true : false;
                onSetInternalDataEnabled(enabled, (Message) msg.obj);
                break;
            }
            case DctConstants.EVENT_CLEAN_UP_ALL_CONNECTIONS:
                if ((msg.obj != null) && (msg.obj instanceof String == false)) {
                    msg.obj = null;
                }
                onCleanUpAllConnections((String) msg.obj);
                break;

            case DctConstants.EVENT_DATA_RAT_CHANGED:
                //May new Network allow setupData, so try it here
                setupDataOnConnectableApns(Phone.REASON_NW_TYPE_CHANGED,
                        RetryFailures.ONLY_ON_CHANGE);
                break;

            case DctConstants.CMD_CLEAR_PROVISIONING_SPINNER:
                // Check message sender intended to clear the current spinner.
                if (mProvisioningSpinner == msg.obj) {
                    mProvisioningSpinner.dismiss();
                    mProvisioningSpinner = null;
                }
                break;

            // M: IA-change attach APN
            case DctConstants.EVENT_ATTACH_APN_CHANGED:
                onMdChangedAttachApn((AsyncResult) msg.obj);
                break;

            //M: FDN Support
            case DctConstants.EVENT_FDN_CHANGED:
                onFdnChanged();
                break;

            case DctConstants.EVENT_RESET_PDP_DONE:
                log("EVENT_RESET_PDP_DONE cid=" + msg.arg1);
                break;

            case DctConstants.EVENT_REMOVE_RESTRICT_EUTRAN:
                if (MTK_CC33_SUPPORT) {
                    log("EVENT_REMOVE_RESTRICT_EUTRAN");
                    mReregisterOnReconnectFailure = false;
                    setupDataOnConnectableApns(Phone.REASON_PS_RESTRICT_DISABLED);
                }
                break;

            // M: [LTE][Low Power][UL traffic shaping] Start
            // TODO: Should this move to NW frameworks to handle?
            case DctConstants.EVENT_LTE_ACCESS_STRATUM_STATE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int[] ints = (int[]) ar.result;
                    int lteAccessStratumDataState = ints[0];
                    if (lteAccessStratumDataState != LTE_AS_CONNECTED) { // LTE AS Disconnected
                        int networkType = ints[1];
                        log("EVENT_LTE_ACCESS_STRATUM_STATE networkType = " + networkType);
                        notifyPsNetworkTypeChanged(networkType);
                    } else { // LTE AS Connected
                        mPhone.notifyPsNetworkTypeChanged(TelephonyManager.NETWORK_TYPE_LTE);
                    }
                    log("EVENT_LTE_ACCESS_STRATUM_STATE lteAccessStratumDataState = "
                            + lteAccessStratumDataState);
                    notifyLteAccessStratumChanged(lteAccessStratumDataState);
                } else {
                    Rlog.e(LOG_TAG, "LteAccessStratumState exception: " + ar.exception);
                }
                break;

            case DctConstants.EVENT_DEFAULT_APN_REFERENCE_COUNT_CHANGED: {
                int newDefaultRefCount = msg.arg1;
                onSharedDefaultApnState(newDefaultRefCount);
                break;
            }
            // M: [LTE][Low Power][UL traffic shaping] End

            // M: JPN IA Start
            case DctConstants.EVENT_REG_PLMN_CHANGED:
                log("handleMessage : <EVENT_REG_PLMN_CHANGED>");
                if (isOp129IaSupport() || isOp17IaSupport()) {
                    handlePlmnChange((AsyncResult) msg.obj);
                }
                break;
            case DctConstants.EVENT_REG_SUSPENDED:
                log("handleMessage : <EVENT_REG_SUSPENDED>");
                if (isOp129IaSupport() || isOp17IaSupport()) {
                    if (isNeedToResumeMd()) {
                        handleRegistrationSuspend((AsyncResult) msg.obj);
                    }
                }
                break;
            case DctConstants.EVENT_SET_RESUME:
                log("handleMessage : <EVENT_SET_RESUME>");
                if (isOp129IaSupport() || isOp17IaSupport()) {
                    handleSetResume();
                }
                break;
            // M: JPN IA End

            case AsyncChannel.CMD_CHANNEL_DISCONNECTED: {
                log("DISCONNECTED_CONNECTED: msg=" + msg);
                DcAsyncChannel dcac = (DcAsyncChannel) msg.obj;
                mDataConnectionAcHashMap.remove(dcac.getDataConnectionIdSync());
                dcac.disconnected();
                break;
            }
            case DctConstants.EVENT_ENABLE_NEW_APN:
                onEnableApn(msg.arg1, msg.arg2);
                break;

            case DctConstants.EVENT_DATA_STALL_ALARM:
                onDataStallAlarm(msg.arg1);
                break;

            case DctConstants.EVENT_ROAMING_OFF:
                onRoamingOff();
                break;

            case DctConstants.EVENT_ROAMING_ON:
                onRoamingOn();
                break;

            case DctConstants.EVENT_DEVICE_PROVISIONED_CHANGE:
                onDeviceProvisionedChange();
                break;

            case DctConstants.EVENT_REDIRECTION_DETECTED:
                ar = (AsyncResult) msg.obj;
                String url = (String) ar.userObj;
                log("dataConnectionTracker.handleMessage: EVENT_REDIRECTION_DETECTED=" + url);
                onDataConnectionRedirected(url, (HashMap<ApnContext, ConnectionParams>) ar.result);

            case DctConstants.EVENT_RADIO_AVAILABLE:
                onRadioAvailable();
                break;

            case DctConstants.EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                onRadioOffOrNotAvailable();
                break;

            case DctConstants.EVENT_DATA_SETUP_COMPLETE:
                onDataSetupComplete((AsyncResult) msg.obj);
                break;

            case DctConstants.EVENT_DATA_SETUP_COMPLETE_ERROR:
                onDataSetupCompleteError((AsyncResult) msg.obj);
                break;

            case DctConstants.EVENT_DISCONNECT_DONE:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DONE msg=" + msg);
                onDisconnectDone((AsyncResult) msg.obj);
                break;

            case DctConstants.EVENT_DISCONNECT_DC_RETRYING:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DC_RETRYING msg=" + msg);
                onDisconnectDcRetrying((AsyncResult) msg.obj);
                break;

            case DctConstants.EVENT_VOICE_CALL_STARTED:
                onVoiceCallStarted();
                break;

            case DctConstants.EVENT_VOICE_CALL_ENDED:
                onVoiceCallEnded();
                break;

            case DctConstants.EVENT_RESET_DONE: {
                if (DBG) log("EVENT_RESET_DONE");
                onResetDone((AsyncResult) msg.obj);
                break;
            }
            case DctConstants.CMD_SET_USER_DATA_ENABLE: {
                final boolean enabled = (msg.arg1 == DctConstants.ENABLED) ? true : false;
                if (DBG) log("CMD_SET_USER_DATA_ENABLE enabled=" + enabled);
                onSetUserDataEnabled(enabled);
                break;
            }
            // TODO - remove
            case DctConstants.CMD_SET_DEPENDENCY_MET: {
                boolean met = (msg.arg1 == DctConstants.ENABLED) ? true : false;
                if (DBG) log("CMD_SET_DEPENDENCY_MET met=" + met);
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    String apnType = (String)bundle.get(DctConstants.APN_TYPE_KEY);
                    if (apnType != null) {
                        onSetDependencyMet(apnType, met);
                    }
                }
                break;
            }
            case DctConstants.CMD_SET_POLICY_DATA_ENABLE: {
                final boolean enabled = (msg.arg1 == DctConstants.ENABLED) ? true : false;
                onSetPolicyDataEnabled(enabled);
                break;
            }
            case DctConstants.CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: {
                sEnableFailFastRefCounter += (msg.arg1 == DctConstants.ENABLED) ? 1 : -1;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: "
                            + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (sEnableFailFastRefCounter < 0) {
                    final String s = "CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: "
                            + "sEnableFailFastRefCounter:" + sEnableFailFastRefCounter + " < 0";
                    loge(s);
                    sEnableFailFastRefCounter = 0;
                }
                final boolean enabled = sEnableFailFastRefCounter > 0;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=" + enabled
                            + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (mFailFast != enabled) {
                    mFailFast = enabled;

                    mDataStallDetectionEnabled = !enabled;
                    if (mDataStallDetectionEnabled
                            && (getOverallState() == DctConstants.State.CONNECTED)
                            && (!mInVoiceCall ||
                                    mPhone.getServiceStateTracker()
                                        .isConcurrentVoiceAndDataAllowed())) {
                        if (DBG) log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                        stopDataStallAlarm();
                        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
                    } else {
                        if (DBG) log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                        stopDataStallAlarm();
                    }
                }

                break;
            }
            case DctConstants.CMD_ENABLE_MOBILE_PROVISIONING: {
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    try {
                        mProvisioningUrl = (String)bundle.get(DctConstants.PROVISIONING_URL_KEY);
                    } catch(ClassCastException e) {
                        loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url not a string" + e);
                        mProvisioningUrl = null;
                    }
                }
                if (TextUtils.isEmpty(mProvisioningUrl)) {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url is empty, ignoring");
                    mIsProvisioning = false;
                    mProvisioningUrl = null;
                } else {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioningUrl=" + mProvisioningUrl);
                    mIsProvisioning = true;
                    startProvisioningApnAlarm();
                }
                break;
            }
            case DctConstants.EVENT_PROVISIONING_APN_ALARM: {
                if (DBG) log("EVENT_PROVISIONING_APN_ALARM");
                ApnContext apnCtx = mApnContextsById.get(DctConstants.APN_DEFAULT_ID);
                if (apnCtx.isProvisioningApn() && apnCtx.isConnectedOrConnecting()) {
                    if (mProvisioningApnAlarmTag == msg.arg1) {
                        if (DBG) log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                        mIsProvisioning = false;
                        mProvisioningUrl = null;
                        stopProvisioningApnAlarm();
                        sendCleanUpConnection(true, apnCtx);
                    } else {
                        if (DBG) {
                            log("EVENT_PROVISIONING_APN_ALARM: ignore stale tag,"
                                    + " mProvisioningApnAlarmTag:" + mProvisioningApnAlarmTag
                                    + " != arg1:" + msg.arg1);
                        }
                    }
                } else {
                    if (DBG) log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                }
                break;
            }
            case DctConstants.CMD_IS_PROVISIONING_APN: {
                if (DBG) log("CMD_IS_PROVISIONING_APN");
                boolean isProvApn;
                try {
                    String apnType = null;
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        apnType = (String)bundle.get(DctConstants.APN_TYPE_KEY);
                    }
                    if (TextUtils.isEmpty(apnType)) {
                        loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                        isProvApn = false;
                    } else {
                        isProvApn = isProvisioningApn(apnType);
                    }
                } catch (ClassCastException e) {
                    loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                    isProvApn = false;
                }
                if (DBG) log("CMD_IS_PROVISIONING_APN: ret=" + isProvApn);
                mReplyAc.replyToMessage(msg, DctConstants.CMD_IS_PROVISIONING_APN,
                        isProvApn ? DctConstants.ENABLED : DctConstants.DISABLED);
                break;
            }
            case DctConstants.EVENT_ICC_CHANGED: {
                onUpdateIcc();
                break;
            }
            case DctConstants.EVENT_RESTART_RADIO: {
                restartRadio();
                break;
            }
            case DctConstants.CMD_NET_STAT_POLL: {
                if (msg.arg1 == DctConstants.ENABLED) {
                    handleStartNetStatPoll((DctConstants.Activity)msg.obj);
                } else if (msg.arg1 == DctConstants.DISABLED) {
                    handleStopNetStatPoll((DctConstants.Activity)msg.obj);
                }
                break;
            }
            case DctConstants.EVENT_DATA_STATE_CHANGED: {
                // no longer do anything, but still registered - clean up log
                // TODO - why are we still registering?
                break;
            }
            //Reset Attach Apn
            case DctConstants.EVENT_RESET_ATTACH_APN: {
                if (mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
                    setInitialAttachApn();
                } else {
                    if (DBG) {
                        log("EVENT_RESET_ATTACH_APN: Ignore due to null APN list");
                    }
                }
                break;
            }
            case DctConstants.EVENT_PCO_STATUS:
                onPcoStatus((AsyncResult) msg.obj);
                break;
            default:
                Rlog.e("DcTracker", "Unhandled event=" + msg);
                break;

        }
    }

    private int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS)) {
            return RILConstants.DATA_PROFILE_IMS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_FOTA)) {
            return RILConstants.DATA_PROFILE_FOTA;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_CBS)) {
            return RILConstants.DATA_PROFILE_CBS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IA)) {
            return RILConstants.DATA_PROFILE_DEFAULT; // DEFAULT for now
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DUN)) {
            return RILConstants.DATA_PROFILE_TETHERED;
        // M: VDF MMS over ePDG @{
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_MMS)) {
            return RILConstants.DATA_PROFILE_MMS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_SUPL)) {
            return RILConstants.DATA_PROFILE_SUPL;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_HIPRI)) {
            return RILConstants.DATA_PROFILE_HIPRI;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DM)) {
            return RILConstants.DATA_PROFILE_DM;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_WAP)) {
            return RILConstants.DATA_PROFILE_WAP;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_NET)) {
            return RILConstants.DATA_PROFILE_NET;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_CMMAIL)) {
            return RILConstants.DATA_PROFILE_CMMAIL;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_RCSE)) {
            return RILConstants.DATA_PROFILE_RCSE;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_EMERGENCY)) {
            return RILConstants.DATA_PROFILE_EMERGENCY;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_XCAP)) {
            return RILConstants.DATA_PROFILE_XCAP;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_RCS)) {
            return RILConstants.DATA_PROFILE_RCS;
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DEFAULT)) {
            return RILConstants.DATA_PROFILE_DEFAULT;
        } else {
        /// @}
            return RILConstants.DATA_PROFILE_INVALID;
        }
    }

    private int getCellLocationId() {
        int cid = -1;
        CellLocation loc = mPhone.getCellLocation();

        if (loc != null) {
            if (loc instanceof GsmCellLocation) {
                cid = ((GsmCellLocation)loc).getCid();
            } else if (loc instanceof CdmaCellLocation) {
                cid = ((CdmaCellLocation)loc).getBaseStationId();
            }
        }
        return cid;
    }

    private IccRecords getUiccRecords(int appFamily) {
        return mUiccController.getIccRecords(mPhone.getPhoneId(), appFamily);
    }


    private void onUpdateIcc() {
        if (mUiccController == null ) {
            return;
        }

        IccRecords newIccRecords = getUiccRecords(UiccController.APP_FAM_3GPP);

        // M: Fix AOSP always get 3GPP when Phone is CDMA.
        if (newIccRecords == null && mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            // M:  CDMALTEPhone gets 3GPP above, pure CDMA card gets 3GPP2 here.
            newIccRecords = getUiccRecords(UiccController.APP_FAM_3GPP2);
        }

        IccRecords r = mIccRecords.get();
        if (VDBG) log("onUpdateIcc: newIccRecords=" + newIccRecords + ", r=" + r);

        if (r != newIccRecords) {
            if (r != null) {
                log("Removing stale icc objects.");
                r.unregisterForRecordsLoaded(this);
                mIccRecords.set(null);
            }
            if (newIccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
                    log("New records found.");
                    mIccRecords.set(newIccRecords);
                    newIccRecords.registerForRecordsLoaded(
                            this, DctConstants.EVENT_RECORDS_LOADED, null);
                    SubscriptionController.getInstance().setSimProvisioningStatus(
                            SubscriptionManager.SIM_PROVISIONED, mPhone.getSubId());
                }
            } else {
                onSimNotReady();
            }
        }

        if (mAllApnSettings != null && r == null && newIccRecords == null) {
            // M: clear mAllApnSettings in main thread to avoid concurrent access exception.
            post(new Runnable() {
                @Override
                public void run() {
                    if (VDBG) log("onUpdateIcc: clear mAllApnSettings, " +
                            (mAllApnSettings != null));
                    if (mAllApnSettings != null) {
                        mAllApnSettings.clear();
                    }
                }
            });
        }

        //MTK START: FDN Support
        UiccCardApplication app = mUiccCardApplication.get();
        UiccCardApplication newUiccCardApp = mUiccController.getUiccCardApplication(
                mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ?
                UiccController.APP_FAM_3GPP2 : UiccController.APP_FAM_3GPP);

        if (app != newUiccCardApp) {
            if (app != null) {
                log("Removing stale UiccCardApplication objects.");
                app.unregisterForFdnChanged(this);
                mUiccCardApplication.set(null);
            }

            if (newUiccCardApp != null) {
                log("New UiccCardApplication found");
                newUiccCardApp.registerForFdnChanged(this, DctConstants.EVENT_FDN_CHANGED, null);
                mUiccCardApplication.set(newUiccCardApp);
            }
        }
        //MTK END: FDN Support
    }

    public void update() {
        log("update sub = " + mPhone.getSubId());
        onUpdateIcc();

        mUserDataEnabled = getDataEnabled();
        mAutoAttachOnCreation.set(false);

        ((GsmCdmaPhone)mPhone).updateCurrentCarrierInProvider();
    }

    public void cleanUpAllConnections(String cause) {
        cleanUpAllConnections(cause, null);
    }

    public void updateRecords() {
        onUpdateIcc();
    }

    public void cleanUpAllConnections(String cause, Message disconnectAllCompleteMsg) {
        log("cleanUpAllConnections");
        if (disconnectAllCompleteMsg != null) {
            mDisconnectAllCompleteMsgList.add(disconnectAllCompleteMsg);
        }

        Message msg = obtainMessage(DctConstants.EVENT_CLEAN_UP_ALL_CONNECTIONS);
        msg.obj = cause;
        sendMessage(msg);
    }

    private boolean checkCarrierAppAvailable(Intent intent) {
        // Read from carrier config manager
        String[] activationApp = getActivationAppName();
        if(activationApp == null || activationApp.length != 2) {
            return false;
        }

        intent.setClassName(activationApp[0], activationApp[1]);
        // Check if activation app is available
        final PackageManager packageManager = mPhone.getContext().getPackageManager();
        if (packageManager.queryBroadcastReceivers(intent,
                PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
            loge("Activation Carrier app is configured, but not available: "
                    + activationApp[0] + "." + activationApp[1]);
            return false;
        }
        return true;
    }

    private boolean notifyCarrierAppWithIntent(Intent intent) {
        // RIL has limitation to process new request while there is pending deactivation requests
        // Make sure there is no pending disconnect before launching carrier app
        if (mDisconnectPendingCount != 0) {
            loge("Wait for pending disconnect requests done");
            return false;
        }
        if (!checkCarrierAppAvailable(intent)) {
            loge("Carrier app is unavailable");
            return false;
        }

        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mPhone.getSubId());
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        try {
            mPhone.getContext().sendBroadcast(intent);
        } catch (ActivityNotFoundException e) {
            loge("sendBroadcast failed: " + e);
            return false;
        }

        if (DBG) log("send Intent to Carrier app with action: " + intent.getAction());
        return true;
    }

    private void notifyCarrierAppForRedirection() {
        // Notify carrier app with redirectionUrl
        if (!isColdSimDetected() && !isOutOfCreditSimDetected() && mRedirectUrl != null) {
            Intent intent = new Intent(TelephonyIntents.ACTION_DATA_CONNECTION_REDIRECTED);
            intent.putExtra(REDIRECTION_URL_KEY, mRedirectUrl);
            if (notifyCarrierAppWithIntent(intent)) mRedirectUrl = null;
        }
    }

    private void notifyDataDisconnectComplete() {
        log("notifyDataDisconnectComplete");
        for (Message m: mDisconnectAllCompleteMsgList) {
            m.sendToTarget();
        }
        mDisconnectAllCompleteMsgList.clear();
    }


    private void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        mFailFast = false;
        mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        mAllDataDisconnectedRegistrants.addUnique(h, what, obj);

        if (isDisconnected()) {
            log("notify All Data Disconnected");
            notifyAllDataDisconnected();
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        mAllDataDisconnectedRegistrants.remove(h);
    }


    private void onSetInternalDataEnabled(boolean enabled, Message onCompleteMsg) {
        if (DBG) log("onSetInternalDataEnabled: enabled=" + enabled);
        boolean sendOnComplete = true;

        synchronized (mDataEnabledLock) {
            mInternalDataEnabled = enabled;
            if (enabled) {
                log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
                onTrySetupData(Phone.REASON_DATA_ENABLED);
            } else {
                sendOnComplete = false;
                log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
                cleanUpAllConnections(null, onCompleteMsg);
            }
        }

        if (sendOnComplete) {
            if (onCompleteMsg != null) {
                onCompleteMsg.sendToTarget();
            }
        }
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        if (DBG) log("setInternalDataEnabledFlag(" + enable + ")");

        if (mInternalDataEnabled != enable) {
            mInternalDataEnabled = enable;
        }
        return true;
    }

    public boolean setInternalDataEnabled(boolean enable) {
        return setInternalDataEnabled(enable, null);
    }

    public boolean setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        if (DBG) log("setInternalDataEnabled(" + enable + ")");

        Message msg = obtainMessage(DctConstants.EVENT_SET_INTERNAL_DATA_ENABLE, onCompleteMsg);
        msg.arg1 = (enable ? DctConstants.ENABLED : DctConstants.DISABLED);
        sendMessage(msg);
        return true;
    }

    public void setDataAllowed(boolean enable, Message response) {
         if (DBG) log("setDataAllowed: enable=" + enable);
         isCleanupRequired.set(!enable);
         mPhone.mCi.setDataAllowed(enable, response);
         mInternalDataEnabled = enable;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DcTracker:");
        pw.println(" RADIO_TESTS=" + RADIO_TESTS);
        pw.println(" mInternalDataEnabled=" + mInternalDataEnabled);
        pw.println(" mUserDataEnabled=" + mUserDataEnabled);
        pw.println(" sPolicyDataEnabed=" + sPolicyDataEnabled);
        pw.flush();
        pw.println(" mRequestedApnType=" + mRequestedApnType);
        pw.println(" mPhone=" + mPhone.getPhoneName());
        pw.println(" mActivity=" + mActivity);
        pw.println(" mState=" + mState);
        pw.println(" mTxPkts=" + mTxPkts);
        pw.println(" mRxPkts=" + mRxPkts);
        pw.println(" mNetStatPollPeriod=" + mNetStatPollPeriod);
        pw.println(" mNetStatPollEnabled=" + mNetStatPollEnabled);
        pw.println(" mDataStallTxRxSum=" + mDataStallTxRxSum);
        pw.println(" mDataStallAlarmTag=" + mDataStallAlarmTag);
        pw.println(" mDataStallDetectionEanbled=" + mDataStallDetectionEnabled);
        pw.println(" mSentSinceLastRecv=" + mSentSinceLastRecv);
        pw.println(" mNoRecvPollCount=" + mNoRecvPollCount);
        pw.println(" mResolver=" + mResolver);
        pw.println(" mIsWifiConnected=" + mIsWifiConnected);
        pw.println(" mReconnectIntent=" + mReconnectIntent);
        pw.println(" mAutoAttachOnCreation=" + mAutoAttachOnCreation.get());
        pw.println(" mIsScreenOn=" + mIsScreenOn);
        pw.println(" mUniqueIdGenerator=" + mUniqueIdGenerator);
        pw.flush();
        pw.println(" ***************************************");
        DcController dcc = mDcc;
        if (dcc != null) {
            dcc.dump(fd, pw, args);
        } else {
            pw.println(" mDcc=null");
        }
        pw.println(" ***************************************");
        HashMap<Integer, DataConnection> dcs = mDataConnections;
        if (dcs != null) {
            Set<Entry<Integer, DataConnection> > mDcSet = mDataConnections.entrySet();
            pw.println(" mDataConnections: count=" + mDcSet.size());
            for (Entry<Integer, DataConnection> entry : mDcSet) {
                pw.printf(" *** mDataConnection[%d] \n", entry.getKey());
                entry.getValue().dump(fd, pw, args);
            }
        } else {
            pw.println("mDataConnections=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        HashMap<String, Integer> apnToDcId = mApnToDataConnectionId;
        if (apnToDcId != null) {
            Set<Entry<String, Integer>> apnToDcIdSet = apnToDcId.entrySet();
            pw.println(" mApnToDataConnectonId size=" + apnToDcIdSet.size());
            for (Entry<String, Integer> entry : apnToDcIdSet) {
                pw.printf(" mApnToDataConnectonId[%s]=%d\n", entry.getKey(), entry.getValue());
            }
        } else {
            pw.println("mApnToDataConnectionId=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        ConcurrentHashMap<String, ApnContext> apnCtxs = mApnContexts;
        if (apnCtxs != null) {
            Set<Entry<String, ApnContext>> apnCtxsSet = apnCtxs.entrySet();
            pw.println(" mApnContexts size=" + apnCtxsSet.size());
            for (Entry<String, ApnContext> entry : apnCtxsSet) {
                entry.getValue().dump(fd, pw, args);
            }
            pw.println(" ***************************************");
        } else {
            pw.println(" mApnContexts=null");
        }
        pw.flush();
        ArrayList<ApnSetting> apnSettings = mAllApnSettings;
        if (apnSettings != null) {
            pw.println(" mAllApnSettings size=" + apnSettings.size());
            for (int i=0; i < apnSettings.size(); i++) {
                pw.printf(" mAllApnSettings[%d]: %s\n", i, apnSettings.get(i));
            }
            pw.flush();
        } else {
            pw.println(" mAllApnSettings=null");
        }
        pw.println(" mPreferredApn=" + mPreferredApn);
        pw.println(" mIsPsRestricted=" + mIsPsRestricted);
        pw.println(" mIsDisposed=" + mIsDisposed);
        pw.println(" mIntentReceiver=" + mIntentReceiver);
        pw.println(" mReregisterOnReconnectFailure=" + mReregisterOnReconnectFailure);
        pw.println(" canSetPreferApn=" + mCanSetPreferApn);
        pw.println(" mApnObserver=" + mApnObserver);
        pw.println(" getOverallState=" + getOverallState());
        pw.println(" mDataConnectionAsyncChannels=%s\n" + mDataConnectionAcHashMap);
        pw.println(" mAttached=" + mAttached.get());
        pw.flush();
    }

    public String[] getPcscfAddress(String apnType) {
        log("getPcscfAddress()");
        ApnContext apnContext = null;

        if(apnType == null){
            log("apnType is null, return null");
            return null;
        }

        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_EMERGENCY)) {
            apnContext = mApnContextsById.get(DctConstants.APN_EMERGENCY_ID);
        } else if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_IMS)) {
            apnContext = mApnContextsById.get(DctConstants.APN_IMS_ID);
        } else {
            log("apnType is invalid, return null");
            return null;
        }

        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }

        DcAsyncChannel dcac = apnContext.getDcAc();
        String[] result = null;

        if (dcac != null) {
            result = dcac.getPcscfAddr();

            for (int i = 0; i < result.length; i++) {
                log("Pcscf[" + i + "]: " + result[i]);
            }
            return result;
        }
        return null;
    }

    /**
     * Read APN configuration from Telephony.db for Emergency APN
     * All opertors recognize the connection request for EPDN based on APN type
     * PLMN name,APN name are not mandatory parameters
     */
    private void initEmergencyApnSetting() {
        // Operator Numeric is not available when sim records are not loaded.
        // Query Telephony.db with APN type as EPDN request does not
        // require APN name, plmn and all operators support same APN config.
        // DB will contain only one entry for Emergency APN
        String selection = "type=\"emergency\"";
        Cursor cursor = mPhone.getContext().getContentResolver().query(
                Telephony.Carriers.CONTENT_URI, null, selection, null, null);

        if (cursor != null) {
            if (cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    mEmergencyApn = makeApnSetting(cursor);
                }
            }
            cursor.close();
        }
    }

    /**
     * Add the Emergency APN settings to APN settings list
     */
    private void addEmergencyApnSetting() {
        if(mEmergencyApn != null) {
            if(mAllApnSettings == null) {
                mAllApnSettings = new ArrayList<ApnSetting>();
            } else {
                boolean hasEmergencyApn = false;
                for (ApnSetting apn : mAllApnSettings) {
                    if (ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_EMERGENCY)) {
                        hasEmergencyApn = true;
                        break;
                    }
                }

                if(hasEmergencyApn == false) {
                    mAllApnSettings.add(mEmergencyApn);
                } else {
                    log("addEmergencyApnSetting - E-APN setting is already present");
                }
            }
        }
    }

    private void cleanUpConnectionsOnUpdatedApns(boolean tearDown) {
        if (DBG) log("cleanUpConnectionsOnUpdatedApns: tearDown=" + tearDown);
        if (mAllApnSettings.isEmpty()) {
            cleanUpAllConnections(tearDown, Phone.REASON_APN_CHANGED);
        } else {
            for (ApnContext apnContext : mApnContexts.values()) {
                if (VDBG) log("cleanUpConnectionsOnUpdatedApns for "+ apnContext);

                boolean cleanUpApn = true;
                ArrayList<ApnSetting> currentWaitingApns = apnContext.getWaitingApns();

                if ((currentWaitingApns != null) && (!apnContext.isDisconnected())) {
                    int radioTech = mPhone.getServiceState().getRilDataRadioTechnology();
                    ArrayList<ApnSetting> waitingApns = buildWaitingApns(
                            apnContext.getApnType(), radioTech);
                    if (VDBG) log("new waitingApns:" + waitingApns);
                    if (waitingApns.size() == currentWaitingApns.size()) {
                        cleanUpApn = false;
                        for (int i = 0; i < waitingApns.size(); i++) {
                            final String currentWaitingApn =
                                    currentWaitingApns.get(i).toStringIgnoreName(!VZW_FEATURE);
                            final String waitingApn = waitingApns.get(i).toStringIgnoreName(
                                    !VZW_FEATURE);
                            if (!TextUtils.equals(currentWaitingApn, waitingApn)) {
                                if (VDBG) log("new waiting apn is different at " + i);
                                cleanUpApn = true;
                                apnContext.setWaitingApns(waitingApns);
                                break;
                            }
                        }
                    }
                }

                if (cleanUpApn) {
                    apnContext.setReason(Phone.REASON_APN_CHANGED);
                    cleanUpConnection(true, apnContext);
                }
            }
        }

        if (!isConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }

        mRequestedApnType = PhoneConstants.APN_TYPE_DEFAULT;

        if (DBG) log("mDisconnectPendingCount = " + mDisconnectPendingCount);
        if (tearDown && mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
    }

    /** M: worker handler to handle DB/IO access */
    private void createWorkerHandler() {
        if (mWorkerHandler == null) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    mWorkerHandler = new WorkerHandler();
                    mWorkerHandler.sendEmptyMessage(DctConstants.EVENT_INIT_EMERGENCY_APN_SETTINGS);
                    Looper.loop();
                }
            };
            thread.start();
        }
    }

    private class WorkerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DctConstants.EVENT_INIT_EMERGENCY_APN_SETTINGS:
                    log("start loading pdn info");
                    initEmergencyApnSetting();
                    addEmergencyApnSetting();
                    break;
                case DctConstants.EVENT_CHECK_FDN_LIST:
                    cleanOrSetupDataConnByCheckFdn();
                    break;
            }
        }
    }

    /** M: throttling/high throughput
     *  Used to specified the maximum concurrent data connections
     */
    protected int getPdpConnectionPoolSize() {
        //here we keep the last DataConnection for low throughput APN
        //so the pool size is the maximum value - 1
        if (isSupportThrottlingApn()) {
            return THROTTLING_MAX_PDP_SIZE;
        } else {
            //here we keep the last DataConnection for low throughput APN
            //so the pool size is the maximum value - 1
            return PDP_CONNECTION_POOL_SIZE - 1 > 0 ? PDP_CONNECTION_POOL_SIZE - 1 : 1;
        }
    }

    private boolean isSupportThrottlingApn() {
        return (THROTTLING_APN_ENABLED || (SystemProperties.getInt(
                PROPERTY_THROTTLING_APN_ENABLED, 0) == 1));
    }

    private int generateDataConnectionId(String reqApnType, ApnSetting apnSetting) {
        int id = -1;
        // 0: internet, 1: tethering, 2~3: others, 4~6: IMS (non-throttling), 7: eMBMS
        AtomicInteger idGenerator = mOthersUniqueIdGenerator;
        for (String apn : HIGH_THROUGHPUT_APN) {
            if (apnSetting != null && apnSetting.canHandleType(apn)
                    && !PhoneConstants.APN_TYPE_EMERGENCY.equals(reqApnType)
                    && !apnSetting.canHandleType(PhoneConstants.APN_TYPE_IMS)) {
                idGenerator = mHighThroughputIdGenerator;
                log("generateDataConnectionId use high throughput DataConnection id generator");
                break;
            }
        }
        if (idGenerator != mHighThroughputIdGenerator) {
            for (String apn : IMS_APN) {
                if (PhoneConstants.APN_TYPE_EMERGENCY.equals(apn)
                        && !PhoneConstants.APN_TYPE_EMERGENCY.equals(reqApnType)) {
                    //skip since not request emergency apn
                } else {
                    if (apnSetting != null && apnSetting.canHandleType(apn)) {
                        int idStart = MIN_ID_IMS_TROUGHPUT;
                        if (PhoneConstants.APN_TYPE_EMERGENCY.equals(apn)) {
                            idStart += 1;
                        }
                        // Set IMS: 4, EIMS: 5 for interface (fixed the interface)
                        mImsUniqueIdGenerator.set(idStart);
                        idGenerator = mImsUniqueIdGenerator;
                        log("generateDataConnectionId use ims DataConnection id generator");
                        break;
                    }
                }
            }
        }

        id = idGenerator.getAndIncrement();
        if (idGenerator == mHighThroughputIdGenerator && id > MAX_ID_HIGH_TROUGHPUT) {
            loge("Max id of highthrouthput is " + MAX_ID_HIGH_TROUGHPUT
                    + ", but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        } else if (idGenerator == mOthersUniqueIdGenerator && id > MAX_ID_OTHERS_TROUGHPUT) {
            loge("Max id of others is " + MAX_ID_OTHERS_TROUGHPUT
                    + ", but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        } else if (idGenerator == mImsUniqueIdGenerator && id > MAX_ID_IMS_TROUGHPUT) {
            loge("Max id of others is " + MAX_ID_IMS_TROUGHPUT
                    + ", but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        }
        if (DBG) {
            log("generateDataConnectionId id = " + id);
        }
        return id;
    }

    // MTK
    public void deactivatePdpByCid(int cid) {
        mPhone.mCi.deactivateDataCall(cid, RILConstants.DEACTIVATE_REASON_PDP_RESET,
                                      obtainMessage(DctConstants.EVENT_RESET_PDP_DONE, cid, 0));
    }

    // M: isVsimActive.
    public boolean isVsimActive(int phoneId) {
        int phoneNum = TelephonyManager.getDefault().getPhoneCount();
        String vsimEnabled = null;
        int act = 0 ;

        for (int id = 0 ; id < phoneNum ; id++) {
            if (id != phoneId) {
                vsimEnabled = TelephonyManager.getDefault().getTelephonyProperty(
                        id, PROPERTY_VSIM_ENABLE, "0");
                act = ((vsimEnabled.isEmpty()) ? 0 : Integer.parseInt(vsimEnabled));
                if (act == 2) {
                    if (DBG) log("Remote Vsim enabled on phone " + id +
                            " and downloaded by phone" + phoneId);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * M: syncApnToMd. Request for sync APN table to MD.
     */
    private void syncApnToMd() {
        log("syncApnToMd");
        if (mAllApnSettings != null && !mAllApnSettings.isEmpty()) {
            ApnSetting apn;
            // The throttling value should be able to modified by EM,
            // read it from system properites. Default value is 15 mins.
            int throttlingTime = SystemProperties.getInt(PROPERTY_THROTTLING_TIME,
                    THROTTLING_TIME_DEFAULT);

            String hPlmn =
                TelephonyManager.getDefault().getSimOperatorNumericForPhone(mPhone.getPhoneId());
            log("syncApnToMd: hPlmn = " + hPlmn + ", HPLMN_OP12 = " + PLMN_OP12);
            if (PLMN_OP12.equals(hPlmn) || MTK_APNSYNC_TEST_SUPPORT) {
                for (int i = 0; i < mAllApnSettings.size(); i++) {
                    apn = mAllApnSettings.get(i);
                    mPhone.mCi.syncApnTable(String.valueOf(i),
                            String.valueOf(getClassType(apn)),
                            apn.apn, apn.protocol,
                            "LTE", //String.valueOf(apn.bearer)
                            apn.carrierEnabled ? "Enabled" : "Disabled",
                            "0",
                            String.valueOf(apn.maxConns),
                            String.valueOf(apn.maxConnsTime),
                            String.valueOf(apn.waitTime),
                            String.valueOf(throttlingTime),
                            String.valueOf(apn.inactiveTimer),
                            null);
                }
            }
        } else {
            log("syncApnToMd: All ApnSettings are null or empty!");
        }
    }

    /**
     * M: getClassType.
     *
     * @param apn ApnSetting
     * @return int for class type
     */
    public int getClassType(ApnSetting apn) {
        int classType = APN_CLASS_3;

        if (ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_EMERGENCY)
            || VZW_EMERGENCY_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = APN_CLASS_0;
        } else if (ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_IMS)
            || VZW_IMS_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = APN_CLASS_1;
        } else if (VZW_ADMIN_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = APN_CLASS_2;
        } else if (VZW_APP_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = APN_CLASS_4;
        } else if (VZW_800_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = APN_CLASS_5;
        } else if (ArrayUtils.contains(apn.types, PhoneConstants.APN_TYPE_DEFAULT)) {
            classType = APN_CLASS_3;
        } else {
            log("getClassType: set to default class 3");
        }

        log("getClassType:" + classType);
        return classType;
    }

    /**
     * M: getClassTypeApn.
     *
     * @param classType APN class type
     * @return ApnSetting for class type apn
     */
    public ApnSetting getClassTypeApn(int classType) {
        ApnSetting classTypeApn = null;
        String apnName = "";

        if (APN_CLASS_0 == classType) {
            apnName = VZW_EMERGENCY_NI;
        } else if (APN_CLASS_1 == classType) {
            apnName = VZW_IMS_NI;
        } else if (APN_CLASS_2 == classType) {
            apnName = VZW_ADMIN_NI;
        } else if (APN_CLASS_3 == classType) {
            apnName = VZW_INTERNET_NI;
        } else if (APN_CLASS_4 == classType) {
            apnName = VZW_APP_NI;
        } else if (APN_CLASS_5 == classType) {
            apnName = VZW_800_NI;
        } else {
            log("getClassTypeApn: can't handle class:" + classType);
            return null;
        }

        if (mAllApnSettings != null) {
            for (ApnSetting apn : mAllApnSettings) {
                if (apnName.compareToIgnoreCase(apn.apn) == 0) {
                    classTypeApn = apn;
                }
            }
        }

        log("getClassTypeApn:" + classTypeApn + ", class:" + classType);
        return classTypeApn;
    }

    // M: [LTE][Low Power][UL traffic shaping] Start
    private void onSharedDefaultApnState(int newDefaultRefCount) {
        if (DBG) {
            log("onSharedDefaultApnState: newDefaultRefCount = " + newDefaultRefCount
                    + ", curDefaultRefCount = " + mDefaultRefCount);
        }

        if(newDefaultRefCount != mDefaultRefCount) {
            if (newDefaultRefCount > 1) {
                mIsSharedDefaultApn = true;
            } else {
                mIsSharedDefaultApn = false;
            }
            mDefaultRefCount = newDefaultRefCount;
            if (DBG) {
                log("onSharedDefaultApnState: mIsSharedDefaultApn = " + mIsSharedDefaultApn);
            }
            notifySharedDefaultApn(mIsSharedDefaultApn);
        }
    }

    public void onSetLteAccessStratumReport(boolean enabled, Message response) {
        mPhone.mCi.setLteAccessStratumReport(enabled, response);
    }

    public void onSetLteUplinkDataTransfer(int timeMillis, Message response) {
        for(ApnContext apnContext : mApnContexts.values()) {
            if(PhoneConstants.APN_TYPE_DEFAULT.equals(apnContext.getApnType())) {
                try {
                    int interfaceId = apnContext.getDcAc().getCidSync();
                    mPhone.mCi.setLteUplinkDataTransfer(timeMillis, interfaceId, response);
                } catch (Exception e) {
                    loge("getDcAc fail!");
                    e.printStackTrace();
                    if (response != null) {
                        AsyncResult.forMessage(response, null,
                                new CommandException(CommandException.Error.GENERIC_FAILURE));
                        response.sendToTarget();
                    }
                }
            }
        }
    }

    private void notifySharedDefaultApn(boolean isSharedDefaultApn) {
        mPhone.notifySharedDefaultApnStateChanged(isSharedDefaultApn);
    }

    // TODO: Should this move to NW frameworks to handle?
    private void notifyLteAccessStratumChanged(int lteAccessStratumDataState) {
        mLteAccessStratumDataState = (lteAccessStratumDataState == LTE_AS_CONNECTED) ?
                PhoneConstants.LTE_ACCESS_STRATUM_STATE_CONNECTED :
                PhoneConstants.LTE_ACCESS_STRATUM_STATE_IDLE;
        if (DBG) {
            log("notifyLteAccessStratumChanged mLteAccessStratumDataState = "
                    + mLteAccessStratumDataState);
        }
        mPhone.notifyLteAccessStratumChanged(mLteAccessStratumDataState);
    }

    // TODO: Should this move to NW frameworks to handle?
    private void notifyPsNetworkTypeChanged(int newRilNwType) {
        int newNwType = mPhone.getServiceState().rilRadioTechnologyToNetworkTypeEx(newRilNwType);
        if (DBG) {
            log("notifyPsNetworkTypeChanged mNetworkType = " + mNetworkType
                    + ", newNwType = " + newNwType
                    + ", newRilNwType = " + newRilNwType);
        }
        if (newNwType != mNetworkType) {
            mNetworkType = newNwType;
            mPhone.notifyPsNetworkTypeChanged(mNetworkType);
        }
    }

    public String getLteAccessStratumState() {
        return mLteAccessStratumDataState;
    }

    public boolean isSharedDefaultApn() {
        return mIsSharedDefaultApn;
    }
    // M: [LTE][Low Power][UL traffic shaping] End

    /**
     * Polling stuff
     */
    private void resetPollStats() {
        mTxPkts = -1;
        mRxPkts = -1;
        mNetStatPollPeriod = POLL_NETSTAT_MILLIS;
    }

    private void startNetStatPoll() {
        if (getOverallState() == DctConstants.State.CONNECTED
                && mNetStatPollEnabled == false) {
            if (DBG) {
                log("startNetStatPoll");
            }
            resetPollStats();
            mNetStatPollEnabled = true;
            mPollNetStat.run();
        }
        if (mPhone != null) {
            mPhone.notifyDataActivity();
        }
    }

    private void stopNetStatPoll() {
        mNetStatPollEnabled = false;
        removeCallbacks(mPollNetStat);
        if (DBG) {
            log("stopNetStatPoll");
        }

        // To sync data activity icon in the case of switching data connection to send MMS.
        if (mPhone != null) {
            mPhone.notifyDataActivity();
        }
    }

    public void sendStartNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(DctConstants.CMD_NET_STAT_POLL);
        msg.arg1 = DctConstants.ENABLED;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStartNetStatPoll(DctConstants.Activity activity) {
        startNetStatPoll();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
        setActivity(activity);
    }

    public void sendStopNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(DctConstants.CMD_NET_STAT_POLL);
        msg.arg1 = DctConstants.DISABLED;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStopNetStatPoll(DctConstants.Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    private void updateDataActivity() {
        long sent, received;

        DctConstants.Activity newActivity;

        TxRxSum preTxRxSum = new TxRxSum(mTxPkts, mRxPkts);
        TxRxSum curTxRxSum = new TxRxSum();
        String strOperatorNumeric =
                TelephonyManager.getDefault().getSimOperatorNumericForPhone(mPhone.getPhoneId());
        if (TextUtils.equals(strOperatorNumeric, "732101")) {
            curTxRxSum.updateTxRxSum();
        } else {
            curTxRxSum.updateTcpTxRxSum();
        }
        mTxPkts = curTxRxSum.txPkts;
        mRxPkts = curTxRxSum.rxPkts;

        if (VDBG) {
            log("updateDataActivity: curTxRxSum=" + curTxRxSum + " preTxRxSum=" + preTxRxSum);
        }

        if (mNetStatPollEnabled && (preTxRxSum.txPkts > 0 || preTxRxSum.rxPkts > 0)) {
            sent = mTxPkts - preTxRxSum.txPkts;
            received = mRxPkts - preTxRxSum.rxPkts;

            if (VDBG)
                log("updateDataActivity: sent=" + sent + " received=" + received);
            if (sent > 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAINANDOUT;
            } else if (sent > 0 && received == 0) {
                newActivity = DctConstants.Activity.DATAOUT;
            } else if (sent == 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAIN;
            } else {
                newActivity = (mActivity == DctConstants.Activity.DORMANT) ?
                        mActivity : DctConstants.Activity.NONE;
            }

            if (mActivity != newActivity && mIsScreenOn) {
                if (VDBG)
                    log("updateDataActivity: newActivity=" + newActivity);
                mActivity = newActivity;
                mPhone.notifyDataActivity();
            }
        }
    }

    /**
     * Data-Stall
     */
    // Recovery action taken in case of data stall
    private static class RecoveryAction {
        public static final int GET_DATA_CALL_LIST      = 0;
        public static final int CLEANUP                 = 1;
        public static final int REREGISTER              = 2;
        public static final int RADIO_RESTART           = 3;
        public static final int RADIO_RESTART_WITH_PROP = 4;

        private static boolean isAggressiveRecovery(int value) {
            return ((value == RecoveryAction.CLEANUP) ||
                    (value == RecoveryAction.REREGISTER) ||
                    (value == RecoveryAction.RADIO_RESTART) ||
                    (value == RecoveryAction.RADIO_RESTART_WITH_PROP));
        }
    }

    private int getRecoveryAction() {
        int action = Settings.System.getInt(mResolver,
                "radio.data.stall.recovery.action", RecoveryAction.GET_DATA_CALL_LIST);
        if (VDBG_STALL) log("getRecoveryAction: " + action);
        return action;
    }

    private void putRecoveryAction(int action) {
        Settings.System.putInt(mResolver, "radio.data.stall.recovery.action", action);
        if (VDBG_STALL) log("putRecoveryAction: " + action);
    }

    private void doRecovery() {
        if (getOverallState() == DctConstants.State.CONNECTED) {
            // Go through a series of recovery steps, each action transitions to the next action
            int recoveryAction = getRecoveryAction();
            switch (recoveryAction) {
            case RecoveryAction.GET_DATA_CALL_LIST:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST,
                        mSentSinceLastRecv);
                if (DBG) log("doRecovery() get data call list");
                mPhone.mCi.getDataCallList(obtainMessage(DctConstants.EVENT_DATA_STATE_CHANGED));
                putRecoveryAction(RecoveryAction.CLEANUP);
                break;
            case RecoveryAction.CLEANUP:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP, mSentSinceLastRecv);
                /* M: Start - abnormal event logging for logger */
                Intent intent = new Intent(TelephonyIntents.ACTION_EXCEPTION_HAPPENED);
                intent.putExtra("Reason", "SmartLogging");
                intent.putExtra("from_where", "DCT");
                mPhone.getContext().sendBroadcast(intent);
                log("Broadcast for SmartLogging - NO DATA");
                /* M: End - abnormal event logging for logger */
                if (DBG) log("doRecovery() cleanup all connections");
                cleanUpAllConnections(Phone.REASON_PDP_RESET);
                putRecoveryAction(RecoveryAction.REREGISTER);
                break;
            case RecoveryAction.REREGISTER:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_REREGISTER,
                        mSentSinceLastRecv);
                if (DBG) log("doRecovery() re-register");
                mPhone.getServiceStateTracker().reRegisterNetwork(null); // AOSP
                // M: re-register PS domain only
                //   Not to use mPhone.getServiceStateTracker().reRegisterNetwork
                //   Re-register may not be triggered by it and both CS and PS could be impacted
                //   FIXME: To choose AOSP or use MTK soltuion.
                /// DataConnectionHelper.getInstance().reRegisterPsNetwork();
                putRecoveryAction(RecoveryAction.RADIO_RESTART);
                break;
            case RecoveryAction.RADIO_RESTART:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART,
                        mSentSinceLastRecv);
                if (DBG) log("restarting radio");
                putRecoveryAction(RecoveryAction.RADIO_RESTART_WITH_PROP);
                restartRadio();
                break;
            case RecoveryAction.RADIO_RESTART_WITH_PROP:
                // This is in case radio restart has not recovered the data.
                // It will set an additional "gsm.radioreset" property to tell
                // RIL or system to take further action.
                // The implementation of hard reset recovery action is up to OEM product.
                // Once RADIO_RESET property is consumed, it is expected to set back
                // to false by RIL.
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART_WITH_PROP, -1);
                if (DBG) log("restarting radio with gsm.radioreset to true");
                SystemProperties.set(RADIO_RESET_PROPERTY, "true");
                // give 1 sec so property change can be notified.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                restartRadio();
                putRecoveryAction(RecoveryAction.GET_DATA_CALL_LIST);
                break;
            default:
                throw new RuntimeException("doRecovery: Invalid recoveryAction=" +
                    recoveryAction);
            }
            mSentSinceLastRecv = 0;
        }
    }

    private void updateDataStallInfo() {
        long sent, received;

        TxRxSum preTxRxSum = new TxRxSum(mDataStallTxRxSum);
        String strOperatorNumeric =
                TelephonyManager.getDefault().getSimOperatorNumericForPhone(mPhone.getPhoneId());
        if (TextUtils.equals(strOperatorNumeric, "732101")) {
            mDataStallTxRxSum.updateTxRxSum();
        } else {
            mDataStallTxRxSum.updateTcpTxRxSum();
        }

        if (VDBG_STALL) {
            log("updateDataStallInfo: mDataStallTxRxSum=" + mDataStallTxRxSum +
                    " preTxRxSum=" + preTxRxSum);
        }

        sent = mDataStallTxRxSum.txPkts - preTxRxSum.txPkts;
        received = mDataStallTxRxSum.rxPkts - preTxRxSum.rxPkts;

        if (RADIO_TESTS) {
            if (SystemProperties.getBoolean("radio.test.data.stall", false)) {
                log("updateDataStallInfo: radio.test.data.stall true received = 0;");
                received = 0;
            }
        }
        if ( sent > 0 && received > 0 ) {
            if (VDBG_STALL) log("updateDataStallInfo: IN/OUT");
            mSentSinceLastRecv = 0;
            putRecoveryAction(RecoveryAction.GET_DATA_CALL_LIST);
        } else if (sent > 0 && received == 0) {
            if (mPhone.getState() == PhoneConstants.State.IDLE) {
                mSentSinceLastRecv += sent;
            } else {
                mSentSinceLastRecv = 0;
            }
            if (DBG) {
                log("updateDataStallInfo: OUT sent=" + sent +
                        " mSentSinceLastRecv=" + mSentSinceLastRecv);
            }
        } else if (sent == 0 && received > 0) {
            if (VDBG_STALL) log("updateDataStallInfo: IN");
            mSentSinceLastRecv = 0;
            putRecoveryAction(RecoveryAction.GET_DATA_CALL_LIST);
        } else {
            if (VDBG_STALL) log("updateDataStallInfo: NONE");
        }
    }

    private void onDataStallAlarm(int tag) {
        if (mDataStallAlarmTag != tag) {
            if (DBG) {
                log("onDataStallAlarm: ignore, tag=" + tag + " expecting " + mDataStallAlarmTag);
            }
            return;
        }
        updateDataStallInfo();

        int hangWatchdogTrigger = Settings.Global.getInt(mResolver,
                Settings.Global.PDP_WATCHDOG_TRIGGER_PACKET_COUNT,
                NUMBER_SENT_PACKETS_OF_HANG);

        boolean suspectedStall = DATA_STALL_NOT_SUSPECTED;
        if (mSentSinceLastRecv >= hangWatchdogTrigger) {
            if (DBG) {
                log("onDataStallAlarm: tag=" + tag + " do recovery action=" + getRecoveryAction());
            }
            if (isOnlyIMSorEIMSPdnConnected() || skipDataStallAlarm()) {
                log("onDataStallAlarm: only IMS or EIMS Connected, or switch data-stall off, "
                        + "skip it!");
            } else {
                suspectedStall = DATA_STALL_SUSPECTED;
                sendMessage(obtainMessage(DctConstants.EVENT_DO_RECOVERY));
            }
        } else {
            if (VDBG_STALL) {
                log("onDataStallAlarm: tag=" + tag + " Sent " + String.valueOf(mSentSinceLastRecv) +
                    " pkts since last received, < watchdogTrigger=" + hangWatchdogTrigger);
            }
        }
        startDataStallAlarm(suspectedStall);
    }

    private void startDataStallAlarm(boolean suspectedStall) {
        int nextAction = getRecoveryAction();
        int delayInMs;

        if (mDataStallDetectionEnabled && getOverallState() == DctConstants.State.CONNECTED) {
            // If screen is on or data stall is currently suspected, set the alarm
            // with an aggressive timeout.
            if (mIsScreenOn || suspectedStall || RecoveryAction.isAggressiveRecovery(nextAction)) {
                delayInMs = Settings.Global.getInt(mResolver,
                        Settings.Global.DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS,
                        DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            } else {
                delayInMs = Settings.Global.getInt(mResolver,
                        Settings.Global.DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS,
                        DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            }

            mDataStallAlarmTag += 1;
            if (VDBG_STALL) {
                log("startDataStallAlarm: tag=" + mDataStallAlarmTag +
                        " delay=" + (delayInMs / 1000) + "s");
            }
            Intent intent = new Intent(INTENT_DATA_STALL_ALARM);
            intent.putExtra(DATA_STALL_ALARM_TAG_EXTRA, mDataStallAlarmTag);
            mDataStallAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayInMs, mDataStallAlarmIntent);
        } else {
            if (VDBG_STALL) {
                log("startDataStallAlarm: NOT started, no connection tag=" + mDataStallAlarmTag);
            }
        }
    }

    private void stopDataStallAlarm() {
        if (VDBG_STALL) {
            log("stopDataStallAlarm: current tag=" + mDataStallAlarmTag +
                    " mDataStallAlarmIntent=" + mDataStallAlarmIntent);
        }
        mDataStallAlarmTag += 1;
        if (mDataStallAlarmIntent != null) {
            mAlarmManager.cancel(mDataStallAlarmIntent);
            mDataStallAlarmIntent = null;
        }
    }

    private void restartDataStallAlarm() {
        if (isConnected() == false) return;
        // To be called on screen status change.
        // Do not cancel the alarm if it is set with aggressive timeout.
        int nextAction = getRecoveryAction();

        if (RecoveryAction.isAggressiveRecovery(nextAction)) {
            if (DBG) log("restartDataStallAlarm: action is pending. not resetting the alarm.");
            return;
        }
        if (VDBG_STALL) log("restartDataStallAlarm: stop then start.");
        stopDataStallAlarm();
        startDataStallAlarm(DATA_STALL_NOT_SUSPECTED);
    }

    /**
     * Provisioning APN
     */
    private void onActionIntentProvisioningApnAlarm(Intent intent) {
        if (DBG) log("onActionIntentProvisioningApnAlarm: action=" + intent.getAction());
        Message msg = obtainMessage(DctConstants.EVENT_PROVISIONING_APN_ALARM,
                intent.getAction());
        msg.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private void startProvisioningApnAlarm() {
        int delayInMs = Settings.Global.getInt(mResolver,
                                Settings.Global.PROVISIONING_APN_ALARM_DELAY_IN_MS,
                                PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT);
        if (Build.IS_DEBUGGABLE) {
            // Allow debug code to use a system property to provide another value
            String delayInMsStrg = Integer.toString(delayInMs);
            delayInMsStrg = System.getProperty(DEBUG_PROV_APN_ALARM, delayInMsStrg);
            try {
                delayInMs = Integer.parseInt(delayInMsStrg);
            } catch (NumberFormatException e) {
                loge("startProvisioningApnAlarm: e=" + e);
            }
        }
        mProvisioningApnAlarmTag += 1;
        if (DBG) {
            log("startProvisioningApnAlarm: tag=" + mProvisioningApnAlarmTag +
                    " delay=" + (delayInMs / 1000) + "s");
        }
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, mProvisioningApnAlarmTag);
        mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayInMs, mProvisioningApnAlarmIntent);
    }

    private void stopProvisioningApnAlarm() {
        if (DBG) {
            log("stopProvisioningApnAlarm: current tag=" + mProvisioningApnAlarmTag +
                    " mProvsioningApnAlarmIntent=" + mProvisioningApnAlarmIntent);
        }
        mProvisioningApnAlarmTag += 1;
        if (mProvisioningApnAlarmIntent != null) {
            mAlarmManager.cancel(mProvisioningApnAlarmIntent);
            mProvisioningApnAlarmIntent = null;
        }
    }

    public boolean isOnlyIMSorEIMSPdnConnected() {
        boolean bIsOnlyIMSorEIMSConnected = false;
        if (MTK_IMS_SUPPORT) {
            for (ApnContext apnContext : mApnContexts.values()) {
                String apnType = apnContext.getApnType();
                if (!apnContext.isDisconnected()) {
                    if (apnType.equals(PhoneConstants.APN_TYPE_IMS) == false &&
                        apnType.equals(PhoneConstants.APN_TYPE_EMERGENCY) == false) {
                        if (DBG) log("apnType: " + apnType + " is still conntected!!");
                    // At least one context (not ims or Emergency) was not disconnected return false
                        bIsOnlyIMSorEIMSConnected = false;
                        break;
                    } else { //IMS or/and Emergency is/are still connected
                        bIsOnlyIMSorEIMSConnected = true;
                    }
                }
            }
        }
        return bIsOnlyIMSorEIMSConnected;
    }

    /**
    * M: get the string of ims ApnSetting in the list.
    *
    * @param apnSettings
    * @return
    */
    private String getIMSApnSetting(ArrayList<ApnSetting> apnSettings) {
        if (apnSettings == null || apnSettings.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (ApnSetting t : apnSettings) {
            if (t.canHandleType("ims")) {
                sb.append(apnToStringIgnoreName(t));
            }
        }
        log("getIMSApnSetting, apnsToStringIgnoreName: sb = " + sb.toString());
        return sb.toString();
    }

    private boolean isIMSApnSettingChanged(ArrayList<ApnSetting> prevApnList,
                                        ArrayList<ApnSetting> currApnList) {
        boolean bImsApnChanged = false;
        String prevIMSApn = getIMSApnSetting(prevApnList);
        String currIMSApn = getIMSApnSetting(currApnList);

        if (!prevIMSApn.isEmpty()) {
            if (!TextUtils.equals(prevIMSApn, currIMSApn)) {
                bImsApnChanged = true;
            }
        }

        return bImsApnChanged;
    }

    /**
     * M: Similar as ApnSetting.toString except the carrier is not considerred
     * because some operator need to change the APN name when locale changed.
     *
     * @param apnSetting
     * @return
     */
    private String apnToStringIgnoreName(ApnSetting apnSetting) {
        if (apnSetting == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(apnSetting.id)
        .append(", ").append(apnSetting.numeric)
        .append(", ").append(apnSetting.apn)
        .append(", ").append(apnSetting.proxy)
        .append(", ").append(apnSetting.mmsc)
        .append(", ").append(apnSetting.mmsProxy)
        .append(", ").append(apnSetting.mmsPort)
        .append(", ").append(apnSetting.port)
        .append(", ").append(apnSetting.authType).append(", ");
        for (int i = 0; i < apnSetting.types.length; i++) {
            sb.append(apnSetting.types[i]);
            if (i < apnSetting.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(apnSetting.protocol);
        sb.append(", ").append(apnSetting.roamingProtocol);
        sb.append(", ").append(apnSetting.carrierEnabled);
        sb.append(", ").append(apnSetting.bearerBitmask);
        log("apnToStringIgnoreName: sb = " + sb.toString());
        return sb.toString();
    }

    // M: Is data allowed even if mobile data off
    private boolean isDataAllowedAsOff(String apnType) {
        boolean isDataAllowedAsOff = false;
        if (!BSP_PACKAGE && mGsmDctExt != null) {
            isDataAllowedAsOff = mGsmDctExt.isDataAllowedAsOff(apnType);
        }

        // M: Vsim
        if (TextUtils.equals(apnType, PhoneConstants.APN_TYPE_DEFAULT)
                && isVsimActive(mPhone.getPhoneId())) {
            if (DBG) {
                log("Vsim is enabled, set isDataAllowedAsOff true");
            }
            isDataAllowedAsOff = true;
        }

        return isDataAllowedAsOff;
    }

    // M: Notify mobile data change
    protected void notifyMobileDataChange(int enabled) {
        log("notifyMobileDataChange, enable = " + enabled);
        Intent intent = new Intent(DataSubSelector.ACTION_MOBILE_DATA_ENABLE);
        intent.putExtra(DataSubSelector.EXTRA_MOBILE_DATA_ENABLE_REASON, enabled);
        mPhone.getContext().sendBroadcast(intent);
        
        /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]BEGIN */
        if (com.android.internal.telephony.FeatureOption.BIRD_ZX_SHOUFUBAO_APPS) {
            if (mPhone.getSubId() < 0) {
                return;
            }
            Intent shbIntent = new Intent("com.android.MOBILE_DATA");
            shbIntent.putExtra("DATA_CHANGED", enabled == 1);
            
            boolean state01 = false;
            int [] subId = SubscriptionManager.getSubId(0);
            if (subId != null && subId.length > 0 && subId[0] >= 0) {
                state01 = TelephonyManager.getDefault().getDataEnabled(subId[0]);
            }
            boolean state02 = false;
            subId = SubscriptionManager.getSubId(1);
            if (subId != null && subId.length > 0 && subId[0] >= 0) {
                state02 = TelephonyManager.getDefault().getDataEnabled(subId[0]);
            }
            shbIntent.putExtra("STATE_LIST", "0," + state01 + " 1," + state02);
            if (mPhone != null) {
                mPhone.getContext().sendBroadcast(shbIntent);
            }
        }
        /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]END */
    }

    // M: Set mobile data property
    private void setUserDataProperty(boolean enabled) {
        int phoneId = mPhone.getPhoneId();
        String dataOnIccid = "0";

        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            log("invalid phone id, don't update");
            return;
        }

        if (enabled) {
            dataOnIccid = SystemProperties.get(PROPERTY_ICCID[phoneId], "0");
        }

        log("setUserDataProperty:" + dataOnIccid);
        TelephonyManager.getDefault().setTelephonyProperty(phoneId, PROPERTY_MOBILE_DATA_ENABLE,
                dataOnIccid);
    }

    // M: JPN IA Start
    private void handleSetResume() {
        if (!SubscriptionManager.isValidPhoneId(mPhone.getPhoneId())) return;
        mPhone.mCi.setResumeRegistration(mSuspendId, null);
    }

    private void handleRegistrationSuspend(AsyncResult ar) {
        if (ar.exception == null && ar.result != null) {
            if (DBG) log("handleRegistrationSuspend: createAllApnList and set initial attach APN");
            mSuspendId = ((int[]) ar.result)[0];
            log("handleRegistrationSuspend: suspending with Id=" + mSuspendId);
            synchronized (mNeedsResumeModemLock) {
                mNeedsResumeModem = true;
            }
            createAllApnList();
            setInitialAttachApn();
        } else {
            log("handleRegistrationSuspend: AsyncResult is wrong " + ar.exception);
        }
    }

    private void handlePlmnChange(AsyncResult ar) {
        if (ar.exception == null && ar.result != null) {
            String[] plmnString = (String[]) ar.result;

            for (int i = 0; i < plmnString.length; i++) {
                log("plmnString[" + i + "]=" + plmnString[i]);
            }
            mRegion = getRegion(plmnString[0]);

            IccRecords r = mIccRecords.get();
            String operator = (r != null) ? r.getOperatorNumeric() : "";
            if (!TextUtils.isEmpty(operator) &&
                    isNeedToResumeMd() == false &&
                    mPhone.getPhoneId() ==
                            SubscriptionManager.getPhoneId(
                            SubscriptionController.getInstance().getDefaultDataSubId())){
                if (DBG) log("handlePlmnChange: createAllApnList and set initial attach APN");
                createAllApnList();
                setInitialAttachApn();
            } else {
                log ("No need to update APN for Operator");
            }
        } else {
            log("AsyncResult is wrong " + ar.exception);
        }
    }

    private int getRegion(String plmn) {
        String currentMcc;
        if (plmn == null || plmn.equals("") || plmn.length() < 5) {
            log("[getRegion] Invalid PLMN");
            return REGION_UNKNOWN;
        }

        currentMcc = plmn.substring(0, 3);
        for (String mcc : MCC_TABLE_TEST) {
            if (currentMcc.equals(mcc)) {
                log("[getRegion] Test PLMN");
                return REGION_UNKNOWN;
            }
        }

        for (String mcc : MCC_TABLE_DOMESTIC) {
            if (currentMcc.equals(mcc)) {
                log("[getRegion] REGION_DOMESTIC");
                return REGION_DOMESTIC;
            } else {
                log("[getRegion] REGION_FOREIGN");
                return REGION_FOREIGN;
            }
        }
        log("[getRegion] REGION_UNKNOWN");
        return REGION_UNKNOWN;
    }

    public boolean getImsEnabled() {
        boolean isImsEnabled = Settings.Global.getInt(mResolver,
                Settings.Global.ENHANCED_4G_MODE_ENABLED, 0) != 0;
        if (DBG) {
            log("getImsEnabled: getInt isImsEnabled=" + isImsEnabled);
        }
        return isImsEnabled;
    }

    /* M: VDF syncApnTableToRds. Request for two cmds as follow:
     * (apn;apn_type(profile_id|profile_id);rat;protocol)
     * @param ArrayList<ApnSetting> apnlist
     * @return void
     */
    private void syncApnTableToRds(ArrayList<ApnSetting> apnlist) {
        log("syncApnTableToRds: E");
        ApnSetting apn;
        ArrayList<String> aryApn = null;
        StringBuilder sb = null;
        int numOfProfileId = 0;
        int rat = 1;

        if (apnlist != null && apnlist.size() > 0) {
            aryApn = new ArrayList<String>();
            for (int i = 0; i < apnlist.size(); i++) {
                apn = apnlist.get(i);
                if (TextUtils.isEmpty(apn.apn)) {
                    log("syncApnTableToRds: apn name is empty");
                    continue;
                }
                sb = new StringBuilder();
                sb.append(apn.apn);
                sb.append(";");
                numOfProfileId = 0;
                for (int j = 0; j < apn.types.length; j++) {
                    int profileId = getApnProfileID(apn.types[j]);
                    if (profileId != RILConstants.DATA_PROFILE_INVALID) {
                        if (numOfProfileId > 0) {
                            sb.append("|");
                        }
                        sb.append(profileId);
                        numOfProfileId++;
                    }
                }
                sb.append(";");
                rat = getApnRatByBearer(apn.bearerBitmask);
                log("apn.rat: " + rat);
                sb.append(rat);
                sb.append(";");
                sb.append(apn.protocol);
                log("syncApnTableToRds: apn: " + sb.toString());
                aryApn.add(sb.toString());
            }
            if (aryApn.size() > 0) {
                mPhone.mCi.syncApnTableToRds(aryApn.toArray(new String[aryApn.size()]), null);
            }
        }
        log("syncApnTableToRds: X");
    }
    // M: VDF MMS over ePDG @{
    private int getApnRatByBearer(int bearerBitMask) {
        int invertIWLANBitMask = 0;
        log("getApnRatByBearer: " + bearerBitMask);

        if (bearerBitMask == 0) {
            return PhoneConstants.APN_RAT_CELLULAR_ONLY;
        } else {
            if (ServiceState.bitmaskHasTech(bearerBitMask,
                    ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)) {
                invertIWLANBitMask = ~(1 << (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN - 1))
                        & 0xffffff;
                if (isWifiOnlyApn(bearerBitMask)) {
                    return PhoneConstants.APN_RAT_WIFI_ONLY;
                } else {
                    return PhoneConstants.APN_RAT_CELLULAR_WIFI;
                }
            } else {
                return PhoneConstants.APN_RAT_CELLULAR_ONLY;
            }
        }
    }

    private boolean isWifiOnlyApn(int bearerBitMask) {
        int invertIWLANBitMask = ~(1 << (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN - 1)) & 0xffffff;

        if (bearerBitMask == 0) {
            return false;
        }
        return ((bearerBitMask & invertIWLANBitMask) == 0);
    }
    /// @}

    public boolean checkIfDomesticInitialAttachApn(String currentMcc) {
        boolean isMccDomestic = false;

        for (String mcc : MCC_TABLE_DOMESTIC) {
            if (currentMcc.equals(mcc)) {
                isMccDomestic = true;
            }
        }
        if (isOp17IaSupport()&& isMccDomestic) {
            if (getImsEnabled()) {
                return mRegion == REGION_DOMESTIC;
            } else {
                return false;
            }
        }
        if (enableOpIA()) {
            return mRegion == REGION_DOMESTIC;
        }

        if (DBG) {
            log("checkIfDomesticInitialAttachApn: Not OP129 or MCC is not in domestic for OP129");
        }
        return true;
    }

    public boolean enableOpIA() {
        IccRecords r = mIccRecords.get();
        String operatorNumeric = (r != null) ? r.getOperatorNumeric() : "";
        if (TextUtils.isEmpty(operatorNumeric)) {
            return false;
        }
        String simOperator = operatorNumeric.substring(0, 3);
        log("enableOpIA: currentMcc = " + simOperator);

        for (String mcc : MCC_TABLE_DOMESTIC) {
            if (simOperator.equals(mcc)) {
                return true;
            }
        }
        return false;
    }

    private void onPcoStatus(AsyncResult ar) {
        if (ar.exception == null) {
            int[] aryPcoStatus = (int[]) ar.result;
            if (aryPcoStatus != null && aryPcoStatus.length == 6) {
                log("onPcoStatus: PCO_MCC = " + aryPcoStatus[0]
                        + ", PCO_MNC = " + aryPcoStatus[1]
                        + ", PCO_VAL = " + aryPcoStatus[2]
                        + ", PCO_TECH = " + aryPcoStatus[3]
                        + ", PCO_PDN_ID = " + aryPcoStatus[5]);

                DcAsyncChannel dcac = mDataConnectionAcHashMap.get(aryPcoStatus[5]);
                if (dcac != null) {
                    String[] aryApnType = dcac.getApnTypeSync();
                    if (aryApnType != null) {
                        for (String apnType: aryApnType) {
                            Intent intent = new Intent(TelephonyIntents.ACTION_PCO_STATUS);
                            intent.putExtra(PhoneConstants.DATA_APN_TYPE_KEY, apnType);
                            intent.putExtra(TelephonyIntents.EXTRA_PCO_TYPE, aryPcoStatus[2]);
                            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                        }
                    } else {
                        log("onPcoStatus: dcac.getApnTypeSync() return null");
                    }
                }
            } else {
                log("onPcoStatus: pco status is null");
            }
        } else {
            loge("onPcoStatus exception: " + ar.exception);
        }
    }

    /**
     * M: VzW feature, sync data switch status to modem.
     */
    private void syncDataSettingsToMd(boolean dataEnabled, boolean dataRoamingEnabled) {
        log("syncDataSettingsToMd " + dataEnabled + "," + dataRoamingEnabled);
        mPhone.mCi.syncDataSettingsToMd(dataEnabled, dataRoamingEnabled, null);
    }

    private boolean skipDataStallAlarm() {
        boolean skipStall = true;
        boolean isTestSim = false;
        int phoneId = mPhone.getPhoneId();
        DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();

        if (SubscriptionManager.isValidPhoneId(phoneId) &&
                dcHelper != null && dcHelper.isTestIccCard(phoneId)) {
            isTestSim = true;
        }

        if (isTestSim) {
            if (SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("0")) {
                skipStall = false;
            } else {
                // majority behavior
                skipStall = true;
            }
        } else {
            if (SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("1")) {
                skipStall = true;
            } else {
                // majority behavior
                skipStall = false;
            }
        }

        return skipStall;
    }

    private void notifyVoiceCallEventToDataConnection(boolean bInVoiceCall,
            boolean bSupportConcurrent) {
        if (DBG) {
            log("notifyVoiceCallEventToDataConnection: bInVoiceCall = " + bInVoiceCall
                    + ", bSupportConcurrent = " + bSupportConcurrent);
        }
        for (DcAsyncChannel dcac : mDataConnectionAcHashMap.values()) {
                dcac.notifyVoiceCallEvent(bInVoiceCall, bSupportConcurrent);
        }
    }

    private boolean isOp17IaSupport() {
        String value = TelephonyManager.getTelephonyProperty(
                mPhone.getPhoneId(), "gsm.ril.sim.op17", "0");
        return value.equals("1") ? true : false;
    }

    private boolean isOp129IaSupport() {
        return SystemProperties.get("gsm.ril.sim.op129").equals("1") ? true : false;
    }

    private boolean isNeedToResumeMd() {
        return SystemProperties.get("gsm.ril.data.op.suspendmd").equals("1") ? true : false;
    }

    private boolean isOp18Sim() {
        IccRecords r = mIccRecords.get();
        String operator = (r != null) ? r.getOperatorNumeric() : "";

        if (operator != null) {
            for (int i = 0; i < MCCMNC_OP18.length; i++) {
                if (operator.startsWith(MCCMNC_OP18[i])) {
                    if (DBG) {
                        log("isOp18Sim: cur MCCMNC : " + operator);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
