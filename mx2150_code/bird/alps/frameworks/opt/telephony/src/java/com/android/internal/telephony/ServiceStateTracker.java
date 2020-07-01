/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.internal.telephony;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthLte;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Pair;
import android.util.TimeUtils;
import android.view.Display;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.telephony.IccCardConstants;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;

import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;


import com.mediatek.internal.telephony.RadioManager;

/*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]BEGIN */
import com.android.internal.telephony.FeatureOption;
/*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]END */

/**
 * {@hide}
 */
public class ServiceStateTracker extends Handler {
    private static final String LOG_TAG = "SST";
    //private static final boolean DBG = true;
    private static final boolean VDBG = false;  // STOPSHIP if true

    private static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";

    private CommandsInterface mCi;
    private UiccController mUiccController = null;
    private UiccCardApplication mUiccApplcation = null;
    private IccRecords mIccRecords = null;
    private TelephonyEventLog mEventLog;

    private boolean mVoiceCapable;
    //[ALPS01803573] - for 4gds/3gds tablet project
    protected boolean mSmsCapable;

    public ServiceState mSS;
    private ServiceState mNewSS;

    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    private long mLastCellInfoListTime;
    private List<CellInfo> mLastCellInfoList = null;

    // M: Report CellInfo by rate was done by polling cell info from framework by rate
    protected int mCellInfoRate = Integer.MAX_VALUE;

    private SignalStrength mSignalStrength;

    // TODO - this should not be public, right now used externally GsmConnetion.
    public RestrictedState mRestrictedState;

    /* The otaspMode passed to PhoneStateListener#onOtaspChanged */
    static public final int OTASP_UNINITIALIZED = 0;
    static public final int OTASP_UNKNOWN = 1;
    static public final int OTASP_NEEDED = 2;
    static public final int OTASP_NOT_NEEDED = 3;
    /**
     * OtaUtil has conflict enum 4: OtaUtils.OTASP_FAILURE_SPC_RETRIES
     */
    static public final int OTASP_SIM_UNPROVISIONED = 5;

    /**
     * A unique identifier to track requests associated with a poll
     * and ignore stale responses.  The value is a count-down of
     * expected responses in this pollingContext.
     */
    private int[] mPollingContext;
    private boolean mDesiredPowerState;

    /**
     * By default, strength polling is enabled.  However, if we're
     * getting unsolicited signal strength updates from the radio, set
     * value to true and don't bother polling any more.
     */
    private boolean mDontPollSignalStrength = false;

    private RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    private RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    private RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    private RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mAttachedRegistrants = new RegistrantList();
    protected RegistrantList mDetachedRegistrants = new RegistrantList();
    private RegistrantList mDataRegStateOrRatChangedRegistrants = new RegistrantList();
    private RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();

    protected RegistrantList mSignalStrengthChangedRegistrants = new RegistrantList();

    /* Radio power off pending flag and tag counter */
    private boolean mPendingRadioPowerOffAfterDataOff = false;
    private int mPendingRadioPowerOffAfterDataOffTag = 0;

    /* PS restrict disabled notify pending flag */
    protected boolean mPendingPsRestrictDisabledNotify = false;


    //MTK-START Replace 20 with 10
    /** Signal strength poll rate. */
    private static final int POLL_PERIOD_MILLIS = 10 * 1000;
    //MTK-END Replace 20 with 10

    /** Waiting period before recheck gprs and voice registration. */
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60 * 1000;

    /** GSM events */
    protected static final int EVENT_RADIO_STATE_CHANGED               = 1;
    protected static final int EVENT_NETWORK_STATE_CHANGED             = 2;
    protected static final int EVENT_GET_SIGNAL_STRENGTH               = 3;
    protected static final int EVENT_POLL_STATE_REGISTRATION           = 4;
    protected static final int EVENT_POLL_STATE_GPRS                   = 5;
    protected static final int EVENT_POLL_STATE_OPERATOR               = 6;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH              = 10;
    protected static final int EVENT_NITZ_TIME                         = 11;
    protected static final int EVENT_SIGNAL_STRENGTH_UPDATE            = 12;
    protected static final int EVENT_RADIO_AVAILABLE                   = 13;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE = 14;
    protected static final int EVENT_GET_LOC_DONE                      = 15;
    protected static final int EVENT_SIM_RECORDS_LOADED                = 16;
    protected static final int EVENT_SIM_READY                         = 17;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED          = 18;
    protected static final int EVENT_GET_PREFERRED_NETWORK_TYPE        = 19;
    protected static final int EVENT_SET_PREFERRED_NETWORK_TYPE        = 20;
    protected static final int EVENT_RESET_PREFERRED_NETWORK_TYPE      = 21;
    protected static final int EVENT_CHECK_REPORT_GPRS                 = 22;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED          = 23;

    /* M: MTK added events begin*/
    protected static final int EVENT_DATA_CONNECTION_DETACHED = 100;
    protected static final int EVENT_INVALID_SIM_INFO = 101; //ALPS00248788
    protected static final int EVENT_PS_NETWORK_STATE_CHANGED = 102;
    protected static final int EVENT_IMEI_LOCK = 103; /* ALPS00296298 */
    protected static final int EVENT_DISABLE_EMMRRS_STATUS = 104;
    protected static final int EVENT_ENABLE_EMMRRS_STATUS = 105;
    protected static final int EVENT_ICC_REFRESH = 106;
    protected static final int EVENT_FEMTO_CELL_INFO = 107;
    protected static final int EVENT_GET_CELL_INFO_LIST_BY_RATE = 108;
    protected static final int EVENT_SET_IMS_ENABLED_DONE = 109;
    protected static final int EVENT_SET_IMS_DISABLE_DONE = 110;
    protected static final int EVENT_IMS_DISABLED_URC = 111;
    protected static final int EVENT_IMS_REGISTRATION_INFO = 112;
    protected static final int EVENT_PS_NETWORK_TYPE_CHANGED = 113;
    protected static final int EVENT_MODULATION_INFO = 117;
    protected static final int EVENT_NETWORK_EVENT = 118;
    /* MTK added events end*/

    ///M: for changed the mtklogger
    protected static final int EVENT_ETS_DEV_CHANGED_LOGGER = 205;
    /// @}

    /** CDMA events */
    protected static final int EVENT_RUIM_READY                        = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED               = 27;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION      = 34;
    protected static final int EVENT_NV_READY                          = 35;
    protected static final int EVENT_ERI_FILE_LOADED                   = 36;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE       = 37;
    protected static final int EVENT_SET_RADIO_POWER_OFF               = 38;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED  = 39;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED          = 40;

    protected static final int EVENT_RADIO_ON                          = 41;
    public    static final int EVENT_ICC_CHANGED                       = 42;
    protected static final int EVENT_GET_CELL_INFO_LIST                = 43;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST              = 44;
    protected static final int EVENT_CHANGE_IMS_STATE                  = 45;
    protected static final int EVENT_IMS_STATE_CHANGED                 = 46;
    protected static final int EVENT_IMS_STATE_DONE                    = 47;
    protected static final int EVENT_IMS_CAPABILITY_CHANGED            = 48;
    protected static final int EVENT_ALL_DATA_DISCONNECTED             = 49;
    protected static final int EVENT_PHONE_TYPE_SWITCHED               = 50;

    protected static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    protected static final String PROPERTY_AUTO_RAT_SWITCH = "persist.radio.autoratswitch";

    private static final boolean mEngLoad = SystemProperties.get("ro.build.type").equals("eng")?
            true : false;
    private static int mLogLv = SystemProperties.getInt("persist.ril.radio.nw.log", 0);
    private static boolean DBG = (mEngLoad ||(mLogLv>0));

    /**
     * List of ISO codes for countries that can have an offset of
     * GMT+0 when not in daylight savings time.  This ignores some
     * small places such as the Canary Islands (Spain) and
     * Danmarkshavn (Denmark).  The list must be sorted by code.
    */
    protected static final String[] GMT_COUNTRY_CODES = {
        "bf", // Burkina Faso
        "ci", // Cote d'Ivoire
        "eh", // Western Sahara
        "fo", // Faroe Islands, Denmark
        "gb", // United Kingdom of Great Britain and Northern Ireland
        "gh", // Ghana
        "gm", // Gambia
        "gn", // Guinea
        "gw", // Guinea Bissau
        "ie", // Ireland
        "lr", // Liberia
        "is", // Iceland
        "ma", // Morocco
        "ml", // Mali
        "mr", // Mauritania
        "pt", // Portugal
        "sl", // Sierra Leone
        "sn", // Senegal
        "st", // Sao Tome and Principe
        "tg", // Togo
    };

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj = new Object();
    }

    /** Reason for registration denial. */
    protected static final String REGISTRATION_DENIED_GEN  = "General";
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";

    private boolean mImsRegistrationOnOff = false;
    private boolean mAlarmSwitch = false;
    private PendingIntent mRadioOffIntent = null;
    private static final String ACTION_RADIO_OFF = "android.intent.action.ACTION_RADIO_OFF";
    private boolean mPowerOffDelayNeed = true;
    private boolean mDeviceShuttingDown = false;
    /** Keep track of SPN display rules, so we only broadcast intent if something changes. */
    private boolean mSpnUpdatePending = false;
    private String mCurSpn = null;
    private String mCurDataSpn = null;
    private String mCurPlmn = null;
    private boolean mCurShowPlmn = false;
    private boolean mCurShowSpn = false;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private boolean mImsRegistered = false;

    private SubscriptionManager mSubscriptionManager;
    private SubscriptionController mSubscriptionController;
    private final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener =
        new SstSubscriptionsChangedListener();

    private class SstSubscriptionsChangedListener extends OnSubscriptionsChangedListener {
        public final AtomicInteger mPreviousSubId =
                new AtomicInteger(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        /**
         * Callback invoked when there is any change to any SubscriptionInfo. Typically
         * this method would invoke {@link SubscriptionManager#getActiveSubscriptionInfoList}
         */
        @Override
        public void onSubscriptionsChanged() {
            // Set the network type, in case the radio does not restore it.
            int subId = mPhone.getSubId();
            if (DBG) log("SubscriptionListener.onSubscriptionInfoChanged start " + subId);
            if (mPreviousSubId.getAndSet(subId) != subId) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    Context context = mPhone.getContext();

                    mPhone.notifyPhoneStateChanged();
                    mPhone.notifyCallForwardingIndicator();

                    boolean restoreSelection = !context.getResources().getBoolean(
                            com.android.internal.R.bool.skip_restoring_network_selection);
                    mPhone.sendSubscriptionSettings(restoreSelection);

                    mPhone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                            ServiceState.rilRadioTechnologyToString(
                                    mSS.getRilDataRadioTechnology()));

                    if (mSpnUpdatePending) {
                        mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(), mCurShowPlmn,
                                mCurPlmn, mCurShowSpn, mCurSpn);
                        mSpnUpdatePending = false;
                    }

                    // Remove old network selection sharedPreferences since SP key names are now
                    // changed to include subId. This will be done only once when upgrading from an
                    // older build that did not include subId in the names.
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                            context);
                    String oldNetworkSelection = sp.getString(
                            Phone.NETWORK_SELECTION_KEY, "");
                    String oldNetworkSelectionName = sp.getString(
                            Phone.NETWORK_SELECTION_NAME_KEY, "");
                    String oldNetworkSelectionShort = sp.getString(
                            Phone.NETWORK_SELECTION_SHORT_KEY, "");
                    // M ADD: MTK solution, always auto mode after sim is insert if skip is true
                    // clear the user's setting to avoid the notification
                    boolean skipRestoringSelection = mPhone.getContext().getResources().getBoolean(
                            com.android.internal.R.bool.skip_restoring_network_selection);
                    if (skipRestoringSelection) {
                        sp.edit().
                            remove(Phone.NETWORK_SELECTION_KEY + subId).
                            remove(Phone.NETWORK_SELECTION_NAME_KEY + subId).
                            remove(Phone.NETWORK_SELECTION_SHORT_KEY + subId).commit();
                    } else if (!TextUtils.isEmpty(oldNetworkSelection) ||
                            !TextUtils.isEmpty(oldNetworkSelectionName) ||
                            !TextUtils.isEmpty(oldNetworkSelectionShort)) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(Phone.NETWORK_SELECTION_KEY + subId,
                                oldNetworkSelection);
                        editor.putString(Phone.NETWORK_SELECTION_NAME_KEY + subId,
                                oldNetworkSelectionName);
                        editor.putString(Phone.NETWORK_SELECTION_SHORT_KEY + subId,
                                oldNetworkSelectionShort);
                        editor.remove(Phone.NETWORK_SELECTION_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_NAME_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_SHORT_KEY);
                        editor.commit();
                    }

                    // Once sub id becomes valid, we need to update the service provider name
                    // displayed on the UI again. The old SPN update intents sent to
                    // MobileSignalController earlier were actually ignored due to invalid sub id.
                    updateSpnDisplay();
                }
                // update voicemail count and notify message waiting changed
                /*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]BEGIN */
                if (FeatureOption.BIRD_VOICE_MESSAGE_CHANGE) {
                
                } else {
                    mPhone.updateVoiceMail();
                }
                /*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]END */
            }
            // Common
            if (mSubscriptionController.isReady()) {
                int phoneId = mPhone.getPhoneId();
                log("phoneId= " + phoneId + " ,mSpnUpdatePending= " + mSpnUpdatePending);
                if (mSpnUpdatePending) {
                    mSubscriptionController.setPlmnSpn(phoneId, mCurShowPlmn,
                        mCurPlmn, mCurShowSpn, mCurSpn);
                    mSpnUpdatePending = false;
                }
            }
        }
    };

    //Common
    private GsmCdmaPhone mPhone;
    public CellLocation mCellLoc;
    private CellLocation mNewCellLoc;
    public static final int MS_PER_HOUR = 60 * 60 * 1000;
    /* Time stamp after 19 January 2038 is not supported under 32 bit */
    private static final int MAX_NITZ_YEAR = 2037;
    /**
     * Sometimes we get the NITZ time before we know what country we
     * are in. Keep the time zone information from the NITZ string so
     * we can fix the time zone once know the country.
     */
    private boolean mNeedFixZoneAfterNitz = false;
    private int mZoneOffset;
    private boolean mZoneDst;
    private long mZoneTime;
    private boolean mGotCountryCode = false;
    private String mSavedTimeZone;
    private long mSavedTime;
    private long mSavedAtTime;
    /** Wake lock used while setting time of day. */
    private PowerManager.WakeLock mWakeLock;
    public static final String WAKELOCK_TAG = "ServiceStateTracker";
    private ContentResolver mCr;
    private ContentObserver mAutoTimeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i(LOG_TAG, "Auto time state changed");
            revertToNitzTime();
        }
    };

    private ContentObserver mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i(LOG_TAG, "Auto time zone state changed");
            revertToNitzTimeZone();
        }
    };

    //GSM
    private int mPreferredNetworkType;
    private int mMaxDataCalls = 1;
    private int mNewMaxDataCalls = 1;
    private int mReasonDataDenied = -1;
    private int mNewReasonDataDenied = -1;
    /**
     * GSM roaming status solely based on TS 27.007 7.2 CREG. Only used by
     * handlePollStateResult to store CREG roaming result.
     */
    private boolean mGsmRoaming = false;
    /**
     * Data roaming status solely based on TS 27.007 10.1.19 CGREG. Only used by
     * handlePollStateResult to store CGREG roaming result.
     */
    private boolean mDataRoaming = false;
    /**
     * Mark when service state is in emergency call only mode
     */
    private boolean mEmergencyOnly = false;
    /** Boolean is true is setTimeFromNITZString was called */
    private boolean mNitzUpdatedTime = false;

    //[ALPS01825832]
    private static boolean[] sReceiveNitz
            = new boolean[TelephonyManager.getDefault().getPhoneCount()];

    /** Started the recheck process after finding gprs should registered but not. */
    private boolean mStartedGprsRegCheck;
    /** Already sent the event-log for no gprs register. */
    private boolean mReportedGprsNoReg;
    /**
     * The Notification object given to the NotificationManager.
     */
    private Notification.Builder mNotificationBuilder;
    private Notification mNotification;
    /** Notification type. */
    public static final int PS_ENABLED = 1001;            // Access Control blocks data service
    public static final int PS_DISABLED = 1002;           // Access Control enables data service
    public static final int CS_ENABLED = 1003;            // Access Control blocks all voice/sms service
    public static final int CS_DISABLED = 1004;           // Access Control enables all voice/sms service
    public static final int CS_NORMAL_ENABLED = 1005;     // Access Control blocks normal voice/sms service
    public static final int CS_EMERGENCY_ENABLED = 1006;  // Access Control blocks emergency call service
    /** Notification id. */
    public static final int PS_NOTIFICATION = 888;  // Id to update and cancel PS restricted
    public static final int CS_NOTIFICATION = 999;  // Id to update and cancel CS restricted

    /** mtk01616_120613 Notification id. */
    static final int REJECT_NOTIFICATION = 890;

    /** [ALPS01558804] Add notification id for using some spcial icc card*/
    static final int SPECIAL_CARD_TYPE_NOTIFICATION = 8903;

    private int gprsState = ServiceState.STATE_OUT_OF_SERVICE;
    private int newGPRSState = ServiceState.STATE_OUT_OF_SERVICE;

    private String mHhbName = null;
    private String mCsgId = null;
    private int mFemtocellDomain = 0;

    /* ALPS00236452: manufacturer maintained table for specific operator with multiple PLMN id */
    // ALFMS00040828 - add "46008"
    public static final String[][] customEhplmn = {{"46000", "46002", "46004", "46007", "46008"},
                                       {"45400", "45402", "45418"},
                                       {"46001", "46009"},
                                       {"45403", "45404"},
                                       {"45412", "45413"},
                                       {"45416", "45419"},
                                       {"45501", "45504"},
                                       {"45503", "45505"},
                                       {"45002", "45008"},
                                       {"52501", "52502"},
                                       {"43602", "43612"},
                                       {"52010", "52099"},
                                       //ALPS02663460[
                                       {"52005", "52018"},
                                       //ALPS02663460]
                                       {"24001", "24005"},
                                       {"26207", "26208", "26203", "26277"},
                                       {"23430", "23431", "23432", "23433", "23434"},
                                       {"72402", "72403", "72404"},
                                       {"72406", "72410", "72411", "72423"},
                                       {"72432", "72433", "72434"},
                                       {"31026", "31031", "310160", "310200", "310210", "310220",
                                        "310230", "310240", "310250", "310260", "310270", "310280",
                                        "311290", "310300", "310310", "310320", "311330", "310660",
                                        "310800"},
                                       {"310150", "310170", "310380", "310410"},
                                       {"31033", "310330"},
                                       //ALPS02446235[
                                       {"21401", "21402", "21403", "21404", "21405", "21406",
                                        "21407", "21408", "21409", "21410", "21411", "21412",
                                        "21413", "21414", "21415", "21416", "21417", "21418",
                                        "21419", "21420", "21421"},
                                       //ALPS02446235]
                                       /** ALPS02501839 treat 20815 as 20801's EPLMN @{ */
                                       {"20815", "20801"},
                                       /** @} */
                                       //[BIRD][52015\52003\52001为同一个运营商，取消漫游][chenguangxiang][20161011]
                                       {"52001", "52003", "52015"}
                                       };

    /** M:[ALPS02503235] add operator considered roaming configures @{ */
    private static final String[][] customOperatorConsiderRoamingMcc = {
        {"404"/*SIM MCC*/, "404", "405"},
        {"405", "404", "405"}
    };
    /** @} */

    public boolean dontUpdateNetworkStateFlag = false;

    //MTK-START [mtk03851][111124]MTK added
    protected static final int EVENT_SET_AUTO_SELECT_NETWORK_DONE = 50;
    /** Indicate the first radio state changed **/
    private boolean mFirstRadioChange = true;
    private int explict_update_spn = 0;

    private String mLastRegisteredPLMN = null;
    private String mLastPSRegisteredPLMN = null;
    private boolean mEverIVSR = false;  /* ALPS00324111: at least one chance to do IVSR  */

    //MTK-ADD: for for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS only
    //SIM card or CS domain network registeration temporary failure
    private boolean isCsInvalidCard = false;

    private IServiceStateExt mServiceStateExt;

    private String mLocatedPlmn = null;
    private int mPsRegState = ServiceState.STATE_OUT_OF_SERVICE;
    private int mPsRegStateRaw = ServiceState.RIL_REG_STATE_NOT_REG;

    /** [ALPS01558804] Add notification id for using some spcial icc card*/
    private String mSimType = "";
    //MTK-START : [ALPS01262709] update TimeZone by MCC/MNC
    /* manufacturer maintained table for specific timezone
         with multiple timezone of country in time_zones_by_country.xml */
    private String[][] mTimeZoneIdOfCapitalCity = {{"au", "Australia/Sydney"},
                                                   {"br", "America/Sao_Paulo"},
                                                   {"ca", "America/Toronto"},
                                                   {"cl", "America/Santiago"},
                                                   {"es", "Europe/Madrid"},
                                                   {"fm", "Pacific/Ponape"},
                                                   {"gl", "America/Godthab"},
                                                   {"id", "Asia/Jakarta"},
                                                   {"kz", "Asia/Almaty"},
                                                   {"mn", "Asia/Ulaanbaatar"},
                                                   {"mx", "America/Mexico_City"},
                                                   {"pf", "Pacific/Tahiti"},
                                                   {"pt", "Europe/Lisbon"},
                                                   {"ru", "Europe/Moscow"},
                                                   {"us", "America/New_York"},
                                                   {"ec", "America/Guayaquil"}
                                                  };
    //MTK-END [ALPS01262709]
    /* manufacturer maintained table for the case that
       MccTable.defaultTimeZoneForMcc() returns unexpected timezone */
    private String[][] mTimeZoneIdByMcc = {{"460", "Asia/Shanghai"},
                                           {"404", "Asia/Calcutta"},
                                           {"454", "Asia/Hong_Kong"}
                                          };

    private boolean mIsImeiLock = false;

    // IMS
    private int mImsRegInfo = 0;
    private int mImsExtInfo = 0;

    //[ALPS01132085] for NetworkType display abnormal
    //[ALPS01497861] when ipo reboot this value must be ture
    //private boolean mIsScreenOn = true;  //[ALPS01810775,ALPS01868743]removed
    private boolean mIsForceSendScreenOnForUpdateNwInfo = false;

    private static Timer mCellInfoTimer = null;

    protected boolean bHasDetachedDuringPolling = false;
    ///M: Fix the operator info not update issue.
    private  boolean mNeedNotify = false;

    // keep the rat info of the voice URC
    private boolean voiceUrcWith4G = false;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mPhone.isPhoneTypeGsm()) {
                /// M: [CDMA] status bar does not update after change language. @{
                if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                    refreshSpnDisplay();
                    return;
                } else if (intent.getAction().equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                    int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                            SubscriptionManager.INVALID_PHONE_INDEX);
                    log("[CDMA]phoneId:" + phoneId + ", mPhoneId:" + mPhone.getPhoneId());
                    if (phoneId == mPhone.getPhoneId()) {
                        String simStatus = intent.getStringExtra(
                                IccCardConstants.INTENT_KEY_ICC_STATE);
                        log("[CDMA]simStatus: " + simStatus);
                        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus)) {
                            mMdn = null;
                            log("[CDMA]Clear MDN!");
                        }
                    }
                    return;
                }
                /// @}

                loge("Ignoring intent " + intent + " received on CDMA phone");
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                // update emergency string whenever locale changed
                ///M: Support language update for spn display. @{
                // updateSpnDisplay();
                refreshSpnDisplay();
                /// @}
            } else if (intent.getAction().equals(ACTION_RADIO_OFF)) {
                mAlarmSwitch = false;
                DcTracker dcTracker = mPhone.mDcTracker;
                powerOffRadioSafely(dcTracker);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                //[ALPS02042805] pollState after RILJ noitfy URC when screen on
                //pollState();
                explict_update_spn = 1;
                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    try {
                        if (mServiceStateExt.needEMMRRS()) {
                            if (isCurrentPhoneDataConnectionOn()) {
                                getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    try {
                        if (mServiceStateExt.needEMMRRS()) {
                            if (isCurrentPhoneDataConnectionOn()) {
                                getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String simState = IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;

                int slotId = intent.getIntExtra(PhoneConstants.PHONE_KEY, -1);
                if (slotId == mPhone.getPhoneId()) {
                    simState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                    log("SIM state change, slotId: " + slotId + " simState[" + simState + "]");
                }

                //[ALPS01558804] MTK-START: send notification for using some spcial icc card
                if ((simState.equals(IccCardConstants.INTENT_VALUE_ICC_READY))
                        && (mSimType.equals(""))) {
                    mSimType = PhoneFactory.getPhone(mPhone.getPhoneId())
                        .getIccCard().getIccCardType();

                    log("SimType= " + mSimType);

                    if ((mSimType != null) && (!mSimType.equals(""))) {
                        if (mSimType.equals("SIM") || mSimType.equals("USIM")) {
                            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                                try {
                                    if (mServiceStateExt.needIccCardTypeNotification(mSimType)) {
                                        //[ALPS01600557] - start : need to check 3G Capability SIM
                                        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                                            int raf = mPhone.getRadioAccessFamily();
                                            log("check RAF=" + raf);
                                            if ((raf&RadioAccessFamily.RAF_LTE)
                                                    == RadioAccessFamily.RAF_LTE) {
                                                setSpecialCardTypeNotification(mSimType, 0, 0);
                                            }
                                        } else {
                                            setSpecialCardTypeNotification(mSimType, 0, 0);
                                        }
                                    }
                                } catch (RuntimeException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                /* [ALPS01602110] START */
                if (slotId == mPhone.getPhoneId()
                        && simState.equals(IccCardConstants.INTENT_VALUE_ICC_IMSI)) {
                    setDeviceRatMode(slotId);
                }
                /* [ALPS01602110] END */

                if (simState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT) ||
                        simState.equals(IccCardConstants.INTENT_VALUE_ICC_NOT_READY)) {
                    mSimType = "";
                    NotificationManager notificationManager = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(SPECIAL_CARD_TYPE_NOTIFICATION);

                    //[ALPS01825832] reset flag
                    setReceivedNitz(mPhone.getPhoneId(), false);
                    //[ALPS01839778] reset flag for user change SIM card
                    mLastRegisteredPLMN = null;
                    mLastPSRegisteredPLMN = null;

                    //[ALPS01509553]-start:reset flag when sim plug-out
                    if (simState.equals(IccCardConstants.INTENT_VALUE_ICC_ABSENT)) {
                        dontUpdateNetworkStateFlag = false;
                    }
                    //[ALPS01509553]-end
                }
                //[ALPS01558804] MTK-END: send notification for using some special icc card
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if (intent.getIntExtra(SubscriptionManager.INTENT_KEY_DETECT_STATUS,
                        SubscriptionManager.EXTRA_VALUE_NOCHANGE)
                        != SubscriptionManager.EXTRA_VALUE_NOCHANGE) {
                    updateSpnDisplayGsm(true);
                }
            }
        }
    };

    /// M: Simulate IMS Registration @{
    private boolean mImsRegistry = false;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            mImsRegistry = intent.getBooleanExtra("registry", false);
            Rlog.w(LOG_TAG, "Simulate IMS Registration: " + mImsRegistry);
            int[] result = new int[] {
                (mImsRegistry ? 1 : 0),
                15 };
            AsyncResult ar = new AsyncResult(null, result, null);
            sendMessage(obtainMessage(EVENT_IMS_REGISTRATION_INFO, ar));
        }
    };
    /// @}

    //MTK-START [ALPS00368272]
    private ContentObserver mDataConnectionSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            log("Data Connection Setting changed");
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (mServiceStateExt.needEMMRRS()) {
                        if (isCurrentPhoneDataConnectionOn()) {
                            getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
                        } else {
                            getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
                        }
                    }
                } catch (RuntimeException e) {
                        e.printStackTrace();
                }
            }
        }
    };
    //MTK-END [ALPS00368272]

    //[ALPS01577029]-START:To support auto switch rat mode to 2G only for 3M TDD csfb project when
    //we are not in china
    private ContentObserver mMsicFeatureConfigObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i("GsmServiceStateTracker", "Msic Feature Config has changed");
            pollState();
        }
    };
    //[ALPS01577029]-END


    //CDMA
    // Min values used to by getOtasp()
    public static final String UNACTIVATED_MIN2_VALUE = "000000";
    public static final String UNACTIVATED_MIN_VALUE = "1111110111";
    // Current Otasp value
    private int mCurrentOtaspMode = OTASP_UNINITIALIZED;
    /** if time between NITZ updates is less than mNitzUpdateSpacing the update may be ignored. */
    public static final int NITZ_UPDATE_SPACING_DEFAULT = 1000 * 60 * 10;
    private int mNitzUpdateSpacing = SystemProperties.getInt("ro.nitz_update_spacing",
            NITZ_UPDATE_SPACING_DEFAULT);
    /** If mNitzUpdateSpacing hasn't been exceeded but update is > mNitzUpdate do the update */
    public static final int NITZ_UPDATE_DIFF_DEFAULT = 2000;
    private int mNitzUpdateDiff = SystemProperties.getInt("ro.nitz_update_diff",
            NITZ_UPDATE_DIFF_DEFAULT);
    private int mRoamingIndicator;
    private boolean mIsInPrl;
    private int mDefaultRoamingIndicator;
    /**
     * Initially assume no data connection.
     */
    private int mRegistrationState = -1;
    private RegistrantList mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
    private String mMdn;
    private int mHomeSystemId[] = null;
    private int mHomeNetworkId[] = null;
    private String mMin;
    private String mPrlVersion;
    private boolean mIsMinInfoReady = false;
    private boolean mIsEriTextLoaded = false;
    private boolean mIsSubscriptionFromRuim = false;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public static final String INVALID_MCC = "000";
    public static final String DEFAULT_MNC = "00";
    private HbpcdUtils mHbpcdUtils = null;
    /* Used only for debugging purposes. */
    private String mRegistrationDeniedReason;
    private String mCurrentCarrier = null;

    /// M: [CDMA] @{
    // Mark when service state is in emergency call only mode.
    private boolean mNetworkExsit = true;
    // Add for cdma plus code feature.
    private IPlusCodeUtils mPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();
    /// @}

    public ServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        initOnce(phone, ci);
        updatePhoneType();
    }

    private void initOnce(GsmCdmaPhone phone, CommandsInterface ci) {
        mPhone = phone;
        mCi = ci;
        mVoiceCapable = mPhone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
        // Common
        //[ALPS01803573] - for 4gds/3gds tablet project
        mSmsCapable = mPhone.getContext().getResources().getBoolean(
            com.android.internal.R.bool.config_sms_capable);
        mUiccController = UiccController.getInstance();

        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mCi.setOnSignalStrengthUpdate(this, EVENT_SIGNAL_STRENGTH_UPDATE, null);
        mCi.registerForCellInfoList(this, EVENT_UNSOL_CELL_INFO_LIST, null);

        mSubscriptionController = SubscriptionController.getInstance();
        mSubscriptionManager = SubscriptionManager.from(phone.getContext());
        mSubscriptionManager
                .addOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);

        mCi.registerForImsNetworkStateChanged(this, EVENT_IMS_STATE_CHANGED, null);

        PowerManager powerManager =
                (PowerManager)phone.getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);

        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                mServiceStateExt = MPlugin.createInstance(
                        IServiceStateExt.class.getName(), phone.getContext());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        mCi.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);
        mCi.registerForVoiceNetworkStateChanged(this, EVENT_NETWORK_STATE_CHANGED, null);
        mCi.setOnNITZTime(this, EVENT_NITZ_TIME, null);

        mCr = phone.getContext().getContentResolver();
        // system setting property AIRPLANE_MODE_ON is set in Settings.
        int airplaneMode = Settings.Global.getInt(mCr, Settings.Global.AIRPLANE_MODE_ON, 0);
        int enableCellularOnBoot = Settings.Global.getInt(mCr,
                Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1);
        mDesiredPowerState = (enableCellularOnBoot > 0) && ! (airplaneMode > 0);

        mCr.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME), true,
                mAutoTimeObserver);
        mCr.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME_ZONE), true,
                mAutoTimeZoneObserver);
        setSignalStrengthDefaultValues();

        // Monitor locale change
        Context context = mPhone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        context.registerReceiver(mIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(ACTION_RADIO_OFF);

        // M : MTK added
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        filter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        // M: MTK added end
        context.registerReceiver(mIntentReceiver, filter);

        mEventLog = new TelephonyEventLog(mPhone.getPhoneId());
        mPhone.notifyOtaspChanged(OTASP_UNINITIALIZED);

        /// M: Simulate IMS Registration @{
        final IntentFilter imsfilter = new IntentFilter();
        imsfilter.addAction("ACTION_IMS_SIMULATE");
        context.registerReceiver(mBroadcastReceiver, imsfilter);
        /// @}
    }

    @VisibleForTesting
    public void updatePhoneType() {
        mSS = new ServiceState();
        mNewSS = new ServiceState();
        mLastCellInfoListTime = 0;
        mLastCellInfoList = null;
        mSignalStrength = new SignalStrength();
        mRestrictedState = new RestrictedState();
        mStartedGprsRegCheck = false;
        mReportedGprsNoReg = false;
        mMdn = null;
        mMin = null;
        mPrlVersion = null;
        mIsMinInfoReady = false;
        mNitzUpdatedTime = false;

        //cancel any pending pollstate request on voice tech switching
        cancelPollState();

        if (mPhone.isPhoneTypeGsm()) {
            //clear CDMA registrations first
            if (mCdmaSSM != null) {
                mCdmaSSM.dispose(this);
            }

            mCi.unregisterForCdmaPrlChanged(this);
            mPhone.unregisterForEriFileLoaded(this);
            mCi.unregisterForCdmaOtaProvision(this);
            mPhone.unregisterForSimRecordsLoaded(this);

            mCellLoc = new GsmCellLocation();
            mNewCellLoc = new GsmCellLocation();
            mCi.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
            mCi.setOnRestrictedStateChanged(this, EVENT_RESTRICTED_STATE_CHANGED, null);


            // M: MTK added
            mCi.registerForPsNetworkStateChanged(this, EVENT_PS_NETWORK_STATE_CHANGED, null);
            mCi.setInvalidSimInfo(this, EVENT_INVALID_SIM_INFO, null); //ALPS00248788
            mCi.registerForModulation(this, EVENT_MODULATION_INFO, null);

            try {
               if (mServiceStateExt.isImeiLocked())
                   mCi.registerForIMEILock(this, EVENT_IMEI_LOCK, null);
            } catch (RuntimeException e) {
               /* BSP must exception here but Turnkey should not exception here */
               loge("No isImeiLocked");
            }


            mCi.registerForIccRefresh(this, EVENT_ICC_REFRESH, null);
            if (SystemProperties.get("persist.mtk_ims_support").equals("1")) {
                mCi.registerForImsDisable(this, EVENT_IMS_DISABLED_URC, null);
                mCi.registerForImsRegistrationInfo(this, EVENT_IMS_REGISTRATION_INFO, null);
            }


            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                mCi.registerForFemtoCellInfo(this, EVENT_FEMTO_CELL_INFO, null);
            }

            mCi.registerForNetworkEvent(this, EVENT_NETWORK_EVENT, null);
            //M: MTK added end

            //[ALPS01577029]-START:
            //To support auto switch rat mode to 2G only for 3M TDD csfb project
            //when we are not in china
            mCr.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.TELEPHONY_MISC_FEATURE_CONFIG), true,
                    mMsicFeatureConfigObserver);
            //[ALPS01577029]-END

            //MTK-START [ALPS00368272]
            mCr.registerContentObserver(
                    Settings.System.getUriFor(Settings.Global.MULTI_SIM_DATA_CALL_SUBSCRIPTION),
                    true,
                    mDataConnectionSettingObserver);
            mCr.registerContentObserver(
                    Settings.System.getUriFor(Settings.Global.MOBILE_DATA), true,
                    mDataConnectionSettingObserver);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (mServiceStateExt.needEMMRRS()) {
                        if (isCurrentPhoneDataConnectionOn()) {
                            getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
                        } else {
                            getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            //MTK-END [ALPS00368272]

            //[ALPS01825832] reset flag
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                setReceivedNitz(i, false);
            }
        } else {
            //clear GSM regsitrations first
            mCi.unregisterForAvailable(this);
            mCi.unSetOnRestrictedStateChanged(this);
            //M:
            // Reset Restricted State
            mPsRestrictDisabledRegistrants.notifyRegistrants();

            mCi.unregisterForPsNetworkStateChanged(this);
            mCi.unSetInvalidSimInfo(this);
            mCi.unregisterForModulation(this);

            try {
                if (mServiceStateExt.isImeiLocked())
                    mCi.unregisterForIMEILock(this);
            } catch (RuntimeException e) {
                /* BSP must exception here but Turnkey should not exception here */
                loge("No isImeiLocked");
            }

            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                mCi.unregisterForFemtoCellInfo(this);
            }

            mCi.unregisterForIccRefresh(this);

            if (SystemProperties.get("persist.mtk_ims_support").equals("1")) {
                mCi.unregisterForImsDisable(this);
                mCi.unregisterForImsRegistrationInfo(this);
            }

            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                mCi.unregisterForFemtoCellInfo(this);
            }

            mCi.unregisterForNetworkEvent(this);
            mCr.unregisterContentObserver(mMsicFeatureConfigObserver);   //[ALPS01577029]
            mCr.unregisterContentObserver(mDataConnectionSettingObserver);
            //M;

            if (mPhone.isPhoneTypeCdmaLte()) {
                mPhone.registerForSimRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            }
            mCellLoc = new CdmaCellLocation();
            mNewCellLoc = new CdmaCellLocation();
            mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(mPhone.getContext(), mCi, this,
                    EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
            mIsSubscriptionFromRuim = (mCdmaSSM.getCdmaSubscriptionSource() ==
                    CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_RUIM);

            mCi.registerForCdmaPrlChanged(this, EVENT_CDMA_PRL_VERSION_CHANGED, null);
            mPhone.registerForEriFileLoaded(this, EVENT_ERI_FILE_LOADED, null);
            mCi.registerForCdmaOtaProvision(this, EVENT_OTA_PROVISION_STATUS_CHANGE, null);

            mHbpcdUtils = new HbpcdUtils(mPhone.getContext());
            // update OTASP state in case previously set by another service
            updateOtaspState();
        }

        // This should be done after the technology specific initializations above since it relies
        // on fields like mIsSubscriptionFromRuim (which is updated above)
        onUpdateIccAvailability();

        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                ServiceState.rilRadioTechnologyToString(ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN));
        // Query signal strength from the modem after service tracker is created (i.e. boot up,
        // switching between GSM and CDMA phone), because the unsolicited signal strength
        // information might come late or even never come. This will get the accurate signal
        // strength information displayed on the UI.
        mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
        sendMessage(obtainMessage(EVENT_PHONE_TYPE_SWITCHED));
    }

    @VisibleForTesting
    public void requestShutdown() {
        if (mDeviceShuttingDown == true) return;
        mDeviceShuttingDown = true;
        mDesiredPowerState = false;
        // setPowerStateToDesired();

        // We need to shut down modem in our solution
        int phoneId = getPhone().getPhoneId();
        RadioManager.getInstance().setModemPower(false, (1 << phoneId));
    }

    public void dispose() {
        mCi.unSetOnSignalStrengthUpdate(this);
        mUiccController.unregisterForIccChanged(this);
        mCi.unregisterForCellInfoList(this);
        mSubscriptionManager
            .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangedListener);
        mCi.unregisterForImsNetworkStateChanged(this);

        //M:
        if (mPhone.isPhoneTypeGsm()) {
            //clear GSM regsitrations
            mCi.unregisterForAvailable(this);
            mCi.unSetOnRestrictedStateChanged(this);

            mCi.unregisterForPsNetworkStateChanged(this);
            mCi.unSetInvalidSimInfo(this);
            mCi.unregisterForModulation(this);

            try {
                if (mServiceStateExt.isImeiLocked())
                    mCi.unregisterForIMEILock(this);
            } catch (RuntimeException e) {
                /* BSP must exception here but Turnkey should not exception here */
                loge("No isImeiLocked");
            }

            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                mCi.unregisterForFemtoCellInfo(this);
            }

            mCi.unregisterForIccRefresh(this);

            if (SystemProperties.get("persist.mtk_ims_support").equals("1")) {
                mCi.unregisterForImsDisable(this);
                mCi.unregisterForImsRegistrationInfo(this);
            }

            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                mCi.unregisterForFemtoCellInfo(this);
            }

            mCi.unregisterForNetworkEvent(this);
            mCr.unregisterContentObserver(mMsicFeatureConfigObserver);   //[ALPS01577029]
            mCr.unregisterContentObserver(mDataConnectionSettingObserver);
        }
        //M;

        /// M: [CDMA] Add for unregister the broadcast receiver. @{
        if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
            mPhone.getContext().unregisterReceiver(mIntentReceiver);
        }
        /// @}
    }

    public boolean getDesiredPowerState() {
        return mDesiredPowerState;
    }

    protected SignalStrength mLastSignalStrength = null;
    protected boolean notifySignalStrength() {
        boolean notified = false;
        if (!mSignalStrength.equals(mLastSignalStrength)) {
            try {
                if (DBG) {
                    log("notifySignalStrength: mSignalStrength.getLevel=" +
                            mSignalStrength.getLevel());
                }
                mPhone.notifySignalStrength();
                // MTK add Common START
                mLastSignalStrength = new SignalStrength(mSignalStrength);
                // END
                notified = true;
            } catch (NullPointerException ex) {
                loge("updateSignalStrength() Phone already destroyed: " + ex
                        + "SignalStrength not notified");
            }
        }
        return notified;
    }

    /**
     * Notify all mDataConnectionRatChangeRegistrants using an
     * AsyncResult in msg.obj where AsyncResult#result contains the
     * new RAT as an Integer Object.
     */
    protected void notifyDataRegStateRilRadioTechnologyChanged() {
        int rat = mSS.getRilDataRadioTechnology();
        int drs = mSS.getDataRegState();
        if (DBG) log("notifyDataRegStateRilRadioTechnologyChanged: drs=" + drs + " rat=" + rat);

        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE,
                ServiceState.rilRadioTechnologyToString(rat));
        mDataRegStateOrRatChangedRegistrants.notifyResult(new Pair<Integer, Integer>(drs, rat));
    }

    /**
     * Some operators have been known to report registration failure
     * data only devices, to fix that use DataRegState.
     */
    protected void useDataRegStateForDataOnlyDevices() {
        //[ALPS01803573] - for 4gds/3gds tablet project
        //if (mVoiceCapable == false) {
        if (mSmsCapable == false) {
            if (DBG) {
                log("useDataRegStateForDataOnlyDevice: VoiceRegState=" + mNewSS.getVoiceRegState()
                    + " DataRegState=" + mNewSS.getDataRegState());
            }
            // TODO: Consider not lying and instead have callers know the difference.
            mNewSS.setVoiceRegState(mNewSS.getDataRegState());

            // Common
            /* Integrate ALPS00286197 with MR2 data only device state update */
            mNewSS.setRegState(ServiceState.REGISTRATION_STATE_HOME_NETWORK);
        }
    }

    protected void updatePhoneObject() {
        if (mPhone.getContext().getResources().
                getBoolean(com.android.internal.R.bool.config_switch_phone_on_voice_reg_state_change)) {
            // If the phone is not registered on a network, no need to update.
            boolean isRegistered = mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE ||
                    mSS.getVoiceRegState() == ServiceState.STATE_EMERGENCY_ONLY;
            if (!isRegistered) {
                log("updatePhoneObject: Ignore update");
                return;
            }
            mPhone.updatePhoneObject(mSS.getRilVoiceRadioTechnology());
        }
    }

    /**
     * Registration point for combined roaming on of mobile voice
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVoiceRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceRoamingOnRegistrants.add(r);

        if (mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOn(Handler h) {
        mVoiceRoamingOnRegistrants.remove(h);
    }

    /**
     * Registration point for roaming off of mobile voice
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVoiceRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceRoamingOffRegistrants.add(r);

        if (!mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOff(Handler h) {
        mVoiceRoamingOffRegistrants.remove(h);
    }

    /**
     * Registration point for combined roaming on of mobile data
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataRoamingOnRegistrants.add(r);

        if (mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOn(Handler h) {
        mDataRoamingOnRegistrants.remove(h);
    }

    /**
     * Registration point for roaming off of mobile data
     * combined roaming is true when roaming is true and ONS differs SPN
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataRoamingOffRegistrants.add(r);

        if (!mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOff(Handler h) {
        mDataRoamingOffRegistrants.remove(h);
    }

    /**
     * Re-register network by toggling preferred network type.
     * This is a work-around to deregister and register network since there is
     * no ril api to set COPS=2 (deregister) only.
     *
     * @param onComplete is dispatched when this is complete.  it will be
     * an AsyncResult, and onComplete.obj.exception will be non-null
     * on failure.
     */
    public void reRegisterNetwork(Message onComplete) {
        mCi.getPreferredNetworkType(
                obtainMessage(EVENT_GET_PREFERRED_NETWORK_TYPE, onComplete));
    }

    public void
    setRadioPower(boolean power) {
        mDesiredPowerState = power;

        setPowerStateToDesired();
    }

    /**
     * These two flags manage the behavior of the cell lock -- the
     * lock should be held if either flag is true.  The intention is
     * to allow temporary acquisition of the lock to get a single
     * update.  Such a lock grab and release can thus be made to not
     * interfere with more permanent lock holds -- in other words, the
     * lock will only be released if both flags are false, and so
     * releases by temporary users will only affect the lock state if
     * there is no continuous user.
     */
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;

    public void enableSingleLocationUpdate() {
        if (mWantSingleLocationUpdate || mWantContinuousLocationUpdates) return;
        mWantSingleLocationUpdate = true;
        mCi.setLocationUpdates(true, obtainMessage(EVENT_LOCATION_UPDATES_ENABLED));
    }

    public void enableLocationUpdates() {
        if (mWantSingleLocationUpdate || mWantContinuousLocationUpdates) return;
        mWantContinuousLocationUpdates = true;
        mCi.setLocationUpdates(true, obtainMessage(EVENT_LOCATION_UPDATES_ENABLED));
    }

    protected void disableSingleLocationUpdate() {
        mWantSingleLocationUpdate = false;
        if (!mWantSingleLocationUpdate && !mWantContinuousLocationUpdates) {
            mCi.setLocationUpdates(false, null);
        }
    }

    public void disableLocationUpdates() {
        mWantContinuousLocationUpdates = false;
        if (!mWantSingleLocationUpdate && !mWantContinuousLocationUpdates) {
            mCi.setLocationUpdates(false, null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        int[] ints;
        Message message;
        switch (msg.what) {
            case EVENT_SET_RADIO_POWER_OFF:
                synchronized(this) {
                    if (mPendingRadioPowerOffAfterDataOff &&
                            (msg.arg1 == mPendingRadioPowerOffAfterDataOffTag)) {
                        if (DBG) log("EVENT_SET_RADIO_OFF, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOffTag += 1;
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_SET_RADIO_OFF is stale arg1=" + msg.arg1 +
                                "!= tag=" + mPendingRadioPowerOffAfterDataOffTag);
                    }
                }
                break;

            case EVENT_ICC_CHANGED:
                onUpdateIccAvailability();
                break;

            /* MR2 newly added event handling START */
            case EVENT_GET_CELL_INFO_LIST_BY_RATE:
            case EVENT_GET_CELL_INFO_LIST: {
                ar = (AsyncResult) msg.obj;
                CellInfoResult result = (CellInfoResult) ar.userObj;
                synchronized(result.lockObj) {
                    if (ar.exception != null) {
                        log("EVENT_GET_CELL_INFO_LIST: error ret null, e=" + ar.exception);
                        result.list = null;
                    } else {
                        result.list = (List<CellInfo>) ar.result;

                        if (DBG) {
                            log("EVENT_GET_CELL_INFO_LIST: size=" + result.list.size()
                                    + " list=" + result.list);
                        }
                    }
                    mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    mLastCellInfoList = result.list;
                    // MTK add Common START
                    if (msg.what == EVENT_GET_CELL_INFO_LIST_BY_RATE) {
                        log("EVENT_GET_CELL_INFO_LIST_BY_RATE notify result");
                        mPhone.notifyCellInfo(result.list);
                    } else {
                    // END
                        result.lockObj.notify();
                        log("EVENT_GET_CELL_INFO_LIST notify result");
                    }
                }
                break;
            }

            case EVENT_UNSOL_CELL_INFO_LIST: {
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + ar.exception);
                } else {
                    List<CellInfo> list = (List<CellInfo>) ar.result;
                    if (VDBG) {
                        log("EVENT_UNSOL_CELL_INFO_LIST: size=" + list.size() + " list=" + list);
                    }
                    mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    mLastCellInfoList = list;
                    mPhone.notifyCellInfo(list);
                }
                break;
            }

            case  EVENT_IMS_STATE_CHANGED: // received unsol
                mCi.getImsRegistrationState(this.obtainMessage(EVENT_IMS_STATE_DONE));
                break;

            case EVENT_IMS_STATE_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    int[] responseArray = (int[])ar.result;
                    mImsRegistered = (responseArray[0] == 1) ? true : false;
                }
                break;

            //GSM
            case EVENT_RADIO_AVAILABLE:
                log("handle EVENT_RADIO_AVAILABLE");
                //check if we boot up under airplane mode
                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    log("not BSP package, notify!");
                    RadioManager.getInstance().notifyRadioAvailable(mPhone.getPhoneId());
                }
                //this is unnecessary
                //setPowerStateToDesired();
                break;

            case EVENT_SIM_READY:
                // Reset the mPreviousSubId so we treat a SIM power bounce
                // as a first boot.  See b/19194287
                mOnSubscriptionsChangedListener.mPreviousSubId.set(-1);
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_SIM_READY GSM");
                    boolean skipRestoringSelection = mPhone.getContext().getResources().getBoolean(
                            com.android.internal.R.bool.skip_restoring_network_selection);
                    if (DBG) log("skipRestoringSelection=" + skipRestoringSelection);
                    if (!skipRestoringSelection) {
                        // restore the previous network selection.
                        mPhone.restoreSavedNetworkSelection(null);
                    }
                }
                pollState();
                // Signal strength polling stops when radio is off
                queueNextSignalStrengthPoll();
                break;

            case EVENT_RADIO_STATE_CHANGED:
            case EVENT_PHONE_TYPE_SWITCHED:
                log("handle EVENT_RADIO_STATE_CHANGED");
                if(!mPhone.isPhoneTypeGsm() &&
                        mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
                    handleCdmaSubscriptionSource(mCdmaSSM.getCdmaSubscriptionSource());

                    // Signal strength polling stops when radio is off.
                    queueNextSignalStrengthPoll();
                }
                if (RadioManager.isMSimModeSupport()) {
                    logd("MTK propiertary Power on flow, setRadioPower:  mDesiredPowerState="
                            + mDesiredPowerState + "  phoneId=" + mPhone.getPhoneId());
                    RadioManager.getInstance().setRadioPower(
                            mDesiredPowerState, mPhone.getPhoneId());
                }
                else {
                    // This will do nothing in the 'radio not available' case
                    // setPowerStateToDesired();
                    log("BSP package but use MTK Power on flow");
                    RadioManager.getInstance().setRadioPower(
                            mDesiredPowerState, mPhone.getPhoneId());
                }
                pollState();
                break;

            case EVENT_NETWORK_STATE_CHANGED:
                if (mPhone.isPhoneTypeGsm()) {
                    logd("handle EVENT_NETWORK_STATE_CHANGED GSM");
                    ar = (AsyncResult) msg.obj;
                    onNetworkStateChangeResult(ar);
                }

                modemTriggeredPollState();
                break;

            case EVENT_PS_NETWORK_STATE_CHANGED:
                if (mPhone.isPhoneTypeGsm()) {
                    logd("handle EVENT_PS_NETWORK_STATE_CHANGED");
                    ar = (AsyncResult) msg.obj;
                    onPsNetworkStateChangeResult(ar);
                    modemTriggeredPollState();
                }
                break;

            case EVENT_GET_SIGNAL_STRENGTH:
                log("handle EVENT_GET_SIGNAL_STRENGTH");
                // This callback is called when signal strength is polled
                // all by itself

                if (!(mCi.getRadioState().isOn())) {
                    // Polling will continue when radio turns back on
                    return;
                }
                ar = (AsyncResult) msg.obj;
                onSignalStrengthResult(ar);
                queueNextSignalStrengthPoll();

                break;

            case EVENT_GET_LOC_DONE:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    String states[] = (String[])ar.result;
                    if (mPhone.isPhoneTypeGsm()) {
                        int lac = -1;
                        int cid = -1;
                        if (states.length >= 3) {
                            try {
                                if (states[1] != null && states[1].length() > 0) {
                                    lac = Integer.parseInt(states[1], 16);
                                }
                                if (states[2] != null && states[2].length() > 0) {
                                    cid = Integer.parseInt(states[2], 16);
                                }
                            } catch (NumberFormatException ex) {
                                Rlog.w(LOG_TAG, "error parsing location: " + ex);
                            }
                        }
                        ((GsmCellLocation)mCellLoc).setLacAndCid(lac, cid);
                    } else {
                        int baseStationId = -1;
                        int baseStationLatitude = CdmaCellLocation.INVALID_LAT_LONG;
                        int baseStationLongitude = CdmaCellLocation.INVALID_LAT_LONG;
                        int systemId = -1;
                        int networkId = -1;

                        if (states.length > 9) {
                            try {
                                if (states[4] != null) {
                                    baseStationId = Integer.parseInt(states[4]);
                                }
                                if (states[5] != null) {
                                    baseStationLatitude = Integer.parseInt(states[5]);
                                }
                                if (states[6] != null) {
                                    baseStationLongitude = Integer.parseInt(states[6]);
                                }
                                // Some carriers only return lat-lngs of 0,0
                                if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                                    baseStationLatitude  = CdmaCellLocation.INVALID_LAT_LONG;
                                    baseStationLongitude = CdmaCellLocation.INVALID_LAT_LONG;
                                }
                                if (states[8] != null) {
                                    systemId = Integer.parseInt(states[8]);
                                }
                                if (states[9] != null) {
                                    networkId = Integer.parseInt(states[9]);
                                }
                            } catch (NumberFormatException ex) {
                                loge("error parsing cell location data: " + ex);
                            }
                        }

                        ((CdmaCellLocation)mCellLoc).setCellLocationData(baseStationId,
                                baseStationLatitude, baseStationLongitude, systemId, networkId);
                    }
                    mPhone.notifyLocationChanged();
                }

                // Release any temporary cell lock, which could have been
                // acquired to allow a single-shot location update.
                disableSingleLocationUpdate();
                break;

            case EVENT_POLL_STATE_REGISTRATION:
            case EVENT_POLL_STATE_GPRS:
            case EVENT_POLL_STATE_OPERATOR:
                ar = (AsyncResult) msg.obj;
                handlePollStateResult(msg.what, ar);
                break;

            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE:
                if (DBG) log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                ar = (AsyncResult) msg.obj;
                if (mPhone.isPhoneTypeGsm()) {
                    handlePollStateResult(msg.what, ar);
                } else {
                    if (ar.exception == null && ar.result != null) {
                        ints = (int[])ar.result;
                        if (ints[0] == 1) {  // Manual selection.
                            mPhone.setNetworkSelectionModeAutomatic(null);
                        }
                    } else {
                        log("Unable to getNetworkSelectionMode");
                    }
                }
                break;

            case EVENT_POLL_SIGNAL_STRENGTH:
                // Just poll signal strength...not part of pollState()
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_POLL_SIGNAL_STRENGTH GSM " + mDontPollSignalStrength);
                    if (mDontPollSignalStrength) {
                        // The radio is telling us about signal strength changes
                        // we don't have to ask it
                        return;
                    }
                }
                mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
                break;

            case EVENT_NITZ_TIME:
                log("handle EVENT_NITZ_TIME");
                ar = (AsyncResult) msg.obj;

                String nitzString = (String)((Object[])ar.result)[0];
                long nitzReceiveTime = ((Long)((Object[])ar.result)[1]).longValue();

                setTimeFromNITZString(nitzString, nitzReceiveTime);
                break;

            case EVENT_SIGNAL_STRENGTH_UPDATE:
                log("handle EVENT_SIGNAL_STRENGTH_UPDATE");
                // This is a notification from CommandsInterface.setOnSignalStrengthUpdate

                ar = (AsyncResult) msg.obj;

                // The radio is telling us about signal strength changes
                // we don't have to ask it
                mDontPollSignalStrength = true;
                if (mPhone.isPhoneTypeGsm()) {
                    if ((ar.exception == null) && (ar.result != null)) {
                        mSignalStrengthChangedRegistrants.notifyResult(
                        new SignalStrength((SignalStrength) ar.result));
                    }
                }

                onSignalStrengthResult(ar);
                break;

            case EVENT_SIM_RECORDS_LOADED:
                log("EVENT_SIM_RECORDS_LOADED: what=" + msg.what);
                updatePhoneObject();
                updateOtaspState();
                if (mPhone.isPhoneTypeGsm()) {
                    /* updateSpnDisplay() will be executed in refreshSpnDisplay() */
                    ////updateSpnDisplay();
                    // pollState() result may be faster than load EF complete, so
                    // update ss.alphaLongShortName
                    refreshSpnDisplay();
                }
                break;

            case EVENT_LOCATION_UPDATES_ENABLED:
                log("handle EVENT_LOCATION_UPDATES_ENABLED");

                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    mCi.getVoiceRegistrationState(obtainMessage(EVENT_GET_LOC_DONE, null));
                }
                break;

            case EVENT_SET_PREFERRED_NETWORK_TYPE:
                log("handle EVENT_SET_PREFERRED_NETWORK_TYPE");

                ar = (AsyncResult) msg.obj;
                // Don't care the result, only use for dereg network (COPS=2)
                message = obtainMessage(EVENT_RESET_PREFERRED_NETWORK_TYPE, ar.userObj);
                mCi.setPreferredNetworkType(mPreferredNetworkType, message);
                break;

            case EVENT_RESET_PREFERRED_NETWORK_TYPE:
                log("handle EVENT_RESET_PREFERRED_NETWORK_TYPE");

                ar = (AsyncResult) msg.obj;
                if (ar.userObj != null) {
                    AsyncResult.forMessage(((Message) ar.userObj)).exception
                            = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
                break;

            case EVENT_GET_PREFERRED_NETWORK_TYPE:
                log("handle EVENT_GET_PREFERRED_NETWORK_TYPE");

                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    mPreferredNetworkType = ((int[])ar.result)[0];
                } else {
                    mPreferredNetworkType = RILConstants.NETWORK_MODE_GLOBAL;
                }

                message = obtainMessage(EVENT_SET_PREFERRED_NETWORK_TYPE, ar.userObj);
                int toggledNetworkType = RILConstants.NETWORK_MODE_GLOBAL;

                mCi.setPreferredNetworkType(toggledNetworkType, message);
                break;

            case EVENT_CHECK_REPORT_GPRS:
                log("handle EVENT_CHECK_REPORT_GPRS");

                if (mPhone.isPhoneTypeGsm() && mSS != null &&
                        !isGprsConsistent(mSS.getDataRegState(), mSS.getVoiceRegState())) {

                    // Can't register data service while voice service is ok
                    // i.e. CREG is ok while CGREG is not
                    // possible a network or baseband side error
                    GsmCellLocation loc = ((GsmCellLocation)mPhone.getCellLocation());
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL,
                            mSS.getOperatorNumeric(), loc != null ? loc.getCid() : -1);
                    mReportedGprsNoReg = true;
                }
                mStartedGprsRegCheck = false;
                break;

            case EVENT_RESTRICTED_STATE_CHANGED:
                log("handle EVENT_RESTRICTED_STATE_CHANGED");

                if (mPhone.isPhoneTypeGsm()) {
                    // This is a notification from
                    // CommandsInterface.setOnRestrictedStateChanged

                    if (DBG) log("EVENT_RESTRICTED_STATE_CHANGED");

                    ar = (AsyncResult) msg.obj;

                    onRestrictedStateChanged(ar);
                }
                break;

            case EVENT_ALL_DATA_DISCONNECTED:
                log("handle EVENT_ALL_DATA_DISCONNECTED");

                int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                ProxyController.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized(this) {
                    if (mPendingRadioPowerOffAfterDataOff) {
                        if (DBG) log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                }
                break;

            case EVENT_CHANGE_IMS_STATE:
                log("handle EVENT_CHANGE_IMS_STATE");

                if (DBG) log("EVENT_CHANGE_IMS_STATE:");

                setPowerStateToDesired();
                break;
            case EVENT_INVALID_SIM_INFO: //ALPS00248788
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_INVALID_SIM_INFO GSM");
                    ar = (AsyncResult) msg.obj;
                    onInvalidSimInfoReceived(ar);
                }
                break;
            case EVENT_MODULATION_INFO:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_MODULATION_INFO GSM");
                    ar = (AsyncResult) msg.obj;
                    onModulationInfoReceived(ar);
                }
                break;
            case EVENT_IMEI_LOCK: //ALPS00296298
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_IMEI_LOCK GSM");
                    mIsImeiLock = true;
                }
                break;
            case EVENT_ICC_REFRESH:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_ICC_REFRESH");
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        IccRefreshResponse res = ((IccRefreshResponse) ar.result);
                        if (res == null) {
                            log("IccRefreshResponse is null");
                            break;
                        }
                        switch (res.refreshResult) {
                            case IccRefreshResponse.REFRESH_INIT_FULL_FILE_UPDATED:
                            case 6: // NAA session Reset only applicable for a 3G platform
                                /* ALPS00949490 */
                                mLastRegisteredPLMN = null;
                                mLastPSRegisteredPLMN = null;
                                log("Reset mLastRegisteredPLMN/mLastPSRegisteredPLMN"
                                        + "for ICC refresh");
                                break;

                            case IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE:
                            case IccRefreshResponse.REFRESH_INIT_FILE_UPDATED:
                                if (res.efId == IccConstants.EF_IMSI) {
                                    mLastRegisteredPLMN = null;
                                    mLastPSRegisteredPLMN = null;
                                    log("Reset flag of IVSR for IMSI update");
                                    break;
                                }
                                break;
                            default:
                                log("GSST EVENT_ICC_REFRESH IccRefreshResponse =" + res);
                            break;
                        }
                    }
                }
                break;
            case EVENT_ENABLE_EMMRRS_STATUS:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_ENABLE_EMMRRS_STATUS GSM");
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        String data[] = (String []) ar.result;
                        log("EVENT_ENABLE_EMMRRS_STATUS, data[0] is : " + data[0]);
                        log("EVENT_ENABLE_EMMRRS_STATUS, einfo value is : " + data[0].substring(8));
                        int oldValue = Integer.valueOf(data[0].substring(8));
                        int value = oldValue | 0x80;
                        log("EVENT_ENABLE_EMMRRS_STATUS, einfo value change is : " + value);
                        if (oldValue != value) {
                            setEINFO(value, null);
                        }
                    }
                    log("EVENT_ENABLE_EMMRRS_STATUS GSM end");
                }
                break;
            case EVENT_DISABLE_EMMRRS_STATUS:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_DISABLE_EMMRRS_STATUS GSM");
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        String data[] = (String []) ar.result;
                        log("EVENT_DISABLE_EMMRRS_STATUS, data[0] is : " + data[0]);
                        log("EVENT_DISABLE_EMMRRS_STATUS, einfo value is : "
                                + data[0].substring(8));

                        try {
                            int oldValue = Integer.valueOf(data[0].substring(8));
                            int value = oldValue & 0xff7f;
                            log("EVENT_DISABLE_EMMRRS_STATUS, einfo value change is : " + value);
                            if (oldValue != value) {
                                setEINFO(value, null);
                            }
                        } catch (NumberFormatException ex) {
                            loge("Unexpected einfo value : " + ex);
                        }
                    }
                    log("EVENT_DISABLE_EMMRRS_STATUS GSM end");
                }
                break;
            case EVENT_FEMTO_CELL_INFO:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_FEMTO_CELL_INFO GSM");
                    ar = (AsyncResult) msg.obj;
                    onFemtoCellInfoResult(ar);
                }
                break;
            case EVENT_IMS_REGISTRATION_INFO:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_IMS_REGISTRATION_INFO GSM");
                    ar = (AsyncResult) msg.obj;
                    /// M: Simulate IMS Registration @{
                    if (SystemProperties.getInt("persist.ims.simulate", 0) == 1) {
                        ((int[]) ar.result)[0] = (mImsRegistry ? 1 : 0);
                        log("Override EVENT_IMS_REGISTRATION_INFO: new mImsRegInfo=" +
                                ((int[]) ar.result)[0]);
                    }
                    /// @}
                    if (((int[]) ar.result)[1] > 0) {
                        mImsExtInfo = ((int[]) ar.result)[1];
                    }
                    log("ImsRegistrationInfoResult [" + mImsRegInfo + ", " + mImsExtInfo + "]");
                }
                break;

            case EVENT_IMS_CAPABILITY_CHANGED:
                if (DBG) log("EVENT_IMS_CAPABILITY_CHANGED");
                updateSpnDisplay();
                break;
            case EVENT_NETWORK_EVENT:
                if (mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_NETWORK_EVENT");
                    ar = (AsyncResult) msg.obj;
                    onNetworkEventReceived(ar);
                }
                break;


            //CDMA
            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                handleCdmaSubscriptionSource(mCdmaSSM.getCdmaSubscriptionSource());
                break;

            case EVENT_RUIM_READY:
                /// M: [CDMA] @{
                if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
                    mIsSubscriptionFromRuim = true;
                }
                /// @}

                if (mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    // Subscription will be read from SIM I/O
                    if (DBG) log("Receive EVENT_RUIM_READY");
                    pollState();
                } else {
                    if (DBG) log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }

                // Only support automatic selection mode in CDMA.
                mCi.getNetworkSelectionMode(obtainMessage(EVENT_POLL_STATE_NETWORK_SELECTION_MODE));

                break;

            case EVENT_NV_READY:
                updatePhoneObject();

                // Only support automatic selection mode in CDMA.
                mCi.getNetworkSelectionMode(obtainMessage(EVENT_POLL_STATE_NETWORK_SELECTION_MODE));

                // For Non-RUIM phones, the subscription information is stored in
                // Non Volatile. Here when Non-Volatile is ready, we can poll the CDMA
                // subscription info.
                getSubscriptionInfoAndStartPollingThreads();
                break;

            case EVENT_POLL_STATE_CDMA_SUBSCRIPTION: // Handle RIL_CDMA_SUBSCRIPTION
                if (!mPhone.isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;

                    if (ar.exception == null) {
                        String cdmaSubscription[] = (String[]) ar.result;
                        if (cdmaSubscription != null && cdmaSubscription.length >= 5) {
                            mMdn = cdmaSubscription[0];
                            parseSidNid(cdmaSubscription[1], cdmaSubscription[2]);

                            mMin = cdmaSubscription[3];
                            mPrlVersion = cdmaSubscription[4];
                            if (DBG) log("GET_CDMA_SUBSCRIPTION: MDN=" + mMdn);

                            mIsMinInfoReady = true;

                            updateOtaspState();
                            // Notify apps subscription info is ready
                            notifyCdmaSubscriptionInfoReady();

                            if (!mIsSubscriptionFromRuim && mIccRecords != null) {
                                if (DBG) {
                                    log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                                }
                                mIccRecords.setImsi(getImsi());
                            } else {
                                if (DBG) {
                                    log("GET_CDMA_SUBSCRIPTION either mIccRecords is null or NV " +
                                            "type device - not setting Imsi in mIccRecords");
                                }
                            }
                        } else {
                            if (DBG) {
                                log("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription " +
                                        "params num=" + cdmaSubscription.length);
                            }
                        }
                    }
                }
                break;

            case EVENT_RUIM_RECORDS_LOADED:
                if (!mPhone.isPhoneTypeGsm()) {
                    log("EVENT_RUIM_RECORDS_LOADED: what=" + msg.what);
                    updatePhoneObject();
                    if (mPhone.isPhoneTypeCdma()) {
                        updateSpnDisplay();
                    } else {
                        RuimRecords ruim = (RuimRecords) mIccRecords;
                        if (ruim != null) {
                            if (ruim.isProvisioned()) {
                                mMdn = ruim.getMdn();
                                mMin = ruim.getMin();
                                parseSidNid(ruim.getSid(), ruim.getNid());
                                mPrlVersion = ruim.getPrlVersion();
                                mIsMinInfoReady = true;
                            }
                            updateOtaspState();

                            /// M: [CDMALTE] Happen in CDMA dual mode card, if change
                            /// from gsm to cdma, we should update spn gain. @{
                            updateSpnDisplay();
                            /// @}

                            // Notify apps subscription info is ready
                            notifyCdmaSubscriptionInfoReady();
                        }
                        // SID/NID/PRL is loaded. Poll service state
                        // again to update to the roaming state with
                        // the latest variables.
                        pollState();
                    }
                }
                break;

            case EVENT_ERI_FILE_LOADED:
                // Repoll the state once the ERI file has been loaded.
                if (DBG) log("ERI file has been loaded, repolling.");
                pollState();
                break;

            case EVENT_OTA_PROVISION_STATUS_CHANGE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    ints = (int[]) ar.result;
                    int otaStatus = ints[0];
                    if (otaStatus == Phone.CDMA_OTA_PROVISION_STATUS_COMMITTED
                            || otaStatus == Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED) {
                        if (DBG) log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                        mCi.getCDMASubscription( obtainMessage(EVENT_POLL_STATE_CDMA_SUBSCRIPTION));
                    }
                }
                break;

            case EVENT_CDMA_PRL_VERSION_CHANGED:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    ints = (int[]) ar.result;
                    mPrlVersion = Integer.toString(ints[0]);
                }
                break;

            default:
                log("Unhandled message with number: " + msg.what);
                break;
        }
    }

    protected int calculateDeviceRatMode(int phoneId) {
        int networkType = -1;
        if (mPhone.isPhoneTypeGsm()) {
            int restrictedNwMode = -1 ;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (mServiceStateExt.isSupportRatBalancing()) {
                        logd("networkType is controlled by RAT Blancing,"
                            + " no need to set network type");
                        return -1;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
               try {
                   restrictedNwMode = mServiceStateExt.needAutoSwitchRatMode(phoneId, mLocatedPlmn);
               } catch (RuntimeException e) {
                   e.printStackTrace();
               }
            }
            networkType = getPreferredNetworkModeSettings(phoneId);
            logd("restrictedNwMode = " + restrictedNwMode);
            if (restrictedNwMode >= Phone.NT_MODE_WCDMA_PREF) {
               if (restrictedNwMode != networkType) {
                   logd("Revise networkType to " + restrictedNwMode);
                   networkType = restrictedNwMode;
               }
            }

        } else {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    networkType = mServiceStateExt.getNetworkTypeForMota(phoneId);
                    log("[CDMA], networkType for mota is: " + networkType);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (networkType == -1) {
                networkType = getPreferredNetworkModeSettings(phoneId);
            }
        }
        logd("calculateDeviceRatMode = " + networkType);
        return networkType;
    }

    protected void setDeviceRatMode(int phoneId) {
        int networkType = -1;
        // log("[setDeviceRatMode]+");
        networkType = calculateDeviceRatMode(phoneId);
        log("[setDeviceRatMode]: " + networkType);
        if (networkType >= Phone.NT_MODE_WCDMA_PREF) {
            mPhone.setPreferredNetworkType(networkType, null);
        }

    }

    public boolean isPsRegStateRoamByUnsol() {
        return regCodeIsRoaming(mPsRegStateRaw);
    }


    protected boolean isSidsAllZeros() {
        if (mHomeSystemId != null) {
            for (int i=0; i < mHomeSystemId.length; i++) {
                if (mHomeSystemId[i] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check whether a specified system ID that matches one of the home system IDs.
     */
    private boolean isHomeSid(int sid) {
        if (mHomeSystemId != null) {
            for (int i=0; i < mHomeSystemId.length; i++) {
                if (sid == mHomeSystemId[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getMdnNumber() {
        return mMdn;
    }

    public String getCdmaMin() {
        return mMin;
    }

    /** Returns null if NV is not yet ready */
    public String getPrlVersion() {
        return mPrlVersion;
    }

    /**
     * Returns IMSI as MCC + MNC + MIN
     */
    public String getImsi() {
        // TODO: When RUIM is enabled, IMSI will come from RUIM not build-time props.
        String operatorNumeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(mPhone.getPhoneId());

        if (!TextUtils.isEmpty(operatorNumeric) && getCdmaMin() != null) {
            return (operatorNumeric + getCdmaMin());
        } else {
            return null;
        }
    }

    /**
     * Check if subscription data has been assigned to mMin
     *
     * return true if MIN info is ready; false otherwise.
     */
    public boolean isMinInfoReady() {
        return mIsMinInfoReady;
    }

    /**
     * Returns OTASP_UNKNOWN, OTASP_UNINITIALIZED, OTASP_NEEDED or OTASP_NOT_NEEDED
     */
    public int getOtasp() {
        int provisioningState;
        // if sim is not loaded, return otasp uninitialized
        if(!mPhone.getIccRecordsLoaded()) {
            if(DBG) log("getOtasp: otasp uninitialized due to sim not loaded");
            return OTASP_UNINITIALIZED;
        }
        // if voice tech is Gsm, return otasp not needed
        if(mPhone.isPhoneTypeGsm()) {
            if(DBG) log("getOtasp: otasp not needed for GSM");
            return OTASP_NOT_NEEDED;
        }
        // for ruim, min is null means require otasp.
        if (mIsSubscriptionFromRuim && mMin == null) {
            return OTASP_NEEDED;
        }
        if (mMin == null || (mMin.length() < 6)) {
            if (DBG) log("getOtasp: bad mMin='" + mMin + "'");
            provisioningState = OTASP_UNKNOWN;
        } else {
            if ((mMin.equals(UNACTIVATED_MIN_VALUE)
                    || mMin.substring(0,6).equals(UNACTIVATED_MIN2_VALUE))
                    || SystemProperties.getBoolean("test_cdma_setup", false)) {
                provisioningState = OTASP_NEEDED;
            } else {
                provisioningState = OTASP_NOT_NEEDED;
            }
        }
        if (DBG) log("getOtasp: state=" + provisioningState);
        return provisioningState;
    }

    protected void parseSidNid (String sidStr, String nidStr) {
        if (sidStr != null) {
            String[] sid = sidStr.split(",");
            mHomeSystemId = new int[sid.length];
            for (int i = 0; i < sid.length; i++) {
                try {
                    mHomeSystemId[i] = Integer.parseInt(sid[i]);
                } catch (NumberFormatException ex) {
                    loge("error parsing system id: " + ex);
                }
            }
        }
        if (DBG) log("CDMA_SUBSCRIPTION: SID=" + sidStr);

        if (nidStr != null) {
            String[] nid = nidStr.split(",");
            mHomeNetworkId = new int[nid.length];
            for (int i = 0; i < nid.length; i++) {
                try {
                    mHomeNetworkId[i] = Integer.parseInt(nid[i]);
                } catch (NumberFormatException ex) {
                    loge("CDMA_SUBSCRIPTION: error parsing network id: " + ex);
                }
            }
        }
        if (DBG) log("CDMA_SUBSCRIPTION: NID=" + nidStr);
    }

    protected void updateOtaspState() {
        int otaspMode = getOtasp();
        int oldOtaspMode = mCurrentOtaspMode;
        mCurrentOtaspMode = otaspMode;

        if (oldOtaspMode != mCurrentOtaspMode) {
            if (DBG) {
                log("updateOtaspState: call notifyOtaspChanged old otaspMode=" +
                        oldOtaspMode + " new otaspMode=" + mCurrentOtaspMode);
            }
            mPhone.notifyOtaspChanged(mCurrentOtaspMode);
        }
    }

    protected Phone getPhone() {
        return mPhone;
    }

    //MTK-ADD Start : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
    //only SIM card or CS domain network registeration temporary failure
    /* update  mNewCellLoc when CS is not registered but PS is registered */
    int psLac = -1;
    int psCid = -1;
    //MTK-ADD END: for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS only
    //SIM card or CS domain network registeration temporary failure


    protected void handlePollStateResult(int what, AsyncResult ar) {
        psLac = -1;
        psCid = -1;

        boolean ignore = false;
        // Ignore stale requests from last poll
        if (ar.userObj != mPollingContext) {
            // loge("handlePollStateResult return due to (ar.userObj != mPollingContext)");
            // return;
            // for log reduction... return later
            ignore = true;
        }

        if (what == EVENT_POLL_STATE_REGISTRATION) {
            logd("handle EVENT_POLL_STATE_REGISTRATION" +
                    (ignore ? " return due to (ar.userObj != mPollingContext)" : ""));
        } else if (what == EVENT_POLL_STATE_GPRS) {
            logd("handle EVENT_POLL_STATE_GPRS" +
                    (ignore ? " return due to (ar.userObj != mPollingContext)" : ""));
        } else if (what == EVENT_POLL_STATE_OPERATOR) {
            logd("handle EVENT_POLL_STATE_OPERATOR" +
                    (ignore ? " return due to (ar.userObj != mPollingContext)" : ""));
        }

        if (ignore) return;

        if (ar.exception != null) {
            CommandException.Error err=null;

            if (ar.exception instanceof CommandException) {
                err = ((CommandException)(ar.exception)).getCommandError();
            }

            if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                // Radio has crashed or turned off
                cancelPollState();
                loge("handlePollStateResult cancelPollState due to RADIO_NOT_AVAILABLE");
                // Clear status and invoke pollStateDone to notify other module
                if (mCi.getRadioState() != CommandsInterface.RadioState.RADIO_ON) {
                    mNewSS.setStateOff();
                    mNewCellLoc.setStateInvalid();
                    setSignalStrengthDefaultValues();
                    mGotCountryCode = false;
                    mNitzUpdatedTime = false;
                    setNullState();
                    mPsRegStateRaw = ServiceState.RIL_REG_STATE_NOT_REG;
                    pollStateDone();
                    loge("handlePollStateResult pollStateDone to notify RADIO_NOT_AVAILABLE");
                }
                return;
            }

            if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                loge("RIL implementation has returned an error where it must succeed" +
                        ar.exception);
            }
        } else try {
            handlePollStateResultMessage(what, ar);
        } catch (RuntimeException ex) {
            loge("Exception while polling service state. Probably malformed RIL response." + ex);
        }

        mPollingContext[0]--;

        if (mPollingContext[0] == 0) {

            if (mPhone.isPhoneTypeGsm()) {

                /**
                 * Notify pending PS restricted status here
                 */
                if (mPendingPsRestrictDisabledNotify) {
                    mPsRestrictDisabledRegistrants.notifyRegistrants();
                    setNotification(PS_DISABLED);
                    mPendingPsRestrictDisabledNotify = false;
                }

                /**
                 * [ALPS00006527]
                 * Only when CS in service, treat PS as in service
                 */
                if ((mNewSS.getState() != ServiceState.STATE_IN_SERVICE) &&
                    (mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
                        //when CS not registered, we update cellLoc by +CGREG
                        log("update cellLoc by +CGREG");
                        ((GsmCellLocation)mNewCellLoc).setLacAndCid(psLac, psCid);
                }
                updateRoamingState();
                mNewSS.setEmergencyOnly(mEmergencyOnly);
            } else {
                boolean namMatch = false;
                if (!isSidsAllZeros() && isHomeSid(mNewSS.getSystemId())) {
                    namMatch = true;
                }

                // Setting SS Roaming (general)
                if (mIsSubscriptionFromRuim) {
                    mNewSS.setVoiceRoaming(isRoamingBetweenOperators(mNewSS.getVoiceRoaming(), mNewSS));
                }
                // For CDMA, voice and data should have the same roaming status
                final boolean isVoiceInService =
                        (mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE);
                final int dataRegType = mNewSS.getRilDataRadioTechnology();
                if (isVoiceInService && ServiceState.isCdma(dataRegType)) {
                    mNewSS.setDataRoaming(mNewSS.getVoiceRoaming());
                }

                /// M: [CDMA] Add for show EccButton when sim out of service. @{
                mEmergencyOnly = false;
                if (mCi.getRadioState().isOn()) {
                    if ((mNewSS.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE)
                            && (mNewSS.getDataRegState() == ServiceState.STATE_OUT_OF_SERVICE)
                            && mNetworkExsit) {
                        log("[CDMA]handlePollStateResult: OUT_OF_SERVICE, mEmergencyOnly=true");
                        mEmergencyOnly = true;
                    }
                }
                log("[CDMA]handlePollStateResult: set mEmergencyOnly=" + mEmergencyOnly
                        + ", mNetworkExsit=" + mNetworkExsit);
                mNewSS.setEmergencyOnly(mEmergencyOnly);
                /// @}

                // Setting SS CdmaRoamingIndicator and CdmaDefaultRoamingIndicator
                mNewSS.setCdmaDefaultRoamingIndicator(mDefaultRoamingIndicator);
                mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                boolean isPrlLoaded = true;
                if (TextUtils.isEmpty(mPrlVersion)) {
                    isPrlLoaded = false;
                }
                if (!isPrlLoaded || (mNewSS.getRilVoiceRadioTechnology()
                        == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
                    log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                } else if (!isSidsAllZeros()) {
                    if (!namMatch && !mIsInPrl) {
                        // Use default
                        mNewSS.setCdmaRoamingIndicator(mDefaultRoamingIndicator);
                    } else if (namMatch && !mIsInPrl) {
                        // TODO this will be removed when we handle roaming on LTE on CDMA+LTE phones
                        if (mNewSS.getRilVoiceRadioTechnology()
                                == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                            log("Turn off roaming indicator as voice is LTE");
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_FLASH);
                        }
                    } else if (!namMatch && mIsInPrl) {
                        // Use the one from PRL/ERI
                        mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                    } else {
                        // It means namMatch && mIsInPrl
                        if ((mRoamingIndicator <= 2)) {
                            mNewSS.setCdmaRoamingIndicator(EriInfo.ROAMING_INDICATOR_OFF);
                        } else {
                            // Use the one from PRL/ERI
                            mNewSS.setCdmaRoamingIndicator(mRoamingIndicator);
                        }
                    }
                }

                int roamingIndicator = mNewSS.getCdmaRoamingIndicator();
                mNewSS.setCdmaEriIconIndex(mPhone.mEriManager.getCdmaEriIconIndex(roamingIndicator,
                        mDefaultRoamingIndicator));
                mNewSS.setCdmaEriIconMode(mPhone.mEriManager.getCdmaEriIconMode(roamingIndicator,
                        mDefaultRoamingIndicator));

                // NOTE: Some operator may require overriding mCdmaRoaming
                // (set by the modem), depending on the mRoamingIndicator.

                if (DBG) {
                    log("Set CDMA Roaming Indicator to: " + mNewSS.getCdmaRoamingIndicator()
                            + ". voiceRoaming = " + mNewSS.getVoiceRoaming()
                            + ". dataRoaming = " + mNewSS.getDataRoaming()
                            + ", isPrlLoaded = " + isPrlLoaded
                            + ". namMatch = " + namMatch + " , mIsInPrl = " + mIsInPrl
                            + ", mRoamingIndicator = " + mRoamingIndicator
                            + ", mDefaultRoamingIndicator= " + mDefaultRoamingIndicator);
                }
            }
            pollStateDone();
        }

    }

    /**
     * Set roaming state when cdmaRoaming is true and ons is different from spn
     * @param cdmaRoaming TS 27.007 7.2 CREG registered roaming
     * @param s ServiceState hold current ons
     * @return true for roaming state set
     */
    private boolean isRoamingBetweenOperators(boolean cdmaRoaming, ServiceState s) {
        return cdmaRoaming && !isSameOperatorNameFromSimAndSS(s);
    }

    void handlePollStateResultMessage(int what, AsyncResult ar) {
        int ints[];
        String states[];
        switch (what) {
            case EVENT_POLL_STATE_REGISTRATION: {
                if (mPhone.isPhoneTypeGsm()) {
                    states = (String[]) ar.result;
                    int lac = -1;
                    int cid = -1;
                    int type = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                    int regState = ServiceState.RIL_REG_STATE_UNKNOWN;
                    int reasonRegStateDenied = -1;
                    int psc = -1;
                    int rejCause = -1;
                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);
                            if (states.length >= 3) {
                                if (states[1] != null && states[1].length() > 0) {
                                    //[ALPS00907900]-START
                                    int tempLac = Integer.parseInt(states[1], 16);
                                    if (tempLac < 0) {
                                        log("set Lac to previous value");
                                        tempLac = ((GsmCellLocation)mCellLoc).getLac();
                                    }
                                    lac = tempLac;
                                    //[ALPS00907900]-END

                                }
                                if (states[2] != null && states[2].length() > 0) {
                                    //[ALPS00907900]-START
                                    int tempCid = Integer.parseInt(states[2], 16);
                                    if (tempCid < 0) {
                                        log("set Cid to previous value");
                                        tempCid = ((GsmCellLocation)mCellLoc).getCid();
                                    }
                                    cid = tempCid;
                                    //[ALPS00907900]-END
                                }

                                // states[3] (if present) is the current radio technology
                                if (states.length >= 4 &&
                                        states[3] != null &&
                                        states[3].length() > 0) {
                                    //[ALPS01810775,ALPS01868743] -Start: update network type at
                                     //screen off
                                     updateNetworkInfo(regState, Integer.parseInt(states[3]));
                                     //[ALPS01810775,ALPS01868743] -End
                                }

                                if (states.length >= 14 &&
                                        states[13] != null && states[13].length() > 0) {
                                    rejCause = Integer.parseInt(states[13]);
                                    mNewSS.setVoiceRejectCause(rejCause);
                                    logd("set voice reject cause to " + rejCause);
                                }
                            }
                            if (states.length > 14) {
                                if (states[14] != null && states[14].length() > 0) {
                                    psc = Integer.parseInt(states[14], 16);
                                }
                            }

                            log("EVENT_POLL_STATE_REGISTRATION mSS getRilVoiceRadioTechnology:"
                                    + mSS.getRilVoiceRadioTechnology() +
                                    ", regState:" + regState +
                                    ", NewSS RilVoiceRadioTechnology:"
                                    + mNewSS.getRilVoiceRadioTechnology() +
                                    ", lac:" + lac +
                                    ", cid:" + cid);
                        } catch (NumberFormatException ex) {
                            loge("error parsing RegistrationState: " + ex);
                        }
                    }

                    mGsmRoaming = regCodeIsRoaming(regState);
                    mNewSS.setVoiceRegState(regCodeToServiceState(regState));

                    boolean isVoiceCapable = mPhone.getContext().getResources()
                            .getBoolean(com.android.internal.R.bool.config_voice_capable);
                    if ((regState == ServiceState.RIL_REG_STATE_DENIED_EMERGENCY_CALL_ENABLED
                            || regState == ServiceState.RIL_REG_STATE_NOT_REG_EMERGENCY_CALL_ENABLED
                            || regState == ServiceState.RIL_REG_STATE_SEARCHING_EMERGENCY_CALL_ENABLED
                            || regState == ServiceState.RIL_REG_STATE_UNKNOWN_EMERGENCY_CALL_ENABLED)
                            && isVoiceCapable) {
                        mEmergencyOnly = true;
                    } else {
                        mEmergencyOnly = false;
                    }


                    log("regState = " + regState + ", isVoiceCapable = " + isVoiceCapable +
                            ", mEmergencyOnly = " + mEmergencyOnly);

                    // LAC and CID are -1 if not avail. LAC and CID will be updated in

                    // onNetworkStateChangeResult() when in OUT_SERVICE
                    if (states.length > 3) {
                        logd("states.length > 3");

                        /* ALPS00291583: ignore unknown lac or cid value */
                        if (lac == 0xfffe || cid == 0x0fffffff) {
                            log("unknown lac:" + lac + " or cid:" + cid);
                        } else {
                            /* AT+CREG? result won't include <lac> and <cid> when  in OUT_SERVICE */
                            if (regCodeToServiceState(regState)
                                    != ServiceState.STATE_OUT_OF_SERVICE) {
                                ((GsmCellLocation)mNewCellLoc).setLacAndCid(lac, cid);
                            }
                        }
                    }
                    ((GsmCellLocation)mNewCellLoc).setPsc(psc);
                } else {
                    states = (String[])ar.result;

                    int registrationState = 4;     //[0] registrationState
                    int radioTechnology = -1;      //[3] radioTechnology
                    int baseStationId = -1;        //[4] baseStationId
                    //[5] baseStationLatitude
                    int baseStationLatitude = CdmaCellLocation.INVALID_LAT_LONG;
                    //[6] baseStationLongitude
                    int baseStationLongitude = CdmaCellLocation.INVALID_LAT_LONG;
                    int cssIndicator = 0;          //[7] init with 0, because it is treated as a boolean
                    int systemId = 0;              //[8] systemId
                    int networkId = 0;             //[9] networkId
                    int roamingIndicator = -1;     //[10] Roaming indicator
                    int systemIsInPrl = 0;         //[11] Indicates if current system is in PRL
                    int defaultRoamingIndicator = 0;  //[12] Is default roaming indicator from PRL
                    int reasonForDenial = 0;       //[13] Denial reason if registrationState = 3

                    if (states.length >= 14) {
                        try {
                            if (states[0] != null) {
                                registrationState = Integer.parseInt(states[0]);
                            }
                            if (states[3] != null) {
                                radioTechnology = Integer.parseInt(states[3]);
                            }
                            if (states[4] != null) {
                                baseStationId = Integer.parseInt(states[4]);
                            }
                            if (states[5] != null) {
                                baseStationLatitude = Integer.parseInt(states[5]);
                            }
                            if (states[6] != null) {
                                baseStationLongitude = Integer.parseInt(states[6]);
                            }
                            // Some carriers only return lat-lngs of 0,0
                            if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                                baseStationLatitude  = CdmaCellLocation.INVALID_LAT_LONG;
                                baseStationLongitude = CdmaCellLocation.INVALID_LAT_LONG;
                            }
                            if (states[7] != null) {
                                cssIndicator = Integer.parseInt(states[7]);
                            }
                            if (states[8] != null) {
                                systemId = Integer.parseInt(states[8]);
                            }
                            if (states[9] != null) {
                                networkId = Integer.parseInt(states[9]);
                            }
                            if (states[10] != null) {
                                roamingIndicator = Integer.parseInt(states[10]);
                            }
                            if (states[11] != null) {
                                systemIsInPrl = Integer.parseInt(states[11]);
                            }
                            if (states[12] != null) {
                                defaultRoamingIndicator = Integer.parseInt(states[12]);
                            }
                            if (states[13] != null) {
                                reasonForDenial = Integer.parseInt(states[13]);
                            }

                            /// M: [CDMA] @{
                            if (states.length > 15 && states[15] != null) {
                                mNetworkExsit = (1 == Integer.parseInt(states[15])) ? true : false;
                            }
                            /// @}
                        } catch (NumberFormatException ex) {
                            loge("EVENT_POLL_STATE_REGISTRATION_CDMA: error parsing: " + ex);
                        }
                    } else {
                        throw new RuntimeException("Warning! Wrong number of parameters returned from "
                                + "RIL_REQUEST_REGISTRATION_STATE: expected 14 or more "
                                + "strings and got " + states.length + " strings");
                    }

                    mRegistrationState = registrationState;
                    // When registration state is roaming and TSB58
                    // roaming indicator is not in the carrier-specified
                    // list of ERIs for home system, mCdmaRoaming is true.
                    boolean cdmaRoaming =
                            regCodeIsRoaming(registrationState) && !isRoamIndForHomeSystem(states[10]);
                    mNewSS.setVoiceRoaming(cdmaRoaming);

                    /// M: [CDMA] @{
                    if (cdmaRoaming) {
                        mNewSS.setRilVoiceRegState(ServiceState.RIL_REG_STATE_ROAMING);
                    } else {
                        mNewSS.setRilVoiceRegState(registrationState);
                    }
                    /// @}

                    mNewSS.setVoiceRegState(regCodeToServiceState(registrationState));

                    mNewSS.setRilVoiceRadioTechnology(radioTechnology);

                    mNewSS.setCssIndicator(cssIndicator);
                    mNewSS.setSystemAndNetworkId(systemId, networkId);
                    mRoamingIndicator = roamingIndicator;
                    mIsInPrl = (systemIsInPrl == 0) ? false : true;
                    mDefaultRoamingIndicator = defaultRoamingIndicator;


                    // Values are -1 if not available.
                    ((CdmaCellLocation)mNewCellLoc).setCellLocationData(baseStationId,
                            baseStationLatitude, baseStationLongitude, systemId, networkId);

                    if (reasonForDenial == 0) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_GEN;
                    } else if (reasonForDenial == 1) {
                        mRegistrationDeniedReason = ServiceStateTracker.REGISTRATION_DENIED_AUTH;
                    } else {
                        mRegistrationDeniedReason = "";
                    }

                    if (mRegistrationState == 3) {
                        if (DBG) log("Registration denied, " + mRegistrationDeniedReason);
                    }
                }
                break;
            }

            case EVENT_POLL_STATE_GPRS: {
                if (mPhone.isPhoneTypeGsm()) {
                    states = (String[]) ar.result;

                    int type = 0;
                    int regState = ServiceState.RIL_REG_STATE_UNKNOWN;
                    mNewReasonDataDenied = -1;
                    mNewMaxDataCalls = 1;
                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);

                            //MTK-ADD Start : for CS not registered , PS regsitered (ex: LTE PS only
                            //mode or 2/3G PS only SIM card or CS domain network registeration
                            //temporary failure
                            if (states.length >= 3) {
                                if (states[1] != null && states[1].length() > 0) {
                                    int tempLac = Integer.parseInt(states[1], 16);
                                    if (tempLac < 0) {
                                        log("set Lac to previous value");
                                        tempLac = ((GsmCellLocation)mCellLoc).getLac();
                                    }
                                    psLac = tempLac;
                                }
                                if (states[2] != null && states[2].length() > 0) {
                                    int tempCid = Integer.parseInt(states[2], 16);
                                    if (tempCid < 0) {
                                        log("set Cid to previous value");
                                        tempCid =((GsmCellLocation)mCellLoc).getCid();
                                    }
                                    psCid = tempCid;
                                }
                            }
                            //MTK-ADD END : for CS not registered , PS regsitered (ex: LTE PS only
                            //mode or 2/3G PS only SIM card or CS domain network registeration
                            //temporary failure

                            // states[3] (if present) is the current radio technology
                            if (states.length >= 4 && states[3] != null) {
                                type = Integer.parseInt(states[3]);
                            }
                            if ((states.length >= 5) && (states[4] != null) &&
                                    (regState == ServiceState.RIL_REG_STATE_DENIED)) {
                                mNewReasonDataDenied = Integer.parseInt(states[4]);
                                log("<mNewReasonDataDenied> " + mNewReasonDataDenied);
                                mNewSS.setDataRejectCause(mNewReasonDataDenied);
                                log("set data reject cause to " + mNewReasonDataDenied);
                            }
                            if (states.length >= 6 && states[5] != null) {
                                mNewMaxDataCalls = Integer.parseInt(states[5]);
                                logd("<mNewMaxDataCalls> " + mNewMaxDataCalls);
                            }
                        } catch (NumberFormatException ex) {
                            loge("error parsing GprsRegistrationState: " + ex);
                        }
                    }
                    int dataRegState = regCodeToServiceState(regState);
                    mNewSS.setRilDataRegState(regState);
                    mNewSS.setDataRegState(dataRegState);
                    mDataRoaming = regCodeIsRoaming(regState);
                    mNewSS.setRilDataRadioTechnology(type);

                    //carrier aggregation
                    mNewSS.setProprietaryDataRadioTechnology(type);
                    //mNewSS.setRilDataRadioTechnology(type);

                    if (DBG) {
                        log("handlPollStateResultMessage: GsmSST setDataRegState=" + dataRegState
                                + " regState=" + regState
                                + " dataRadioTechnology=" + type);
                    }
                } else if (mPhone.isPhoneTypeCdma()) {
                    states = (String[])ar.result;
                    if (DBG) {
                        log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" +
                                states.length + " states=" + states);
                    }

                    int regState = ServiceState.RIL_REG_STATE_UNKNOWN;
                    int dataRadioTechnology = 0;

                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);

                            // states[3] (if present) is the current radio technology
                            if (states.length >= 4 && states[3] != null) {
                                dataRadioTechnology = Integer.parseInt(states[3]);
                            }
                        } catch (NumberFormatException ex) {
                            loge("handlePollStateResultMessage: error parsing GprsRegistrationState: "
                                    + ex);
                        }
                    }

                    int dataRegState = regCodeToServiceState(regState);
                    mNewSS.setDataRegState(dataRegState);
                    /// M: [CDMA] @{
                    mNewSS.setRilDataRegState(regState);
                    /// @}
                    mNewSS.setRilDataRadioTechnology(dataRadioTechnology);
                    mNewSS.setDataRoaming(regCodeIsRoaming(regState));
                    if (DBG) {
                        log("handlPollStateResultMessage: cdma setDataRegState=" + dataRegState
                                + " regState=" + regState
                                + " dataRadioTechnology=" + dataRadioTechnology);
                    }
                } else {
                    states = (String[])ar.result;
                    if (DBG) {
                        log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" +
                                states.length + " states=" + states);
                    }

                    int newDataRAT = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                    int regState = -1;
                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);

                            // states[3] (if present) is the current radio technology
                            if (states.length >= 4 && states[3] != null) {
                                newDataRAT = Integer.parseInt(states[3]);
                            }
                        } catch (NumberFormatException ex) {
                            loge("handlePollStateResultMessage: error parsing GprsRegistrationState: "
                                    + ex);
                        }
                    }

                    // If the unsolicited signal strength comes just before data RAT family changes (i.e.
                    // from UNKNOWN to LTE, CDMA to LTE, LTE to CDMA), the signal bar might display
                    // the wrong information until the next unsolicited signal strength information coming
                    // from the modem, which might take a long time to come or even not come at all.
                    // In order to provide the best user experience, we query the latest signal
                    // information so it will show up on the UI on time.

                    int oldDataRAT = mSS.getRilDataRadioTechnology();
                    if ((oldDataRAT == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN &&
                            newDataRAT != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) ||
                            (ServiceState.isCdma(oldDataRAT) &&
                                    newDataRAT == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) ||
                            (oldDataRAT == ServiceState.RIL_RADIO_TECHNOLOGY_LTE &&
                                    ServiceState.isCdma(newDataRAT))) {
                        mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
                    }

                    mNewSS.setRilDataRadioTechnology(newDataRAT);
                    int dataRegState = regCodeToServiceState(regState);
                    mNewSS.setDataRegState(dataRegState);
                    /// M: [CDMALTE] @{
                    mNewSS.setRilDataRegState(regState);
                    // Carrier aggregation
                    mNewSS.setProprietaryDataRadioTechnology(newDataRAT);
                    /// @}
                    // voice roaming state in done while handling EVENT_POLL_STATE_REGISTRATION_CDMA
                    /// M: [CDMALTE] @{
                    boolean isDateRoaming = regCodeIsRoaming(regState);
                    mNewSS.setDataRoaming(isDateRoaming);
                    if (isDateRoaming) {
                        mNewSS.setRilDataRegState(ServiceState.RIL_REG_STATE_ROAMING);
                    }
                    /// @}
                    if (DBG) {
                        log("handlPollStateResultMessage: CdmaLteSST setDataRegState=" + dataRegState
                                + " regState=" + regState
                                + " dataRadioTechnology=" + newDataRAT);
                    }
                }
                break;
            }

            case EVENT_POLL_STATE_OPERATOR: {
                if (mPhone.isPhoneTypeGsm()) {
                    String opNames[] = (String[]) ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        // FIXME: Giving brandOverride higher precedence, is this desired?
                        String brandOverride = mUiccController.getUiccCard(getPhoneId()) != null ?
                                mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                        if (brandOverride != null) {
                            log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + brandOverride);
                            mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                        } else {

                            String strOperatorLong = null;
                            String strOperatorShort = null;
                            SpnOverride spnOverride = SpnOverride.getInstance();

                            strOperatorLong = mCi.lookupOperatorNameFromNetwork(
                                    SubscriptionManager.getSubIdUsingPhoneId(mPhone.getPhoneId()),
                                    opNames[2], true);
                            if (strOperatorLong != null) {
                                log("EVENT_POLL_STATE_OPERATOR: OperatorLong use lookFromNetwork");
                            } else {
                                strOperatorLong = spnOverride.lookupOperatorName(
                                        SubscriptionManager.getSubIdUsingPhoneId(
                                                mPhone.getPhoneId()), opNames[2], true,
                                                mPhone.getContext());
                                if (strOperatorLong != null) {
                                    logd("EVENT_POLL_STATE_OPERATOR: "
                                        + "OperatorLong use lookupOperatorName");
                                    strOperatorLong = mServiceStateExt.updateOpAlphaLongForHK(
                                        strOperatorLong, opNames[2], mPhone.getPhoneId());
                                } else {
                                    log("EVENT_POLL_STATE_OPERATOR: "
                                        + "OperatorLong use value from ril");
                                    strOperatorLong = opNames[0];
                                }
                            }
                            strOperatorShort = mCi.lookupOperatorNameFromNetwork(
                                    SubscriptionManager.getSubIdUsingPhoneId(mPhone.getPhoneId()),
                                            opNames[2], false);
                            if (strOperatorShort != null) {
                                log("EVENT_POLL_STATE_OPERATOR: OperatorShort use "
                                    + "lookupOperatorNameFromNetwork");
                            } else {
                                strOperatorShort = spnOverride.lookupOperatorName(
                                        SubscriptionManager.getSubIdUsingPhoneId(
                                                mPhone.getPhoneId()),
                                                opNames[2],
                                                false, mPhone.getContext());
                                if (strOperatorShort != null) {
                                    logd("EVENT_POLL_STATE_OPERATOR: OperatorShort "
                                            + "use lookupOperatorName");
                                } else {
                                    log("EVENT_POLL_STATE_OPERATOR: OperatorShort "
                                            + "use value from ril");
                                    strOperatorShort = opNames[1];
                                }
                            }
                            log("EVENT_POLL_STATE_OPERATOR: " + strOperatorLong
                                    + ", " + strOperatorShort);
                            mNewSS.setOperatorName (strOperatorLong, strOperatorShort, opNames[2]);
                        }
                        updateLocatedPlmn(opNames[2]);
                    } else if (opNames != null && opNames.length == 1) {
                        log("opNames:" + opNames[0] + " len=" + opNames[0].length());
                        mNewSS.setOperatorName(null, null, null);
                        // to keep the original AOSP behavior, set null when not registered

                        /* Do NOT update invalid PLMN value "000000" */
                        if (opNames[0].length() >= 5 && !(opNames[0].equals("000000"))) {
                            updateLocatedPlmn(opNames[0]);
                        } else {
                            updateLocatedPlmn(null);
                        }
                    }
                } else {
                    String opNames[] = (String[])ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        // TODO: Do we care about overriding in this case.
                        // If the NUMERIC field isn't valid use PROPERTY_CDMA_HOME_OPERATOR_NUMERIC
                        if ((opNames[2] == null) || (opNames[2].length() < 5)
                                || ("00000".equals(opNames[2]))
                                || ("N/AN/A".equals(opNames[2]))) {
                            opNames[2] = SystemProperties.get(
                                    GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
                            if (DBG) {
                                log("RIL_REQUEST_OPERATOR.response[2], the numeric, " +
                                        " is bad. Using SystemProperties '" +
                                        GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC +
                                        "'= " + opNames[2]);
                            }
                        }

                        /// M: [CDMA] Add for cdma plus code feature. @{
                        String numeric = opNames[2];
                        boolean plusCode = false;
                        if (numeric.startsWith("2134") && numeric.length() == 7) {
                            String tempStr = mPlusCodeUtils.checkMccBySidLtmOff(numeric);
                            if (!tempStr.equals("0")) {
                                opNames[2] = tempStr + numeric.substring(4);
                                numeric = tempStr;
                                log("EVENT_POLL_STATE_OPERATOR_CDMA: checkMccBySidLtmOff: numeric ="
                                        + numeric + ", plmn =" + opNames[2]);
                            }
                            plusCode = true;
                        }
                        /// @}

                        if (!mIsSubscriptionFromRuim) {
                            // NV device (as opposed to CSIM)
                            /// M: [CDMA] Add for cdma plus code feature. @{
                            if (plusCode) {
                                opNames[1] = SpnOverride.getInstance().lookupOperatorName(
                                        mPhone.getSubId(), opNames[2], false, mPhone.getContext());
                            }
                            mNewSS.setOperatorName(null, opNames[1], opNames[2]);
                            /// @}
                        } else {
                            String brandOverride = mUiccController.getUiccCard(
                                    getPhoneId()) != null ?
                                    mUiccController.getUiccCard(
                                            getPhoneId()).getOperatorBrandOverride() : null;
                            if (brandOverride != null) {
                                /// M: [CDMA] Add log. @{
                                log("EVENT_POLL_STATE_OPERATOR_CDMA: use brand=" + brandOverride);
                                /// @}
                                mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                            } else {
                                /// M: [CDMA] Add for Operator Name display. @{
                                // mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                                String strOperatorLong = null;
                                String strOperatorShort = null;
                                SpnOverride spnOverride = SpnOverride.getInstance();

                                strOperatorLong = mCi.lookupOperatorNameFromNetwork(
                                        SubscriptionManager.getSubIdUsingPhoneId(
                                                mPhone.getPhoneId()), opNames[2], true);
                                if (strOperatorLong != null) {
                                    log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong "
                                            + "use lookupOperatorNameFromNetwork");
                                } else {
                                    strOperatorLong = spnOverride.lookupOperatorName(
                                            SubscriptionManager.getSubIdUsingPhoneId(
                                                    mPhone.getPhoneId()), opNames[2], true,
                                            mPhone.getContext());
                                    if (strOperatorLong != null) {
                                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong "
                                                + "use lookupOperatorName");
                                    } else {
                                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong "
                                                + "use value from ril");
                                        strOperatorLong = opNames[0];
                                    }
                                }
                                strOperatorShort = mCi.lookupOperatorNameFromNetwork(
                                        SubscriptionManager.getSubIdUsingPhoneId(
                                                mPhone.getPhoneId()), opNames[2], false);
                                if (strOperatorShort != null) {
                                    log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort use "
                                            + "lookupOperatorNameFromNetwork");
                                } else {
                                    strOperatorShort = spnOverride.lookupOperatorName(
                                            SubscriptionManager.getSubIdUsingPhoneId(
                                                    mPhone.getPhoneId()),
                                            opNames[2], false, mPhone.getContext());
                                    if (strOperatorShort != null) {
                                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort "
                                                + "use lookupOperatorName");
                                    } else {
                                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort "
                                                + "use value from ril");
                                        strOperatorShort = opNames[1];
                                    }
                                }
                                log("EVENT_POLL_STATE_OPERATOR_CDMA: "
                                        + strOperatorLong + ", " + strOperatorShort);
                                mNewSS.setOperatorName(strOperatorLong, strOperatorShort,
                                        opNames[2]);
                                /// @}
                            }
                        }
                    } else {
                        if (DBG) log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                    }
                }
                break;
            }

            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE: {
                ints = (int[])ar.result;
                mNewSS.setIsManualSelection(ints[0] == 1);
                if ((ints[0] == 1) && (!mPhone.isManualNetSelAllowed())) {
                        /*
                         * modem is currently in manual selection but manual
                         * selection is not allowed in the current mode so
                         * switch to automatic registration
                         */
                    mPhone.setNetworkSelectionModeAutomatic (null);
                    log(" Forcing Automatic Network Selection, " +
                            "manual selection is not allowed");
                }
                break;
            }

            default:
                loge("handlePollStateResultMessage: Unexpected RIL response received: " + what);
        }
    }

    /**
     * Determine whether a roaming indicator is in the carrier-specified list of ERIs for
     * home system
     *
     * @param roamInd roaming indicator in String
     * @return true if the roamInd is in the carrier-specified list of ERIs for home network
     */
    private boolean isRoamIndForHomeSystem(String roamInd) {
        // retrieve the carrier-specified list of ERIs for home system
        String[] homeRoamIndicators = mPhone.getContext().getResources()
                .getStringArray(com.android.internal.R.array.config_cdma_home_system);

        if (homeRoamIndicators != null) {
            // searches through the comma-separated list for a match,
            // return true if one is found.
            for (String homeRoamInd : homeRoamIndicators) {
                if (homeRoamInd.equals(roamInd)) {
                    return true;
                }
            }
            // no matches found against the list!
            return false;
        }

        // no system property found for the roaming indicators for home system
        return false;
    }

    /**
     * Query the carrier configuration to determine if there any network overrides
     * for roaming or not roaming for the current service state.
     */
    protected void updateRoamingState() {
        if (mPhone.isPhoneTypeGsm()) {
            /**
             * Since the roaming state of gsm service (from +CREG) and
             * data service (from +CGREG) could be different, the new SS
             * is set to roaming when either is true.
             *
             * There are exceptions for the above rule.
             * The new SS is not set as roaming while gsm service reports
             * roaming but indeed it is same operator.
             * And the operator is considered non roaming.
             *
             * The test for the operators is to handle special roaming
             * agreements and MVNO's.
             */
            boolean roaming = (mGsmRoaming || mDataRoaming);
            log("set roaming=" + roaming + ",mGsmRoaming= " + mGsmRoaming
                            + ",mDataRoaming= " + mDataRoaming);

            //add for special SIM
            boolean isRoamingForSpecialSim = false;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                String simType = PhoneFactory.getPhone(mPhone.getPhoneId())
                        .getIccCard().getIccCardType();
                try {
                    if ((mNewSS.getOperatorNumeric() != null)
                            && (getSIMOperatorNumeric() != null)
                            && ((simType != null) && (!simType.equals("")) && simType
                                    .equals("CSIM"))
                            && mServiceStateExt.isRoamingForSpecialSIM(
                                    mNewSS.getOperatorNumeric(), getSIMOperatorNumeric())) {
                        isRoamingForSpecialSim = true;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            if (!isRoamingForSpecialSim) {
                //ALPS02446235[
                /* AOSP
                if (mGsmRoaming && !isOperatorConsideredRoaming(mNewSS) &&
                    (isSameNamedOperators(mNewSS) || isOperatorConsideredNonRoaming(mNewSS))) {
                */
                if (mGsmRoaming && isSameNamedOperators(mNewSS)
                        && !isOperatorConsideredRoamingMtk(mNewSS)) {
                // ALPS02446235]
                    if (VDBG)
                        log("set raoming fasle due to special roaming agreements and MVNO's.");
                    roaming = false;
                }

                if (mPhone.isMccMncMarkedAsNonRoaming(mNewSS.getOperatorNumeric())) {
                    roaming = false;
                } else if (mPhone.isMccMncMarkedAsRoaming(mNewSS.getOperatorNumeric())) {
                    roaming = true;
                }
            }

            // Save the roaming state before carrier config possibly overrides it.
            mNewSS.setDataRoamingFromRegistration(roaming);

            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);

            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());

                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        roaming = false;
                    } else if (isNonRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set non roaming:"
                                + mNewSS.getOperatorNumeric());
                        roaming = false;
                    } else if (isRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set roaming:"
                                + mNewSS.getOperatorNumeric());
                        roaming = true;
                    }
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }

            mNewSS.setVoiceRoaming(roaming);
            mNewSS.setDataRoaming(roaming);
        } else {
            // Save the roaming state before carrier config possibly overrides it.
            mNewSS.setDataRoamingFromRegistration(mNewSS.getDataRoaming());

            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());
                    String systemId = Integer.toString(mNewSS.getSystemId());

                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        setRoamingOff();
                    } else if (isNonRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())
                            || isNonRoamingInCdmaNetwork(b, systemId)) {
                        log("updateRoamingState: carrier config override set non-roaming:"
                                + mNewSS.getOperatorNumeric() + ", " + systemId);
                        setRoamingOff();
                    } else if (isRoamingInGsmNetwork(b, mNewSS.getOperatorNumeric())
                            || isRoamingInCdmaNetwork(b, systemId)) {
                        log("updateRoamingState: carrier config override set roaming:"
                                + mNewSS.getOperatorNumeric() + ", " + systemId);
                        setRoamingOn();
                    }
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }

            if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
                mNewSS.setVoiceRoaming(true);
                mNewSS.setDataRoaming(true);
            }
        }
    }

    private void setRoamingOn() {
        mNewSS.setVoiceRoaming(true);
        mNewSS.setDataRoaming(true);
        mNewSS.setCdmaEriIconIndex(EriInfo.ROAMING_INDICATOR_ON);
        mNewSS.setCdmaEriIconMode(EriInfo.ROAMING_ICON_MODE_NORMAL);
    }

    private void setRoamingOff() {
        mNewSS.setVoiceRoaming(false);
        mNewSS.setDataRoaming(false);
        mNewSS.setCdmaEriIconIndex(EriInfo.ROAMING_INDICATOR_OFF);
    }

    public void refreshSpnDisplay() {
        String numeric = mSS.getOperatorNumeric();
        String newAlphaLong = null;
        String newAlphaShort = null;

        if ((numeric != null) && (!(numeric.equals("")))) {
            newAlphaLong = SpnOverride.getInstance().lookupOperatorName(
                   SubscriptionManager.getSubIdUsingPhoneId(mPhone.getPhoneId()), numeric,
                   true, mPhone.getContext());
            newAlphaShort = SpnOverride.getInstance().lookupOperatorName(
                   SubscriptionManager.getSubIdUsingPhoneId(mPhone.getPhoneId()), numeric,
                   false, mPhone.getContext());
            //[ALPS01804936]-start:fix JE when change system language to "Burmese"
            //mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA,
            //      newAlphaLong);
            //updateOperatorAlpha(newAlphaLong);    //remark for [ALPS01965792]
            //[ALPS01804936]-end

            if (mPhone.isPhoneTypeGsm()) {
                if (newAlphaLong != null) {
                    newAlphaLong = mServiceStateExt.updateOpAlphaLongForHK(newAlphaLong,
                            numeric, mPhone.getPhoneId());
                }
            }

            log("refreshSpnDisplay set alpha to " + newAlphaLong + ","
                    + newAlphaShort + "," + numeric);
            mSS.setOperatorName(newAlphaLong, newAlphaShort, numeric);
        }
        updateSpnDisplay();
    }

    protected void updateSpnDisplay() {
        if (mPhone.isPhoneTypeGsm()) {
            updateSpnDisplayGsm(false);
        } else {
            updateSpnDisplayCdma(false);
        }
    }

    protected void updateSpnDisplayGsm(boolean forceUpdate) {
        SIMRecords simRecords = null;
        IccRecords r = mPhone.mIccRecords.get();
        if (r != null) {
            simRecords = (SIMRecords) r;
        }

        int rule = (simRecords != null) ? simRecords.getDisplayRule(
                mSS.getOperatorNumeric()) : SIMRecords.SPN_RULE_SHOW_PLMN;
        String strNumPlmn = mSS.getOperatorNumeric();
        String spn = (simRecords != null) ? simRecords.getServiceProviderName() : "";
        String sEons = null;
        boolean showPlmn = false;
        String plmn = null;
        String realPlmn = null;
        String mSimOperatorNumeric = (simRecords != null) ? simRecords.getOperatorNumeric() : "";

        try {
            sEons = (simRecords != null) ? simRecords.getEonsIfExist(mSS.getOperatorNumeric(),
                    ((GsmCellLocation)mCellLoc).getLac(), true) : null;
        } catch (RuntimeException ex) {
            loge("Exception while getEonsIfExist. " + ex);
        }

        if (sEons != null) {
            plmn = sEons;
        }
        else if (strNumPlmn != null && strNumPlmn.equals(mSimOperatorNumeric)) {
            log("Home PLMN, get CPHS ons");
            plmn = (simRecords != null) ? simRecords.getSIMCPHSOns() : "";
        }

        if (TextUtils.isEmpty(plmn)) {
            log("No matched EONS and No CPHS ONS");
            plmn = mSS.getOperatorAlphaLong();
            // M:[ALPS02414050] OperatorAlphaLong maybe ""
            if (TextUtils.isEmpty(plmn) || plmn.equals(mSS.getOperatorNumeric())) {
                plmn = mSS.getOperatorAlphaShort();
            }
        }

        /*[ALPS00460547] - star */
        //keep operator name for update PROPERTY_OPERATOR_ALPHA
        realPlmn = plmn;
        /*[ALPS00460547] - end */

        // Do not display SPN before get normal service
        //M: for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS only SIM card
        //or CS domain network registeration temporary failure
        //if (mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) {
        if ((mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) &&
                (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE)) {
            showPlmn = true;
            plmn = Resources.getSystem().
                    getText(com.android.internal.R.string.lockscreen_carrier_default).toString();

        }
        log("updateSpnDisplay mVoiceCapable=" + mVoiceCapable + " mEmergencyOnly=" + mEmergencyOnly
            + " mCi.getRadioState().isOn()=" + mCi.getRadioState().isOn() + " getVoiceRegState()="
            + mSS.getVoiceRegState() + " getDataRegState()" + mSS.getDataRegState());

        // ALPS00283717 For emergency calls only, pass the EmergencyCallsOnly string via EXTRA_PLMN
        //MTK-ADD START : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
        //only SIM card or CS domain network registeration temporary failure
        //mEmergencyOnly is always right only when voice's urc is not 4G
        if ((voiceUrcWith4G == false) && mVoiceCapable && mEmergencyOnly && mCi.getRadioState().isOn()
                && (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE)) {
            log("updateSpnDisplay show mEmergencyOnly");
            showPlmn = true;

            plmn = Resources.getSystem().getText(
                    com.android.internal.R.string.emergency_calls_only).toString();

            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    //CDR-NWS-2409
                    if(mServiceStateExt.needBlankDisplay(mSS.getVoiceRejectCause()) == true){
                        log("Do NOT show emergency call only display");
                        plmn = "";
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        // If voice's urc is 4G, show ECC only when IMS's ECC is supported and OOS
        ImsPhone imsPhone = (ImsPhone) mPhone.getImsPhone();
        if ((voiceUrcWith4G == true) && mPhone.isImsUseEnabled() && (imsPhone != null)) {
            if (imsPhone.getServiceState() != null) {
                boolean isImsEccOnly = imsPhone.isSupportLteEcc();
                if (isImsEccOnly &&
                        (mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) &&
                        (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE)) {
                    log("updateSpnDisplay show mEmergencyOnly for Ims ECC");
                    showPlmn = true;
                    plmn = Resources.getSystem().getText(
                            com.android.internal.R.string.emergency_calls_only).toString();
                }
            }
        }


        /**
         * mImeiAbnormal=0, Valid IMEI
         * mImeiAbnormal=1, IMEI is null or not valid format
         * mImeiAbnormal=2, Phone1/Phone2 have same IMEI
         */
        int imeiAbnormal = mPhone.isDeviceIdAbnormal();
        if (imeiAbnormal == 1) {
            //[ALPS00872883] don't update plmn string when radio is not available
            if (mCi.getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                plmn = Resources.getSystem().getText(com.mediatek.R.string.invalid_imei).toString();
            }
        } else if (imeiAbnormal == 2) {
            plmn = Resources.getSystem().getText(com.mediatek.R.string.same_imei).toString();
        } else if (imeiAbnormal == 0) {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    plmn = mServiceStateExt.onUpdateSpnDisplay(plmn, mSS,
                               mPhone.getPhoneId());
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            // If CS not registered , PS registered , add "Data
            // connection only" postfix in PLMN name
            if ((mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) &&
                    (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
                //[ALPS01650043]-Start: don't update PLMN name
                // when it is null for backward compatible
                if (plmn != null) {
                    if (getImsServiceState() != ServiceState.STATE_IN_SERVICE) {
                        plmn = plmn + "(" + Resources.getSystem()
                                .getText(com.mediatek.R.string.data_conn_only)
                                .toString() + ")";
                    }
                } else {
                    log("PLMN name is null when CS not registered and PS registered");
                }
            }
        }
        /* ALPS00296298 */
        if (mIsImeiLock) {
            plmn = Resources.getSystem().getText(com.mediatek.R.string.invalid_card).toString();
        }

        //MTK-ADD Start : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
        //only SIM card or CS domain network registeration temporary failure
        //if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
        if ((mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) ||
            (mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
            showPlmn = !TextUtils.isEmpty(plmn) &&
                    ((rule & SIMRecords.SPN_RULE_SHOW_PLMN)
                            == SIMRecords.SPN_RULE_SHOW_PLMN);
        /* } else {
                  // Power off state, such as airplane mode, show plmn as "No service"
                  showPlmn = true;
                  plmn = Resources.getSystem().
                  getText(com.android.internal.R.string.lockscreen_carrier_default).toString();
                  if (DBG) log("updateSpnDisplay: radio is off w/ showPlmn="
                         + showPlmn + " plmn=" + plmn);
              }              */
        }

             /*
              // The value of spn/showSpn are same in different scenarios.
              // EXTRA_SHOW_SPN = depending on IccRecords rul
              // EXTRA_SPN = spn
              String spn = (iccRecords != null) ? iccRecords.getServiceProviderName() : "";
             */

        // The value of spn/showSpn are same in different scenarios.
        //    EXTRA_SHOW_SPN = depending on IccRecords rule and radio/IMS state
        //    EXTRA_SPN = spn
        //    EXTRA_DATA_SPN = dataSpn
        String dataSpn = spn;
        boolean showSpn = !TextUtils.isEmpty(spn)
                && ((rule & SIMRecords.SPN_RULE_SHOW_SPN)
                        == SIMRecords.SPN_RULE_SHOW_SPN);

        if (!TextUtils.isEmpty(spn)
                && mPhone.getImsPhone() != null
                && ((ImsPhone) mPhone.getImsPhone()).isWifiCallingEnabled() ) {
            // In Wi-Fi Calling mode show SPN+WiFi
            final String[] wfcSpnFormats =
                    mPhone.getContext().getResources().getStringArray(
                            com.android.internal.R.array.wfcSpnFormats);
            int voiceIdx = 0;
            int dataIdx = 0;
            CarrierConfigManager configLoader = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(mPhone.getSubId());
                    if (b != null) {
                        voiceIdx = b.getInt(CarrierConfigManager.KEY_WFC_SPN_FORMAT_IDX_INT);
                        dataIdx = b.getInt(
                                CarrierConfigManager.KEY_WFC_DATA_SPN_FORMAT_IDX_INT);
                    }
                } catch (Exception e) {
                    loge("updateSpnDisplay: carrier config error: " + e);
                }
            }

            String formatVoice = wfcSpnFormats[voiceIdx];
            String formatData = wfcSpnFormats[dataIdx];
            String originalSpn = spn.trim();
            spn = String.format(formatVoice, originalSpn);
            dataSpn = String.format(formatData, originalSpn);
            showSpn = true;
            showPlmn = false;
        /// M: ALPS02293142, don't show spn when no service/emergency only
        } else if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                || (showPlmn && TextUtils.equals(spn, plmn))
                || ((mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE) &&
                        (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE))) {
            // airplane mode or spn equals plmn, do not show spn
            spn = null;
            showSpn = false;
        }

        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                if (mServiceStateExt.needSpnRuleShowPlmnOnly() && !TextUtils.isEmpty(plmn)) {
                    log("origin showSpn:" + showSpn + " showPlmn:" + showPlmn + " rule:" + rule);
                    showSpn = false;
                    showPlmn = true;
                    rule = SIMRecords.SPN_RULE_SHOW_PLMN;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        ///M : WFC @{
        try {
            plmn = mServiceStateExt.onUpdateSpnDisplayForIms(
                                   plmn, mSS, ((GsmCellLocation)mCellLoc).getLac(),
                                   mPhone.getPhoneId(),simRecords);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        /// @}

        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int[] subIds = SubscriptionManager.getSubId(mPhone.getPhoneId());
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }

        // Update SPN_STRINGS_UPDATED_ACTION IFF any value changes
        if (mSubId != subId ||
                showPlmn != mCurShowPlmn
                || showSpn != mCurShowSpn
                || !TextUtils.equals(spn, mCurSpn)
                || !TextUtils.equals(dataSpn, mCurDataSpn)
                || !TextUtils.equals(plmn, mCurPlmn)
                || forceUpdate) {
            // M: [ALPS521030] for [CT case][TC-IRLAB-02009]
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (!mServiceStateExt.allowSpnDisplayed()) {
                        log("For CT test case don't show SPN.");
                        if (rule == (SIMRecords.SPN_RULE_SHOW_PLMN
                                | SIMRecords.SPN_RULE_SHOW_SPN)) {
                            showSpn = false;
                            spn = null;
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (DBG) {
                log(String.format("updateSpnDisplay: changed" +
                        " sending intent rule=" + rule +
                        " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'",
                        showPlmn, plmn, showSpn, spn, dataSpn, subId));
            }

            Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);

            // For multiple SIM support, share the same intent, do not replace the other one
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }

            intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, showSpn);
            intent.putExtra(TelephonyIntents.EXTRA_SPN, spn);
            intent.putExtra(TelephonyIntents.EXTRA_DATA_SPN, dataSpn);
            intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);

            //M: Femtocell (CSG) info
            intent.putExtra(TelephonyIntents.EXTRA_HNB_NAME, mHhbName);
            intent.putExtra(TelephonyIntents.EXTRA_CSG_ID, mCsgId);
            intent.putExtra(TelephonyIntents.EXTRA_DOMAIN, mFemtocellDomain);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

            int phoneId = mPhone.getPhoneId();

            // Append Femtocell (CSG) Info
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")){
                if((mHhbName == null) && (mCsgId != null)){
                    if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        try {
                            if (mServiceStateExt.needToShowCsgId() == true) {
                                plmn += " - ";
                                plmn += mCsgId;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        plmn += " - ";
                        plmn += mCsgId;
                    }
                } else if(mHhbName != null){
                    plmn += " - ";
                    plmn += mHhbName;
                }
            }

            boolean setResult = mSubscriptionController.setPlmnSpn(phoneId,
                    showPlmn, plmn, showSpn, spn);
            if (!setResult) {
                mSpnUpdatePending = true;
            }
            log("showSpn:" + showSpn + " spn:" + spn + " showPlmn:" + showPlmn +
                    " plmn:" + plmn + " rule:" + rule +
                    " setResult:" + setResult + " phoneId:" + phoneId);
        }

        //[ALPS01554309]-start
        // update new operator info. when operator numeric has change.
        /* ALPS00357573 for consistent operator name display */
        String operatorLong = mSS.getOperatorAlphaLong();
        if ((showSpn == true) && (showPlmn == false) && (spn != null)) {
            /* When only <spn> is shown , we update with <spn> */
            log("updateAllOpertorInfo with spn:" + spn);
            //[ALPS01804936]-start:fix JE when change system language to "Burmese"
            //mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, spn);
            if (operatorLong == null || !operatorLong.equals(spn)) {
                mSS.setOperatorAlphaLong(spn); // add for [ALPS01965792]
                //[ALPS01804936]-end
                mNeedNotify = true;
            }
            updateOperatorAlpha(spn);
        } else {
            log("updateAllOpertorInfo with realPlmn:" + realPlmn);
            //[ALPS01804936]-start:fix JE when change system language to "Burmese"
            //mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, realPlmn);
            if (operatorLong == null || !operatorLong.equals(realPlmn)) {
                mSS.setOperatorAlphaLong(realPlmn); // add for [ALPS01965792]
                // [ALPS01804936]-end
                mNeedNotify = true;
            }
            updateOperatorAlpha(realPlmn);
        }
        //[ALPS01554309]-end

        mSubId = subId;
        mCurShowSpn = showSpn;
        mCurShowPlmn = showPlmn;
        mCurSpn = spn;
        mCurDataSpn = dataSpn;
        mCurPlmn = plmn;
    }

    /// M: [CDMA] Add for CDMA update SpnDisplay. @{
    private void updateSpnDisplayCdma(boolean forceUpdate) {
        // mOperatorAlphaLong contains the ERI text
        String plmn = mSS.getOperatorAlphaLong();
        boolean showPlmn = false;

        showPlmn = plmn != null;

        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int[] subIds = SubscriptionManager.getSubId(mPhone.getPhoneId());
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }

        /// M: [CDMA] Add for the spn display feature. @{
        if (plmn == null || plmn.equals("")) {
            plmn = mSS.getOperatorAlphaLong();
            if (plmn == null || plmn.equals(mSS.getOperatorNumeric())) {
                plmn = mSS.getOperatorAlphaShort();
            }
        }

        if (plmn != null) {
            showPlmn = true;
            if (plmn.equals("")) {
                plmn = null;
            }
        }

        log("[CDMA]updateSpnDisplay: getOperatorAlphaLong=" + mSS.getOperatorAlphaLong()
                + ", getOperatorAlphaShort=" + mSS.getOperatorAlphaShort()
                + ", plmn=" + plmn + ", forceUpdate=" + forceUpdate);

        // Do not display SPN before get normal service
        if ((mSS.getState() != ServiceState.STATE_IN_SERVICE) &&
                (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE)) {
            log("[CDMA]updateSpnDisplay: Do not display SPN before get normal service");
            showPlmn = true;
            plmn = Resources.getSystem().getText(
                    com.android.internal.R.string.lockscreen_carrier_default).toString();
        }

        // C2k modify for emergency only.
        if (mEmergencyOnly && mCi.getRadioState().isOn()) {
            log("[CDMA]updateSpnDisplay: phone show emergency call only, mEmergencyOnly = true");
            showPlmn = true;
            plmn = Resources.getSystem().
                    getText(com.android.internal.R.string.emergency_calls_only).toString();
        }

        int rule = 0;
        String spn = "";
        boolean showSpn = false;
        // From RuimRecord get show display rule and spn
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                if (mServiceStateExt.allowSpnDisplayed()) {
                    IccRecords r = mPhone.mIccRecords.get();
                    rule = (r != null) ? r.getDisplayRule(mSS
                            .getOperatorNumeric()) : IccRecords.SPN_RULE_SHOW_PLMN;
                    spn = (r != null) ? r.getServiceProviderName() : "";

                    showSpn = !TextUtils.isEmpty(spn)
                            && ((rule & RuimRecords.SPN_RULE_SHOW_SPN)
                                == RuimRecords.SPN_RULE_SHOW_SPN)
                            && !(mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF)
                            && !mSS.getRoaming();

                    log("[CDMA]updateSpnDisplay: rule=" + rule + ", spn=" + spn
                            + ", showSpn=" + showSpn);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        /// @}

        /// M: [CDMA] @{
        if (mSubId != subId
                || showPlmn != mCurShowPlmn
                || showSpn != mCurShowSpn
                || !TextUtils.equals(spn, mCurSpn)
                || !TextUtils.equals(plmn, mCurPlmn)
                || forceUpdate) {
            /// @}
            // Allow A blank plmn, "" to set showPlmn to true. Previously, we
            // would set showPlmn to true only if plmn was not empty, i.e. was not
            // null and not blank. But this would cause us to incorrectly display
            // "No Service". Now showPlmn is set to true for any non null string.

            /// M: [CDMA] Modify for the spn display feature. @{
            showPlmn = plmn != null;

            // Airplane mode, out_of_service, roaming state or spn is null, do not show spn
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (mServiceStateExt.allowSpnDisplayed()) {
                        if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF
                                || mSS.getVoiceRegState() == ServiceState.STATE_OUT_OF_SERVICE
                                || mSS.getRoaming()
                                || (spn == null || spn.equals(""))) {
                            showSpn = false;
                            showPlmn = true;
                        } else {
                            showSpn = true;
                            showPlmn = false;
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            if (DBG) {
                log(String.format("[CDMA]updateSpnDisplay: changed sending intent" +
                        " subId='%d' showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'",
                        subId, showPlmn, plmn, showSpn, spn));
            }
            /// @}

            Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            // For multiple SIM support, share the same intent, do not replace the other one
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }
            intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, showSpn);
            intent.putExtra(TelephonyIntents.EXTRA_SPN, spn);
            intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

            /// M: [CDMA] @{
            boolean setResult = mSubscriptionController.setPlmnSpn(mPhone.getPhoneId(),
                    showPlmn, plmn, showSpn, spn);
            if (!setResult) {
                mSpnUpdatePending = true;
            }
            log("[CDMA]updateSpnDisplay: subId=" + subId +
                    ", showPlmn=" + showPlmn +
                    ", plmn=" + plmn +
                    ", showSpn=" + showSpn +
                    ", spn=" + spn +
                    ", setResult=" + setResult +
                    ", mSpnUpdatePending=" + mSpnUpdatePending);
            /// @}
        }

        mSubId = subId;
        mCurShowSpn = showSpn;
        mCurShowPlmn = showPlmn;
        mCurSpn = spn;
        mCurPlmn = plmn;
    }
    /// @}

    protected void setPowerStateToDesired() {
        if (DBG) {
            log("mDeviceShuttingDown=" + mDeviceShuttingDown +
                    ", mDesiredPowerState=" + mDesiredPowerState +
                    ", getRadioState=" + mCi.getRadioState() +
                    ", mPowerOffDelayNeed=" + mPowerOffDelayNeed +
                    ", mAlarmSwitch=" + mAlarmSwitch);
        }

        if (mPhone.isPhoneTypeGsm() && mAlarmSwitch) {
            if(DBG) log("mAlarmSwitch == true");
            Context context = mPhone.getContext();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mRadioOffIntent);
            mAlarmSwitch = false;
        }

        // If we want it on and it's off, turn it on
        if (mDesiredPowerState
                && mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            // MTK-START some actions must be took before EFUN
            RadioManager.getInstance().sendRequestBeforeSetRadioPower(true, mPhone.getPhoneId());
            /// MTK-END
            mCi.setRadioPower(true, null);
        } else if (!mDesiredPowerState && mCi.getRadioState().isOn()) {
            // If it's on and available and we want it off gracefully
            if (mPhone.isPhoneTypeGsm() && mPowerOffDelayNeed) {
                if (mImsRegistrationOnOff && !mAlarmSwitch) {
                    if(DBG) log("mImsRegistrationOnOff == true");
                    Context context = mPhone.getContext();
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    Intent intent = new Intent(ACTION_RADIO_OFF);
                    mRadioOffIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

                    mAlarmSwitch = true;
                    if (DBG) log("Alarm setting");
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + 3000, mRadioOffIntent);
                } else {
                    DcTracker dcTracker = mPhone.mDcTracker;
                    powerOffRadioSafely(dcTracker);
                }
            } else {
                DcTracker dcTracker = mPhone.mDcTracker;
                powerOffRadioSafely(dcTracker);
            }
        } else if (mDeviceShuttingDown && mCi.getRadioState().isAvailable()) {
            mCi.requestShutdown(null);
        }
    }

    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = getUiccCardApplication();

        /// M: [CDMA] Add for show EccButton when PIN and PUK status. @{
        if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
            if (newUiccApplication != null) {
                AppState appState = newUiccApplication.getState();
                if ((appState == AppState.APPSTATE_PIN || appState == AppState.APPSTATE_PUK)
                        && mNetworkExsit) {
                    mEmergencyOnly = true;
                } else {
                    mEmergencyOnly = false;
                }
                log("[CDMA]onUpdateIccAvailability, appstate=" + appState
                        + ", mNetworkExsit=" + mNetworkExsit
                        + ", mEmergencyOnly=" + mEmergencyOnly);
            }
        }
        /// @}

        if (mUiccApplcation != newUiccApplication) {
            if (mUiccApplcation != null) {
                log("Removing stale icc objects.");
                mUiccApplcation.unregisterForReady(this);
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }
                mIccRecords = null;
                mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                log("New card found");
                mUiccApplcation = newUiccApplication;
                mIccRecords = mUiccApplcation.getIccRecords();
                if (mPhone.isPhoneTypeGsm()) {
                    mUiccApplcation.registerForReady(this, EVENT_SIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
                    }
                } else if (mIsSubscriptionFromRuim) {
                    mUiccApplcation.registerForReady(this, EVENT_RUIM_READY, null);
                    if (mIccRecords != null) {
                        mIccRecords.registerForRecordsLoaded(this, EVENT_RUIM_RECORDS_LOADED, null);
                    }
                }
            }
        }
    }

    protected void logd(String s) {
        if (mEngLoad ||(mLogLv>0)) {
            if (mPhone.isPhoneTypeGsm()) {
                Rlog.d(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
            } else if (mPhone.isPhoneTypeCdma()) {
                Rlog.d(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
            } else {
                Rlog.d(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
            }
        }
    }

    protected void log(String s) {
        if (mPhone.isPhoneTypeGsm()) {
            Rlog.d(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
        } else if (mPhone.isPhoneTypeCdma()) {
            Rlog.d(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.d(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
        }
    }

    protected void loge(String s) {
        if (mPhone.isPhoneTypeGsm()) {
            Rlog.e(LOG_TAG, "[GsmSST" + mPhone.getPhoneId() + "] " + s);
        } else if (mPhone.isPhoneTypeCdma()) {
            Rlog.e(LOG_TAG, "[CdmaSST" + mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.e(LOG_TAG, "[CdmaLteSST" + mPhone.getPhoneId() + "] " + s);
        }
    }

    /**
     * @return The current GPRS state. IN_SERVICE is the same as "attached"
     * and OUT_OF_SERVICE is the same as detached.
     */
    public int getCurrentDataConnectionState() {
        return mSS.getDataRegState();
    }

    /**
     * @return true if phone is camping on a technology (eg UMTS)
     * that could support voice and data simultaneously.
     */
    public boolean isConcurrentVoiceAndDataAllowed() {
        if (mPhone.isPhoneTypeGsm()) {
            //return (mSS.getRilVoiceRadioTechnology() >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);

            //[ALPS01520958]-START:Detail HSPA PS bearer information for HSPA DC icon display
            boolean isAllowed = false;
            if (mSS.isVoiceRadioTechnologyHigher(ServiceState.RIL_RADIO_TECHNOLOGY_UMTS) ||
                mSS.getRilVoiceRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_UMTS) {
                isAllowed = true;
            }
            //[ALPS01520958]-END

            if (DBG) {
                log("isConcurrentVoiceAndDataAllowed(): " + isAllowed);
            }
            return isAllowed;
        } else if (mPhone.isPhoneTypeCdma()) {
            // Note: it needs to be confirmed which CDMA network types
            // can support voice and data calls concurrently.
            // For the time-being, the return value will be false.
            return false;
        } else {
            // Using the Conncurrent Service Supported flag for CdmaLte devices.
            /// M: For svlte concurrent voice and data allow. @{
            // here just a indicator of concurrent capability, but may be could not concurrent right
            // now, so ignore voice register state.
            if (SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1
                    && mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                return true;
            }
            /// @}
            return mSS.getCssIndicator() == 1;
        }
    }

    public void setImsRegistrationState(boolean registered) {
        log("ImsRegistrationState - registered : " + registered);

        if (mImsRegistrationOnOff && !registered) {
            if (mAlarmSwitch) {
                mImsRegistrationOnOff = registered;

                Context context = mPhone.getContext();
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.cancel(mRadioOffIntent);
                mAlarmSwitch = false;

                sendMessage(obtainMessage(EVENT_CHANGE_IMS_STATE));
                return;
            }
        }
        mImsRegistrationOnOff = registered;
    }

    public void onImsCapabilityChanged() {
        if (mPhone.isPhoneTypeGsm()) {
            sendMessage(obtainMessage(EVENT_IMS_CAPABILITY_CHANGED));
        }
    }

    private void onNetworkStateChangeResult(AsyncResult ar) {
        String info[];
        int state = -1;
        int lac = -1;
        int cid = -1;
        int Act = -1;
        int cause = -1;

        /* Note: There might not be full +CREG URC info when screen off
                   Full URC format: +CREG:  <stat>, <lac>, <cid>, <Act>,<cause> */
        if (ar.exception != null || ar.result == null) {
           loge("onNetworkStateChangeResult exception");
        } else {
            info = (String[]) ar.result;

            if (info.length > 0) {

                state = Integer.parseInt(info[0]);

                if (info[1] != null && info[1].length() > 0) {
                   lac = Integer.parseInt(info[1], 16);
                }

                if (info[2] != null && info[2].length() > 0) {
                   //TODO: fix JE (java.lang.NumberFormatException: Invalid int: "ffffffff")
                   if (info[2].equals("FFFFFFFF") || info[2].equals("ffffffff")) {
                       log("Invalid cid:" + info[2]);
                       info[2] = "0000ffff";
                   }
                   cid = Integer.parseInt(info[2], 16);
                }

                if (info[3] != null && info[3].length() > 0) {
                   Act = Integer.parseInt(info[3]);
                }

                if (info[4] != null && info[4].length() > 0) {
                   cause = Integer.parseInt(info[4]);
                }

                log("onNetworkStateChangeResult state:" + state + " lac:" + lac + " cid:" + cid
                        + " Act:" + Act + " cause:" + cause);

                // determine whether this URC comes with 4G
                if (Act == 7) voiceUrcWith4G = true;
                else voiceUrcWith4G = false;

                //ALPS00267573 CDR-ONS-245
                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    // Modem-based network loss optimization is only supported by newer version
                    // modem, so we use this feature option to distinguish new/old modem.
                    // AP-based solution here will thus be only applied to old version modem.
                    if (SystemProperties.get("ro.mtk_md_world_mode_support").equals("0")) {
                        try {
                            if (mServiceStateExt.needIgnoredState(
                                    mSS.getVoiceRegState(), state, cause) == true) {
                                //MTK-ADD START : for CS not registered , PS regsitered (ex: LTE PS
                                //only mode or 2/3G PS only SIM card or CS domain network
                                //registeration temporary failure
                                /* in case of CS not registered but PS regsitered, it will fasle
                                   alarm "CS invalid".*/
                                log("onNetworkStateChangeResult isCsInvalidCard:" +
                                        isCsInvalidCard);
                                if (!isCsInvalidCard) {
                                    if (dontUpdateNetworkStateFlag == false) {
                                        broadcastHideNetworkState("start",
                                                ServiceState.STATE_OUT_OF_SERVICE);
                                    }
                                    dontUpdateNetworkStateFlag = true;
                                } //end of if (!isCsInvalidCard)
                                return;
                            } else {
                                if (dontUpdateNetworkStateFlag == true) {
                                    broadcastHideNetworkState("stop",
                                            ServiceState.STATE_OUT_OF_SERVICE);
                                }
                                dontUpdateNetworkStateFlag = false;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                }
                /* AT+CREG? result won't include <lac>,<cid> when phone is NOT registered.
                   So we wpdate mNewCellLoc via +CREG URC when phone is not registered to network,
                   so that CellLoc can be updated when pollStateDone  */
                if ((lac != -1) && (cid != -1) && (regCodeToServiceState(state)
                        == ServiceState.STATE_OUT_OF_SERVICE)) {
                    // ignore unknown lac or cid value
                    if (lac == 0xfffe || cid == 0x0fffffff) {
                        log("unknown lac:" + lac + " or cid:" + cid);
                    } else {
                        log("mNewCellLoc Updated, lac:" + lac + " and cid:" + cid);
                        ((GsmCellLocation)mNewCellLoc).setLacAndCid(lac, cid);
                    }
                }

                if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    try {
                    // ALPS00283696 CDR-NWS-241
                        if (mServiceStateExt.needRejectCauseNotification(cause) == true) {
                            setRejectCauseNotification(cause);
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                logd("onNetworkStateChangeResult length zero");
            }
        }

        return;
    }

    public void setEverIVSR(boolean value)
    {
        log("setEverIVSR:" + value);
        mEverIVSR = value;

        /* ALPS00376525 notify IVSR start event */
        if (value == true) {
            Intent intent = new Intent(TelephonyIntents.ACTION_IVSR_NOTIFY);
            intent.putExtra(TelephonyIntents.INTENT_KEY_IVSR_ACTION, "start");
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());

            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }

            log("broadcast ACTION_IVSR_NOTIFY intent");

            mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /**
     * Return the current located PLMN string (ex: "46000") or null (ex: flight mode or no signal
     * area)
     */
    public String getLocatedPlmn() {
        return mLocatedPlmn;
    }

    private void updateLocatedPlmn(String plmn) {
        logd("updateLocatedPlmn(),previous plmn= " + mLocatedPlmn + " ,update to: " + plmn);

        if (((mLocatedPlmn == null) && (plmn != null)) ||
            ((mLocatedPlmn != null) && (plmn == null)) ||
            ((mLocatedPlmn != null) && (plmn != null) && !(mLocatedPlmn.equals(plmn)))) {
            Intent intent = new Intent(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }
            intent.putExtra(TelephonyIntents.EXTRA_PLMN, plmn);

            if (plmn != null) {
                int mcc;
                try {
                    mcc = Integer.parseInt(plmn.substring(0, 3));
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, MccTable.countryCodeForMcc(mcc));
                } catch (NumberFormatException ex) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex);
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
                } catch (StringIndexOutOfBoundsException ex) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex);
                    intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
                }
                if (SystemProperties.get(PROPERTY_AUTO_RAT_SWITCH).equals("0")) {
                    loge("updateLocatedPlmn: framework auto RAT switch disabled");
                } else {
                    mLocatedPlmn = plmn;  //[ALPS02198932]
                    setDeviceRatMode(mPhone.getPhoneId());
                }
            } else {
                intent.putExtra(TelephonyIntents.EXTRA_ISO, "");
            }

            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        mLocatedPlmn = plmn;
    }

    private void onFemtoCellInfoResult(AsyncResult ar) {
        String info[];
        int isCsgCell = 0;

        if (ar.exception != null || ar.result == null) {
           loge("onFemtoCellInfo exception");
        } else {
            info = (String[]) ar.result;

            if (info.length > 0) {

                if (info[0] != null && info[0].length() > 0) {
                    mFemtocellDomain = Integer.parseInt(info[0]);
                    log("onFemtoCellInfo: mFemtocellDomain set to " + mFemtocellDomain);
                }

                if (info[5] != null && info[5].length() > 0) {
                   isCsgCell = Integer.parseInt(info[5]);
                }

                log("onFemtoCellInfo: domain= " + mFemtocellDomain + ",isCsgCell= " + isCsgCell);

                if (isCsgCell == 1) {
                    if (info[6] != null && info[6].length() > 0) {
                        mCsgId = info[6];
                        log("onFemtoCellInfo: mCsgId set to " + mCsgId);
                    }

                    if (info[8] != null && info[8].length() > 0) {
                        mHhbName = new String(IccUtils.hexStringToBytes(info[8]));
                        log("onFemtoCellInfo: mHhbName set from " + info[8] + " to " + mHhbName);
                    } else {
                        mHhbName = null;
                        log("onFemtoCellInfo: mHhbName is not available ,set to null");
                    }
                } else {
                    mCsgId = null;
                    mHhbName = null;
                    log("onFemtoCellInfo: csgId and hnbName are cleared");
                }
                if ((info[1] != null && info[1].length() > 0)  &&
                    (info[9] != null && info[0].length() > 0)) {
                    int state = Integer.parseInt(info[1]);
                    int cause = Integer.parseInt(info[9]);
                    if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        try {
                            if (mServiceStateExt.needIgnoreFemtocellUpdate(state, cause) == true) {
                                log("needIgnoreFemtocellUpdate due to state= " + state + ",cause= "
                                    + cause);
                                // return here to prevent update variables and broadcast for CSG
                                return;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Intent intent = new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());

                if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                }

                intent.putExtra(TelephonyIntents.EXTRA_SHOW_SPN, mCurShowSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SPN, mCurSpn);
                intent.putExtra(TelephonyIntents.EXTRA_SHOW_PLMN, mCurShowPlmn);
                intent.putExtra(TelephonyIntents.EXTRA_PLMN, mCurPlmn);
                // Femtocell (CSG) info
                intent.putExtra(TelephonyIntents.EXTRA_HNB_NAME, mHhbName);
                intent.putExtra(TelephonyIntents.EXTRA_CSG_ID, mCsgId);
                intent.putExtra(TelephonyIntents.EXTRA_DOMAIN, mFemtocellDomain);

                mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);

                int phoneId = mPhone.getPhoneId();
                String plmn = mCurPlmn;
                if((mHhbName == null) && (mCsgId != null)){
                    if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        try {
                            if (mServiceStateExt.needToShowCsgId() == true) {
                                plmn += " - ";
                                plmn += mCsgId;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        plmn += " - ";
                        plmn += mCsgId;
                    }
                } else if(mHhbName != null){
                    plmn += " - ";
                    plmn += mHhbName;
                }
                boolean setResult = mSubscriptionController.setPlmnSpn(phoneId,
                        mCurShowPlmn, plmn, mCurShowSpn, mCurSpn);
                if (!setResult) {
                    mSpnUpdatePending = true;
                }
            }
        }
    }

    /* ALPS01139189 START */
    private void broadcastHideNetworkState(String action, int state) {
        if (DBG) log("broadcastHideNetworkUpdate action=" + action + " state=" + state);
        Intent intent = new Intent(TelephonyIntents.ACTION_HIDE_NETWORK_STATE);
        if (TelephonyManager.getDefault().getPhoneCount() == 1) {
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        }
        intent.putExtra(TelephonyIntents.EXTRA_ACTION, action);
        intent.putExtra(TelephonyIntents.EXTRA_REAL_SERVICE_STATE, state);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }
    /* ALPS01139189 END */

    //ALPS00248788
    private void onInvalidSimInfoReceived(AsyncResult ar) {
        String[] InvalidSimInfo = (String[]) ar.result;
        String plmn = InvalidSimInfo[0];
        int cs_invalid = Integer.parseInt(InvalidSimInfo[1]);
        int ps_invalid = Integer.parseInt(InvalidSimInfo[2]);
        int cause = Integer.parseInt(InvalidSimInfo[3]);
        int testMode = -1;

        // do NOT apply IVSR when in TEST mode
        testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
        // there is only one test mode in modem. actually it's not SIM dependent , so remove
        // testmode2 property here

        log("onInvalidSimInfoReceived testMode:" + testMode + " cause:" + cause + " cs_invalid:"
                + cs_invalid + " ps_invalid:" + ps_invalid + " plmn:" + plmn
                + " mEverIVSR:" + mEverIVSR);

        //Check UE is set to test mode or not   (CTA =1,FTA =2 , IOT=3 ...)
        if (testMode != 0) {
            log("InvalidSimInfo received during test mode: " + testMode);
            return;
        }
        if (mServiceStateExt.isNeedDisableIVSR()) {
            log("Disable IVSR");
            return;
        }
         //MTK-ADD Start : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
         //only SIM card or CS domain network registeration temporary failure
         if (cs_invalid == 1) {
             isCsInvalidCard = true;
         }
         //MTK-ADD END : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS
         //only SIM card or CS domain network registeration temporary failure

        /* check if CS domain ever sucessfully registered to the invalid SIM PLMN */
        /* Integrate ALPS00286197 with MR2 data only device state update , not to apply CS domain
           IVSR for data only device */
        if (mVoiceCapable) {
            if ((cs_invalid == 1) && (mLastRegisteredPLMN != null)
                    && (plmn.equals(mLastRegisteredPLMN))) {
                log("InvalidSimInfo reset SIM due to CS invalid");
                setEverIVSR(true);
                mLastRegisteredPLMN = null;
                mLastPSRegisteredPLMN = null;
                mCi.setSimPower(RILConstants.SIM_POWER_RESET, null);
                return;
            }
        }

        /* check if PS domain ever sucessfully registered to the invalid SIM PLMN */
        //[ALPS02261450] - start
        if ((ps_invalid == 1) && (isAllowRecoveryOnIvsr(ar)) &&
                (mLastPSRegisteredPLMN != null) && (plmn.equals(mLastPSRegisteredPLMN))){
        //if ((ps_invalid == 1) && (mLastPSRegisteredPLMN != null) &&
        //              (plmn.equals(mLastPSRegisteredPLMN)))
        //[ALPS02261450] - end
            log("InvalidSimInfo reset SIM due to PS invalid ");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            mCi.setSimPower(RILConstants.SIM_POWER_RESET, null);
            return;
        }

        /* ALPS00324111: to force trigger IVSR */
        /* ALPS00407923  : The following code is to "Force trigger IVSR even
                  when MS never register to the
                  network before"The code was intended to cover the scenario of "invalid
                  SIM NW issue happen
                  at the first network registeration during boot-up".
                  However, it might cause false alarm IVSR ex: certain sim card only register
                  CS domain network , but PS domain is invalid.
                  For such sim card, MS will receive invalid SIM at the first PS domain
                  network registeration In such case , to trigger IVSR will be a false alarm,
                  which will cause  CS domain network
                  registeration time longer (due to IVSR impact)
                  It's a tradeoff. Please think about the false alarm impact
                  before using the code below.*/
        /*
        if ((mEverIVSR == false) && (gprsState != ServiceState.STATE_IN_SERVICE)
                &&(mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE))
        {
            log("InvalidSimInfo set TRM due to never set IVSR");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            phone.setTRM(3, null);
            return;
        }
        */

    }

    private void onModulationInfoReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
           loge("onModulationInfoReceived exception");
        } else {
            int info[];
            int modulation;
            info = (int[]) ar.result;
            modulation = info[0];
            log("[onModulationInfoReceived] modulation:" + modulation);

            Intent intent = new Intent(TelephonyIntents.ACTION_NOTIFY_MODULATION_INFO);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(TelephonyIntents.EXTRA_MODULATION_INFO, modulation);

            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    //[ALPS02261450]
    private boolean isAllowRecoveryOnIvsr(AsyncResult ar) {
        if (mPhone.isInCall()){
            log("[isAllowRecoveryOnIvsr] isInCall()=true");
            Message msg;
            msg = obtainMessage();
            msg.what = EVENT_INVALID_SIM_INFO;
            msg.obj = ar;
            sendMessageDelayed(msg, POLL_PERIOD_MILLIS);
            return false;
        } else {
            log("isAllowRecoveryOnIvsr() return true");
            return true;
        }
    }

    /**
     * Post a notification to NotificationManager for network reject cause
     *
     * @param cause
     */
    private void setRejectCauseNotification(int cause) {
        if (DBG) log("setRejectCauseNotification: create notification " + cause);

        Context context = mPhone.getContext();
        mNotificationBuilder = new Notification.Builder(context);
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setAutoCancel(true);
        mNotificationBuilder.setSmallIcon(com.android.internal.R.drawable.stat_sys_warning);

        Intent intent = new Intent();
        mNotificationBuilder.setContentIntent(PendingIntent.
                getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        CharSequence details = "";
        CharSequence title = context.getText(com.mediatek.R.string.RejectCauseTitle);
        int notificationId = REJECT_NOTIFICATION;

        switch (cause) {
            case 2:
                details = context.getText(com.mediatek.R.string.MMRejectCause2);;
                break;
            case 3:
                details = context.getText(com.mediatek.R.string.MMRejectCause3);;
                break;
            case 5:
                details = context.getText(com.mediatek.R.string.MMRejectCause5);;
                break;
            case 6:
                details = context.getText(com.mediatek.R.string.MMRejectCause6);;
                break;
            case 13:
                details = context.getText(com.mediatek.R.string.MMRejectCause13);
                break;
            default:
                break;
        }

        if (DBG) log("setRejectCauseNotification: put notification " + title + " / " + details);
        mNotificationBuilder.setContentTitle(title);
        mNotificationBuilder.setContentText(details);

        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = mNotificationBuilder.build();
        notificationManager.notify(notificationId, mNotification);
    }

    /**
     * Post a notification to NotificationManager for spcial icc card type
     *
     * @param cause
     */
    //[ALPS01558804] MTK-START: send notification for using some spcial icc card
    private void setSpecialCardTypeNotification(String iccCardType, int titleType, int detailType) {
        if (DBG) log("setSpecialCardTypeNotification: create notification for " + iccCardType);

        //status notification
        Context context = mPhone.getContext();
        int notificationId = SPECIAL_CARD_TYPE_NOTIFICATION;

        mNotificationBuilder = new Notification.Builder(context);
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setAutoCancel(true);
        mNotificationBuilder.setSmallIcon(com.android.internal.R.drawable.stat_sys_warning);

        Intent intent = new Intent();
        mNotificationBuilder.setContentIntent(PendingIntent.
                getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        CharSequence title = "";
        switch (titleType) {
            case 0:
                title = context.getText(
                        com.mediatek.R.string.Special_Card_Type_Title_Lte_Not_Available);
                break;
            default:
                break;
        }

        CharSequence details = "";
        switch (detailType) {
            case 0:
                details = context.getText(com.mediatek.R.string.Suggest_To_Change_USIM);
                break;
            default:
                break;
        }

        if (DBG) log("setSpecialCardTypeNotification: put notification " + title + " / " + details);
        mNotificationBuilder.setContentTitle(title);
        mNotificationBuilder.setContentText(details);


        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotification = mNotificationBuilder.build();
        notificationManager.notify(notificationId, mNotification);
    }
    //[ALPS01558804] MTK-END: send notification for using some spcial icc card


    //MTK-START [ALPS00540036]
    private int getDstForMcc(int mcc, long when) {
        int dst = 0;

        if (mcc != 0) {
            String tzId = MccTable.defaultTimeZoneForMcc(mcc);
            if (tzId != null) {
                TimeZone timeZone = TimeZone.getTimeZone(tzId);
                Date date = new Date(when);
                boolean isInDaylightTime = timeZone.inDaylightTime(date);
                if (isInDaylightTime) {
                    dst = 1;
                    log("[NITZ] getDstForMcc: dst=" + dst);
                }
            }
        }

        return dst;
    }

    private int getMobileCountryCode() {
        int mcc = 0;

        String operatorNumeric = mSS.getOperatorNumeric();
        if (operatorNumeric != null) {
            try {
                mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
            } catch (NumberFormatException ex) {
                loge("countryCodeForMcc error" + ex);
            } catch (StringIndexOutOfBoundsException ex) {
                loge("countryCodeForMcc error" + ex);
            }
        }

        return mcc;
    }
    //MTK-END [ALPS00540036]

    //MTK-START: update TimeZone by MCC/MNC
    //Find TimeZone in manufacturer maintained table for the country has multiple timezone
    private TimeZone getTimeZonesWithCapitalCity(String iso) {
        TimeZone tz = null;

        //[ALPS01666276]-Start: don't udpate with capital city when we has received nitz before
        if ((mZoneOffset == 0) && (mZoneDst == false)) {
            for (int i = 0; i < mTimeZoneIdOfCapitalCity.length; i++) {
                if (iso.equals(mTimeZoneIdOfCapitalCity[i][0])) {
                    tz = TimeZone.getTimeZone(mTimeZoneIdOfCapitalCity[i][1]);
                    log("uses TimeZone of Capital City:" + mTimeZoneIdOfCapitalCity[i][1]);
                    break;
                }
            }
        } else {
            log("don't udpate with capital city, cause we have received nitz");
        }
        //[ALPS01666276]-End
        return tz;
    }

    // For the case that MccTable.defaultTimeZoneForMcc() returns unexpected timezone
    private String getTimeZonesByMcc(String mcc) {
        String tz = null;

        for (int i = 0; i < mTimeZoneIdByMcc.length; i++) {
            if (mcc.equals(mTimeZoneIdByMcc[i][0])) {
                tz = mTimeZoneIdByMcc[i][1];
                log("uses Timezone of GsmSST by mcc: " + mTimeZoneIdByMcc[i][1]);
                break;
            }
        }
        return tz;
    }

    //MTK-Add-start : [ALPS01267367] fix timezone by MCC
    protected void fixTimeZone() {
        TimeZone zone = null;
        String iso = "";
        String operatorNumeric = mSS.getOperatorNumeric();
        String mcc = null;

        //[ALPS01416062] MTK ADD-START
        if (operatorNumeric != null && !operatorNumeric.equals("") && isNumeric(operatorNumeric)) {
        //if (operatorNumeric != null) {
        //[ALPS01416062] MTK ADD-END
            mcc = operatorNumeric.substring(0, 3);
        } else {
            log("fixTimeZone but not registered and operatorNumeric is null or invalid value");
            return;
        }

        try {
            iso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
        } catch (NumberFormatException ex) {
            loge("fixTimeZone countryCodeForMcc error" + ex);
        }

        if (!mcc.equals("000") && !TextUtils.isEmpty(iso) && getAutoTimeZone()) {

            // Test both paths if ignore nitz is true
            boolean testOneUniqueOffsetPath = SystemProperties.getBoolean(
                        TelephonyProperties.PROPERTY_IGNORE_NITZ, false) &&
                            ((SystemClock.uptimeMillis() & 1) == 0);

            ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(iso);
            if ((uniqueZones.size() == 1) || testOneUniqueOffsetPath) {
                zone = uniqueZones.get(0);
                if (DBG) {
                   log("fixTimeZone: no nitz but one TZ for iso-cc=" + iso +
                           " with zone.getID=" + zone.getID() +
                           " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                }
                setAndBroadcastNetworkSetTimeZone(zone.getID());
            //MTK-START: [ALPS01262709] update time with MCC/MNC
            //} else {
            } else if (uniqueZones.size() > 1) {
                log("uniqueZones.size=" + uniqueZones.size());
                zone = getTimeZonesWithCapitalCity(iso);
                //[ALPS01666276]-Start: don't udpate with capital city when we has received nitz
                //before
                if (zone != null) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
                //[ALPS01666276]-End
            //MTK-END: [ALPS01262709] update time with MCC/MNC
            } else {
                if (DBG) {
                    log("fixTimeZone: there are " + uniqueZones.size() +
                        " unique offsets for iso-cc='" + iso +
                        " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath +
                        "', do nothing");
                }
            }
        }

        if (zone != null) {
            log("fixTimeZone: zone != null zone.getID=" + zone.getID());
            if (getAutoTimeZone()) {
                setAndBroadcastNetworkSetTimeZone(zone.getID());
            }
            saveNitzTimeZone(zone.getID());
        } else {
            log("fixTimeZone: zone == null");
        }
    }
    //[ALPS01416062] MTK ADD-START
    public boolean isNumeric(String str) {
        //[ALPS01565135] MTK ADD -START for avoide JE on Pattern.Matcher
        //Pattern pattern = Pattern.compile("[0-9]*");
        //Matcher isNum = pattern.matcher(str);
        //if(!isNum.matches()) {
        //    return false;
        //}
        //return true;

        try {
            int testNum = Integer.parseInt(str);
        } catch (NumberFormatException eNFE) {
            log("isNumeric:" + eNFE.toString());
            return false;
        } catch (Exception e) {
            log("isNumeric:" + e.toString());
            return false;
        }
        return true;
        //[ALPS01565135] MTK ADD -END
    }
    //[ALPS01416062] MTK ADD-END

    //MTK-END:  [ALPS01262709]  update TimeZone by MCC/MNC

    public class timerTask extends TimerTask {
        public void run() {
            log("CellInfo Timeout invoke getAllCellInfoByRate()");
            if ((mCellInfoRate != Integer.MAX_VALUE) && (mCellInfoRate != 0)
                    && (mCellInfoTimer != null)) {
                log("timerTask schedule timer with period = " + mCellInfoRate + " ms");
                mCellInfoTimer.schedule(new timerTask(), mCellInfoRate);
            }

            new Thread(new Runnable() {
                public void run() {
                    log("timerTask invoke getAllCellInfoByRate() in another thread");
                    getAllCellInfoByRate();
                }
            }).start();

        }
    };

    //MTK-START [ALPS01830723]

    private void onPsNetworkStateChangeResult(AsyncResult ar) {
        int info[];
        int newUrcState;

        if (ar.exception != null || ar.result == null) {
           loge("onPsNetworkStateChangeResult exception");
        } else {
            info = (int[]) ar.result;
            newUrcState = regCodeToServiceState(info[0]);
            log("mPsRegState:" + mPsRegState + ",new:" + newUrcState + ",result:" + info[0]);
            //get the raw state value for roaming
            mPsRegStateRaw = info[0];

            if (mPsRegState == ServiceState.STATE_IN_SERVICE
                       && newUrcState != ServiceState.STATE_IN_SERVICE) {
                log("set flag for ever detach, may notify attach later");
                bHasDetachedDuringPolling = true;
            }
        }
    }

    private void handlePsRegNotification(int oldState, int newState) {

        boolean hasGprsAttached = false;
        boolean hasGprsDetached = false;
        boolean specificNotify = false;

        log("old:" + oldState + " ,mPsRegState:" + mPsRegState + ",new:" + newState);

        // Compare oldState and mPsRegState
        hasGprsAttached =
                oldState != ServiceState.STATE_IN_SERVICE
                && mPsRegState == ServiceState.STATE_IN_SERVICE;

        hasGprsDetached =
                oldState == ServiceState.STATE_IN_SERVICE
                && mPsRegState != ServiceState.STATE_IN_SERVICE;

        if (hasGprsAttached) {
            mAttachedRegistrants.notifyRegistrants();
            mLastPSRegisteredPLMN = mSS.getOperatorNumeric() ;
            log("mLastPSRegisteredPLMN= " + mLastPSRegisteredPLMN);
            bHasDetachedDuringPolling = false;
        }

        if (hasGprsDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        // Compare mPsRegState and newState
        hasGprsAttached =
                mPsRegState != ServiceState.STATE_IN_SERVICE
                && newState == ServiceState.STATE_IN_SERVICE;

        hasGprsDetached =
                mPsRegState == ServiceState.STATE_IN_SERVICE
                && newState != ServiceState.STATE_IN_SERVICE;


        if (!hasGprsAttached &&
            bHasDetachedDuringPolling && newState == ServiceState.STATE_IN_SERVICE) {
            // M: It means:   attached -> (detached) -> attached, need to compensate for notifying
            // this modification is for "network losing enhancement"
            specificNotify = true;
            log("need to compensate for notifying");
        }

        if (hasGprsAttached || specificNotify) {
            mAttachedRegistrants.notifyRegistrants();
            mLastPSRegisteredPLMN = mSS.getOperatorNumeric() ;
            log("mLastPSRegisteredPLMN= " + mLastPSRegisteredPLMN);
        }

        if (hasGprsDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        mPsRegState = newState;
        bHasDetachedDuringPolling = false; // reset flag
    }
    //MTK-END [ALPS01830723]

    //MTK-START [ALPS00368272]
    private void getEINFO(int eventId) {
        mPhone.invokeOemRilRequestStrings(new String[]{"AT+EINFO?", "+EINFO"},
                this.obtainMessage(eventId));
        log("getEINFO for EMMRRS");
    }

    private void setEINFO(int value, Message onComplete) {
        String Cmd[] = new String[2];
        Cmd[0] = "AT+EINFO=" + value;
        Cmd[1] = "+EINFO";
        mPhone.invokeOemRilRequestStrings(Cmd, onComplete);
        log("setEINFO for EMMRRS, ATCmd[0]=" + Cmd[0]);
    }

    private boolean isCurrentPhoneDataConnectionOn() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean userDataEnabled = true;

        try {
            userDataEnabled = TelephonyManager.getIntWithSubId(
                    mPhone.getContext().getContentResolver(),
                    Settings.Global.MOBILE_DATA, defaultDataSubId) == 1;
        } catch (Settings.SettingNotFoundException snfe) {
            if (DBG) log("isCurrentPhoneDataConnectionOn: SettingNofFoundException snfe=" + snfe);
        }
        log("userDataEnabled=" + userDataEnabled + ", defaultDataSubId=" + defaultDataSubId);
        if (userDataEnabled && (defaultDataSubId
                == SubscriptionManager.getSubIdUsingPhoneId(mPhone.getPhoneId()))) {
            return true;
        }
        return false;
    }
    //MTK-END[ALPS00368272]

    //[ALPS01804936]-start:fix JE when change system language to "Burmese"
    protected int updateOperatorAlpha(String operatorAlphaLong) {
        int myPhoneId = mPhone.getPhoneId();
        if (myPhoneId == PhoneConstants.SIM_ID_1) {
            SystemProperties.set(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, operatorAlphaLong);
        } else if (myPhoneId == PhoneConstants.SIM_ID_2) {
            SystemProperties.set(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2, operatorAlphaLong);
        } else if (myPhoneId == PhoneConstants.SIM_ID_3) {
            SystemProperties.set(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_3, operatorAlphaLong);
        } else if (myPhoneId == PhoneConstants.SIM_ID_4) {
            SystemProperties.set(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_4, operatorAlphaLong);
        }
        return 1;
    }
    //[ALPS01804936]-end

    //[ALPS01810775,ALPS01868743] -Start: update network type at screen off
    private void updateNetworkInfo(int newRegState, int newNetworkType) {
        int displayState = mCi.getDisplayState();

        boolean isRegisted = false;
        if ((newRegState == ServiceState.REGISTRATION_STATE_HOME_NETWORK) ||
                (newRegState == ServiceState.REGISTRATION_STATE_ROAMING)) {
            isRegisted = true;
        } else {
            isRegisted = false;
        }

        //Case1: update network type with new type.
        //
        //       situation 1): The format of CREG is long format when screen is on.
        //       situation 2): mIsForceSendScreenOnForUpdateNwInfo is ture
        //                         means we forec changed format to long at last time.
        //       situation 3): not camp on network when screen is off
        //
        //Case2: change format to update cid , lac and network type
        //       when camp on network after screen off.
        //
        //Case3: update network type with old type.
        //       screen is off and registered before screen off

        if ((displayState != Display.STATE_OFF) ||
                mIsForceSendScreenOnForUpdateNwInfo ||
                ((!isRegisted) && (displayState == Display.STATE_OFF))) {
            mNewSS.setRilVoiceRadioTechnology(newNetworkType);
        } else if ((mSS.getVoiceRegState()
                        == ServiceState.STATE_OUT_OF_SERVICE) &&
                        (isRegisted) && (displayState == Display.STATE_OFF)) {
            if (!mIsForceSendScreenOnForUpdateNwInfo) {
                log("send screen state ON to change format of CREG");
                mIsForceSendScreenOnForUpdateNwInfo = true;
                mCi.sendScreenState(true);
                pollState();
            }
        } else if ((displayState == Display.STATE_OFF) && isRegisted) {
            mNewSS.setRilVoiceRadioTechnology(mSS.getRilVoiceRadioTechnology());
            log("set Voice network type=" + mNewSS.getRilVoiceRadioTechnology() +
                    " update network type with old type.");
        }
    }
    //[ALPS01810775,ALPS01868743] -End

    public boolean isSameRadioTechnologyMode(int nRadioTechnology1, int nRadioTechnology2) {
        if ((nRadioTechnology1 == ServiceState.RIL_RADIO_TECHNOLOGY_LTE &&
                nRadioTechnology2 == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) ||
                (nRadioTechnology1 == ServiceState.RIL_RADIO_TECHNOLOGY_GSM &&
                nRadioTechnology2 == ServiceState.RIL_RADIO_TECHNOLOGY_GSM)) {
            return true;
        } else if (((nRadioTechnology1 >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS &&
                        nRadioTechnology1 <= ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD) ||
                        nRadioTechnology1 == ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP) &&
                        ((nRadioTechnology2 >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS &&
                        nRadioTechnology2 <= ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD) ||
                        nRadioTechnology2 == ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP)) {
            return true;
        }
        return false;
    }

    private void setReceivedNitz(int phoneId, boolean receivedNitz) {
        log("setReceivedNitz : phoneId = " + phoneId);
        sReceiveNitz[phoneId] = receivedNitz;
    }

    private boolean getReceivedNitz() {
        return sReceiveNitz[mPhone.getPhoneId()];
    }

    private void onNetworkEventReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
           loge("onNetworkEventReceived exception");
        } else {
            // result[0]: <Act> not used
            // result[1]: <event_type> 0: for RAU event , 1: for TAU event
            int nwEventType = ((int[]) ar.result)[1];
            log("[onNetworkEventReceived] event_type:" + nwEventType);

            Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_EVENT);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(TelephonyIntents.EXTRA_EVENT_TYPE, nwEventType + 1);

            mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }



    /**
     * A complete "service state" from our perspective is
     * composed of a handful of separate requests to the radio.
     *
     * We make all of these requests at once, but then abandon them
     * and start over again if the radio notifies us that some
     * event has changed
     */
    public void pollState() {
        pollState(false);
    }
    /**
     * We insist on polling even if the radio says its off.
     * Used when we get a network changed notification
     * but the radio is off - part of iwlan hack
     */
    private void modemTriggeredPollState() {
        pollState(true);
    }

    public void pollState(boolean modemTriggered) {

        //[ALPS01577029]-START:To support auto switch rat mode to 2G only for 3M TDD csfb project
        //when we are not in china
        int currentNetworkMode = getPreferredNetworkModeSettings(mPhone.getPhoneId());
        //[ALPS01577029]-END

        log("pollState RadioState is " + mCi.getRadioState() + ", currentNetworkMode= "
                + currentNetworkMode);

        mPollingContext = new int[1];
        mPollingContext[0] = 0;

        if (mPhone.isPhoneTypeGsm()) {
            //[ALPS01996342]
            if (dontUpdateNetworkStateFlag == true) {
                log("pollState is ignored!!");
                return;
            }
        }

        switch (mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:

                //M: MTK added for [ALPS01802701]
                if (!mPhone.isPhoneTypeCdmaLte()) {
                    // mNewSS.setStateOutOfService();
                    mNewSS.setStateOff();
                } else {
                    mNewSS.setStateOutOfService();
                }
                //M: MTK added end

                mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mGotCountryCode = false;
                mNitzUpdatedTime = false;
                if (mPhone.isPhoneTypeGsm()) {
                    //M: MTK added
                    setNullState();
                    mPsRegStateRaw = ServiceState.RIL_REG_STATE_NOT_REG;
                    //M: MTK added end
                }
                pollStateDone();
                break;

            case RADIO_OFF:
                mNewSS.setStateOff();
                mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mGotCountryCode = false;
                mNitzUpdatedTime = false;
                if (mPhone.isPhoneTypeGsm()) {
                    //M: MTK added
                    setNullState();
                    //M: MTK added end
                }
                // don't poll for state when the radio is off
                // EXCEPT, if the poll was modemTrigged (they sent us new radio data)
                // or we're on IWLAN
                if (!modemTriggered && ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                        != mSS.getRilDataRadioTechnology()
                    //M: MTK added
                    && ServiceState.STATE_IN_SERVICE
                        != regCodeToServiceState(mPsRegStateRaw)) {
                    mPsRegStateRaw = ServiceState.RIL_REG_STATE_NOT_REG;
                    //M: MTK added end
                    pollStateDone();
                    break;
                }

                /// M: [CDMA] @{
                if (mPhone.isPhoneTypeCdma()) {
                    break;
                }
                /// @}

            default:
                // Issue all poll-related commands at once then count down the responses, which
                // are allowed to arrive out-of-order
                mPollingContext[0]++;
                mCi.getOperator(obtainMessage(EVENT_POLL_STATE_OPERATOR, mPollingContext));

                mPollingContext[0]++;
                mCi.getDataRegistrationState(obtainMessage(EVENT_POLL_STATE_GPRS, mPollingContext));

                mPollingContext[0]++;
                mCi.getVoiceRegistrationState(obtainMessage(EVENT_POLL_STATE_REGISTRATION,
                        mPollingContext));

                if (mPhone.isPhoneTypeGsm()) {
                    mPollingContext[0]++;
                    mCi.getNetworkSelectionMode(obtainMessage(
                            EVENT_POLL_STATE_NETWORK_SELECTION_MODE, mPollingContext));
                }
                break;
        }
    }

    //todo: try to merge pollstate functions
    private void pollStateDone() {
        if (mPhone.isPhoneTypeGsm()) {
            pollStateDoneGsm();
        } else if (mPhone.isPhoneTypeCdma()) {
            pollStateDoneCdma();
        } else {
            pollStateDoneCdmaLte();
        }
    }

    // use to share iso, mcc between pollStateDoneGsm, fixTimeZone;
    private String iso = "";
    private String mcc = "";
    private void pollStateDoneGsm() {
        iso = "";
        mcc = "";

        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            mNewSS.setVoiceRoaming(true);
            mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();

        //if (DBG) {
            log("Poll ServiceState done: " +
                    " oldSS=[" + mSS + "] newSS=[" + mNewSS + "]" +
                    " oldMaxDataCalls=" + mMaxDataCalls +
                    " mNewMaxDataCalls=" + mNewMaxDataCalls +
                    " oldReasonDataDenied=" + mReasonDataDenied +
                    " mNewReasonDataDenied=" + mNewReasonDataDenied);
        //}

        //[ALPS01664312]-Add:Start
        //change format to update cid , lac and network type when camp on network after screen off
        if (mIsForceSendScreenOnForUpdateNwInfo) {
            log("send screen state OFF to restore format of CREG");
            mIsForceSendScreenOnForUpdateNwInfo = false;

            //[ALPS01810775,ALPS01868743] -Start: update network type at screen off
            //if (!mIsScreenOn) {
            if (mCi.getDisplayState() == Display.STATE_OFF) {
                mCi.sendScreenState(false);
            }
        }
        //[ALPS01664312]-Add:end

        boolean hasRegistered =
                mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered =
                mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasGprsAttached =
                mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasGprsDetached =
                mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasDataRegStateChanged =
                mSS.getDataRegState() != mNewSS.getDataRegState();

        boolean hasVoiceRegStateChanged =
                mSS.getVoiceRegState() != mNewSS.getVoiceRegState();

        //[ALPS01507528]-START:udpate Sim Indicate State when +CREG:<state> is changed
        boolean hasRilVoiceRegStateChanged =
                mSS.getRilVoiceRegState() != mNewSS.getRilVoiceRegState();
        //[ALPS01507528]-END

        boolean hasRilVoiceRadioTechnologyChanged =
                mSS.getRilVoiceRadioTechnology() != mNewSS.getRilVoiceRadioTechnology();

        boolean hasRilDataRadioTechnologyChanged =
                mSS.getRilDataRadioTechnology() != mNewSS.getRilDataRadioTechnology();

        ///M: Fix the operator info not update issue.
        boolean hasChanged = !mNewSS.equals(mSS) || mNeedNotify;

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        boolean hasDataRoamingOn = !mSS.getDataRoaming() && mNewSS.getDataRoaming();

        boolean hasDataRoamingOff = mSS.getDataRoaming() && !mNewSS.getDataRoaming();

        boolean hasLocationChanged = !mNewCellLoc.equals(mCellLoc);

        boolean hasLacChanged =
                ((GsmCellLocation)mNewCellLoc).getLac() != ((GsmCellLocation)mCellLoc).getLac();

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        log("pollStateDone,hasRegistered:" + hasRegistered + ",hasDeregistered:" + hasDeregistered
                + ",hasGprsAttached:" + hasGprsAttached
                + ",hasRilVoiceRadioTechnologyChanged:" + hasRilVoiceRadioTechnologyChanged
                + ",hasRilDataRadioTechnologyChanged:" + hasRilDataRadioTechnologyChanged
                + ",hasVoiceRegStateChanged:" + hasVoiceRegStateChanged + ",hasDataRegStateChanged:"
                + hasDataRegStateChanged + ",hasChanged:" + hasChanged + ",hasVoiceRoamingOn:"
                + hasVoiceRoamingOn + ",hasVoiceRoamingOff:" + hasVoiceRoamingOff
                + ",hasDataRoamingOn:" + hasDataRoamingOn + ",hasDataRoamingOff:"
                + hasDataRoamingOff + ",hasLocationChanged:" + hasLocationChanged
                + ",hasLacChanged:" + hasLacChanged
                + ",sReceiveNitz:" + getReceivedNitz());

        // Add an event log when connection state changes
        if (hasVoiceRegStateChanged || hasDataRegStateChanged) {
            EventLog.writeEvent(EventLogTags.GSM_SERVICE_STATE_CHANGE,
                    mSS.getVoiceRegState(), mSS.getDataRegState(),
                    mNewSS.getVoiceRegState(), mNewSS.getDataRegState());
        }

        // Add an event log when network type switched
        // TODO: we may add filtering to reduce the event logged,
        // i.e. check preferred network setting, only switch to 2G, etc
        if (hasRilVoiceRadioTechnologyChanged) {
            int cid = -1;
            GsmCellLocation loc = (GsmCellLocation)mNewCellLoc;
            if (loc != null) cid = loc.getCid();
            // NOTE: this code was previously located after mSS and mNewSS are swapped, so
            // existing logs were incorrectly using the new state for "network_from"
            // and STATE_OUT_OF_SERVICE for "network_to". To avoid confusion, use a new log tag
            // to record the correct states.
            EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, cid,
                    mSS.getRilVoiceRadioTechnology(),
                    mNewSS.getRilVoiceRadioTechnology());
            if (DBG) {
                log("RAT switched "
                        + ServiceState.rilRadioTechnologyToString(mSS.getRilVoiceRadioTechnology())
                        + " -> "
                        + ServiceState.rilRadioTechnologyToString(
                        mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
            }
        }

        // swap mSS and mNewSS to put new state in mSS
        ServiceState tss = mSS;
        mSS = mNewSS;
        mNewSS = tss;
        // clean slate for next time
        ////mNewSS.setStateOutOfService();

        // swap mCellLoc and mNewCellLoc to put new state in mCellLoc
        GsmCellLocation tcl = (GsmCellLocation)mCellLoc;
        mCellLoc = mNewCellLoc;
        mNewCellLoc = tcl;

        mReasonDataDenied = mNewReasonDataDenied;
        mMaxDataCalls = mNewMaxDataCalls;

        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), mSS.getRilDataRadioTechnology());

            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();

            mLastRegisteredPLMN = mSS.getOperatorNumeric() ;
            log("mLastRegisteredPLMN= " + mLastRegisteredPLMN);

            if (DBG) {
                log("pollStateDone: registering current mNitzUpdatedTime=" +
                        mNitzUpdatedTime + " changing to false");
            }
            mNitzUpdatedTime = false;
        }


        if (explict_update_spn == 1)
        {
             /* ALPS00273961 :Screen on, modem explictly send CREG URC , but still not able to
                update screen due to hasChanged is false
                In this case , we update SPN display by explict_update_spn */
             if (!hasChanged)
             {
                 log("explict_update_spn trigger to refresh SPN");
                 updateSpnDisplay();
             }
             explict_update_spn = 0;
        }

        if (hasChanged) {
            String operatorNumeric;

            updateSpnDisplay();

            ///M: Fix the operator info not update issue.
            mNeedNotify = false;
            //[ALPS01804936]-start:fix JE when change system language to "Burmese"
            // tm.setNetworkOperatorNameForPhone(mPhone.getPhoneId(), mSS.getOperatorAlphaLong());
            //updateOperatorAlpha(mSS.getOperatorAlphaLong());  //remark for [ALPS01965792]
            //[ALPS01804936]-end

            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(mPhone.getPhoneId());
            operatorNumeric = mSS.getOperatorNumeric();
            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric,
                    prevOperatorNumeric, mPhone.getContext());
            //[ALPS01416062] MTK ADD-START
            if ((operatorNumeric != null) && (!isNumeric(operatorNumeric))) {
                if (DBG) log("operatorNumeric is Invalid value, don't update timezone");
            } else if (TextUtils.isEmpty(operatorNumeric)) {
                if (DBG) log("operatorNumeric is null");
                updateCarrierMccMncConfiguration(operatorNumeric,
                    prevOperatorNumeric, mPhone.getContext());
                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), "");
                mGotCountryCode = false;
                mNitzUpdatedTime = false;
            } else {
                try{
                    mcc = operatorNumeric.substring(0, 3);
                    iso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
                } catch ( NumberFormatException ex){
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch ( StringIndexOutOfBoundsException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                }

                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), iso);
                mGotCountryCode = true;

                TimeZone zone = null;

                if (!mNitzUpdatedTime && !mcc.equals("000") && !TextUtils.isEmpty(iso) &&
                        getAutoTimeZone()) {

                    // Test both paths if ignore nitz is true
                    boolean testOneUniqueOffsetPath = SystemProperties.getBoolean(
                            TelephonyProperties.PROPERTY_IGNORE_NITZ, false) &&
                            ((SystemClock.uptimeMillis() & 1) == 0);

                    ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(iso);
                    if ((uniqueZones.size() == 1) || testOneUniqueOffsetPath) {
                        zone = uniqueZones.get(0);
                        if (DBG) {
                            log("pollStateDone: no nitz but one TZ for iso-cc=" + iso +
                                    " with zone.getID=" + zone.getID() +
                                    " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                        }
                        setAndBroadcastNetworkSetTimeZone(zone.getID());
                    //MTK-START: [ALPS01262709] update time with MCC/MNC
                    } else if (uniqueZones.size() > 1) {
                        log("uniqueZones.size=" + uniqueZones.size() + " iso= " + iso);
                        zone = getTimeZonesWithCapitalCity(iso);
                        if (zone != null) {
                            setAndBroadcastNetworkSetTimeZone(zone.getID());
                        } else {
                            log("Can't find time zone for capital city");
                        }
                    //MTK-END: [ALPS01262709] update time with MCC/MNC
                    } else {
                        if (DBG) {
                            log("pollStateDone: there are " + uniqueZones.size() +
                                    " unique offsets for iso-cc='" + iso +
                                    " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath +
                                    "', do nothing");
                        }
                    }
                }

                if (shouldFixTimeZoneNow(mPhone, operatorNumeric, prevOperatorNumeric,
                        mNeedFixZoneAfterNitz)) {
                    fixTimeZone(iso);
                }
            }

            tm.setNetworkRoamingForPhone(mPhone.getPhoneId(), mSS.getVoiceRoaming());

            setRoamingType(mSS);
            log("Broadcasting ServiceState : " + mSS);
            mPhone.notifyServiceStateChanged(mSS);

            mEventLog.writeServiceStateChanged(mSS);
        }

        if (hasGprsAttached) {
            mAttachedRegistrants.notifyRegistrants();
            mLastPSRegisteredPLMN = mSS.getOperatorNumeric() ;
            log("mLastPSRegisteredPLMN= " + mLastPSRegisteredPLMN);
        }

        if (hasGprsDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();

            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                mPhone.notifyDataConnection(Phone.REASON_IWLAN_AVAILABLE);
            } else {
                mPhone.notifyDataConnection(null);
            }
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();

        } else if (((mNewSS.getRilDataRegState() == ServiceState.REGISTRATION_STATE_HOME_NETWORK &&
                    (mSS.getRilDataRegState() == ServiceState.REGISTRATION_STATE_HOME_NETWORK ||
                    mSS.getRilDataRegState() == ServiceState.REGISTRATION_STATE_ROAMING)) ||
                    (mSS.getRilDataRegState() == ServiceState.REGISTRATION_STATE_ROAMING &&
                    mDataRoaming == false)) &&
                    mPsRegStateRaw == ServiceState.RIL_REG_STATE_ROAMING) {
            //Consider
            //1. From home plmn -> roaming URC -> home URC -> home plmnand -> recover setup data
            //2. From home plmn -> roaming URC -> domestic roam -> home plmn -> recover setup data
            //3. From home(domestic) plmn -> roaming URC -> home(domestic) plmn -> recover setup
            //   data
            log("recover setup data for roaming off. OldDataRegState:"
            + mNewSS.getRilDataRegState() + " NewDataRegState:" + mSS.getRilDataRegState() +
            " NewRoamingState:" + mSS.getRoaming() + " NewDataRoamingState:" + mDataRoaming +
            " PsRegState:" + mPsRegStateRaw);

            mPsRegStateRaw = ServiceState.RIL_REG_STATE_HOME;
            if (!mSS.getRoaming()) {
                mDataRoamingOffRegistrants.notifyRegistrants();
            }
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged();
        }

        if (!isGprsConsistent(mSS.getDataRegState(), mSS.getVoiceRegState())) {
            if (!mStartedGprsRegCheck && !mReportedGprsNoReg) {
                mStartedGprsRegCheck = true;

                int check_period = Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        Settings.Global.GPRS_REGISTER_CHECK_PERIOD_MS,
                        DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
                sendMessageDelayed(obtainMessage(EVENT_CHECK_REPORT_GPRS),
                        check_period);
            }
        } else {
            mReportedGprsNoReg = false;
        }
    }

    protected void pollStateDoneCdma() {
        updateRoamingState();

        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        if (DBG) log("pollStateDone: cdma oldSS=[" + mSS + "] newSS=[" + mNewSS + "]");

        boolean hasRegistered =
                mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionAttached =
                mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionDetached =
                mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionChanged =
                mSS.getDataRegState() != mNewSS.getDataRegState();

        boolean hasRilVoiceRadioTechnologyChanged =
                mSS.getRilVoiceRadioTechnology() != mNewSS.getRilVoiceRadioTechnology();

        boolean hasRilDataRadioTechnologyChanged =
                mSS.getRilDataRadioTechnology() != mNewSS.getRilDataRadioTechnology();

        boolean hasChanged = !mNewSS.equals(mSS);

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        boolean hasDataRoamingOn = !mSS.getDataRoaming() && mNewSS.getDataRoaming();

        boolean hasDataRoamingOff = mSS.getDataRoaming() && !mNewSS.getDataRoaming();

        boolean hasLocationChanged = !mNewCellLoc.equals(mCellLoc);

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        // Add an event log when connection state changes
        if (mSS.getVoiceRegState() != mNewSS.getVoiceRegState() ||
                mSS.getDataRegState() != mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE,
                    mSS.getVoiceRegState(), mSS.getDataRegState(),
                    mNewSS.getVoiceRegState(), mNewSS.getDataRegState());
        }

        ServiceState tss;
        tss = mSS;
        mSS = mNewSS;
        mNewSS = tss;
        // clean slate for next time
        mNewSS.setStateOutOfService();

        CdmaCellLocation tcl = (CdmaCellLocation)mCellLoc;
        mCellLoc = mNewCellLoc;
        mNewCellLoc = tcl;

        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), mSS.getRilDataRadioTechnology());

            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
        }

        if (hasChanged) {
            if ((mCi.getRadioState().isOn()) && (!mIsSubscriptionFromRuim)) {
                String eriText;
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used for
                    // mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext().getText(
                            com.android.internal.R.string.roamingTextSearching).toString();
                }
                mSS.setOperatorAlphaLong(eriText);
            }

            String operatorNumeric;

            tm.setNetworkOperatorNameForPhone(mPhone.getPhoneId(), mSS.getOperatorAlphaLong());

            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(mPhone.getPhoneId());
            operatorNumeric = mSS.getOperatorNumeric();

            // try to fix the invalid Operator Numeric
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }

            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric,
                    prevOperatorNumeric, mPhone.getContext());

            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) log("operatorNumeric "+ operatorNumeric +"is invalid");
                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), "");
                mGotCountryCode = false;
            } else {
                String isoCountryCode = "";
                String mcc = operatorNumeric.substring(0, 3);
                try{
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(
                            operatorNumeric.substring(0, 3)));
                } catch ( NumberFormatException ex){
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch ( StringIndexOutOfBoundsException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                }

                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), isoCountryCode);
                mGotCountryCode = true;

                setOperatorIdd(operatorNumeric);

                if (shouldFixTimeZoneNow(mPhone, operatorNumeric, prevOperatorNumeric,
                        mNeedFixZoneAfterNitz)) {
                    fixTimeZone(isoCountryCode);
                }
            }

            tm.setNetworkRoamingForPhone(mPhone.getPhoneId(),
                    (mSS.getVoiceRoaming() || mSS.getDataRoaming()));

            updateSpnDisplay();
            // set roaming type
            setRoamingType(mSS);
            log("Broadcasting ServiceState : " + mSS);
            mPhone.notifyServiceStateChanged(mSS);
        }

        if (hasCdmaDataConnectionAttached) {
            mAttachedRegistrants.notifyRegistrants();
        }

        if (hasCdmaDataConnectionDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        if (hasCdmaDataConnectionChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                mPhone.notifyDataConnection(Phone.REASON_IWLAN_AVAILABLE);
            } else {
                mPhone.notifyDataConnection(null);
            }
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged();
        }
        // TODO: Add CdmaCellIdenity updating, see CdmaLteServiceStateTracker.
    }

    protected void pollStateDoneCdmaLte() {
        updateRoamingState();

        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            mNewSS.setVoiceRoaming(true);
            mNewSS.setDataRoaming(true);
        }

        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        log("pollStateDone: lte 1 ss=[" + mSS + "] newSS=[" + mNewSS + "]");

        boolean hasRegistered = mSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE
                && mNewSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered = mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE
                && mNewSS.getVoiceRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionAttached =
                mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionDetached =
                mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE
                        && mNewSS.getDataRegState() != ServiceState.STATE_IN_SERVICE;

        boolean hasCdmaDataConnectionChanged =
                mSS.getDataRegState() != mNewSS.getDataRegState();

        boolean hasVoiceRadioTechnologyChanged = mSS.getRilVoiceRadioTechnology()
                != mNewSS.getRilVoiceRadioTechnology();

        boolean hasDataRadioTechnologyChanged = mSS.getRilDataRadioTechnology()
                != mNewSS.getRilDataRadioTechnology();

        boolean hasChanged = !mNewSS.equals(mSS);

        boolean hasVoiceRoamingOn = !mSS.getVoiceRoaming() && mNewSS.getVoiceRoaming();

        boolean hasVoiceRoamingOff = mSS.getVoiceRoaming() && !mNewSS.getVoiceRoaming();

        boolean hasDataRoamingOn = !mSS.getDataRoaming() && mNewSS.getDataRoaming();

        boolean hasDataRoamingOff = mSS.getDataRoaming() && !mNewSS.getDataRoaming();

        boolean hasLocationChanged = !mNewCellLoc.equals(mCellLoc);

        boolean has4gHandoff =
                mNewSS.getDataRegState() == ServiceState.STATE_IN_SERVICE &&
                        (((mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) &&
                                (mNewSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)) ||
                                ((mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD) &&
                                        (mNewSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE)));

        boolean hasMultiApnSupport =
                (((mNewSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) ||
                        (mNewSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)) &&
                        ((mSS.getRilDataRadioTechnology() != ServiceState.RIL_RADIO_TECHNOLOGY_LTE) &&
                                (mSS.getRilDataRadioTechnology() != ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD)));

        boolean hasLostMultiApnSupport =
                ((mNewSS.getRilDataRadioTechnology() >= ServiceState.RIL_RADIO_TECHNOLOGY_IS95A) &&
                        (mNewSS.getRilDataRadioTechnology() <= ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A));

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (DBG) {
            log("pollStateDone:"
                    + " hasRegistered=" + hasRegistered
                    + " hasDeegistered=" + hasDeregistered
                    + " hasCdmaDataConnectionAttached=" + hasCdmaDataConnectionAttached
                    + " hasCdmaDataConnectionDetached=" + hasCdmaDataConnectionDetached
                    + " hasCdmaDataConnectionChanged=" + hasCdmaDataConnectionChanged
                    + " hasVoiceRadioTechnologyChanged= " + hasVoiceRadioTechnologyChanged
                    + " hasDataRadioTechnologyChanged=" + hasDataRadioTechnologyChanged
                    + " hasChanged=" + hasChanged
                    + " hasVoiceRoamingOn=" + hasVoiceRoamingOn
                    + " hasVoiceRoamingOff=" + hasVoiceRoamingOff
                    + " hasDataRoamingOn=" + hasDataRoamingOn
                    + " hasDataRoamingOff=" + hasDataRoamingOff
                    + " hasLocationChanged=" + hasLocationChanged
                    + " has4gHandoff = " + has4gHandoff
                    + " hasMultiApnSupport=" + hasMultiApnSupport
                    + " hasLostMultiApnSupport=" + hasLostMultiApnSupport);
        }
        // Add an event log when connection state changes
        if (mSS.getVoiceRegState() != mNewSS.getVoiceRegState()
                || mSS.getDataRegState() != mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE, mSS.getVoiceRegState(),
                    mSS.getDataRegState(), mNewSS.getVoiceRegState(), mNewSS.getDataRegState());
        }

        /// M: [CDMALTE] If RAT group changed between 3GPP and 3GPP2, we need to update
        // signal strenth(isGsm value) because MD won't report CSQ URC if no changes.@{
        final int oldRilDataRadioTechnology = mSS.getRilDataRadioTechnology();
        /// @}

        ServiceState tss;
        tss = mSS;
        mSS = mNewSS;
        mNewSS = tss;
        // clean slate for next time
        mNewSS.setStateOutOfService();

        CdmaCellLocation tcl = (CdmaCellLocation)mCellLoc;
        mCellLoc = mNewCellLoc;
        mNewCellLoc = tcl;

        mNewSS.setStateOutOfService(); // clean slate for next time

        if (hasVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }

        if (hasDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(mPhone.getPhoneId(), mSS.getRilDataRadioTechnology());

            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }

            /// M: [CDMALTE] If RAT group changed between 3GPP and 3GPP2, we need to update
            // signal strenth(isGsm value) because MD won't report CSQ URC if no changes.@{
            if (oldRilDataRadioTechnology == ServiceState.RIL_RADIO_TECHNOLOGY_LTE
                    || mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                log("[CDMALTE]pollStateDone: update signal for RAT switch between diff group");
                sendMessage(obtainMessage(EVENT_POLL_SIGNAL_STRENGTH));
            }
            /// @}
        }

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
        }

        if (hasChanged) {
            boolean hasBrandOverride = mUiccController.getUiccCard(getPhoneId()) == null ? false :
                    (mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() != null);
            if (!hasBrandOverride && (mCi.getRadioState().isOn()) && (mPhone.isEriFileLoaded()) &&
                    (mSS.getRilVoiceRadioTechnology() != ServiceState.RIL_RADIO_TECHNOLOGY_LTE ||
                            mPhone.getContext().getResources().getBoolean(com.android.internal.R.
                                    bool.config_LTE_eri_for_network_name)) &&
                                    (!mIsSubscriptionFromRuim)) { /// M: [CDMALTE]
                // Only when CDMA is in service, ERI will take effect
                String eriText = mSS.getOperatorAlphaLong();
                // Now the Phone sees the new ServiceState so it can get the new ERI text
                if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
                    eriText = mPhone.getCdmaEriText();
                } else if (mSS.getVoiceRegState() == ServiceState.STATE_POWER_OFF) {
                    eriText = (mIccRecords != null) ? mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(eriText)) {
                        // Sets operator alpha property by retrieving from
                        // build-time system property
                        eriText = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE) {
                    // Note that ServiceState.STATE_OUT_OF_SERVICE is valid used
                    // for mRegistrationState 0,2,3 and 4
                    eriText = mPhone.getContext()
                            .getText(com.android.internal.R.string.roamingTextSearching).toString();
                }
                mSS.setOperatorAlphaLong(eriText);
            }

            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY &&
                    mIccRecords != null && (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE)
                    && mSS.getRilVoiceRadioTechnology() != ServiceState.RIL_RADIO_TECHNOLOGY_LTE) {
                // SIM is found on the device. If ERI roaming is OFF, and SID/NID matches
                // one configured in SIM, use operator name from CSIM record. Note that ERI, SID,
                // and NID are CDMA only, not applicable to LTE.
                boolean showSpn =
                        ((RuimRecords)mIccRecords).getCsimSpnDisplayCondition();
                int iconIndex = mSS.getCdmaEriIconIndex();

                if (showSpn && (iconIndex == EriInfo.ROAMING_INDICATOR_OFF) &&
                        isInHomeSidNid(mSS.getSystemId(), mSS.getNetworkId()) &&
                        mIccRecords != null) {
                    /// M: [CDMALTE] if the SPN is null from CSIM, will try other applications. @{
                    if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                        try {
                            if (mServiceStateExt.allowSpnDisplayed()) {
                                String rltSpn = mIccRecords.getServiceProviderName();
                                if (rltSpn == null) {
                                    UiccCardApplication newUiccApplication =
                                            mUiccController.getUiccCardApplication(
                                                    mPhone.getPhoneId(),
                                                    UiccController.APP_FAM_3GPP);
                                    if (newUiccApplication != null) {
                                        rltSpn = newUiccApplication.getIccRecords()
                                                .getServiceProviderName();
                                    }
                                }
                                log("[CDMALTE] rltSpn:" + rltSpn);
                                mSS.setOperatorAlphaLong(rltSpn);
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                    /// @}
                }
            }

            String operatorNumeric;

            tm.setNetworkOperatorNameForPhone(mPhone.getPhoneId(), mSS.getOperatorAlphaLong());

            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(mPhone.getPhoneId());
            operatorNumeric = mSS.getOperatorNumeric();
            // try to fix the invalid Operator Numeric
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }
            tm.setNetworkOperatorNumericForPhone(mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric,
                    prevOperatorNumeric, mPhone.getContext());

            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) log("operatorNumeric is null");
                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), "");
                mGotCountryCode = false;
            } else {
                String isoCountryCode = "";
                String mcc = operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric
                            .substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex) {
                    loge("countryCodeForMcc error" + ex);
                }

                tm.setNetworkCountryIsoForPhone(mPhone.getPhoneId(), isoCountryCode);
                mGotCountryCode = true;

                setOperatorIdd(operatorNumeric);

                if (shouldFixTimeZoneNow(mPhone, operatorNumeric, prevOperatorNumeric,
                        mNeedFixZoneAfterNitz)) {
                    fixTimeZone(isoCountryCode);
                }
            }

            tm.setNetworkRoamingForPhone(mPhone.getPhoneId(),
                    (mSS.getVoiceRoaming() || mSS.getDataRoaming()));

            updateSpnDisplay();
            setRoamingType(mSS);
            log("Broadcasting ServiceState : " + mSS);
            mPhone.notifyServiceStateChanged(mSS);
        }

        if (hasCdmaDataConnectionAttached || has4gHandoff) {
            mAttachedRegistrants.notifyRegistrants();
        }

        if (hasCdmaDataConnectionDetached) {
            mDetachedRegistrants.notifyRegistrants();
        }

        if ((hasCdmaDataConnectionChanged || hasDataRadioTechnologyChanged)) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                    == mSS.getRilDataRadioTechnology()) {
                mPhone.notifyDataConnection(Phone.REASON_IWLAN_AVAILABLE);
            } else {
                mPhone.notifyDataConnection(null);
            }
        }

        if (hasVoiceRoamingOn) {
            mVoiceRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasVoiceRoamingOff) {
            mVoiceRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOn) {
            mDataRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasDataRoamingOff) {
            mDataRoamingOffRegistrants.notifyRegistrants();
        }

        if (hasLocationChanged) {
            mPhone.notifyLocationChanged();
        }
    }

    /**
     * Check whether the specified SID and NID pair appears in the HOME SID/NID list
     * read from NV or SIM.
     *
     * @return true if provided sid/nid pair belongs to operator's home network.
     */
    private boolean isInHomeSidNid(int sid, int nid) {
        // if SID/NID is not available, assume this is home network.
        if (isSidsAllZeros()) return true;

        // length of SID/NID shold be same
        if (mHomeSystemId.length != mHomeNetworkId.length) return true;

        if (sid == 0) return true;

        for (int i = 0; i < mHomeSystemId.length; i++) {
            // Use SID only if NID is a reserved value.
            // SID 0 and NID 0 and 65535 are reserved. (C.0005 2.6.5.2)
            if ((mHomeSystemId[i] == sid) &&
                    ((mHomeNetworkId[i] == 0) || (mHomeNetworkId[i] == 65535) ||
                            (nid == 0) || (nid == 65535) || (mHomeNetworkId[i] == nid))) {
                return true;
            }
        }
        // SID/NID are not in the list. So device is not in home network
        return false;
    }

    protected void setOperatorIdd(String operatorNumeric) {
        // Retrieve the current country information
        // with the MCC got from opeatorNumeric.
        /// M: Use try catch to avoid Integer pars exception @{
        String idd = "";
        try {
            idd = mHbpcdUtils.getIddByMcc(
                Integer.parseInt(operatorNumeric.substring(0,3)));
        } catch (NumberFormatException ex) {
            loge("setOperatorIdd: idd error" + ex);
        } catch (StringIndexOutOfBoundsException ex) {
            loge("setOperatorIdd: idd error" + ex);
        }
        /// @}
        if (idd != null && !idd.isEmpty()) {
            mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING,
                    idd);
        } else {
            // use default "+", since we don't know the current IDP
            mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_IDP_STRING, "+");
        }
    }

    protected boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return operatorNumeric == null || operatorNumeric.length() < 5 ||
                operatorNumeric.startsWith(INVALID_MCC);
    }

    protected String fixUnknownMcc(String operatorNumeric, int sid) {
        if (sid <= 0) {
            // no cdma information is available, do nothing
            return operatorNumeric;
        }

        // resolve the mcc from sid;
        // if mSavedTimeZone is null, TimeZone would get the default timeZone,
        // and the fixTimeZone couldn't help, because it depends on operator Numeric;
        // if the sid is conflict and timezone is unavailable, the mcc may be not right.
        boolean isNitzTimeZone = false;
        int timeZone = 0;
        TimeZone tzone = null;
        if (mSavedTimeZone != null) {
            timeZone =
                    TimeZone.getTimeZone(mSavedTimeZone).getRawOffset()/MS_PER_HOUR;
            isNitzTimeZone = true;
        } else {
            tzone = getNitzTimeZone(mZoneOffset, mZoneDst, mZoneTime);
            if (tzone != null)
                timeZone = tzone.getRawOffset()/MS_PER_HOUR;
        }

        int mcc = mHbpcdUtils.getMcc(sid,
                timeZone, (mZoneDst ? 1 : 0), isNitzTimeZone);
        if (mcc > 0) {
            operatorNumeric = Integer.toString(mcc) + DEFAULT_MNC;
        }
        return operatorNumeric;
    }

    protected void fixTimeZone(String isoCountryCode) {
        TimeZone zone = null;
        // If the offset is (0, false) and the time zone property
        // is set, use the time zone property rather than GMT.
        String zoneName = SystemProperties.get(TIMEZONE_PROPERTY);
        if (DBG) {
            log("fixTimeZone zoneName='" + zoneName +
                    "' mZoneOffset=" + mZoneOffset + " mZoneDst=" + mZoneDst +
                    " iso-cc='" + isoCountryCode +
                    "' iso-cc-idx=" + Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode));
        }
        if ("".equals(isoCountryCode) && mNeedFixZoneAfterNitz) {
            // Country code not found.  This is likely a test network.
            // Get a TimeZone based only on the NITZ parameters (best guess).
            zone = getNitzTimeZone(mZoneOffset, mZoneDst, mZoneTime);
            if (DBG) log("pollStateDone: using NITZ TimeZone");
        } else if ((mZoneOffset == 0) && (mZoneDst == false) && (zoneName != null)
                && (zoneName.length() > 0)
                && (Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode) < 0)) {
            // For NITZ string without time zone,
            // need adjust time to reflect default time zone setting
            zone = TimeZone.getDefault();
            if (mPhone.isPhoneTypeGsm()) {
                //MTK-ADD-Start: [ALPS01262709] try ot fix timezone by MCC
                //[ALPS01825832] fix timezone by MCC only if we don't recevice NITZ before
                if (isAllowFixTimeZone()) {
                    try {
                        String mccTz = getTimeZonesByMcc(mcc);
                        mccTz = (mccTz == null) ?
                            MccTable.defaultTimeZoneForMcc(Integer.parseInt(mcc)) : mccTz;
                        if (mccTz != null) {
                            zone = TimeZone.getTimeZone(mccTz);
                            if (DBG) log("pollStateDone: try to fixTimeZone mcc:" + mcc
                                    + " mccTz:" + mccTz + " zone.getID=" + zone.getID());
                        }
                    } catch (Exception e) {
                        log("pollStateDone: parse error: mcc=" + mcc);
                    }
                }
                //MTK-ADD-END: [ALPS01262709] try ot fix timezone by MCC
            }

            if (mNeedFixZoneAfterNitz) {
                long ctm = System.currentTimeMillis();
                long tzOffset = zone.getOffset(ctm);
                if (DBG) {
                    log("fixTimeZone: tzOffset=" + tzOffset +
                            " ltod=" + TimeUtils.logTimeOfDay(ctm));
                }
                if (getAutoTime()) {
                    long adj = ctm - tzOffset;
                    if (DBG) log("fixTimeZone: adj ltod=" + TimeUtils.logTimeOfDay(adj));
                    setAndBroadcastNetworkSetTime(adj);
                } else {
                    // Adjust the saved NITZ time to account for tzOffset.
                    mSavedTime = mSavedTime - tzOffset;
                    if (DBG) log("fixTimeZone: adj mSavedTime=" + mSavedTime);
                }
            }
            if (DBG) log("fixTimeZone: using default TimeZone");
        } else {
            zone = TimeUtils.getTimeZone(mZoneOffset, mZoneDst, mZoneTime, isoCountryCode);
            if (DBG) log("fixTimeZone: using getTimeZone(off, dst, time, iso)");
        }

        mNeedFixZoneAfterNitz = false;

        if (zone != null) {
            log("fixTimeZone: zone != null zone.getID=" + zone.getID());
            if (getAutoTimeZone()) {
                setAndBroadcastNetworkSetTimeZone(zone.getID());
            } else {
                log("fixTimeZone: skip changing zone as getAutoTimeZone was false");
            }
            saveNitzTimeZone(zone.getID());

            /// M: [CDMA] Save NITZ timezone ID in system propery for CDMA SMS.@{
            if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
                TelephonyManager.setTelephonyProperty(
                        mPhone.getPhoneId(), IPlusCodeUtils.PROPERTY_NITZ_TIME_ZONE_ID,
                        zone.getID());
            }
            /// @}
        } else {
            log("fixTimeZone: zone == null, do nothing for zone");
        }
    }

    /**
     * Check if GPRS got registered while voice is registered.
     *
     * @param dataRegState i.e. CGREG in GSM
     * @param voiceRegState i.e. CREG in GSM
     * @return false if device only register to voice but not gprs
     */
    private boolean isGprsConsistent(int dataRegState, int voiceRegState) {
        return !((voiceRegState == ServiceState.STATE_IN_SERVICE) &&
                (dataRegState != ServiceState.STATE_IN_SERVICE));
    }

    /**
     * Returns a TimeZone object based only on parameters from the NITZ string.
     */
    private TimeZone getNitzTimeZone(int offset, boolean dst, long when) {
        TimeZone guess = findTimeZone(offset, dst, when);
        if (guess == null) {
            // Couldn't find a proper timezone.  Perhaps the DST data is wrong.
            guess = findTimeZone(offset, !dst, when);
        }
        if (DBG) log("getNitzTimeZone returning " + (guess == null ? guess : guess.getID()));
        return guess;
    }

    private TimeZone findTimeZone(int offset, boolean dst, long when) {
        log("[NITZ],findTimeZone,offset:" + offset + ",dst:" + dst + ",when:" + when);
        int rawOffset = offset;
        if (dst) {
            rawOffset -= MS_PER_HOUR;
        }
        String[] zones = TimeZone.getAvailableIDs(rawOffset);
        TimeZone guess = null;
        Date d = new Date(when);
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            if (tz.getOffset(when) == offset &&
                    tz.inDaylightTime(d) == dst) {
                guess = tz;
                log("[NITZ],find time zone.");
                break;
            }
        }

        return guess;
    }

    /** code is registration state 0-5 from TS 27.007 7.2 */
    private int regCodeToServiceState(int code) {
        switch (code) {
            case 0:
            case 2: // 2 is "searching"
            case 3: // 3 is "registration denied"
            case 4: // 4 is "unknown" no vaild in current baseband
            case 10:// same as 0, but indicates that emergency call is possible.
            case 12:// same as 2, but indicates that emergency call is possible.
            case 13:// same as 3, but indicates that emergency call is possible.
            case 14:// same as 4, but indicates that emergency call is possible.
                return ServiceState.STATE_OUT_OF_SERVICE;

            case 1:
            case 5: // 5 is "registered, roaming"
                return ServiceState.STATE_IN_SERVICE;

            default:
                loge("regCodeToServiceState: unexpected service state " + code);
                return ServiceState.STATE_OUT_OF_SERVICE;
        }
    }

    /** code is registration state 0-5 from TS 27.007 7.2 */
    private int regCodeToRegState(int code) {
        switch (code) {
            case 10:// same as 0, but indicates that emergency call is possible.
                return ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING;
            case 12:// same as 2, but indicates that emergency call is possible.
                return ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING;
            case 13:// same as 3, but indicates that emergency call is possible.
                return ServiceState.REGISTRATION_STATE_REGISTRATION_DENIED;
            case 14:// same as 4, but indicates that emergency call is possible.
                return ServiceState.REGISTRATION_STATE_UNKNOWN;
            default:
                return code;
        }
    }

    private String getSIMOperatorNumeric() {
        IccRecords r = mIccRecords;
        String mccmnc;
        String imsi;

        if (r != null) {
            mccmnc = r.getOperatorNumeric();

            //M: [ALPS01591758]Try to get HPLMN from IMSI (getOperatorNumeric might response null
            //due to mnc length is not available yet)
            if (mccmnc == null) {
                imsi = r.getIMSI();
                if (imsi != null && !imsi.equals("")) {
                    mccmnc = imsi.substring(0, 5);
                    log("get MCC/MNC from IMSI = " + mccmnc);
                }
            }
            return mccmnc;
        } else {
            return null;
        }
    }

    /**
     * code is registration state 0-5 from TS 27.007 7.2
     * returns true if registered roam, false otherwise
     */
    private boolean regCodeIsRoaming (int code) {
        if (mPhone.isPhoneTypeGsm()) {
            //M: MTK added
            boolean isRoaming = false;
            String strHomePlmn = getSIMOperatorNumeric();
            String strServingPlmn = mNewSS.getOperatorNumeric();
            boolean isServingPlmnInGroup = false;
            boolean isHomePlmnInGroup = false;
            boolean ignoreDomesticRoaming = false;

            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                String simType = PhoneFactory.getPhone(mPhone.getPhoneId())
                        .getIccCard().getIccCardType();
                try {
                    if ((strServingPlmn != null)
                            && (strHomePlmn != null)
                            && ((simType != null) && (!simType.equals("")) && simType
                                    .equals("CSIM"))
                            && mServiceStateExt.isRoamingForSpecialSIM(strServingPlmn,
                            strHomePlmn)) {
                        return true;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            if (ServiceState.RIL_REG_STATE_ROAMING == code) {
                isRoaming = true;
            }

            /* ALPS00296372 */
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    ignoreDomesticRoaming = mServiceStateExt.ignoreDomesticRoaming();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            if ((ignoreDomesticRoaming == true) && (isRoaming == true)
                    && (strServingPlmn != null) && (strHomePlmn != null)) {
                log("ServingPlmn = " + strServingPlmn + " HomePlmn = " + strHomePlmn);
                if (strHomePlmn.substring(0, 3).equals(strServingPlmn.substring(0, 3))) {
                    log("Same MCC,don't set as roaming");
                    isRoaming = false;
                }
            }

            /* ALPS00236452: check manufacturer maintained table for specific operator with
               multiple home PLMN id */
            if ((isRoaming == true) && (strServingPlmn != null) && (strHomePlmn != null)) {
                log("strServingPlmn = " + strServingPlmn + " strHomePlmn = " + strHomePlmn);

                for (int i = 0; i < customEhplmn.length; i++) {
                    //reset flag
                    isServingPlmnInGroup = false;
                    isHomePlmnInGroup = false;

                    //check if serving plmn or home plmn in this group
                    for (int j = 0; j < customEhplmn[i].length; j++) {
                        if (strServingPlmn.equals(customEhplmn[i][j])) {
                            isServingPlmnInGroup = true;
                        }
                        if (strHomePlmn.equals(customEhplmn[i][j])) {
                            isHomePlmnInGroup = true;
                        }
                    }

                    //if serving plmn and home plmn both in the same group
                    //, do NOT treat it as roaming
                    if ((isServingPlmnInGroup == true) && (isHomePlmnInGroup == true)) {
                        isRoaming = false;
                        log("Ignore roaming");
                        break;
                    }
                }
            }

            return isRoaming;
            // M : MTK added end
        } else {
            return ServiceState.RIL_REG_STATE_ROAMING == code;
        }
    }

    private boolean isSameOperatorNameFromSimAndSS(ServiceState s) {
        String spn = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNameForPhone(getPhoneId());

        // NOTE: in case of RUIM we should completely ignore the ERI data file and
        // mOperatorAlphaLong is set from RIL_REQUEST_OPERATOR response 0 (alpha ONS)
        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();

        if (VDBG) log("isSameNamedOperators(): onsl=" + onsl + ",onss=" + onss + ",spn=" + spn);

        boolean equalsOnsl = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onsl);
        boolean equalsOnss = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onss);

        return (equalsOnsl || equalsOnss);
    }

    /**
     * Set roaming state if operator mcc is the same as sim mcc
     * and ons is not different from spn
     *
     * @param s ServiceState hold current ons
     * @return true if same operator
     */
    private boolean isSameNamedOperators(ServiceState s) {
        return currentMccEqualsSimMcc(s) && isSameOperatorNameFromSimAndSS(s);
    }

    /**
     * Compare SIM MCC with Operator MCC
     *
     * @param s ServiceState hold current ons
     * @return true if both are same
     */
    private boolean currentMccEqualsSimMcc(ServiceState s) {
        String simNumeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(getPhoneId());
        String operatorNumeric = s.getOperatorNumeric();
        boolean equalsMcc = true;

        try {
            equalsMcc = simNumeric.substring(0, 3).
                    equals(operatorNumeric.substring(0, 3));
            if (VDBG) log("currentMccEqualsSimMcc(): equalsMcc=" + equalsMcc + ",simNumeric="
                    + simNumeric + ",operatorNumeric=" + operatorNumeric);
        } catch (Exception e){
        }
        return equalsMcc;
    }

    /**
     * Do not set roaming state in case of oprators considered non-roaming.
     *
     * Can use mcc or mcc+mnc as item of config_operatorConsideredNonRoaming.
     * For example, 302 or 21407. If mcc or mcc+mnc match with operator,
     * don't set roaming state.
     *
     * @param s ServiceState hold current ons
     * @return false for roaming state set
     */
    private boolean isOperatorConsideredNonRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.config_operatorConsideredNonRoaming);

        if (VDBG) log("isOperatorConsideredNonRoaming operatorNumeric= " + operatorNumeric
                + ",legnth= " + numericArray.length);

        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }

        for (String numeric : numericArray) {
            if (VDBG) log("isOperatorConsideredNonRoaming numeric= " + numeric);
            if (operatorNumeric.startsWith(numeric)) {
                if (VDBG) log("isOperatorConsideredNonRoaming return true");
                return true;
            }
        }
        return false;
    }

    /** M:[ALPS02503235]add operator considered roaming configures @{ */
    private boolean isOperatorConsideredRoamingMtk(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String simOperatorNumeric = ((TelephonyManager) mPhone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE))
                .getSimOperatorNumericForPhone(getPhoneId());

        if (VDBG)
            log("isOperatorConsideredRoamingMtk operatorNumeric= "
                    + operatorNumeric + ",legnth= "
                    + customOperatorConsiderRoamingMcc.length);

        if (customOperatorConsiderRoamingMcc.length == 0
                || TextUtils.isEmpty(operatorNumeric)
                || TextUtils.isEmpty(simOperatorNumeric)) {
            return false;
        }

        for (String[] numerics : customOperatorConsiderRoamingMcc) {
            if (simOperatorNumeric.startsWith(numerics[0])) {
                for (int idx = 1; idx < numerics.length; idx++) {
                    if (VDBG) {
                        log("isOperatorConsideredRoamingMtk numeric= "
                                + numerics[idx]);
                    }
                    if (operatorNumeric.startsWith(numerics[idx])) {
                        if (VDBG) {
                            log("isOperatorConsideredRoamingMtk return true");
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    /** @} */


    private boolean isOperatorConsideredRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.config_sameNamedOperatorConsideredRoaming);

        if (VDBG) log("isOperatorConsideredRoaming operatorNumeric= " + operatorNumeric
                + ",legnth= " + numericArray.length);

        if (numericArray.length == 0 || operatorNumeric == null) {
            return false;
        }

        for (String numeric : numericArray) {
            if (VDBG) log("isOperatorConsideredRoaming numeric= " + numeric);
            if (operatorNumeric.startsWith(numeric)) {
                if (VDBG) log("isOperatorConsideredRoaming return true");
                return true;
            }
        }
        return false;
    }

    /**
     * Set restricted state based on the OnRestrictedStateChanged notification
     * If any voice or packet restricted state changes, trigger a UI
     * notification and notify registrants when sim is ready.
     *
     * @param ar an int value of RIL_RESTRICTED_STATE_*
     */
    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();

        if (DBG) log("onRestrictedStateChanged: E rs "+ mRestrictedState);

        if (ar.exception == null) {
            int[] ints = (int[])ar.result;
            int state = ints[0];

            newRs.setCsEmergencyRestricted(
                    ((state & RILConstants.RIL_RESTRICTED_STATE_CS_EMERGENCY) != 0) ||
                            ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
            //ignore the normal call and data restricted state before SIM READY
            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY) {
                newRs.setCsNormalRestricted(
                        ((state & RILConstants.RIL_RESTRICTED_STATE_CS_NORMAL) != 0) ||
                                ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
                newRs.setPsRestricted(
                        (state & RILConstants.RIL_RESTRICTED_STATE_PS_ALL)!= 0);
            } else if (mPhone.isPhoneTypeGsm()) {
                log("IccCard state Not ready ");
                if (mRestrictedState.isCsNormalRestricted() &&
                    ((state & RILConstants.RIL_RESTRICTED_STATE_CS_NORMAL) == 0 &&
                    (state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) == 0)) {
                        newRs.setCsNormalRestricted(false);
                }

                if (mRestrictedState.isPsRestricted()
                        && ((state & RILConstants.RIL_RESTRICTED_STATE_PS_ALL) == 0)) {
                    newRs.setPsRestricted(false);
                }
            }

            if (DBG) log("onRestrictedStateChanged: new rs "+ newRs);

            if (!mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(PS_ENABLED);
            } else if (mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                if (mPhone.isPhoneTypeGsm()) {
                    // MTK
                    if (mPollingContext[0] != 0) {
                        mPendingPsRestrictDisabledNotify = true;
                    } else {
                        mPsRestrictDisabledRegistrants.notifyRegistrants();
                        setNotification(PS_DISABLED);
                    }
                } else {
                    // AOSP
                    mPsRestrictDisabledRegistrants.notifyRegistrants();
                    setNotification(PS_DISABLED);
                }
            }

            /**
             * There are two kind of cs restriction, normal and emergency. So
             * there are 4 x 4 combinations in current and new restricted states
             * and we only need to notify when state is changed.
             */
            if (mRestrictedState.isCsRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (!newRs.isCsNormalRestricted()) {
                    // remove normal restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    // remove emergency restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (mRestrictedState.isCsEmergencyRestricted() &&
                    !mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // remove emergency restriction and enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (!mRestrictedState.isCsEmergencyRestricted() &&
                    mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // remove normal restriction and enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                }
            } else {
                if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            }

            mRestrictedState = newRs;
        }
        log("onRestrictedStateChanged: X rs "+ mRestrictedState);
    }

    /**
     * @return the current cell location information. Prefer Gsm location
     * information if available otherwise return LTE location information
     */
    public CellLocation getCellLocation() {
        if (((GsmCellLocation)mCellLoc).getLac() >= 0 &&
                ((GsmCellLocation)mCellLoc).getCid() >= 0) {
            if (DBG) log("getCellLocation(): X good mCellLoc=" + mCellLoc);
            return mCellLoc;
        } else {
            List<CellInfo> result = getAllCellInfo();
            if (result != null) {
                // A hack to allow tunneling of LTE information via GsmCellLocation
                // so that older Network Location Providers can return some information
                // on LTE only networks, see bug 9228974.
                //
                // We'll search the return CellInfo array preferring GSM/WCDMA
                // data, but if there is none we'll tunnel the first LTE information
                // in the list.
                //
                // The tunnel'd LTE information is returned as follows:
                //   LAC = TAC field
                //   CID = CI field
                //   PSC = 0.
                GsmCellLocation cellLocOther = new GsmCellLocation();
                for (CellInfo ci : result) {
                    if (ci instanceof CellInfoGsm) {
                        CellInfoGsm cellInfoGsm = (CellInfoGsm)ci;
                        CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
                        cellLocOther.setLacAndCid(cellIdentityGsm.getLac(),
                                cellIdentityGsm.getCid());
                        cellLocOther.setPsc(cellIdentityGsm.getPsc());
                        if (DBG) log("getCellLocation(): X ret GSM info=" + cellLocOther);
                        return cellLocOther;
                    } else if (ci instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma)ci;
                        CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                        cellLocOther.setLacAndCid(cellIdentityWcdma.getLac(),
                                cellIdentityWcdma.getCid());
                        cellLocOther.setPsc(cellIdentityWcdma.getPsc());
                        if (DBG) log("getCellLocation(): X ret WCDMA info=" + cellLocOther);
                        return cellLocOther;
                    } else if ((ci instanceof CellInfoLte) &&
                            ((cellLocOther.getLac() < 0) || (cellLocOther.getCid() < 0))) {
                        // We'll return the first good LTE info we get if there is no better answer
                        CellInfoLte cellInfoLte = (CellInfoLte)ci;
                        CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                        if ((cellIdentityLte.getTac() != Integer.MAX_VALUE)
                                && (cellIdentityLte.getCi() != Integer.MAX_VALUE)) {
                            cellLocOther.setLacAndCid(cellIdentityLte.getTac(),
                                    cellIdentityLte.getCi());
                            cellLocOther.setPsc(0);
                            if (DBG) {
                                log("getCellLocation(): possible LTE cellLocOther=" + cellLocOther);
                            }
                        }
                    }
                }
                if (DBG) {
                    log("getCellLocation(): X ret best answer cellLocOther=" + cellLocOther);
                }
                return cellLocOther;
            } else {
                if (DBG) {
                    log("getCellLocation(): X empty mCellLoc and CellInfo mCellLoc=" + mCellLoc);
                }
                return mCellLoc;
            }
        }
    }

    /**
     * nitzReceiveTime is time_t that the NITZ time was posted
     */
    private void setTimeFromNITZString (String nitz, long nitzReceiveTime) {
        // "yy/mm/dd,hh:mm:ss(+/-)tz"
        // tz is in number of quarter-hours

        long start = SystemClock.elapsedRealtime();
        if (DBG) {log("NITZ: " + nitz + "," + nitzReceiveTime +
                " start=" + start + " delay=" + (start - nitzReceiveTime));
        }

        /// M: [CDMA] @{
        if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
            if (nitz.length() <= 0) {
                return;
            }
        }
        /// @}

        try {
            /* NITZ time (hour:min:sec) will be in UTC but it supplies the timezone
             * offset as well (which we won't worry about until later) */
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            c.clear();
            c.set(Calendar.DST_OFFSET, 0);

            String[] nitzSubs = nitz.split("[/:,+-]");

            int year = 2000 + Integer.parseInt(nitzSubs[0]);
            if (year > MAX_NITZ_YEAR) {
                if (DBG) loge("NITZ year: " + year + " exceeds limit, skip NITZ time update");
                return;
            }
            c.set(Calendar.YEAR, year);

            // month is 0 based!
            int month = Integer.parseInt(nitzSubs[1]) - 1;
            c.set(Calendar.MONTH, month);

            int date = Integer.parseInt(nitzSubs[2]);
            c.set(Calendar.DATE, date);

            int hour = Integer.parseInt(nitzSubs[3]);
            c.set(Calendar.HOUR, hour);

            int minute = Integer.parseInt(nitzSubs[4]);
            c.set(Calendar.MINUTE, minute);

            int second = Integer.parseInt(nitzSubs[5]);
            c.set(Calendar.SECOND, second);

            boolean sign = (nitz.indexOf('-') == -1);

            int tzOffset = Integer.parseInt(nitzSubs[6]);

            int dst = (nitzSubs.length >= 8 ) ? Integer.parseInt(nitzSubs[7]) : 0;

            /// M: [CDMA] Save NITZ timezone ID in system propery for CDMA SMS.@{
            if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
                final int ltmoffset = (sign ? 1 : -1) * tzOffset;
                if (DBG) {
                    log("[CDMA] NITZ: year = " + year + ", month = " + month + ", date = " + date
                            + ", hour = " + hour + ", minute = " + minute + ", second = " + second
                            + ", tzOffset = " + tzOffset + ", ltmoffset = " + ltmoffset
                            + ", dst = " + dst);
                }
                TelephonyManager.setTelephonyProperty(
                        mPhone.getPhoneId(), IPlusCodeUtils.PROPERTY_TIME_LTMOFFSET,
                        Integer.toString(ltmoffset));
            }
            /// @}

            //MTK-START [ALPS00540036]
            if (mPhone.isPhoneTypeGsm()) {
                dst = (nitzSubs.length >= 8 ) ? Integer.parseInt(nitzSubs[7])
                        : getDstForMcc(getMobileCountryCode(), c.getTimeInMillis());

                //int dst = (nitzSubs.length >= 8 ) ? Integer.parseInt(nitzSubs[7])
                //                                  : 0;
            }
            //MTK-END [ALPS00540036]

            // The zone offset received from NITZ is for current local time,
            // so DST correction is already applied.  Don't add it again.
            //
            // tzOffset += dst * 4;
            //
            // We could unapply it if we wanted the raw offset.

            tzOffset = (sign ? 1 : -1) * tzOffset * 15 * 60 * 1000;

            TimeZone    zone = null;

            // As a special extension, the Android emulator appends the name of
            // the host computer's timezone to the nitz string. this is zoneinfo
            // timezone name of the form Area!Location or Area!Location!SubLocation
            // so we need to convert the ! into /
            if (nitzSubs.length >= 9) {
                String  tzname = nitzSubs[8].replace('!','/');
                zone = TimeZone.getTimeZone( tzname );
                log("[NITZ] setTimeFromNITZString,tzname:" + tzname + " zone:" + zone);
            }

            String iso = ((TelephonyManager) mPhone.getContext().
                    getSystemService(Context.TELEPHONY_SERVICE)).
                    getNetworkCountryIsoForPhone(mPhone.getPhoneId());
            log("[NITZ] setTimeFromNITZString,mGotCountryCode:" + mGotCountryCode);

            if (zone == null) {

                if (mGotCountryCode) {
                    if (iso != null && iso.length() > 0) {
                        zone = TimeUtils.getTimeZone(tzOffset, dst != 0,
                                c.getTimeInMillis(),
                                iso);
                    } else {
                        // We don't have a valid iso country code.  This is
                        // most likely because we're on a test network that's
                        // using a bogus MCC (eg, "001"), so get a TimeZone
                        // based only on the NITZ parameters.
                        zone = getNitzTimeZone(tzOffset, (dst != 0), c.getTimeInMillis());
                    }
                }
            }

            if ((zone == null) || (mZoneOffset != tzOffset) || (mZoneDst != (dst != 0))){
                // We got the time before the country or the zone has changed
                // so we don't know how to identify the DST rules yet.  Save
                // the information and hope to fix it up later.

                mNeedFixZoneAfterNitz = true;
                mZoneOffset  = tzOffset;
                mZoneDst     = dst != 0;
                mZoneTime    = c.getTimeInMillis();

                if (mPhone.isPhoneTypeGsm()) {
                    //[ALPS01825832] set flag when receive NITZ
                    setReceivedNitz(mPhone.getPhoneId(), true);
                }
            }
            if (DBG) {
                log("NITZ: tzOffset=" + tzOffset + " dst=" + dst + " zone=" +
                        (zone!=null ? zone.getID() : "NULL") +
                        " iso=" + iso + " mGotCountryCode=" + mGotCountryCode +
                        " mNeedFixZoneAfterNitz=" + mNeedFixZoneAfterNitz);
            }

            if (zone != null) {
                if (getAutoTimeZone()) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
                saveNitzTimeZone(zone.getID());

                /// M: [CDMA] Save NITZ timezone ID in system propery for CDMA SMS.@{
                if (mPhone.isPhoneTypeCdma() || mPhone.isPhoneTypeCdmaLte()) {
                    TelephonyManager.setTelephonyProperty(
                            mPhone.getPhoneId(), IPlusCodeUtils.PROPERTY_NITZ_TIME_ZONE_ID,
                            zone.getID());
                }
                /// @}
            }

            String ignore = SystemProperties.get("gsm.ignore-nitz");
            if (ignore != null && ignore.equals("yes")) {
                log("NITZ: Not setting clock because gsm.ignore-nitz is set");
                return;
            }

            try {
                mWakeLock.acquire();

                if (!mPhone.isPhoneTypeGsm() || getAutoTime()) {
                    long millisSinceNitzReceived
                            = SystemClock.elapsedRealtime() - nitzReceiveTime;

                    if (millisSinceNitzReceived < 0) {
                        // Sanity check: something is wrong
                        if (DBG) {
                            log("NITZ: not setting time, clock has rolled "
                                    + "backwards since NITZ time was received, "
                                    + nitz);
                        }
                        return;
                    }

                    if (millisSinceNitzReceived > Integer.MAX_VALUE) {
                        // If the time is this far off, something is wrong > 24 days!
                        if (DBG) {
                            log("NITZ: not setting time, processing has taken "
                                    + (millisSinceNitzReceived / (1000 * 60 * 60 * 24))
                                    + " days");
                        }
                        return;
                    }

                    // Note: with range checks above, cast to int is safe
                    c.add(Calendar.MILLISECOND, (int)millisSinceNitzReceived);

                    if (DBG) {
                        log("NITZ: Setting time of day to " + c.getTime()
                                + " NITZ receive delay(ms): " + millisSinceNitzReceived
                                + " gained(ms): "
                                + (c.getTimeInMillis() - System.currentTimeMillis())
                                + " from " + nitz);
                    }
                    if (mPhone.isPhoneTypeGsm()) {
                        setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                        Rlog.i(LOG_TAG, "NITZ: after Setting time of day");
                    } else {
                        if (getAutoTime()) {
                            /**
                             * Update system time automatically
                             */
                            long gained = c.getTimeInMillis() - System.currentTimeMillis();
                            long timeSinceLastUpdate = SystemClock.elapsedRealtime() - mSavedAtTime;
                            int nitzUpdateSpacing = Settings.Global.getInt(mCr,
                                    Settings.Global.NITZ_UPDATE_SPACING, mNitzUpdateSpacing);
                            int nitzUpdateDiff = Settings.Global.getInt(mCr,
                                    Settings.Global.NITZ_UPDATE_DIFF, mNitzUpdateDiff);

                            if ((mSavedAtTime == 0) || (timeSinceLastUpdate > nitzUpdateSpacing)
                                    || (Math.abs(gained) > nitzUpdateDiff)) {
                                if (DBG) {
                                    log("NITZ: Auto updating time of day to " + c.getTime()
                                            + " NITZ receive delay=" + millisSinceNitzReceived
                                            + "ms gained=" + gained + "ms from " + nitz);
                                }

                                setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                            } else {
                                if (DBG) {
                                    log("NITZ: ignore, a previous update was "
                                            + timeSinceLastUpdate + "ms ago and gained=" + gained + "ms");
                                }
                                return;
                            }
                        }
                    }
                }
                SystemProperties.set("gsm.nitz.time", String.valueOf(c.getTimeInMillis()));
                saveNitzTime(c.getTimeInMillis());
                mNitzUpdatedTime = true;
            } finally {
                if (DBG) {
                    long end = SystemClock.elapsedRealtime();
                    log("NITZ: end=" + end + " dur=" + (end - start));
                }
                mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            loge("NITZ: Parsing NITZ time " + nitz + " ex=" + ex);
        }
    }


    private boolean isAllowFixTimeZone() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (sReceiveNitz[i]) {
                log("Phone" + i + " has received NITZ!!");
                return false;
            }
        }
        log("Fix time zone allowed");
        return true;
    }

    private boolean getAutoTime() {
        try {
            return Settings.Global.getInt(mCr, Settings.Global.AUTO_TIME) > 0;
        } catch (Settings.SettingNotFoundException snfe) {
            return true;
        }
    }

    private boolean getAutoTimeZone() {
        try {
            return Settings.Global.getInt(mCr, Settings.Global.AUTO_TIME_ZONE) > 0;
        } catch (Settings.SettingNotFoundException snfe) {
            return true;
        }
    }

    private void saveNitzTimeZone(String zoneId) {
        log("saveNitzTimeZone zoneId:" + zoneId);
        mSavedTimeZone = zoneId;
    }

    private void saveNitzTime(long time) {
        if (DBG) log("saveNitzTime: time=" + time);
        mSavedTime = time;
        mSavedAtTime = SystemClock.elapsedRealtime();
    }

    /**
     * Set the timezone and send out a sticky broadcast so the system can
     * determine if the timezone was set by the carrier.
     *
     * @param zoneId timezone set by carrier
     */
    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        if (DBG) log("setAndBroadcastNetworkSetTimeZone: setTimeZone=" + zoneId);
        AlarmManager alarm =
                (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(zoneId);
        Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_SET_TIMEZONE);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("time-zone", zoneId);
        mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (DBG) {
            log("setAndBroadcastNetworkSetTimeZone: call alarm.setTimeZone and broadcast zoneId=" +
                    zoneId);
        }
    }

    /**
     * Set the time and Send out a sticky broadcast so the system can determine
     * if the time was set by the carrier.
     *
     * @param time time set by network
     */
    private void setAndBroadcastNetworkSetTime(long time) {
        if (DBG) log("setAndBroadcastNetworkSetTime: time=" + time + "ms");
        SystemClock.setCurrentTimeMillis(time);
        Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_SET_TIME);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("time", time);
        mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void revertToNitzTime() {
        if (Settings.Global.getInt(mCr, Settings.Global.AUTO_TIME, 0) == 0) {
            log("[NITZ]:revertToNitz,AUTO_TIME is 0");
            return;
        }
        if (mPhone.isPhoneTypeGsm()) {
            // M
            // [ALPS01962013] This phone has received NITZ, so no need to do any fix
            if (getReceivedNitz()) {
                if (DBG) {
                    log("Reverting to NITZ Time: mSavedTime=" + mSavedTime + " mSavedAtTime=" +
                            mSavedAtTime + " tz='" + mSavedTimeZone + "'");
                }
                if (mSavedTime != 0 && mSavedAtTime != 0) {
                    setAndBroadcastNetworkSetTime(mSavedTime
                            + (SystemClock.elapsedRealtime() - mSavedAtTime));
                }
                return;
            }

            // [ALPS01962013] No phone has recieved NITZ, so fix it and update
            if (isAllowFixTimeZone()) {
                fixTimeZone();
                if (DBG) log("Reverting to fixed TimeZone: tz='" + mSavedTimeZone);
            if (mSavedTimeZone != null) {
                setAndBroadcastNetworkSetTimeZone(mSavedTimeZone);
            }
                return;
            }

            // [ALPS01962013] This phone did't receive NITZ, but other phone did
            if (DBG) log("Do nothing since other phone has received NITZ, but this phone didn't");
        } else {
            // AOSP
            if (DBG) {
                log("Reverting to NITZ Time: mSavedTime=" + mSavedTime + " mSavedAtTime=" +
                        mSavedAtTime + " tz='" + mSavedTimeZone + "'");
            }
            if (mSavedTime != 0 && mSavedAtTime != 0) {
                setAndBroadcastNetworkSetTime(mSavedTime
                        + (SystemClock.elapsedRealtime() - mSavedAtTime));
            }
        }
    }

    private void revertToNitzTimeZone() {
        if (Settings.Global.getInt(mCr, Settings.Global.AUTO_TIME_ZONE, 0) == 0) {
            return;
        }
        if (DBG) log("Reverting to NITZ TimeZone: tz='" + mSavedTimeZone);
        if (mSavedTimeZone != null) {
            setAndBroadcastNetworkSetTimeZone(mSavedTimeZone);
        }
    }

    /**
     * Post a notification to NotificationManager for restricted state
     *
     * @param notifyType is one state of PS/CS_*_ENABLE/DISABLE
     */
    private void setNotification(int notifyType) {
    /* ALPS00339508 :Remove restricted access change notification */
    /*
        if (DBG) log("setNotification: create notification " + notifyType);

        // Needed because sprout RIL sends these when they shouldn't?
        boolean isSetNotification = mPhone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_user_notification_of_restrictied_mobile_access);
        if (!isSetNotification) {
            if (DBG) log("Ignore all the notifications");
            return;
        }

        Context context = mPhone.getContext();


        CharSequence details = "";
        CharSequence title = context.getText(com.android.internal.R.string.RestrictedOnData);
        int notificationId = CS_NOTIFICATION;

        switch (notifyType) {
            case PS_ENABLED:
                long dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                if (dataSubId != mPhone.getSubId()) {
                    return;
                }
                notificationId = PS_NOTIFICATION;
                details = context.getText(com.android.internal.R.string.RestrictedOnData);
                break;
            case PS_DISABLED:
                notificationId = PS_NOTIFICATION;
                break;
            case CS_ENABLED:
                details = context.getText(com.android.internal.R.string.RestrictedOnAllVoice);
                break;
            case CS_NORMAL_ENABLED:
                details = context.getText(com.android.internal.R.string.RestrictedOnNormal);
                break;
            case CS_EMERGENCY_ENABLED:
                details = context.getText(com.android.internal.R.string.RestrictedOnEmergency);
                break;
            case CS_DISABLED:
                // do nothing and cancel the notification later
                break;
        }

        if (DBG) log("setNotification: put notification " + title + " / " +details);
        mNotification = new Notification.Builder(context)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(com.android.internal.R.drawable.stat_sys_warning)
                .setTicker(title)
                .setColor(context.getResources().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
                .setContentText(details)
                .build();

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notifyType == PS_DISABLED || notifyType == CS_DISABLED) {
            // cancel previous post notification
            notificationManager.cancel(notificationId);
        } else {
            // update restricted state notification
            notificationManager.notify(notificationId, mNotification);
        }
    */
    }

    private UiccCardApplication getUiccCardApplication() {
        if (mPhone.isPhoneTypeGsm()) {
            return mUiccController.getUiccCardApplication(mPhone.getPhoneId(),
                    UiccController.APP_FAM_3GPP);
        } else {
            return mUiccController.getUiccCardApplication(mPhone.getPhoneId(),
                    UiccController.APP_FAM_3GPP2);
        }
    }

    private void queueNextSignalStrengthPoll() {
        if (mDontPollSignalStrength) {
            // The radio is telling us about signal strength changes
            // we don't have to ask it
            return;
        }

        Message msg;

        msg = obtainMessage();
        msg.what = EVENT_POLL_SIGNAL_STRENGTH;

        long nextTime;

        // TODO Don't poll signal strength if screen is off
        sendMessageDelayed(msg, POLL_PERIOD_MILLIS);
    }

    private void notifyCdmaSubscriptionInfoReady() {
        if (mCdmaForSubscriptionInfoReadyRegistrants != null) {
            if (DBG) log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
            mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
        }
    }

    /**
     * Registration point for transition into DataConnection attached.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataConnectionAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mAttachedRegistrants.add(r);

        if (getCurrentDataConnectionState() == ServiceState.STATE_IN_SERVICE) {
            r.notifyRegistrant();
        }
    }
    public void unregisterForDataConnectionAttached(Handler h) {
        mAttachedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into DataConnection detached.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataConnectionDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDetachedRegistrants.add(r);

        if (getCurrentDataConnectionState() != ServiceState.STATE_IN_SERVICE) {
            r.notifyRegistrant();
        }
    }
    public void unregisterForDataConnectionDetached(Handler h) {
        mDetachedRegistrants.remove(h);
    }

    /**
     * Registration for DataConnection RIL Data Radio Technology changing. The
     * new radio technology will be returned AsyncResult#result as an Integer Object.
     * The AsyncResult will be in the notification Message#obj.
     *
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForDataRegStateOrRatChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mDataRegStateOrRatChangedRegistrants.add(r);
        notifyDataRegStateRilRadioTechnologyChanged();
    }
    public void unregisterForDataRegStateOrRatChanged(Handler h) {
        mDataRegStateOrRatChangedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into network attached.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj in Message.obj
     */
    public void registerForNetworkAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);

        mNetworkAttachedRegistrants.add(r);
        if (mSS.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
            r.notifyRegistrant();
        }
    }
    public void unregisterForNetworkAttached(Handler h) {
        mNetworkAttachedRegistrants.remove(h);
    }

    /**
     * Registration point for transition into packet service restricted zone.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForPsRestrictedEnabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPsRestrictEnabledRegistrants.add(r);

        if (mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedEnabled(Handler h) {
        mPsRestrictEnabledRegistrants.remove(h);
    }

    /**
     * Registration point for transition out of packet service restricted zone.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForPsRestrictedDisabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mPsRestrictDisabledRegistrants.add(r);

        if (mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedDisabled(Handler h) {
        mPsRestrictDisabledRegistrants.remove(h);
    }

    /**
     * Registration point for signal strength changed.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSignalStrengthChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSignalStrengthChangedRegistrants.add(r);
    }

    /**
     * Unregister registration point for signal strength changed.
     * @param h handler to notify
    */
    public void unregisterForSignalStrengthChanged(Handler h) {
        mSignalStrengthChangedRegistrants.remove(h);
    }


    /**
     * Clean up existing voice and data connection then turn off radio power.
     *
     * Hang up the existing voice calls to decrease call drop rate.
     */
    public void powerOffRadioSafely(DcTracker dcTracker) {
        synchronized (this) {
            if (!mPendingRadioPowerOffAfterDataOff) {
                if (mPhone.isPhoneTypeGsm() || mPhone.isPhoneTypeCdmaLte()) {
                    int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                    int phoneSubId = mPhone.getSubId();
                    // To minimize race conditions we call cleanUpAllConnections on
                    // both if else paths instead of before this isDisconnected test.
                    log("powerOffRadioSafely phoneId=" + SubscriptionManager.getPhoneId(dds)
                            + ", dds=" + dds + ", mPhone.getSubId()=" + mPhone.getSubId()
                            + ", phoneSubId=" + phoneSubId);
                    if (dds != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                            && (dcTracker.isDisconnected() || dds != phoneSubId)) {
                        // M: remove check peer phone data state
                        // To minimize race conditions we do this after isDisconnected
                        dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                        if (DBG) log("Data disconnected, turn off radio right away.");
                        hangupAndPowerOff();
                    } else if (!mPhone.isPhoneTypeGsm()
                            && (dcTracker.isDisconnected()
                                && (dds == mPhone.getSubId()
                                || (dds != mPhone.getSubId()
                                && ProxyController.getInstance().isDataDisconnected(dds))))) {
                        // To minimize race conditions we do this after isDisconnected
                        dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                        if (DBG) log("Data disconnected, turn off radio right away.");
                        hangupAndPowerOff();
                    } else {
                        // hang up all active voice calls first
                        if (mPhone.isPhoneTypeGsm() && mPhone.isInCall()) {
                            mPhone.mCT.mRingingCall.hangupIfAlive();
                            mPhone.mCT.mBackgroundCall.hangupIfAlive();
                            mPhone.mCT.mForegroundCall.hangupIfAlive();
                        }
                        dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);

                        if (mPhone.isPhoneTypeGsm()) {
                            if (dds == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                                    || SubscriptionManager.getPhoneId(dds)
                                    == SubscriptionManager.DEFAULT_PHONE_INDEX) {
                                if (dcTracker.isDisconnected()
                                        || dcTracker.isOnlyIMSorEIMSPdnConnected()) {
                                    if (DBG) log("Data disconnected (no data sub), " +
                                            "turn off radio right away.");
                                    hangupAndPowerOff();
                                    return;
                                } else {
                                    if (DBG) {
                                        log("Data is active on.  Wait for all data disconnect");
                                    }
                                    mPhone.registerForAllDataDisconnected(this,
                                            EVENT_ALL_DATA_DISCONNECTED, null);
                                    mPendingRadioPowerOffAfterDataOff = true;
                                }
                            }
                        } else {
                            if (dds != mPhone.getSubId()
                                    && !ProxyController.getInstance().isDataDisconnected(dds)) {
                                if (DBG) log("Data is active on DDS. "
                                        + "Wait for all data disconnect");
                                // Data is not disconnected on DDS.
                                // Wait for the data disconnect complete
                                // before sending the RADIO_POWER off.
                                ProxyController.getInstance().registerForAllDataDisconnected(
                                        dds, this,
                                        EVENT_ALL_DATA_DISCONNECTED, null);
                                mPendingRadioPowerOffAfterDataOff = true;
                            }
                        }

                        if (dcTracker.isOnlyIMSorEIMSPdnConnected()) {
                            if (DBG) {
                                log("Only IMS or EIMS connected, " +
                                        "turn off radio right away.");
                            }
                            hangupAndPowerOff();
                            return;
                        }

                        Message msg = Message.obtain(this);
                        msg.what = EVENT_SET_RADIO_POWER_OFF;
                        msg.arg1 = ++mPendingRadioPowerOffAfterDataOffTag;
                        if (sendMessageDelayed(msg, 5000)) {
                            if (DBG) {
                                log("Wait upto 5s for data to disconnect, then turn off radio.");
                            }
                            mPendingRadioPowerOffAfterDataOff = true;
                        } else {
                            log("Cannot send delayed Msg, turn off radio right away.");
                            hangupAndPowerOff();
                            mPendingRadioPowerOffAfterDataOff = false;
                        }
                    }
                } else {
                    // In some network, deactivate PDP connection cause releasing of RRC connection,
                    // which MM/IMSI detaching request needs. Without this detaching, network can
                    // not release the network resources previously attached.
                    // So we are avoiding data detaching on these networks.
                    String[] networkNotClearData = mPhone.getContext().getResources()
                            .getStringArray(com.android.internal.R.array.networks_not_clear_data);
                    String currentNetwork = mSS.getOperatorNumeric();
                    if ((networkNotClearData != null) && (currentNetwork != null)) {
                        for (int i = 0; i < networkNotClearData.length; i++) {
                            if (currentNetwork.equals(networkNotClearData[i])) {
                                // Don't clear data connection for this carrier
                                if (DBG)
                                    log("Not disconnecting data for " + currentNetwork);
                                hangupAndPowerOff();
                                return;
                            }
                        }
                    }
                    // To minimize race conditions we call cleanUpAllConnections on
                    // both if else paths instead of before this isDisconnected test.
                    if (dcTracker.isDisconnected()) {
                        // To minimize race conditions we do this after isDisconnected
                        dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                        if (DBG) log("Data disconnected, turn off radio right away.");
                        hangupAndPowerOff();
                    } else {
                        dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                        Message msg = Message.obtain(this);
                        msg.what = EVENT_SET_RADIO_POWER_OFF;
                        msg.arg1 = ++mPendingRadioPowerOffAfterDataOffTag;
                        if (sendMessageDelayed(msg, 30000)) {
                            if (DBG)
                                log("Wait upto 30s for data to disconnect, then turn off radio.");
                            mPendingRadioPowerOffAfterDataOff = true;
                        } else {
                            log("Cannot send delayed Msg, turn off radio right away.");
                            hangupAndPowerOff();
                        }
                    }
                }
            }
        }
    }

    /**
     * process the pending request to turn radio off after data is disconnected
     *
     * return true if there is pending request to process; false otherwise.
     */
    public boolean processPendingRadioPowerOffAfterDataOff() {
        synchronized(this) {
            if (mPendingRadioPowerOffAfterDataOff) {
                if (DBG) log("Process pending request to turn radio off.");
                mPendingRadioPowerOffAfterDataOffTag += 1;
                hangupAndPowerOff();
                mPendingRadioPowerOffAfterDataOff = false;
                return true;
            }
            return false;
        }
    }

    /**
     * send signal-strength-changed notification if changed Called both for
     * solicited and unsolicited signal strength updates
     *
     * @return true if the signal strength changed and a notification was sent.
     */
    protected boolean onSignalStrengthResult(AsyncResult ar) {
        boolean isGsm = false;
        //override isGsm for CDMA LTE
        if (mPhone.isPhoneTypeGsm() ||
                (mPhone.isPhoneTypeCdmaLte() &&
                        mSS.getRilDataRadioTechnology() == ServiceState.RIL_RADIO_TECHNOLOGY_LTE)) {
            isGsm = true;
        }

        // This signal is used for both voice and data radio signal so parse
        // all fields

        if ((ar.exception == null) && (ar.result != null)) {
            mSignalStrength = (SignalStrength) ar.result;
            mSignalStrength.validateInput();
            mSignalStrength.setGsm(isGsm);
            if (DBG) {
                log("onSignalStrengthResult():" +
                        ((mLastSignalStrength != null) ? ("LastSignalStrength="
                        + mLastSignalStrength.toString()) : "") +
                        "new mSignalStrength="
                        + mSignalStrength.toString());
            }
        } else {
            log("onSignalStrengthResult() Exception from RIL : " + ar.exception);
            mSignalStrength = new SignalStrength(isGsm);
        }

        boolean ssChanged = notifySignalStrength();

        return ssChanged;
    }

    /**
     * Hang up all voice call and turn off radio. Implemented by derived class.
     */
    protected void hangupAndPowerOff() {
        // hang up all active voice calls
        if (!mPhone.isPhoneTypeGsm() || mPhone.isInCall()) {
            mPhone.mCT.mRingingCall.hangupIfAlive();
            mPhone.mCT.mBackgroundCall.hangupIfAlive();
            mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        //MTK-START some actions must be took before EFUN
        RadioManager.getInstance().sendRequestBeforeSetRadioPower(false, mPhone.getPhoneId());
        //MTK-END
        mCi.setRadioPower(false, null);

    }

    /** Cancel a pending (if any) pollState() operation */
    protected void cancelPollState() {
        // This will effectively cancel the rest of the poll requests.
        mPollingContext = new int[1];
    }

    /**
     * Return true if time zone needs fixing.
     *
     * @param phone
     * @param operatorNumeric
     * @param prevOperatorNumeric
     * @param needToFixTimeZone
     * @return true if time zone needs to be fixed
     */
    protected boolean shouldFixTimeZoneNow(Phone phone, String operatorNumeric,
            String prevOperatorNumeric, boolean needToFixTimeZone) {
        // Return false if the mcc isn't valid as we don't know where we are.
        // Return true if we have an IccCard and the mcc changed or we
        // need to fix it because when the NITZ time came in we didn't
        // know the country code.

        // If mcc is invalid then we'll return false
        int mcc;
        try {
            mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
        } catch (Exception e) {
            if (DBG) {
                log("shouldFixTimeZoneNow: no mcc, operatorNumeric=" + operatorNumeric +
                        " retVal=false");
            }
            return false;
        }

        // If prevMcc is invalid will make it different from mcc
        // so we'll return true if the card exists.
        int prevMcc;
        try {
            prevMcc = Integer.parseInt(prevOperatorNumeric.substring(0, 3));
        } catch (Exception e) {
            prevMcc = mcc + 1;
        }

        // Determine if the Icc card exists
        boolean iccCardExist = false;
        if (mUiccApplcation != null) {
            iccCardExist = mUiccApplcation.getState() != AppState.APPSTATE_UNKNOWN;
        }

        // Determine retVal
        boolean retVal = ((iccCardExist && (mcc != prevMcc)) || needToFixTimeZone);
        if (DBG) {
            long ctm = System.currentTimeMillis();
            log("shouldFixTimeZoneNow: retVal=" + retVal +
                    " iccCardExist=" + iccCardExist +
                    " operatorNumeric=" + operatorNumeric + " mcc=" + mcc +
                    " prevOperatorNumeric=" + prevOperatorNumeric + " prevMcc=" + prevMcc +
                    " needToFixTimeZone=" + needToFixTimeZone +
                    " ltod=" + TimeUtils.logTimeOfDay(ctm));
        }
        return retVal;
    }

    public String getSystemProperty(String property, String defValue) {
        return TelephonyManager.getTelephonyProperty(mPhone.getPhoneId(), property, defValue);
    }

    /**
     * @return all available cell information or null if none.
     */
    public List<CellInfo> getAllCellInfo() {
        CellInfoResult result = new CellInfoResult();
        // if (DBG) log("SST.getAllCellInfo(): E");
        String mLog = "SST.getAllCellInfo(): ";
        int ver = mCi.getRilVersion();
        if (ver >= 8) {
            if (isCallerOnDifferentThread()) {
                if ((SystemClock.elapsedRealtime() - mLastCellInfoListTime)
                        > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                    Message msg = obtainMessage(EVENT_GET_CELL_INFO_LIST, result);
                    synchronized(result.lockObj) {
                        result.list = null;
                        mCi.getCellInfoList(msg);
                        try {
                            result.lockObj.wait(5000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // if (DBG) log("SST.getAllCellInfo(): return last, back to back calls");
                    mLog = mLog + "return last, back to back calls. ";
                    result.list = mLastCellInfoList;
                }
            } else {
                // if (DBG) log("SST.getAllCellInfo(): return last, same thread can't block");
                mLog = mLog + "return last, same thread can't block. ";
                result.list = mLastCellInfoList;
            }
        } else {
            // if (DBG) log("SST.getAllCellInfo(): not implemented");
            mLog = mLog + "not implemented. ";
            result.list = null;
        }
        synchronized(result.lockObj) {
            if (result.list != null) {
                if (VDBG) log(mLog + "X size=" + result.list.size()
                        + " list=" + result.list);
                return result.list;
            } else {
                mLog = mLog + "X size=0 list=null.";
                if (DBG) log(mLog);
                return null;
            }
        }
    }

    //M: MTK START Common
    protected List<CellInfo> getAllCellInfoByRate() {
        CellInfoResult result = new CellInfoResult();
        if (DBG) {
            log("SST.getAllCellInfoByRate(): enter");
        }
        int ver = mCi.getRilVersion();
        if (ver >= 8) {
            if (isCallerOnDifferentThread()) {
                if ((SystemClock.elapsedRealtime() - mLastCellInfoListTime)
                        > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                    Message msg = obtainMessage(EVENT_GET_CELL_INFO_LIST_BY_RATE, result);
                    synchronized (result.lockObj) {
                        mCi.getCellInfoList(msg);
                        try {
                            result.lockObj.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            result.list = null;
                        }
                    }
                } else {
                    if (DBG) {
                        log("SST.getAllCellInfoByRate(): return last, back to back calls");
                    }
                    result.list = mLastCellInfoList;
                }
            } else {
                if (DBG) {
                    log("SST.getAllCellInfoByRate(): return last, same thread can't block");
                }
                result.list = mLastCellInfoList;
            }
        } else {
            if (DBG) log("SST.getAllCellInfoByRate(): not implemented");
            result.list = null;
        }
        if (DBG) {
            if (result.list != null) {
                log("SST.getAllCellInfoByRate(): X size=" + result.list.size()
                    + " list=" + result.list);
            } else {
                log("SST.getAllCellInfoByRate(): X size=0 list=null");
            }
        }
        return result.list;
    }

    public void setCellInfoRate(int rateInMillis) {
        log("SST.setCellInfoRate()");
        mCellInfoRate = rateInMillis;
        updateCellInfoRate();
    }

    protected void updateCellInfoRate() {
        log("SST.updateCellInfoRate()");
        if (mPhone.isPhoneTypeGsm()) {
            log("updateCellInfoRate(),mCellInfoRate= " + mCellInfoRate);
            if ((mCellInfoRate != Integer.MAX_VALUE) && (mCellInfoRate != 0)) {
                if (mCellInfoTimer != null) {
                    log("cancel previous timer if any");
                    mCellInfoTimer.cancel();
                    mCellInfoTimer = null;
                }

                mCellInfoTimer = new Timer(true);

                log("schedule timer with period = " + mCellInfoRate + " ms");
                mCellInfoTimer.schedule(new timerTask(), mCellInfoRate);
            } else if ((mCellInfoRate == 0) || (mCellInfoRate == Integer.MAX_VALUE)) {
                if (mCellInfoTimer != null) {
                    log("cancel cell info timer if any");
                    mCellInfoTimer.cancel();
                    mCellInfoTimer = null;
                }
            }
        }
    }
    // M: MTK END

    /**
     * @return signal strength
     */
    public SignalStrength getSignalStrength() {
        return mSignalStrength;
    }

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mCdmaForSubscriptionInfoReadyRegistrants.add(r);

        if (isMinInfoReady()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        mCdmaForSubscriptionInfoReadyRegistrants.remove(h);
    }

    /**
     * Save current source of cdma subscription
     * @param source - 1 for NV, 0 for RUIM
     */
    private void saveCdmaSubscriptionSource(int source) {
        log("Storing cdma subscription source: " + source);
        Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE,
                source);
        log("Read from settings: " + Settings.Global.getInt(mPhone.getContext().getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, -1));
    }

    private void getSubscriptionInfoAndStartPollingThreads() {
        mCi.getCDMASubscription(obtainMessage(EVENT_POLL_STATE_CDMA_SUBSCRIPTION));

        // Get Registration Information
        pollState();
    }

    private void handleCdmaSubscriptionSource(int newSubscriptionSource) {
        log("Subscription Source : " + newSubscriptionSource);
        mIsSubscriptionFromRuim =
                (newSubscriptionSource == CdmaSubscriptionSourceManager.SUBSCRIPTION_FROM_RUIM);
        log("isFromRuim: " + mIsSubscriptionFromRuim);
        saveCdmaSubscriptionSource(newSubscriptionSource);
        if (!mIsSubscriptionFromRuim) {
            // NV is ready when subscription source is NV
            sendMessage(obtainMessage(EVENT_NV_READY));
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ServiceStateTracker:");
        pw.println(" mSubId=" + mSubId);
        pw.println(" mSS=" + mSS);
        pw.println(" mNewSS=" + mNewSS);
        pw.println(" mVoiceCapable=" + mVoiceCapable);
        pw.println(" mRestrictedState=" + mRestrictedState);
        pw.println(" mPollingContext=" + mPollingContext + " - " +
                (mPollingContext != null ? mPollingContext[0] : ""));
        pw.println(" mDesiredPowerState=" + mDesiredPowerState);
        pw.println(" mDontPollSignalStrength=" + mDontPollSignalStrength);
        pw.println(" mSignalStrength=" + mSignalStrength);
        pw.println(" mLastSignalStrength=" + mLastSignalStrength);
        pw.println(" mRestrictedState=" + mRestrictedState);
        pw.println(" mPendingRadioPowerOffAfterDataOff=" + mPendingRadioPowerOffAfterDataOff);
        pw.println(" mPendingRadioPowerOffAfterDataOffTag=" + mPendingRadioPowerOffAfterDataOffTag);
        pw.println(" mCellLoc=" + mCellLoc);
        pw.println(" mNewCellLoc=" + mNewCellLoc);
        pw.println(" mLastCellInfoListTime=" + mLastCellInfoListTime);
        pw.println(" mPreferredNetworkType=" + mPreferredNetworkType);
        pw.println(" mMaxDataCalls=" + mMaxDataCalls);
        pw.println(" mNewMaxDataCalls=" + mNewMaxDataCalls);
        pw.println(" mReasonDataDenied=" + mReasonDataDenied);
        pw.println(" mNewReasonDataDenied=" + mNewReasonDataDenied);
        pw.println(" mGsmRoaming=" + mGsmRoaming);
        pw.println(" mDataRoaming=" + mDataRoaming);
        pw.println(" mEmergencyOnly=" + mEmergencyOnly);
        pw.println(" mNeedFixZoneAfterNitz=" + mNeedFixZoneAfterNitz);
        pw.flush();
        pw.println(" mZoneOffset=" + mZoneOffset);
        pw.println(" mZoneDst=" + mZoneDst);
        pw.println(" mZoneTime=" + mZoneTime);
        pw.println(" mGotCountryCode=" + mGotCountryCode);
        pw.println(" mNitzUpdatedTime=" + mNitzUpdatedTime);
        pw.println(" mSavedTimeZone=" + mSavedTimeZone);
        pw.println(" mSavedTime=" + mSavedTime);
        pw.println(" mSavedAtTime=" + mSavedAtTime);
        pw.println(" mStartedGprsRegCheck=" + mStartedGprsRegCheck);
        pw.println(" mReportedGprsNoReg=" + mReportedGprsNoReg);
        pw.println(" mNotification=" + mNotification);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" mCurSpn=" + mCurSpn);
        pw.println(" mCurDataSpn=" + mCurDataSpn);
        pw.println(" mCurShowSpn=" + mCurShowSpn);
        pw.println(" mCurPlmn=" + mCurPlmn);
        pw.println(" mCurShowPlmn=" + mCurShowPlmn);
        pw.flush();
        pw.println(" mCurrentOtaspMode=" + mCurrentOtaspMode);
        pw.println(" mRoamingIndicator=" + mRoamingIndicator);
        pw.println(" mIsInPrl=" + mIsInPrl);
        pw.println(" mDefaultRoamingIndicator=" + mDefaultRoamingIndicator);
        pw.println(" mRegistrationState=" + mRegistrationState);
        pw.println(" mMdn=" + mMdn);
        pw.println(" mHomeSystemId=" + mHomeSystemId);
        pw.println(" mHomeNetworkId=" + mHomeNetworkId);
        pw.println(" mMin=" + mMin);
        pw.println(" mPrlVersion=" + mPrlVersion);
        pw.println(" mIsMinInfoReady=" + mIsMinInfoReady);
        pw.println(" mIsEriTextLoaded=" + mIsEriTextLoaded);
        pw.println(" mIsSubscriptionFromRuim=" + mIsSubscriptionFromRuim);
        pw.println(" mCdmaSSM=" + mCdmaSSM);
        pw.println(" mRegistrationDeniedReason=" + mRegistrationDeniedReason);
        pw.println(" mCurrentCarrier=" + mCurrentCarrier);
        pw.flush();
        pw.println(" mImsRegistered=" + mImsRegistered);
        pw.println(" mImsRegistrationOnOff=" + mImsRegistrationOnOff);
        pw.println(" mAlarmSwitch=" + mAlarmSwitch);
        pw.println(" mPowerOffDelayNeed=" + mPowerOffDelayNeed);
        pw.println(" mDeviceShuttingDown=" + mDeviceShuttingDown);
        pw.println(" mSpnUpdatePending=" + mSpnUpdatePending);


    }

    public boolean isImsRegistered() {
        return mImsRegistered;
    }
    /**
     * Verifies the current thread is the same as the thread originally
     * used in the initialization of this instance. Throws RuntimeException
     * if not.
     *
     * @exception RuntimeException if the current thread is not
     * the thread that originally obtained this Phone instance.
     */
    protected void checkCorrectThread() {
        if (Thread.currentThread() != getLooper().getThread()) {
            throw new RuntimeException(
                    "ServiceStateTracker must be used from within one thread");
        }
    }

    protected boolean isCallerOnDifferentThread() {
        boolean value = Thread.currentThread() != getLooper().getThread();
        if (VDBG) log("isCallerOnDifferentThread: " + value);
        return value;
    }

    protected void updateCarrierMccMncConfiguration(String newOp, String oldOp, Context context) {
        // if we have a change in operator, notify wifi (even to/from none)
        if (((newOp == null) && (TextUtils.isEmpty(oldOp) == false)) ||
                ((newOp != null) && (newOp.equals(oldOp) == false))) {
            log("update mccmnc=" + newOp + " fromServiceState=true");
            MccTable.updateMccMncConfiguration(context, newOp, true);
        }
    }

    /**
     * Check ISO country by MCC to see if phone is roaming in same registered country
     */
    protected boolean inSameCountry(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || (operatorNumeric.length() < 5)) {
            // Not a valid network
            return false;
        }
        final String homeNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeNumeric) || (homeNumeric.length() < 5)) {
            // Not a valid SIM MCC
            return false;
        }
        boolean inSameCountry = true;
        final String networkMCC = operatorNumeric.substring(0, 3);
        final String homeMCC = homeNumeric.substring(0, 3);
        final String networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
        final String homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            // Not a valid country
            return false;
        }
        inSameCountry = homeCountry.equals(networkCountry);
        if (inSameCountry) {
            return inSameCountry;
        }
        // special same country cases
        if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
            inSameCountry = true;
        } else if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
            inSameCountry = true;
        }
        return inSameCountry;
    }

    /**
     * Set both voice and data roaming type,
     * judging from the ISO country of SIM VS network.
     */
    protected void setRoamingType(ServiceState currentServiceState) {
        final boolean isVoiceInService =
                (currentServiceState.getVoiceRegState() == ServiceState.STATE_IN_SERVICE);
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                if (mPhone.isPhoneTypeGsm()) {
                    // check roaming type by MCC
                    if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_DOMESTIC);
                    } else {
                        currentServiceState.setVoiceRoamingType(
                                ServiceState.ROAMING_TYPE_INTERNATIONAL);
                    }
                } else {
                    // some carrier defines international roaming by indicator
                    int[] intRoamingIndicators = mPhone.getContext().getResources().getIntArray(
                            com.android.internal.R.array.config_cdma_international_roaming_indicators);
                    if ((intRoamingIndicators != null) && (intRoamingIndicators.length > 0)) {
                        // It's domestic roaming at least now
                        currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_DOMESTIC);
                        int curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                        for (int i = 0; i < intRoamingIndicators.length; i++) {
                            if (curRoamingIndicator == intRoamingIndicators[i]) {
                                currentServiceState.setVoiceRoamingType(
                                        ServiceState.ROAMING_TYPE_INTERNATIONAL);
                                break;
                            }
                        }
                    } else {
                        // check roaming type by MCC
                        if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setVoiceRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            } else {
                currentServiceState.setVoiceRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            }
        }
        final boolean isDataInService =
                (currentServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE);
        final int dataRegType = currentServiceState.getRilDataRadioTechnology();
        if (isDataInService) {
            if (!currentServiceState.getDataRoaming()) {
                currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_NOT_ROAMING);
            } else {
                if (mPhone.isPhoneTypeGsm()) {
                    if (ServiceState.isGsm(dataRegType)) {
                        if (isVoiceInService) {
                            // GSM data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide GSM data roaming type without voice
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                        }
                    } else {
                        // we can not decide 3gpp2 roaming state here
                        currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                    }
                } else {
                    if (ServiceState.isCdma(dataRegType)) {
                        if (isVoiceInService) {
                            // CDMA data should have the same state as voice
                            currentServiceState.setDataRoamingType(currentServiceState
                                    .getVoiceRoamingType());
                        } else {
                            // we can not decide CDMA data roaming type without voice
                            // set it as same as last time
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_UNKNOWN);
                        }
                    } else {
                        // take it as 3GPP roaming
                        if (inSameCountry(currentServiceState.getDataOperatorNumeric())) {
                            currentServiceState.setDataRoamingType(ServiceState.ROAMING_TYPE_DOMESTIC);
                        } else {
                            currentServiceState.setDataRoamingType(
                                    ServiceState.ROAMING_TYPE_INTERNATIONAL);
                        }
                    }
                }
            }
        }
    }

    private void setSignalStrengthDefaultValues() {
        mSignalStrength = new SignalStrength(true);
    }

    private void setNullState() {
        mGsmRoaming = false;
        mNewReasonDataDenied = -1;
        mNewMaxDataCalls = 1;
        mDataRoaming = false;
        //[ALPS00423362]
        mEmergencyOnly = false;
        updateLocatedPlmn(null);
        //[ALPS00439473] MTK add - START
        mDontPollSignalStrength = false;
        mLastSignalStrength = new SignalStrength(true);
        //[ALPS00439473] MTK add - END
        //MTK-ADD : for CS not registered , PS regsitered (ex: LTE PS only mode or 2/3G PS only SIM
        //card or CS domain network registeration temporary failure
        isCsInvalidCard = false;
        //MTK-ADD: ALPS01830723
        mPsRegState = ServiceState.STATE_OUT_OF_SERVICE;
    }

    protected String getHomeOperatorNumeric() {
        String numeric = ((TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE)).
                getSimOperatorNumericForPhone(mPhone.getPhoneId());
        if (!mPhone.isPhoneTypeGsm() && TextUtils.isEmpty(numeric)) {
            numeric = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
        }
        return numeric;
    }

    protected int getPhoneId() {
        return mPhone.getPhoneId();
    }

    /* Reset Service state when IWLAN is enabled as polling in airplane mode
     * causes state to go to OUT_OF_SERVICE state instead of STATE_OFF
     */
    protected void resetServiceStateInIwlanMode() {
        if (mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            boolean resetIwlanRatVal = false;
            log("set service state as POWER_OFF");
            if (ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN
                        == mNewSS.getRilDataRadioTechnology()) {
                log("pollStateDone: mNewSS = " + mNewSS);
                log("pollStateDone: reset iwlan RAT value");
                resetIwlanRatVal = true;
            }
            mNewSS.setStateOff();
            if (resetIwlanRatVal) {
                mNewSS.setRilDataRadioTechnology(ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN);
                mNewSS.setDataRegState(ServiceState.STATE_IN_SERVICE);
                log("pollStateDone: mNewSS = " + mNewSS);
            }
        }
    }

    /**
     * Check if device is non-roaming and always on home network.
     *
     * @param b carrier config bundle obtained from CarrierConfigManager
     * @return true if network is always on home network, false otherwise
     * @see CarrierConfigManager
     */
    protected final boolean alwaysOnHomeNetwork(BaseBundle b) {
        return b.getBoolean(CarrierConfigManager.KEY_FORCE_HOME_NETWORK_BOOL);
    }

    /**
     * Check if the network identifier has membership in the set of
     * network identifiers stored in the carrier config bundle.
     *
     * @param b carrier config bundle obtained from CarrierConfigManager
     * @param network The network identifier to check network existence in bundle
     * @param key The key to index into the bundle presenting a string array of
     *            networks to check membership
     * @return true if network has membership in bundle networks, false otherwise
     * @see CarrierConfigManager
     */
    private boolean isInNetwork(BaseBundle b, String network, String key) {
        String[] networks = b.getStringArray(key);

        if (networks != null && Arrays.asList(networks).contains(network)) {
            return true;
        }
        return false;
    }

    protected final boolean isRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_GSM_ROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isNonRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_GSM_NONROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_CDMA_ROAMING_NETWORKS_STRING_ARRAY);
    }

    protected final boolean isNonRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, CarrierConfigManager.KEY_CDMA_NONROAMING_NETWORKS_STRING_ARRAY);
    }

    // MTK add Common
    protected int getPreferredNetworkModeSettings(int phoneId) {
        int networkType = -1;
        int subId[] = SubscriptionManager.getSubId(phoneId);
        if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
            networkType = PhoneFactory.calculatePreferredNetworkType(
                mPhone.getContext(), subId[0]);
        } else {
            log("Invalid subId, return invalid networkType");
        }
        return networkType;
    }
    /**
         * Set the Signal Strength for the phone.
         * @param ar The param include the Signal Strength.
         * @param isGsm Mark for the Signal Strength is gsm or not.
         */
    // MTK add Common
    protected void setSignalStrength(AsyncResult ar, boolean isGsm) {
        SignalStrength oldSignalStrength = mSignalStrength;
        if ((DBG) && (mLastSignalStrength != null)) {
            log("Before combine Signal Strength, setSignalStrength(): isGsm = "
                    + isGsm + " LastSignalStrength = "
                    + mLastSignalStrength.toString());
        }
        // This signal is used for both voice and data radio signal so parse
        // all fields
        if ((ar.exception == null) && (ar.result != null)) {
            mSignalStrength = (SignalStrength) ar.result;
            mSignalStrength.validateInput();
            mSignalStrength.setGsm(isGsm);
            if (DBG) {
                log("Before combine Signal Strength, setSignalStrength(): isGsm = "
                        + isGsm + "new mSignalStrength = "
                        + mSignalStrength.toString());
            }
        } else {
            log("Before combine Signal Strength, setSignalStrength() Exception from RIL : "
                    + ar.exception);
            mSignalStrength = new SignalStrength(isGsm);
        }
    }
    /// @}

    /// M: Add for VOLTE @{
    private final int getImsServiceState() {
        final Phone imsPhone = mPhone.getImsPhone();
        if (mPhone.isImsUseEnabled() && imsPhone != null && imsPhone.isVolteEnabled()) {
            return imsPhone.getServiceState().getState();
        }
        return ServiceState.STATE_OUT_OF_SERVICE;
    }
    /// @}

    /**
     * Return true if plmn is Home Plmn.
     * @param plmn
     * @return true is plmn is home plmn
     */
    public boolean isHPlmn(String plmn) {
        if (mPhone.isPhoneTypeGsm()) {
            //follow the behavior of modem, according to the length of plmn to compare mcc/mnc
            //ex: mccmnc: 334030 but plmn:33403 => still be HPLMN
            String mccmnc = getSIMOperatorNumeric();
            if (plmn == null) return false;

            if (mccmnc == null || mccmnc.equals("")) {
                log("isHPlmn getSIMOperatorNumeric error: " + mccmnc);
                return false;
            }

            if (plmn.equals(mccmnc)) {
                return true;
            } else {
                if (plmn.length() == 5 && mccmnc.length() == 6
                    && plmn.equals(mccmnc.substring(0, 5))) {
                    return true;
                }
            }

            /* ALPS01473952 check if plmn in customized EHPLMN table */
            if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                boolean isServingPlmnInGroup = false;
                boolean isHomePlmnInGroup = false;
                for (int i = 0; i < customEhplmn.length; i++) {
                    //reset flag
                    isServingPlmnInGroup = false;
                    isHomePlmnInGroup = false;

                    //check if target plmn or home plmn in this group
                    for (int j = 0; j < customEhplmn[i].length; j++) {
                        if (plmn.equals(customEhplmn[i][j])) {
                            isServingPlmnInGroup = true;
                        }
                        if (mccmnc.equals(customEhplmn[i][j])) {
                            isHomePlmnInGroup = true;
                        }
                    }

                    //if target plmn and home plmn both in the same group
                    if ((isServingPlmnInGroup == true) &&
                            (isHomePlmnInGroup == true)) {
                        log("plmn:" + plmn + "is in customized ehplmn table");
                        return true;
                    }
                }
            }
            /* ALPS01473952 END */

            return false;
        }
        return false;
    }


    /** Check if the device is shutting down. */
    public boolean isDeviceShuttingDown() {
        return mDeviceShuttingDown;
    }
}
