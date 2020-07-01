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

package com.android.server.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

///M: Add ringtone for notification @{
import android.media.RingtoneManager;
import android.util.Slog;
///@}

///M: for operator plugin @{
import com.mediatek.common.wifi.IWifiFwkExt;
import android.net.wifi.IWifiManager;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import android.telephony.TelephonyManager;
///@}
/*[BIRD][BIRD_REMOVE_WLAN_NOTIFICATION_SOUND][去除可用WLAN提示音][zhangaman][20170217]begin*/
import android.os.SystemProperties;
/*[BIRD][BIRD_REMOVE_WLAN_NOTIFICATION_SOUND][去除可用WLAN提示音][zhangaman][20170217]end*/

/* Takes care of handling the "open wi-fi network available" notification @hide */
final class WifiNotificationController {
    private static final String TAG = "WifiNotificationController";

    /**
     * The icon to show in the 'available networks' notification. This will also
     * be the ID of the Notification given to the NotificationManager.
     */
    private static final int ICON_NETWORKS_AVAILABLE =
            com.android.internal.R.drawable.stat_notify_wifi_in_range;
    /**
     * When a notification is shown, we wait this amount before possibly showing it again.
     */
    private final long NOTIFICATION_REPEAT_DELAY_MS;
    /**
     * Whether the user has set the setting to show the 'available networks' notification.
     */
    private boolean mNotificationEnabled;
    /**
     * Observes the user setting to keep {@link #mNotificationEnabled} in sync.
     */
    private NotificationEnabledSettingObserver mNotificationEnabledSettingObserver;
    /**
     * The {@link System#currentTimeMillis()} must be at least this value for us
     * to show the notification again.
     */
    private long mNotificationRepeatTime;
    /**
     * The Notification object given to the NotificationManager.
     */
    private Notification.Builder mNotificationBuilder;
    /**
     * Whether the notification is being shown, as set by us. That is, if the
     * user cancels the notification, we will not receive the callback so this
     * will still be true. We only guarantee if this is false, then the
     * notification is not showing.
     */
    private boolean mNotificationShown;
    /**
     * The number of continuous scans that must occur before consider the
     * supplicant in a scanning state. This allows supplicant to associate with
     * remembered networks that are in the scan results.
     */
    ///M: ALPS02781210 Modified for pop up when the first scan result available
    private static final int NUM_SCANS_BEFORE_ACTUALLY_SCANNING = 0;
    /**
     * The number of scans since the last network state change. When this
     * exceeds {@link #NUM_SCANS_BEFORE_ACTUALLY_SCANNING}, we consider the
     * supplicant to actually be scanning. When the network state changes to
     * something other than scanning, we reset this to 0.
     */
    private int mNumScansSinceNetworkStateChange;

    private final Context mContext;
    private final WifiStateMachine mWifiStateMachine;
    private NetworkInfo mNetworkInfo;
    private NetworkInfo.DetailedState mDetailedState;
    private volatile int mWifiState;
    private FrameworkFacade mFrameworkFacade;
    ///M: for operator plugin @{
    private boolean mWaitForScanResult = false;
    private boolean mShowReselectDialog = false;
    ///@}

    WifiNotificationController(Context context, Looper looper, WifiStateMachine wsm,
            FrameworkFacade framework, Notification.Builder builder) {
        mContext = context;
        mWifiStateMachine = wsm;
        mFrameworkFacade = framework;
        mNotificationBuilder = builder;
        mWifiState = WifiManager.WIFI_STATE_UNKNOWN;
        mDetailedState = NetworkInfo.DetailedState.IDLE;

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                            mWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                                    WifiManager.WIFI_STATE_UNKNOWN);
                            resetNotification();
                            ///M: for operator plugin @{
                            mWaitForScanResult = false;
                            mShowReselectDialog = false;
                            ///@}
                        } else if (intent.getAction().equals(
                                WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                            mNetworkInfo = (NetworkInfo) intent.getParcelableExtra(
                                    WifiManager.EXTRA_NETWORK_INFO);
                            NetworkInfo.DetailedState detailedState =
                                    mNetworkInfo.getDetailedState();
                            if (detailedState != NetworkInfo.DetailedState.SCANNING
                                    && detailedState != mDetailedState) {
                                mDetailedState = detailedState;
                                // reset & clear notification on a network connect & disconnect
                                switch(mDetailedState) {
                                    case CONNECTED:
                                        ///M: for operator plugin @{
                                        mWaitForScanResult = false;
                                        ///@}
                                    case DISCONNECTED:
                                    case CAPTIVE_PORTAL_CHECK:
                                        resetNotification();
                                        break;
                                }
                            }
                        } else if (intent.getAction().equals(
                                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                            ///M: for operator plugin @{
                            mShowReselectDialog =
                                intent.getBooleanExtra(
                                    IWifiFwkExt.EXTRA_SHOW_RESELECT_DIALOG_FLAG,
                                    false);
                            ///@}
                            checkAndSetNotification(mNetworkInfo,
                                    mWifiStateMachine.syncGetScanResultsList());
                        }
                    }
                }, filter);

        // Setting is in seconds
        NOTIFICATION_REPEAT_DELAY_MS = mFrameworkFacade.getIntegerSetting(context,
                Settings.Global.WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY, 900) * 1000l;
        mNotificationEnabledSettingObserver = new NotificationEnabledSettingObserver(
                new Handler(looper));
        mNotificationEnabledSettingObserver.register();
    }

    private synchronized void checkAndSetNotification(NetworkInfo networkInfo,
            List<ScanResult> scanResults) {

        // TODO: unregister broadcast so we do not have to check here
        // If we shouldn't place a notification on available networks, then
        // don't bother doing any of the following
        if (!mNotificationEnabled) return;
        if (mWifiState != WifiManager.WIFI_STATE_ENABLED) return;

        NetworkInfo.State state = NetworkInfo.State.DISCONNECTED;
        if (networkInfo != null)
            state = networkInfo.getState();

        ///M: for operator plugin @{
        if (mWifiStateMachine.hasCustomizedAutoConnect()) {
            Slog.i(TAG, "checkAndSetNotification, mWaitForScanResult:" + mWaitForScanResult);
            if (mWaitForScanResult && scanResults == null) {
                showSwitchDialog();
            }
        }
        Slog.i(TAG, "checkAndSetNotification, state:" + state);
        ///@}

        if ((state == NetworkInfo.State.DISCONNECTED)
                || (state == NetworkInfo.State.UNKNOWN)) {
            if (scanResults != null) {
                int numOpenNetworks = 0;
                for (int i = scanResults.size() - 1; i >= 0; i--) {
                    ScanResult scanResult = scanResults.get(i);

                    //A capability of [ESS] represents an open access point
                    //that is available for an STA to connect
                    if (scanResult.capabilities != null &&
                            scanResult.capabilities.equals("[ESS]")) {
                        numOpenNetworks++;
                    }
                }

                ///M: for operator plugin @{
                IBinder binder = ServiceManager.getService(Context.WIFI_SERVICE);
                final IWifiManager wifiService = IWifiManager.Stub.asInterface(binder);
                int networkId = -1;
                try {
                    if (wifiService != null) {
                        networkId = wifiService.syncGetConnectingNetworkId();
                    }
                } catch (RemoteException e) {
                      Slog.d(TAG, "syncGetConnectingNetworkId failed!");
                }

                boolean isConnecting = mWifiStateMachine.isWifiConnecting(networkId);
                Slog.d(TAG, "Connecting networkId:" + networkId + ", isConnecting:" + isConnecting);
                if (mWifiStateMachine.hasCustomizedAutoConnect()) {
                    if (isConnecting) {
                        //mWaitForScanResult = false;
                        return;
                    } else {
                        if (mWaitForScanResult) {
                            showSwitchDialog();
                        }
                    }
                }

                Slog.i(TAG, "Open network num:" + numOpenNetworks);
                ///@}

                if (numOpenNetworks > 0) {
                    if (++mNumScansSinceNetworkStateChange >= NUM_SCANS_BEFORE_ACTUALLY_SCANNING) {
                        /*
                         * We've scanned continuously at least
                         * NUM_SCANS_BEFORE_NOTIFICATION times. The user
                         * probably does not have a remembered network in range,
                         * since otherwise supplicant would have tried to
                         * associate and thus resetting this counter.
                         */
                        setNotificationVisible(true, numOpenNetworks, false, 0);
                    }
                    return;
                }
            }
        }

        // No open networks in range, remove the notification
        setNotificationVisible(false, 0, false, 0);
    }

    /**
     * Clears variables related to tracking whether a notification has been
     * shown recently and clears the current notification.
     */
    private synchronized void resetNotification() {
        mNotificationRepeatTime = 0;
        mNumScansSinceNetworkStateChange = 0;
        setNotificationVisible(false, 0, false, 0);
    }

    /**
     * Display or don't display a notification that there are open Wi-Fi networks.
     * @param visible {@code true} if notification should be visible, {@code false} otherwise
     * @param numNetworks the number networks seen
     * @param force {@code true} to force notification to be shown/not-shown,
     * even if it is already shown/not-shown.
     * @param delay time in milliseconds after which the notification should be made
     * visible or invisible.
     */
    private void setNotificationVisible(boolean visible, int numNetworks, boolean force,
            int delay) {

        // Since we use auto cancel on the notification, when the
        // mNetworksAvailableNotificationShown is true, the notification may
        // have actually been canceled.  However, when it is false we know
        // for sure that it is not being shown (it will not be shown any other
        // place than here)

        // If it should be hidden and it is already hidden, then noop
        if (!visible && !mNotificationShown && !force) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Message message;
        if (visible) {

            // Not enough time has passed to show the notification again
            if (System.currentTimeMillis() < mNotificationRepeatTime) {
                return;
            }

            if (mNotificationBuilder == null) {
                //[BIRD][bug-13819][BIRD_OLD_PHONE][songwei][20170825]BEGIN
                boolean isOldMode = Settings.System.getInt(mContext.getContentResolver(), "is_oldphoneluancher_mode", 0) == 1;
                Intent mOldintent = new Intent();
                mOldintent.setClassName("com.android.settings","com.android.settings.wifi.OldPhoneWifiSettings");
                //[BIRD][bug-13819][BIRD_OLD_PHONE][songwei][20170825]END
                // Cache the Notification builder object.
                //[BIRD][bug-13819][BIRD_OLD_PHONE][songwei][20170825]BEGIN
                mNotificationBuilder = new Notification.Builder(mContext)
                        .setWhen(0)
                        .setSmallIcon(ICON_NETWORKS_AVAILABLE)
                        .setAutoCancel(true)
                        .setContentIntent(TaskStackBuilder.create(mContext)
                                .addNextIntentWithParentStack(isOldMode ? mOldintent:
                                        new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK))
                                .getPendingIntent(0, 0, null, UserHandle.CURRENT))
                        .setColor(mContext.getResources().getColor(
                                com.android.internal.R.color.system_notification_accent_color));
                //[BIRD][bug-13819][BIRD_OLD_PHONE][songwei][20170825]END
            }

            CharSequence title = mContext.getResources().getQuantityText(
                    com.android.internal.R.plurals.wifi_available, numNetworks);
            CharSequence details = mContext.getResources().getQuantityText(
                    com.android.internal.R.plurals.wifi_available_detailed, numNetworks);
            mNotificationBuilder.setTicker(title);
            mNotificationBuilder.setContentTitle(title);
            mNotificationBuilder.setContentText(details);

            mNotificationRepeatTime = System.currentTimeMillis() + NOTIFICATION_REPEAT_DELAY_MS;

            ///M: Add ringtone for notification @{
            if (!mNotificationShown) {
                mNotificationBuilder.setSound(RingtoneManager.getActualDefaultRingtoneUri(mContext,
                    RingtoneManager.TYPE_NOTIFICATION));
            } else {
                mNotificationBuilder.setSound(null);
            }
            /*[BIRD][BIRD_REMOVE_WLAN_NOTIFICATION_SOUND][去除可用WLAN提示音][zhangaman][20170217]begin*/
            if (SystemProperties.get("ro.bird_remove_wlan_sound").equals("1")) {
                mNotificationBuilder.setSound(null);
            }
            /*[BIRD][BIRD_REMOVE_WLAN_NOTIFICATION_SOUND][去除可用WLAN提示音][zhangaman][20170217]end*/

            Slog.d(TAG, "Pop up notification, mNotificationBuilder.setSound");
            ///@}
            ///M: ALPS01931078 UserHandle change to OWNER
            notificationManager.notifyAsUser(null, ICON_NETWORKS_AVAILABLE,
                    mNotificationBuilder.build(), UserHandle.CURRENT);
        } else {
            Slog.d(TAG, "cancel notification");
            notificationManager.cancelAsUser(null, ICON_NETWORKS_AVAILABLE, UserHandle.ALL);
        }

        mNotificationShown = visible;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mNotificationEnabled " + mNotificationEnabled);
        pw.println("mNotificationRepeatTime " + mNotificationRepeatTime);
        pw.println("mNotificationShown " + mNotificationShown);
        pw.println("mNumScansSinceNetworkStateChange " + mNumScansSinceNetworkStateChange);
    }

    private class NotificationEnabledSettingObserver extends ContentObserver {
        public NotificationEnabledSettingObserver(Handler handler) {
            super(handler);
        }

        public void register() {
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.Global.getUriFor(
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON), true, this);
            synchronized (WifiNotificationController.this) {
                mNotificationEnabled = getValue();
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            synchronized (WifiNotificationController.this) {
                mNotificationEnabled = getValue();
                resetNotification();
            }
        }

        private boolean getValue() {
            return mFrameworkFacade.getIntegerSetting(mContext,
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 1) == 1;
        }
    }

    ///M: for operator plugin @{
    private boolean isDataAvailable() {
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (phone == null || !phone.isRadioOn(mContext.getPackageName()) || tm == null) {
                return false;
            }

            boolean isSim1Insert = tm.hasIccCard(PhoneConstants.SIM_ID_1);
            boolean isSim2Insert = false;
            if (tm.getDefault().getPhoneCount() >= 2) {
                isSim2Insert = tm.hasIccCard(PhoneConstants.SIM_ID_2);
            }
            if (!isSim1Insert && !isSim2Insert) {
               return false;
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get phone service, error:" + e);
            return false;
        }
        return true;
    }

    private void showSwitchDialog() {
        mWaitForScanResult = false;
        boolean isDataAvailable = isDataAvailable();
        Slog.d(TAG, "showSwitchDialog, isDataAvailable:" + isDataAvailable
                + ", mShowReselectDialog:" + mShowReselectDialog);
        if (mShowReselectDialog) {
            return;
        }
        if (isDataAvailable) {
            Intent intent = new Intent(IWifiFwkExt.ACTION_WIFI_FAILOVER_GPRS_DIALOG);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /*@hide*/
    public void setWaitForScanResult(boolean value) {
        mWaitForScanResult = value;
    }
    ///@}
}
