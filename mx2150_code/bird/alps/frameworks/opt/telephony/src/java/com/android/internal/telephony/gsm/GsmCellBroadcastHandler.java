/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.Phone;

import java.util.HashMap;
import java.util.Iterator;

// MTK-START
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneConstants;

// ETWS features
import com.mediatek.internal.telephony.EtwsNotification;
import com.mediatek.internal.telephony.EtwsUtils;
import com.mediatek.internal.telephony.CellBroadcastFwkExt;
import android.content.IntentFilter;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.SystemClock;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;

// For Special PWS rule in specific region
import android.text.TextUtils;
import com.android.internal.telephony.TelephonyIntents;
// MTK-END
//[BIRD][BIRD_CB_SAE_FEATURE_CUSTOM][小区广播SAE功能客制化][dingjiayuan][20161125]end	
import android.os.SystemProperties;
//[BIRD][BIRD_CB_SAE_FEATURE_CUSTOM][小区广播SAE功能客制化][dingjiayuan][20161125]end	

/**
 * Handler for 3GPP format Cell Broadcasts. Parent class can also handle CDMA Cell Broadcasts.
 */
public class GsmCellBroadcastHandler extends CellBroadcastHandler {
    private static final boolean VDBG = false;  // log CB PDU data

    /** This map holds incomplete concatenated messages waiting for assembly. */
    private final HashMap<SmsCbConcatInfo, byte[][]> mSmsCbPageMap =
            new HashMap<SmsCbConcatInfo, byte[][]>(4);

    // MTK-START
    // For ETWS feature
    private PendingIntent mEtwsAlarmIntent = null;
    private static final String INTENT_ETWS_ALARM =
            "com.android.internal.telephony.etws";
    private CellBroadcastFwkExt mCellBroadcastFwkExt = null;

    // For Special PWS rule in specific region
    private static boolean[] mIsCellAreaInSpecificRegion =
            new boolean[SmsCbConstants.specificRegionList.length];

    // MTK-END

    protected GsmCellBroadcastHandler(Context context, Phone phone) {
        super("GsmCellBroadcastHandler", context, phone);
        phone.mCi.setOnNewGsmBroadcastSms(getHandler(), EVENT_NEW_SMS_MESSAGE, null);
        // MTK-START
        phone.mCi.setOnEtwsNotification(getHandler(), EVENT_NEW_ETWS_NOTIFICATION, null);

        mCellBroadcastFwkExt = new CellBroadcastFwkExt(phone);

        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ETWS_ALARM);
        context.registerReceiver(mEtwsPrimaryBroadcastReceiver, filter);

        // For Special PWS rule in specific region
        IntentFilter plmnFliter = new IntentFilter();
        plmnFliter.addAction(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);
        context.registerReceiver(mPlmnChangedBroadcastReceiver, plmnFliter);

