/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.power;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;

import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.widget.Toast;
import com.android.systemui.FeatureOption;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.io.IOException;
//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END

public class PowerUI extends SystemUI {
    static final String TAG = "PowerUI";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver();

    private PowerManager mPowerManager;
    private WarningsUI mWarnings;
    private int mBatteryLevel = 100;
    private int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private int mPlugType = 0;
    private int mInvalidCharger = 0;

    private int mLowBatteryAlertCloseLevel;
    private final int[] mLowBatteryReminderLevels = new int[2];

    private long mScreenOffTime = -1;
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    ContentResolver mContentResolver;
    final static String ACTION_GENERAL_SAVING_CLOSE = "com.bird.powermanager.ACTION_GENERAL_SAVING_CLOSE";
    private final static String ACTION_SUPER_SAVING_STATUS_CHANGE = "com.bird.powermanager.ACTION_SUPER_SAVING_STATUS_CHANGE";
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
    
    /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]BEGIN */
    private BirdShutdownChargeNotification mBirdShutdownChargeNotification;
    /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]END */

    public void start() {
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mScreenOffTime = mPowerManager.isScreenOn() ? -1 : SystemClock.elapsedRealtime();
        mWarnings = new PowerNotificationWarnings(mContext, getComponent(PhoneStatusBar.class));
        
        /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]BEGIN */
        if (FeatureOption.BIRD_SHOTDOWN_CHARGE_SUGGESTION) {
            mBirdShutdownChargeNotification = new BirdShutdownChargeNotification(mContext);
        }
        /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]END */

        ContentObserver obs = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                updateBatteryWarningLevels();
            }
        };
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL),
                false, obs, UserHandle.USER_ALL);
        updateBatteryWarningLevels();
        mReceiver.init();
    }

    void updateBatteryWarningLevels() {
        int critLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);

        final ContentResolver resolver = mContext.getContentResolver();
        int defWarnLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        int warnLevel = Settings.Global.getInt(resolver,
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, defWarnLevel);
        if (warnLevel == 0) {
            warnLevel = defWarnLevel;
        }
        if (warnLevel < critLevel) {
            warnLevel = critLevel;
        }
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
        mContentResolver = mContext.getContentResolver();
        //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END

        mLowBatteryReminderLevels[0] = warnLevel;
        mLowBatteryReminderLevels[1] = critLevel;
        mLowBatteryAlertCloseLevel = mLowBatteryReminderLevels[0]
                + mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_lowBatteryCloseWarningBump);
    }

    /**
     * Buckets the battery level.
     *
     * The code in this function is a little weird because I couldn't comprehend
     * the bucket going up when the battery level was going down. --joeo
     *
     * 1 means that the battery is "ok"
     * 0 means that the battery is between "ok" and what we should warn about.
     * less than 0 means that the battery is low
     */
    private int findBatteryLevelBucket(int level) {
        if (level >= mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (level > mLowBatteryReminderLevels[0]) {
            return 0;
        }
        final int N = mLowBatteryReminderLevels.length;
        for (int i=N-1; i>=0; i--) {
            if (level <= mLowBatteryReminderLevels[i]) {
                return -1-i;
            }
        }
        throw new RuntimeException("not possible!");
    }

    private final class Receiver extends BroadcastReceiver {

        public void init() {
            // Register for Intent broadcasts for...
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            mContext.registerReceiver(this, filter, null, mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                final int oldBatteryLevel = mBatteryLevel;
                mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                final int oldBatteryStatus = mBatteryStatus;
                mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                final int oldPlugType = mPlugType;
                mPlugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
                final int oldInvalidCharger = mInvalidCharger;
                mInvalidCharger = intent.getIntExtra(BatteryManager.EXTRA_INVALID_CHARGER, 0);

                final boolean plugged = mPlugType != 0;
                final boolean oldPlugged = oldPlugType != 0;

                int oldBucket = findBatteryLevelBucket(oldBatteryLevel);
                int bucket = findBatteryLevelBucket(mBatteryLevel);

                if (DEBUG) {
                    Slog.d(TAG, "buckets   ....." + mLowBatteryAlertCloseLevel
                            + " .. " + mLowBatteryReminderLevels[0]
                            + " .. " + mLowBatteryReminderLevels[1]);
                    Slog.d(TAG, "level          " + oldBatteryLevel + " --> " + mBatteryLevel);
                    Slog.d(TAG, "status         " + oldBatteryStatus + " --> " + mBatteryStatus);
                    Slog.d(TAG, "plugType       " + oldPlugType + " --> " + mPlugType);
                    Slog.d(TAG, "invalidCharger " + oldInvalidCharger + " --> " + mInvalidCharger);
                    Slog.d(TAG, "bucket         " + oldBucket + " --> " + bucket);
                    Slog.d(TAG, "plugged        " + oldPlugged + " --> " + plugged);
                }
                
                /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]BEGIN */
                if (FeatureOption.BIRD_SHOTDOWN_CHARGE_SUGGESTION) {
                    if (plugged && oldPlugged == false) {
                        mBirdShutdownChargeNotification.showNotification();
                    } else if (plugged == false) {
                        mBirdShutdownChargeNotification.dismissNotification();
                    }
                }
                /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]END */
                
                //[BIRD][BIRD_FULL_POWER_RING][充满电铃声提示][youhonggang][20170324] begin
                if (FeatureOption.BIRD_FULL_POWER_RING && plugged 
                    && (oldBatteryLevel == 99 && mBatteryLevel == 100)
                    /*&& mBatteryStatus == BatteryManager.BATTERY_STATUS_FULL*/) {
                    Slog.d(TAG,"mBatteryStatus :" + mBatteryStatus);
                    playLowBatterySound(context);
                }
                //[BIRD][BIRD_FULL_POWER_RING][充满电铃声提示][youhonggang][20170324] end
                
                //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
                if (FeatureOption.BIRD_POWER_MANAGER_V2) {
                    if (oldPlugged == false && plugged == true && mSaverConfirmation != null) {
                        mSaverConfirmation.dismiss();
                    }
                    //如果是普省
                    if (Settings.System.getInt(mContentResolver, Settings.System.BIRD_GENERAL_SAVING_STATUS, 0) == 1) {
                        //电量在充电，同时是普省，并且到达80%
                        if (plugged && oldBatteryLevel == 79 && mBatteryLevel == 80 ) {
                        	Intent generalCloseIntent = new Intent(ACTION_GENERAL_SAVING_CLOSE);
                            mContext.sendBroadcast(generalCloseIntent);
                        }
                    }

                    //如果电量大于80%，下面无需进行|| 超省已进入
                    if (mBatteryLevel >= 80 
                            || Settings.System.getInt(mContentResolver, Settings.System.BIRD_SUPER_SAVING_STATUS, 0) == 1) {
                        return;
                    }

                    if (Settings.System.getInt(mContentResolver, Settings.System.BIRD_SUPER_SAVING_AUTO_ENTER, 0) == 1) {
                        //超省自动打开。普通进入；开机低电量进入自动进入
                        if (oldInvalidCharger == 0 && mInvalidCharger != 0) {
                            Slog.d(TAG, "showing invalid charger warning");
                            return;
                        } else if (oldInvalidCharger != 0 && mInvalidCharger == 0) {
                        } else if (mWarnings.isInvalidChargerWarningShowing()) {
                            Log.i(TAG, "mWarnings.isInvalidChargerWarningShowing() ");
                            return;
                        }

                        int autoValue = Settings.System.getInt(mContentResolver,
                                Settings.System.BIRD_SUPER_SAVING_AUTO_ENTER_DEFAULT_VALUE, 15);
                        Log.i(TAG, "autoValue = " + autoValue);
                        if (!plugged
                                && oldBatteryLevel >= autoValue + 1
                                && mBatteryLevel <= autoValue
                                && mBatteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN) {
                            playLowBatterySound(context);
                            // Intent superSaverIntent = new Intent(ACTION_SUPER_SAVING_STATUS_CHANGE);
                            // superSaverIntent.putExtra("launcher_mode", "super_save");
                            // mContext.sendBroadcast(superSaverIntent);
                            showStartSaverConfirmation(true);
                            Log.i(TAG, "mBatteryLevel= " + mBatteryLevel);
                            return;
                        } else if (!plugged && oldPlugged && mBatteryLevel <= autoValue) {
                            playLowBatterySound(context);
                            // Intent superSaverIntent = new Intent(ACTION_SUPER_SAVING_STATUS_CHANGE);
                            // superSaverIntent.putExtra("launcher_mode", "super_save");
                            // mContext.sendBroadcast(superSaverIntent);
                            showStartSaverConfirmation(true);
                            Log.i(TAG, "mBatteryLevel= " + mBatteryLevel + ", oldPlugged = " +oldPlugged);
                            return;
                        }
                        //自动已经打开，但是电量到达10% 20% 或者当前低于20%且 从充电到没有充电，弹出进入对话框
                        if (!plugged
                                && ((oldBatteryLevel >= 16 && mBatteryLevel <=15 )|| (oldBatteryLevel == 11 && mBatteryLevel == 10))//[BIRD][TASK-4837][20%电量就出现了低电提示 （15% 开始才能出现低点提示）][yangheng][20170526]
                                && mBatteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN) {
                            playLowBatterySound(context);
                            showStartSaverConfirmation(false);
                        } else if (!plugged && oldPlugged && mBatteryLevel < 15) {
                            playLowBatterySound(context);
                            showStartSaverConfirmation(false);
                        }
                    } else {
                        //超省自动没有打开，则在两处分别进行判断10% 20%提示
                        if (oldInvalidCharger == 0 && mInvalidCharger != 0) {
                            Slog.d(TAG, "showing invalid charger warning");
                            return;
                        } else if (oldInvalidCharger != 0 && mInvalidCharger == 0) {
                        } else if (mWarnings.isInvalidChargerWarningShowing()) {
                            return;
                        }
                        //超省自动没有打开，则在两处分别进行判断10% 20%提示
                        if (!plugged
                                && ((oldBatteryLevel >= 16 && mBatteryLevel <=15 )|| (oldBatteryLevel == 11 && mBatteryLevel == 10))//[BIRD][TASK-4837][20%电量就出现了低电提示 （15% 开始才能出现低点提示）][yangheng][20170526]
                                && mBatteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN) {
                            playLowBatterySound(context);
                            showStartSaverConfirmation(false);
                        } else if (!plugged && oldPlugged && mBatteryLevel < 15) {
                            playLowBatterySound(context);
                            showStartSaverConfirmation(false);
                        }
                    }

                } else {
                //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END
                    mWarnings.update(mBatteryLevel, bucket, mScreenOffTime);
                    if (oldInvalidCharger == 0 && mInvalidCharger != 0) {
                        Slog.d(TAG, "showing invalid charger warning");
                        mWarnings.showInvalidChargerWarning();
                        return;
                    } else if (oldInvalidCharger != 0 && mInvalidCharger == 0) {
                        mWarnings.dismissInvalidChargerWarning();
                    } else if (mWarnings.isInvalidChargerWarningShowing()) {
                        // if invalid charger is showing, don't show low battery
                        return;
                    }

                    boolean isPowerSaver = mPowerManager.isPowerSaveMode();
                    if (!plugged
                            && !isPowerSaver
                            && (bucket < oldBucket || oldPlugged)
                            && mBatteryStatus != BatteryManager.BATTERY_STATUS_UNKNOWN
                            && bucket < 0) {
                        // only play SFX when the dialog comes up or the bucket changes
                        final boolean playSound = bucket != oldBucket || oldPlugged;
                        mWarnings.showLowBatteryWarning(playSound);
                    } else if (isPowerSaver || plugged || (bucket > oldBucket && bucket > 0)) {
                        mWarnings.dismissLowBatteryWarning();
                    } else {
                        mWarnings.updateLowBatteryWarning();
                    }//[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] 
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mScreenOffTime = SystemClock.elapsedRealtime();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                mScreenOffTime = -1;
            } else if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                mWarnings.userSwitched();
            } else {
                Slog.w(TAG, "unknown intent: " + intent);
            }
        }
    };

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mLowBatteryAlertCloseLevel=");
        pw.println(mLowBatteryAlertCloseLevel);
        pw.print("mLowBatteryReminderLevels=");
        pw.println(Arrays.toString(mLowBatteryReminderLevels));
        pw.print("mBatteryLevel=");
        pw.println(Integer.toString(mBatteryLevel));
        pw.print("mBatteryStatus=");
        pw.println(Integer.toString(mBatteryStatus));
        pw.print("mPlugType=");
        pw.println(Integer.toString(mPlugType));
        pw.print("mInvalidCharger=");
        pw.println(Integer.toString(mInvalidCharger));
        pw.print("mScreenOffTime=");
        pw.print(mScreenOffTime);
        if (mScreenOffTime >= 0) {
            pw.print(" (");
            pw.print(SystemClock.elapsedRealtime() - mScreenOffTime);
            pw.print(" ago)");
        }
        pw.println();
        pw.print("soundTimeout=");
        pw.println(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.LOW_BATTERY_SOUND_TIMEOUT, 0));
        pw.print("bucket: ");
        pw.println(Integer.toString(findBatteryLevelBucket(mBatteryLevel)));
        mWarnings.dump(pw);
    }

    public interface WarningsUI {
        void update(int batteryLevel, int bucket, long screenOffTime);
        void dismissLowBatteryWarning();
        void showLowBatteryWarning(boolean playSound);
        void dismissInvalidChargerWarning();
        void showInvalidChargerWarning();
        void updateLowBatteryWarning();
        boolean isInvalidChargerWarningShowing();
        void dump(PrintWriter pw);
        void userSwitched();
    }
    
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    private SystemUIDialog mSaverConfirmation;
    private void showStartSaverConfirmation(boolean isTimerCount) {
        if (mSaverConfirmation != null) return;
        final SystemUIDialog d = new SystemUIDialog(mContext, R.style.StyleDialog);
        d.setTitle(R.string.bird_low_battery);
        d.setNegativeButton(R.string.bird_cancel, null);
        d.setPositiveButton(R.string.bird_enter_super_saver, mStartSaverMode);
        d.setShowForAllUsers(true);
        d.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mCountDownTimer.cancel();
                mSaverConfirmation = null;
            }
        });
        if (isTimerCount) {
             d.setMessage(mContext.getResources().getString(R.string.bird_low_battery_remaining_with_timer_count,
                 Settings.System.getInt(mContentResolver,Settings.System.BIRD_SUPER_SAVING_AUTO_ENTER_DEFAULT_VALUE, 15),
                 mBatteryLevel,
                 15));
            mCountDownTimer.start();
        } else {
            d.setMessage(mContext.getResources().getString(R.string.bird_low_battery_remaining, mBatteryLevel));
        }
        d.show();
        mSaverConfirmation = d;
    }

    private CountDownTimer mCountDownTimer = new CountDownTimer(16000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            if (mSaverConfirmation != null) {
                mSaverConfirmation.setMessage(mContext.getResources().getString(R.string.bird_low_battery_remaining_with_timer_count,
                        Settings.System.getInt(mContentResolver,Settings.System.BIRD_SUPER_SAVING_AUTO_ENTER_DEFAULT_VALUE, 15),
                        mBatteryLevel,
                        (int)(millisUntilFinished / 1000)
                    )
                );
            }

        }  
  
        @Override  
        public void onFinish() {
            //not need check if it is super saver already.
            if (mSaverConfirmation != null) {
                mSaverConfirmation.dismiss();
            }
            startSendBroadcast();
        }
    }; 

    private final OnClickListener mStartSaverMode = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    startSendBroadcast();
                }
            });
        }
    };

    synchronized private void startSendBroadcast() {
        if (Settings.System.getInt(mContentResolver, Settings.System.BIRD_SUPER_SAVING_STATUS, 0) != 1) {
            Intent intent = new Intent(ACTION_SUPER_SAVING_STATUS_CHANGE);
            intent.putExtra("launcher_mode", "super_save");
            mContext.sendBroadcast(intent);
        }
    }

    private static void playLowBatterySound(final Context context) {

        //add for plug in.@{
        /// M: if in silent mode, do not play USSD tone, see ALPS00424814 @{
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            Log.d(TAG, "ringerMode = " + ringerMode + ", do not play USSD tone...");
            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = new long[]{0l, 350l, 250l, 350l};
                vibrator.vibrate(pattern, -1);
                Log.d(TAG, "ringerMode = " + ringerMode + ", BIRD, need VIBRATE");
            }
            return;
        }


        new Thread(new Runnable() {
            public void run() {
                final MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.reset();
                try {
                    mediaPlayer.setDataSource(context, Uri.parse("file://" + Settings.Global.getString(context.getContentResolver(),
                            Settings.Global.LOW_BATTERY_SOUND)));
                    mediaPlayer.prepare();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer.release();
                    }
                });
            }
        }).start();
    }
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
}

