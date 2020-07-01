/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
package com.bird.security;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageManager.Stub;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.Slog;
import android.content.pm.PackageManager;
import android.text.TextUtils;

public class ContainerManager {
    
    private static String TAG = "ContainerManager";
    private static ContainerManager sInstance = null;
    private final Context mContext;
    private final IContainerManager mService;
    
    public static final int SECURITY_USER_ID = 9;
    
    public static final String KEY_SECRET_NUMBER_KEY = "secret_number_key";

    public static synchronized ContainerManager get(Context context) {
        ContainerManager containerManager;
        synchronized (ContainerManager.class) {
            if (sInstance == null) {
                sInstance = (ContainerManager) context.getSystemService(Context.BIRD_CONTAINER_SERVICE);
            }
            containerManager = sInstance;
        }
        return containerManager;
    }

    public ContainerManager(Context context, IContainerManager service) {
        this.mService = service;
        this.mContext = context;
    }

    public boolean switchToSecurityContainer() {
        try {
            return this.mService.switchToSecurityContainer();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call switchToSecurityContainer", e);
            return false;
        }
    }

    public boolean createSecurityContainer() {
        boolean result = false;
        try {
            result = this.mService.addDefaultSecurityUser();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call createSecurityContainer", e);
        }
        initComponent();
        return result;
    }

    private void initComponent() {
        
    }

    private void enableComponent(ComponentName[] cmpName, int userId, boolean enable) {
        try {
            IPackageManager sPackageManager = Stub.asInterface(ServiceManager.getService("package"));
            for (ComponentName componentEnabledSetting : cmpName) {
                sPackageManager.setComponentEnabledSetting(componentEnabledSetting, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP, userId);
            }
        } catch (Exception e) {
            Log.w(TAG, "enableComponent fail ", e);
        }
    }
    
    public void switchToZEROUserContainer() {
        try {
            this.mService.switchToZEROUserContainer();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call switchToZEROUserContainer", e);
        }
    }

    public boolean isCurrentContainerUser() {
        try {
            return this.mService.isCurrentContainerUser();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call isCurrentContainerUser", e);
            return false;
        }
    }

    public void setData(String key, String data) {
        try {
            this.mService.setData(key, data);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call setData: " + key + " " + data, e);
        }
    }

    public String getData(String key) {
        try {
            return this.mService.getData(key);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call getData: " + key, e);
            return null;
        }
    }
    
    public String getSecretNumberKey() {
        String secret = getData(KEY_SECRET_NUMBER_KEY);
        if (TextUtils.isEmpty(secret)) {
            secret = "#";
        }
        return secret;
    }
    
    public void setSecretNumberKey(String number) {
        setData(KEY_SECRET_NUMBER_KEY, number);
    }

    public boolean isContainerJustRemoved() {
        try {
            return this.mService.isContainerJustRemoved();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call isContainerJustRemoved: ", e);
            return false;
        }
    }

    public void resetContainer() {
        try {
            this.mService.resetContainer();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call resetContainer: ", e);
        }
    }
    
    public void destroyContainer() {
        try {
            this.mService.destroyContainer();
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call destroyContainer: ", e);
        }
    }

    public boolean isUserRemoving(int userId) {
        try {
            return this.mService.isUserRemoving(userId);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not call isUserRemoving: ", e);
            return false;
        }
    }
    
    public void reportFailedPasswordAttempt() {
        try {
            this.mService.reportFailedPasswordAttempt();
        } catch (RemoteException re) {
            
        }
    }
    
    public void reportSuccessfulPasswordAttempt() {
        try {
            this.mService.reportSuccessfulPasswordAttempt();
        } catch (RemoteException re) {
            
        }
    }
    
    public int getPasswordAttemptRemainingTimes() {
        try {
            return this.mService.getPasswordAttemptRemainingTimes();
        } catch (RemoteException re) {
            
        }
        return 0;
    }
}
/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */
