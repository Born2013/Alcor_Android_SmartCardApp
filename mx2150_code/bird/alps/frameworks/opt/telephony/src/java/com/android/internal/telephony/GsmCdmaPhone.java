/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.telephony.Rlog;
import android.util.Log;

import com.android.ims.ImsManager;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_DISABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ENABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ERASURE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_REGISTRATION;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL_CONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NO_REPLY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NOT_REACHABLE;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_BUSY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_UNCONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_VOICE;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE;
import static com.android.internal.telephony.TelephonyProperties.TERMINAL_BASED_CALL_WAITING_DISABLED;
import static com.android.internal.telephony.TelephonyProperties.TERMINAL_BASED_CALL_WAITING_ENABLED_OFF;
import static com.android.internal.telephony.TelephonyProperties.TERMINAL_BASED_CALL_WAITING_ENABLED_ON;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_UT_CFU_NOTIFICATION_MODE;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_DISABLED;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_ON;
import static com.android.internal.telephony.TelephonyProperties.UT_CFU_NOTIFICATION_MODE_OFF;

import com.android.ims.ImsException;
import com.android.ims.ImsReasonInfo;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.gsm.GsmMmiCode;
/// M: CC: Proprietary CRSS handling
import com.android.internal.telephony.gsm.SuppCrssNotification;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.android.internal.telephony.test.SimulatedRadioControl;
// MTK-START
import com.android.internal.telephony.uicc.CsimFileHandler;
// MTK-END
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccVmNotSupportedException;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
// MTK-START
import com.android.internal.telephony.uicc.IccFileHandler;
// MTK-END
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;

/// M: SS Ut part @{
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ISupplementaryServiceExt;
import com.mediatek.internal.telephony.OperatorUtils;
import com.mediatek.internal.telephony.OperatorUtils.OPID;
/// @}

// MTK-START
import com.mediatek.internal.telephony.uicc.CsimPhbStorageInfo;
// MTK-END

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
import com.android.internal.telephony.FeatureOption;
import com.android.internal.telephony.uicc.SIMRecords;
/*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
//[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
import com.mediatek.internal.telephony.ITelephonyEx;
import android.os.ServiceManager;
import android.os.RemoteException;
//[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END
/**
 * {@hide}
 */
public class GsmCdmaPhone extends Phone {
    // NOTE that LOG_TAG here is "GsmCdma", which means that log messages
    // from this file will go into the radio log rather than the main
    // log.  (Use "adb logcat -b radio" to see them.)
    public static final String LOG_TAG = "GsmCdmaPhone";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; /* STOPSHIP if true */

    //GSM
    // Key used to read/write voice mail number
    private static final String VM_NUMBER = "vm_number_key";
    // Key used to read/write the SIM IMSI used for storing the voice mail
    private static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    /** List of Registrants to receive Supplementary Service Notifications. */
    private RegistrantList mSsnRegistrants = new RegistrantList();

    //CDMA
    // Default Emergency Callback Mode exit timer
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    private static final String VM_NUMBER_CDMA = "vm_number_key_cdma";
    public static final int RESTART_ECM_TIMER = 0; // restart Ecm timer
    public static final int CANCEL_ECM_TIMER = 1; // cancel Ecm timer
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public int mCdmaSubscriptionSource = CdmaSubscriptionSourceManager.SUBSCRIPTION_SOURCE_UNKNOWN;
    public EriManager mEriManager;
    private PowerManager.WakeLock mWakeLock;
    // mEriFileLoadedRegistrants are informed after the ERI text has been loaded
    private final RegistrantList mEriFileLoadedRegistrants = new RegistrantList();
    // mEcmExitRespRegistrant is informed after the phone has been exited
    //the emergency callback mode
    //keep track of if phone is in emergency callback mode
    private boolean mIsPhoneInEcmState;
    private Registrant mEcmExitRespRegistrant;
    private String mEsn;
    private String mMeid;
    // string to define how the carrier specifies its own ota sp number
    private String mCarrierOtaSpNumSchema;

    /* M: SS part */
    private boolean needQueryCfu = true;

    /* For solving ALPS01023811
       To determine if CFU query is for power-on query.
    */
    private int mCfuQueryRetryCount = 0;
    private static final String CFU_QUERY_PROPERTY_NAME = "gsm.poweron.cfu.query.";
    private static final int cfuQueryWaitTime = 1000;
    private static final int CFU_QUERY_MAX_COUNT = 60;

    private static final String CFU_QUERY_ICCID_PROP = "persist.radio.cfu.iccid.";
    private static final String CFU_QUERY_SIM_CHANGED_PROP = "persist.radio.cfu.change.";
    public static final String IMS_DEREG_PROP = "gsm.radio.ss.imsdereg";

    private static final String SS_SERVICE_CLASS_PROP = "gsm.radio.ss.sc";

    public static final String IMS_DEREG_ON = "1";
    public static final String IMS_DEREG_OFF = "0";
    public static final int MESSAGE_SET_CF = 1;
    /* M: SS part end */

    /// M: SS Ut part @{
    SSRequestDecisionMaker mSSReqDecisionMaker;
    ISupplementaryServiceExt mSupplementaryServiceExt;
    public static final int TBCW_UNKNOWN = 0;
    public static final int TBCW_NOT_OPTBCW = 1;
    public static final int TBCW_OPTBCW_VOLTE_USER = 2;
    public static final int TBCW_OPTBCW_NOT_VOLTE_USER = 3;
    public static final int TBCW_OPTBCW_WITH_CS = 4;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE  = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };
    private int mTbcwMode = TBCW_UNKNOWN;

    public boolean mIsNetworkInitiatedUssr = false;
    /// @}

    /// M: CDMA LTE mode system property
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private static final boolean MTK_SVLTE_SUPPORT = (SystemProperties.getInt(
            PROP_MTK_CDMA_LTE_MODE, 0) == 1);

    // A runnable which is used to automatically exit from Ecm after a period of time.
    private Runnable mExitEcmRunnable = new Runnable() {
        @Override
        public void run() {
            exitEmergencyCallbackMode();
        }
    };
    public static final String PROPERTY_CDMA_HOME_OPERATOR_NUMERIC =
            "ro.cdma.home.operator.numeric";

    //CDMALTE
    /** PHONE_TYPE_CDMA_LTE in addition to RuimRecords needs access to SIMRecords and
     * IsimUiccRecords
     */
    private SIMRecords mSimRecords;

    //Common
    // Instance Variables
    private IsimUiccRecords mIsimUiccRecords;
    public GsmCdmaCallTracker mCT;
    public ServiceStateTracker mSST;
    private ArrayList <MmiCode> mPendingMMIs = new ArrayList<MmiCode>();
    private IccPhoneBookInterfaceManager mIccPhoneBookIntManager;

    private int mPrecisePhoneType;

    // mEcmTimerResetRegistrants are informed after Ecm timer is canceled or re-started
    private final RegistrantList mEcmTimerResetRegistrants = new RegistrantList();

    private String mImei;
    private String mImeiSv;
    private String mVmNumber;
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
    private boolean isFirstBoot = true;
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END

    /**
     * mDeviceIdAbnormal=0, Valid IMEI
     * mDeviceIdAbnormal=1, IMEI is null or not valid format
     * mDeviceIdAbnormal=2, Phone1/Phone2 have same IMEI
     */
    private int mDeviceIdAbnormal = 0;

    // Create Cfu (Call forward unconditional) so that dialing number &
    // mOnComplete (Message object passed by client) can be packed &
    // given as a single Cfu object as user data to RIL.
    private static class Cfu {
        final String mSetCfNumber;
        final Message mOnComplete;

        Cfu(String cfNumber, Message onComplete) {
            mSetCfNumber = cfNumber;
            mOnComplete = onComplete;
        }
    }

    private IccSmsInterfaceManager mIccSmsInterfaceManager;
    private IccCardProxy mIccCardProxy;

    private boolean mResetModemOnRadioTechnologyChange = false;

    private int mRilVersion;
    private boolean mBroadcastEmergencyCallStateChanges = false;


    /// M: CC: Proprietary CRSS handling @{
    RegistrantList mCallRelatedSuppSvcRegistrants = new RegistrantList();
    private AsyncResult mCachedSsn = null;
    private AsyncResult mCachedCrssn = null;
    /// @}

    // Constructors

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, int phoneId,
                        int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory) {
        this(context, ci, notifier, false, phoneId, precisePhoneType, telephonyComponentFactory);
    }

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier,
                        boolean unitTestMode, int phoneId, int precisePhoneType,
                        TelephonyComponentFactory telephonyComponentFactory) {
        super(precisePhoneType == PhoneConstants.PHONE_TYPE_GSM ? "GSM" : "CDMA",
                notifier, context, ci, unitTestMode, phoneId, telephonyComponentFactory);

        // phone type needs to be set before other initialization as other objects rely on it
        mPrecisePhoneType = precisePhoneType;
        initOnce(ci);
        initRatSpecific(precisePhoneType);
        mSST = mTelephonyComponentFactory.makeServiceStateTracker(this, this.mCi);
        // DcTracker uses SST so needs to be created after it is instantiated
        mDcTracker = mTelephonyComponentFactory.makeDcTracker(this);
        mSST.registerForNetworkAttached(this, EVENT_REGISTERED_TO_NETWORK, null);
        logd("GsmCdmaPhone: constructor: sub = " + mPhoneId);
        //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
        Settings.System.putInt(context.getContentResolver(), "birdunlockchina", 0);
		Settings.System.putInt(context.getContentResolver(), "birdsimchina", 0);
		Settings.System.putInt(mContext.getContentResolver(), "birdtranslock", 0);
		//[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                sendMessage(obtainMessage(EVENT_CARRIER_CONFIG_CHANGED));
            }
            /* M: SS part */
            else if (TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED.equals(action)) {
                SubscriptionManager subMgr = SubscriptionManager.from(mContext);
                SubscriptionInfo mySubInfo = null;
                if (subMgr != null) {
                    mySubInfo = subMgr.getActiveSubscriptionInfo(getSubId());
                }

                String mySettingName = CFU_QUERY_ICCID_PROP + getPhoneId();
                String oldIccId = SystemProperties.get(mySettingName, "");

                String defaultQueryCfuMode = PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE;
                if (mSupplementaryServiceExt != null) {
                    defaultQueryCfuMode = mSupplementaryServiceExt.getOpDefaultQueryCfuMode();
                    Rlog.d(LOG_TAG, "defaultQueryCfuMode = " + defaultQueryCfuMode);
                }
                String cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP,
                                defaultQueryCfuMode);

                if ((mySubInfo != null) && (mySubInfo.getIccId() != null)) {
                    if (!mySubInfo.getIccId().equals(oldIccId)) {
                        Rlog.w(LOG_TAG, " mySubId " + getSubId() + " mySettingName "
                                + mySettingName + " old iccid : " + oldIccId + " new iccid : "
                                + mySubInfo.getIccId());
                        SystemProperties.set(mySettingName, mySubInfo.getIccId());
                        String isChanged = CFU_QUERY_SIM_CHANGED_PROP + getPhoneId();
                        SystemProperties.set(isChanged, "1");
                        needQueryCfu = true;
                        setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                        setTbcwMode(TBCW_UNKNOWN);  //reset to unknow due to sim change.
                        setSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                                TERMINAL_BASED_CALL_WAITING_DISABLED);
                        /// M: SS OP01 Ut
                        saveTimeSlot(null);

                        // Remove the CLIR setting for new SIM
                        SharedPreferences sp =
                                PreferenceManager.getDefaultSharedPreferences(getContext());
                        int clirSetting = sp.getInt(CLIR_KEY + getPhoneId(), -1);
                        if (clirSetting != -1) {
                            SharedPreferences.Editor editor = sp.edit();
                            editor.remove(CLIR_KEY + getPhoneId());
                            if (!editor.commit()) {
                                Rlog.e(LOG_TAG, "failed to commit the removal of CLIR preference");
                            }
                        }

                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);

                        // When we get Icc ID later than REGISTERED_TO_NETWORK, need to query CFU
                        if (mSST != null && mSST.mSS != null
                                && (mSST.mSS.getState() == ServiceState.STATE_IN_SERVICE)) {
                            Rlog.w(LOG_TAG, "Send EVENT_QUERY_CFU");
                            Message msgQueryCfu = obtainMessage(EVENT_QUERY_CFU);
                            sendMessage(msgQueryCfu);
                        }
                    } else if (cfuSetting.equals("2")) {
                        Rlog.i(LOG_TAG, "Always query CFU.");
                        if (mSST != null && mSST.mSS != null
                                && (mSST.mSS.getState() == ServiceState.STATE_IN_SERVICE)) {
                            needQueryCfu = true;
                            Message msgQueryCfu = obtainMessage(EVENT_QUERY_CFU);
                            sendMessage(msgQueryCfu);
                        }
                    }
                }
                Rlog.d(LOG_TAG, "onReceive(): ACTION_SUBINFO_RECORD_UPDATED: mTbcwMode = "
                        + mTbcwMode);
                if ((mTbcwMode == TBCW_UNKNOWN) && (isIccCardMncMccAvailable(getPhoneId()))) {
                    if (isOpTbcwWithCS(getPhoneId())) {
                        setTbcwMode(TBCW_OPTBCW_WITH_CS);
                        setTbcwToEnabledOnIfDisabled();
                    }
                }
            } else if (action.equals(ImsManager.ACTION_IMS_STATE_CHANGED)) {
                int reg = intent.getIntExtra(ImsManager.EXTRA_IMS_REG_STATE_KEY , -1);
                int slotId = intent.getIntExtra(ImsManager.EXTRA_PHONE_ID, -1);
                Rlog.d(LOG_TAG, "onReceive ACTION_IMS_STATE_CHANGED: reg=" + reg
                        + ", SimID=" + slotId);
                if (slotId == getPhoneId() && (reg == ServiceState.STATE_IN_SERVICE)) {
                    if (isOpTbcwWithCS(getPhoneId())) {
                        setTbcwMode(TBCW_OPTBCW_WITH_CS);
                        setTbcwToEnabledOnIfDisabled();
                    } else {
                        // TBCW for VoLTE user
                        setTbcwMode(TBCW_OPTBCW_VOLTE_USER);
                        setTbcwToEnabledOnIfDisabled();
                    }

                    Rlog.i(LOG_TAG, "needQueryCfu for IMS CFU status.");
                    needQueryCfu = true;
                    Message msgQueryCfu = obtainMessage(EVENT_QUERY_CFU);
                    sendMessage(msgQueryCfu);
                }

                /// M: Service State should be notified when ims state has changed @{
                if (mSST == null ||
                        (mSST.mSS.getState() != ServiceState.STATE_IN_SERVICE &&
                        mSST.mSS.getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
                    notifyServiceStateChanged(mSST.mSS);
                }
                /// @}
            } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)) {
                Rlog.d(LOG_TAG, "set needQueryCfu to true.");
                needQueryCfu = true;
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean bAirplaneModeOn = intent.getBooleanExtra("state", false);

                Rlog.d(LOG_TAG, "ACTION_AIRPLANE_MODE_CHANGED, bAirplaneModeOn = " +
                       bAirplaneModeOn);
                if (bAirplaneModeOn) {
                    Rlog.d(LOG_TAG, "Set needQueryCfu true.");
                    needQueryCfu = true;
                }
            }
            /* M: SS part end */
        }
    };

    private void initOnce(CommandsInterface ci) {
        if (ci instanceof SimulatedRadioControl) {
            mSimulatedRadioControl = (SimulatedRadioControl) ci;
        }

        mCT = mTelephonyComponentFactory.makeGsmCdmaCallTracker(this);
        mIccPhoneBookIntManager = mTelephonyComponentFactory.makeIccPhoneBookInterfaceManager(this);
        PowerManager pm
                = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mIccSmsInterfaceManager = mTelephonyComponentFactory.makeIccSmsInterfaceManager(this);
        mIccCardProxy = mTelephonyComponentFactory.makeIccCardProxy(mContext, mCi, mPhoneId);

        /// M: SS Ut part @{
        mSSReqDecisionMaker = new SSRequestDecisionMaker(mContext, this);

        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                mSupplementaryServiceExt = MPlugin.createInstance(
                        ISupplementaryServiceExt.class.getName(), mContext);
                if (mSupplementaryServiceExt != null) {
                    mSupplementaryServiceExt.registerReceiver(mContext, mPhoneId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /// @}

        mCi.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
        mCi.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        mCi.registerForOn(this, EVENT_RADIO_ON, null);
        mCi.setOnSuppServiceNotification(this, EVENT_SSN, null);

        //GSM
        mCi.setOnUSSD(this, EVENT_USSD, null);
        mCi.setOnSs(this, EVENT_SS, null);

        /// M: CC: Proprietary incoming call handling
        mCT.registerForVoiceCallIncomingIndication(this,
                EVENT_VOICE_CALL_INCOMING_INDICATION, null);

        /// M: CC: Proprietary CRSS handling
        mCi.setOnCallRelatedSuppSvc(this, EVENT_CRSS_IND, null);

        //CDMA
        mCdmaSSM = mTelephonyComponentFactory.getCdmaSubscriptionSourceManagerInstance(mContext,
                mCi, this, EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
        mEriManager = mTelephonyComponentFactory.makeEriManager(this, mContext,
                EriManager.ERI_FROM_XML);
        mCi.setEmergencyCallbackMode(this, EVENT_EMERGENCY_CALLBACK_MODE_ENTER, null);
        mCi.registerForExitEmergencyCallbackMode(this, EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE,
                null);
        // get the string that specifies the carrier OTA Sp number
        mCarrierOtaSpNumSchema = TelephonyManager.from(mContext).getOtaSpNumberSchemaForPhone(
                getPhoneId(), "");

        mResetModemOnRadioTechnologyChange = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_RESET_ON_RADIO_TECH_CHANGE, false);

        mCi.registerForRilConnected(this, EVENT_RIL_CONNECTED, null);
        mCi.registerForVoiceRadioTechChanged(this, EVENT_VOICE_RADIO_TECH_CHANGED, null);
        // mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(
        //         CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));
        final IntentFilter filter = new IntentFilter();
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        filter.addAction(ImsManager.ACTION_IMS_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);

    }

    private void initRatSpecific(int precisePhoneType) {
        mPendingMMIs.clear();
        // mIccPhoneBookIntManager.updateIccRecords(null); // ALPS02819064
        //todo: maybe not needed?? should the count also be updated on sim_state_absent?
        mVmCount = 0;
        mEsn = null;
        mMeid = null;

        mPrecisePhoneType = precisePhoneType;

        TelephonyManager tm = TelephonyManager.from(mContext);
        if (isPhoneTypeGsm()) {
            mCi.setPhoneType(PhoneConstants.PHONE_TYPE_GSM);
            tm.setPhoneType(getPhoneId(), PhoneConstants.PHONE_TYPE_GSM);
            mIccCardProxy.setVoiceRadioTech(ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);
        } else {
            mCdmaSubscriptionSource = CdmaSubscriptionSourceManager.SUBSCRIPTION_SOURCE_UNKNOWN;
            // This is needed to handle phone process crashes
            /// M: Get the property by phoneId @{
            //String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "false");
            String inEcm = getSystemProperty(TelephonyProperties.PROPERTY_INECM_MODE, "false");
            /// @}
            mIsPhoneInEcmState = inEcm.equals("true");
            if (mIsPhoneInEcmState) {
                // Send a message which will invoke handleExitEmergencyCallbackMode
                mCi.exitEmergencyCallbackMode(
                        obtainMessage(EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE));
            }

            mCi.setPhoneType(PhoneConstants.PHONE_TYPE_CDMA);
            tm.setPhoneType(getPhoneId(), PhoneConstants.PHONE_TYPE_CDMA);
            mIccCardProxy.setVoiceRadioTech(ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT);
            // Sets operator properties by retrieving from build-time system property
            String operatorAlpha = SystemProperties.get("ro.cdma.home.operator.alpha");
            String operatorNumeric = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
            logd("init: operatorAlpha='" + operatorAlpha
                    + "' operatorNumeric='" + operatorNumeric + "'");
            if (mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP) ==
                    null || isPhoneTypeCdmaLte()) {
                if (!TextUtils.isEmpty(operatorAlpha)) {
                    logd("init: set 'gsm.sim.operator.alpha' to operator='" + operatorAlpha + "'");
                    tm.setSimOperatorNameForPhone(mPhoneId, operatorAlpha);
                }
                if (!TextUtils.isEmpty(operatorNumeric)) {
                    logd("init: set 'gsm.sim.operator.numeric' to operator='" + operatorNumeric +
                            "'");
                    logd("update icc_operator_numeric=" + operatorNumeric);
                    tm.setSimOperatorNumericForPhone(mPhoneId, operatorNumeric);

                    SubscriptionController.getInstance().setMccMnc(operatorNumeric, getSubId());
                    // Sets iso country property by retrieving from build-time system property
                    setIsoCountryProperty(operatorNumeric);
                    // Updates MCC MNC device configuration information
                    logd("update mccmnc=" + operatorNumeric);
                    MccTable.updateMccMncConfiguration(mContext, operatorNumeric, false);
                }
            }

            // Sets current entry in the telephony carrier table
            updateCurrentCarrierInProvider(operatorNumeric);
        }
    }

    //CDMA
    /**
     * Sets PROPERTY_ICC_OPERATOR_ISO_COUNTRY property
     *
     */
    private void setIsoCountryProperty(String operatorNumeric) {
        TelephonyManager tm = TelephonyManager.from(mContext);
        if (TextUtils.isEmpty(operatorNumeric)) {
            logd("setIsoCountryProperty: clear 'gsm.sim.operator.iso-country'");
            tm.setSimCountryIsoForPhone(mPhoneId, "");
        } else {
            String iso = "";
            try {
                iso = MccTable.countryCodeForMcc(Integer.parseInt(
                        operatorNumeric.substring(0,3)));
            } catch (NumberFormatException ex) {
                Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", ex);
            } catch (StringIndexOutOfBoundsException ex) {
                Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", ex);
            }

            logd("setIsoCountryProperty: set 'gsm.sim.operator.iso-country' to iso=" + iso);
            tm.setSimCountryIsoForPhone(mPhoneId, iso);
        }
    }

    public boolean isPhoneTypeGsm() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_GSM;
    }

    public boolean isPhoneTypeCdma() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_CDMA;
    }

    public boolean isPhoneTypeCdmaLte() {
        return mPrecisePhoneType == PhoneConstants.PHONE_TYPE_CDMA_LTE;
    }

    private void switchPhoneType(int precisePhoneType) {
        removeCallbacks(mExitEcmRunnable);

        initRatSpecific(precisePhoneType);

        mSST.updatePhoneType();
        setPhoneName(precisePhoneType == PhoneConstants.PHONE_TYPE_GSM ? "GSM" : "CDMA");
        onUpdateIccAvailability();
        mCT.updatePhoneType();

        CommandsInterface.RadioState radioState = mCi.getRadioState();
        if (radioState.isAvailable()) {
            handleRadioAvailable();
            if (radioState.isOn()) {
                handleRadioOn();
            }
        }
        if (!radioState.isAvailable() || !radioState.isOn()) {
            handleRadioOffOrNotAvailable();
        }
    }

    @Override
    protected void finalize() {
        if(DBG) logd("GsmCdmaPhone finalized");
        if (mWakeLock.isHeld()) {
            Rlog.e(LOG_TAG, "UNEXPECTED; mWakeLock is held when finalizing.");
            mWakeLock.release();
        }
    }

    @Override
    public ServiceState getServiceState() {
        /// M: For IMS @{
        /// IMS service state is reliable only when data registration state is in service
        // if (mSST == null || mSST.mSS.getState() != ServiceState.STATE_IN_SERVICE) {
        if (mSST == null
                || (mSST.mSS.getState() != ServiceState.STATE_IN_SERVICE && mSST.mSS
                        .getDataRegState() == ServiceState.STATE_IN_SERVICE)) {
            /// ALPS02500449, Fix timing issue.
            /// Keep reference for mImsPhone to avoid null pointer exception
            /// if mImsPhone sets to null by other thread. @{
            Phone phone = mImsPhone;
            if (phone != null) {
                return ServiceState.mergeServiceStates(
                        (mSST == null) ? new ServiceState() : mSST.mSS,
                        phone.getServiceState());
            }
            /// @}
        }

        if (mSST != null) {
            return mSST.mSS;
        } else {
            // avoid potential NPE in EmergencyCallHelper during Phone switch
            return new ServiceState();
        }
    }

    @Override
    public CellLocation getCellLocation() {
        if (isPhoneTypeGsm()) {
            return mSST.getCellLocation();
        } else {
            CdmaCellLocation loc = (CdmaCellLocation)mSST.mCellLoc;

            int mode = Settings.Secure.getInt(getContext().getContentResolver(),
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            if (mode == Settings.Secure.LOCATION_MODE_OFF) {
                // clear lat/long values for location privacy
                CdmaCellLocation privateLoc = new CdmaCellLocation();
                privateLoc.setCellLocationData(loc.getBaseStationId(),
                        CdmaCellLocation.INVALID_LAT_LONG,
                        CdmaCellLocation.INVALID_LAT_LONG,
                        loc.getSystemId(), loc.getNetworkId());
                loc = privateLoc;
            }
            return loc;
        }
    }

    @Override
    public PhoneConstants.State getState() {
        if (mImsPhone != null) {
            PhoneConstants.State imsState = mImsPhone.getState();
            if (imsState != PhoneConstants.State.IDLE) {
                return imsState;
            }
        }

        return mCT.mState;
    }

    @Override
    public int getPhoneType() {
        if (mPrecisePhoneType == PhoneConstants.PHONE_TYPE_GSM) {
            return PhoneConstants.PHONE_TYPE_GSM;
        } else {
            return PhoneConstants.PHONE_TYPE_CDMA;
        }
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return mSST;
    }

    @Override
    public CallTracker getCallTracker() {
        return mCT;
    }

    @Override
    public void updateVoiceMail() {
        if (isPhoneTypeGsm()) {
            int countVoiceMessages = 0;
            IccRecords r = mIccRecords.get();
            if (r != null) {
                // get voice mail count from SIM
                countVoiceMessages = r.getVoiceMessageCount();
            }
            int countVoiceMessagesStored = getStoredVoiceMessageCount();
            if (countVoiceMessages == -1 && countVoiceMessagesStored != 0) {
                countVoiceMessages = countVoiceMessagesStored;
            //[MTK][ALPS03346103][语音信箱问题][yangheng][20170605] BEGIN
            } else if(countVoiceMessages == -1 && countVoiceMessagesStored == 0 && FeatureOption.BIRD_VOICE_MESSAGE_CHANGE) {
                countVoiceMessages = 0;
            //[MTK][ALPS03346103][语音信箱问题][yangheng][20170605] END
            }
            logd("updateVoiceMail countVoiceMessages = " + countVoiceMessages
                    + " subId " + getSubId());
            setVoiceMessageCount(countVoiceMessages);
        } else {
            setVoiceMessageCount(getStoredVoiceMessageCount());
        }
    }

    @Override
    public List<? extends MmiCode>
    getPendingMmiCodes() {
        /// M: @{
        Rlog.d(LOG_TAG, "getPendingMmiCodes");
        dumpPendingMmi();

        ImsPhone imsPhone = (ImsPhone)mImsPhone;
        ArrayList<MmiCode> imsphonePendingMMIs = new ArrayList<MmiCode>();
        if (imsPhone != null
                && imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE) {
            List<ImsPhoneMmiCode> imsMMIs
                    = (List<ImsPhoneMmiCode>) imsPhone.getPendingMmiCodes();
            for(ImsPhoneMmiCode mmi : imsMMIs) {
                imsphonePendingMMIs.add((MmiCode) mmi);
            }
        }
        ArrayList<MmiCode> allPendingMMIs = new ArrayList<MmiCode>(mPendingMMIs);
        allPendingMMIs.addAll(imsphonePendingMMIs);
        Rlog.d(LOG_TAG, "allPendingMMIs.size() = " + allPendingMMIs.size());

        for (int i=0, s=allPendingMMIs.size(); i<s; i++) {
            Rlog.d(LOG_TAG, "dump allPendingMMIs: " + allPendingMMIs.get(i));
        }

        return allPendingMMIs;
        // return mPendingMMIs;
        /// @}
    }

    private static final boolean MTK_IMS_SUPPORT = SystemProperties.get("persist.mtk_ims_support")
                                                               .equals("1");

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;

        if (mSST == null) {
            // Radio Technology Change is ongoning, dispose() and removeReferences() have
            // already been called

            ret = PhoneConstants.DataState.DISCONNECTED;
        } else if (mSST.getCurrentDataConnectionState() != ServiceState.STATE_IN_SERVICE
                && (isPhoneTypeCdma() ||
                (isPhoneTypeGsm() && !apnType.equals(PhoneConstants.APN_TYPE_EMERGENCY)))) {
            // If we're out of service, open TCP sockets may still work
            // but no data will flow

            // Emergency APN is available even in Out Of Service
            // Pass the actual State of EPDN

            logd("getDataConnectionState: dataConnectionState is not in service");
            if (MTK_IMS_SUPPORT && apnType.equals(PhoneConstants.APN_TYPE_IMS)) {
                switch (mDcTracker.getState(apnType)) {
                case RETRYING:
                   logd("getDataConnectionState: apnType: " + apnType
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
                   ret = PhoneConstants.DataState.DISCONNECTED;
                   break;
                };
            } else {
               ret = PhoneConstants.DataState.DISCONNECTED;
            }
        } else { /* mSST.gprsState == ServiceState.STATE_IN_SERVICE */
            switch (mDcTracker.getState(apnType)) {
                case RETRYING:
                case FAILED:
                case IDLE:
                    ret = PhoneConstants.DataState.DISCONNECTED;
                break;

                case CONNECTED:
                case DISCONNECTING:
                    if ( mCT.mState != PhoneConstants.State.IDLE
                            && !mSST.isConcurrentVoiceAndDataAllowed()) {
                        ret = PhoneConstants.DataState.SUSPENDED;
                    } else {
                        ret = PhoneConstants.DataState.CONNECTED;
                    }

                    // M: check peer phone is in call also
                    int phoneCount = TelephonyManager.getDefault().getPhoneCount();
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        for (int i = 0; i < phoneCount; i++) {
                            Phone pf = PhoneFactory.getPhone(i);

                            if (pf != null && i != getPhoneId() &&
                                    pf.getState() != PhoneConstants.State.IDLE) {
                                logd("getDataConnectionState: Phone[" + getPhoneId() + "] Phone"
                                        + i + " is in call.");
                                if (MTK_SVLTE_SUPPORT) {
                                    int phoneType = pf.getPhoneType();
                                    int rilRat = getServiceState().getRilDataRadioTechnology();
                                    logd("getDataConnectionState: SVLTE, phoneType: " + phoneType
                                            + " rilRat: " + rilRat);

                                    /// M: Data setup on GSM and Peer phone calling also on GSM.
                                    ///    Need to suspend for this case although these codes may
                                    ///    never be executed. (Ex: CT 4G + CMCC, call on CMCC and
                                    ///    then enable data on CT SIM.)
                                    if (phoneType == PhoneConstants.PHONE_TYPE_GSM
                                            && ServiceState.isGsm(rilRat)) {
                                        ret = PhoneConstants.DataState.SUSPENDED;
                                    }
                                } else {
                                    logd("getDataConnectionState: set Data state as SUSPENDED");
                                    ret = PhoneConstants.DataState.SUSPENDED;
                                }
                                break;
                            }
                        }
                    }

                    //ALPS01454896: If default data is disable, and current state is disconnecting
                    //we don't have to show the data icon.
                    if (ret == PhoneConstants.DataState.CONNECTED &&
                                apnType == PhoneConstants.APN_TYPE_DEFAULT &&
                                mDcTracker.getState(apnType) == DctConstants.State.DISCONNECTING &&
                                !mDcTracker.getDataEnabled()) {
                        logd("getDataConnectionState: Connected but default data is not open.");
                        ret = PhoneConstants.DataState.DISCONNECTED;
                    }
                break;

                case CONNECTING:
                case SCANNING:
                    ret = PhoneConstants.DataState.CONNECTING;
                break;
            }
        }

        logd("getDataConnectionState apnType=" + apnType + " ret=" + ret);
        return ret;
    }

    @Override
    public DataActivityState getDataActivityState() {
        DataActivityState ret = DataActivityState.NONE;

        if (mSST.getCurrentDataConnectionState() == ServiceState.STATE_IN_SERVICE) {
            switch (mDcTracker.getActivity()) {
                case DATAIN:
                    ret = DataActivityState.DATAIN;
                break;

                case DATAOUT:
                    ret = DataActivityState.DATAOUT;
                break;

                case DATAINANDOUT:
                    ret = DataActivityState.DATAINANDOUT;
                break;

                case DORMANT:
                    ret = DataActivityState.DORMANT;
                break;

                default:
                    ret = DataActivityState.NONE;
                break;
            }
        }

        return ret;
    }

    /**
     * Notify any interested party of a Phone state change
     * {@link com.android.internal.telephony.PhoneConstants.State}
     */
    public void notifyPhoneStateChanged() {
        mNotifier.notifyPhoneState(this);
    }

    /**
     * Notify registrants of a change in the call state. This notifies changes in
     * {@link com.android.internal.telephony.Call.State}. Use this when changes
     * in the precise call state are needed, else use notifyPhoneStateChanged.
     */
    public void notifyPreciseCallStateChanged() {
        /* we'd love it if this was package-scoped*/
        super.notifyPreciseCallStateChangedP();
    }

    public void notifyNewRingingConnection(Connection c) {
        super.notifyNewRingingConnectionP(c);
    }

    public void notifyDisconnect(Connection cn) {
        mDisconnectRegistrants.notifyResult(cn);

        mNotifier.notifyDisconnectCause(cn.getDisconnectCause(), cn.getPreciseDisconnectCause());
    }

    public void notifyUnknownConnection(Connection cn) {
        super.notifyUnknownConnectionP(cn);
    }

    @Override
    public boolean isInEmergencyCall() {
        if (isPhoneTypeGsm()) {
            return false;
        } else {
            return mCT.isInEmergencyCall();
        }
    }

    @Override
    protected void setIsInEmergencyCall() {
        if (!isPhoneTypeGsm()) {
            mCT.setIsInEmergencyCall();
        }
    }

    @Override
    public boolean isInEcm() {
        if (isPhoneTypeGsm()) {
            return false;
        } else {
            return mIsPhoneInEcmState;
        }
    }

    // MTK-START
    @Override
    public void queryPhbStorageInfo(int type, Message response) {
        if (isPhoneTypeGsm()) {
            mCi.queryPhbStorageInfo(type, response);
        } else {
            // M: SIM PHB for C2K
            IccFileHandler fh;
            fh = getIccFileHandler();
            if ((fh != null) && (fh instanceof CsimFileHandler)) {
                CsimPhbStorageInfo.checkPhbRecordInfo(response);
            } else {
                mCi.queryPhbStorageInfo(type, response);
            }
            Rlog.d(LOG_TAG, "queryPhbStorageInfo IccFileHandler" + fh);
        }
    }
    // MTK-END

    //CDMA
    private void sendEmergencyCallbackModeChange(){
        //Send an Intent
        Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intent.putExtra(PhoneConstants.PHONE_IN_ECM_STATE, mIsPhoneInEcmState);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.USER_ALL);
        if (DBG) logd("sendEmergencyCallbackModeChange");
    }

    @Override
    public void sendEmergencyCallStateChange(boolean callActive) {
        if (mBroadcastEmergencyCallStateChanges) {
            Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALL_STATE_CHANGED);
            intent.putExtra(PhoneConstants.PHONE_IN_EMERGENCY_CALL, callActive);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
            ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.USER_ALL);
            if (DBG) Rlog.d(LOG_TAG, "sendEmergencyCallStateChange");
        }
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        mBroadcastEmergencyCallStateChanges = broadcast;
    }

    public void notifySuppServiceFailed(SuppService code) {
        mSuppServiceFailedRegistrants.notifyResult(code);
    }

    public void notifyServiceStateChanged(ServiceState ss) {
        super.notifyServiceStateChangedP(ss);
    }

    public void notifyLocationChanged() {
        mNotifier.notifyCellLocation(this);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        TelephonyManager tm = TelephonyManager.from(mContext);
        int simState = tm.getSimState(mPhoneId);
        Rlog.d(LOG_TAG, "notifyCallForwardingIndicator: " + simState);
        if (simState == TelephonyManager.SIM_STATE_READY) {
            mNotifier.notifyCallForwardingChanged(this);
        }
    }

    // override for allowing access from other classes of this package
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSystemProperty(String property, String value) {
        if (getUnitTestMode()) {
            return;
        }
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            TelephonyManager.setTelephonyProperty(mPhoneId, property, value);
        } else {
            super.setSystemProperty(property, value);
        }
    }

    @Override
    public void registerForSuppServiceNotification(
            Handler h, int what, Object obj) {
        mSsnRegistrants.addUnique(h, what, obj);

        /// M: CC: Proprietary CRSS handling @{
        // Do not enable or disable CSSN since it is already enabled in RIL initial callback.
        //if (mSsnRegistrants.size() == 1) mCi.setSuppServiceNotifications(true, null);
        if (mCachedSsn != null) {
            mSsnRegistrants.notifyRegistrants(mCachedSsn);
            mCachedSsn = null;
        }
        /// @}
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        mSsnRegistrants.remove(h);
        /// M: CC: Proprietary CRSS handling @{
        // Do not enable or disable CSSN since it is already enabled in RIL initial callback.
        //if (mSsnRegistrants.size() == 0) mCi.setSuppServiceNotifications(false, null);
        mCachedSsn = null;
        /// @}
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        mSimRecordsLoadedRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        mSimRecordsLoadedRegistrants.remove(h);
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        Phone imsPhone = mImsPhone;
        if ( imsPhone != null && imsPhone.getRingingCall().isRinging() ) {
            imsPhone.acceptCall(videoState);
        } else {
            /// M: CC: For 3G VT only @{
            //mCT.acceptCall();
            mCT.acceptCall(videoState);
            /// @}
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public String getIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            // to get ICCID form SIMRecords because it is on MF.
            r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP);
        }
        return (r != null) ? r.getIccId() : null;
    }

    @Override
    public String getFullIccSerialNumber() {
        IccRecords r = mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            // to get ICCID form SIMRecords because it is on MF.
            r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP);
        }
        return (r != null) ? r.getFullIccId() : null;
    }

    @Override
    public boolean canConference() {
        if (mImsPhone != null && mImsPhone.canConference()) {
            return true;
        }
        if (isPhoneTypeGsm()) {
            return mCT.canConference();
        } else {
            loge("canConference: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void conference() {
        if (mImsPhone != null && mImsPhone.canConference()) {
            logd("conference() - delegated to IMS phone");
            try {
                mImsPhone.conference();
            } catch (CallStateException e) {
                loge(e.toString());
            }
            return;
        }
        if (isPhoneTypeGsm()) {
            mCT.conference();
        } else {
            // three way calls in CDMA will be handled by feature codes
            loge("conference: not possible in CDMA");
        }
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("enableEnhancedVoicePrivacy: not expected on GSM");
        } else {
            mCi.setPreferredVoicePrivacy(enable, onComplete);
        }
    }

    @Override
    public void getEnhancedVoicePrivacy(Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("getEnhancedVoicePrivacy: not expected on GSM");
        } else {
            mCi.getPreferredVoicePrivacy(onComplete);
        }
    }

    @Override
    public void clearDisconnected() {
        mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return mCT.canTransfer();
        } else {
            loge("canTransfer: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void explicitCallTransfer() {
        if (isPhoneTypeGsm()) {
            mCT.explicitCallTransfer();
        } else {
            loge("explicitCallTransfer: not possible in CDMA");
        }
    }

    @Override
    public GsmCdmaCall getForegroundCall() {
        return mCT.mForegroundCall;
    }

    @Override
    public GsmCdmaCall getBackgroundCall() {
        return mCT.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        Phone imsPhone = mImsPhone;
        // It returns the ringing call of ImsPhone if the ringing call of GSMPhone isn't ringing.
        // In CallManager.registerPhone(), it always registers ringing call of ImsPhone, because
        // the ringing call of GSMPhone isn't ringing. Consequently, it can't answer GSM call
        // successfully by invoking TelephonyManager.answerRingingCall() since the implementation
        // in PhoneInterfaceManager.answerRingingCallInternal() could not get the correct ringing
        // call from CallManager. So we check the ringing call state of imsPhone first as
        // accpetCall() does.
        if ( imsPhone != null && imsPhone.getRingingCall().isRinging()) {
            return imsPhone.getRingingCall();
        }
        return mCT.mRingingCall;
    }

    /// M: CC: Use 0+SEND MMI to release held calls or sets UDUB
    // (User Determined User Busy) for a waiting call. @{
    // 3GPP 22.030 6.5.5
    private boolean handleUdubIncallSupplementaryService(
            String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (getRingingCall().getState() != GsmCdmaCall.State.IDLE ||
                getBackgroundCall().getState() != GsmCdmaCall.State.IDLE) {
            if (DBG) Rlog.d(LOG_TAG,
                    "MmiCode 0: hangupWaitingOrBackground");
            mCT.hangupWaitingOrBackground();
        }

        return true;
    }
    /// @}

    private boolean handleCallDeflectionIncallSupplementaryService(
            String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (getRingingCall().getState() != GsmCdmaCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: rejectCall");
            try {
                mCT.rejectCall();
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG,
                        "reject failed", e);
                notifySuppServiceFailed(Phone.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != GsmCdmaCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: hangupWaitingOrBackground");
            mCT.hangupWaitingOrBackground();
        }

        return true;
    }

    //GSM
    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        GsmCdmaCall call = getForegroundCall();

        try {
            if (len > 1) {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';

                if (callIndex >= 1 && callIndex <= GsmCdmaCallTracker.MAX_CONNECTIONS_GSM) {
                    if (DBG) logd("MmiCode 1: hangupConnectionByIndex " + callIndex);
                    mCT.hangupConnectionByIndex(call, callIndex);
                }
            } else {
                if (call.getState() != GsmCdmaCall.State.IDLE) {
                    if (DBG) logd("MmiCode 1: hangup foreground");
                    //mCT.hangupForegroundResumeBackground();
                    mCT.hangup(call);
                } else {
                    if (DBG) logd("MmiCode 1: switchWaitingOrHoldingAndActive");
                    mCT.switchWaitingOrHoldingAndActive();
                }
            }
        } catch (CallStateException e) {
            if (DBG) Rlog.d(LOG_TAG,
                    "hangup failed", e);
            notifySuppServiceFailed(Phone.SuppService.HANGUP);
        }

        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        GsmCdmaCall call = getForegroundCall();

        if (len > 1) {
            try {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                GsmCdmaConnection conn = mCT.getConnectionByIndex(call, callIndex);

                // GsmCdma index starts at 1, up to 5 connections in a call,
                if (conn != null && callIndex >= 1 && callIndex <= GsmCdmaCallTracker.MAX_CONNECTIONS_GSM) {
                    if (DBG) logd("MmiCode 2: separate call " + callIndex);
                    mCT.separate(conn);
                } else {
                    if (DBG) logd("separate: invalid call index " + callIndex);
                    notifySuppServiceFailed(Phone.SuppService.SEPARATE);
                }
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "separate failed", e);
                notifySuppServiceFailed(Phone.SuppService.SEPARATE);
            }
        } else {
            try {
                if (getRingingCall().getState() != GsmCdmaCall.State.IDLE) {
                    if (DBG) logd("MmiCode 2: accept ringing call");
                    mCT.acceptCall();
                } else {
                    if (DBG) logd("MmiCode 2: switchWaitingOrHoldingAndActive");
                    mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "switch failed", e);
                notifySuppServiceFailed(Phone.SuppService.SWITCH);
            }
        }

        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (DBG) logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {

        int len = dialString.length();

        if (len != 1) {
            return false;
        }

        if (DBG) logd("MmiCode 4: explicit call transfer");
        explicitCallTransfer();
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        // Treat it as an "unknown" service.
        notifySuppServiceFailed(Phone.SuppService.UNKNOWN);
        return true;
    }

    /// M: CC: Check GSM call state to avoid InCallMMI dispatching to IMS @{
    // [ALPS02516173],[ALPS02615800]
    public Call getCSRingingCall() {
        return mCT.mRingingCall;
    }

    boolean isInCSCall() {
        GsmCdmaCall.State foregroundCallState = getForegroundCall().getState();
        GsmCdmaCall.State backgroundCallState = getBackgroundCall().getState();
        GsmCdmaCall.State ringingCallState = getCSRingingCall().getState();

       return (foregroundCallState.isAlive() ||
                backgroundCallState.isAlive() ||
                ringingCallState.isAlive());
    }
    /// @}

    @Override
    public boolean handleInCallMmiCommands(String dialString) throws CallStateException {
        if (!isPhoneTypeGsm()) {
            loge("method handleInCallMmiCommands is NOT supported in CDMA!");
            return false;
        }

        Phone imsPhone = mImsPhone;
        if (imsPhone != null
                && imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE) {
            /// M: CC: Check GSM call state to avoid InCallMMI dispatching to IMS @{
            // [ALPS02516173],[ALPS02615800]
            //return imsPhone.handleInCallMmiCommands(dialString);
            if (!isInCSCall()) {
                return imsPhone.handleInCallMmiCommands(dialString);
            }
            /// @}
        }

        if (!isInCall()) {
            return false;
        }

        if (TextUtils.isEmpty(dialString)) {
            return false;
        }

        boolean result = false;
        char ch = dialString.charAt(0);
        switch (ch) {
            case '0':
                /// M: CC: Use 0+SEND MMI to release held calls or sets UDUB
                // (User Determined User Busy) for a waiting call. @{
                // 3GPP 22.030 6.5.5
                //result = handleCallDeflectionIncallSupplementaryService(dialString);
                result = handleUdubIncallSupplementaryService(dialString);
                ///@}
                break;
            case '1':
                result = handleCallWaitingIncallSupplementaryService(dialString);
                break;
            case '2':
                result = handleCallHoldIncallSupplementaryService(dialString);
                break;
            case '3':
                result = handleMultipartyIncallSupplementaryService(dialString);
                break;
            case '4':
                result = handleEctIncallSupplementaryService(dialString);
                break;
            case '5':
                result = handleCcbsIncallSupplementaryService(dialString);
                break;
            default:
                break;
        }

        return result;
    }

    public boolean isInCall() {
        GsmCdmaCall.State foregroundCallState = getForegroundCall().getState();
        GsmCdmaCall.State backgroundCallState = getBackgroundCall().getState();
        GsmCdmaCall.State ringingCallState = getRingingCall().getState();

       return (foregroundCallState.isAlive() ||
                backgroundCallState.isAlive() ||
                ringingCallState.isAlive());
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return dial(dialString, null, videoState, null);
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras)
            throws CallStateException {
        if (!isPhoneTypeGsm() && uusInfo != null) {
            throw new CallStateException("Sending UUS information NOT supported in CDMA!");
        }

        boolean isEmergency = PhoneNumberUtils.isEmergencyNumber(dialString);
        Phone imsPhone = mImsPhone;

        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean alwaysTryImsForEmergencyCarrierConfig = configManager.getConfigForSubId(getSubId())
                .getBoolean(CarrierConfigManager.KEY_CARRIER_USE_IMS_FIRST_FOR_EMERGENCY_BOOL);

        boolean imsUseEnabled = isImsUseEnabled()
                 && imsPhone != null
                 && (imsPhone.isVolteEnabled() || imsPhone.isWifiCallingEnabled() ||
                 (imsPhone.isVideoEnabled() && VideoProfile.isVideo(videoState)))
                 && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE);

        boolean useImsForEmergency = imsPhone != null
                && isEmergency
                && alwaysTryImsForEmergencyCarrierConfig
                && ImsManager.isNonTtyOrTtyOnVolteEnabled(mContext)
                && (imsPhone.getServiceState().getState() != ServiceState.STATE_POWER_OFF);

        /// M: @{
        if (!isPhoneTypeGsm()) {
            useImsForEmergency = false;     //TODO: remove this workaround for ECC fail
        }
        /// @}

        String dialPart = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.
                stripSeparators(dialString));
        boolean isUt = (dialPart.startsWith("*") || dialPart.startsWith("#"))
                && dialPart.endsWith("#");

        /// M: @{
        boolean useImsForUt = imsPhone != null && imsPhone.isUtEnabled()
                && !(OperatorUtils.isNotSupportXcap(getOperatorNumeric()));
        /// @}

        if (DBG) {
            logd("imsUseEnabled=" + imsUseEnabled
                    + ", useImsForEmergency=" + useImsForEmergency
                    + ", useImsForUt=" + useImsForUt
                    + ", isUt=" + isUt
                    + ", imsPhone=" + imsPhone
                    + ", imsPhone.isVolteEnabled()="
                    + ((imsPhone != null) ? imsPhone.isVolteEnabled() : "N/A")
                    + ", imsPhone.isVowifiEnabled()="
                    + ((imsPhone != null) ? imsPhone.isWifiCallingEnabled() : "N/A")
                    + ", imsPhone.isVideoEnabled()="
                    + ((imsPhone != null) ? imsPhone.isVideoEnabled() : "N/A")
                    + ", imsPhone.getServiceState().getState()="
                    + ((imsPhone != null) ? imsPhone.getServiceState().getState() : "N/A"));
        }

        Phone.checkWfcWifiOnlyModeBeforeDial(mImsPhone, mContext);

        /// M: should be removed later, just for debug @{
        Rlog.w(LOG_TAG, "IMS: imsphone = " + imsPhone + "isEmergencyNumber = " + isEmergency);
        if (imsPhone != null) {
            Rlog.w(LOG_TAG, "service state = " + imsPhone.getServiceState().getState());
        }
        /// @}

        if ((imsUseEnabled && (!isUt || useImsForUt)) || useImsForEmergency) {
            /// M: CC: Check GSM call state to avoid InCallMMI dispatching to IMS @{
            // [ALPS02516173],[ALPS02615800]
            if (isInCSCall()) {
                if (DBG) Rlog.d(LOG_TAG, "has CS Call. Don't try IMS PS Call!");
            } else {
            /// @}
                try {
                    /// M: ALPS02137073 3G VT Refactory
                    if (videoState == VideoProfile.STATE_AUDIO_ONLY) {
                        if (DBG) Rlog.d(LOG_TAG, "Trying IMS PS call");

                        //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]begin
                        if(matchString(dialString) && FeatureOption.BIRD_GSM_CANNOT_CALLOUT){
                            Rlog.d("xiaowo", "can't dial the special number  "+ dialString);
                            throw new CallStateException("can't dial the special number");
                        }
                        //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]end


                        return imsPhone.dial(dialString, uusInfo, videoState, intentExtras);
                    } else {
                        if (SystemProperties.get("persist.mtk_vilte_support").equals("1")) {
                            if (DBG) {
                                Rlog.d(LOG_TAG, "Trying IMS PS video call");
                            }
                            return imsPhone.dial(dialString, uusInfo, videoState, intentExtras);
                        } else {
                            /// M: CC: For 3G VT only @{
                            if (DBG) {
                                Rlog.d(LOG_TAG, "Trying (non-IMS) CS video call");
                            }
                            return dialInternal(dialString, uusInfo, videoState, intentExtras);
                            /// @}
                        }
                    }
                } catch (CallStateException e) {
                    if (DBG) logd("IMS PS call exception " + e +
                            "imsUseEnabled =" + imsUseEnabled + ", imsPhone =" + imsPhone);
                    if (!Phone.CS_FALLBACK.equals(e.getMessage())) {
                        CallStateException ce = new CallStateException(e.getMessage());
                        ce.setStackTrace(e.getStackTrace());
                        throw ce;
                    }
                }
            /// M: CC: Check GSM call state to avoid InCallMMI dispatching to IMS @{
            }
            /// @}
        }

        /// M: CC: FTA requires call should be dialed out even out of service @{
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) != 2) {
            if (mSST != null && mSST.mSS.getState() == ServiceState.STATE_OUT_OF_SERVICE
                    && mSST.mSS.getDataRegState() != ServiceState.STATE_IN_SERVICE
                    && !isEmergency) {
                throw new CallStateException("cannot dial in current state");
            }
        }
        /// @}
        if (DBG) logd("Trying (non-IMS) CS call");

        if (isPhoneTypeGsm()) {
            /// M: CC: For 3G VT only @{
            //return dialInternal(dialString, null, VideoProfile.STATE_AUDIO_ONLY, intentExtras);
            return dialInternal(dialString, null, videoState, intentExtras);
            /// @}
        } else {
            return dialInternal(dialString, null, videoState, intentExtras);
        }
    }

    //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]begin
    private boolean matchString(String dialedNumber){
        if (dialedNumber != null) {
            if ( (dialedNumber.length() == 4 || dialedNumber.startsWith("1")) && !"1175".equals(dialedNumber) && !"1148".equals(dialedNumber) 
                && !"1149".equals(dialedNumber) && !"1".equals(dialedNumber) ) {
                return true;
            }
        }
        return false;
    }
    //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]end

    @Override
    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState,
                                      Bundle intentExtras)
            throws CallStateException {

        // Need to make sure dialString gets parsed properly
        /// M: Ignore stripping for VoLTE SIP uri. @{
        // String newDialString = PhoneNumberUtils.stripSeparators(dialString);
        String newDialString = dialString;
        if (!PhoneNumberUtils.isUriNumber(dialString)) {
            // Need to make sure dialString gets parsed properly
            newDialString = PhoneNumberUtils.stripSeparators(dialString);
        }
        /// @}

        if (isPhoneTypeGsm()) {
            // handle in-call MMI first if applicable
            if (handleInCallMmiCommands(newDialString)) {
                return null;
            }

            // Only look at the Network portion for mmi
            String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
            GsmMmiCode mmi =
                    GsmMmiCode.newFromDialString(networkPortion, this, mUiccApplication.get());
            if (DBG) logd("dialing w/ mmi '" + mmi + "'...");

            if (mmi == null) {

                /// M: CC: For 3G VT only @{
                //return mCT.dial(newDialString, uusInfo, intentExtras);
                if (videoState == VideoProfile.STATE_AUDIO_ONLY) {
                    return mCT.dial(newDialString, uusInfo, intentExtras);
                } else {
                    if (!is3GVTEnabled()) {
                        throw new CallStateException("cannot vtDial for non-3GVT-capable device");
                    }
                    return mCT.vtDial(newDialString, uusInfo, intentExtras);
                }
                /// @}
            } else if (mmi.isTemporaryModeCLIR()) {
                /// M: CC: For 3G VT only @{
                //return mCT.dial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo, intentExtras);
                if (videoState == VideoProfile.STATE_AUDIO_ONLY) {
                    return mCT.dial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo, intentExtras);
                } else {
                    if (!is3GVTEnabled()) {
                        throw new CallStateException("cannot vtDial for non-3GVT-capable device");
                    }
                    return mCT.vtDial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo, intentExtras);
                }
                /// @}
            } else {

                /// M: @{
                if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                    Rlog.d(LOG_TAG, "Stop CS MMI during IMS Ecc Call or VoLTE call");
                    throw new CallStateException("Stop CS MMI during IMS Ecc Call or VoLTE call");
                }
                /// @}

                mPendingMMIs.add(mmi);
                /// M: @{
                Rlog.d(LOG_TAG, "dialInternal: " + dialString + ", mmi=" + mmi);
                dumpPendingMmi();
                /// @}
                mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
                try {
                    mmi.processCode();
                } catch (CallStateException e) {
                    //do nothing
                }

                // FIXME should this return null or something else?
                return null;
            }
        } else {
            return mCT.dial(newDialString);
        }
    }

    @Override
    public boolean handlePinMmi(String dialString) {
        MmiCode mmi;
        if (isPhoneTypeGsm()) {
            mmi = GsmMmiCode.newFromDialString(dialString, this, mUiccApplication.get());
        } else {
            mmi = CdmaMmiCode.newFromDialString(dialString, this, mUiccApplication.get());
        }

        if (mmi != null && mmi.isPinPukCommand()) {
            mPendingMMIs.add(mmi);
            /// M: @{
            Rlog.d(LOG_TAG, "handlePinMmi: " + dialString + ", mmi=" + mmi);
            dumpPendingMmi();
            /// @}
            mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
            try {
                mmi.processCode();
            } catch (CallStateException e) {
                //do nothing
            }
            return true;
        }

        loge("Mmi is null or unrecognized!");
        return false;
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        if (isPhoneTypeGsm()) {
            GsmMmiCode mmi = GsmMmiCode.newFromUssdUserInput(ussdMessge, this, mUiccApplication.get());
            mPendingMMIs.add(mmi);
            /// M: @{
            Rlog.d(LOG_TAG, "sendUssdResponse: " + ussdMessge + ", mmi=" + mmi);
            dumpPendingMmi();
            /// @}
            mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
            mmi.sendUssd(ussdMessge);
        } else {
            loge("sendUssdResponse: not possible in CDMA");
        }
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
        } else {
            if (mCT.mState ==  PhoneConstants.State.OFFHOOK) {
                mCi.sendDtmf(c, null);
            }
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
        } else {
            mCi.startDtmf(c, null);
        }
    }

    @Override
    public void stopDtmf() {
        mCi.stopDtmf(null);
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] sendBurstDtmf() is a CDMA method");
        } else {
            boolean check = true;
            for (int itr = 0;itr < dtmfString.length(); itr++) {
                if (!PhoneNumberUtils.is12Key(dtmfString.charAt(itr))) {
                    Rlog.e(LOG_TAG,
                            "sendDtmf called with invalid character '" + dtmfString.charAt(itr)+ "'");
                    check = false;
                    break;
                }
            }
            if (mCT.mState == PhoneConstants.State.OFFHOOK && check) {
                mCi.sendBurstDtmf(dtmfString, on, off, onComplete);
            }
        }
    }

    @Override
    public void setRadioPower(boolean power) {
        mSST.setRadioPower(power);
    }

    private void storeVoiceMailNumber(String number) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        if (isPhoneTypeGsm()) {
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
            if(FeatureOption.BIRD_VOICE_MAIL_NUMBER_FROM_SIM){
			    editor.putString(getSubscriberId(), number); //不再使用卡槽作为保存VM number的单位，而使用IMSI
		    }else{
			    editor.putString(VM_NUMBER + getPhoneId(), number);
		    }
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
            editor.apply();
            setVmSimImsi(getSubscriberId());
        } else {
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
            if(FeatureOption.BIRD_VOICE_MAIL_NUMBER_FROM_SIM){
			    editor.putString(getSubscriberId(), number); //不再使用卡槽作为保存VM number的单位，而使用IMSI
		    }else{
			    editor.putString(VM_NUMBER + getPhoneId(), number);
		    }
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
            editor.apply();
        }
    }

    @Override
    public String getVoiceMailNumber() {
        String number = null;
        if (isPhoneTypeGsm()) {
            // Read from the SIM. If its null, try reading from the shared preference area.
            IccRecords r = mIccRecords.get();
            number = (r != null) ? r.getVoiceMailNumber() : "";
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
            if(FeatureOption.BIRD_VOICE_MAIL_NUMBER_FROM_SIM){
            	if (TextUtils.isEmpty(number) || ((SIMRecords)r).isSetByCountry) {
				    //如果SIM卡中
				    //无VM number或是通过voicemail-conf.xml来设置的，则应该读取一下Preference，看是否用户
				    //对此SIM卡设置过VM number。
                	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
				    String temp = sp.getString(getSubscriberId(), null);
				    if (temp != null) {
					    number = temp;
				    }
            	}
		    } else {
                if (TextUtils.isEmpty(number)) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                    number = sp.getString(VM_NUMBER + getPhoneId(), null);
                }
            }
            /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            number = sp.getString(VM_NUMBER_CDMA + getPhoneId(), null);
        }

        if (TextUtils.isEmpty(number)) {
            String[] listArray = getContext().getResources()
                .getStringArray(com.android.internal.R.array.config_default_vm_number);
            if (listArray != null && listArray.length > 0) {
                for (int i=0; i<listArray.length; i++) {
                    if (!TextUtils.isEmpty(listArray[i])) {
                        String[] defaultVMNumberArray = listArray[i].split(";");
                        if (defaultVMNumberArray != null && defaultVMNumberArray.length > 0) {
                            if (defaultVMNumberArray.length == 1) {
                                number = defaultVMNumberArray[0];
                            } else if (defaultVMNumberArray.length == 2 &&
                                    !TextUtils.isEmpty(defaultVMNumberArray[1]) &&
                                    isMatchGid(defaultVMNumberArray[1])) {
                                number = defaultVMNumberArray[0];
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (!isPhoneTypeGsm() && TextUtils.isEmpty(number)) {
            // Read platform settings for dynamic voicemail number
            if (getContext().getResources().getBoolean(com.android.internal
                    .R.bool.config_telephony_use_own_number_for_voicemail)) {
                number = getLine1Number();
            } else {
                number = "*86";
            }
        }

        return number;
    }

    private String getVmSimImsi() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getString(VM_SIM_IMSI + getPhoneId(), null);
    }

    private void setVmSimImsi(String imsi) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VM_SIM_IMSI + getPhoneId(), imsi);
        editor.apply();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        String ret = "";

        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();

            ret = (r != null) ? r.getVoiceMailAlphaTag() : "";
        }

        if (ret == null || ret.length() == 0) {
            return mContext.getText(
                com.android.internal.R.string.defaultVoiceMailAlphaTag).toString();
        }

        return ret;
    }

    @Override
    public String getDeviceId() {
        if (isPhoneTypeGsm()) {
            return mImei;
        } else {
            if (getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                ///M: google CTS need return IMEI
                Rlog.d(LOG_TAG, "getDeviceId() in LTE_ON_CDMA_TRUE : return Imei");
                return getImei();
            } else {
                String id = getMeid();
                if ((id == null) || id.matches("^0*$")) {
                    loge("getDeviceId(): MEID is not initialized use ESN");
                    id = getEsn();
                }
                return id;
            }
        }
    }

    public int isDeviceIdAbnormal() {
        return mDeviceIdAbnormal;
    }

    public void setDeviceIdAbnormal(int abnormal) {
        mDeviceIdAbnormal = abnormal;
    }

    @Override
    public String getDeviceSvn() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            return mImeiSv;
        } else {
            loge("getDeviceSvn(): return 0");
            return "0";
        }
    }

    @Override
    public IsimRecords getIsimRecords() {
        return mIsimUiccRecords;
    }

    @Override
    public String getImei() {
        return mImei;
    }

    @Override
    public String getEsn() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getEsn() is a CDMA method");
            return "0";
        } else {
            return mEsn;
        }
    }

    @Override
    public String getMeid() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getMeid() is a CDMA method");
            return "0";
        } else {
            return mMeid;
        }
    }

    @Override
    public String getNai() {
        IccRecords r = mUiccController.getIccRecords(mPhoneId, UiccController.APP_FAM_3GPP2);
        if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
            Rlog.v(LOG_TAG, "IccRecords is " + r);
        }
        return (r != null) ? r.getNAI() : null;
    }

    @Override
    public String getSubscriberId() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getIMSI() : null;
        } else if (isPhoneTypeCdma()) {
            return mSST.getImsi();
        } else { //isPhoneTypeCdmaLte()
            IccRecords r = mIccRecords.get();
            return (mSimRecords != null) ? mSimRecords.getIMSI()
                    : ((r != null) ? r.getIMSI() : null);
        }
    }

    @Override
    public String getGroupIdLevel1() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getGid1() : null;
        } else if (isPhoneTypeCdma()) {
            loge("GID1 is not available in CDMA");
            return null;
        } else { //isPhoneTypeCdmaLte()
            return (mSimRecords != null) ? mSimRecords.getGid1() : "";
        }
    }

    @Override
    public String getGroupIdLevel2() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getGid2() : null;
        } else if (isPhoneTypeCdma()) {
            loge("GID2 is not available in CDMA");
            return null;
        } else { //isPhoneTypeCdmaLte()
            return (mSimRecords != null) ? mSimRecords.getGid2() : "";
        }
    }

    @Override
    public String getLine1Number() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnNumber() : null;
        } else {
            return mSST.getMdnNumber();
        }
    }

    @Override
    public String getCdmaPrlVersion() {
        return mSST.getPrlVersion();
    }

    @Override
    public String getCdmaMin() {
        return mSST.getCdmaMin();
    }

    @Override
    public boolean isMinInfoReady() {
        return mSST.isMinInfoReady();
    }

    @Override
    public String getMsisdn() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnNumber() : null;
        } else if (isPhoneTypeCdmaLte()) {
            return (mSimRecords != null) ? mSimRecords.getMsisdnNumber() : null;
        } else {
            loge("getMsisdn: not expected on CDMA");
            return null;
        }
    }

    @Override
    public String getLine1AlphaTag() {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            return (r != null) ? r.getMsisdnAlphaTag() : null;
        } else {
            loge("getLine1AlphaTag: not possible in CDMA");
            return null;
        }
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                r.setMsisdnNumber(alphaTag, number, onComplete);
                return true;
            } else {
                return false;
            }
        } else {
            loge("setLine1Number: not possible in CDMA");
            return false;
        }
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        Message resp;
        mVmNumber = voiceMailNumber;
        resp = obtainMessage(EVENT_SET_VM_NUMBER_DONE, 0, 0, onComplete);
        IccRecords r = mIccRecords.get();
        if (r != null) {
            r.setVoiceMailNumber(alphaTag, mVmNumber, resp);
        }
    }

    private boolean isValidCommandInterfaceCFReason (int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case CF_REASON_UNCONDITIONAL:
            case CF_REASON_BUSY:
            case CF_REASON_NO_REPLY:
            case CF_REASON_NOT_REACHABLE:
            case CF_REASON_ALL:
            case CF_REASON_ALL_CONDITIONAL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            if (getUnitTestMode()) {
                return null;
            }
            return TelephonyManager.getTelephonyProperty(mPhoneId, property, defValue);
        } else {
            return super.getSystemProperty(property, defValue);
        }
    }

    private boolean isValidCommandInterfaceCFAction (int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
            case CF_ACTION_DISABLE:
            case CF_ACTION_ENABLE:
            case CF_ACTION_REGISTRATION:
            case CF_ACTION_ERASURE:
                return true;
            default:
                return false;
        }
    }

    private boolean isCfEnable(int action) {
        return (action == CF_ACTION_ENABLE) || (action == CF_ACTION_REGISTRATION);
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && ((imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    || imsPhone.isUtEnabled())
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CF,
                    onComplete);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                ss.mParcel.writeInt(CommandsInterface.SERVICE_CLASS_VOICE);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);

                if (isOpReregisterForCF()) {
                    // Check the arg2 is MESSAGE_SET_CF
                    if (onComplete.arg2 == MESSAGE_SET_CF) {
                        Rlog.i(LOG_TAG, "Set ims dereg to ON.");
                        SystemProperties.set(IMS_DEREG_PROP, IMS_DEREG_ON);
                    }
                }

                imsPhone.getCallForwardingOption(commandInterfaceCFReason, imsUtResult);
                return;
            }

            if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
                if (DBG) logd("requesting call forwarding query.");
                Message resp;
                if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                    resp = obtainMessage(EVENT_GET_CALL_FORWARD_DONE, onComplete);
                } else {
                    resp = onComplete;
                }

                /// M: SS Ut part @{
                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    mSSReqDecisionMaker.queryCallForwardStatus(commandInterfaceCFReason,
                            0, null, resp);
                    return;
                }
                /// @}

                if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                    setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                }

                /// M: Not support from Ut to cs domain part @{
                if (isNotSupportUtToCS()) {
                    sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
                } else {
                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }
                    mCi.queryCallForwardStatus(commandInterfaceCFReason,0,null,resp);
                }
            }
         } else {
            loge("getCallForwardingOption: not possible in CDMA");
            /// M: CDMA Ut part @{
            if (isNotSupportUtToCS()) {
                if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
                    if (DBG) {
                        logd("requesting call forwarding query.");
                    }
                    Message resp;
                    if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                        resp = obtainMessage(EVENT_GET_CALL_FORWARD_DONE, onComplete);
                    } else {
                        resp = onComplete;
                    }
                    if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                            && isGsmUtSupport()) {
                        mSSReqDecisionMaker.queryCallForwardStatus(commandInterfaceCFReason,
                                0, null, resp);
                        return;
                    }
                    if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                        setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                    }
                    sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
                }
            }
           /// @}
        }
    }

    @Override
    public void getCallForwardingOptionForServiceClass(int commandInterfaceCFReason,
                                  int serviceClass,
                                  Message onComplete) {
        ImsPhone imsPhone = (ImsPhone) mImsPhone;
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {
            SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CF,
                    onComplete);
            ss.mParcel.writeInt(commandInterfaceCFReason);
            ss.mParcel.writeInt(serviceClass);
            Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
            setServiceClass(serviceClass);
            imsPhone.getCallForwardingOption(commandInterfaceCFReason, imsUtResult);
            return;
        }

        if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            Rlog.d(LOG_TAG, "requesting call forwarding query.");
            Message resp;
            if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                resp = obtainMessage(EVENT_GET_CALL_FORWARD_DONE, onComplete);
            } else {
                resp = onComplete;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                mSSReqDecisionMaker.queryCallForwardStatus(commandInterfaceCFReason,
                        serviceClass, null, resp);
                return;
            }
            /// @}

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }
            mCi.queryCallForwardStatus(commandInterfaceCFReason,serviceClass,null,resp);
        }
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int timerSeconds,
            Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && ((imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    || imsPhone.isUtEnabled())
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CF,
                    onComplete);
                ss.mParcel.writeInt(commandInterfaceCFAction);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                ss.mParcel.writeString(dialingNumber);
                ss.mParcel.writeInt(timerSeconds);
                ss.mParcel.writeInt(CommandsInterface.SERVICE_CLASS_VOICE);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                imsPhone.setCallForwardingOption(commandInterfaceCFAction,
                        commandInterfaceCFReason, dialingNumber, timerSeconds, imsUtResult);
                return;
            }

            if ((isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                    (isValidCommandInterfaceCFReason(commandInterfaceCFReason))) {

                Message resp;
                if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                    int origUtCfuMode = 0;
                    String utCfuMode = getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                            UT_CFU_NOTIFICATION_MODE_DISABLED);
                    if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                        origUtCfuMode = 1;
                    } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                        origUtCfuMode = 2;
                    }

                    setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                            UT_CFU_NOTIFICATION_MODE_DISABLED);

                    Cfu cfu = new Cfu(dialingNumber, onComplete);
                    resp = obtainMessage(EVENT_SET_CALL_FORWARD_DONE,
                            isCfEnable(commandInterfaceCFAction) ? 1 : 0, origUtCfuMode, cfu);
                } else {
                    resp = onComplete;
                }

                /// M: SS Ut part @{
                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    mSSReqDecisionMaker.setCallForward(commandInterfaceCFAction,
                            commandInterfaceCFReason, CommandsInterface.SERVICE_CLASS_VOICE,
                            dialingNumber, timerSeconds, resp);
                    return;
                }
                /// @}

                if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                    setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                }

                if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                    if (onComplete != null) {
                        sendErrorResponse(onComplete,
                                CommandException.Error.GENERIC_FAILURE);
                        return;
                    }
                }

                mCi.setCallForward(commandInterfaceCFAction,
                        commandInterfaceCFReason,
                        CommandsInterface.SERVICE_CLASS_VOICE,
                        dialingNumber,
                        timerSeconds,
                        resp);
            }
        } else {
            loge("setCallForwardingOption: not possible in CDMA");
            /// M: CDMA Ut part @{
            if (isNotSupportUtToCS()) {
                if (DBG) {
                    logd("setCallForwardingOption.");
                }
                if ((isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                        (isValidCommandInterfaceCFReason(commandInterfaceCFReason))) {
                    Message resp;
                    if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                        int origUtCfuMode = 0;
                        String utCfuMode = getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);
                        if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                            origUtCfuMode = 1;
                        } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                            origUtCfuMode = 2;
                        }
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);
                        Cfu cfu = new Cfu(dialingNumber, onComplete);
                        resp = obtainMessage(EVENT_SET_CALL_FORWARD_DONE,
                                isCfEnable(commandInterfaceCFAction) ? 1 : 0, origUtCfuMode, cfu);
                     } else {
                            resp = onComplete;
                     }
                     if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                             && isGsmUtSupport()) {
                         mSSReqDecisionMaker.setCallForward(commandInterfaceCFAction,
                                 commandInterfaceCFReason, CommandsInterface.SERVICE_CLASS_VOICE,
                                 dialingNumber, timerSeconds, resp);
                         return;
                     }
                     if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                         setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                     }
                }
            }
            /// @}
        }
    }

    @Override
    public void setCallForwardingOptionForServiceClass(int commandInterfaceCFReason,
                                 int commandInterfaceCFAction,
                                 String dialingNumber,
                                 int timerSeconds,
                                 int serviceClass,
                                 Message onComplete) {
        ImsPhone imsPhone = (ImsPhone) mImsPhone;
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {
            SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CF,
                    onComplete);
            ss.mParcel.writeInt(commandInterfaceCFAction);
            ss.mParcel.writeInt(commandInterfaceCFReason);
            ss.mParcel.writeString(dialingNumber);
            ss.mParcel.writeInt(timerSeconds);
            ss.mParcel.writeInt(serviceClass);
            Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
            imsPhone.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason,
                    dialingNumber, serviceClass, timerSeconds, imsUtResult);
            return;
        }

        if (    (isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                (isValidCommandInterfaceCFReason(commandInterfaceCFReason))) {

            Message resp;
            if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                int origUtCfuMode = 0;
                String utCfuMode = getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                        UT_CFU_NOTIFICATION_MODE_DISABLED);
                if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                    origUtCfuMode = 1;
                } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                    origUtCfuMode = 2;
                }

                setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                        UT_CFU_NOTIFICATION_MODE_DISABLED);

                Cfu cfu = new Cfu(dialingNumber, onComplete);
                resp = obtainMessage(EVENT_SET_CALL_FORWARD_DONE,
                        isCfEnable(commandInterfaceCFAction) ? 1 : 0, origUtCfuMode, cfu);
            } else {
                resp = onComplete;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                mSSReqDecisionMaker.setCallForward(commandInterfaceCFAction,
                        commandInterfaceCFReason, serviceClass,
                        dialingNumber, timerSeconds, resp);
                return;
            }
            /// @}

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }
            mCi.setCallForward(commandInterfaceCFAction,
                    commandInterfaceCFReason,
                    serviceClass,
                    dialingNumber,
                    timerSeconds,
                    resp);
        }
    }

    /**
     * Get Terminal-based CLIR.
     * @return Response array by 27.007.
     */
    public int[] getSavedClirSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int clirSetting = sp.getInt(CLIR_KEY + getPhoneId(), -1);
        int presentationMode;
        int getClirResult;
        if ((clirSetting == 0) || (clirSetting == -1)) {
            //allow CLI presentation
            presentationMode = 4;
            getClirResult = CommandsInterface.CLIR_DEFAULT;
        } else if (clirSetting == 1) {
            //restrict CLI presentation
            presentationMode = 3;
            getClirResult = CommandsInterface.CLIR_INVOCATION;
        } else {
            //allow CLI presentation
            presentationMode = 4;
            getClirResult = CommandsInterface.CLIR_SUPPRESSION;
        }

        int getClirResponse [] = new int[2];
        getClirResponse[0] = getClirResult;
        getClirResponse[1] = presentationMode;

        Rlog.i(LOG_TAG, "getClirResult: " + getClirResult);
        Rlog.i(LOG_TAG, "presentationMode: " + presentationMode);

        return getClirResponse;
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = mImsPhone;
            /// M: SS @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                && (imsPhone.isVolteEnabled()
                || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                if (isOpTbClir()) {
                    if (onComplete != null) {
                        int[] result = getSavedClirSetting();
                        AsyncResult.forMessage(onComplete, result, null);
                        onComplete.sendToTarget();
                    }
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CLIR,
                        onComplete);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                imsPhone.getOutgoingCallerIdDisplay(imsUtResult);
                return;
            }
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                if (isOpTbClir()) {
                    if (onComplete != null) {
                        int[] result = getSavedClirSetting();
                        AsyncResult.forMessage(onComplete, result, null);
                        onComplete.sendToTarget();
                    }
                    return;
                }
                mSSReqDecisionMaker.getCLIR(onComplete);
                return;
            }
            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }
            /// @}

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            mCi.getCLIR(onComplete);
        } else {
            loge("getOutgoingCallerIdDisplay: not possible in CDMA");
        }
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = mImsPhone;
            /// M: SS @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }

                if (isOpTbClir()) {
                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }

                    mCi.setCLIR(commandInterfaceCLIRMode,
                        obtainMessage(EVENT_SET_CLIR_COMPLETE, commandInterfaceCLIRMode,
                        0, onComplete));
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CLIR,
                        onComplete);
                ss.mParcel.writeInt(commandInterfaceCLIRMode);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                imsPhone.setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, imsUtResult);
                return;
            }
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                if (isOpTbClir()) {

                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }

                    mCi.setCLIR(commandInterfaceCLIRMode,
                        obtainMessage(EVENT_SET_CLIR_COMPLETE, commandInterfaceCLIRMode,
                        0, onComplete));
                    return;
                }
                mSSReqDecisionMaker.setCLIR(commandInterfaceCLIRMode,
                        obtainMessage(EVENT_SET_CLIR_COMPLETE,
                                commandInterfaceCLIRMode, 0, onComplete));
                return;
            }
            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }
            /// @}

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            mCi.setCLIR(commandInterfaceCLIRMode,
                    obtainMessage(EVENT_SET_CLIR_COMPLETE, commandInterfaceCLIRMode, 0, onComplete));
        } else {
            loge("setOutgoingCallerIdDisplay: not possible in CDMA");
        }
    }

    private void initTbcwMode() {
        if (mTbcwMode == TBCW_UNKNOWN) {
            if (isOpTbcwWithCS(getPhoneId())) {
                setTbcwMode(TBCW_OPTBCW_WITH_CS);
                setTbcwToEnabledOnIfDisabled();
            } else if (!isUsimCard()) {
                setTbcwMode(TBCW_OPTBCW_NOT_VOLTE_USER);
                setSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                                TERMINAL_BASED_CALL_WAITING_DISABLED);
            }
        }
        Rlog.i(LOG_TAG, "initTbcwMode: " + mTbcwMode);
    }

    public int getTbcwMode() {
        if (mTbcwMode == TBCW_UNKNOWN) {
            initTbcwMode();
        }
        return mTbcwMode;
    }

    public void setTbcwMode(int newMode) {
        Rlog.i(LOG_TAG,"Set tbcwmode: " + newMode);
        mTbcwMode = newMode;
    }

    /**
     * Set the system property PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE
     * to TERMINAL_BASED_CALL_WAITING_ENABLED_ON if it is TERMINAL_BASED_CALL_WAITING_DISABLED.
     */
    public void setTbcwToEnabledOnIfDisabled() {
        String tbcwMode = getSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                TERMINAL_BASED_CALL_WAITING_DISABLED);
        if (TERMINAL_BASED_CALL_WAITING_DISABLED.equals(tbcwMode)) {
            setSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                    TERMINAL_BASED_CALL_WAITING_ENABLED_ON);
        }
    }

    /**
     * Return Terminal-based Call Waiting configuration.
     * @param onComplete Message callback
     */
    public void getTerminalBasedCallWaiting(Message onComplete) {
        String tbcwMode = getSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                TERMINAL_BASED_CALL_WAITING_DISABLED);
        if (DBG) {
            Rlog.d(LOG_TAG, "getTerminalBasedCallWaiting(): tbcwMode = " + tbcwMode
                    + ", onComplete = " + onComplete);
        }
        if (TERMINAL_BASED_CALL_WAITING_ENABLED_ON.equals(tbcwMode)) {
            if (onComplete != null) {
                int[] cwInfos = new int[2];
                cwInfos[0] = 1;
                cwInfos[1] = SERVICE_CLASS_VOICE;
                AsyncResult.forMessage(onComplete, cwInfos, null);
                onComplete.sendToTarget();
            }
            return;
        } else if (TERMINAL_BASED_CALL_WAITING_ENABLED_OFF.equals(tbcwMode)) {
            if (onComplete != null) {
                int[] cwInfos = new int[2];
                cwInfos[0] = 0;
                AsyncResult.forMessage(onComplete, cwInfos, null);
                onComplete.sendToTarget();
            }
            return;
        }

        Rlog.e(LOG_TAG, "getTerminalBasedCallWaiting(): ERROR: tbcwMode = " + tbcwMode);
        return;
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        if (isPhoneTypeGsm()) {
            //isOpNwCW:Call Waiting is configured by Ut interface.
            if (!isOpNwCW()) {
                if (mTbcwMode == TBCW_UNKNOWN) {
                    initTbcwMode();
                }

                if (DBG) {
                    Rlog.d(LOG_TAG, "getCallWaiting(): mTbcwMode = " + mTbcwMode
                            + ", onComplete = " + onComplete);
                }
                if (mTbcwMode == TBCW_OPTBCW_VOLTE_USER) {
                    getTerminalBasedCallWaiting(onComplete);
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_NOT_VOLTE_USER) {
                    /// M: Not support from Ut to cs domain part @{
                    if (isNotSupportUtToCS()) {
                        sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
                    } else {
                        if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                            if (onComplete != null) {
                                sendErrorResponse(onComplete,
                                        CommandException.Error.GENERIC_FAILURE);
                                return;
                            }
                        }
                        mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE, onComplete);
                    }
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_WITH_CS) {
                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }

                    Message resp = obtainMessage(EVENT_GET_CALL_WAITING_DONE, onComplete);
                    mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE, resp);
                    return;
                }
            }
            Phone imsPhone = mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && ((imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    || imsPhone.isUtEnabled())
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNwCW()) {
                    Rlog.d(LOG_TAG, "isOpNwCW(), getCallWaiting() by Ut interface");
                    SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CW,
                        onComplete);
                    Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                    imsPhone.getCallWaiting(imsUtResult);
                } else {
                    Rlog.d(LOG_TAG, "isOpTbCW(), getTerminalBasedCallWaiting");
                    setTbcwMode(TBCW_OPTBCW_VOLTE_USER);
                    setTbcwToEnabledOnIfDisabled();
                    getTerminalBasedCallWaiting(onComplete);
                }
                return;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mSSReqDecisionMaker.queryCallWaiting");
                mSSReqDecisionMaker.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE,
                    onComplete);
                return;
            }

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }
           /// @}

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            //As per 3GPP TS 24.083, section 1.6 UE doesn't need to send service
            //class parameter in call waiting interrogation  to network

            /// M: Not support from Ut to cs domain part @{
            if (isNotSupportUtToCS()) {
                sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
            } else {
                Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE, onComplete);
            }
            /// @}
            //  mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE, onComplete);
        } else {
            /// M: CDMA Ut part @{
            Rlog.d(LOG_TAG, "CDMA Ut part: getCallWaiting()");
            if (isNotSupportUtToCS()) {
                if (mTbcwMode == TBCW_UNKNOWN) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "getCallWaiting(): mTbcwMode = " + mTbcwMode
                        + ", onComplete = " + onComplete);
                if (mTbcwMode == TBCW_OPTBCW_VOLTE_USER) {
                    getTerminalBasedCallWaiting(onComplete);
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_NOT_VOLTE_USER) {
                    sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
                    return;
                }
                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    Rlog.d(LOG_TAG, "mSSReqDecisionMaker.queryCallWaiting");
                    mSSReqDecisionMaker.queryCallWaiting(CommandsInterface.SERVICE_CLASS_NONE,
                            onComplete);
                    return;
                }
                sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
            }
           /// @}
            mCi.queryCallWaiting(CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
        }
    }

    /**
     * Set Terminal-based Call Waiting configuration.
     * @param enable true if activate Call Waiting. false if deactivate Call Waiting.
     * @param onComplete Message callback
     */
    public void setTerminalBasedCallWaiting(boolean enable, Message onComplete) {
        String tbcwMode = getSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                TERMINAL_BASED_CALL_WAITING_DISABLED);
        if (DBG) {
            Rlog.d(LOG_TAG, "setTerminalBasedCallWaiting(): tbcwMode = " + tbcwMode
                    + ", enable = " + enable);
        }
        if (TERMINAL_BASED_CALL_WAITING_ENABLED_ON.equals(tbcwMode)) {
            if (!enable) {
                setSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                        TERMINAL_BASED_CALL_WAITING_ENABLED_OFF);
            }
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, null, null);
                onComplete.sendToTarget();
            }
            return;
        } else if (TERMINAL_BASED_CALL_WAITING_ENABLED_OFF.equals(tbcwMode)) {
            if (enable) {
                setSystemProperty(PROPERTY_TERMINAL_BASED_CALL_WAITING_MODE,
                        TERMINAL_BASED_CALL_WAITING_ENABLED_ON);
            }
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, null, null);
                onComplete.sendToTarget();
            }
            return;
        }

        Rlog.e(LOG_TAG, "setTerminalBasedCallWaiting(): ERROR: tbcwMode = " + tbcwMode);
        return;
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        if (isPhoneTypeGsm()) {
            //isOpNwCW():Call Waiting is configured by Ut interface.
            if (!isOpNwCW()) {
                if (mTbcwMode == TBCW_UNKNOWN) {
                    initTbcwMode();
                }

                if (DBG) {
                    Rlog.d(LOG_TAG, "setCallWaiting(): mTbcwMode = " + mTbcwMode
                            + ", onComplete = " + onComplete);
                }

                if (mTbcwMode == TBCW_OPTBCW_VOLTE_USER) {
                    setTerminalBasedCallWaiting(enable, onComplete);
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_NOT_VOLTE_USER) {
                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }

                    mCi.setCallWaiting(enable, CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_WITH_CS) {
                    if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                        if (onComplete != null) {
                            sendErrorResponse(onComplete,
                                    CommandException.Error.GENERIC_FAILURE);
                            return;
                        }
                    }

                    Message resp = obtainMessage(EVENT_SET_CALL_WAITING_DONE,
                        enable == true ? 1 : 0, 0, onComplete);
                    mCi.setCallWaiting(enable, CommandsInterface.SERVICE_CLASS_VOICE, resp);
                    return;
                }
            }
            Phone imsPhone = mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                && (imsPhone != null)
                && ((imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                || imsPhone.isUtEnabled())
                && (imsPhone.isVolteEnabled()
                || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNwCW()) {
                    Rlog.d(LOG_TAG, "isOpNwCW(), setCallWaiting(): IMS in service");
                    SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CW,
                            onComplete);
                    int enableState = enable ? 1 : 0;
                    ss.mParcel.writeInt(enableState);
                    Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                    imsPhone.setCallWaiting(enable, imsUtResult);
                } else {
                    Rlog.d(LOG_TAG, "isOpTbCW(), setTerminalBasedCallWaiting(): IMS in service");
                    setTbcwMode(TBCW_OPTBCW_VOLTE_USER);
                    setTbcwToEnabledOnIfDisabled();
                    setTerminalBasedCallWaiting(enable, onComplete);
                }
                return;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mSSReqDecisionMaker.setCallWaiting");
                mSSReqDecisionMaker.setCallWaiting(enable,
                        CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
                return;
            }
            /// @}

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            mCi.setCallWaiting(enable, CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
        } else {
            loge("method setCallWaiting is NOT supported in CDMA!");
            /// M: CDMA Ut part @{
            if (isNotSupportUtToCS()) {
                if (mTbcwMode == TBCW_UNKNOWN) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "setCallWaiting(): mTbcwMode = " + mTbcwMode
                        + ", onComplete = " + onComplete);
                if (mTbcwMode == TBCW_OPTBCW_VOLTE_USER) {
                    setTerminalBasedCallWaiting(enable, onComplete);
                    return;
                } else if (mTbcwMode == TBCW_OPTBCW_NOT_VOLTE_USER) {
                    sendErrorResponse(onComplete, CommandException.Error.UT_XCAP_403_FORBIDDEN);
                    return;
                }
                // do not handle TBCW_OPTBCW_NOT_VOLTE_USER for CT VOLTE
                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    Rlog.d(LOG_TAG, "mSSReqDecisionMaker.setCallWaiting");
                    mSSReqDecisionMaker.setCallWaiting(enable,
                            CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
                    return;
                }
                if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                    setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                }
            }
            /// @}
        }
    }

    /// M: @{
    /**
     * Get Call Barring State
     */
    public void getFacilityLock(String facility, String password, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                /// M: SS Ut part @{
                if (isOpNotSupportOCB(facility)) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                /// @}

                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CB,
                        onComplete);
                ss.mParcel.writeString(facility);
                ss.mParcel.writeString(password);
                ss.mParcel.writeInt(CommandsInterface.SERVICE_CLASS_VOICE);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);

                imsPhone.getCallBarring(facility, imsUtResult);
                return;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                mSSReqDecisionMaker.queryFacilityLock(facility,
                        password, CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
                return;
            }
            /// @}

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            mCi.queryFacilityLock(facility, password, CommandsInterface.SERVICE_CLASS_VOICE,
                    onComplete);
        } else {
            loge("method getFacilityLock is NOT supported in CDMA!");
        }
    }

    /**
     * Set Call Barring State
     */

    public void setFacilityLock(String facility, boolean enable,
        String password, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                    && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                    && (imsPhone.isVolteEnabled()
                    || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                /// M: SS Ut part @{
                if (isOpNotSupportOCB(facility)) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                /// @}

                SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CB,
                        onComplete);
                ss.mParcel.writeString(facility);
                int enableState = enable ? 1 : 0;
                ss.mParcel.writeInt(enableState);
                ss.mParcel.writeString(password);
                ss.mParcel.writeInt(CommandsInterface.SERVICE_CLASS_VOICE);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);

                imsPhone.setCallBarring(facility, enable, password, imsUtResult);
                return;
            }

            /// M: SS Ut part @{
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isGsmUtSupport()) {
                mSSReqDecisionMaker.setFacilityLock(facility,
                        enable, password, CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
                return;
            }
            /// @}

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }

            if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                if (onComplete != null) {
                    sendErrorResponse(onComplete,
                            CommandException.Error.GENERIC_FAILURE);
                    return;
                }
            }

            mCi.setFacilityLock(facility, enable, password,
                CommandsInterface.SERVICE_CLASS_VOICE, onComplete);
        } else {
            loge("method setFacilityLock is NOT supported in CDMA!");
        }
    }

    public void getFacilityLockForServiceClass(String facility, String password, int serviceClass,
            Message onComplete) {
        ImsPhone imsPhone = (ImsPhone) mImsPhone;
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {

            // / M: SS Ut part @{
            if (isOpNotSupportOCB(facility)) {
                sendErrorResponse(onComplete,
                        CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                return;
            }
            // / @}

            SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_GET_CB,
                    onComplete);
            ss.mParcel.writeString(facility);
            ss.mParcel.writeString(password);
            ss.mParcel.writeInt(serviceClass);
            Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
            setServiceClass(serviceClass);
            imsPhone.getCallBarring(facility, imsUtResult);
            return;
        }

        // / M: SS Ut part @{
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && isGsmUtSupport()) {
            setServiceClass(serviceClass);
            mSSReqDecisionMaker.queryFacilityLock(facility, password, serviceClass, onComplete);
            return;
        }
        // / @}

        if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
            setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
        }
        mCi.queryFacilityLock(facility, password, serviceClass, onComplete);
    }

    public void setFacilityLockForServiceClass(String facility, boolean enable, String password,
            int serviceClass, Message onComplete) {
        ImsPhone imsPhone = (ImsPhone) mImsPhone;
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && (imsPhone != null)
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {

            // / M: SS Ut part @{
            if (isOpNotSupportOCB(facility)) {
                sendErrorResponse(onComplete,
                        CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                return;
            }
            // / @}

            SuppSrvRequest ss = SuppSrvRequest.obtain(SuppSrvRequest.SUPP_SRV_REQ_SET_CB,
                    onComplete);
            ss.mParcel.writeString(facility);
            int enableState = enable ? 1 : 0;
            ss.mParcel.writeInt(enableState);
            ss.mParcel.writeString(password);
            ss.mParcel.writeInt(serviceClass);
            Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
            setServiceClass(serviceClass);
            imsPhone.setCallBarring(facility, enable, password, imsUtResult);
            return;
        }

        // / M: SS Ut part @{
        if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED) && isGsmUtSupport()) {
            setServiceClass(serviceClass);
            mSSReqDecisionMaker.setFacilityLock(facility, enable, password, serviceClass,
                    onComplete);
            return;
        }
        // / @}

        if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
            setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
        }
        mCi.setFacilityLock(facility, enable, password, serviceClass, onComplete);
    }

    /**
     * Change Call Barring Password
     */
    public void changeBarringPassword(String facility, String oldPwd,
        String newPwd, Message onComplete) {
        if (isPhoneTypeGsm()) {
            if (isDuringImsCall()) {
                // Prevent CS domain SS request during IMS call
                if (onComplete != null) {
                    CommandException ce = new CommandException(
                            CommandException.Error.GENERIC_FAILURE);
                    AsyncResult.forMessage(onComplete, null, ce);
                    onComplete.sendToTarget();
                }
            } else {
                mCi.changeBarringPassword(facility, oldPwd, newPwd, onComplete);
            }
        } else {
            loge("method setFacilityLock is NOT supported in CDMA!");
        }
    }

    /// M: SS OP01 Ut @{
    private static class CfuEx {
        final String mSetCfNumber;
        final long[] mSetTimeSlot;
        final Message mOnComplete;

        CfuEx(String cfNumber, long[] cfTimeSlot, Message onComplete) {
            mSetCfNumber = cfNumber;
            mSetTimeSlot = cfTimeSlot;
            mOnComplete = onComplete;
        }
    }

    @Override
    public void getCallForwardInTimeSlot(int commandInterfaceCFReason, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isOp(OPID.OP01) && (imsPhone != null)
                    && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {
                imsPhone.getCallForwardInTimeSlot(commandInterfaceCFReason, onComplete);
                return;
            }

            if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                if (DBG) {
                    Rlog.d(LOG_TAG, "requesting call forwarding in time slot query.");
                }
                Message resp;
                resp = obtainMessage(EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE, onComplete);

                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                        UT_CFU_NOTIFICATION_MODE_DISABLED);

                    mSSReqDecisionMaker.queryCallForwardInTimeSlotStatus(
                            commandInterfaceCFReason,
                            CommandsInterface.SERVICE_CLASS_VOICE, resp);
                } else {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                }
            } else if (onComplete != null) {
                sendErrorResponse(onComplete, CommandException.Error.GENERIC_FAILURE);
            }
        } else {
            loge("method getCallForwardInTimeSlot is NOT supported in CDMA!");
        }
    }

    @Override
    public void setCallForwardInTimeSlot(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int timerSeconds,
            long[] timeSlot,
            Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) mImsPhone;
            if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                    && isOp(OPID.OP01) && (imsPhone != null)
                    && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(
                        SuppSrvRequest.SUPP_SRV_REQ_SET_CF_IN_TIME_SLOT, onComplete);
                ss.mParcel.writeInt(commandInterfaceCFAction);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                ss.mParcel.writeString(dialingNumber);
                ss.mParcel.writeInt(timerSeconds);
                Message imsUtResult = obtainMessage(EVENT_IMS_UT_DONE, ss);
                imsPhone.setCallForwardInTimeSlot(commandInterfaceCFAction,
                        commandInterfaceCFReason, dialingNumber,
                        timerSeconds, timeSlot, imsUtResult);
                return;
            }

            if ((isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                    (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL)) {
                Message resp;
                CfuEx cfuEx = new CfuEx(dialingNumber, timeSlot, onComplete);
                resp = obtainMessage(EVENT_SET_CALL_FORWARD_TIME_SLOT_DONE,
                        isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cfuEx);

                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport()) {
                    mSSReqDecisionMaker.setCallForwardInTimeSlot(commandInterfaceCFAction,
                            commandInterfaceCFReason, CommandsInterface.SERVICE_CLASS_VOICE,
                            dialingNumber, timerSeconds, timeSlot, resp);
                } else {
                    sendErrorResponse(onComplete,
                            CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                }
            } else {
                sendErrorResponse(onComplete, CommandException.Error.GENERIC_FAILURE);
            }
        } else {
            loge("method setCallForwardInTimeSlot is NOT supported in CDMA!");
        }
    }

    private void handleCfuInTimeSlotQueryResult(CallForwardInfoEx[] infos) {
        IccRecords r = mIccRecords.get();
        if (r != null) {
            if (infos == null || infos.length == 0) {
                // Assume the default is not active
                // Set unconditional CFF in SIM to false
                setVoiceCallForwardingFlag(1, false, null);
                setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,UT_CFU_NOTIFICATION_MODE_OFF);
            } else {
                for (int i = 0, s = infos.length; i < s; i++) {
                    if ((infos[i].serviceClass & SERVICE_CLASS_VOICE) != 0) {
                        setVoiceCallForwardingFlag(1, (infos[i].status == 1),
                                infos[i].number);
                        String mode = infos[i].status == 1 ?
                            UT_CFU_NOTIFICATION_MODE_ON : UT_CFU_NOTIFICATION_MODE_OFF;
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE, mode);
                        saveTimeSlot(infos[i].timeSlot);
                        break;
                    }
                }
            }
        }
    }

    void sendErrorResponse(Message onComplete, CommandException.Error error) {
        Rlog.d(LOG_TAG, "sendErrorResponse" + error);
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, null, new CommandException(error));
            onComplete.sendToTarget();
        }
    }

    public boolean queryCfuOrWait() {
        int sid1 = 99, sid2 = 99;
        /* M: SS part */ //TODO need to check if there any new implementation
        //int slotId = SubscriptionManager.getSlotId(getSubId());//reference code

        /*
        if (mySimId == PhoneConstants.GEMINI_SIM_1) {
           sid1 = PhoneConstants.GEMINI_SIM_2;
           sid2 = PhoneConstants.GEMINI_SIM_3;
        } else if (mySimId == PhoneConstants.GEMINI_SIM_2) {
           sid1 = PhoneConstants.GEMINI_SIM_1;
           sid2 = PhoneConstants.GEMINI_SIM_3;
        } else if (mySimId == PhoneConstants.GEMINI_SIM_3) {
           sid1 = PhoneConstants.GEMINI_SIM_1;
           sid2 = PhoneConstants.GEMINI_SIM_2;
        }*/
        String oppositePropertyValue1 = SystemProperties.get(CFU_QUERY_PROPERTY_NAME + sid1);
        String oppositePropertyValue2 = SystemProperties.get(CFU_QUERY_PROPERTY_NAME + sid2);
        if ((oppositePropertyValue1.equals("1")) ||
            (oppositePropertyValue2.equals("1"))) { /* The opposite phone is querying CFU status */
            Message message = obtainMessage(EVENT_CFU_QUERY_TIMEOUT);
            sendMessageDelayed(message, cfuQueryWaitTime);
            return false;
        } else {
            boolean bDataEnable = getDataEnabled();
            Rlog.d(LOG_TAG, "bDataEnable: " + bDataEnable);
            if (isPhoneTypeGsm()) {
                //setSystemProperty(CFU_QUERY_PROPERTY_NAME + mySimId, "1");//* M: SS part */TODO
                if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && isGsmUtSupport() && bDataEnable) {
                    mSSReqDecisionMaker.queryCallForwardInTimeSlotStatus(
                        CF_REASON_UNCONDITIONAL,
                        SERVICE_CLASS_VOICE,
                        obtainMessage(EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE, 1, 0, null));
                } else {
                    Phone imsPhone = mImsPhone;
                    if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                        && (imsPhone != null)
                        && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)
                        && !bDataEnable
                        && isOp(OPID.OP01) ) {
                       Rlog.i(LOG_TAG, "No need query CFU in CS domain!");
                   } else {
                       if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                           setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
                       }

                       if (isDuringVoLteCall()|| isDuringImsEccCall()) {
                           Rlog.i(LOG_TAG, "No need query CFU in CS domain!");
                       } else {
                           mCi.queryCallForwardStatus(CF_REASON_UNCONDITIONAL, SERVICE_CLASS_VOICE,
                                   null,obtainMessage(EVENT_GET_CALL_FORWARD_DONE));
                       }
                   }
                }
           } else {
               Rlog.d(LOG_TAG, "isPhoneTypeCdma.");
               if (isNotSupportUtToCS()) {
                   if ((getCsFallbackStatus() == PhoneConstants.UT_CSFB_PS_PREFERRED)
                           && isGsmUtSupport() && bDataEnable) {
                       mSSReqDecisionMaker.queryCallForwardStatus(CF_REASON_UNCONDITIONAL,
                               SERVICE_CLASS_VOICE, null,
                               obtainMessage(EVENT_GET_CALL_FORWARD_DONE));
                   }
               }
           }
           return true;
        }
    }

    public SSRequestDecisionMaker getSSRequestDecisionMaker() {
        return mSSReqDecisionMaker;
    }
    /// @}

    public boolean isDuringVoLteCall() {
        boolean isOnLtePdn = (mImsPhone != null && mImsPhone.isVolteEnabled());
        boolean r = isDuringImsCall() && isOnLtePdn;
        Rlog.d(LOG_TAG, "isDuringVoLteCall: " + r);
        return r;
    }

    public boolean isDuringImsEccCall() {
        boolean isInImsEccCall = (mImsPhone != null && mImsPhone.isInEmergencyCall());
        Rlog.d(LOG_TAG, "isInImsEccCall: " + isInImsEccCall);
        return isInImsEccCall;
    }

    public boolean isDuringImsCall() {
        if (mImsPhone != null) {
            Call.State foregroundCallState = mImsPhone.getForegroundCall().getState();
            Call.State backgroundCallState = mImsPhone.getBackgroundCall().getState();
            Call.State ringingCallState = mImsPhone.getRingingCall().getState();
            boolean isDuringImsCall = (foregroundCallState.isAlive() ||
                    backgroundCallState.isAlive() || ringingCallState.isAlive());
            if (isDuringImsCall) {
                Rlog.d(LOG_TAG, "During IMS call.");
                return true;
            }
        }
        return false;
    }

    private void handleImsUtCsfb(Message msg) {
        SuppSrvRequest ss = (SuppSrvRequest) msg.obj;
        if (ss == null) {
            Rlog.e(LOG_TAG, "handleImsUtCsfb: Error SuppSrvRequest null!");
            return;
        }

        if (isDuringVoLteCall()|| isDuringImsEccCall()) {
            // Prevent CS domain SS request during IMS Ecc call or VOLTE call
            Message resultCallback = ss.getResultCallback();
            if (resultCallback != null) {
                CommandException ce = new CommandException(
                        CommandException.Error.GENERIC_FAILURE);
                AsyncResult.forMessage(resultCallback, null, ce);
                resultCallback.sendToTarget();
            }

            if (getCsFallbackStatus() == PhoneConstants.UT_CSFB_ONCE) {
                setCsFallbackStatus(PhoneConstants.UT_CSFB_PS_PREFERRED);
            }

            ss.setResultCallback(null);
            ss.mParcel.recycle();
            return;
        }

        final int requestCode = ss.getRequestCode();
        ss.mParcel.setDataPosition(0);
        switch(requestCode) {
            case SuppSrvRequest.SUPP_SRV_REQ_GET_CF:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CF");
                int commandInterfaceCFReason = ss.mParcel.readInt();
                int serviceClass = ss.mParcel.readInt();
                getCallForwardingOptionForServiceClass(commandInterfaceCFReason,
                        serviceClass, ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_SET_CF:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CF");
                int commandInterfaceCFAction = ss.mParcel.readInt();
                int commandInterfaceCFReason = ss.mParcel.readInt();
                String dialingNumber = ss.mParcel.readString();
                int timerSeconds = ss.mParcel.readInt();
                int serviceClass = ss.mParcel.readInt();
                setCallForwardingOptionForServiceClass(commandInterfaceCFReason,
                        commandInterfaceCFAction, dialingNumber, timerSeconds,
                        serviceClass, ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_GET_CLIR:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CLIR");
                getOutgoingCallerIdDisplay(ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_SET_CLIR:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CLIR");
                int commandInterfaceCLIRMode = ss.mParcel.readInt();
                setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_GET_CW:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CW");
                getCallWaiting(ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_SET_CW:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CW");
                int enableState = ss.mParcel.readInt();
                boolean enable = (enableState != 0);
                setCallWaiting(enable, ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_GET_CB:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CB");
                String facility = ss.mParcel.readString();
                String password = ss.mParcel.readString();
                int serviceClass = ss.mParcel.readInt();
                getFacilityLockForServiceClass(facility, password, serviceClass,
                        ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_SET_CB:
            {
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CB");
                String facility = ss.mParcel.readString();
                int enableState = ss.mParcel.readInt();
                boolean enable = (enableState != 0);
                String password = ss.mParcel.readString();
                int serviceClass = ss.mParcel.readInt();
                setFacilityLockForServiceClass(facility, enable, password, serviceClass,
                        ss.getResultCallback());
                break;
            }
            case SuppSrvRequest.SUPP_SRV_REQ_MMI_CODE:
            {
                String dialString = ss.mParcel.readString();
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: dialString = "
                        + dialString);
                try {
                    dial(dialString, VideoProfile.STATE_AUDIO_ONLY);
                } catch (CallStateException ex) {
                    Rlog.e(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: CallStateException!");
                    ex.printStackTrace();
                }
                break;
            }
            default:
                Rlog.e(LOG_TAG, "handleImsUtCsfb: invalid requestCode = " + requestCode);
                break;
        }

        ss.setResultCallback(null);
        ss.mParcel.recycle();
    }

    private void handleUssiCsfb(String dialString) {
        Rlog.d(LOG_TAG, "handleUssiCsfb: dialString=" + dialString);
        try {
            dial(dialString, VideoProfile.STATE_AUDIO_ONLY);
        } catch (CallStateException ex) {
            Rlog.e(LOG_TAG, "handleUssiCsfb: CallStateException!");
            ex.printStackTrace();
        }
    }

    /* M: SS part end */

    @Override
    public void getAvailableNetworks(Message response) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            mCi.getAvailableNetworks(response);
        } else {
            loge("getAvailableNetworks: not possible in CDMA");
        }
    }

    @Override
    public void getNeighboringCids(Message response) {
        if (isPhoneTypeGsm()) {
            mCi.getNeighboringCids(response);
        } else {
            /*
             * This is currently not implemented.  At least as of June
             * 2009, there is no neighbor cell information available for
             * CDMA because some party is resisting making this
             * information readily available.  Consequently, calling this
             * function can have no useful effect.  This situation may
             * (and hopefully will) change in the future.
             */
            if (response != null) {
                CommandException ce = new CommandException(
                        CommandException.Error.REQUEST_NOT_SUPPORTED);
                AsyncResult.forMessage(response).exception = ce;
                response.sendToTarget();
            }
        }
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
       if (mImsPhone != null) {
           mImsPhone.setUiTTYMode(uiTtyMode, onComplete);
       }
    }

    @Override
    public void setMute(boolean muted) {
        mCT.setMute(muted);
    }

    @Override
    public boolean getMute() {
        return mCT.getMute();
    }

    @Override
    public void getDataCallList(Message response) {
        mCi.getDataCallList(response);
    }

    @Override
    public void updateServiceLocation() {
        mSST.enableSingleLocationUpdate();
    }

    @Override
    public void enableLocationUpdates() {
        mSST.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        mSST.disableLocationUpdates();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return mDcTracker.getDataOnRoamingEnabled();
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        mDcTracker.setDataOnRoamingEnabled(enable);
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        mCi.registerForCdmaOtaProvision(h, what, obj);
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler h) {
        mCi.unregisterForCdmaOtaProvision(h);
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        mSST.registerForSubscriptionInfoReady(h, what, obj);
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler h) {
        mSST.unregisterForSubscriptionInfoReady(h);
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        mEcmExitRespRegistrant.clear();
    }

    @Override
    public void registerForCallWaiting(Handler h, int what, Object obj) {
        mCT.registerForCallWaiting(h, what, obj);
    }

    @Override
    public void unregisterForCallWaiting(Handler h) {
        mCT.unregisterForCallWaiting(h);
    }

    @Override
    public boolean getDataEnabled() {
        return mDcTracker.getDataEnabled();
    }

    @Override
    public void setDataEnabled(boolean enable) {
        mDcTracker.setDataEnabled(enable);
    }

    /**
     * Removes the given MMI from the pending list and notifies
     * registrants that it is complete.
     * @param mmi MMI that is done
     */
    public void onMMIDone(MmiCode mmi) {
        /* Only notify complete if it's on the pending list.
         * Otherwise, it's already been handled (eg, previously canceled).
         * The exception is cancellation of an incoming USSD-REQUEST, which is
         * not on the list.
         */
        /// M: @{
        Rlog.d(LOG_TAG, "onMMIDone: " + mmi);
        dumpPendingMmi();
        /// @}
        if (mPendingMMIs.remove(mmi) || (isPhoneTypeGsm() && (mmi.isUssdRequest() ||
                ((GsmMmiCode)mmi).isSsInfo()))) {
            mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
        }
    }

    /**
     * Removes the given MMI from the pending list and notifies
     * registrants that it is complete.
     * @param mmi MMI that is done
     * @param obj User object to deliver to application
     */
    public void onMMIDone(GsmMmiCode mmi, Object obj) {
        /* Only notify complete if it's on the pending list.
         * Otherwise, it's already been handled (eg, previously canceled).
         * The exception is cancellation of an incoming USSD-REQUEST, which is
         * not on the list.
         */
        /// M: @{
        Rlog.d(LOG_TAG, "onMMIDone: " + mmi + ", obj=" + obj);
        dumpPendingMmi();
        /// @}
        if (mPendingMMIs.remove(mmi) || mmi.isUssdRequest() || mmi.isSsInfo()) {
            mMmiCompleteRegistrants.notifyRegistrants(
                    new AsyncResult(obj, mmi, null));
        }
    }
    /// M: @{
    public  void dumpPendingMmi() {
        int size = mPendingMMIs.size();
        if (size == 0) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: none");
            return;
        }
        for (int i=0; i<size; i++) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: " + mPendingMMIs.get(i));
        }
    }
    /// @}
    private void onNetworkInitiatedUssd(MmiCode mmi) {
        mMmiCompleteRegistrants.notifyRegistrants(
            new AsyncResult(null, mmi, null));
    }

    /** ussdMode is one of CommandsInterface.USSD_MODE_* */
    private void onIncomingUSSD (int ussdMode, String ussdMessage) {
        if (!isPhoneTypeGsm()) {
            loge("onIncomingUSSD: not expected on GSM");
        }
        boolean isUssdError;
        boolean isUssdRequest;
        boolean isUssdRelease;
        boolean isUssdhandleByStk;

        isUssdRequest
            = (ussdMode == CommandsInterface.USSD_MODE_REQUEST);

        // isUssdError
        //     = (ussdMode != CommandsInterface.USSD_MODE_NOTIFY
        //         && ussdMode != CommandsInterface.USSD_MODE_REQUEST);

        /* M: SS part */
        //MTK-START [mtk04070][111118][ALPS00093395]MTK modified
        isUssdError
            = ((ussdMode == CommandsInterface.USSD_OPERATION_NOT_SUPPORTED)
               || (ussdMode == CommandsInterface.USSD_NETWORK_TIMEOUT));
        //MTK-END [mtk04070][111118][ALPS00093395]MTK modified

        isUssdhandleByStk
            = (ussdMode == CommandsInterface.USSD_HANDLED_BY_STK);
        /* M: SS part end */

        isUssdRelease = (ussdMode == CommandsInterface.USSD_MODE_NW_RELEASE);


        // See comments in GsmMmiCode.java
        // USSD requests aren't finished until one
        // of these two events happen
        GsmMmiCode found = null;
        Rlog.d(LOG_TAG, "USSD:mPendingMMIs= " + mPendingMMIs + " size=" + mPendingMMIs.size());
        for (int i = 0, s = mPendingMMIs.size() ; i < s; i++) {
            Rlog.d(LOG_TAG, "i= " + i + " isPending="
                + ((GsmMmiCode)mPendingMMIs.get(i)).isPendingUSSD());
            if(((GsmMmiCode)mPendingMMIs.get(i)).isPendingUSSD()) {
                found = (GsmMmiCode)mPendingMMIs.get(i);
                Rlog.d(LOG_TAG, "found = " + found);
                break;
            }
        }

        if (found != null) {
            // Complete pending USSD
            /* M: SS part */
            //For ALPS01471897
            Rlog.d(LOG_TAG, "setUserInitiatedMMI  TRUE");
            found.setUserInitiatedMMI(true);
            /* M: SS part end */
            if (isUssdRelease && mIsNetworkInitiatedUssr) {
                Rlog.d(LOG_TAG, "onIncomingUSSD(): USSD_MODE_NW_RELEASE.");
                found.onUssdRelease();
            } else if (isUssdError) {
                found.onUssdFinishedError();
            } else if (isUssdhandleByStk) {
                found.onUssdStkHandling(ussdMessage, isUssdRequest);
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
            }
        } else { // pending USSD not found
            // The network may initiate its own USSD request

            // ignore everything that isnt a Notify or a Request
            // also, discard if there is no message to present

            /* M: SS part */
            //For ALPS01471897
            if (isUssdRequest) {
                Rlog.d(LOG_TAG, "The default value of UserInitiatedMMI is FALSE");
                mIsNetworkInitiatedUssr = true;
                Rlog.d(LOG_TAG, "onIncomingUSSD(): Network Initialized USSD");
            }

            if (!isUssdError && ussdMessage != null) {
                GsmMmiCode mmi;
                mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage,
                                                   isUssdRequest,
                                                   GsmCdmaPhone.this,
                                                   mUiccApplication.get());
                onNetworkInitiatedUssd(mmi);

            //MTK-START [mtk04070][111118][ALPS00093395]MTK added
            } else if (isUssdError) {
                GsmMmiCode mmi;
                mmi = GsmMmiCode.newNetworkInitiatedUssdError(ussdMessage,
                                                   isUssdRequest,
                                                   GsmCdmaPhone.this,
                                                   mUiccApplication.get());
                onNetworkInitiatedUssd(mmi);
            //MTK-END [mtk04070][111118][ALPS00093395]MTK added
            }
            /* M: SS part end */
        }

        /* M: SS part */
        if (isUssdRelease || isUssdError) {
            mIsNetworkInitiatedUssr = false;
        }
        /* M: SS part end */

        // if (found != null) {
        //     // Complete pending USSD

        //     if (isUssdRelease) {
        //         found.onUssdRelease();
        //     } else if (isUssdError) {
        //         found.onUssdFinishedError();
        //     } else {
        //         found.onUssdFinished(ussdMessage, isUssdRequest);
        //     }
        // } else { // pending USSD not found
        //     // The network may initiate its own USSD request

        //     // ignore everything that isnt a Notify or a Request
        //     // also, discard if there is no message to present
        //     if (!isUssdError && ussdMessage != null) {
        //         GsmMmiCode mmi;
        //         mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage,
        //                                            isUssdRequest,
        //                                            GsmCdmaPhone.this,
        //                                            mUiccApplication.get());
        //         onNetworkInitiatedUssd(mmi);
        //     }
        // }
    }

    /**
     * Make sure the network knows our preferred setting.
     */
    private void syncClirSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int clirSetting = sp.getInt(CLIR_KEY + getPhoneId(), -1);
        if (clirSetting >= 0) {
            mCi.setCLIR(clirSetting, null);
        }
    }

    private void handleRadioAvailable() {
        mCi.getBasebandVersion(obtainMessage(EVENT_GET_BASEBAND_VERSION_DONE));

        if (isPhoneTypeGsm()) {
            mCi.getIMEI(obtainMessage(EVENT_GET_IMEI_DONE));
            mCi.getIMEISV(obtainMessage(EVENT_GET_IMEISV_DONE));
        } else {
            mCi.getDeviceIdentity(obtainMessage(EVENT_GET_DEVICE_IDENTITY_DONE));
        }
        mCi.getRadioCapability(obtainMessage(EVENT_GET_RADIO_CAPABILITY));
        startLceAfterRadioIsAvailable();
    }

    private void handleRadioOn() {
        /* Proactively query voice radio technologies */
        mCi.getVoiceRadioTechnology(obtainMessage(EVENT_REQUEST_VOICE_RADIO_TECH_DONE));

        if (!isPhoneTypeGsm()) {
            mCdmaSubscriptionSource = mCdmaSSM.getCdmaSubscriptionSource();
        }

        // If this is on APM off, SIM may already be loaded. Send setPreferredNetworkType
        // request to RIL to preserve user setting across APM toggling
        setPreferredNetworkTypeIfSimLoaded();
    }

    private void handleRadioOffOrNotAvailable() {
        if (isPhoneTypeGsm()) {
            // Some MMI requests (eg USSD) are not completed
            // within the course of a CommandsInterface request
            // If the radio shuts off or resets while one of these
            // is pending, we need to clean up.

            for (int i = mPendingMMIs.size() - 1; i >= 0; i--) {
                if (((GsmMmiCode) mPendingMMIs.get(i)).isPendingUSSD()) {
                    ((GsmMmiCode) mPendingMMIs.get(i)).onUssdFinishedError();
                }
            }
        }
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            /// M: Set service state when WFC is not enabled @{
            // imsPhone.getServiceState().setStateOff();
            if (imsPhone.isWifiCallingEnabled() == false) {
                /// M: ALPS01973935.
                imsPhone.getServiceState().setState(ServiceState.STATE_OUT_OF_SERVICE);
            }
            /// @}
        }
        mRadioOffOrNotAvailableRegistrants.notifyRegistrants();
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;
        Message onComplete;

        switch (msg.what) {
            case EVENT_RADIO_AVAILABLE: {
                handleRadioAvailable();
            }
            break;

            case EVENT_GET_DEVICE_IDENTITY_DONE:{
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    setDeviceIdAbnormal(1);
                    Rlog.e(LOG_TAG, "Invalid Device Id");
                    break;
                }
                String[] respId = (String[])ar.result;
                mImei = respId[0];
                mImeiSv = respId[1];
                mEsn  =  respId[2];
                mMeid =  respId[3];
                setDeviceIdAbnormal(0);
                //add for fota begine
                if (!SystemProperties.get("ro.fota.device").equals("")) {
                    if (mPhoneId == PhoneConstants.SIM_ID_1 || mPhoneId == 10) { //SIM1
                        logd("fota: is SIM1, IMEI is: " + mImei);
                        if (SystemProperties.get("persist.sys.fota_deviceid1").equals("")) {
                            setSystemProperty("persist.sys.fota_deviceid1", mImei);
                        }
                    } else if (mPhoneId == PhoneConstants.SIM_ID_2 || mPhoneId == 11) { //SIM2
                        logd("fota: is SIM2, IMEI is: " + mImei);
                        if (SystemProperties.get("persist.sys.fota_deviceid2").equals("")) {
                            setSystemProperty("persist.sys.fota_deviceid2", mImei);
                        }
                    }
                    if (SystemProperties.get("persist.sys.fota_deviceid3").equals("")) {
                        logd("fota: SN is: " + mEsn);
                        setSystemProperty("persist.sys.fota_deviceid3", mEsn);
                    }
                    if (SystemProperties.get("persist.sys.fota_deviceid4").equals("")) {
                        logd("fota: MEID is: " + mMeid);
                        setSystemProperty("persist.sys.fota_deviceid4", mMeid);
                    }
                }
                //add for fota end
            }
            break;

            case EVENT_EMERGENCY_CALLBACK_MODE_ENTER:{
                handleEnterEmergencyCallbackMode(msg);
            }
            break;

            case  EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE:{
                handleExitEmergencyCallbackMode(msg);
            }
            break;

            case EVENT_RUIM_RECORDS_LOADED:
                logd("Event EVENT_RUIM_RECORDS_LOADED Received");
                updateCurrentCarrierInProvider();
                break;

            case EVENT_RADIO_ON:
                logd("Event EVENT_RADIO_ON Received");
                handleRadioOn();
                break;

            case EVENT_RIL_CONNECTED:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null && ar.result != null) {
                    mRilVersion = (Integer) ar.result;
                } else {
                    logd("Unexpected exception on EVENT_RIL_CONNECTED");
                    mRilVersion = -1;
                }
                break;

            case EVENT_VOICE_RADIO_TECH_CHANGED:
            case EVENT_REQUEST_VOICE_RADIO_TECH_DONE:
                String what = (msg.what == EVENT_VOICE_RADIO_TECH_CHANGED) ?
                        "EVENT_VOICE_RADIO_TECH_CHANGED" : "EVENT_REQUEST_VOICE_RADIO_TECH_DONE";
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    if ((ar.result != null) && (((int[]) ar.result).length != 0)) {
                        int newVoiceTech = ((int[]) ar.result)[0];
                        logd(what + ": newVoiceTech=" + newVoiceTech);
                        phoneObjectUpdater(newVoiceTech);
                    } else {
                        loge(what + ": has no tech!");
                    }
                } else {
                    loge(what + ": exception=" + ar.exception);
                }
                break;

            case EVENT_UPDATE_PHONE_OBJECT:
                phoneObjectUpdater(msg.arg1);
                break;

            case EVENT_CARRIER_CONFIG_CHANGED:
                // Only check for the voice radio tech if it not going to be updated by the voice
                // registration changes.
                if (!mContext.getResources().getBoolean(com.android.internal.R.bool.
                        config_switch_phone_on_voice_reg_state_change)) {
                    mCi.getVoiceRadioTechnology(obtainMessage(EVENT_REQUEST_VOICE_RADIO_TECH_DONE));
                }
                // Force update IMS service
                ImsManager.updateImsServiceConfig(mContext, mPhoneId, true);

                // Update broadcastEmergencyCallStateChanges
                CarrierConfigManager configMgr = (CarrierConfigManager)
                        getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
                PersistableBundle b = configMgr.getConfigForSubId(getSubId());
                if (b != null) {
                    boolean broadcastEmergencyCallStateChanges = b.getBoolean(
                            CarrierConfigManager.KEY_BROADCAST_EMERGENCY_CALL_STATE_CHANGES_BOOL);
                    logd("broadcastEmergencyCallStateChanges =" + broadcastEmergencyCallStateChanges);
                    setBroadcastEmergencyCallStateChanges(broadcastEmergencyCallStateChanges);
                } else {
                    loge("didn't get broadcastEmergencyCallStateChanges from carrier config");
                }

                // Changing the cdma roaming settings based carrier config.
                if (b != null) {
                    int config_cdma_roaming_mode = b.getInt(
                            CarrierConfigManager.KEY_CDMA_ROAMING_MODE_INT);
                    int current_cdma_roaming_mode =
                            Settings.Global.getInt(getContext().getContentResolver(),
                            Settings.Global.CDMA_ROAMING_MODE,
                            CarrierConfigManager.CDMA_ROAMING_MODE_RADIO_DEFAULT);
                    switch (config_cdma_roaming_mode) {
                        // Carrier's cdma_roaming_mode will overwrite the user's previous settings
                        // Keep the user's previous setting in global variable which will be used
                        // when carrier's setting is turn off.
                        case CarrierConfigManager.CDMA_ROAMING_MODE_HOME:
                        case CarrierConfigManager.CDMA_ROAMING_MODE_AFFILIATED:
                        case CarrierConfigManager.CDMA_ROAMING_MODE_ANY:
                            logd("cdma_roaming_mode is going to changed to "
                                    + config_cdma_roaming_mode);
                            setCdmaRoamingPreference(config_cdma_roaming_mode,
                                    obtainMessage(EVENT_SET_ROAMING_PREFERENCE_DONE));
                            break;

                        // When carrier's setting is turn off, change the cdma_roaming_mode to the
                        // previous user's setting
                        case CarrierConfigManager.CDMA_ROAMING_MODE_RADIO_DEFAULT:
                            if (current_cdma_roaming_mode != config_cdma_roaming_mode) {
                                logd("cdma_roaming_mode is going to changed to "
                                        + current_cdma_roaming_mode);
                                setCdmaRoamingPreference(current_cdma_roaming_mode,
                                        obtainMessage(EVENT_SET_ROAMING_PREFERENCE_DONE));
                            }

                        default:
                            loge("Invalid cdma_roaming_mode settings: "
                                    + config_cdma_roaming_mode);
                    }
                } else {
                    loge("didn't get the cdma_roaming_mode changes from the carrier config.");
                }

                // Load the ERI based on carrier config. Carrier might have their specific ERI.
                prepareEri();
                if (!isPhoneTypeGsm()) {
                    mSST.pollState();
                }

                break;

            case EVENT_SET_ROAMING_PREFERENCE_DONE:
                logd("cdma_roaming_mode change is done");
                break;

            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                logd("EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED");
                mCdmaSubscriptionSource = mCdmaSSM.getCdmaSubscriptionSource();
                break;

            case EVENT_REGISTERED_TO_NETWORK:
                logd("Event EVENT_REGISTERED_TO_NETWORK Received");
                //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
                if (FeatureOption.BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE) {
                    unlockRelatedSim();
                }
                //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END
                if (isPhoneTypeGsm()) {
                    syncClirSetting();
                }
                /// M: To query CFU @{
                sendMessage(obtainMessage(EVENT_QUERY_CFU));
                /// @}
                break;

            /* M: SS part */
            case EVENT_QUERY_CFU: // fallback from EVENT_REGISTERED_TO_NETWORK
                Rlog.d(LOG_TAG, "Receive EVENT_QUERY_CFU phoneid: " + getPhoneId() +
                    " needQueryCfu:" + needQueryCfu);
                if (needQueryCfu) {
                    String defaultQueryCfuMode = PhoneConstants.CFU_QUERY_TYPE_DEF_VALUE;
                    if (mSupplementaryServiceExt != null) {
                        defaultQueryCfuMode =
                            mSupplementaryServiceExt.getOpDefaultQueryCfuMode();
                        Rlog.d(LOG_TAG, "defaultQueryCfuMode = " + defaultQueryCfuMode);
                    }
                    String cfuSetting;
                    if (!TelephonyManager.from(mContext).isVoiceCapable()) {
                        // disable CFU query for non voice capable devices (i.e. tablet devices)
                        cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP, "1");
                    } else {
                        cfuSetting = SystemProperties.get(PhoneConstants.CFU_QUERY_TYPE_PROP,
                                defaultQueryCfuMode);
                    }
                    String isTestSim = "0";
                    /// M: Add for CMCC RRM test. @{
                    boolean isRRMEnv = false;
                    String operatorNumeric = null;
                    /// @}
                    if (mPhoneId == PhoneConstants.SIM_ID_1) {
                        isTestSim = SystemProperties.get("gsm.sim.ril.testsim", "0");
                    }
                    else if (mPhoneId == PhoneConstants.SIM_ID_2) {
                        isTestSim = SystemProperties.get("gsm.sim.ril.testsim.2", "0");
                    }

                    /// M: Add for CMCC RRM test. @{
                    // RRM test use 46602 as PLMN, which will not appear in the actual network
                    // Note that this should be modified when the PLMN for RRM test is changed
                    operatorNumeric = getServiceState().getOperatorNumeric();
                    if (operatorNumeric != null && operatorNumeric.equals("46602")) {
                        isRRMEnv = true;
                    }
                    /// @}
                    Rlog.d(LOG_TAG, "[GSMPhone] CFU_KEY = " + cfuSetting + " isTestSIM : " +
                        isTestSim + " isRRMEnv : " + isRRMEnv + " phoneid: " + getPhoneId());

                    if (isTestSim.equals("0") && isRRMEnv == false) { /// M: Add for CMCC RRM test.
                        String isChangedProp = CFU_QUERY_SIM_CHANGED_PROP + getPhoneId();
                        String isChanged = SystemProperties.get(isChangedProp, "0");

                        Rlog.d(LOG_TAG, "[GSMPhone] isChanged " + isChanged);
                        // 0 : default
                        // 1 : OFF
                        // 2 : ON
                        if (cfuSetting.equals("2")
                            || (cfuSetting.equals("0") && isChanged.equals("1"))) {
                            /* For solving ALPS01023811 */
                            mCfuQueryRetryCount = 0;
                            queryCfuOrWait();
                            needQueryCfu = false;
                            SystemProperties.set(isChangedProp, "0");
                        } else {
                            String utCfuMode = getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                    UT_CFU_NOTIFICATION_MODE_DISABLED);

                            Rlog.d(LOG_TAG, "utCfuMode: " + utCfuMode);
                            if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                                IccRecords r = mIccRecords.get();
                                if (r != null) {
                                    setVoiceCallForwardingFlag(1, true, "");
                                }
                            } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                                IccRecords r = mIccRecords.get();
                                if (r != null) {
                                    setVoiceCallForwardingFlag(1, false, "");
                                }
                            }
                        }
                    } else {
                        String utCfuMode = getSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_DISABLED);

                        Rlog.d(LOG_TAG, "utCfuMode: " + utCfuMode);
                        if (UT_CFU_NOTIFICATION_MODE_ON.equals(utCfuMode)) {
                            IccRecords r = mIccRecords.get();
                            if (r != null) {
                                setVoiceCallForwardingFlag(1, true, "");
                            }
                        } else if (UT_CFU_NOTIFICATION_MODE_OFF.equals(utCfuMode)) {
                            IccRecords r = mIccRecords.get();
                            if (r != null) {
                                setVoiceCallForwardingFlag(1, false, "");
                            }
                        }
                    }
                }
                /* M: SS part end */
                break;

            case EVENT_SIM_RECORDS_LOADED:
                if (isPhoneTypeGsm()) {
                    updateCurrentCarrierInProvider();

                    // Check if this is a different SIM than the previous one. If so unset the
                    // voice mail number.
                    String imsi = getVmSimImsi();
                    String imsiFromSIM = getSubscriberId();
                    if (imsi != null && imsiFromSIM != null && !imsiFromSIM.equals(imsi)) {
                        /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
                        if (FeatureOption.BIRD_VOICE_MAIL_NUMBER_FROM_SIM) {
					        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
					        // 当相应卡槽更换SIM卡后，是否清除用户对之前SIM卡的VM number设置
					        boolean clear_if_change_sim = sp.getBoolean("clear_if_change", false);
					        if (clear_if_change_sim && imsi != null && imsiFromSIM != null && !imsiFromSIM.equals(imsi)) {
						        SharedPreferences.Editor editor = sp.edit();
						        editor.remove(getVmSimImsi());
						        editor.apply();
						        setVmSimImsi(null);
					        }
				        } else {
                            storeVoiceMailNumber(null);
                            setVmSimImsi(null);
                        }
                        /*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
                    }
                }

                mSimRecordsLoadedRegistrants.notifyRegistrants();
                /*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]BEGIN */
                if (FeatureOption.BIRD_VOICE_MESSAGE_CHANGE) {
                    updateVoiceMail();
                }
                /*[BIRD][BIRD_VOICE_MESSAGE_CHANGE][未读语音信箱通知在手机重启后不要消失][yangbo][20171028]END */
                break;

            case EVENT_GET_BASEBAND_VERSION_DONE:
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    break;
                }

                if (DBG) logd("Baseband version: " + ar.result);
                TelephonyManager.from(mContext).setBasebandVersionForPhone(getPhoneId(),
                        (String)ar.result);
            break;

            case EVENT_GET_IMEI_DONE:
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    Rlog.e(LOG_TAG, "Invalid DeviceId (IMEI)");
                    setDeviceIdAbnormal(1);
                    break;
                }

                mImei = (String)ar.result;
                //add for fota begine
                if (!SystemProperties.get("ro.fota.device").equals("")) {
                    if (mPhoneId == PhoneConstants.SIM_ID_1 || mPhoneId == 10) { //SIM1
                        logd("fota: is SIM1, IMEI is: " + mImei);
                        if (SystemProperties.get("persist.sys.fota_deviceid1").equals("")) {
                            setSystemProperty("persist.sys.fota_deviceid1", mImei);
                        }
                    } else if (mPhoneId == PhoneConstants.SIM_ID_2 || mPhoneId == 11) { //SIM2
                        logd("fota: is SIM2, IMEI is: " + mImei);
                        if (SystemProperties.get("persist.sys.fota_deviceid2").equals("")) {
                            setSystemProperty("persist.sys.fota_deviceid2", mImei);
                        }
                    }
                }
                //add for fota begine
                Rlog.d(LOG_TAG, "IMEI: " + mImei);

                try {
                    Long.parseLong(mImei);
                    setDeviceIdAbnormal(0);
                } catch (NumberFormatException e) {
                    setDeviceIdAbnormal(1);
                    Rlog.e(LOG_TAG, "Invalid DeviceId (IMEI) Format: " + e.toString() + ")");
                }
            break;

            case EVENT_GET_IMEISV_DONE:
                ar = (AsyncResult)msg.obj;

                if (ar.exception != null) {
                    break;
                }

                mImeiSv = (String)ar.result;
            break;

            case EVENT_USSD:
                ar = (AsyncResult)msg.obj;

                String[] ussdResult = (String[]) ar.result;

                if (ussdResult.length > 1) {
                    try {
                        onIncomingUSSD(Integer.parseInt(ussdResult[0]), ussdResult[1]);
                    } catch (NumberFormatException e) {
                        Rlog.w(LOG_TAG, "error parsing USSD");
                    }
                }
            break;

            case EVENT_RADIO_OFF_OR_NOT_AVAILABLE: {
                logd("Event EVENT_RADIO_OFF_OR_NOT_AVAILABLE Received");
                handleRadioOffOrNotAvailable();
                break;
            }

            case EVENT_SSN:
                logd("Event EVENT_SSN Received");
                if (isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    SuppServiceNotification not = (SuppServiceNotification) ar.result;
                   /// M: CC: Proprietary CRSS handling @{
                    if (mSsnRegistrants.size() == 0) {
                        mCachedSsn = ar;
                    }
                    /// @}
                    mSsnRegistrants.notifyRegistrants(ar);
                }
                break;

            case EVENT_SET_CALL_FORWARD_DONE:
                ar = (AsyncResult)msg.obj;
                IccRecords r = mIccRecords.get();
                Cfu cfu = (Cfu) ar.userObj;
                if (ar.exception == null && r != null) {
                    //only CFU would go in this case.
                    //because only CFU use EVENT_SET_CALL_FORWARD_DONE.
                    //So no need to check it is for CFU.
                    if (queryCFUAgainAfterSet()) {
                        if (ar.result != null) {
                            CallForwardInfo[] cfinfo = (CallForwardInfo[]) ar.result;
                            if (cfinfo == null || cfinfo.length == 0) {
                                Rlog.i(LOG_TAG, "cfinfo is null or length is 0.");
                            } else {
                                Rlog.i(LOG_TAG, "[EVENT_SET_CALL_FORWARD_DONE check cfinfo");
                                for (int i = 0 ; i < cfinfo.length ; i++) {
                                    if ((cfinfo[i].serviceClass & SERVICE_CLASS_VOICE) != 0) {
                                        setVoiceCallForwardingFlag(1, (cfinfo[i].status == 1),
                                            cfinfo[i].number);
                                        break;
                                    }
                                }
                            }
                        } else {
                            Rlog.i(LOG_TAG, "ar.result is null.");
                        }
                    } else {
                        setVoiceCallForwardingFlag(1, msg.arg1 == 1, cfu.mSetCfNumber);
                    }
                }
                if ((ar.exception != null) && (msg.arg2 != 0)) {
                    if (msg.arg2 == 1) {
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_ON);
                    } else {
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_OFF);
                    }
                }
                if (cfu.mOnComplete != null) {
                    AsyncResult.forMessage(cfu.mOnComplete, ar.result, ar.exception);
                    cfu.mOnComplete.sendToTarget();
                }
                break;

            case EVENT_GET_CALL_WAITING_DONE:
                ar = (AsyncResult) msg.obj;
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_WAITING_]ar.exception = " + ar.exception);

                onComplete = (Message) ar.userObj;
                if (ar.exception == null) {
                    int[] cwArray = (int[]) ar.result;
                    // If cwArray[0] is = 1, then cwArray[1] must follow,
                    // with the TS 27.007 service class bit vector of services
                    // for which call waiting is enabled.
                    try {
                        Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE cwArray[0]:cwArray[1] = "
                                + cwArray[0] + ":" + cwArray[1]);

                        boolean csEnable = ((cwArray[0] == 1) &&
                            ((cwArray[1] & 0x01) == SERVICE_CLASS_VOICE));

                        setTerminalBasedCallWaiting(csEnable, null);

                        if (onComplete != null) {
                            AsyncResult.forMessage(onComplete, ar.result, null);
                            onComplete.sendToTarget();
                            break;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE: improper result: err ="
                                + e.getMessage());
                        if (onComplete != null) {
                            AsyncResult.forMessage(onComplete, ar.result, null);
                            onComplete.sendToTarget();
                            break;
                        }
                    }
                } else {
                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                        onComplete.sendToTarget();
                        break;
                    }
                }
                break;

            case EVENT_SET_CALL_WAITING_DONE:
                ar = (AsyncResult) msg.obj;
                onComplete = (Message) ar.userObj;

                if (ar.exception != null) {
                    Rlog.d(LOG_TAG, "EVENT_SET_CALL_WAITING_DONE: ar.exception=" + ar.exception);

                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                        onComplete.sendToTarget();
                        break;
                    }
                } else {
                    boolean enable = msg.arg1 == 1 ? true : false;
                    setTerminalBasedCallWaiting(enable, onComplete);
                }
                break;

            case EVENT_SET_VM_NUMBER_DONE:
                ar = (AsyncResult)msg.obj;
                if ((isPhoneTypeGsm() && IccVmNotSupportedException.class.isInstance(ar.exception)) ||
                        (!isPhoneTypeGsm() && IccException.class.isInstance(ar.exception))){
                    storeVoiceMailNumber(mVmNumber);
                    ar.exception = null;
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;


            case EVENT_GET_CALL_FORWARD_DONE:
                /* M: SS part */ //TODO need check mPhoneID
                /* For solving ALPS00997715 */
                Rlog.d(LOG_TAG, "mPhoneId= " + mPhoneId + "subId=" + getSubId());
                setSystemProperty(CFU_QUERY_PROPERTY_NAME + mPhoneId, "0");
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    handleCfuQueryResult((CallForwardInfo[])ar.result);
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;

            case EVENT_SET_NETWORK_AUTOMATIC:
                // Automatic network selection from EF_CSP SIM record
                ar = (AsyncResult) msg.obj;
                if (mSST.mSS.getIsManualSelection()) {
                    setNetworkSelectionModeAutomatic((Message) ar.result);
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: set to automatic");
                } else {
                    // prevent duplicate request which will push current PLMN to low priority
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: already automatic, ignore");
                }
                break;

            case EVENT_ICC_RECORD_EVENTS:
                Rlog.d(LOG_TAG, "EVENT_ICC_RECORD_EVENTS");
                ar = (AsyncResult)msg.obj;
                processIccRecordEvents((Integer)ar.result);
                break;

            case EVENT_SET_CLIR_COMPLETE:
                ar = (AsyncResult)msg.obj;
                if (ar.exception == null) {
                    saveClirSetting(msg.arg1);
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;

            case EVENT_SS:
                ar = (AsyncResult)msg.obj;
                logd("Event EVENT_SS received");
                if (isPhoneTypeGsm()) {
                    // SS data is already being handled through MMI codes.
                    // So, this result if processed as MMI response would help
                    // in re-using the existing functionality.
                    GsmMmiCode mmi = new GsmMmiCode(this, mUiccApplication.get());
                    mmi.processSsData(ar);
                }
                break;

            case EVENT_GET_RADIO_CAPABILITY:
                ar = (AsyncResult) msg.obj;
                RadioCapability rc = (RadioCapability) ar.result;
                if (ar.exception != null) {
                    Rlog.d(LOG_TAG, "get phone radio capability fail, no need to change " +
                            "mRadioCapability");
                } else {
                    radioCapabilityUpdated(rc);
                }
                Rlog.d(LOG_TAG, "EVENT_GET_RADIO_CAPABILITY: phone rc: " + rc);
                break;

            /// M:
            case EVENT_IMS_UT_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar == null) {
                    Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error AsyncResult null!");
                } else {
                    SuppSrvRequest ss = (SuppSrvRequest) ar.userObj;
                    if (ss == null) {
                        Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error SuppSrvRequest null!");
                    } else if (SuppSrvRequest.SUPP_SRV_REQ_SET_CF_IN_TIME_SLOT
                            == ss.getRequestCode()) {
                        if (ar.exception == null) {
                            ss.mParcel.setDataPosition(0);
                            Rlog.d(LOG_TAG, "EVENT_IMS_UT_DONE: SUPP_SRV_REQ_SET_CF_IN_TIME_SLOT");
                            int commandInterfaceCFAction = ss.mParcel.readInt();
                            int commandInterfaceCFReason = ss.mParcel.readInt();
                            String dialingNumber = ss.mParcel.readString();
                            if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                                if (isCfEnable(commandInterfaceCFAction)) {
                                    setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                            UT_CFU_NOTIFICATION_MODE_ON);
                                } else {
                                    setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                            UT_CFU_NOTIFICATION_MODE_OFF);
                                }
                            }
                        }
                        onComplete = ss.getResultCallback();
                        if (onComplete != null) {
                            AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                            onComplete.sendToTarget();
                        }
                        ss.mParcel.recycle();
                    } else {
                        CommandException cmdException = null;
                        ImsException imsException = null;
                        if ((ar.exception != null) && (ar.exception instanceof CommandException)) {
                            cmdException = (CommandException) ar.exception;
                        }
                        if ((ar.exception != null) && (ar.exception instanceof ImsException)) {
                            imsException = (ImsException) ar.exception;
                        }
                        if ((cmdException != null) && (cmdException.getCommandError()
                                == CommandException.Error.UT_XCAP_403_FORBIDDEN)) {
                            setCsFallbackStatus(PhoneConstants.UT_CSFB_UNTIL_NEXT_BOOT);
                            if (isNotSupportUtToCS()) {
                                Rlog.d(LOG_TAG, "UT_XCAP_403_FORBIDDEN.");
                                ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_403_FORBIDDEN);
                                onComplete = ss.getResultCallback();
                                if (onComplete != null) {
                                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                                    onComplete.sendToTarget();
                                }
                                ss.mParcel.recycle();
                            } else {
                                Rlog.d(LOG_TAG, "Csfallback next_reboot.");
                                Message msgCSFB = obtainMessage(EVENT_IMS_UT_CSFB, ss);
                                sendMessage(msgCSFB);
                            }
                        } else if ((cmdException != null) && (cmdException.getCommandError()
                                == CommandException.Error.UT_UNKNOWN_HOST)) {
                            if (isNotSupportUtToCS()) {
                                Rlog.d(LOG_TAG, "CommandException.Error.UT_UNKNOWN_HOST.");
                                ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_403_FORBIDDEN);
                                onComplete = ss.getResultCallback();
                                if (onComplete != null) {
                                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                                    onComplete.sendToTarget();
                                }
                                ss.mParcel.recycle();
                            } else {
                                Rlog.d(LOG_TAG, "Csfallback once.");
                                setCsFallbackStatus(PhoneConstants.UT_CSFB_ONCE);
                                Message msgCSFB = obtainMessage(EVENT_IMS_UT_CSFB, ss);
                                sendMessage(msgCSFB);
                            }
                        } else if ((imsException != null) && (imsException.getCode()
                                == ImsReasonInfo.CODE_UT_XCAP_403_FORBIDDEN)) {
                            setCsFallbackStatus(PhoneConstants.UT_CSFB_UNTIL_NEXT_BOOT);
                            if (isNotSupportUtToCS()) {
                                Rlog.d(LOG_TAG, "ImsReasonInfo.CODE_UT_XCAP_403_FORBIDDEN.");
                                ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_403_FORBIDDEN);
                                onComplete = ss.getResultCallback();
                                if (onComplete != null) {
                                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                                    onComplete.sendToTarget();
                                }
                                ss.mParcel.recycle();
                            }  else {
                                Rlog.d(LOG_TAG, "Csfallback next_reboot.");
                                Message msgCSFB = obtainMessage(EVENT_IMS_UT_CSFB, ss);
                                sendMessage(msgCSFB);
                            }
                        } else if ((imsException != null) && (imsException.getCode()
                                == ImsReasonInfo.CODE_UT_UNKNOWN_HOST)) {
                            if (isNotSupportUtToCS()) {
                                Rlog.d(LOG_TAG, "CommandException.Error.UT_UNKNOWN_HOST.");
                                ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_403_FORBIDDEN);
                                onComplete = ss.getResultCallback();
                                if (onComplete != null) {
                                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                                    onComplete.sendToTarget();
                                }
                                ss.mParcel.recycle();
                            } else {
                                Rlog.d(LOG_TAG, "Csfallback once.");
                                setCsFallbackStatus(PhoneConstants.UT_CSFB_ONCE);
                                Message msgCSFB = obtainMessage(EVENT_IMS_UT_CSFB, ss);
                                sendMessage(msgCSFB);
                            }
                        } else {
                            if ((ar.exception == null) &&
                                    (SuppSrvRequest.SUPP_SRV_REQ_SET_CF == ss.getRequestCode())) {
                                ss.mParcel.setDataPosition(0);
                                Rlog.d(LOG_TAG, "EVENT_IMS_UT_DONE: SUPP_SRV_REQ_SET_CF");
                                int commandInterfaceCFAction = ss.mParcel.readInt();
                                int commandInterfaceCFReason = ss.mParcel.readInt();
                                String dialingNumber = ss.mParcel.readString();
                                if (commandInterfaceCFReason == CF_REASON_UNCONDITIONAL) {
                                    if (queryCFUAgainAfterSet()) {
                                        if (ar.result != null) {
                                            CallForwardInfo[] cfinfo =
                                                (CallForwardInfo[]) ar.result;

                                            if (cfinfo == null || cfinfo.length == 0) {
                                                Rlog.i(LOG_TAG, "cfinfo is null or 0.");
                                            } else {
                                                for (int i = 0 ; i < cfinfo.length ; i++) {
                                                    if ((cfinfo[i].serviceClass
                                                        & SERVICE_CLASS_VOICE) != 0) {
                                                        if (cfinfo[i].status == 1) {
                                                            Rlog.d(LOG_TAG,
                                                                "Set enable, serviceClass: "
                                                                + cfinfo[i].serviceClass);
                                                            setSystemProperty(
                                                                PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                                                UT_CFU_NOTIFICATION_MODE_ON);
                                                        } else {
                                                            Rlog.d(LOG_TAG,
                                                                "Set disable, serviceClass: "
                                                                + cfinfo[i].serviceClass);
                                                            setSystemProperty(
                                                                PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                                                UT_CFU_NOTIFICATION_MODE_OFF);
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            Rlog.i(LOG_TAG, "ar.result is null.");
                                        }
                                    } else {
                                        if (isCfEnable(commandInterfaceCFAction)) {
                                            setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                                    UT_CFU_NOTIFICATION_MODE_ON);
                                        } else {
                                            setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                                    UT_CFU_NOTIFICATION_MODE_OFF);
                                        }
                                    }
                                }
                            } else if ((imsException != null) && (imsException.getCode()
                                    == ImsReasonInfo.CODE_UT_XCAP_404_NOT_FOUND)) {
                                // Only consider CB && op05 and response 404 status.
                                // Get it from ImsPhone.java
                                // if not CB && op05, then transfer to GENERIC_FAILURE
                                if (isOpTransferXcap404()
                                    && (ss.getRequestCode() == SuppSrvRequest.SUPP_SRV_REQ_GET_CB
                                    || ss.getRequestCode() == SuppSrvRequest.SUPP_SRV_REQ_SET_CB)) {
                                    ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_404_NOT_FOUND);
                                } else {
                                    ar.exception = new CommandException(
                                        CommandException.Error.GENERIC_FAILURE);
                                }
                            } else if ((cmdException != null) && (cmdException.getCommandError()
                                == CommandException.Error.UT_XCAP_404_NOT_FOUND)) {
                                // Only consider CB && op05 and response 404 status.
                                // Get it from ImsPhone.java
                                // if not CB && op05, then transfer to GENERIC_FAILURE
                                if (isOpTransferXcap404()
                                    && (ss.getRequestCode() == SuppSrvRequest.SUPP_SRV_REQ_GET_CB
                                    || ss.getRequestCode() == SuppSrvRequest.SUPP_SRV_REQ_SET_CB)) {
                                    Rlog.i(LOG_TAG, "GSMPhone get UT_XCAP_404_NOT_FOUND.");
                                } else {
                                    ar.exception = new CommandException(
                                        CommandException.Error.GENERIC_FAILURE);
                                }
                            } else if ((imsException != null) && (imsException.getCode()
                                    == ImsReasonInfo.CODE_UT_XCAP_409_CONFLICT)) {
                                if (!isEnableXcapHttpResponse409()) {
                                    // Transfer back to gereric failure.
                                    Rlog.i(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT, " +
                                        "return GENERIC_FAILURE");
                                    ar.exception = new CommandException(
                                        CommandException.Error.GENERIC_FAILURE);
                                } else {
                                    Rlog.i(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT.");
                                    ar.exception = new CommandException(
                                        CommandException.Error.UT_XCAP_409_CONFLICT);
                                }
                            } else if ((cmdException != null) && (cmdException.getCommandError()
                                == CommandException.Error.UT_XCAP_409_CONFLICT)) {
                                if (!isEnableXcapHttpResponse409()) {
                                    // Transfer back to gereric failure.
                                    Rlog.i(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT, " +
                                        "return GENERIC_FAILURE");
                                    ar.exception = new CommandException(
                                        CommandException.Error.GENERIC_FAILURE);
                                } else {
                                    Rlog.i(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT.");
                                }
                            }

                            onComplete = ss.getResultCallback();
                            if (onComplete != null) {
                                AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                                onComplete.sendToTarget();
                            }
                            ss.mParcel.recycle();
                        }
                    }
                }
                break;

            case EVENT_IMS_UT_CSFB:
                handleImsUtCsfb(msg);
                break;
            case EVENT_USSI_CSFB:
                handleUssiCsfb((String)msg.obj);
                break;
            ///@}

            /// M: SS OP01 Ut @{
            case EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE:
                Rlog.d(LOG_TAG, "mPhoneId = " + mPhoneId + ", subId = " + getSubId());
                setSystemProperty(CFU_QUERY_PROPERTY_NAME + mPhoneId, "0");
                ar = (AsyncResult) msg.obj;
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]ar.exception = "
                        + ar.exception);
                if (ar.exception == null) {
                    handleCfuInTimeSlotQueryResult((CallForwardInfoEx[]) ar.result);
                }
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]msg.arg1 = "
                        + msg.arg1);
                if ((ar.exception != null) && (ar.exception instanceof CommandException)) {
                    CommandException cmdException = (CommandException) ar.exception;
                    if ((msg.arg1 == 1) && (cmdException != null) &&
                            (cmdException.getCommandError() ==
                                    CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED)) {
                        if (mSST != null && mSST.mSS != null
                                && (mSST.mSS.getState() == ServiceState.STATE_IN_SERVICE)) {
                            getCallForwardingOption(CF_REASON_UNCONDITIONAL,
                                    obtainMessage(EVENT_GET_CALL_FORWARD_DONE));
                        }
                    }
                }
                onComplete = (Message) ar.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar.result, ar.exception);
                    onComplete.sendToTarget();
                }
                break;

            case EVENT_SET_CALL_FORWARD_TIME_SLOT_DONE:
                ar = (AsyncResult) msg.obj;
                IccRecords records = mIccRecords.get();
                CfuEx cfuEx = (CfuEx) ar.userObj;
                if (ar.exception == null && records != null) {
                    records.setVoiceCallForwardingFlag(1, msg.arg1 == 1, cfuEx.mSetCfNumber);
                    saveTimeSlot(cfuEx.mSetTimeSlot);
                    if (msg.arg1 == 1) {
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_ON);
                    } else {
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,
                                UT_CFU_NOTIFICATION_MODE_OFF);
                    }
                }
                if (cfuEx.mOnComplete != null) {
                    AsyncResult.forMessage(cfuEx.mOnComplete, ar.result, ar.exception);
                    cfuEx.mOnComplete.sendToTarget();
                }
                break;
            /// @}

            /// M: CC: Proprietary incoming call handling @{
            case EVENT_VOICE_CALL_INCOMING_INDICATION:
                Rlog.d(LOG_TAG, "handle EVENT_VOICE_CALL_INCOMING_INDICATION");
                mVoiceCallIncomingIndicationRegistrants.notifyRegistrants(
                        new AsyncResult(null, this, null));
                break;
            ///@}

            /// M: CC: Proprietary CRSS handling @{
            case EVENT_CRSS_IND:
                ar = (AsyncResult) msg.obj;
                SuppCrssNotification noti = (SuppCrssNotification) ar.result;

                /// M: CC: number presentation via CLIP @{
                if (noti.code == SuppCrssNotification.CRSS_CALLING_LINE_ID_PREST) {
                    // update numberPresentation in gsmconnection
                    if (getRingingCall().getState() != GsmCdmaCall.State.IDLE) {
                        Connection cn = (Connection) (getRingingCall().getConnections().get(0));
                        /* CLI validity value,
                          0: PRESENTATION_ALLOWED,
                          1: PRESENTATION_RESTRICTED,
                          2: PRESENTATION_UNKNOWN
                          3: PRESENTATION_PAYPHONE
                        */
                        Rlog.d(LOG_TAG, "set number presentation to connection : "
                                + noti.cli_validity);
                        switch (noti.cli_validity) {
                            case 1:
                                cn.setNumberPresentation(PhoneConstants.PRESENTATION_RESTRICTED);
                                break;

                            case 2:
                                cn.setNumberPresentation(PhoneConstants.PRESENTATION_UNKNOWN);
                                break;

                            case 3:
                                cn.setNumberPresentation(PhoneConstants.PRESENTATION_PAYPHONE);
                                break;

                            case 0:
                            default:
                                cn.setNumberPresentation(PhoneConstants.PRESENTATION_ALLOWED);
                                break;
                        }
                    }
                /// @}
                /// M: CC: Redirecting number via COLP @{
                } else if (noti.code == SuppCrssNotification.CRSS_CONNECTED_LINE_ID_PREST) {
                /* If the phone number in +COLP is different from the address of connection,
                       store it to connection as redirecting address.
                    */
                    Rlog.d(LOG_TAG, "[COLP]noti.number = " + noti.number);
                    if (getForegroundCall().getState() != GsmCdmaCall.State.IDLE) {
                        Connection cn = (Connection) (getForegroundCall().getConnections().get(0));
                        if ((cn != null) &&
                            (cn.getAddress() != null) &&
                            !cn.getAddress().equals(noti.number)) {
                           cn.setRedirectingAddress(noti.number);
                           Rlog.d(LOG_TAG, "[COLP]Redirecting address = " +
                            cn.getRedirectingAddress());
                        }
                    }
                }
                /// @}

                if (mCallRelatedSuppSvcRegistrants.size() == 0) {
                    mCachedCrssn = ar;
                }
                mCallRelatedSuppSvcRegistrants.notifyRegistrants(ar);
                break;
                /// @}
            default:
                super.handleMessage(msg);
        }
    }
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
    public void unlockRelatedSim() {
        //(1) 卡1成功注册上网络了
        int birdunlock = Settings.System.getInt(mContext.getContentResolver(), "birdunlockchina", 0);
        if(isFirstBoot && (getPhoneId() == PhoneConstants.SIM_ID_1)) {
            //TODO:调用TelephonyManager.getSimState(PhoneConstants.SIM_ID_2)获取卡2状态,
            //如果为SIM_STATE_NETWORK_LOCKED，则调用supplyNetworkDeper sonalization()来解锁卡2
            int state = TelephonyManager.from(mContext).getSimState(PhoneConstants.SIM_ID_2);
            Settings.System.putInt(mContext.getContentResolver(), "birdtranslock", 1);
            Log.e("chenguangxiang", "supply state : " + state);
            if (state == TelephonyManager.SIM_STATE_NETWORK_LOCKED) {
                try {
                    Log.e("chenguangxiang", "supply Phone2" );
                    int [] subIds = SubscriptionManager.getSubId(1);
                    ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"))
                            .supplyNetworkDepersonalization(subIds[0], "12345678");
                    //ITelephonyEx.Stub.asInterface(ServiceManager.checkService("phoneEx")).
                        //supplyNetworkDepersonalization(PhoneConstants.SIM_ID_2, "12345678");
                } catch (RemoteException e) {
                    Log.e("chenguangxiang", "supplyNetworkDepersonalization got exception: " + e.getMessage());
                }
            }
            isFirstBoot = false; //确保下次注册网络不会引起解锁操作
            TelephonyManager tempTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            int [] subId1 = SubscriptionManager.getSubId(0);
            int sub1 = (subId1 != null) ? subId1[0] : SubscriptionManager.getDefaultSubId();
            String imsia = tempTelephonyManager.getSubscriberId(sub1);
            
            boolean isRely = false;
            String [] birdrely = {"41401", "46000", "46001", "46002", "46007", "46009"};
            for (int i = 0; i < birdrely.length; i++) {
                if ((imsia != null) && (imsia.startsWith(birdrely[i]))) {
                    isRely = true;
                    break;
                }
            }
            if (isRely == false) {
                Settings.System.putInt(mContext.getContentResolver(), "birdsimchina", 1);
            }
        } else if (getPhoneId() == PhoneConstants.SIM_ID_2) {
            TelephonyManager tempTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            int [] subId2 = SubscriptionManager.getSubId(1);
            int sub2 = (subId2 != null) ? subId2[0] : SubscriptionManager.getDefaultSubId();
            String imsib = tempTelephonyManager.getSubscriberId(sub2);
            
            boolean isRely = false;
            String [] birdrely = {"41401", "46000", "46001", "46002", "46007", "46009"};
            for (int i = 0; i < birdrely.length; i++) {
                if ((imsib != null) && (imsib.startsWith(birdrely[i]))) {
                    isRely = true;
                    break;
                }
            }
            if (isRely == false) {
                Settings.System.putInt(mContext.getContentResolver(), "birdsimchina", 1);
            }
        }
    }
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END

    public UiccCardApplication getUiccCardApplication() {
        if (isPhoneTypeGsm()) {
            return mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP);
        } else {
            return mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_3GPP2);
        }
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication = null;

        // Update mIsimUiccRecords
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            newUiccApplication =
                    mUiccController.getUiccCardApplication(mPhoneId, UiccController.APP_FAM_IMS);
            IsimUiccRecords newIsimUiccRecords = null;

            if (newUiccApplication != null) {
                newIsimUiccRecords = (IsimUiccRecords) newUiccApplication.getIccRecords();
                if (DBG) logd("New ISIM application found");
            }
            mIsimUiccRecords = newIsimUiccRecords;
        }

        // Update mSimRecords
        if (mSimRecords != null) {
            mSimRecords.unregisterForRecordsLoaded(this);
        }
        if (isPhoneTypeCdmaLte()) {
            newUiccApplication = mUiccController.getUiccCardApplication(mPhoneId,
                    UiccController.APP_FAM_3GPP);
            SIMRecords newSimRecords = null;
            if (newUiccApplication != null) {
                newSimRecords = (SIMRecords) newUiccApplication.getIccRecords();
            }
            mSimRecords = newSimRecords;
            if (mSimRecords != null) {
                mSimRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
            }
        } else {
            mSimRecords = null;
        }

        // Update mIccRecords, mUiccApplication, mIccPhoneBookIntManager
        newUiccApplication = getUiccCardApplication();
        if (!isPhoneTypeGsm() && newUiccApplication == null) {
            logd("can't find 3GPP2 application; trying APP_FAM_3GPP");
            newUiccApplication = mUiccController.getUiccCardApplication(mPhoneId,
                    UiccController.APP_FAM_3GPP);
        }

        UiccCardApplication app = mUiccApplication.get();
        // MTK-START
        IccRecords newIccRecord =
                (newUiccApplication != null)? newUiccApplication.getIccRecords() : null;
        if ((app != newUiccApplication) || (mIccRecords.get() != newIccRecord)) {
        //if (app != newUiccApplication) {
        // MTK-END
            if (app != null) {
                if (DBG) logd("Removing stale icc objects.");
                if (mIccRecords.get() != null) {
                    unregisterForIccRecordEvents();
                    mIccPhoneBookIntManager.updateIccRecords(null);
                }
                mIccRecords.set(null);
                mUiccApplication.set(null);
            }
            if (newUiccApplication != null) {
                if (DBG) {
                    logd("New Uicc application found. type = " + newUiccApplication.getType());
                }
                mUiccApplication.set(newUiccApplication);
                mIccRecords.set(newUiccApplication.getIccRecords());
                registerForIccRecordEvents();
                mIccPhoneBookIntManager.updateIccRecords(mIccRecords.get());
            }
        }
    }

    private void processIccRecordEvents(int eventCode) {
        switch (eventCode) {
            case IccRecords.EVENT_CFI:
                Rlog.d(LOG_TAG, "processIccRecordEvents");
                notifyCallForwardingIndicator();
                break;
        }
    }

    /**
     * Sets the "current" field in the telephony provider according to the SIM's operator
     *
     * @return true for success; false otherwise.
     */
    @Override
    public boolean updateCurrentCarrierInProvider() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            long currentDds = SubscriptionManager.getDefaultDataSubscriptionId();
            String operatorNumeric = getOperatorNumeric();

            logd("updateCurrentCarrierInProvider: mSubId = " + getSubId()
                    + " currentDds = " + currentDds + " operatorNumeric = " + operatorNumeric);

            if (!TextUtils.isEmpty(operatorNumeric) && (getSubId() == currentDds)) {
                try {
                    Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                    ContentValues map = new ContentValues();
                    map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                    mContext.getContentResolver().insert(uri, map);
                    return true;
                } catch (SQLException e) {
                    Rlog.e(LOG_TAG, "Can't store current operator", e);
                }
            }
            return false;
        } else {
            return true;
        }
    }

    //CDMA
    /**
     * Sets the "current" field in the telephony provider according to the
     * build-time operator numeric property
     *
     * @return true for success; false otherwise.
     */
    private boolean updateCurrentCarrierInProvider(String operatorNumeric) {
        if (isPhoneTypeCdma()
                || (isPhoneTypeCdmaLte() && mUiccController.getUiccCardApplication(mPhoneId,
                        UiccController.APP_FAM_3GPP) == null)) {
            logd("CDMAPhone: updateCurrentCarrierInProvider called");
            if (!TextUtils.isEmpty(operatorNumeric)) {
                try {
                    Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                    ContentValues map = new ContentValues();
                    map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                    logd("updateCurrentCarrierInProvider from system: numeric=" + operatorNumeric);
                    getContext().getContentResolver().insert(uri, map);

                    // Updates MCC MNC device configuration information
                    logd("update mccmnc=" + operatorNumeric);
                    MccTable.updateMccMncConfiguration(mContext, operatorNumeric, false);

                    return true;
                } catch (SQLException e) {
                    Rlog.e(LOG_TAG, "Can't store current operator", e);
                }
            }
            return false;
        } else { // isPhoneTypeCdmaLte()
            if (DBG) logd("updateCurrentCarrierInProvider not updated X retVal=" + true);
            return true;
        }
    }

    private void handleCfuQueryResult(CallForwardInfo[] infos) {
        IccRecords r = mIccRecords.get();
        if (r != null) {
            if (infos == null || infos.length == 0) {
                // Assume the default is not active
                // Set unconditional CFF in SIM to false
                setVoiceCallForwardingFlag(1, false, null);
                setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE,UT_CFU_NOTIFICATION_MODE_OFF);
            } else {
                for (int i = 0, s = infos.length; i < s; i++) {
                    if ((infos[i].serviceClass & SERVICE_CLASS_VOICE) != 0) {
                        setVoiceCallForwardingFlag(1, (infos[i].status == 1),
                            infos[i].number);
                        String mode = infos[i].status == 1 ?
                            UT_CFU_NOTIFICATION_MODE_ON : UT_CFU_NOTIFICATION_MODE_OFF;
                        setSystemProperty(PROPERTY_UT_CFU_NOTIFICATION_MODE, mode);
                        // should only have the one
                        break;
                    }
                }
            }
        }
    }

    /**
     * Retrieves the IccPhoneBookInterfaceManager of the GsmCdmaPhone
     */
    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager(){
        return mIccPhoneBookIntManager;
    }

    //CDMA
    public void registerForEriFileLoaded(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        mEriFileLoadedRegistrants.add(r);
    }

    //CDMA
    public void unregisterForEriFileLoaded(Handler h) {
        mEriFileLoadedRegistrants.remove(h);
    }

    //CDMA
    public void prepareEri() {
        if (mEriManager == null) {
            Rlog.e(LOG_TAG, "PrepareEri: Trying to access stale objects");
            return;
        }
        mEriManager.loadEriFile();
        if(mEriManager.isEriFileLoaded()) {
            // when the ERI file is loaded
            logd("ERI read, notify registrants");
            mEriFileLoadedRegistrants.notifyRegistrants();
        }
    }

    //CDMA
    public boolean isEriFileLoaded() {
        return mEriManager.isEriFileLoaded();
    }


    /**
     * Activate or deactivate cell broadcast SMS.
     *
     * @param activate 0 = activate, 1 = deactivate
     * @param response Callback message is empty on completion
     */
    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        loge("[GsmCdmaPhone] activateCellBroadcastSms() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Query the current configuration of cdma cell broadcast SMS.
     *
     * @param response Callback message is empty on completion
     */
    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        loge("[GsmCdmaPhone] getCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Configure cdma cell broadcast SMS.
     *
     * @param response Callback message is empty on completion
     */
    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        loge("[GsmCdmaPhone] setCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    /**
     * Returns true if OTA Service Provisioning needs to be performed.
     */
    @Override
    public boolean needsOtaServiceProvisioning() {
        if (isPhoneTypeGsm()) {
            return false;
        } else {
            return mSST.getOtasp() != ServiceStateTracker.OTASP_NOT_NEEDED;
        }
    }

    @Override
    public boolean isCspPlmnEnabled() {
        IccRecords r = mIccRecords.get();
        return (r != null) ? r.isCspPlmnEnabled() : false;
    }

    public boolean isManualNetSelAllowed() {

        int nwMode = Phone.PREFERRED_NT_MODE;
        int subId = getSubId();

        nwMode = android.provider.Settings.Global.getInt(mContext.getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId, nwMode);

        logd("isManualNetSelAllowed in mode = " + nwMode);
        /*
         *  For multimode targets in global mode manual network
         *  selection is disallowed
         */
        if (isManualSelProhibitedInGlobalMode()
                && ((nwMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA)
                        || (nwMode == Phone.NT_MODE_GLOBAL)) ){
            logd("Manual selection not supported in mode = " + nwMode);
            return false;
        } else {
            logd("Manual selection is supported in mode = " + nwMode);
        }

        /*
         *  Single mode phone with - GSM network modes/global mode
         *  LTE only for 3GPP
         *  LTE centric + 3GPP Legacy
         *  Note: the actual enabling/disabling manual selection for these
         *  cases will be controlled by csp
         */
        return true;
    }

    private boolean isManualSelProhibitedInGlobalMode() {
        boolean isProhibited = false;
        final String configString = getContext().getResources().getString(com.android.internal.
                R.string.prohibit_manual_network_selection_in_gobal_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");

            if (configArray != null &&
                    ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) ||
                        (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) &&
                            configArray[0].equalsIgnoreCase("true") &&
                            isMatchGid(configArray[1])))) {
                            isProhibited = true;
            }
        }
        logd("isManualNetSelAllowedInGlobal in current carrier is " + isProhibited);
        return isProhibited;
    }

    private void registerForIccRecordEvents() {
        Rlog.d(LOG_TAG, "registerForIccRecordEvents, phonetype: " + isPhoneTypeGsm());
        IccRecords r = mIccRecords.get();
        if (r == null) {
            return;
        }
        if (isPhoneTypeGsm()) {
            r.registerForNetworkSelectionModeAutomatic(
                    this, EVENT_SET_NETWORK_AUTOMATIC, null);
            r.registerForRecordsEvents(this, EVENT_ICC_RECORD_EVENTS, null);
            r.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
        } else {
            r.registerForRecordsLoaded(this, EVENT_RUIM_RECORDS_LOADED, null);
        }
    }

    private void unregisterForIccRecordEvents() {
        Rlog.d(LOG_TAG, "unregisterForIccRecordEvents");
        IccRecords r = mIccRecords.get();
        if (r == null) {
            return;
        }
        r.unregisterForNetworkSelectionModeAutomatic(this);
        r.unregisterForRecordsEvents(this);
        r.unregisterForRecordsLoaded(this);
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (isPhoneTypeGsm()) {
            if (mImsPhone != null) {
                mImsPhone.exitEmergencyCallbackMode();
            }
        } else {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }
            // Send a message which will invoke handleExitEmergencyCallbackMode
            mCi.exitEmergencyCallbackMode(obtainMessage(EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE));
        }
    }

    //CDMA
    private void handleEnterEmergencyCallbackMode(Message msg) {
        if (DBG) {
            Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= "
                    + mIsPhoneInEcmState);
        }
        // if phone is not in Ecm mode, and it's changed to Ecm mode
        if (mIsPhoneInEcmState == false) {
            mIsPhoneInEcmState = true;
            // notify change
            sendEmergencyCallbackModeChange();
            setSystemProperty(TelephonyProperties.PROPERTY_INECM_MODE, "true");

            // Post this runnable so we will automatically exit
            // if no one invokes exitEmergencyCallbackMode() directly.
            long delayInMillis = SystemProperties.getLong(
                    TelephonyProperties.PROPERTY_ECM_EXIT_TIMER, DEFAULT_ECM_EXIT_TIMER_VALUE);
            postDelayed(mExitEcmRunnable, delayInMillis);
            // We don't want to go to sleep while in Ecm
            mWakeLock.acquire();
        }
    }

    //CDMA
    private void handleExitEmergencyCallbackMode(Message msg) {
        AsyncResult ar = (AsyncResult)msg.obj;
        if (DBG) {
            Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode,ar.exception , mIsPhoneInEcmState "
                    + ar.exception + mIsPhoneInEcmState);
        }
        // Remove pending exit Ecm runnable, if any
        removeCallbacks(mExitEcmRunnable);

        if (mEcmExitRespRegistrant != null) {
            mEcmExitRespRegistrant.notifyRegistrant(ar);
        }
        // if exiting ecm success
        if (ar.exception == null) {
            // release wakeLock
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
            }

            if (mIsPhoneInEcmState) {
                mIsPhoneInEcmState = false;
                setSystemProperty(TelephonyProperties.PROPERTY_INECM_MODE, "false");
            }
            // send an Intent
            sendEmergencyCallbackModeChange();
            // Re-initiate data connection
            mDcTracker.setInternalDataEnabled(true);
            notifyEmergencyCallRegistrants(false);
        }
    }

    //CDMA
    public void notifyEmergencyCallRegistrants(boolean started) {
        mEmergencyCallToggledRegistrants.notifyResult(started ? 1 : 0);
    }

    //CDMA
    /**
     * Handle to cancel or restart Ecm timer in emergency call back mode
     * if action is CANCEL_ECM_TIMER, cancel Ecm timer and notify apps the timer is canceled;
     * otherwise, restart Ecm timer and notify apps the timer is restarted.
     */
    public void handleTimerInEmergencyCallbackMode(int action) {
        switch(action) {
            case CANCEL_ECM_TIMER:
                removeCallbacks(mExitEcmRunnable);
                mEcmTimerResetRegistrants.notifyResult(Boolean.TRUE);
                break;
            case RESTART_ECM_TIMER:
                long delayInMillis = SystemProperties.getLong(
                        TelephonyProperties.PROPERTY_ECM_EXIT_TIMER, DEFAULT_ECM_EXIT_TIMER_VALUE);
                postDelayed(mExitEcmRunnable, delayInMillis);
                mEcmTimerResetRegistrants.notifyResult(Boolean.FALSE);
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + action);
        }
    }

    //CDMA
    private static final String IS683A_FEATURE_CODE = "*228";
    private static final int IS683A_FEATURE_CODE_NUM_DIGITS = 4;
    private static final int IS683A_SYS_SEL_CODE_NUM_DIGITS = 2;
    private static final int IS683A_SYS_SEL_CODE_OFFSET = 4;

    private static final int IS683_CONST_800MHZ_A_BAND = 0;
    private static final int IS683_CONST_800MHZ_B_BAND = 1;
    private static final int IS683_CONST_1900MHZ_A_BLOCK = 2;
    private static final int IS683_CONST_1900MHZ_B_BLOCK = 3;
    private static final int IS683_CONST_1900MHZ_C_BLOCK = 4;
    private static final int IS683_CONST_1900MHZ_D_BLOCK = 5;
    private static final int IS683_CONST_1900MHZ_E_BLOCK = 6;
    private static final int IS683_CONST_1900MHZ_F_BLOCK = 7;
    private static final int INVALID_SYSTEM_SELECTION_CODE = -1;

    // Define the pattern/format for carrier specified OTASP number schema.
    // It separates by comma and/or whitespace.
    private static Pattern pOtaSpNumSchema = Pattern.compile("[,\\s]+");

    //CDMA
    private static boolean isIs683OtaSpDialStr(String dialStr) {
        int sysSelCodeInt;
        boolean isOtaspDialString = false;
        int dialStrLen = dialStr.length();

        if (dialStrLen == IS683A_FEATURE_CODE_NUM_DIGITS) {
            if (dialStr.equals(IS683A_FEATURE_CODE)) {
                isOtaspDialString = true;
            }
        } else {
            sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
            switch (sysSelCodeInt) {
                case IS683_CONST_800MHZ_A_BAND:
                case IS683_CONST_800MHZ_B_BAND:
                case IS683_CONST_1900MHZ_A_BLOCK:
                case IS683_CONST_1900MHZ_B_BLOCK:
                case IS683_CONST_1900MHZ_C_BLOCK:
                case IS683_CONST_1900MHZ_D_BLOCK:
                case IS683_CONST_1900MHZ_E_BLOCK:
                case IS683_CONST_1900MHZ_F_BLOCK:
                    isOtaspDialString = true;
                    break;
                default:
                    break;
            }
        }
        return isOtaspDialString;
    }

    //CDMA
    /**
     * This function extracts the system selection code from the dial string.
     */
    private static int extractSelCodeFromOtaSpNum(String dialStr) {
        int dialStrLen = dialStr.length();
        int sysSelCodeInt = INVALID_SYSTEM_SELECTION_CODE;

        if ((dialStr.regionMatches(0, IS683A_FEATURE_CODE,
                0, IS683A_FEATURE_CODE_NUM_DIGITS)) &&
                (dialStrLen >= (IS683A_FEATURE_CODE_NUM_DIGITS +
                        IS683A_SYS_SEL_CODE_NUM_DIGITS))) {
            // Since we checked the condition above, the system selection code
            // extracted from dialStr will not cause any exception
            sysSelCodeInt = Integer.parseInt (
                    dialStr.substring (IS683A_FEATURE_CODE_NUM_DIGITS,
                            IS683A_FEATURE_CODE_NUM_DIGITS + IS683A_SYS_SEL_CODE_NUM_DIGITS));
        }
        if (DBG) Rlog.d(LOG_TAG, "extractSelCodeFromOtaSpNum " + sysSelCodeInt);
        return sysSelCodeInt;
    }

    //CDMA
    /**
     * This function checks if the system selection code extracted from
     * the dial string "sysSelCodeInt' is the system selection code specified
     * in the carrier ota sp number schema "sch".
     */
    private static boolean checkOtaSpNumBasedOnSysSelCode(int sysSelCodeInt, String sch[]) {
        boolean isOtaSpNum = false;
        try {
            // Get how many number of system selection code ranges
            int selRc = Integer.parseInt(sch[1]);
            for (int i = 0; i < selRc; i++) {
                if (!TextUtils.isEmpty(sch[i+2]) && !TextUtils.isEmpty(sch[i+3])) {
                    int selMin = Integer.parseInt(sch[i+2]);
                    int selMax = Integer.parseInt(sch[i+3]);
                    // Check if the selection code extracted from the dial string falls
                    // within any of the range pairs specified in the schema.
                    if ((sysSelCodeInt >= selMin) && (sysSelCodeInt <= selMax)) {
                        isOtaSpNum = true;
                        break;
                    }
                }
            }
        } catch (NumberFormatException ex) {
            // If the carrier ota sp number schema is not correct, we still allow dial
            // and only log the error:
            Rlog.e(LOG_TAG, "checkOtaSpNumBasedOnSysSelCode, error", ex);
        }
        return isOtaSpNum;
    }

    //CDMA
    /**
     * The following function checks if a dial string is a carrier specified
     * OTASP number or not by checking against the OTASP number schema stored
     * in PROPERTY_OTASP_NUM_SCHEMA.
     *
     * Currently, there are 2 schemas for carriers to specify the OTASP number:
     * 1) Use system selection code:
     *    The schema is:
     *    SELC,the # of code pairs,min1,max1,min2,max2,...
     *    e.g "SELC,3,10,20,30,40,60,70" indicates that there are 3 pairs of
     *    selection codes, and they are {10,20}, {30,40} and {60,70} respectively.
     *
     * 2) Use feature code:
     *    The schema is:
     *    "FC,length of feature code,feature code".
     *     e.g "FC,2,*2" indicates that the length of the feature code is 2,
     *     and the code itself is "*2".
     */
    private boolean isCarrierOtaSpNum(String dialStr) {
        boolean isOtaSpNum = false;
        int sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
        if (sysSelCodeInt == INVALID_SYSTEM_SELECTION_CODE) {
            return isOtaSpNum;
        }
        // mCarrierOtaSpNumSchema is retrieved from PROPERTY_OTASP_NUM_SCHEMA:
        if (!TextUtils.isEmpty(mCarrierOtaSpNumSchema)) {
            Matcher m = pOtaSpNumSchema.matcher(mCarrierOtaSpNumSchema);
            if (DBG) {
                Rlog.d(LOG_TAG, "isCarrierOtaSpNum,schema" + mCarrierOtaSpNumSchema);
            }

            if (m.find()) {
                String sch[] = pOtaSpNumSchema.split(mCarrierOtaSpNumSchema);
                // If carrier uses system selection code mechanism
                if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("SELC")) {
                    if (sysSelCodeInt!=INVALID_SYSTEM_SELECTION_CODE) {
                        isOtaSpNum=checkOtaSpNumBasedOnSysSelCode(sysSelCodeInt,sch);
                    } else {
                        if (DBG) {
                            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,sysSelCodeInt is invalid");
                        }
                    }
                } else if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("FC")) {
                    int fcLen =  Integer.parseInt(sch[1]);
                    String fc = sch[2];
                    if (dialStr.regionMatches(0,fc,0,fcLen)) {
                        isOtaSpNum = true;
                    } else {
                        if (DBG) Rlog.d(LOG_TAG, "isCarrierOtaSpNum,not otasp number");
                    }
                } else {
                    if (DBG) {
                        Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema not supported" + sch[0]);
                    }
                }
            } else {
                if (DBG) {
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern not right" +
                            mCarrierOtaSpNumSchema);
                }
            }
        } else {
            if (DBG) Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern empty");
        }
        return isOtaSpNum;
    }

    /**
     * isOTASPNumber: checks a given number against the IS-683A OTASP dial string and carrier
     * OTASP dial string.
     *
     * @param dialStr the number to look up.
     * @return true if the number is in IS-683A OTASP dial string or carrier OTASP dial string
     */
    @Override
    public  boolean isOtaSpNumber(String dialStr) {
        if (isPhoneTypeGsm()) {
            return super.isOtaSpNumber(dialStr);
        } else {
            boolean isOtaSpNum = false;
            String dialableStr = PhoneNumberUtils.extractNetworkPortionAlt(dialStr);
            if (dialableStr != null) {
                isOtaSpNum = isIs683OtaSpDialStr(dialableStr);
                if (isOtaSpNum == false) {
                    isOtaSpNum = isCarrierOtaSpNum(dialableStr);
                }
            }
            if (DBG) Rlog.d(LOG_TAG, "isOtaSpNumber " + isOtaSpNum);
            return isOtaSpNum;
        }
    }

    @Override
    public int getCdmaEriIconIndex() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconIndex();
        } else {
            return getServiceState().getCdmaEriIconIndex();
        }
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    @Override
    public int getCdmaEriIconMode() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconMode();
        } else {
            return getServiceState().getCdmaEriIconMode();
        }
    }

    /**
     * Returns the CDMA ERI text,
     */
    @Override
    public String getCdmaEriText() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriText();
        } else {
            int roamInd = getServiceState().getCdmaRoamingIndicator();
            int defRoamInd = getServiceState().getCdmaDefaultRoamingIndicator();
            return mEriManager.getCdmaEriText(roamInd, defRoamInd);
        }
    }

    private void phoneObjectUpdater(int newVoiceRadioTech) {
        logd("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech);

        // Check for a voice over lte replacement
        if ((newVoiceRadioTech == ServiceState.RIL_RADIO_TECHNOLOGY_LTE)
                || (newVoiceRadioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)) {
            CarrierConfigManager configMgr = (CarrierConfigManager)
                    getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configMgr.getConfigForSubId(getSubId());
            if (b != null) {
                int volteReplacementRat =
                        b.getInt(CarrierConfigManager.KEY_VOLTE_REPLACEMENT_RAT_INT);
                logd("phoneObjectUpdater: volteReplacementRat=" + volteReplacementRat);
                if (volteReplacementRat != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                    newVoiceRadioTech = volteReplacementRat;
                }
            } else {
                loge("phoneObjectUpdater: didn't get volteReplacementRat from carrier config");
            }
        }

        if(mRilVersion == 6 && getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
            /*
             * On v6 RIL, when LTE_ON_CDMA is TRUE, always create CDMALTEPhone
             * irrespective of the voice radio tech reported.
             */
            if (getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Use CDMA Phone" +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                return;
            } else {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Switch to CDMALTEPhone" +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                newVoiceRadioTech = ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT;
            }
        } else {

            // If the device is shutting down, then there is no need to switch to the new phone
            // which might send unnecessary attach request to the modem.
            if (isShuttingDown()) {
                logd("Device is shutting down. No need to switch phone now.");
                return;
            }

            boolean matchCdma = ServiceState.isCdma(newVoiceRadioTech);
            boolean matchGsm = ServiceState.isGsm(newVoiceRadioTech);
            if ((matchCdma && getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) ||
                    (matchGsm && getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)) {
                // MTK-START
                if (matchCdma  &&
                        getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    // Update IccCardProxy app type and it is different for cdma 4G and 3G card.
                    mIccCardProxy.setVoiceRadioTech(newVoiceRadioTech);
                }
                // MTK-END
                // Nothing changed. Keep phone as it is.
                logd("phoneObjectUpdater: No change ignore," +
                        " newVoiceRadioTech=" + newVoiceRadioTech +
                        " mActivePhone=" + getPhoneName());
                return;
            }
            if (!matchCdma && !matchGsm) {
                loge("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech +
                        " doesn't match either CDMA or GSM - error! No phone change");
                return;
            }
        }

        if (newVoiceRadioTech == ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
            // We need some voice phone object to be active always, so never
            // delete the phone without anything to replace it with!
            logd("phoneObjectUpdater: Unknown rat ignore, "
                    + " newVoiceRadioTech=Unknown. mActivePhone=" + getPhoneName());
            return;
        }

        boolean oldPowerState = false; // old power state to off
        if (mResetModemOnRadioTechnologyChange) {
            if (mCi.getRadioState().isOn()) {
                oldPowerState = true;
                logd("phoneObjectUpdater: Setting Radio Power to Off");
                mCi.setRadioPower(false, null);
            }
        }

        switchVoiceRadioTech(newVoiceRadioTech);

        if (mResetModemOnRadioTechnologyChange && oldPowerState) { // restore power state
            logd("phoneObjectUpdater: Resetting Radio");
            mCi.setRadioPower(oldPowerState, null);
        }

        // update voice radio tech in icc card proxy
        mIccCardProxy.setVoiceRadioTech(newVoiceRadioTech);

        // Send an Intent to the PhoneApp that we had a radio technology change
        Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(PhoneConstants.PHONE_NAME_KEY, getPhoneName());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhoneId);
        ActivityManagerNative.broadcastStickyIntent(intent, null, UserHandle.USER_ALL);
    }

    private void switchVoiceRadioTech(int newVoiceRadioTech) {

        String outgoingPhoneName = getPhoneName();

        logd("Switching Voice Phone : " + outgoingPhoneName + " >>> "
                + (ServiceState.isGsm(newVoiceRadioTech) ? "GSM" : "CDMA"));

        if (ServiceState.isCdma(newVoiceRadioTech)) {
            switchPhoneType(PhoneConstants.PHONE_TYPE_CDMA_LTE);
        } else if (ServiceState.isGsm(newVoiceRadioTech)) {
            switchPhoneType(PhoneConstants.PHONE_TYPE_GSM);
        } else {
            loge("deleteAndCreatePhone: newVoiceRadioTech=" + newVoiceRadioTech +
                    " is not CDMA or GSM (error) - aborting!");
            return;
        }
    }

    @Override
    public IccSmsInterfaceManager getIccSmsInterfaceManager(){
        return mIccSmsInterfaceManager;
    }

    @Override
    public void updatePhoneObject(int voiceRadioTech) {
        logd("updatePhoneObject: radioTechnology=" + voiceRadioTech);
        sendMessage(obtainMessage(EVENT_UPDATE_PHONE_OBJECT, voiceRadioTech, 0, null));
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        mSST.setImsRegistrationState(registered);
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return mIccCardProxy.getIccRecordsLoaded();
    }

    @Override
    public IccCard getIccCard() {
        return mIccCardProxy;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCdmaPhone extends:");
        super.dump(fd, pw, args);
        pw.println(" mPrecisePhoneType=" + mPrecisePhoneType);
        pw.println(" mCT=" + mCT);
        pw.println(" mSST=" + mSST);
        pw.println(" mPendingMMIs=" + mPendingMMIs);
        pw.println(" mIccPhoneBookIntManager=" + mIccPhoneBookIntManager);
        if (VDBG) pw.println(" mImei=" + mImei);
        if (VDBG) pw.println(" mImeiSv=" + mImeiSv);
        pw.println(" mVmNumber=" + mVmNumber);
        pw.println(" mCdmaSSM=" + mCdmaSSM);
        pw.println(" mCdmaSubscriptionSource=" + mCdmaSubscriptionSource);
        pw.println(" mEriManager=" + mEriManager);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" mIsPhoneInEcmState=" + mIsPhoneInEcmState);
        if (VDBG) pw.println(" mEsn=" + mEsn);
        if (VDBG) pw.println(" mMeid=" + mMeid);
        pw.println(" mCarrierOtaSpNumSchema=" + mCarrierOtaSpNumSchema);
        if (!isPhoneTypeGsm()) {
            pw.println(" getCdmaEriIconIndex()=" + getCdmaEriIconIndex());
            pw.println(" getCdmaEriIconMode()=" + getCdmaEriIconMode());
            pw.println(" getCdmaEriText()=" + getCdmaEriText());
            pw.println(" isMinInfoReady()=" + isMinInfoReady());
        }
        pw.println(" isCspPlmnEnabled()=" + isCspPlmnEnabled());
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");

        try {
            mIccCardProxy.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        if (mUiccController == null) {
            return false;
        }

        UiccCard card = mUiccController.getUiccCard(getPhoneId());
        if (card == null) {
            return false;
        }

        boolean status = card.setOperatorBrandOverride(brand);

        // Refresh.
        if (status) {
            IccRecords iccRecords = mIccRecords.get();
            if (iccRecords != null) {
                TelephonyManager.from(mContext).setSimOperatorNameForPhone(
                        getPhoneId(), iccRecords.getServiceProviderName());
            }
            if (mSST != null) {
                mSST.pollState();
            }
        }
        return status;
    }

    /**
     * @return operator numeric.
     */
    private String getOperatorNumeric() {
        String operatorNumeric = null;
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                operatorNumeric = r.getOperatorNumeric();
            }
        } else { //isPhoneTypeCdmaLte()
            IccRecords curIccRecords = null;
            if (mCdmaSubscriptionSource == CDMA_SUBSCRIPTION_NV) {
                operatorNumeric = SystemProperties.get("ro.cdma.home.operator.numeric");
            } else if (mCdmaSubscriptionSource == CDMA_SUBSCRIPTION_RUIM_SIM) {
                curIccRecords = mSimRecords;
                if (curIccRecords != null) {
                    operatorNumeric = curIccRecords.getOperatorNumeric();
                } else {
                    curIccRecords = mIccRecords.get();
                    if (curIccRecords != null && (curIccRecords instanceof RuimRecords)) {
                        RuimRecords csim = (RuimRecords) curIccRecords;
                        operatorNumeric = csim.getRUIMOperatorNumeric();
                    }
                }
            }
            if (operatorNumeric == null) {
                loge("getOperatorNumeric: Cannot retrieve operatorNumeric:"
                        + " mCdmaSubscriptionSource = " + mCdmaSubscriptionSource +
                        " mIccRecords = " + ((curIccRecords != null) ?
                        curIccRecords.getRecordsLoaded() : null));
            }

            logd("getOperatorNumeric: mCdmaSubscriptionSource = " + mCdmaSubscriptionSource
                    + " operatorNumeric = " + operatorNumeric);

        }
        return operatorNumeric;
    }

    public void notifyEcbmTimerReset(Boolean flag) {
        mEcmTimerResetRegistrants.notifyResult(flag);
    }

    /**
     * Registration point for Ecm timer reset
     *
     * @param h handler to notify
     * @param what User-defined message code
     * @param obj placed in Message.obj
     */
    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        mEcmTimerResetRegistrants.remove(h);
    }

    /**
     * Sets the SIM voice message waiting indicator records.
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        if (isPhoneTypeGsm()) {
            IccRecords r = mIccRecords.get();
            if (r != null) {
                r.setVoiceMessageWaiting(line, countWaiting);
            } else {
                logd("SIM Records not found, MWI not updated");
            }
        } else {
            setVoiceMessageCount(countWaiting);
        }
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[GsmCdmaPhone] " + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[GsmCdmaPhone] " + s);
    }

    @Override
    public boolean isUtEnabled() {
        Phone imsPhone = mImsPhone;
        if (imsPhone != null) {
            return imsPhone.isUtEnabled();
        } else {
            logd("isUtEnabled: called for GsmCdma");
            return false;
        }
    }

    public String getDtmfToneDelayKey() {
        return isPhoneTypeGsm() ?
                CarrierConfigManager.KEY_GSM_DTMF_TONE_DELAY_INT :
                CarrierConfigManager.KEY_CDMA_DTMF_TONE_DELAY_INT;
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    /// M: CC: HangupAll for FTA 31.4.4.2 @{
    @Override
    public void hangupAll() throws CallStateException {
        mCT.hangupAll();
    }
    /// @}


    /// M: CC: Proprietary incoming call handling @{
    /**
     * Set EAIC to accept or reject modem to send MT call related notifications.
     *
     * @param accept {@code true} if accept; {@code false} if reject.
     * @internal
     */
    public void setIncomingCallIndicationResponse(boolean accept) {
        Rlog.d(LOG_TAG, "setIncomingCallIndicationResponse " + accept);
        mCT.setIncomingCallIndicationResponse(accept);
    }
    /// @}

    /// M: CC: Proprietary CRSS handling @{
    public void registerForCrssSuppServiceNotification(
            Handler h, int what, Object obj) {
        mCallRelatedSuppSvcRegistrants.addUnique(h, what, obj);
        if (mCachedCrssn != null) {
            mCallRelatedSuppSvcRegistrants.notifyRegistrants(mCachedCrssn);
            mCachedCrssn = null;
        }
    }

    public void unregisterForCrssSuppServiceNotification(Handler h) {
        mCallRelatedSuppSvcRegistrants.remove(h);
        mCachedCrssn = null;
    }
    /// @}

    /// M: IMS feature @{
    // For VoLTE enhanced conference call
    @Override
    public Connection
    dial(List<String> numbers, int videoState) throws CallStateException {
        Phone imsPhone = mImsPhone;
        boolean imsUseEnabled =
                ImsManager.isVolteEnabledByPlatform(mContext) &&
                ImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext) &&
                ImsManager.isNonTtyOrTtyOnVolteEnabled(mContext);

        if (!imsUseEnabled) {
            Rlog.w(LOG_TAG, "IMS is disabled and can not dial conference call directly.");
            return null;
        }

        if (imsPhone != null) {
            Rlog.w(LOG_TAG, "service state = " + imsPhone.getServiceState().getState());
        }

        if (imsUseEnabled && imsPhone != null
                && (imsPhone.getServiceState().getState() == ServiceState.STATE_IN_SERVICE)) {
            try {
                if (DBG) {
                    Rlog.d(LOG_TAG, "Trying IMS PS conference call");
                }
                return imsPhone.dial(numbers, videoState);
            } catch (CallStateException e) {
                if (DBG) {
                    Rlog.d(LOG_TAG, "IMS PS conference call exception " + e);
                }
                if (!Phone.CS_FALLBACK.equals(e.getMessage())) {
                    CallStateException ce = new CallStateException(e.getMessage());
                    ce.setStackTrace(e.getStackTrace());
                    throw ce;
                }
            }
        }
        return null;
    }
    /// @}


    // MTK-START, SIM framework
    /**
     * Request security context authentication for USIM/SIM/ISIM
     */
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag,
            String param1, String param2, Message result) {
        if (isPhoneTypeGsm()) {
            mCi.doGeneralSimAuthentication(sessionId, mode, tag, param1, param2, result);
        }
    }

    // MVNO API
    public String getMvnoMatchType() {
        String type = PhoneConstants.MVNO_TYPE_NONE;
        if (isPhoneTypeGsm()) {
            if (mIccRecords.get() != null) {
                type = mIccRecords.get().getMvnoMatchType();
            }
            logd("getMvnoMatchType: Type = " + type);
        }
        return type;
    }

    public String getMvnoPattern(String type) {
        String pattern = "";
        if (isPhoneTypeGsm()) {
            if (mIccRecords.get() != null) {
                if (type.equals(PhoneConstants.MVNO_TYPE_SPN)) {
                    pattern = mIccRecords.get().getSpNameInEfSpn();
                } else if (type.equals(PhoneConstants.MVNO_TYPE_IMSI)) {
                    pattern = mIccRecords.get().isOperatorMvnoForImsi();
                } else if (type.equals(PhoneConstants.MVNO_TYPE_PNN)) {
                    pattern = mIccRecords.get().isOperatorMvnoForEfPnn();
                } else if (type.equals(PhoneConstants.MVNO_TYPE_GID)) {
                    pattern = mIccRecords.get().getGid1();
                } else {
                    logd("getMvnoPattern: Wrong type = " + type);
                }
            }
        }
        return pattern;
    }

    public int getCdmaSubscriptionActStatus() {
        return (mCdmaSSM != null) ? mCdmaSSM.getActStatus() : 0;
    }
    // MTK-END, SIM framework

    /// M: SS @{
    /**
     * Check whether GSM support UT interface for the
     * supplementary service configuration or not.
     *
     * @return true if support UT interface in GSMPhone
     */
    public boolean isGsmUtSupport() {
        if (SystemProperties.get("persist.mtk_ims_support").equals("1")
                && SystemProperties.get("persist.mtk_volte_support").equals("1")
                && OperatorUtils.isGsmUtSupport(getOperatorNumeric()) && isUsimCard()) {
            boolean isWfcEnable = (mImsPhone != null) && (mImsPhone.isWifiCallingEnabled());
            boolean isWfcUtSupport = isWFCUtSupport();
            if (DBG) logd("in isGsmUtSupport isWfcEnable -->"+isWfcEnable + "isWfcUtSupport-->"
                    + isWfcUtSupport);
            if (isWfcEnable && !isWfcUtSupport) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Check whether WifiCalling support UT interface for the
     * supplementary service configuration or not.
     *
     * @return true if support UT interface in GSMPhone
     */
    public boolean isWFCUtSupport() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        if (SystemProperties.get("persist.mtk_ims_support").equals("1") &&
                SystemProperties.get("persist.mtk_wfc_support").equals("1")) {
            if (isOp(OPID.OP11) || isOp(OPID.OP15)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isUsimCard() {
        if (isPhoneTypeGsm() &&  !isOp(OPID.OP09)) {
            boolean r = false;
            String iccCardType = PhoneFactory.getPhone(getPhoneId()).
                    getIccCard().getIccCardType();
            if (iccCardType != null && iccCardType.equals("USIM")) {
                r = true;
            }
            Rlog.d(LOG_TAG, "isUsimCard: " + r + ", " + iccCardType);
            return r;
        } else {
            String prop = null;
            String values[] = null;
            int subId = SubscriptionManager.getSubIdUsingPhoneId(getPhoneId());
            int slotId = SubscriptionManager.getSlotId(subId);
            if (slotId < 0 || slotId >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
                return false;
            }
            prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId], "");
            if ((!prop.equals("")) && (prop.length() > 0)) {
                values = prop.split(",");
            }
            Rlog.d(LOG_TAG, "isUsimCard PhoneId = " + getPhoneId() +
                    " cardType = " + Arrays.toString(values));
            if (values == null) {
                return false;
            }
            for (String s : values) {
                if (s.equals("USIM")) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isOpNotSupportOCB(String facility) {
        boolean r = false;
        boolean isOcb = false;
        if (facility.equals(CommandsInterface.CB_FACILITY_BAOC)
                || facility.equals(CommandsInterface.CB_FACILITY_BAOIC)
                || facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
            isOcb = true;
        }
        if (isOcb && isOp(OPID.OP01)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpNotSupportOCB: " + r + ", facility=" + facility);
        return r;
    }

    private boolean isOp(OPID id) {
        return OperatorUtils.isOperator(getOperatorNumeric(), id);
    }

    private boolean isOpTbcwWithCS(int phoneId) {
        boolean r = false;
        if (OperatorUtils.isNotSupportXcap(getOperatorNumeric())) {
            /* For those operators which do not use CS network based CW */
            if (!OperatorUtils.isNotSupportXcapButUseTBCW(getOperatorNumeric())) {
                r = true;
            }
        }
        Rlog.d(LOG_TAG, "isOpTbcwWithCS: " + r);
        return r;
    }

    /**
     * Check whether Operator support TBCLIR.
     *
     * @param phoneId input current phone id.
     * @return true if Operator support TBCLIR.
     */
    public boolean isOpTbClir() {
        boolean r = false;
        if (OperatorUtils.isTbClir(getOperatorNumeric())) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpTbClir: " + r);
        return r;
    }

    public void setServiceClass(int serviceClass) {
        Rlog.d(LOG_TAG, "setServiceClass: " + serviceClass);
        SystemProperties.set(SS_SERVICE_CLASS_PROP, String.valueOf(serviceClass));
    }

    /**
     * Check whether Operator support NwCW.
     *
     * @return true if Operator support NwCW.
     */
    public boolean isOpNwCW() {
        boolean r = false;
        if (OperatorUtils.isImsNwCW(getOperatorNumeric())) {
           r = true;
        }
        Rlog.i(LOG_TAG, "isOpNwCW(): " + r);
        return r;
    }

    /**
     * Get the enable/disable for 409 conflict response.
     * @return Operator support 409 response.
     */
    public boolean isEnableXcapHttpResponse409() {
        boolean r = false;
        if (isOp(OPID.OP05)) {
           r = true;
        }
        Rlog.i(LOG_TAG, "isEnableXcapHttpResponse409: " + r);
        return r;
    }

    public boolean isOpTransferXcap404() {
        boolean r = false;
        if (isOp(OPID.OP05)) {
           r = true;
        }
        Rlog.i(LOG_TAG, "isOpTransferXcap404: " + r);
        return r;
    }

    public boolean isOpNotSupportCallIdentity() {
        boolean r = false;
        if (isOp(OPID.OP01) || isOp(OPID.OP146) || isOp(OPID.OP178)) {
           r = true;
        }
        Rlog.i(LOG_TAG, "isOpNotSupportCallIdentity: " + r);
        return r;
    }

    public boolean isOpReregisterForCF() {
        boolean r = false;
        if (isOp(OPID.OP08)) {
           r = true;
        }
        Rlog.i(LOG_TAG, "isOpReregisterForCF: " + r);
        return r;
    }

    private boolean isIccCardMncMccAvailable(int phoneId) {
        UiccController uiccCtl = UiccController.getInstance();
        IccRecords iccRecords = uiccCtl.getIccRecords(phoneId, UiccController.APP_FAM_3GPP);
        if (iccRecords != null) {
            String mccMnc = iccRecords.getOperatorNumeric();
            Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): mccMnc is " + mccMnc);
            return (mccMnc != null);
        }
        Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): false");
        return false;
    }

    // + [ALPS02301009]
    /**
     * Check whether Operator support save the cf number to sharedpref.
     * @return true if Operator support save the cf number to sharedpref.
     */
    public boolean isSupportSaveCFNumber() {
        boolean r = false;
        if (isOp(OPID.OP07)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isSupportSaveCFNumber: " + r);
        return r;
    }

    /**
     * Clear CF number in sharedpref.
     * @param cfReason input call forwarding reason.
     */
    public void clearCFSharePreference(int cfReason) {
        String key = null;
        switch (cfReason) {
            case CF_REASON_BUSY:
                key = CFB_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NO_REPLY:
                key = CFNR_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NOT_REACHABLE:
                key = CFNRC_KEY + "_" + String.valueOf(mPhoneId);
                break;
            default:
                Rlog.e(LOG_TAG, "No need to store cfreason: " + cfReason);
                return;
        }

        Rlog.e(LOG_TAG, "Read to clear the key: " + key);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + key);
        } else {
            Rlog.e(LOG_TAG, "Commit the removal of CF preference: " + key);
        }
    }

    /**
     * Store the CF number in sharedpref.
     *
     * @param cfReason input cf reason.
     * @param setNumber is numebr.
     * @return true if save success.
     */
    public boolean applyCFSharePreference(int cfReason, String setNumber) {
        String key = null;
        switch (cfReason) {
            case CF_REASON_BUSY:
                key = CFB_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NO_REPLY:
                key = CFNR_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NOT_REACHABLE:
                key = CFNRC_KEY + "_" + String.valueOf(mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to store cfreason: " + cfReason);
                return false;
        }

        IccRecords r = mIccRecords.get();
        if (r == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return false;
        }

        String currentImsi = r.getIMSI();

        if (currentImsi == null || currentImsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return false;
        }

        if (setNumber == null || setNumber.isEmpty()) {
            Rlog.d(LOG_TAG, "setNumber is empty");
            return false;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();

        String content = currentImsi + ";" + setNumber;

        if (content == null || content.isEmpty()) {
            Rlog.e(LOG_TAG, "imsi or content are empty or null.");
            return false;
        }

        Rlog.e(LOG_TAG, "key: " + key);
        Rlog.e(LOG_TAG, "content: " + content);

        editor.putString(key, content);
        editor.apply();

        return true;
    }

    /**
     * Get previous CF number.
     *
     * @param cfReason input cf reason.
     * @return cf numebr from previous setting.
     */
    public String getCFPreviousDialNumber(int cfReason) {
        String key = null;
        switch (cfReason) {
            case CF_REASON_BUSY:
                key = CFB_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NO_REPLY:
                key = CFNR_KEY + "_" + String.valueOf(mPhoneId);
                break;
            case CF_REASON_NOT_REACHABLE:
                key = CFNRC_KEY + "_" + String.valueOf(mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to do the reason: " + cfReason);
                return null;
        }

        Rlog.d(LOG_TAG, "key: " + key);

        IccRecords r = mIccRecords.get();
        if (r == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return null;
        }

        String currentImsi = r.getIMSI();

        if (currentImsi == null || currentImsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return null;
        }

        Rlog.d(LOG_TAG, "currentImsi: " + currentImsi);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String info = sp.getString(key, null);

        if (info == null) {
            Rlog.d(LOG_TAG, "Sharedpref not with: " + key);
            return null;
        }

        String[] infoAry = info.split(";");

        if (infoAry == null || infoAry.length < 2) {
            Rlog.d(LOG_TAG, "infoAry.length < 2");
            return null;
        }

        String imsi = infoAry[0];
        String number = infoAry[1];

        if (imsi == null || imsi.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref imsi is empty.");
            return null;
        }

        if (number == null || number.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref number is empty.");
            return null;
        }

        Rlog.d(LOG_TAG, "Sharedpref imsi: " + imsi);
        Rlog.d(LOG_TAG, "Sharedpref number: " + number);

        if (currentImsi.equals(imsi)) {
            Rlog.d(LOG_TAG, "Get dial number from sharepref: " + number);
            return number;
        } else {
            SharedPreferences.Editor editor = sp.edit();
            editor.remove(key);
            if (!editor.commit()) {
                Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + key);
            }
        }

        return null;
    }

    private static final String CFB_KEY = "CFB";
    private static final String CFNR_KEY = "CFNR";
    private static final String CFNRC_KEY = "CFNRC";
    // - [ALPS02301009]

    public boolean queryCFUAgainAfterSet() {
        boolean r = false;
        if (isOp(OPID.OP05) || isOp(OPID.OP11)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "queryCFUAgainAfterSet: " + r);
        return r;
    }
    /// @}

    // M: nw START

    @Override
    public void refreshSpnDisplay(){
        mSST.refreshSpnDisplay();
    }

    @Override
    public int getNetworkHideState(){
        if (mSST.dontUpdateNetworkStateFlag == true) {
            return ServiceState.STATE_OUT_OF_SERVICE;
        } else {
            return mSST.mSS.getState();
        }
    }

    @Override
    public String getLocatedPlmn(){
        return mSST.getLocatedPlmn();
    }
    // M: nw END

    /**
     * No Support from Ut to cs domain.
     * @return true if support.
     */
    public boolean isNotSupportUtToCS() {
        boolean r = false;
        String optr = SystemProperties.get("persist.operator.optr");
        if (optr != null && optr.equals("OP09") && isOp(OPID.OP09) && isUsimCard()) {
           r = true;
        }
        Rlog.d(LOG_TAG, "isNotSupportUtToCS: " + r);
        return r;
    }

    private boolean isAllowXcapIfDataRoaming(String mccMnc) {
        // Check roaming state.
        //     -> if false, return directly.
        //     -> if true, check VOWIFI status.
        //                 -> if true, check skip wifi op list in roaming state.
        //                             -> if true, check VOWIFI state, and return directly.
        //                             -> if false, DONT CARE.
        //                 -> if false, check op list for support roaming nw.
        //                             -> if true, return false.
        //                             -> if false, return true.

        if (!getServiceState().getDataRoaming()) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: true (not roaming state)");
            return true;
        }

        if (OperatorUtils.isNoNeedCheckDataRoamingOverEPDGList(mccMnc)) {
            if (mImsPhone != null &&
                    mImsPhone.isWifiCallingEnabled() &&
                    isWFCUtSupport()) {
                Rlog.d(LOG_TAG,
                        "isAllowXcapIfDataRoaming: true (VOWIFI reg and no dont care for VOWIFI)");
                return true;
            }
        }

        if (!OperatorUtils.isNeedCheckDataRoaming(mccMnc)) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: true (ignore roaming state)");
            return true;
        }

        Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: false (roaming state, block SS)");
        return false;
    }

    private boolean isAllowXcapIfDataEnabled(String mccMnc) {
        if (!OperatorUtils.isNeedCheckDataEnabled(mccMnc)) {
            return true;
        }

        if (getDataEnabled()) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataEnabled: true");
            return true;
        }

        Rlog.d(LOG_TAG, "isAllowXcapIfDataEnabled: false");
        return false;
    }

    @Override
    public int getCsFallbackStatus() {
        if (!isAllowXcapIfDataEnabled(getOperatorNumeric())){
            return PhoneConstants.UT_CSFB_ONCE;
        }

        if (!isAllowXcapIfDataRoaming(getOperatorNumeric())){
            return PhoneConstants.UT_CSFB_ONCE;
        }

        return super.getCsFallbackStatus();
    }

    @Override
    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
        super.setVoiceCallForwardingFlag(line, enable, number);
        if (isNotSupportUtToCS() && getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            notifyCallForwardingIndicator();
        }
    }

    @Override
    public boolean getCallForwardingIndicator() {
        if (getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA && !isNotSupportUtToCS()) {
            Rlog.e(LOG_TAG, "getCallForwardingIndicator: not possible in CDMA");
            return false;
        }
        return super.getCallForwardingIndicator();
    }
}
