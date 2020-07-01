
package com.bird.trafficmgr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import android.os.ServiceManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;
import android.net.Uri;
import android.os.AsyncTask;
import android.database.Cursor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import java.util.HashMap;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
/**
 * Created by caoyuangui on 12/26/15.
 */
public class MyFireWallService extends Service {
    private static final String TAG = "MyFireWallService - gaowei";
    private static final String MY_FIREWALL_PREFERENCES = "myfirewall";
    private static final boolean DEBUG = true;//isBuildEng();
    private static final int WIFI_CHAIN = 1;
    private static final int  MOBILE_CHAIN = 0 ;

    private SharedPreferences myPrefsFireWall;
    private SharedPreferences.Editor myPrefsEditorFireWall;
    private Context mContext;
    private QueryDataAsyncTask mQueryDataAsyncTask;
    private ServiceBinder mBinder = new ServiceBinder();
    private INetworkManagementService mNetworkService;

    private ApplicationInfo appInfo;
    public static final Uri mUri =
            Uri.parse("content://com.xycm.schoolbased.AppContentProvider/apps");
    public static final String COL_PACKAGE_NAME = "pkgname";
    public static final String COL_STATE = "state";
    public static final String[] COL_S = {
            COL_PACKAGE_NAME,
            COL_STATE
    };
    public static final int LOCK = 1;
    public static final int UN_LOCK = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG)
            Log.d(TAG,"onBind ==========?");
        return mBinder;
    }

    /**
     * class use to return service instance
     */
    public class ServiceBinder extends Binder {
        /**
         * get MyFireWallService service instance
         * @return service instance
         */
        MyFireWallService getService() {
            return MyFireWallService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG)
            Log.d(TAG,"onStartCommand ==========begin");

        clearFireWall();
        
        if (mQueryDataAsyncTask != null) {
            mQueryDataAsyncTask.cancel(true);
            mQueryDataAsyncTask = null;
        }
        if (mQueryDataAsyncTask == null) {
            mQueryDataAsyncTask = new QueryDataAsyncTask();
        }
        mQueryDataAsyncTask.execute();

        Log.d(TAG,"onStartCommand  ====end ======");
        return super.onStartCommand(intent, Service.START_REDELIVER_INTENT, startId);
        
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG)
            Log.d(TAG,"onCreate ==========begin"); 
        mContext  = this;
        myPrefsFireWall = mContext.getSharedPreferences(MY_FIREWALL_PREFERENCES, MODE_PRIVATE);
        myPrefsEditorFireWall = myPrefsFireWall.edit();
        if (mNetworkService == null) {
            mNetworkService = INetworkManagementService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        }
        Log.d(TAG,"onCreate ==========end");        
    }

    //查询黑名单
    public void quertBlackApp() {
        Cursor cursor = android.app.ActivityThread.currentApplication().getApplicationContext().getContentResolver().query(mUri, COL_S, null, null, null);
        Log.d(TAG, "quertBlackApp cursor = "+cursor);
        if (cursor != null) {
            int packagenameIndex = cursor.getColumnIndex(COL_PACKAGE_NAME);
            int stateIndex = cursor.getColumnIndex(COL_STATE);
            Log.d(TAG, "quertBlackApp packagenameIndex = "+packagenameIndex);
            Log.d(TAG, "quertBlackApp stateIndex = "+stateIndex);
            while (cursor.moveToNext()) {
                int state = cursor.getInt(stateIndex);
                String packageName = cursor.getString(packagenameIndex);
                Log.d(TAG, "quertBlackApp state = "+state);
                Log.d(TAG, "quertBlackApp packageName = "+packageName);
                int uID = getPackageUid(packageName);
                if (uID != -1) {
                    if (state == LOCK) {
                        if ("com.xycm.schoolbased".equals(packageName)) {
                            myPrefsEditorFireWall.putInt(packageName+":"+uID,UN_LOCK);
                            myPrefsEditorFireWall.commit();
                        } else {
                            myPrefsEditorFireWall.putInt(packageName+":"+uID,LOCK);
                            myPrefsEditorFireWall.commit();
                        }
                    } else if (state == UN_LOCK) {
                        myPrefsEditorFireWall.putInt(packageName+":"+uID,UN_LOCK);
                        myPrefsEditorFireWall.commit();
                    }
                }
            }
        }
    }

    //查询黑白名单
    public class QueryDataAsyncTask extends AsyncTask<String, String, Void> {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }
        @Override
        protected Void doInBackground(String... params)
        {
            quertBlackApp();
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG,"QueryDataAsyncTask   onPostExecute!!!!");
            setFireWall();
        }
    }

    //打开防火墙
    private void setFireWall() {
        Log.d(TAG,"setFireWall   begin!!!!");
        if (myPrefsFireWall == null) {
            myPrefsFireWall = mContext.getSharedPreferences(MY_FIREWALL_PREFERENCES, MODE_PRIVATE);
        }
        Map<String,?> myFireWallMap = myPrefsFireWall.getAll();
        if (myFireWallMap == null || myFireWallMap.isEmpty()) {
            if (DEBUG)
                Log.d(TAG,"handleMessage the iptables is empley");
            return ;
        }
        Set <String> keyset = myFireWallMap.keySet();
        Iterator<String> iterator =  keyset.iterator();
        if (iterator == null ) {
            if (DEBUG)
                Log.d(TAG,"handleMessage the iptables is empley twice");
            return ;
        }
        while (iterator.hasNext()) {
            String key = iterator.next();
            String [] args = key.split(":");
            String packageName = args[0];
            int uID = Integer.valueOf(args[1]);
            int value = myPrefsFireWall.getInt(key,-1);
            if (value == LOCK) {
                if (DEBUG) {
                    Log.d(TAG,"setFireWall LOCK mNetworkService = "+mNetworkService);
                    Log.d(TAG,"setFireWall LOCK packageName = "+packageName+",UID = "+uID);
                }
                if (DEBUG)
                    Log.d(TAG,"mNetworkService = "+mNetworkService);

                if (mNetworkService != null) {
                    try {
                        mNetworkService.setFirewallUidChainRule(uID,WIFI_CHAIN,true);
                        mNetworkService.setFirewallUidChainRule(uID,MOBILE_CHAIN,true);
                    } catch (RemoteException e) {
                        if (DEBUG)
                            Log.d(TAG+"yeyunfeng0000", " e = "+e);
                    } catch (Exception e){
                        if (DEBUG)
                            Log.d(TAG+"here ERROR:", " e = "+e);
                    }
                }

            } else if (value == UN_LOCK) {
                if (DEBUG)
                    Log.d(TAG,"setFireWall UN_LOCK packageName = "+packageName+",UID = "+uID);

                // if (DEBUG)
                //     Log.d(TAG,"mNetworkService = "+mNetworkService);

                // if (mNetworkService != null) {
                //     try {
                //         mNetworkService.setFirewallUidChainRule(uID,WIFI_CHAIN,false);
                //         mNetworkService.setFirewallUidChainRule(uID,MOBILE_CHAIN,false);
                //     } catch (RemoteException e) {
                //         if (DEBUG)
                //             Log.d(TAG+"yeyunfeng0000", " e = "+e);
                //     } catch (Exception e){
                //         if (DEBUG)
                //             Log.d(TAG+"here ERROR:", " e = "+e);
                //     }
                // }
                
            } else {
                if (DEBUG)
                    Log.d(TAG, "here ERROR !!! packageName = "+packageName);
            }
            
        }
        Log.d(TAG,"setFireWall   end!!!!");
    }

    //关闭防火墙
    private void clearFireWall() {
        if (DEBUG)
            Log.d(TAG,"clearFireWall   begin!!!!");

        if (DEBUG)
            Log.d(TAG,"mNetworkService = "+mNetworkService);

        if (mNetworkService != null) {
            try {
                mNetworkService.clearFirewallChain("wifi");
                mNetworkService.clearFirewallChain("mobile");
                // mNetworkService.clearFirewallChain("fw_INPUT");
                // mNetworkService.clearFirewallChain("fw_OUTPUT");
            } catch (RemoteException e) {
                if (DEBUG)
                    Log.d(TAG+"yeyunfeng0000", " e = "+e);
            }
        }
        if (DEBUG)
            Log.d(TAG,"clearFireWall   end!!!!");
    }

    //获取应用UID
    public int getPackageUid(String packageName) {
        try {
            ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (applicationInfo != null) {
                Log.d(TAG,"applicationInfo.uid = "+applicationInfo.uid);
                return applicationInfo.uid;
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }
}
