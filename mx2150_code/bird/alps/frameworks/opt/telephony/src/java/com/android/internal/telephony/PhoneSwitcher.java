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

import static android.telephony.SubscriptionManager.INVALID_PHONE_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.dataconnection.DcRequest;
import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.IllegalArgumentException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// M: start
import com.mediatek.internal.telephony.dataconnection.DataConnectionHelper;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
// M: end

/**
 * Utility singleton to monitor subscription changes and incoming NetworkRequests
 * and determine which phone/phones are active.
 *
 * Manages the ALLOW_DATA calls to modems and notifies phones about changes to
 * the active phones.  Note we don't wait for data attach (which may not happen anyway).
 */
public class PhoneSwitcher extends Handler {
    private final static String LOG_TAG = "PhoneSwitcher";
    private final static boolean VDBG = true;

    private int mMaxActivePhones;
    private final List<DcRequest> mPrioritizedDcRequests = new ArrayList<DcRequest>();
    private final RegistrantList[] mActivePhoneRegistrants;
    private final SubscriptionController mSubscriptionController;
    private final int[] mPhoneSubscriptions;
    private final CommandsInterface[] mCommandsInterfaces;
    private final Context mContext;
    private final PhoneState[] mPhoneStates;
    private final int mNumPhones;
    private final Phone[] mPhones;
    private final LocalLog mLocalLog;

    private int mDefaultDataSubscription;

    private final static int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 101;
    private final static int EVENT_SUBSCRIPTION_CHANGED         = 102;
    private final static int EVENT_REQUEST_NETWORK              = 103;
    private final static int EVENT_RELEASE_NETWORK              = 104;
    private final static int EVENT_EMERGENCY_TOGGLE             = 105;
    private final static int EVENT_RESEND_DATA_ALLOWED          = 106;

    private final static int MAX_LOCAL_LOG_LINES = 30;

    /** M: start */
    static final String PROPERTY_RIL_DATA_ICCID = "persist.radio.data.iccid";
    private String[] PROPERTY_ICCID_SIM = {
        "ril.iccid.sim1",
        "ril.iccid.sim2",
        "ril.iccid.sim3",
        "ril.iccid.sim4",
    };
    private static final String INVALID_ICCID = "N/A";

    @VisibleForTesting
    public PhoneSwitcher(Looper looper) {
        super(looper);
        mMaxActivePhones = 0;
        mSubscriptionController = null;
        mPhoneSubscriptions = null;
        mCommandsInterfaces = null;
        mContext = null;
        mPhoneStates = null;
        mPhones = null;
        mLocalLog = null;
        mActivePhoneRegistrants = null;
        mNumPhones = 0;
    }

