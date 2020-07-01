/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.server.wifi;

import static com.android.server.wifi.WifiController.CMD_AIRPLANE_TOGGLED;
import static com.android.server.wifi.WifiController.CMD_BATTERY_CHANGED;
import static com.android.server.wifi.WifiController.CMD_EMERGENCY_CALL_STATE_CHANGED;
import static com.android.server.wifi.WifiController.CMD_EMERGENCY_MODE_CHANGED;
import static com.android.server.wifi.WifiController.CMD_LOCKS_CHANGED;
import static com.android.server.wifi.WifiController.CMD_SCAN_ALWAYS_MODE_CHANGED;
import static com.android.server.wifi.WifiController.CMD_SCREEN_OFF;
import static com.android.server.wifi.WifiController.CMD_SCREEN_ON;
import static com.android.server.wifi.WifiController.CMD_SET_AP;
import static com.android.server.wifi.WifiController.CMD_USER_PRESENT;
import static com.android.server.wifi.WifiController.CMD_WIFI_TOGGLED;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.Network;
import android.net.NetworkScorerAppManager;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.ip.IpManager;
import android.net.wifi.IWifiManager;
import android.net.wifi.PasspointManagementObjectDefinition;
import android.net.wifi.ScanResult;
import android.net.wifi.ScanSettings;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConnectionStatistics;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiLinkLayerStats;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryStats;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import com.android.server.wifi.configparse.ConfigBuilder;

import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.net.wifi.PPPOEInfo;
import android.os.ServiceManager;
import com.mediatek.server.wifi.WifiNvRamAgent;

/// M: Permission check for CTA requirement @{
import com.mediatek.cta.CtaUtils;
/// @}

/// M: Hotspot manager implementation
import android.net.wifi.HotspotClient;
import android.net.wifi.WpsInfo;
import android.net.wifi.WpsResult;
import android.net.wifi.WpsResult.Status;

/**
 * WifiService handles remote WiFi operation requests by implementing
 * the IWifiManager interface.
 *
 * @hide
 */
public class WifiServiceImpl extends IWifiManager.Stub {
    private static final String TAG = "WifiService";
    private static final boolean DBG = true;
    private static final boolean VDBG = false;
    private static final String BOOT_DEFAULT_WIFI_COUNTRY_CODE = "ro.boot.wificountrycode";

    final WifiStateMachine mWifiStateMachine;

    private final Context mContext;
    private final FrameworkFacade mFacade;

    final LockList mLocks = new LockList();
    // some wifi lock statistics
    private int mFullHighPerfLocksAcquired;
    private int mFullHighPerfLocksReleased;
    private int mFullLocksAcquired;
    private int mFullLocksReleased;
    private int mScanLocksAcquired;
    private int mScanLocksReleased;

    private final List<Multicaster> mMulticasters =
            new ArrayList<Multicaster>();
    private int mMulticastEnabled;
    private int mMulticastDisabled;

    private final IBatteryStats mBatteryStats;
    private final PowerManager mPowerManager;
    private final AppOpsManager mAppOps;
    private final UserManager mUserManager;
    private final WifiCountryCode mCountryCode;
    // Debug counter tracking scan requests sent by WifiManager
    private int scanRequestCounter = 0;

    /* Tracks the open wi-fi network notification */
    private WifiNotificationController mNotificationController;
    /* Polls traffic stats and notifies clients */
    private WifiTrafficPoller mTrafficPoller;
    /* Tracks the persisted states for wi-fi & airplane mode */
    final WifiSettingsStore mSettingsStore;
    /* Logs connection events and some general router and scan stats */
    private final WifiMetrics mWifiMetrics;
    /* Manages affiliated certificates for current user */
    private final WifiCertManager mCertManager;

    private final WifiInjector mWifiInjector;
    /**
     * Asynchronous channel to WifiStateMachine
     */
    private AsyncChannel mWifiStateMachineChannel;

    //M: ALPS02230807 Wifi off reason
    private final static int DISABLE_WIFI_SETTING = 0;
    private final static int DISABLE_WIFI_FLIGHTMODE = 1;

    ///M: ALPS02800523 For recording sim card state
    private boolean mIsSim1Ready = false;
    private boolean mIsSim2Ready = false;

    ///M: ALPS02813779 Alarm boot, should not start WifiController twice
    private boolean mIsFirstTime = true;

    /**
     * Handles client connections
     */
    private class ClientHandler extends Handler {

        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED: {
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        if (DBG) Slog.d(TAG, "New client listening to asynchronous messages");
                        // We track the clients by the Messenger
                        // since it is expected to be always available
                        mTrafficPoller.addClient(msg.replyTo);
                    } else {
                        Slog.e(TAG, "Client connection failure, error=" + msg.arg1);
                    }
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED: {
                    if (msg.arg1 == AsyncChannel.STATUS_SEND_UNSUCCESSFUL) {
                        if (DBG) Slog.d(TAG, "Send failed, client connection lost");
                    } else {
                        if (DBG) Slog.d(TAG, "Client connection lost with reason: " + msg.arg1);
                    }
                    mTrafficPoller.removeClient(msg.replyTo);
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_FULL_CONNECTION: {
                    AsyncChannel ac = new AsyncChannel();
                    ac.connect(mContext, this, msg.replyTo);
                    break;
                }
                /* Client commands are forwarded to state machine */
                case WifiManager.CONNECT_NETWORK:
                case WifiManager.SAVE_NETWORK: {
                    WifiConfiguration config = (WifiConfiguration) msg.obj;
                    int networkId = msg.arg1;
                    if (msg.what == WifiManager.SAVE_NETWORK) {
                        Slog.d("WiFiServiceImpl ", "SAVE"
                                + " nid=" + Integer.toString(networkId)
                                + " uid=" + msg.sendingUid
                                + " name="
                                + mContext.getPackageManager().getNameForUid(msg.sendingUid));
                    }
                    if (msg.what == WifiManager.CONNECT_NETWORK) {
                        Slog.d("WiFiServiceImpl ", "CONNECT "
                                + " nid=" + Integer.toString(networkId)
                                + " uid=" + msg.sendingUid
                                + " name="
                                + mContext.getPackageManager().getNameForUid(msg.sendingUid));
                    }

                    if (config != null && isValid(config)) {
                        if (DBG) Slog.d(TAG, "Connect with config" + config);
                        mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else if (config == null
                            && networkId != WifiConfiguration.INVALID_NETWORK_ID) {
                        if (DBG) Slog.d(TAG, "Connect with networkId" + networkId);
                        mWifiStateMachine.sendMessage(Message.obtain(msg));
                    } else {
                        Slog.e(TAG, "ClientHandler.handleMessage ignoring invalid msg=" + msg);
                        if (msg.what == WifiManager.CONNECT_NETWORK) {
                            replyFailed(msg, WifiManager.CONNECT_NETWORK_FAILED,
                                    WifiManager.INVALID_ARGS);
                        } else {
                            replyFailed(msg, WifiManager.SAVE_NETWORK_FAILED,
                                    WifiManager.INVALID_ARGS);
                        }
                    }
                    break;
                }
                case WifiManager.FORGET_NETWORK:
                    mWifiStateMachine.sendMessage(Message.obtain(msg));
                    break;
                case WifiManager.START_WPS:
                case WifiManager.CANCEL_WPS:
                case WifiManager.DISABLE_NETWORK:
                case WifiManager.RSSI_PKTCNT_FETCH:
                ///M: Add for PPPOE
                case WifiManager.START_PPPOE:
                case WifiManager.STOP_PPPOE:
                //M: for proprietary use, not reconnect or scan during a period time
                case WifiManager.SET_WIFI_NOT_RECONNECT_AND_SCAN:
                {
                    mWifiStateMachine.sendMessage(Message.obtain(msg));
                    break;
                }
                default: {
                    Slog.d(TAG, "ClientHandler.handleMessage ignoring msg=" + msg);
                    break;
                }
            }
        }

        private void replyFailed(Message msg, int what, int why) {
            Message reply = msg.obtain();
            reply.what = what;
            reply.arg1 = why;
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
                // There's not much we can do if reply can't be sent!
            }
        }
    }
    private ClientHandler mClientHandler;

    /**
     * Handles interaction with WifiStateMachine
     */
    private class WifiStateMachineHandler extends Handler {
        private AsyncChannel mWsmChannel;

        WifiStateMachineHandler(Looper looper) {
            super(looper);
            mWsmChannel = new AsyncChannel();
            mWsmChannel.connect(mContext, this, mWifiStateMachine.getHandler());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED: {
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mWifiStateMachineChannel = mWsmChannel;
                    } else {
                        Slog.e(TAG, "WifiStateMachine connection failure, error=" + msg.arg1);
                        mWifiStateMachineChannel = null;
                    }
                    break;
                }
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED: {
                    Slog.e(TAG, "WifiStateMachine channel lost, msg.arg1 =" + msg.arg1);
                    mWifiStateMachineChannel = null;
                    //Re-establish connection to state machine
                    mWsmChannel.connect(mContext, this, mWifiStateMachine.getHandler());
                    break;
                }
                default: {
                    Slog.d(TAG, "WifiStateMachineHandler.handleMessage ignoring msg=" + msg);
                    break;
                }
            }
        }
    }

    WifiStateMachineHandler mWifiStateMachineHandler;

    private WifiController mWifiController;

    // M: For bug fix
    private boolean mWifiIpoOff = false;
    private boolean mIsReceiverRegistered = false;

    ///M: for alarm-boot feature
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";

    public WifiServiceImpl(Context context) {
        mContext = context;
        mWifiInjector = WifiInjector.getInstance();
        mFacade = new FrameworkFacade();
        HandlerThread wifiThread = new HandlerThread("WifiService");
        wifiThread.start();
        mWifiMetrics = mWifiInjector.getWifiMetrics();
        mTrafficPoller = new WifiTrafficPoller(mContext, wifiThread.getLooper(),
                WifiNative.getWlanNativeInterface().getInterfaceName());
        mUserManager = UserManager.get(mContext);
        HandlerThread wifiStateMachineThread = new HandlerThread("WifiStateMachine");
        wifiStateMachineThread.start();
        mCountryCode = new WifiCountryCode(
                WifiNative.getWlanNativeInterface(),
                SystemProperties.get(BOOT_DEFAULT_WIFI_COUNTRY_CODE),
                mFacade.getStringSetting(mContext, Settings.Global.WIFI_COUNTRY_CODE),
                mContext.getResources().getBoolean(
                        R.bool.config_wifi_revert_country_code_on_cellular_loss));
        mWifiStateMachine = new WifiStateMachine(mContext, mFacade,
            wifiStateMachineThread.getLooper(), mUserManager, mWifiInjector,
            new BackupManagerProxy(), mCountryCode);
        mSettingsStore = new WifiSettingsStore(mContext);
        mWifiStateMachine.enableRssiPolling(true);
        mBatteryStats = BatteryStatsService.getService();
        mPowerManager = context.getSystemService(PowerManager.class);
        mAppOps = (AppOpsManager)context.getSystemService(Context.APP_OPS_SERVICE);
        mCertManager = new WifiCertManager(mContext);

        mNotificationController = new WifiNotificationController(mContext,
                wifiThread.getLooper(), mWifiStateMachine, mFacade, null);

        mClientHandler = new ClientHandler(wifiThread.getLooper());
        mWifiStateMachineHandler = new WifiStateMachineHandler(wifiThread.getLooper());
        mWifiController = new WifiController(mContext, mWifiStateMachine,
                mSettingsStore, mLocks, wifiThread.getLooper(), mFacade);

        ///M:
        initializeExtra();
    }


    /**
     * Check if Wi-Fi needs to be enabled and start
     * if needed
     *
     * This function is used only at boot time
     */
    public void checkAndStartWifi() {

        ///M: for operator plugin @{
        mWifiStateMachine.autoConnectInit();
        ///@}
        ///M: ALPS02803569 WifiController should initial before alarm boot @{
        ///M: ALPS02813779 Alarm boot, should not start WifiController twice
        Slog.d(TAG, "mIsFirstTime: " + mIsFirstTime);
        if (mIsFirstTime) {
            Slog.d(TAG, "mWifiController.start");
            mWifiController.start();
            mIsFirstTime = false;
        }

        ///M: alarm-boot feature@{
        String bootReason = SystemProperties.get("sys.boot.reason");
        boolean isAlarmBoot = (bootReason != null && bootReason.equals("1")) ? true : false;

        if (isAlarmBoot) {
            Slog.i(TAG, "isAlarmBoot = true don't start wifi");
            //register broadcast receiver
            mContext.registerReceiver(
                    new BroadcastReceiver() {
                        public void onReceive(Context context, Intent intent) {
                            Slog.i(TAG, "receive NORMAL_BOOT_ACTION for alarm boot");
                            mContext.unregisterReceiver(this);
                            checkAndStartWifi();
                        }
                    }, new IntentFilter(NORMAL_BOOT_ACTION));
            return;
        }
        ///@}

        /* Check if wi-fi needs to be enabled */
        boolean wifiEnabled = mSettingsStore.isWifiToggleEnabled();
        Slog.i(TAG, "WifiService starting up with Wi-Fi " +
                (wifiEnabled ? "enabled" : "disabled"));

        registerForScanModeChange();
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        ///M: modify for timing issue to access Settings.Global.AIRPLANE_MODE_ON
                        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                        Slog.i(TAG, "ACTION_AIRPLANE_MODE_CHANGED isAirplaneModeOn="
                            + isAirplaneModeOn);
                        if (mSettingsStore.handleAirplaneModeToggled()) {
                            ///M: ALPS02230807 for notify WifiOffListener @{
                            if (isAirplaneModeOn) {
                                if (mWifiOffListenerCount > 0) {
                                    Slog.d(TAG, "mWifiOffListenerCount: " + mWifiOffListenerCount);
                                    reportWifiOff(DISABLE_WIFI_FLIGHTMODE);
                                    Slog.d(TAG, "Let callback to call wifi off");
                                    return;
                                }else {
                                    Slog.d(TAG, "mWifiOffListenerCount < 0");
                                }
                            }
                            ///@}
                            mWifiController.sendMessage(CMD_AIRPLANE_TOGGLED);
                        }
                        if (mSettingsStore.isAirplaneModeOn()) {
                            Log.d(TAG, "resetting country code because Airplane mode is ON");
                            mCountryCode.airplaneModeEnabled();
                        }
                    }
                },
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));

        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                        ///M: ALPS02800523 extend to multiple sim @{
                        int simSlot = intent.getIntExtra(PhoneConstants.SLOT_KEY, -1);
                        Log.d(TAG, "onReceive ACTION_SIM_STATE_CHANGED iccState: " + state
                            + ", simSlot: " + simSlot);
                        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state)) {
                            if (0 == simSlot) {
                                mIsSim1Ready = false;
                            }else if (1 == simSlot) {
                                mIsSim2Ready = false;
                            }
                            Log.d(TAG, "resetting networks because SIM was removed"
                                + ", simSlot: " + simSlot);
                            mWifiStateMachine.resetSimAuthNetworks(simSlot);
                            if (mIsSim1Ready == false && mIsSim2Ready == false) {
                                Log.d(TAG, "All sim card is absent, "
                                    + "resetting country code because SIM is removed");
                                mCountryCode.simCardRemoved();
                            }
                        } else if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(state)) {
                            if (0 == simSlot) {
                                mIsSim1Ready = true;
                            } else if (1 == simSlot) {
                                mIsSim2Ready = true;
                            }
                        }
                        ///@}
                    }
                },
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED));

        // Adding optimizations of only receiving broadcasts when wifi is enabled
        // can result in race conditions when apps toggle wifi in the background
        // without active user involvement. Always receive broadcasts.
        registerForBroadcasts();
        registerForPackageOrUserRemoval();
        mInIdleMode = mPowerManager.isDeviceIdleMode();

        ///M: ALPS02803569 Move this before alarm boot
        //mWifiController.start();

        // If we are already disabled (could be due to airplane mode), avoid changing persist
        // state here
        if (wifiEnabled) setWifiEnabled(wifiEnabled);
    }

    public void handleUserSwitch(int userId) {
        mWifiStateMachine.handleUserSwitch(userId);
    }

    /**
     * see {@link android.net.wifi.WifiManager#pingSupplicant()}
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public boolean pingSupplicant() {
        enforceAccessPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncPingSupplicant(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return false;
        }
    }

    /**
     * see {@link android.net.wifi.WifiManager#startScan}
     * and {@link android.net.wifi.WifiManager#startCustomizedScan}
     *
     * @param settings If null, use default parameter, i.e. full scan.
     * @param workSource If null, all blame is given to the calling uid.
     */
    public void startScan(ScanSettings settings, WorkSource workSource) {
        enforceChangePermission();
        synchronized (this) {
            if (mInIdleMode) {
                // Need to send an immediate scan result broadcast in case the
                // caller is waiting for a result ..

                // clear calling identity to send broadcast
                long callingIdentity = Binder.clearCallingIdentity();
                try {
                    mWifiStateMachine.sendScanResultsAvailableBroadcast(/* scanSucceeded = */ false);
                } finally {
                    // restore calling identity
                    Binder.restoreCallingIdentity(callingIdentity);
                }
                mScanPending = true;
                return;
            }
        }
        if (settings != null) {
            settings = new ScanSettings(settings);
            if (!settings.isValid()) {
                Slog.e(TAG, "invalid scan setting");
                return;
            }
        }
        if (workSource != null) {
            enforceWorkSourcePermission();
            // WifiManager currently doesn't use names, so need to clear names out of the
            // supplied WorkSource to allow future WorkSource combining.
            workSource.clearNames();
        }
        if (workSource == null && Binder.getCallingUid() >= 0) {
            workSource = new WorkSource(Binder.getCallingUid());
        }
        mWifiStateMachine.startScan(Binder.getCallingUid(), scanRequestCounter++,
                settings, workSource);
    }

    public String getWpsNfcConfigurationToken(int netId) {
        enforceConnectivityInternalPermission();
        return mWifiStateMachine.syncGetWpsNfcConfigurationToken(netId);
    }

    boolean mInIdleMode;
    boolean mScanPending;

    void handleIdleModeChanged() {
        boolean doScan = false;
        synchronized (this) {
            boolean idle = mPowerManager.isDeviceIdleMode();
            if (mInIdleMode != idle) {
                mInIdleMode = idle;
                if (!idle) {
                    if (mScanPending) {
                        mScanPending = false;
                        doScan = true;
                    }
                }
            }
        }
        if (doScan) {
            // Someone requested a scan while we were idle; do a full scan now.
            startScan(null, null);
        }
    }

    private void enforceAccessPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE,
                "WifiService");
    }

    private void enforceChangePermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE,
                "WifiService");
    }

    private void enforceLocationHardwarePermission() {
        mContext.enforceCallingOrSelfPermission(Manifest.permission.LOCATION_HARDWARE,
                "LocationHardware");
    }

    private void enforceReadCredentialPermission() {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.READ_WIFI_CREDENTIAL,
                                                "WifiService");
    }

    private void enforceWorkSourcePermission() {
        mContext.enforceCallingPermission(android.Manifest.permission.UPDATE_DEVICE_STATS,
                "WifiService");

    }

    private void enforceMulticastChangePermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                "WifiService");
    }

    private void enforceConnectivityInternalPermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.CONNECTIVITY_INTERNAL,
                "ConnectivityService");
    }

    /**
     * see {@link android.net.wifi.WifiManager#setWifiEnabled(boolean)}
     * @param enable {@code true} to enable, {@code false} to disable.
     * @return {@code true} if the enable/disable operation was
     *         started or is already in the queue.
     */
    public synchronized boolean setWifiEnabled(boolean enable) {
        enforceChangePermission();
        Slog.d(TAG, "setWifiEnabled: " + enable + " pid=" + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
        //[BIRD][ALPS03119002][手机无法开机][pangmeizhou][20170116]begin
        ///M: Permission check for CTA requirement @{
        if (SystemProperties.get("ro.mtk_cta_support").equals("1") && enable) {
            CtaUtils.enforceCheckPermission(com.mediatek.Manifest.permission.CTA_ENABLE_WIFI,
                    "Enable WiFi");
        }
        ///@}
        //[BIRD][ALPS03119002][手机无法开机][pangmeizhou][20170116]end

        ///M: ALPS02230807 for notify WifiOffListener @{
        if (!enable) {
            if (mWifiOffListenerCount > 0) {
                Slog.d(TAG, "mWifiOffListenerCount: " + mWifiOffListenerCount);
                reportWifiOff(DISABLE_WIFI_SETTING);
                Slog.d(TAG, "Let callback to call wifi off");
                return true;
            }else {
                Slog.d(TAG, "mWifiOffListenerCount < 0");
            }
        }
        ///@}

        /*
        * Caller might not have WRITE_SECURE_SETTINGS,
        * only CHANGE_WIFI_STATE is enforced
        */

        long ident = Binder.clearCallingIdentity();
        try {
            if (! mSettingsStore.handleWifiToggled(enable)) {
                // Nothing to do if wifi cannot be toggled
                return true;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        ///M: put extra mWifiIpoOff
        mWifiController.obtainMessage(CMD_WIFI_TOGGLED, mWifiIpoOff ? 1 : 0,
                Binder.getCallingUid()).sendToTarget();
        return true;
    }

    ///* M: For IPO if enable == true meansnormal state, otherwise means  IPO shutdown
    public synchronized boolean setWifiEnabledForIPO(boolean enable) {
        Slog.d(TAG, "setWifiEnabledForIPO:" + enable + ", pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        if (enable) {
            mWifiIpoOff = false;
        } else {
            mWifiIpoOff = true;
            // clear setCheckSavedStateAtBoot, so next boot will process saved state
            mSettingsStore.setCheckSavedStateAtBoot(false);
        }

        //M: after this line, go to normal state, wifi state depend on wifiSettingStore

        ///M: put extra mWifiIpoOff
        mWifiController.obtainMessage(CMD_WIFI_TOGGLED, mWifiIpoOff ? 1 : 0,
                Binder.getCallingUid()).sendToTarget();

        if (enable) {
            if (!mIsReceiverRegistered) {
                registerForBroadcasts();
                mIsReceiverRegistered = true;
            }
        } else if (mIsReceiverRegistered) {
            mContext.unregisterReceiver(mReceiver);
            mIsReceiverRegistered = false;
        }

        return true;
    }


    /**
     * see {@link WifiManager#getWifiState()}
     * @return One of {@link WifiManager#WIFI_STATE_DISABLED},
     *         {@link WifiManager#WIFI_STATE_DISABLING},
     *         {@link WifiManager#WIFI_STATE_ENABLED},
     *         {@link WifiManager#WIFI_STATE_ENABLING},
     *         {@link WifiManager#WIFI_STATE_UNKNOWN}
     */
    public int getWifiEnabledState() {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetWifiState();
    }

    /**
     * see {@link android.net.wifi.WifiManager#setWifiApEnabled(WifiConfiguration, boolean)}
     * @param wifiConfig SSID, security and channel details as
     *        part of WifiConfiguration
     * @param enabled true to enable and false to disable
     */
    public void setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        Slog.d(TAG, "setWifiApEnabled:" + enabled + ", pid:" + Binder.getCallingPid()
            + ", uid:" + Binder.getCallingUid() + ", wifiConfig:" + wifiConfig);
        enforceChangePermission();
        ConnectivityManager.enforceTetherChangePermission(mContext);
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING)) {
            throw new SecurityException("DISALLOW_CONFIG_TETHERING is enabled for this user.");
        }
        // null wifiConfig is a meaningful input for CMD_SET_AP
        if (wifiConfig == null || isValid(wifiConfig)) {
            mWifiController.obtainMessage(CMD_SET_AP, enabled ? 1 : 0, 0, wifiConfig).sendToTarget();
        } else {
            Slog.e(TAG, "Invalid WifiConfiguration");
        }
    }

    /**
     * see {@link WifiManager#getWifiApState()}
     * @return One of {@link WifiManager#WIFI_AP_STATE_DISABLED},
     *         {@link WifiManager#WIFI_AP_STATE_DISABLING},
     *         {@link WifiManager#WIFI_AP_STATE_ENABLED},
     *         {@link WifiManager#WIFI_AP_STATE_ENABLING},
     *         {@link WifiManager#WIFI_AP_STATE_FAILED}
     */
    public int getWifiApEnabledState() {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetWifiApState();
    }

    /**
     * see {@link WifiManager#getWifiApConfiguration()}
     * @return soft access point configuration
     */
    public WifiConfiguration getWifiApConfiguration() {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetWifiApConfiguration();
    }

    /**
     * see {@link WifiManager#buildWifiConfig()}
     * @return a WifiConfiguration.
     */
    public WifiConfiguration buildWifiConfig(String uriString, String mimeType, byte[] data) {
        if (mimeType.equals(ConfigBuilder.WifiConfigType)) {
            try {
                return ConfigBuilder.buildConfig(uriString, data, mContext);
            }
            catch (IOException | GeneralSecurityException | SAXException e) {
                Log.e(TAG, "Failed to parse wi-fi configuration: " + e);
            }
        }
        else {
            Log.i(TAG, "Unknown wi-fi config type: " + mimeType);
        }
        return null;
    }

    /**
     * see {@link WifiManager#setWifiApConfiguration(WifiConfiguration)}
     * @param wifiConfig WifiConfiguration details for soft access point
     */
    public void setWifiApConfiguration(WifiConfiguration wifiConfig) {
        enforceChangePermission();
        if (wifiConfig == null)
            return;
        if (isValid(wifiConfig)) {
            mWifiStateMachine.setWifiApConfiguration(wifiConfig);
        } else {
            Slog.e(TAG, "Invalid WifiConfiguration");
        }
    }

    /**
     * @param enable {@code true} to enable, {@code false} to disable.
     * @return {@code true} if the enable/disable operation was
     *         started or is already in the queue.
     */
    public boolean isScanAlwaysAvailable() {
        enforceAccessPermission();
        return mSettingsStore.isScanAlwaysAvailable();
    }

    /**
     * see {@link android.net.wifi.WifiManager#disconnect()}
     */
    public void disconnect() {
        Slog.d(TAG, "disconnect, pid:" + Binder.getCallingPid()
            + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        mWifiStateMachine.disconnectCommand();
    }

    /**
     * see {@link android.net.wifi.WifiManager#reconnect()}
     */
    public void reconnect() {
        Slog.d(TAG, "reconnect, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        mWifiStateMachine.reconnectCommand();
    }

    /**
     * see {@link android.net.wifi.WifiManager#reassociate()}
     */
    public void reassociate() {
        Slog.d(TAG, "reassociate, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        enforceChangePermission();
        mWifiStateMachine.reassociateCommand();
    }

    /**
     * see {@link android.net.wifi.WifiManager#getSupportedFeatures}
     */
    public int getSupportedFeatures() {
        enforceAccessPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetSupportedFeatures(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return 0;
        }
    }

    @Override
    public void requestActivityInfo(ResultReceiver result) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BatteryStats.RESULT_RECEIVER_CONTROLLER_KEY, reportActivityInfo());
        result.send(0, bundle);
    }

    /**
     * see {@link android.net.wifi.WifiManager#getControllerActivityEnergyInfo(int)}
     */
    public WifiActivityEnergyInfo reportActivityInfo() {
        enforceAccessPermission();
        if ((getSupportedFeatures() & WifiManager.WIFI_FEATURE_LINK_LAYER_STATS) == 0) {
            return null;
        }
        WifiLinkLayerStats stats;
        WifiActivityEnergyInfo energyInfo = null;
        if (mWifiStateMachineChannel != null) {
            stats = mWifiStateMachine.syncGetLinkLayerStats(mWifiStateMachineChannel);
            if (stats != null) {
                final long rxIdleCurrent = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_wifi_idle_receive_cur_ma);
                final long rxCurrent = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_wifi_active_rx_cur_ma);
                final long txCurrent = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_wifi_tx_cur_ma);
                final double voltage = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_wifi_operating_voltage_mv)
                        / 1000.0;

                final long rxIdleTime = stats.on_time - stats.tx_time - stats.rx_time;
                final long[] txTimePerLevel;
                if (stats.tx_time_per_level != null) {
                    txTimePerLevel = new long[stats.tx_time_per_level.length];
                    for (int i = 0; i < txTimePerLevel.length; i++) {
                        txTimePerLevel[i] = stats.tx_time_per_level[i];
                        // TODO(b/27227497): Need to read the power consumed per level from config
                    }
                } else {
                    // This will happen if the HAL get link layer API returned null.
                    txTimePerLevel = new long[0];
                }
                final long energyUsed = (long)((stats.tx_time * txCurrent +
                        stats.rx_time * rxCurrent +
                        rxIdleTime * rxIdleCurrent) * voltage);
                if (VDBG || rxIdleTime < 0 || stats.on_time < 0 || stats.tx_time < 0 ||
                        stats.rx_time < 0 || energyUsed < 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(" rxIdleCur=" + rxIdleCurrent);
                    sb.append(" rxCur=" + rxCurrent);
                    sb.append(" txCur=" + txCurrent);
                    sb.append(" voltage=" + voltage);
                    sb.append(" on_time=" + stats.on_time);
                    sb.append(" tx_time=" + stats.tx_time);
                    sb.append(" tx_time_per_level=" + Arrays.toString(txTimePerLevel));
                    sb.append(" rx_time=" + stats.rx_time);
                    sb.append(" rxIdleTime=" + rxIdleTime);
                    sb.append(" energy=" + energyUsed);
                    Log.d(TAG, " reportActivityInfo: " + sb.toString());
                }

                // Convert the LinkLayerStats into EnergyActivity
                energyInfo = new WifiActivityEnergyInfo(SystemClock.elapsedRealtime(),
                        WifiActivityEnergyInfo.STACK_STATE_STATE_IDLE, stats.tx_time,
                        txTimePerLevel, stats.rx_time, rxIdleTime, energyUsed);
            }
            if (energyInfo != null && energyInfo.isValid()) {
                return energyInfo;
            } else {
                return null;
            }
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
    }

    /**
     * see {@link android.net.wifi.WifiManager#getConfiguredNetworks()}
     * @return the list of configured networks
     */
    public List<WifiConfiguration> getConfiguredNetworks() {
        enforceAccessPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetConfiguredNetworks(Binder.getCallingUid(),
                    mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
    }

    /**
     * see {@link android.net.wifi.WifiManager#getPrivilegedConfiguredNetworks()}
     * @return the list of configured networks with real preSharedKey
     */
    public List<WifiConfiguration> getPrivilegedConfiguredNetworks() {
        enforceReadCredentialPermission();
        enforceAccessPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetPrivilegedConfiguredNetwork(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
    }

    /**
     * Returns a WifiConfiguration matching this ScanResult
     * @param scanResult scanResult that represents the BSSID
     * @return {@link WifiConfiguration} that matches this BSSID or null
     */
    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetMatchingWifiConfig(scanResult, mWifiStateMachineChannel);
    }

    /**
     * see {@link android.net.wifi.WifiManager#addOrUpdateNetwork(WifiConfiguration)}
     * @return the supplicant-assigned identifier for the new or updated
     * network if the operation succeeds, or {@code -1} if it fails
     */
    public int addOrUpdateNetwork(WifiConfiguration config) {
        Slog.d(TAG, "addOrUpdateNetwork, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid() + ", config:" + config);
        enforceChangePermission();
        if (isValid(config) && isValidPasspoint(config)) {

            ///M: ALPS02521029 Only passpoint config need to change protocals @{
            if (config.isPasspoint()) {
                //M: PROTO issue, WPA HS20 Spec said "WPA2-Enterprise;
                //when an AP indicates support for Hotspot 2.0, TKIP and WEP shall not be used."
                //if we don't set PROTO, supplicant will set PROTO as default value WPA RSN.
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            }
            ///@}

            WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;

            if (config.isPasspoint() &&
                    (enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TLS ||
                            enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TTLS)) {
                if (config.updateIdentifier != null) {
                    enforceAccessPermission();
                }
                else {
                    ///M: modify for HS20 test mode@{
                    if (SystemProperties.get("persist.wifi.hs20.test.mode").equals("1")) {
                        Slog.d(TAG, "In HS20 test mode. do not need verifyCert");
                    } else {
                        try {
                            verifyCert(enterpriseConfig.getCaCertificate());
                        } catch (CertPathValidatorException cpve) {
                            Slog.e(TAG, "CA Cert " +
                                    enterpriseConfig.getCaCertificate().getSubjectX500Principal() +
                                " untrusted: " + cpve.getMessage() +
                                " certificate path: " + cpve.getCertPath());
                            return -1;
                        } catch (GeneralSecurityException | IOException e) {
                            Slog.e(TAG, "Failed to verify certificate" +
                                    enterpriseConfig.getCaCertificate().getSubjectX500Principal() +
                                    ": " + e);
                            return -1;
                        }
                    }
                }
            }

            //TODO: pass the Uid the WifiStateMachine as a message parameter
            Slog.i("addOrUpdateNetwork", " uid = " + Integer.toString(Binder.getCallingUid())
                    + " SSID " + config.SSID
                    + " nid=" + Integer.toString(config.networkId));
            if (config.networkId == WifiConfiguration.INVALID_NETWORK_ID) {
                config.creatorUid = Binder.getCallingUid();
            } else {
                config.lastUpdateUid = Binder.getCallingUid();
            }
            if (mWifiStateMachineChannel != null) {
                return mWifiStateMachine.syncAddOrUpdateNetwork(mWifiStateMachineChannel, config);
            } else {
                Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
                return -1;
            }
        } else {
            Slog.e(TAG, "bad network configuration");
            return -1;
        }
    }

    public static void verifyCert(X509Certificate caCert)
            throws GeneralSecurityException, IOException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        CertPathValidator validator =
                CertPathValidator.getInstance(CertPathValidator.getDefaultType());
        CertPath path = factory.generateCertPath(
                Arrays.asList(caCert));
        KeyStore ks = KeyStore.getInstance("AndroidCAStore");
        ks.load(null, null);
        PKIXParameters params = new PKIXParameters(ks);
        params.setRevocationEnabled(false);
        validator.validate(path, params);
    }

    /**
     * See {@link android.net.wifi.WifiManager#removeNetwork(int)}
     * @param netId the integer that identifies the network configuration
     * to the supplicant
     * @return {@code true} if the operation succeeded
     */
    public boolean removeNetwork(int netId) {
        Slog.d(TAG, "removeNetwork, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid() + ", netId:" + netId);
        enforceChangePermission();

        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncRemoveNetwork(mWifiStateMachineChannel, netId);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return false;
        }
    }

    /**
     * See {@link android.net.wifi.WifiManager#enableNetwork(int, boolean)}
     * @param netId the integer that identifies the network configuration
     * to the supplicant
     * @param disableOthers if true, disable all other networks.
     * @return {@code true} if the operation succeeded
     */
    public boolean enableNetwork(int netId, boolean disableOthers) {
        Slog.d(TAG, "enableNetwork, pid:" + Binder.getCallingPid() +
                ", uid:" + Binder.getCallingUid() + ", netId:" + netId +
                ", disableOthers:" + disableOthers);
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncEnableNetwork(mWifiStateMachineChannel, netId,
                    disableOthers);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return false;
        }
    }

    /**
     * See {@link android.net.wifi.WifiManager#disableNetwork(int)}
     * @param netId the integer that identifies the network configuration
     * to the supplicant
     * @return {@code true} if the operation succeeded
     */
    public boolean disableNetwork(int netId) {
        Slog.d(TAG, "disableNetwork, pid:" + Binder.getCallingPid() +
                ", uid:" + Binder.getCallingUid() + ", netId:" + netId);
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncDisableNetwork(mWifiStateMachineChannel, netId);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return false;
        }
    }

    /**
     * See {@link android.net.wifi.WifiManager#getConnectionInfo()}
     * @return the Wi-Fi information, contained in {@link WifiInfo}.
     */
    public WifiInfo getConnectionInfo() {
        enforceAccessPermission();
        /*
         * Make sure we have the latest information, by sending
         * a status request to the supplicant.
         */
        return mWifiStateMachine.syncRequestConnectionInfo();
    }

    /**
     * Return the results of the most recent access point scan, in the form of
     * a list of {@link ScanResult} objects.
     * @return the list of results
     */
    public List<ScanResult> getScanResults(String callingPackage) {
        enforceAccessPermission();
        int userId = UserHandle.getCallingUserId();
        int uid = Binder.getCallingUid();
        boolean canReadPeerMacAddresses = checkPeersMacAddress();
        boolean isActiveNetworkScorer =
                NetworkScorerAppManager.isCallerActiveScorer(mContext, uid);
        boolean hasInteractUsersFull = checkInteractAcrossUsersFull();
        
        /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]BEGIN */
        if (SystemProperties.get("ro.bird_zx_shb_apps").equals("1")) {
            if (!TextUtils.isEmpty(callingPackage) && ZX_APPS.contains(callingPackage)) {
                canReadPeerMacAddresses = true;
                hasInteractUsersFull = true;
            }
        }
        /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]END */
        
        long ident = Binder.clearCallingIdentity();
        try {
            if (!canReadPeerMacAddresses && !isActiveNetworkScorer
                    && !isLocationEnabled(callingPackage)) {
                return new ArrayList<ScanResult>();
            }
            if (!canReadPeerMacAddresses && !isActiveNetworkScorer
                    && !checkCallerCanAccessScanResults(callingPackage, uid)) {
                return new ArrayList<ScanResult>();
            }
            if (mAppOps.noteOp(AppOpsManager.OP_WIFI_SCAN, uid, callingPackage)
                    != AppOpsManager.MODE_ALLOWED) {
                return new ArrayList<ScanResult>();
            }
            if (!isCurrentProfile(userId) && !hasInteractUsersFull) {
                return new ArrayList<ScanResult>();
            }
            return mWifiStateMachine.syncGetScanResultsList();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /**
     * Add a Hotspot 2.0 release 2 Management Object
     * @param mo The MO in XML form
     * @return -1 for failure
     */
    public int addPasspointManagementObject(String mo) {
        return mWifiStateMachine.syncAddPasspointManagementObject(mWifiStateMachineChannel, mo);
    }

    /**
     * Modify a Hotspot 2.0 release 2 Management Object
     * @param fqdn The FQDN of the service provider
     * @param mos A List of MO definitions to be updated
     * @return the number of nodes updated, or -1 for failure
     */
    public int modifyPasspointManagementObject(
                String fqdn, List<PasspointManagementObjectDefinition> mos) {
        return mWifiStateMachine.syncModifyPasspointManagementObject(
                mWifiStateMachineChannel, fqdn, mos);
    }

    /**
     * Query for a Hotspot 2.0 release 2 OSU icon
     * @param bssid The BSSID of the AP
     * @param fileName Icon file name
     */
    public void queryPasspointIcon(long bssid, String fileName) {
        mWifiStateMachine.syncQueryPasspointIcon(mWifiStateMachineChannel, bssid, fileName);
    }

    /**
     * Match the currently associated network against the SP matching the given FQDN
     * @param fqdn FQDN of the SP
     * @return ordinal [HomeProvider, RoamingProvider, Incomplete, None, Declined]
     */
    public int matchProviderWithCurrentNetwork(String fqdn) {
        return mWifiStateMachine.matchProviderWithCurrentNetwork(mWifiStateMachineChannel, fqdn);
    }

    /**
     * Deauthenticate and set the re-authentication hold off time for the current network
     * @param holdoff hold off time in milliseconds
     * @param ess set if the hold off pertains to an ESS rather than a BSS
     */
    public void deauthenticateNetwork(long holdoff, boolean ess) {
        mWifiStateMachine.deauthenticateNetwork(mWifiStateMachineChannel, holdoff, ess);
    }

    private boolean isLocationEnabled(String callingPackage) {
        boolean legacyForegroundApp = !isMApp(mContext, callingPackage)
                && isForegroundApp(callingPackage);
        return legacyForegroundApp || Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                != Settings.Secure.LOCATION_MODE_OFF;
    }

    /**
     * Returns true if the caller holds INTERACT_ACROSS_USERS_FULL.
     */
    private boolean checkInteractAcrossUsersFull() {
        return mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.INTERACT_ACROSS_USERS_FULL)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true if the caller holds PEERS_MAC_ADDRESS.
     */
    private boolean checkPeersMacAddress() {
        return mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.PEERS_MAC_ADDRESS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true if the calling user is the current one or a profile of the
     * current user..
     */
    private boolean isCurrentProfile(int userId) {
        int currentUser = ActivityManager.getCurrentUser();
        if (userId == currentUser) {
            return true;
        }
        List<UserInfo> profiles = mUserManager.getProfiles(currentUser);
        for (UserInfo user : profiles) {
            if (userId == user.id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tell the supplicant to persist the current list of configured networks.
     * @return {@code true} if the operation succeeded
     *
     * TODO: deprecate this
     */
    public boolean saveConfiguration() {
        Slog.d(TAG, "saveConfiguration, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        boolean result = true;
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncSaveConfig(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return false;
        }
    }

    /**
     * Set the country code
     * @param countryCode ISO 3166 country code.
     * @param persist {@code true} if the setting should be remembered.
     *
     * The persist behavior exists so that wifi can fall back to the last
     * persisted country code on a restart, when the locale information is
     * not available from telephony.
     */
    public void setCountryCode(String countryCode, boolean persist) {
        Slog.i(TAG, "WifiService trying to set country code to " + countryCode +
                " with persist set to " + persist);
        enforceConnectivityInternalPermission();
        final long token = Binder.clearCallingIdentity();
        try {
            if (mCountryCode.setCountryCode(countryCode, persist) && persist) {
                // Save this country code to persistent storage
                mFacade.setStringSetting(mContext,
                        Settings.Global.WIFI_COUNTRY_CODE,
                        countryCode);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

     /**
     * Get the country code
     * @return ISO 3166 country code.
     */
    public String getCountryCode() {
        enforceConnectivityInternalPermission();
        String country = mCountryCode.getCurrentCountryCode();
        return country;
    }
    /**
     * Set the operational frequency band
     * @param band One of
     *     {@link WifiManager#WIFI_FREQUENCY_BAND_AUTO},
     *     {@link WifiManager#WIFI_FREQUENCY_BAND_5GHZ},
     *     {@link WifiManager#WIFI_FREQUENCY_BAND_2GHZ},
     * @param persist {@code true} if the setting should be remembered.
     *
     */
    public void setFrequencyBand(int band, boolean persist) {
        enforceChangePermission();
        if (!isDualBandSupported()) return;
        Slog.i(TAG, "WifiService trying to set frequency band to " + band +
                " with persist set to " + persist);
        final long token = Binder.clearCallingIdentity();
        try {
            mWifiStateMachine.setFrequencyBand(band, persist);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }


    /**
     * Get the operational frequency band
     */
    public int getFrequencyBand() {
        enforceAccessPermission();
        return mWifiStateMachine.getFrequencyBand();
    }

    public boolean isDualBandSupported() {
        //TODO: Should move towards adding a driver API that checks at runtime
        return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wifi_dual_band_support);
    }

    /**
     * Return the DHCP-assigned addresses from the last successful DHCP request,
     * if any.
     * @return the DHCP information
     * @deprecated
     */
    public DhcpInfo getDhcpInfo() {
        enforceAccessPermission();
        DhcpResults dhcpResults = mWifiStateMachine.syncGetDhcpResults();

        DhcpInfo info = new DhcpInfo();

        if (dhcpResults.ipAddress != null &&
                dhcpResults.ipAddress.getAddress() instanceof Inet4Address) {
            info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.ipAddress.getAddress());
        }

        if (dhcpResults.gateway != null) {
            info.gateway = NetworkUtils.inetAddressToInt((Inet4Address) dhcpResults.gateway);
        }

        int dnsFound = 0;
        for (InetAddress dns : dhcpResults.dnsServers) {
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                }
                if (++dnsFound > 1) break;
            }
        }
        Inet4Address serverAddress = dhcpResults.serverAddress;
        if (serverAddress != null) {
            info.serverAddress = NetworkUtils.inetAddressToInt(serverAddress);
        }
        info.leaseDuration = dhcpResults.leaseDuration;

        return info;
    }

    /**
     * see {@link android.net.wifi.WifiManager#addToBlacklist}
     *
     */
    public void addToBlacklist(String bssid) {
        enforceChangePermission();

        mWifiStateMachine.addToBlacklist(bssid);
    }

    /**
     * see {@link android.net.wifi.WifiManager#clearBlacklist}
     *
     */
    public void clearBlacklist() {
        enforceChangePermission();

        mWifiStateMachine.clearBlacklist();
    }

    /**
     * enable TDLS for the local NIC to remote NIC
     * The APPs don't know the remote MAC address to identify NIC though,
     * so we need to do additional work to find it from remote IP address
     */

    class TdlsTaskParams {
        public String remoteIpAddress;
        public boolean enable;
    }

    class TdlsTask extends AsyncTask<TdlsTaskParams, Integer, Integer> {
        @Override
        protected Integer doInBackground(TdlsTaskParams... params) {

            // Retrieve parameters for the call
            TdlsTaskParams param = params[0];
            String remoteIpAddress = param.remoteIpAddress.trim();
            boolean enable = param.enable;

            // Get MAC address of Remote IP
            String macAddress = null;

            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader("/proc/net/arp"));

                // Skip over the line bearing colum titles
                String line = reader.readLine();

                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("[ ]+");
                    if (tokens.length < 6) {
                        continue;
                    }

                    // ARP column format is
                    // Address HWType HWAddress Flags Mask IFace
                    String ip = tokens[0];
                    String mac = tokens[3];

                    if (remoteIpAddress.equals(ip)) {
                        macAddress = mac;
                        break;
                    }
                }

                if (macAddress == null) {
                    Slog.w(TAG, "Did not find remoteAddress {" + remoteIpAddress + "} in " +
                            "/proc/net/arp");
                } else {
                    enableTdlsWithMacAddress(macAddress, enable);
                }

            } catch (FileNotFoundException e) {
                Slog.e(TAG, "Could not open /proc/net/arp to lookup mac address");
            } catch (IOException e) {
                Slog.e(TAG, "Could not read /proc/net/arp to lookup mac address");
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                }
                catch (IOException e) {
                    // Do nothing
                }
            }

            return 0;
        }
    }

    public void enableTdls(String remoteAddress, boolean enable) {
        if (remoteAddress == null) {
          throw new IllegalArgumentException("remoteAddress cannot be null");
        }

        TdlsTaskParams params = new TdlsTaskParams();
        params.remoteIpAddress = remoteAddress;
        params.enable = enable;
        new TdlsTask().execute(params);
    }


    public void enableTdlsWithMacAddress(String remoteMacAddress, boolean enable) {
        if (remoteMacAddress == null) {
          throw new IllegalArgumentException("remoteMacAddress cannot be null");
        }

        mWifiStateMachine.enableTdls(remoteMacAddress, enable);
    }

    /**
     * Get a reference to handler. This is used by a client to establish
     * an AsyncChannel communication with WifiService
     */
    public Messenger getWifiServiceMessenger() {
        Slog.d(TAG, "getWifiServiceMessenger, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(mClientHandler);
    }

    /**
     * Disable an ephemeral network, i.e. network that is created thru a WiFi Scorer
     */
    public void disableEphemeralNetwork(String SSID) {
        enforceAccessPermission();
        enforceChangePermission();
        mWifiStateMachine.disableEphemeralNetwork(SSID);
    }

    /**
     * Get the IP and proxy configuration file
     */
    public String getConfigFile() {
        enforceAccessPermission();
        return mWifiStateMachine.getConfigFile();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.d(TAG, "onReceive, action:" + action);
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                mWifiController.sendMessage(CMD_SCREEN_ON);
            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                mWifiController.sendMessage(CMD_USER_PRESENT);
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mWifiController.sendMessage(CMD_SCREEN_OFF);
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int pluggedType = intent.getIntExtra("plugged", 0);
                mWifiController.sendMessage(CMD_BATTERY_CHANGED, pluggedType, 0, null);
            } else if (action.equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE,
                        BluetoothAdapter.STATE_DISCONNECTED);
                mWifiStateMachine.sendBluetoothAdapterStateChange(state);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                boolean emergencyMode = intent.getBooleanExtra("phoneinECMState", false);
                mWifiController.sendMessage(
                            CMD_EMERGENCY_MODE_CHANGED, emergencyMode ? 1 : 0, 0);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALL_STATE_CHANGED)) {
                boolean inCall = intent.getBooleanExtra(
                            PhoneConstants.PHONE_IN_EMERGENCY_CALL, false);
                mWifiController.sendMessage(CMD_EMERGENCY_CALL_STATE_CHANGED, inCall ? 1 : 0, 0);
            } else if (action.equals(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)) {
                handleIdleModeChanged();
            }
            ///M: Add for update country code @{
            else if (action.equals(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED)) {
                String plmn = (String) intent.getExtra(TelephonyIntents.EXTRA_PLMN);
                String iso = (String) intent.getExtra(TelephonyIntents.EXTRA_ISO);
                Log.d(TAG, "ACTION_LOCATED_PLMN_CHANGED: " + plmn + " iso =" + iso);
                if (iso != null && plmn != null) {
                    mCountryCode.setCountryCode(iso, false);
                }
            }
            ///@}
        }
    };

    /**
     * Observes settings changes to scan always mode.
     */
    private void registerForScanModeChange() {
        ContentObserver contentObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                mSettingsStore.handleWifiScanAlwaysAvailableToggled();
                mWifiController.sendMessage(CMD_SCAN_ALWAYS_MODE_CHANGED);
            }
        };

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE),
                false, contentObserver);
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intentFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        ///M: Add for update country code
        intentFilter.addAction(TelephonyIntents.ACTION_LOCATED_PLMN_CHANGED);

        boolean trackEmergencyCallState = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wifi_turn_off_during_emergency_call);
        if (trackEmergencyCallState) {
            intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALL_STATE_CHANGED);
        }

        mContext.registerReceiver(mReceiver, intentFilter);
    }

    private void registerForPackageOrUserRemoval() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_USER_REMOVED);
        mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Intent.ACTION_PACKAGE_REMOVED: {
                        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            return;
                        }
                        int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                        Uri uri = intent.getData();
                        if (uid == -1 || uri == null) {
                            return;
                        }
                        String pkgName = uri.getSchemeSpecificPart();
                        mWifiStateMachine.removeAppConfigs(pkgName, uid);
                        break;
                    }
                    case Intent.ACTION_USER_REMOVED: {
                        int userHandle = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, 0);
                        mWifiStateMachine.removeUserConfigs(userHandle);
                        break;
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: can't dump WifiService from from pid="
                    + Binder.getCallingPid()
                    + ", uid=" + Binder.getCallingUid());
            return;
        }
        if (args.length > 0 && WifiMetrics.PROTO_DUMP_ARG.equals(args[0])) {
            // WifiMetrics proto bytes were requested. Dump only these.
            mWifiStateMachine.updateWifiMetrics();
            mWifiMetrics.dump(fd, pw, args);
        } else if (args.length > 0 && IpManager.DUMP_ARG.equals(args[0])) {
            // IpManager dump was requested. Pass it along and take no further action.
            String[] ipManagerArgs = new String[args.length - 1];
            System.arraycopy(args, 1, ipManagerArgs, 0, ipManagerArgs.length);
            mWifiStateMachine.dumpIpManager(fd, pw, ipManagerArgs);
        } else {
            pw.println("Wi-Fi is " + mWifiStateMachine.syncGetWifiStateByName());
            pw.println("Stay-awake conditions: " +
                    Settings.Global.getInt(mContext.getContentResolver(),
                                           Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0));
            pw.println("mMulticastEnabled " + mMulticastEnabled);
            pw.println("mMulticastDisabled " + mMulticastDisabled);
            pw.println("mInIdleMode " + mInIdleMode);
            pw.println("mScanPending " + mScanPending);
            mWifiController.dump(fd, pw, args);
            mSettingsStore.dump(fd, pw, args);
            mNotificationController.dump(fd, pw, args);
            mTrafficPoller.dump(fd, pw, args);

            pw.println("Latest scan results:");
            List<ScanResult> scanResults = mWifiStateMachine.syncGetScanResultsList();
            long nowMs = System.currentTimeMillis();
            if (scanResults != null && scanResults.size() != 0) {
                pw.println("    BSSID              Frequency  RSSI    Age      SSID " +
                        "                                Flags");
                for (ScanResult r : scanResults) {
                    long ageSec = 0;
                    long ageMilli = 0;
                    if (nowMs > r.seen && r.seen > 0) {
                        ageSec = (nowMs - r.seen) / 1000;
                        ageMilli = (nowMs - r.seen) % 1000;
                    }
                    String candidate = " ";
                    if (r.isAutoJoinCandidate > 0) candidate = "+";
                    pw.printf("  %17s  %9d  %5d  %3d.%03d%s   %-32s  %s\n",
                                             r.BSSID,
                                             r.frequency,
                                             r.level,
                                             ageSec, ageMilli,
                                             candidate,
                                             r.SSID == null ? "" : r.SSID,
                                             r.capabilities);
                }
            }
            pw.println();
            pw.println("Locks acquired: " + mFullLocksAcquired + " full, " +
                    mFullHighPerfLocksAcquired + " full high perf, " +
                    mScanLocksAcquired + " scan");
            pw.println("Locks released: " + mFullLocksReleased + " full, " +
                    mFullHighPerfLocksReleased + " full high perf, " +
                    mScanLocksReleased + " scan");
            pw.println();
            pw.println("Locks held:");
            mLocks.dump(pw);

            pw.println("Multicast Locks held:");
            for (Multicaster l : mMulticasters) {
                pw.print("    ");
                pw.println(l);
            }

            pw.println();
            mWifiStateMachine.dump(fd, pw, args);
            pw.println();
        }
    }

    private class WifiLock extends DeathRecipient {
        int mMode;
        WorkSource mWorkSource;

        WifiLock(int lockMode, String tag, IBinder binder, WorkSource ws) {
            super(tag, binder);
            mMode = lockMode;
            mWorkSource = ws;
        }

        public void binderDied() {
            synchronized (mLocks) {
                releaseWifiLockLocked(mBinder);
            }
        }

        public String toString() {
            return "WifiLock{" + mTag + " type=" + mMode + " uid=" + mUid + "}";
        }
    }

    public class LockList {
        private List<WifiLock> mList;

        private LockList() {
            mList = new ArrayList<WifiLock>();
        }

        synchronized boolean hasLocks() {
            return !mList.isEmpty();
        }

        synchronized int getStrongestLockMode() {
            if (mList.isEmpty()) {
                return WifiManager.WIFI_MODE_FULL;
            }

            if (mFullHighPerfLocksAcquired > mFullHighPerfLocksReleased) {
                return WifiManager.WIFI_MODE_FULL_HIGH_PERF;
            }

            if (mFullLocksAcquired > mFullLocksReleased) {
                return WifiManager.WIFI_MODE_FULL;
            }

            return WifiManager.WIFI_MODE_SCAN_ONLY;
        }

        synchronized void updateWorkSource(WorkSource ws) {
            for (int i = 0; i < mLocks.mList.size(); i++) {
                ws.add(mLocks.mList.get(i).mWorkSource);
            }
        }

        private void addLock(WifiLock lock) {
            if (findLockByBinder(lock.mBinder) < 0) {
                mList.add(lock);
            }
        }

        private WifiLock removeLock(IBinder binder) {
            int index = findLockByBinder(binder);
            if (index >= 0) {
                WifiLock ret = mList.remove(index);
                ret.unlinkDeathRecipient();
                return ret;
            } else {
                return null;
            }
        }

        private int findLockByBinder(IBinder binder) {
            int size = mList.size();
            for (int i = size - 1; i >= 0; i--) {
                if (mList.get(i).mBinder == binder)
                    return i;
            }
            return -1;
        }

        private void dump(PrintWriter pw) {
            for (WifiLock l : mList) {
                pw.print("    ");
                pw.println(l);
            }
        }
    }

    void enforceWakeSourcePermission(int uid, int pid) {
        if (uid == android.os.Process.myUid()) {
            return;
        }
        mContext.enforcePermission(android.Manifest.permission.UPDATE_DEVICE_STATS,
                pid, uid, null);
    }

    public boolean acquireWifiLock(IBinder binder, int lockMode, String tag, WorkSource ws) {
        Slog.d(TAG, "acquireWifiLock, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.WAKE_LOCK, null);
        if (lockMode != WifiManager.WIFI_MODE_FULL &&
                lockMode != WifiManager.WIFI_MODE_SCAN_ONLY &&
                lockMode != WifiManager.WIFI_MODE_FULL_HIGH_PERF) {
            Slog.e(TAG, "Illegal argument, lockMode= " + lockMode);
            if (DBG) throw new IllegalArgumentException("lockMode=" + lockMode);
            return false;
        }
        if (ws != null && ws.size() == 0) {
            ws = null;
        }
        if (ws != null) {
            enforceWakeSourcePermission(Binder.getCallingUid(), Binder.getCallingPid());
        }
        if (ws == null) {
            ws = new WorkSource(Binder.getCallingUid());
        }
        WifiLock wifiLock = new WifiLock(lockMode, tag, binder, ws);
        synchronized (mLocks) {
            return acquireWifiLockLocked(wifiLock);
        }
    }

    private void noteAcquireWifiLock(WifiLock wifiLock) throws RemoteException {
        switch(wifiLock.mMode) {
            case WifiManager.WIFI_MODE_FULL:
            case WifiManager.WIFI_MODE_FULL_HIGH_PERF:
            case WifiManager.WIFI_MODE_SCAN_ONLY:
                mBatteryStats.noteFullWifiLockAcquiredFromSource(wifiLock.mWorkSource);
                break;
        }
    }

    private void noteReleaseWifiLock(WifiLock wifiLock) throws RemoteException {
        switch(wifiLock.mMode) {
            case WifiManager.WIFI_MODE_FULL:
            case WifiManager.WIFI_MODE_FULL_HIGH_PERF:
            case WifiManager.WIFI_MODE_SCAN_ONLY:
                mBatteryStats.noteFullWifiLockReleasedFromSource(wifiLock.mWorkSource);
                break;
        }
    }

    private boolean acquireWifiLockLocked(WifiLock wifiLock) {
        if (DBG) Slog.d(TAG, "acquireWifiLockLocked: " + wifiLock);

        mLocks.addLock(wifiLock);

        long ident = Binder.clearCallingIdentity();
        try {
            noteAcquireWifiLock(wifiLock);
            switch(wifiLock.mMode) {
            case WifiManager.WIFI_MODE_FULL:
                ++mFullLocksAcquired;
                break;
            case WifiManager.WIFI_MODE_FULL_HIGH_PERF:
                ++mFullHighPerfLocksAcquired;
                break;

            case WifiManager.WIFI_MODE_SCAN_ONLY:
                ++mScanLocksAcquired;
                break;
            }
            mWifiController.sendMessage(CMD_LOCKS_CHANGED);
            return true;
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updateWifiLockWorkSource(IBinder lock, WorkSource ws) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (ws != null && ws.size() == 0) {
            ws = null;
        }
        if (ws != null) {
            enforceWakeSourcePermission(uid, pid);
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (mLocks) {
                int index = mLocks.findLockByBinder(lock);
                if (index < 0) {
                    throw new IllegalArgumentException("Wifi lock not active");
                }
                WifiLock wl = mLocks.mList.get(index);
                noteReleaseWifiLock(wl);
                wl.mWorkSource = ws != null ? new WorkSource(ws) : new WorkSource(uid);
                noteAcquireWifiLock(wl);
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean releaseWifiLock(IBinder lock) {
        Slog.d(TAG, "releaseWifiLock, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());

        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.WAKE_LOCK, null);
        synchronized (mLocks) {
            return releaseWifiLockLocked(lock);
        }
    }

    private boolean releaseWifiLockLocked(IBinder lock) {
        boolean hadLock;

        WifiLock wifiLock = mLocks.removeLock(lock);

        if (DBG) Slog.d(TAG, "releaseWifiLockLocked: " + wifiLock);

        hadLock = (wifiLock != null);

        long ident = Binder.clearCallingIdentity();
        try {
            if (hadLock) {
                noteReleaseWifiLock(wifiLock);
                switch(wifiLock.mMode) {
                    case WifiManager.WIFI_MODE_FULL:
                        ++mFullLocksReleased;
                        break;
                    case WifiManager.WIFI_MODE_FULL_HIGH_PERF:
                        ++mFullHighPerfLocksReleased;
                        break;
                    case WifiManager.WIFI_MODE_SCAN_ONLY:
                        ++mScanLocksReleased;
                        break;
                }
                mWifiController.sendMessage(CMD_LOCKS_CHANGED);
            }
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        return hadLock;
    }

    private abstract class DeathRecipient
            implements IBinder.DeathRecipient {
        String mTag;
        int mUid;
        IBinder mBinder;

        DeathRecipient(String tag, IBinder binder) {
            super();
            mTag = tag;
            mUid = Binder.getCallingUid();
            mBinder = binder;
            try {
                mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        void unlinkDeathRecipient() {
            mBinder.unlinkToDeath(this, 0);
        }

        public int getUid() {
            return mUid;
        }
    }

    private class Multicaster extends DeathRecipient {
        Multicaster(String tag, IBinder binder) {
            super(tag, binder);
        }

        public void binderDied() {
            Slog.e(TAG, "Multicaster binderDied");
            synchronized (mMulticasters) {
                int i = mMulticasters.indexOf(this);
                if (i != -1) {
                    removeMulticasterLocked(i, mUid);
                }
            }
        }

        public String toString() {
            return "Multicaster{" + mTag + " uid=" + mUid  + "}";
        }
    }

    public void initializeMulticastFiltering() {
        enforceMulticastChangePermission();

        synchronized (mMulticasters) {
            // if anybody had requested filters be off, leave off
            if (mMulticasters.size() != 0) {
                return;
            } else {
                mWifiStateMachine.startFilteringMulticastPackets();
            }
        }
    }

    public void acquireMulticastLock(IBinder binder, String tag) {
        enforceMulticastChangePermission();

        synchronized (mMulticasters) {
            mMulticastEnabled++;
            mMulticasters.add(new Multicaster(tag, binder));
            // Note that we could call stopFilteringMulticastPackets only when
            // our new size == 1 (first call), but this function won't
            // be called often and by making the stopPacket call each
            // time we're less fragile and self-healing.
            mWifiStateMachine.stopFilteringMulticastPackets();
        }

        int uid = Binder.getCallingUid();
        final long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.noteWifiMulticastEnabled(uid);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void releaseMulticastLock() {
        Slog.d(TAG, "releaseMulticastLock, pid:" + Binder.getCallingPid()
                + ", uid:" + Binder.getCallingUid());

        enforceMulticastChangePermission();

        int uid = Binder.getCallingUid();
        synchronized (mMulticasters) {
            mMulticastDisabled++;
            int size = mMulticasters.size();
            for (int i = size - 1; i >= 0; i--) {
                Multicaster m = mMulticasters.get(i);
                if ((m != null) && (m.getUid() == uid)) {
                    removeMulticasterLocked(i, uid);
                }
            }
        }
    }

    private void removeMulticasterLocked(int i, int uid)
    {
        Multicaster removed = mMulticasters.remove(i);

        if (removed != null) {
            removed.unlinkDeathRecipient();
        }
        if (mMulticasters.size() == 0) {
            mWifiStateMachine.startFilteringMulticastPackets();
        }

        final long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.noteWifiMulticastDisabled(uid);
        } catch (RemoteException e) {
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean isMulticastEnabled() {
        enforceAccessPermission();

        synchronized (mMulticasters) {
            return (mMulticasters.size() > 0);
        }
    }

    public void enableVerboseLogging(int verbose) {
        enforceAccessPermission();
        mWifiStateMachine.enableVerboseLogging(verbose);
    }

    public int getVerboseLoggingLevel() {
        enforceAccessPermission();
        return mWifiStateMachine.getVerboseLoggingLevel();
    }

    public void enableAggressiveHandover(int enabled) {
        enforceAccessPermission();
        mWifiStateMachine.enableAggressiveHandover(enabled);
    }

    public int getAggressiveHandover() {
        enforceAccessPermission();
        return mWifiStateMachine.getAggressiveHandover();
    }

    public void setAllowScansWithTraffic(int enabled) {
        enforceAccessPermission();
        mWifiStateMachine.setAllowScansWithTraffic(enabled);
    }

    public int getAllowScansWithTraffic() {
        enforceAccessPermission();
        return mWifiStateMachine.getAllowScansWithTraffic();
    }

    public boolean setEnableAutoJoinWhenAssociated(boolean enabled) {
        enforceChangePermission();
        return mWifiStateMachine.setEnableAutoJoinWhenAssociated(enabled);
    }

    public boolean getEnableAutoJoinWhenAssociated() {
        enforceAccessPermission();
        return mWifiStateMachine.getEnableAutoJoinWhenAssociated();
    }

    /* Return the Wifi Connection statistics object */
    public WifiConnectionStatistics getConnectionStatistics() {
        enforceAccessPermission();
        enforceReadCredentialPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetConnectionStatistics(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
    }

    public void factoryReset() {
        enforceConnectivityInternalPermission();

        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_NETWORK_RESET)) {
            return;
        }

        if (!mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_TETHERING)) {
            // Turn mobile hotspot off
            setWifiApEnabled(null, false);
        }

        if (!mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)) {
            // Enable wifi
            setWifiEnabled(true);
            // Delete all Wifi SSIDs
            ///M: ALPS02279279 [Goolge Issue] Configured networks is null when wifi is enabling @{
            // Remove all networks in WifiStateMachine
            mWifiStateMachine.factoryReset(Binder.getCallingUid());
            /*
            List<WifiConfiguration> networks = getConfiguredNetworks();
            if (networks != null) {
                for (WifiConfiguration config : networks) {
                    removeNetwork(config.networkId);
                }
                saveConfiguration();
            }
            */
            ///@}
        }
    }

    /* private methods */
    static boolean logAndReturnFalse(String s) {
        Log.d(TAG, s);
        return false;
    }

    public static boolean isValid(WifiConfiguration config) {
        String validity = checkValidity(config);
        return validity == null || logAndReturnFalse(validity);
    }

    public static boolean isValidPasspoint(WifiConfiguration config) {
        String validity = checkPasspointValidity(config);
        return validity == null || logAndReturnFalse(validity);
    }

    public static String checkValidity(WifiConfiguration config) {
        if (config.allowedKeyManagement == null)
            return "allowed kmgmt";

        if (config.allowedKeyManagement.cardinality() > 1) {
            if (config.allowedKeyManagement.cardinality() != 2) {
                return "cardinality != 2";
            }
            if (!config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
                return "not WPA_EAP";
            }
            if ((!config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X))
                    && (!config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK))) {
                return "not PSK or 8021X";
            }
        }
        return null;
    }

    public static String checkPasspointValidity(WifiConfiguration config) {
        if (!TextUtils.isEmpty(config.FQDN)) {
            /* this is passpoint configuration; it must not have an SSID */
            if (!TextUtils.isEmpty(config.SSID)) {
                return "SSID not expected for Passpoint: '" + config.SSID +
                        "' FQDN " + toHexString(config.FQDN);
            }
            /* this is passpoint configuration; it must have a providerFriendlyName */
            if (TextUtils.isEmpty(config.providerFriendlyName)) {
                return "no provider friendly name";
            }
            WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
            /* this is passpoint configuration; it must have enterprise config */
            if (enterpriseConfig == null
                    || enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.NONE ) {
                return "no enterprise config";
            }
            if ((enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TLS ||
                    enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.TTLS ||
                    enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.PEAP) &&
                    enterpriseConfig.getCaCertificate() == null) {
                return "no CA certificate";
            }
        }
        return null;
    }

    public Network getCurrentNetwork() {
        enforceAccessPermission();
        return mWifiStateMachine.getCurrentNetwork();
    }

    public static String toHexString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('\'').append(s).append('\'');
        for (int n = 0; n < s.length(); n++) {
            sb.append(String.format(" %02x", s.charAt(n) & 0xffff));
        }
        return sb.toString();
    }

    /**
     * Checks that calling process has android.Manifest.permission.ACCESS_COARSE_LOCATION or
     * android.Manifest.permission.ACCESS_FINE_LOCATION and a corresponding app op is allowed
     */
    private boolean checkCallerCanAccessScanResults(String callingPackage, int uid) {
        if (ActivityManager.checkUidPermission(Manifest.permission.ACCESS_FINE_LOCATION, uid)
                == PackageManager.PERMISSION_GRANTED
                && checkAppOppAllowed(AppOpsManager.OP_FINE_LOCATION, callingPackage, uid)) {
            return true;
        }

        if (ActivityManager.checkUidPermission(Manifest.permission.ACCESS_COARSE_LOCATION, uid)
                == PackageManager.PERMISSION_GRANTED
                && checkAppOppAllowed(AppOpsManager.OP_COARSE_LOCATION, callingPackage, uid)) {
            return true;
        }
        boolean apiLevel23App = isMApp(mContext, callingPackage);
        // Pre-M apps running in the foreground should continue getting scan results
        if (!apiLevel23App && isForegroundApp(callingPackage)) {
            return true;
        }
        Log.e(TAG, "Permission denial: Need ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION "
                + "permission to get scan results");
        return false;
    }

    private boolean checkAppOppAllowed(int op, String callingPackage, int uid) {
        return mAppOps.noteOp(op, uid, callingPackage) == AppOpsManager.MODE_ALLOWED;
    }

    private static boolean isMApp(Context context, String pkgName) {
        try {
            return context.getPackageManager().getApplicationInfo(pkgName, 0)
                    .targetSdkVersion >= Build.VERSION_CODES.M;
        } catch (PackageManager.NameNotFoundException e) {
            // In case of exception, assume M app (more strict checking)
        }
        return true;
    }

    public void hideCertFromUnaffiliatedUsers(String alias) {
        mCertManager.hideCertFromUnaffiliatedUsers(alias);
    }

    public String[] listClientCertsForCurrentUser() {
        return mCertManager.listClientCertsForCurrentUser();
    }

    /**
     * Return true if the specified package name is a foreground app.
     *
     * @param pkgName application package name.
     */
    private boolean isForegroundApp(String pkgName) {
        ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        return !tasks.isEmpty() && pkgName.equals(tasks.get(0).topActivity.getPackageName());
    }

    /**
     * Enable/disable WifiConnectivityManager at runtime
     *
     * @param enabled true-enable; false-disable
     */
    public void enableWifiConnectivityManager(boolean enabled) {
        enforceConnectivityInternalPermission();
        mWifiStateMachine.enableWifiConnectivityManager(enabled);
    }

    // M: For CTIA @{
    public boolean doCtiaTestOn() {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncDoCtiaTestOn(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    public boolean doCtiaTestOff() {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncDoCtiaTestOff(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    public boolean doCtiaTestRate(int rate) {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncDoCtiaTestRate(mWifiStateMachineChannel, rate);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    public boolean setTxPowerEnabled(boolean enable) {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncSetTxPowerEnabled(mWifiStateMachineChannel, enable);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    public boolean setTxPower(int offset) {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncSetTxPower(mWifiStateMachineChannel, offset);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }
    ///@}

    ///M: Add functions @{
    public void addSimCardAuthenticationService(String name, IBinder binder) {
        enforceChangePermission();
        ServiceManager.addService(name, binder);
    }

    /// M: Hotspot manager implementation @{
    public void startApWps(WpsInfo config) {
        enforceChangePermission();
        mWifiStateMachine.startApWpsCommand(config);
    }

    public List<HotspotClient> getHotspotClients() {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetHotspotClientsList(mWifiStateMachineChannel);
    }

    private ArrayList<String> readClientList(String filename) {
        FileInputStream fstream = null;
        ArrayList<String> list = new ArrayList<String>();
        try {
            fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            // throw away the title line
            while (((s = br.readLine()) != null) && (s.length() != 0)) {
                list.add(s);
            }
        } catch (IOException ex) {
            // return current list, possibly empty
            Slog.e(TAG, "IOException:" + ex);
        } finally {
          if (fstream != null) {
                try {
                    fstream.close();
                } catch (IOException ex) {
                    Slog.e(TAG, "IOException:" + ex);
                }
            }
        }
        return list;
    }

    public String getClientIp(String deviceAddress) {
        enforceAccessPermission();
        if (TextUtils.isEmpty(deviceAddress)) {
            return null;
        }
        final String LEASES_FILE = "/data/misc/dhcp/dnsmasq.leases";

        for (String s : readClientList(LEASES_FILE)) {
            if (s.indexOf(deviceAddress) != -1) {
                String[] fields = s.split(" ");
                if (fields.length > 3) {
                    return fields[2];
                }
            }
        }
        return null;
    }

    public String getClientDeviceName(String deviceAddress) {
        enforceAccessPermission();
        if (TextUtils.isEmpty(deviceAddress)) {
            return null;
        }
        final String LEASES_FILE = "/data/misc/dhcp/dnsmasq.leases";

        for (String s : readClientList(LEASES_FILE)) {
            if (s.indexOf(deviceAddress) != -1) {
                String[] fields = s.split(" ");
                if (fields.length > 4) {
                    return fields[3];
                }
            }
        }
        return null;
    }

    public boolean blockClient(HotspotClient client) {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncBlockClient(mWifiStateMachineChannel, client);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    public boolean unblockClient(HotspotClient client) {
        enforceChangePermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncUnblockClient(mWifiStateMachineChannel, client);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }
    /// @}

    public void suspendNotification(int type) {
        enforceChangePermission();
        mWifiStateMachine.suspendNotification(type);
    }

    public boolean hasConnectableAp() {
        enforceAccessPermission();
        boolean value = mSettingsStore.hasConnectableAp();
        if (!value) { return false; }
        boolean result = mWifiStateMachine.hasConnectableAp();
        if (result) {
            //mNotificationController.setWaitForScanResult(true);
        }
        return result;
    }

    private void initializeExtra() {
        ///M: ALPS02230807 clear listener count
        mWifiOffListenerCount = 0;
    }

    public String getWifiStatus() {
        enforceAccessPermission();
        String result = "";
        if (mWifiStateMachineChannel != null) {
            result = mWifiStateMachine.getWifiStatus(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
        }
        return result;
    }

    public void setPowerSavingMode(boolean mode) {
        enforceAccessPermission();
        mWifiStateMachine.setPowerSavingMode(mode);
    }

    public int syncGetConnectingNetworkId() {
        /*
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetConnectingNetworkId(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return -1;
        }
        */
        return -1;
  }

    public PPPOEInfo getPPPOEInfo() {
        enforceAccessPermission();
        return mWifiStateMachine.syncGetPppoeInfo();
    }
    ///M: Add API For Set WOWLAN Mode
    public boolean setWoWlanNormalMode() {
        enforceChangePermission();
        Slog.d(TAG, "setWoWlanNormalMode");
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncSetWoWlanNormalMode(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }
    ///M: Add API For Set WOWLAN Mode
    public boolean setWoWlanMagicMode() {
        enforceChangePermission();
        Slog.d(TAG, "setWoWlanMagicMode");
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncSetWoWlanMagicMode(mWifiStateMachineChannel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized!");
            return false;
        }
    }

    /**
     * Returns true if the connectivity IC supports 5G band.
     * @return true menas the Wi-Fi 5G band is supported.
     */
    @SuppressWarnings(value = {"localfinalvariablename" })
        public boolean is5gBandSupported() {
            final int NARAM_5G_BAND_SUPPORT = 0xc5;
            final int NARAM_5G_BAND_ENABLE = 0x106;
            final String MAC_ADDRESS_FILENAME = "/data/nvram/APCFG/APRDEB/WIFI";
            final String NVRAM_AGENT_SERVICE = "NvRAMAgent";
            int wifi5gBandSupported = 0;

            try {
                WifiNvRamAgent agent = WifiNvRamAgent.Stub.asInterface(
                        ServiceManager.getService(NVRAM_AGENT_SERVICE));
                byte[] buffer = agent.readFileByName(MAC_ADDRESS_FILENAME);
                wifi5gBandSupported = buffer[NARAM_5G_BAND_SUPPORT] & buffer[NARAM_5G_BAND_ENABLE];
                Log.i(TAG, "wifiSupport5g:" + wifi5gBandSupported
                        + ":" + buffer[NARAM_5G_BAND_SUPPORT] + ":" + buffer[NARAM_5G_BAND_ENABLE]);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException ee) {
                ee.printStackTrace();
            }
            return (wifi5gBandSupported == 1) ? true : false;
        }


    /**
     * Returns true  if set hotspot optimization success.
     *@param enable set enable true means do not reconnect and scan.
     * @return true menas  set hotspot optimization success.
     */
    @SuppressWarnings(value = {"localfinalvariablename" })
        public boolean setHotspotOptimization(boolean enable) {
            mWifiStateMachine.setHotspotOptimization(enable);
            return true;
        }

    /**
     * Get test environment.
     * @param channel Wi-Fi channel
     * @return test environment string
     */
    public String getTestEnv(int channel) {
        enforceAccessPermission();
        if (mWifiStateMachineChannel != null) {
            return mWifiStateMachine.syncGetTestEnv(mWifiStateMachineChannel, channel);
        } else {
            Slog.e(TAG, "mWifiStateMachineChannel is not initialized");
            return null;
        }
    }

    /**
     * Set TDLS Power Save mode.
     * @param enable
     * @return success or not
     */
    public void setTdlsPowerSave(boolean enable) {
        mWifiStateMachine.setTdlsPowerSave(enable);
    }
    ///@}

    ///M: ALPS02230807 Other modules can use WifiOffListener to know wifi is going to disabled.
    //    And turn off itself and call setWifiDisable to close wifi. @{
    /**
     * Disable Wi-Fi.
     * @param flag the flag type of disable method.
     * @return the disable functin is succeed or nots   .
     * {@hide}
     */
    public boolean setWifiDisabled(int flag) {
        boolean enable = false;

        Slog.d(TAG, "setWifiDisabled:" + flag);

        if (flag == DISABLE_WIFI_SETTING) {
              /*
               * Caller might not have WRITE_SECURE_SETTINGS,
               * only CHANGE_WIFI_STATE is enforced
               */
              long ident = Binder.clearCallingIdentity();
              try {
                  if (!mSettingsStore.handleWifiToggled(enable)) {
                      // Nothing to do if wifi cannot be toggled
                      return true;
                  }
              } finally {
                  Binder.restoreCallingIdentity(ident);
              }

              ///M: put extra mWifiIpoOff
              mWifiController.obtainMessage(CMD_WIFI_TOGGLED, mWifiIpoOff ? 1 : 0,
                      WifiController.EPDG_UID).sendToTarget();
        } else if (flag == DISABLE_WIFI_FLIGHTMODE) {
            mWifiController.sendMessage(CMD_AIRPLANE_TOGGLED, 1/*source from setWifiDisabled*/);
        }

        return true;
   }

    private int mWifiOffListenerCount = 0;
    public void registerWifiOffListener() {
        mWifiOffListenerCount ++;
        Slog.d(TAG, "registWifiOffListener, mWifiOffListenerCount: " + mWifiOffListenerCount);
    }

    public void unregisterWifiOffListener() {
        mWifiOffListenerCount --;
        Slog.d(TAG, "unregistWifiOffListener, mWifiOffListenerCount: " + mWifiOffListenerCount);
    }

    private void reportWifiOff(int reason) {
        Slog.d(TAG, "reportWifiOff, reason: " +
                (reason == DISABLE_WIFI_SETTING ? "wifi setting" : "airplane mode on"));
        Intent intent = new Intent(WifiManager.WIFI_OFF_NOTIFY);
        intent.putExtra(WifiManager.EXTRA_WIFI_OFF_REASON, reason);
        //[BIRD][TASK #5767][jira 127 设置-用户 添加用户 ,设置新添加的用户时 会报错][chenguangxiang][20170909] begin
        //mContext.sendBroadcast(intent);
        long id = Binder.clearCallingIdentity();
        try {
            mContext.sendBroadcast(intent);
        } finally {
            Binder.restoreCallingIdentity(id);
        }
        //[BIRD][TASK #5767][jira 127 设置-用户 添加用户 ,设置新添加的用户时 会报错][chenguangxiang][20170909] end
    }
    ///@}
    
    /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]BEGIN */
    private final List<String> ZX_APPS = Arrays.asList("com.njzx.launcher", "com.njzx.lock", "com.njzx.shbmoniservice", "com.njzx.shbremotedesktop", "com.njzx.traffic", "com.njzx.weather", "com.cutecomm.cloudcc.service", "com.njzx.shbsetting");
    /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]END */
}