        for (int i = 0; i < SmsCbConstants.specificRegionList.length; i++) {
            mIsCellAreaInSpecificRegion[i] = false;
        }
        // MTK-END
    }

    @Override
    protected void onQuitting() {
        mPhone.mCi.unSetOnNewGsmBroadcastSms(getHandler());
        // MTK-START
        mPhone.mCi.unSetOnEtwsNotification(getHandler());
        // MTK-END
        super.onQuitting();     // release wakelock
    }

    /**
     * Create a new CellBroadcastHandler.
     * @param context the context to use for dispatching Intents
     * @return the new handler
     */
    public static GsmCellBroadcastHandler makeGsmCellBroadcastHandler(Context context,
            Phone phone) {
        GsmCellBroadcastHandler handler = new GsmCellBroadcastHandler(context, phone);
        handler.start();
        return handler;
    }

    /**
     * Handle 3GPP-format Cell Broadcast messages sent from radio.
     *
     * @param message the message to process
     * @return true if an ordered broadcast was sent; false on failure
     */
    @Override
    protected boolean handleSmsMessage(Message message) {
        if (message.obj instanceof AsyncResult) {
            SmsCbMessage cbMessage = handleGsmBroadcastSms((AsyncResult) message.obj);
            if (cbMessage != null) {
                handleBroadcastSms(cbMessage);
                return true;
            }
        }
        return super.handleSmsMessage(message);
    }

    /**
     * Handle 3GPP format SMS-CB message.
     * @param ar the AsyncResult containing the received PDUs
     */
    private SmsCbMessage handleGsmBroadcastSms(AsyncResult ar) {
        try {
            byte[] receivedPdu = (byte[]) ar.result;

            if (VDBG) {
                int pduLength = receivedPdu.length;
                for (int i = 0; i < pduLength; i += 8) {
                    StringBuilder sb = new StringBuilder("SMS CB pdu data: ");
                    for (int j = i; j < i + 8 && j < pduLength; j++) {
                        int b = receivedPdu[j] & 0xff;
                        if (b < 0x10) {
                            sb.append('0');
                        }
                        sb.append(Integer.toHexString(b)).append(' ');
                    }
                    log(sb.toString());
                }
            }

            // MTK-START, set it is not a ETWS primary message
            SmsCbHeader header = new SmsCbHeader(receivedPdu, false);
            // MTK-END
            String plmn = TelephonyManager.from(mContext).getNetworkOperatorForPhone(
                    mPhone.getPhoneId());
            int lac = -1;
            int cid = -1;
            CellLocation cl = mPhone.getCellLocation();
            // Check if cell location is GsmCellLocation.  This is required to support
            // dual-mode devices such as CDMA/LTE devices that require support for
            // both 3GPP and 3GPP2 format messages
            if (cl instanceof GsmCellLocation) {
                GsmCellLocation cellLocation = (GsmCellLocation)cl;
                lac = cellLocation.getLac();
                cid = cellLocation.getCid();
            }

            SmsCbLocation location;
            switch (header.getGeographicalScope()) {
                case SmsCbMessage.GEOGRAPHICAL_SCOPE_LA_WIDE:
                    location = new SmsCbLocation(plmn, lac, -1);
                    break;

                case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE:
                case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE:
                    location = new SmsCbLocation(plmn, lac, cid);
                    break;

                case SmsCbMessage.GEOGRAPHICAL_SCOPE_PLMN_WIDE:
                default:
                    location = new SmsCbLocation(plmn);
                    break;
            }

            byte[][] pdus;
            int pageCount = header.getNumberOfPages();
            if (pageCount > 1) {
                // Multi-page message
                SmsCbConcatInfo concatInfo = new SmsCbConcatInfo(header, location);

                // Try to find other pages of the same message
                pdus = mSmsCbPageMap.get(concatInfo);

                if (pdus == null) {
                    // This is the first page of this message, make room for all
                    // pages and keep until complete
                    pdus = new byte[pageCount][];

                    mSmsCbPageMap.put(concatInfo, pdus);
                }

                // Page parameter is one-based
                pdus[header.getPageIndex() - 1] = receivedPdu;

                for (byte[] pdu : pdus) {
                    if (pdu == null) {
                        // Still missing pages, exit
                        return null;
                    }
                }

                // Message complete, remove and dispatch
                mSmsCbPageMap.remove(concatInfo);
            } else {
                // Single page message
                pdus = new byte[1][];
                pdus[0] = receivedPdu;
            }

            // Remove messages that are out of scope to prevent the map from
            // growing indefinitely, containing incomplete messages that were
            // never assembled
            Iterator<SmsCbConcatInfo> iter = mSmsCbPageMap.keySet().iterator();

            while (iter.hasNext()) {
                SmsCbConcatInfo info = iter.next();

                if (!info.matchesLocation(plmn, lac, cid)) {
                    iter.remove();
                }
            }

            // MTK-START, For ETWS alarm and open the necessary ETWS channels
            if (header.getServiceCategory() == SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING
                    || header.getServiceCategory() == SmsCbConstants.MESSAGE_ID_ETWS_TSUNAMI_WARNING
                    || header.getServiceCategory() ==
                    SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_AND_TSUNAMI_WARNING
                    || header.getServiceCategory() == SmsCbConstants.MESSAGE_ID_ETWS_TEST_MESSAGE
                    || header.getServiceCategory() ==
                    SmsCbConstants.MESSAGE_ID_ETWS_OTHER_EMERGENCY_TYPE) {
                stopEtwsAlarm();
                startEtwsAlarm();
            }
            // MTK-END
            return GsmSmsCbMessage.createSmsCbMessage(header, location, pdus);

        } catch (RuntimeException e) {
            loge("Error in decoding SMS CB pdu", e);
            return null;
        }
    }

    /**
     * Holds all info about a message page needed to assemble a complete concatenated message.
     */
    private static final class SmsCbConcatInfo {

        private final SmsCbHeader mHeader;
        private final SmsCbLocation mLocation;

        SmsCbConcatInfo(SmsCbHeader header, SmsCbLocation location) {
            mHeader = header;
            mLocation = location;
        }

        @Override
        public int hashCode() {
            return (mHeader.getSerialNumber() * 31) + mLocation.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SmsCbConcatInfo) {
                SmsCbConcatInfo other = (SmsCbConcatInfo)obj;

                // Two pages match if they have the same serial number (which includes the
                // geographical scope and update number), and both pages belong to the same
                // location (PLMN, plus LAC and CID if these are part of the geographical scope).
                return mHeader.getSerialNumber() == other.mHeader.getSerialNumber()
                        && mLocation.equals(other.mLocation)
                        // MTK-START
                        /**
                         * If channel, service category, is different, these two cell broadcast
                         * messages should not be treated as the same concatenated messages.
                         */
                        && mHeader.getServiceCategory() == other.mHeader.getServiceCategory();
                        // MTK-END
            }

            return false;
        }

        /**
         * Compare the location code for this message to the current location code. The match is
         * relative to the geographical scope of the message, which determines whether the LAC
         * and Cell ID are saved in mLocation or set to -1 to match all values.
         *
         * @param plmn the current PLMN
         * @param lac the current Location Area (GSM) or Service Area (UMTS)
         * @param cid the current Cell ID
         * @return true if this message is valid for the current location; false otherwise
         */
        public boolean matchesLocation(String plmn, int lac, int cid) {
            return mLocation.isInLocationArea(plmn, lac, cid);
        }
    }

    // MTK-START, start ETWS alarm and open the necessary channels
    /**
     * Implemented by subclass to handle messages in {@link IdleState}.
     * It is used to handle the ETWS primary notification. The different
     * domain should handle by itself. Default will not handle this message.
     * @param message the message to process
     * @return true to transition to {@link WaitingState}; false to stay in {@link IdleState}
     */
    @Override
    protected boolean handleEtwsPrimaryNotification(Message message) {
        if (message.obj instanceof AsyncResult)
        {
            AsyncResult ar = (AsyncResult) message.obj;
            EtwsNotification noti = (EtwsNotification) ar.result;
            log(noti.toString());

            boolean isDuplicated = mCellBroadcastFwkExt.containDuplicatedEtwsNotification(noti);
            if (!isDuplicated) {
                // save new noti into list, create
                mCellBroadcastFwkExt.openEtwsChannel(noti);
                // create SmsCbMessage from EtwsNotification, then
                // broadcast it to app
                SmsCbMessage etwsPrimary = handleEtwsPdu(noti.getEtwsPdu(), noti.plmnId);
                if (etwsPrimary != null) {
                    log("ETWS Primary dispatch to App, open necessary channels and start timer");
                    handleBroadcastSms(etwsPrimary, true);
                    stopEtwsAlarm();
                    startEtwsAlarm();
                    return true;
                }
            } else {
                // discard duplicated ETWS notification
                log("find duplicated ETWS notifiction");
                return false;
            }
        }
        return super.handleEtwsPrimaryNotification(message);
    }

    private SmsCbMessage handleEtwsPdu(byte[] pdu, String plmn) {
        if (pdu == null || pdu.length != EtwsUtils.ETWS_PDU_LENGTH) {
            log("invalid ETWS PDU");
            return null;
        }
        // Set as ETWS primary message
        SmsCbHeader header = new SmsCbHeader(pdu, true);
        GsmCellLocation cellLocation = (GsmCellLocation) mPhone.getCellLocation();
        int lac = cellLocation.getLac();
        int cid = cellLocation.getCid();

        SmsCbLocation location;
        switch (header.getGeographicalScope()) {
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_LA_WIDE:
                location = new SmsCbLocation(plmn, lac, -1);
                break;

            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE:
            case SmsCbMessage.GEOGRAPHICAL_SCOPE_CELL_WIDE_IMMEDIATE:
                location = new SmsCbLocation(plmn, lac, cid);
                break;

            case SmsCbMessage.GEOGRAPHICAL_SCOPE_PLMN_WIDE:
            default:
                location = new SmsCbLocation(plmn);
                break;
        }

        byte[][] pdus = new byte[1][];
        pdus[0] = pdu;

        return GsmSmsCbMessage.createSmsCbMessage(header, location, pdus);
    }

    protected void startEtwsAlarm() {
        int delayInMs = 30 * 60 * 1000;
        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        log("startEtwsAlarm");
        Intent intent = new Intent(INTENT_ETWS_ALARM);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        mEtwsAlarmIntent = PendingIntent.getBroadcast(mPhone.getContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayInMs, mEtwsAlarmIntent);
    }

    protected void stopEtwsAlarm() {
        AlarmManager am =
            (AlarmManager) mPhone.getContext().getSystemService(Context.ALARM_SERVICE);
        log("stopEtwsAlarm");
        if (mEtwsAlarmIntent != null) {
            am.cancel(mEtwsAlarmIntent);
            mEtwsAlarmIntent = null;
        }
    }

    private final BroadcastReceiver mEtwsPrimaryBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ETWS_ALARM)) {
                long etws_sub = intent.getLongExtra(PhoneConstants.SUBSCRIPTION_KEY,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                log("receive EVENT_ETWS_ALARM " + etws_sub);
                if (etws_sub == mPhone.getSubId()) {
                    mCellBroadcastFwkExt.closeEtwsChannel(new EtwsNotification());
                    stopEtwsAlarm();
                }
            }
        }
    };

    // For Taiwan PWS-START
    private final BroadcastReceiver mPlmnChangedBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED)) {
                String plmn = intent.getStringExtra(TelephonyIntents.EXTRA_PLMN);
                if (!TextUtils.isEmpty(plmn)) {
                    String iso = intent.getStringExtra(TelephonyIntents.EXTRA_ISO);
                    String mcc = plmn.substring(0,3);
                    log("receive Plmn Changed " + plmn + ", mcc " + mcc + ", iso " + iso);
                    if (TextUtils.isEmpty(iso)) {
                        log("empty iso! It maybe test in lab so ignore this change");
                    } else {
                        for (int i = 0; i < SmsCbConstants.specificRegionList.length; i++) {
                            mIsCellAreaInSpecificRegion[i] = false;
                            if (mcc.equals(
                                    Integer.toString(SmsCbConstants.specificRegionList[i]))) {
                                log("Update cell area to " + SmsCbConstants.specificRegionList[i]);
                                mIsCellAreaInSpecificRegion[i] = true;
                            }
                        }
                    }
                }
            }
        }
    };

    public static int isCellAreaInSpecificRegion() {
        for (int i = 0; i < SmsCbConstants.specificRegionList.length; i++) {
            if (mIsCellAreaInSpecificRegion[i]) {
                return SmsCbConstants.specificRegionList[i];
            }
        }

        return SmsCbConstants.REGION_UNDEFINED;
    }
    // For Taiwan PWS-END
    // MTK-END
    /*[BIRD][BIRD_CB_SAE_FEATURE_CUSTOM_CHANEL][客制化紧急小区广播频道][zhangaman][20170721]begin*/
    public static boolean isCustomChanel() {
        return SystemProperties.get("ro.bd_sae_pe_custom_chanel").equals("1");
    }
    /*[BIRD][BIRD_CB_SAE_FEATURE_CUSTOM_CHANEL][客制化紧急小区广播频道][zhangaman][20170721]end*/
}