    public PhoneSwitcher(int maxActivePhones, int numPhones, Context context,
            SubscriptionController subscriptionController, Looper looper, ITelephonyRegistry tr,
            CommandsInterface[] cis, Phone[] phones) {
        super(looper);
        mContext = context;
        mNumPhones = numPhones;
        mPhones = phones;
        mPhoneSubscriptions = new int[numPhones];
        mMaxActivePhones = maxActivePhones;
        mLocalLog = new LocalLog(MAX_LOCAL_LOG_LINES);

        mSubscriptionController = subscriptionController;

        mActivePhoneRegistrants = new RegistrantList[numPhones];
        mPhoneStates = new PhoneState[numPhones];
        for (int i = 0; i < numPhones; i++) {
            mActivePhoneRegistrants[i] = new RegistrantList();
            mPhoneStates[i] = new PhoneState();
            if (mPhones[i] != null) {
                mPhones[i].registerForEmergencyCallToggle(this, EVENT_EMERGENCY_TOGGLE, null);
            }
        }

        mCommandsInterfaces = cis;

        try {
            tr.addOnSubscriptionsChangedListener("PhoneSwitcher", mSubscriptionsChangedListener);
        } catch (RemoteException e) {
        }

        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));

        NetworkCapabilities netCap = new NetworkCapabilities();
        netCap.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_XCAP);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        netCap.addCapability(NetworkCapabilities.NET_CAPABILITY_BIP);
        netCap.setNetworkSpecifier(NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER);

        NetworkFactory networkFactory = new PhoneSwitcherNetworkRequestListener(looper, context,
                netCap, this);
        // we want to see all requests
        networkFactory.setScoreFilter(101);
        networkFactory.register();

        log("PhoneSwitcher started");
    }

    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_DEFAULT_SUBSCRIPTION_CHANGED);
            msg.sendToTarget();
        }
    };

    private final IOnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new IOnSubscriptionsChangedListener.Stub() {
        @Override
        public void onSubscriptionsChanged() {
            Message msg = PhoneSwitcher.this.obtainMessage(EVENT_SUBSCRIPTION_CHANGED);
            msg.sendToTarget();
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SUBSCRIPTION_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "subChanged");
                break;
            }
            case EVENT_DEFAULT_SUBSCRIPTION_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "defaultChanged");
                break;
            }
            case EVENT_REQUEST_NETWORK: {
                onRequestNetwork((NetworkRequest)msg.obj);
                break;
            }
            case EVENT_RELEASE_NETWORK: {
                onReleaseNetwork((NetworkRequest)msg.obj);
                break;
            }
            case EVENT_EMERGENCY_TOGGLE: {
                onEvaluate(REQUESTS_CHANGED, "emergencyToggle");
                break;
            }
            case EVENT_RESEND_DATA_ALLOWED: {
                onResendDataAllowed(msg);
                break;
            }
        }
    }

    private boolean isEmergency() {
        for (Phone p : mPhones) {
            if (p == null) continue;
            if (p.isInEcm() || p.isInEmergencyCall()) return true;
        }
        return false;
    }

    private static class PhoneSwitcherNetworkRequestListener extends NetworkFactory {
        private final PhoneSwitcher mPhoneSwitcher;
        public PhoneSwitcherNetworkRequestListener (Looper l, Context c,
                NetworkCapabilities nc, PhoneSwitcher ps) {
            super(l, c, "PhoneSwitcherNetworkRequstListener", nc);
            mPhoneSwitcher = ps;
        }

        @Override
        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            if (VDBG) log("needNetworkFor " + networkRequest + ", " + score);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_REQUEST_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }

        @Override
        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            if (VDBG) log("releaseNetworkFor " + networkRequest);
            Message msg = mPhoneSwitcher.obtainMessage(EVENT_RELEASE_NETWORK);
            msg.obj = networkRequest;
            msg.sendToTarget();
        }
    }

    private void onRequestNetwork(NetworkRequest networkRequest) {
        final DcRequest dcRequest = new DcRequest(networkRequest, mContext);
        if (mPrioritizedDcRequests.contains(dcRequest) == false) {
            mPrioritizedDcRequests.add(dcRequest);
            Collections.sort(mPrioritizedDcRequests);
            onEvaluate(REQUESTS_CHANGED, "netRequest");
        }
    }

    private void onReleaseNetwork(NetworkRequest networkRequest) {
        final DcRequest dcRequest = new DcRequest(networkRequest, mContext);

        if (mPrioritizedDcRequests.remove(dcRequest)) {
            onEvaluate(REQUESTS_CHANGED, "netReleased");
        }
    }

    private static final boolean REQUESTS_CHANGED   = true;
    private static final boolean REQUESTS_UNCHANGED = false;
    /**
     * Re-evaluate things.
     * Do nothing if nothing's changed.
     *
     * Otherwise, go through the requests in priority order adding their phone
     * until we've added up to the max allowed.  Then go through shutting down
     * phones that aren't in the active phone list.  Finally, activate all
     * phones in the active phone list.
     */
    private void onEvaluate(boolean requestsChanged, String reason) {
        StringBuilder sb = new StringBuilder(reason);
        if (isEmergency()) {
            log("onEvalute aborted due to Emergency");
            return;
        }

        boolean diffDetected = requestsChanged;
        final int dataSub = mSubscriptionController.getDefaultDataSubId();
        log("onEvalute amlinsan dataSub : " + dataSub + " ; mDefaultDataSubscription: " + mDefaultDataSubscription + "; reason: " + reason);

        if (dataSub != mDefaultDataSubscription) {
            sb.append(" default ").append(mDefaultDataSubscription).append("->").append(dataSub);
            mDefaultDataSubscription = dataSub;
            updateDefaultDataIccid();
            if (DataConnectionHelper.MTK_SVLTE_SUPPORT) {
                // M: update mMaxActivePhones instant to avoid status chaos during mode change.
                DataConnectionHelper.getInstance().updateMaxActivePhoneSvlte(
                            SubscriptionManager.getPhoneId(mDefaultDataSubscription));
            }
            diffDetected = true;

        }

        for (int i = 0; i < mNumPhones; i++) {
            int sub = mSubscriptionController.getSubIdUsingPhoneId(i);
            if (sub != mPhoneSubscriptions[i]) {
                sb.append(" phone[").append(i).append("] ").append(mPhoneSubscriptions[i]);
                sb.append("->").append(sub);
                mPhoneSubscriptions[i] = sub;
                diffDetected = true;
            }
        }

        if (diffDetected) {
            log("evaluating due to " + sb.toString());

            List<Integer> newActivePhones = new ArrayList<Integer>();

            for (DcRequest dcRequest : mPrioritizedDcRequests) {
                int phoneIdForRequest = phoneIdForRequest(dcRequest.networkRequest);
                if (phoneIdForRequest == INVALID_PHONE_INDEX) {
                    //VoLTE: ECC without SIM
                    if(isSkipEimsCheck(dcRequest.networkRequest) == true) {
                        phoneIdForRequest = getMainCapPhoneId();
                    } else {
                        continue;
                    }
                    //VoLTE: ECC without SIM
                }
                if (newActivePhones.contains(phoneIdForRequest)) continue;
                newActivePhones.add(phoneIdForRequest);
                if (newActivePhones.size() >= mMaxActivePhones) break;
            }

            // M: start
            // Scenario:
            //    1. Default data unset and camp on 4G at SIM1.
            //    2. IMS request and release after at SIM1.
            //    3. 4G been detached and not recover ever.
            //  The reason for that is because default data unset and after detach,
            //  there's no more active phone to attach.
            // Solution:
            //    In the case of no active phone, add one with following condition
            //    1. Get main capability (4G) phone id
            //    2. Add it into activie phone if SIM inserted.
            DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();
            if (newActivePhones.isEmpty()) {
                log("newActivePhones is empty");
                int mainCapPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
                if (dcHelper.isSimInserted(mainCapPhoneId)) {
                    log("newActivePhones mainCapPhoneId=" + mainCapPhoneId);
                    newActivePhones.add(mainCapPhoneId);
                }
            }
            // M: end

            // M: Denali SVLTE C+LG mode need keep two active phones.
            if (DataConnectionHelper.MTK_SVLTE_SUPPORT) {
                dcHelper.updateActivePhonesSvlte(newActivePhones);
            }

            if (VDBG) {
                log("default subId = " + mDefaultDataSubscription);
                for (int i = 0; i < mNumPhones; i++) {
                    log(" phone[" + i + "] using sub[" + mPhoneSubscriptions[i] + "]");
                }
                log(" newActivePhones:");
                for (Integer i : newActivePhones) log("  " + i);
            }

            for (int phoneId = 0; phoneId < mNumPhones; phoneId++) {
                if (newActivePhones.contains(phoneId) == false) {
                    deactivate(phoneId);
                }
            }

            // only activate phones up to the limit
            for (int phoneId : newActivePhones) {
                activate(phoneId);
            }
        }
    }

    private static class PhoneState {
        public volatile boolean active = false;
        public long lastRequested = 0;
    }

    private void deactivate(int phoneId) {
        PhoneState state = mPhoneStates[phoneId];
        if (state.active == false) return;
        state.active = false;
        log("deactivate " + phoneId);
        state.lastRequested = System.currentTimeMillis();
        mCommandsInterfaces[phoneId].setDataAllowed(false, null);
        mActivePhoneRegistrants[phoneId].notifyRegistrants();
    }

    private void activate(int phoneId) {
        PhoneState state = mPhoneStates[phoneId];
        if (state.active == true) return;
        state.active = true;
        log("activate " + phoneId);
        state.lastRequested = System.currentTimeMillis();
        mCommandsInterfaces[phoneId].setDataAllowed(true, null);
        mActivePhoneRegistrants[phoneId].notifyRegistrants();
    }

    // used when the modem may have been rebooted and we want to resend
    // setDataAllowed
    public void resendDataAllowed(int phoneId) {
        // M: start
        log("resendDataAllowed " + phoneId);
        // M: end
        validatePhoneId(phoneId);
        Message msg = obtainMessage(EVENT_RESEND_DATA_ALLOWED);
        msg.arg1 = phoneId;
        msg.sendToTarget();
    }

    private void onResendDataAllowed(Message msg) {
        final int phoneId = msg.arg1;
        mCommandsInterfaces[phoneId].setDataAllowed(mPhoneStates[phoneId].active, null);
    }

    private int phoneIdForRequest(NetworkRequest netRequest) {
        String specifier = netRequest.networkCapabilities.getNetworkSpecifier();
        int subId;

        if (TextUtils.isEmpty(specifier)) {
            subId = mDefaultDataSubscription;
        } else {
            subId = Integer.parseInt(specifier);
        }
        int phoneId = INVALID_PHONE_INDEX;
        if (subId == INVALID_SUBSCRIPTION_ID) return phoneId;

        for (int i = 0 ; i < mNumPhones; i++) {
            if (mPhoneSubscriptions[i] == subId) {
                phoneId = i;
                break;
            }
        }
        return phoneId;
    }

    public boolean isPhoneActive(int phoneId) {
        validatePhoneId(phoneId);
        return mPhoneStates[phoneId].active;
    }

    public void registerForActivePhoneSwitch(int phoneId, Handler h, int what, Object o) {
        validatePhoneId(phoneId);
        Registrant r = new Registrant(h, what, o);
        mActivePhoneRegistrants[phoneId].add(r);
        r.notifyRegistrant();
    }

    public void unregisterForActivePhoneSwitch(int phoneId, Handler h) {
        validatePhoneId(phoneId);
        mActivePhoneRegistrants[phoneId].remove(h);
    }

    private void validatePhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= mNumPhones) {
            throw new IllegalArgumentException("Invalid PhoneId");
        }
    }

    private void log(String l) {
        Rlog.d(LOG_TAG, l);
        mLocalLog.log(l);
    }

    private static void logv(String s) {
        if (VDBG) Rlog.v(LOG_TAG, "[DctController] " + s);
    }

    private static void logd(String s) {
        if (VDBG) Rlog.d(LOG_TAG, "[DctController] " + s);
    }

    private static void logw(String s) {
        if (VDBG) Rlog.w(LOG_TAG, "[DctController] " + s);
    }

    private static void loge(String s) {
        if (VDBG) Rlog.e(LOG_TAG, "[DctController] " + s);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("PhoneSwitcher:");
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < mNumPhones; i++) {
            PhoneState ps = mPhoneStates[i];
            c.setTimeInMillis(ps.lastRequested);
            pw.println("PhoneId(" + i + ") active=" + ps.active + ", lastRequest=" +
                    (ps.lastRequested == 0 ? "never" :
                     String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c)));
        }
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
    }

    // MTK Start
    private void updateDefaultDataIccid() {
        int dataPhoneId = SubscriptionManager.getPhoneId(mDefaultDataSubscription);
        String defaultIccid = "";
        if (dataPhoneId >= 0) {
            if (dataPhoneId >= PROPERTY_ICCID_SIM.length) {
                loge("updateDefaultDataIccid, phoneId out of boundary:" + dataPhoneId);
            } else {
                defaultIccid = SystemProperties.get(PROPERTY_ICCID_SIM[dataPhoneId]);
                logd("updateDefaultDataIccid, Iccid = " + defaultIccid + ", dataPhoneId:"
                         + dataPhoneId);
                if (defaultIccid.equals("") || defaultIccid.equals(INVALID_ICCID)) {
                    /// M: The ICCID may be cleared as TRM or world mode switch,
                    /// so, we need to get ICCID from subscriptioninfo.
                    SubscriptionInfo subInfo =
                            mSubscriptionController.getSubscriptionInfo(mDefaultDataSubscription);
                    if (subInfo != null) {
                        defaultIccid = subInfo.getIccId();
                    }
                    if (defaultIccid.equals("") || defaultIccid.equals(INVALID_ICCID)) {
                        logd("updateDefaultDataIccid, get iccid fail");
                        return;
                    }
                }
            }
        } else {
            logd("updateDefaultDataIccid, default data unset");
        }
        logd("updateDefaultDataIccid: Set " + defaultIccid);
        SystemProperties.set(PROPERTY_RIL_DATA_ICCID, defaultIccid);
    }

    // FIXME: this method should be move to dataconnection helper.
    private boolean isSimInserted(int phoneId) {
        String iccid = SystemProperties.get(PROPERTY_ICCID_SIM[phoneId], "");
        return !TextUtils.isEmpty(iccid) && !INVALID_ICCID.equals(iccid);
    }

    private boolean isSkipEimsCheck(NetworkRequest networkRequest) {
        if (networkRequest.networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            //Check if sim inserted
            for (int i = 0; i < mNumPhones; i++) {
                if (isSimInserted(i)) {
                    logd("isSkipEimsCheck: not without sim case, don't ignore.");
                    return false;
                }
            }
            return true;
        }
        logd("isSkipEimsCheck: not include EIMS capability");
        return false;
    }

    // FIXME: this function should be remove and use below API instead ASAP.
    //    The current implemention would result error if property is empty and returen -1.
    //    API : RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
    //    Owner: IMS module
    private int getMainCapPhoneId() {
        String curr4GSim = SystemProperties.get("persist.radio.simswitch", "");
        int curr4GPhoneId = -1;
        logd("current 4G Sim = " + curr4GSim);
        if (curr4GSim != null && !curr4GSim.equals("")) {
            curr4GPhoneId = Integer.parseInt(curr4GSim) - 1;
        }
        return curr4GPhoneId;
    }

    // For the data stall case needs to reattach data.
    public void reRegisterPsNetwork() {
        for (int phoneId = 0; phoneId < mPhones.length; phoneId++) {
            deactivate(phoneId);
        }
        onEvaluate(false, "doRecovery");
    }
    // MTK End

    public void setMaxActivePhones(int count) {
        mMaxActivePhones = count;
    }

    public int getMaxActivePhonesCount() {
        return mMaxActivePhones;
    }

    public void onModeChanged() {
        /// M: though requests unchanged, but we force it to ensure update.
        onEvaluate(REQUESTS_CHANGED, "modeChanged");
    }
    // MTK End
}
