/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.screenshot;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.WindowManager;

//[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import com.android.systemui.R;
import com.android.systemui.FeatureOption;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
//[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END

public class TakeScreenshotService extends Service {
    private static final String TAG = "TakeScreenshotService";

    private static GlobalScreenshot mScreenshot;

    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
    private static final int PROFILE_SWITCH_DIALOG_LONG_TIMEOUT = 5000;
    private static final int PROFILE_SWITCH_DIALOG_SHORT_TIMEOUT = 2000;
    private static final int SHOW_SCREENSHOT_SELECTION_DIALOG = 0x1024;
    public static final String BIRD_SCREENSHOT_EXTRA = "bird_screenshot_extra";

    //this for custom regional & long
    private static BirdGlobalScreenshot mBirdScreenshot;
    private Context mContext;
    private Dialog mDialog;
    private static boolean isShowing = false;
    private boolean mFinishedScreenShot = true;
    private String[] mImageButtonTags = new String[] {"bird_full_btn", "bird_regional_btn", "bird_long_btn", "bird_help_btn"};
    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final Messenger callback = msg.replyTo;
            Runnable finisher = new Runnable() {
                @Override
                public void run() {
                    Message reply = Message.obtain(null, 1);
                    try {
                        callback.send(reply);
                    } catch (RemoteException e) {
                    }
                }
            };
            if (mScreenshot == null) {
                mScreenshot = new GlobalScreenshot(TakeScreenshotService.this);
            }

            switch (msg.what) {
                case WindowManager.TAKE_SCREENSHOT_FULLSCREEN:
                    mScreenshot.takeScreenshot(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                case WindowManager.TAKE_SCREENSHOT_SELECTED_REGION:
                    mScreenshot.takeScreenshotPartial(finisher, msg.arg1 > 0, msg.arg2 > 0);
                    break;
                //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
                case 3:
                    //coming from QS
                    showSelectionDialog();
                    break;
                case 4://full
                    msg = mHandler.obtainMessage(WindowManager.TAKE_SCREENSHOT_FULLSCREEN);
                    msg.replyTo = new Messenger(new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                Log.i(TAG, "BIRD_SUPER_SCREENSHOT_SUPPORT, has resend msg!");
                            }
                    });
                    mHandler.sendMessage(msg);
                    break;
                case 5://regional
                case 6://long
                    takeScreenshotNow(msg);
                    break;
                case 7://help
                    Intent intent = new Intent();
                    ComponentName componentName = new ComponentName("com.android.systemui",
                        "com.android.systemui.screenshot.BirdScreenshotHelpMain");
                    intent.setComponent(componentName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                    break;
                //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mScreenshot != null) mScreenshot.stopScreenshot();
		//[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
        quit();
		//[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END
        return true;
    }
    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
    private void takeScreenshotNow(Message msg) {
        if (mBirdScreenshot == null) {
            mBirdScreenshot = new BirdGlobalScreenshot(TakeScreenshotService.this);
        }
        mBirdScreenshot.init();
        String whichAction = "regional";
        if (msg.what == 5) {
            whichAction = "regional";
        } else if (msg.what == 6) {
            whichAction = "long";
        }
        mBirdScreenshot.takeScreenshot(new Runnable() {
            @Override public void run() {
                Log.i(TAG, "BIRD_SUPER_SCREENSHOT_SUPPORT, has finish taking screenshot");
            }
        }, msg.arg1 > 0, msg.arg2 > 0, whichAction);//不知道这两个参数干嘛的
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        registerReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int intExtra = intent.getIntExtra(BIRD_SCREENSHOT_EXTRA, -1);
            if (intExtra == 3) {
                Message msg = mHandler.obtainMessage(intExtra);
                mHandler.sendMessage(msg);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    private void dismissScreenShotSelectionDialog(int timeout) {
        removeScreenShotSelectionDialogCallbacks();
        if (mDialog != null) {
            mHandler.postDelayed(mDismissScreenShotSelectionRunnable, timeout);
        }
    }

    private Runnable mDismissScreenShotSelectionRunnable = new Runnable() {
        public void run() {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            removeScreenShotSelectionDialogCallbacks();
        };
    };

    private void removeScreenShotSelectionDialogCallbacks() {
        mHandler.removeCallbacks(mDismissScreenShotSelectionRunnable);
    }


    private void showSelectionDialog() {

        mDialog = new Dialog(this, R.style.BirdScreenShotStyleDialog);

        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.bird_screen_shot_action_select);
        mDialog.getWindow().getAttributes().privateFlags |=
                WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT; 
        lp.height = WindowManager.LayoutParams.MATCH_PARENT; 
        mDialog.getWindow().setAttributes(lp);
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener () {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                //stopSelf();
            }
        });

        ImageButton fullBtn = (ImageButton) mDialog.findViewById(R.id.bird_full_btn);
        fullBtn.setTag("bird_full_btn");
        fullBtn.setOnClickListener(mScreenSelectionListener);

        ImageButton regionalBtn = (ImageButton) mDialog.findViewById(R.id.bird_regional_btn);
        regionalBtn.setTag("bird_regional_btn");
        regionalBtn.setOnClickListener(mScreenSelectionListener);

        ImageButton longBtn = (ImageButton) mDialog.findViewById(R.id.bird_long_btn);
        longBtn.setTag("bird_long_btn");
        longBtn.setOnClickListener(mScreenSelectionListener);
        //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][屏蔽横屏长截屏][qianliliang][20160903] begin
        int ori = TakeScreenshotService.this.getResources().getConfiguration().orientation;
        if (ori != 1) {
            longBtn.setEnabled(false);
            longBtn.setAlpha(0.5f);
        }
        //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][屏蔽横屏长截屏][qianliliang][20160903] end
        ImageButton helpBtn = (ImageButton) mDialog.findViewById(R.id.bird_help_btn);
        helpBtn.setTag("bird_help_btn");
        helpBtn.setOnClickListener(mScreenSelectionListener);

        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);

        if (!mDialog.isShowing()) {
            mDialog.show();
            dismissScreenShotSelectionDialog(PROFILE_SWITCH_DIALOG_LONG_TIMEOUT);
        }
    }

    private View.OnClickListener mScreenSelectionListener = new View.OnClickListener() {
        public void onClick(View v) {
            for (int i = 0; i < 4; i++) {
                if (v.getTag() != null && v.getTag().equals(mImageButtonTags[i])) {
                    mDialog.dismiss();
                    Message msg = mHandler.obtainMessage(i + 4);
                    mHandler.sendMessageDelayed(msg, 500);
                }
            }
        }
    };
	
    private void registerReceiver() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mFilter.addAction("android.intent.action.PHONE_STATE"); 
        mFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS); 
        mFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED); 
        registerReceiver(mReceiver, mFilter);
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                quit();
            } else if ("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
                TelephonyManager tManager = (TelephonyManager) context
                            .getSystemService(Service.TELEPHONY_SERVICE);
                if (tManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                    quit();
                }
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                    Log.d(TAG, "home pressed");
                    quit();
                }
            } else if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
                Log.d("BirdGlobalScreenshot", "CONFIGURATION_CHANGED");
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                } else {
                    if (mBirdScreenshot != null) {
                        mBirdScreenshot.orientationChangedQuit();
                    }
                }
            }
        }
    };
    
    private void quit() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        if (mBirdScreenshot != null) {
            mBirdScreenshot.quitSuperScreenshot();
        }
    }
    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END
}
