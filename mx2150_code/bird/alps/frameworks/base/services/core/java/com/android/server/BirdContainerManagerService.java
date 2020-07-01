/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
package com.android.server;

import android.app.ActivityManagerNative;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.FileUtils;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.am.ActivityManagerService;
import com.android.server.connectivity.PacManager;
import com.android.server.pm.UserManagerService;
import com.bird.security.IContainerManager.Stub;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import com.bird.security.ContainerManager;
import android.widget.Toast;
import android.view.WindowManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.server.pm.PackageManagerService;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import com.android.internal.R;
import android.bluetooth.BluetoothAdapter;
import android.hardware.usb.UsbPort;
import android.os.Binder;

public class BirdContainerManagerService extends Stub {
    public static final String ACTION_USER_ADDED = "com.bird.intent.action.USER_ADDED";
    public static final String ACTION_USER_REMOVED = "com.bird.intent.action.USER_REMOVED";
    private static final String LOG_TAG = "BirdContainerManagerService";
    
    private static final String ENCRYPTSPACE_APP_PACKAGENAME = "com.bird.encryptspace";
    
    public static final int SECURITY_USER_ID = ContainerManager.SECURITY_USER_ID;
    
    private static final int MAX_ATTEMPT_COUNT = 20;
    private static final int DEFAULT_SCREEN_TIMEOUT = 10 * 60 * 1000;
    private static final String KEY_REMAINING_TIMES = "remaining_times";
    
    private final static String KEY_RESTORE_BLUETOOTH = "restore_bluetooth";
    private final static String KEY_RESTORE_ADB = "restore_adb";
    private final static String KEY_RESTORE_DEVELOPMENT_SETTINGS = "restore_development_settings";
    
    private static BirdContainerManagerService sInstance;
    private ActivityManagerService mAms;
    private boolean mContainerJustRemoved = false;
    private boolean mRemoving = false;
    private final Context mContext;
    private boolean mDelContainer = false;
    private boolean mStopContainer = false;
    private final UserManagerService mUms;
    
    private BirdContainerDatabaseHelper mDbHelper;
    
    BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                if (isCurrentContainerUser()) {
                    
                } else {
                    if (mDelContainer) {
                        removeDefaultSecurityUser();
                        mDelContainer = false;
                    }
                    restoreZEROUserStates();
                }
            } else if (Intent.ACTION_USER_REMOVED.equals(intent.getAction())) {
                mRemoving = false;
                mContainerJustRemoved = true;
                context.sendBroadcastAsUser(new Intent(ACTION_USER_REMOVED), new UserHandle(UserHandle.USER_ALL));
                try {
                    mContext.unregisterReceiver(receiver);
                } catch (Exception e) {
                   
                }
                showToast(mContext.getResources().getString(com.android.internal.R.string.zzzzz_secure_user_deleted));
                
            } else if (Intent.ACTION_USER_ADDED.equals(intent.getAction())) {
                int user = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                Log.v(LOG_TAG, "ACTION_USER_ADDED user=" + user);
                if (user == SECURITY_USER_ID) {
                    initConfig(mContext, SECURITY_USER_ID);
                }
                context.sendBroadcastAsUser(new Intent(ACTION_USER_ADDED), new UserHandle(UserHandle.USER_ALL));
                try {
                    mContext.unregisterReceiver(receiver);
                } catch (Exception e) {
                }
            }
        }
    };

    public static BirdContainerManagerService getInstance() {
        BirdContainerManagerService containerManagerService;
        synchronized (BirdContainerManagerService.class) {
            containerManagerService = sInstance;
        }
        return containerManagerService;
    }

    public BirdContainerManagerService(Context context, UserManagerService um) {
        mContext = context;
        mUms = um;
        sInstance = this;
        mDbHelper = new BirdContainerDatabaseHelper(context);
    }

    public void init() {
        initConfig(mContext, 0);
    }

    ActivityManagerService getActivityManagerLocked() {
        if (mAms == null) {
            mAms = (ActivityManagerService) ServiceManager.getService("activity");
        }
        return mAms;
    }

    public boolean addDefaultSecurityUser() {
        if (hasSecurityUser()) {
            Slog.i(LOG_TAG, "has Security User before.");
            return true;
        }
        try {
            mContext.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_ADDED));
            UserInfo info = mUms.createUser(mContext.getResources().getString(R.string.zzzzz_secure_user_name), UserInfo.FLAG_ADMIN);
            if (info != null) {
                Slog.i(LOG_TAG, "Success: created user id " + info.id);
                Settings.Secure.putIntForUser(mContext.getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1, SECURITY_USER_ID);
                return true;
            } else {
                Slog.i(LOG_TAG, "Error: couldn't create User.");
                return false;
            }
        } catch (Exception e) {
            Slog.e(LOG_TAG, "Error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeDefaultSecurityUser() {
        if (hasSecurityUser()) {
            try {
                mRemoving = true;
                showToast(mContext.getResources().getString(com.android.internal.R.string.zzzzz_secure_user_deleting));
                if (mUms.removeUser(SECURITY_USER_ID)) {
                    Slog.i(LOG_TAG, "Success: removed user");
                    return true;
                } else {
                    Slog.i(LOG_TAG, "Error: couldn't remove user id " + SECURITY_USER_ID);
                    return false;
                }
            } catch (Exception e) {
                Slog.e(LOG_TAG, "Error: " + e.getMessage());
                return false;
            }
        }
        Slog.i(LOG_TAG, "No Security User,do not need to remove.");
        return true;
    }

    public boolean isCurrentContainerUser() {
        return SECURITY_USER_ID == getActivityManagerLocked().getCurrentUserIdLocked();
    }

    public boolean hasSecurityUser() {
        for (int user : mUms.getUserIds()) {
            if (SECURITY_USER_ID == user) {
                return true;
            }
        }
        return false;
    }

    public boolean switchToSecurityContainer() {
        final long oldId = Binder.clearCallingIdentity();
        try {
            switchToSecurityContainer(0);
            return true;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    private void switchToSecurityContainer(int reason) {
        Slog.i(LOG_TAG, "switch to container reason:" + reason + " time:" + System.currentTimeMillis());
        if (!compareCurrentUserId(SECURITY_USER_ID)) {
            if (hasSecurityUser()) {
                mContext.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_SWITCHED));
                Slog.v(LOG_TAG, "register ACTION_USER_SWITCHED");
                try {
                    storeZEROUserStates();
                    //关闭adb
                    Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
                    Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                    Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, 0);
                    //关闭蓝牙
                    setBluetoothEnabled(false);
                    //关闭定位
                    final ContentResolver cr = mContext.getContentResolver();
                    Settings.Secure.putIntForUser(cr, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF, SECURITY_USER_ID);
                
                    PackageManager packageManager = mContext.getPackageManager();  
                    Intent queryIntent = new Intent(Intent.ACTION_MAIN);  
                    queryIntent.addCategory(Intent.CATEGORY_HOME);  
                    List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.MATCH_DISABLED_COMPONENTS);
                    PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
                    
                    if (new LockPatternUtils(mContext).getKeyguardStoredPasswordQuality(SECURITY_USER_ID) <= 0) {
                        //如果锁屏不是安全锁，则强制设置界面
                        for (ResolveInfo ri : resolveInfo) {
                            if (ENCRYPTSPACE_APP_PACKAGENAME.equals(ri.activityInfo.packageName) && "com.bird.encryptspace.activity.FirstPwdSetupActivity".equals(ri.activityInfo.name)) {
                                try {
                                    pms.setComponentEnabledSetting(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP, SECURITY_USER_ID);
                                } catch (Exception e) {
                                }
                            } else {
                                try {
                                    pms.setComponentEnabledSetting(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, SECURITY_USER_ID);
                                } catch (Exception e) {
                                }
                            }
                        }
                    } else {
                        for (ResolveInfo ri : resolveInfo) {
                            if (ENCRYPTSPACE_APP_PACKAGENAME.equals(ri.activityInfo.packageName) && "com.bird.encryptspace.activity.DesktopActivity".equals(ri.activityInfo.name)) {
                                try {
                                    pms.setComponentEnabledSetting(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP, SECURITY_USER_ID);
                                } catch (Exception e) {
                                }
                            } else {
                                try {
                                    pms.setComponentEnabledSetting(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, SECURITY_USER_ID);
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                    
                    getActivityManagerLocked().switchUser(SECURITY_USER_ID);
                    
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Couldn't switch user.", e);
                }
                return;
            }
        }
    }

    public void switchToZEROUserContainer() {
        switchToZEROUserContainer(0);
    }

    void switchToZEROUserContainer(int reason) {
        Slog.i(LOG_TAG, "switch to zerouser reason:" + reason + " time:" + System.currentTimeMillis());
        if (!compareCurrentUserId(0)) {
            try {
                getActivityManagerLocked().switchUser(0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Couldn't switch user.", e);
            }
        }
    }

    public void setData(String key, String data) {
        if (mDbHelper != null) {
            mDbHelper.saveData(key, data);
        }
    }

    public String getData(String key) {
        if (mDbHelper != null) {
            return mDbHelper.getData(key);
        } else {
            return "";
        }
    }

    public boolean isContainerJustRemoved() {
        return mContainerJustRemoved | mUms.isUserRemoving(SECURITY_USER_ID);
    }

    public void resetContainer() {
        
    }
    
    public void destroyContainer() {
        mDbHelper.clearAllData();
        if (isCurrentContainerUser()) {
            new LockPatternUtils(mContext).clearLock(true, SECURITY_USER_ID);
            mDelContainer = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
            filter.addAction(Intent.ACTION_USER_REMOVED);
            mContext.registerReceiver(receiver, filter);
            switchToZEROUserContainer();
            return;
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_USER_REMOVED);
            mContext.registerReceiver(receiver, filter);
            if (!removeDefaultSecurityUser()) {
                throw new IllegalStateException("fail to reset securitycontainer!");
            }
        }
    }

    public void stopContainer() {
        if (isCurrentContainerUser()) {
            mStopContainer = true;
            mContext.registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_SWITCHED));
            switchToZEROUserContainer();
        } else if (mAms.stopUser(SECURITY_USER_ID, false, new IStopUserCallback.Stub() {
            public void userStopped(int userId) {
                Slog.i(LOG_TAG, "user UserInfo.SECURITY_USER_ID Stopped!");
            }

            public void userStopAborted(int userId) {
            }
        }) == 0) {
            Slog.i(LOG_TAG, "StopContainer ActivityManager.USER_OP_SUCCESS");
        }
    }

    public String getContainerPwdType() {
        return getData("password_type_key");
    }

    public boolean isUserRemoving(int userId) {
        return mRemoving;
    }
    
    private void initConfig(Context context, int userId) {
        PackageManagerService pms = (PackageManagerService) ServiceManager.getService("package");
        if (userId == SECURITY_USER_ID) {
            //设置最大出错次数
            setData(KEY_REMAINING_TIMES, "" + MAX_ATTEMPT_COUNT);
            
            ContentResolver contentResolver = context.getContentResolver();
            Settings.System.putIntForUser(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_SCREEN_TIMEOUT, SECURITY_USER_ID);
            //禁止手动安装apk  注释掉是因为：“加密信道”app 要升级
            /*
            try {
                pms.setApplicationEnabledSetting("com.android.packageinstaller", PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId, null);
            } catch (Exception e) {
                Log.e(LOG_TAG, "initConfig ", e);
            }
            */
        } else {
            try {
                pms.setComponentEnabledSetting(new ComponentName(ENCRYPTSPACE_APP_PACKAGENAME, "com.bird.encryptspace.activity.FirstPwdSetupActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId);
                pms.setComponentEnabledSetting(new ComponentName(ENCRYPTSPACE_APP_PACKAGENAME, "com.bird.encryptspace.activity.ContainerActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId);
                pms.setComponentEnabledSetting(new ComponentName(ENCRYPTSPACE_APP_PACKAGENAME, "com.bird.encryptspace.activity.ExitActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId);
                pms.setComponentEnabledSetting(new ComponentName(ENCRYPTSPACE_APP_PACKAGENAME, "com.bird.encryptspace.activity.DesktopActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId);
                
                pms.setApplicationEnabledSetting("com.yuneasy.toec", PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId, context.getPackageName());
                
            } catch (Exception e) {
                Log.e(LOG_TAG, "initConfig ", e);
            }
        }
    }
    
    private boolean compareCurrentUserId(int targetId) {
        int currentUserId = getActivityManagerLocked().getCurrentUserIdLocked();
        Slog.i(LOG_TAG, "currentUserId " + currentUserId);
        return targetId == currentUserId;
    }
    
    public void reportFailedPasswordAttempt() {
        if (!isCurrentContainerUser()) {
            return;
        }
        int times = getPasswordAttemptRemainingTimes();
        times--;
        setData(KEY_REMAINING_TIMES, "" + times);
        
        if (times == 0) {
            destroyContainer();
        } 
    }
    
    public void reportSuccessfulPasswordAttempt() {
        if (!isCurrentContainerUser()) {
            return;
        }
        setData(KEY_REMAINING_TIMES, "" + MAX_ATTEMPT_COUNT);
    }
    
    public int getPasswordAttemptRemainingTimes() {
        String s = getData(KEY_REMAINING_TIMES);
        int times = MAX_ATTEMPT_COUNT;
        if (!TextUtils.isEmpty(s)) {
            try {
                times = Integer.parseInt(s);
            } catch (Exception e) {
                
            }
        }
        return times;
    }
    
    private void storeZEROUserStates() {
        //蓝牙
        setData(KEY_RESTORE_BLUETOOTH, isBluetoothEnabled() ? "1": "0");
        //ADB
        setData(KEY_RESTORE_ADB, "" + Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, 0));
        setData(KEY_RESTORE_DEVELOPMENT_SETTINGS, "" + Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0));
    }
    
    private void restoreZEROUserStates() {
        //蓝牙
        setBluetoothEnabled("1".equals(getData(KEY_RESTORE_BLUETOOTH)));
        //ADB
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, "1".equals(getData(KEY_RESTORE_DEVELOPMENT_SETTINGS)) ? 1 : 0);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.ADB_ENABLED, "1".equals(getData(KEY_RESTORE_ADB)) ? 1 : 0);
    }
    
    private void showToast(String s) {
        Toast toast = Toast.makeText(mContext, s, Toast.LENGTH_SHORT);
        toast.getWindowParams().type = WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL;  
        toast.getWindowParams().privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;  
        toast.getWindowParams().flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        toast.show();
    }
    
    private void setBluetoothEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        if (enabled) {
            BluetoothAdapter.getDefaultAdapter().enable();
        } else {
            BluetoothAdapter.getDefaultAdapter().disable();
        }
    }
    
    public boolean isBluetoothEnabled() {
        // TODO Auto-generated method stub
        final int state = BluetoothAdapter.getDefaultAdapter().getState();
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_ON:
                return true;
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_OFF:
                return false;
            default:
                return false;
        }
    }
}
/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */
