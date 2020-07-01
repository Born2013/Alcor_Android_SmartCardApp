/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
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

package com.android.server;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManager;

import com.android.internal.R;

import java.io.IOException;
/*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]begin*/
import java.io.FileWriter;
import java.io.File;
import android.os.SystemProperties;
/*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]end*/

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_REMOTE_INTENT)) {
            if (!"google.com".equals(intent.getStringExtra("from"))) {
                Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
                return;
            }
        }
        
        /*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]begin*/
        setFactoryReset();
        /*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]end*/
        
        final boolean shutdown = intent.getBooleanExtra("shutdown", false);
        final String reason = intent.getStringExtra(Intent.EXTRA_REASON);
        final boolean wipeExternalStorage = intent.getBooleanExtra(
                Intent.EXTRA_WIPE_EXTERNAL_STORAGE, false);

        Slog.w(TAG, "!!! FACTORY RESET !!!");
        // The reboot call is blocking, so we need to do it on another thread.
        Thread thr = new Thread("Reboot") {
            @Override
            public void run() {
                try {
                    Slog.d(TAG, "Call mtehod: rebootWipeUserData");
                    RecoverySystem.rebootWipeUserData(context, shutdown, reason);
                    Slog.e(TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e) {
                    Slog.e(TAG, "Can't perform master clear/factory reset", e);
                }
            }
        };

        if (wipeExternalStorage) {
            // thr will be started at the end of this task.
            new WipeAdoptableDisksTask(context, thr).execute();
        } else {
            thr.start();
        }
    }

    private class WipeAdoptableDisksTask extends AsyncTask<Void, Void, Void> {
        private final Thread mChainedTask;
        private final Context mContext;
        private final ProgressDialog mProgressDialog;

        public WipeAdoptableDisksTask(Context context, Thread chainedTask) {
            mContext = context;
            mChainedTask = chainedTask;
            mProgressDialog = new ProgressDialog(context);
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mProgressDialog.setMessage(mContext.getText(R.string.progress_erasing));
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Slog.w(TAG, "Wiping adoptable disks");
            StorageManager sm = (StorageManager) mContext.getSystemService(
                    Context.STORAGE_SERVICE);
            sm.wipeAdoptableDisks();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgressDialog.dismiss();
            mChainedTask.start();
        }

    }

    /*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]begin*/
    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171025 begin*/
    private static final boolean BIRD_SYSTEM_APP = SystemProperties.get("ro.bird_system_app").equals("1");
    private static final boolean BIRD_SYSTEM_APP_FOR_DM = SystemProperties.get("ro.bird_system_app_for_dm").equals("1");
    private static final boolean BIRD_SYSTEM_APP_DM_SWITCH = SystemProperties.get("ro.bird_dm_switch").equals("1");
    private static final String FAKE_ROM_PROPERTY = "persist.sys.fake_rom";
    private static final String PERSIST_PRODUCT_PROPERTY = "persist.sys.product.model";
    private static final String PERSIST_BRANCH_PROPERTY = "persist.sys.product.brand";
    private static final String FAKE_RAM_PROPERTY = "persist.sys.fake_ram_value";
    private static final String RAM_RAM_PROPERTY = "persist.sys.storage.custom";
    private static final String BLUETOOTH_HOSTNAME_PROPERTY = "persist.sys.btname";
    private static final String WLAN_SSID_PROPERTY = "persist.sys.wlan.ssid";
    private static final String ADD_FAKE_ROM_PROPERTY = "persist.sys.fake_add_rom";
    private static final String SYS_RAM_MAX_PROPERTY = "persist.sys.tt.ram_max";
    private static final String SYS_RAM_MIN_PROPERTY = "persist.sys.tt.ram_min";
    private static final String SYS_ROM_SYSTEM_PROPERTY = "persist.sys.tt.rom_data";
    private static final String SYS_ROM_INTERNAL_PROPERTY = "persist.sys.tt.rom_sd";
    private static final String CPU_HZ_PROPERTY = "persist.sys.tt.cpu_hz";
    private static final String CPU_HZ_PROPERTY2 = "persist.sys.tt.cpu_hz2";
    private static final String BIRD_SET_CPU_PROPERTY = "persist.sys.set.cpu.info";
    private static final String PERSIST_YUNOS_PROPERTY = "persist.sys.tt.ali";
    private static final String PERSIST_ANDROID_PROPERTY = "persist.sys.tt.android";
    private static final String PERSIST_BUILD_YUNOS_PROPERTY = "persist.sys.tt.build_ali";
    private static final String PERSIST_CPU_MODEL_PROPERTY = "persist.sys.tt.cpu_model";
    private static final String DM_SYS_RAM_MAX_PROPERTY = "persist.sys.dm.ram_max";
    private static final String DM_SYS_RAM_MIN_PROPERTY = "persist.sys.dm.ram_min";
    private static final String DM_SYS_ROM_INTERNAL_PROPERTY = "persist.sys.dm.rom_sd";
    private static final String DM_SYS_SWITCH_PROPERTY = "persist.sys.dm_switch";  
    private static final String DM_PERSIST_CPU_MODEL_PROPERTY = "persist.sys.dm.cpu_model";
    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171025 end*/

    private static final boolean BIRD_SAVE_IMEI_WHEN_FACTORY_RESET = SystemProperties.get("ro.bd_save_imei_when_fac").equals("1");
    private static final boolean MTK_C2K_SUPPORT =
            "1".equals(SystemProperties.get("ro.mtk_c2k_support"));

    private static final String FACTORY_RESET_SEND_BROADCAST_PROPERTY = "persist.sys.send_imei";
    private static final String FACTORY_RESET_IMEI1_PROPERTY = "persist.sys.reset_imei1";
    private static final String FACTORY_RESET_IMEI2_PROPERTY = "persist.sys.reset_imei2";
    private static final String FACTORY_RESET_MEID_PROPERTY = "persist.sys.reset_meid";
    private static final String SET_IMEI1_PROPERTY = "persist.sys.imei1";
    private static final String SET_IMEI2_PROPERTY = "persist.sys.imei2";
    private static final String SET_MEID_PROPERTY = "persist.sys.meid1";


    private void setFactoryReset() {
        FileWriter fw = null;
        try {
            File dir = new File("/protect_f/customer/");
            if(!dir.exists()) {
                boolean flag = dir.mkdir();
                Log.d(TAG, "flag = " + flag);
            }
            File file = new File("/protect_f/customer/customer.prop");
            fw = new FileWriter(file,false);

            /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171025 begin*/
            if (BIRD_SYSTEM_APP) {
                fw.write(SYS_RAM_MAX_PROPERTY + "="+ SystemProperties.get(SYS_RAM_MAX_PROPERTY, "") + "\n");
                fw.write(SYS_RAM_MIN_PROPERTY + "="+ SystemProperties.get(SYS_RAM_MIN_PROPERTY, "") + "\n");
                fw.write(SYS_ROM_SYSTEM_PROPERTY + "="+ SystemProperties.get(SYS_ROM_SYSTEM_PROPERTY, "") + "\n");
                fw.write(SYS_ROM_INTERNAL_PROPERTY + "="+ SystemProperties.get(SYS_ROM_INTERNAL_PROPERTY, "") + "\n");
                //fw.write(BIRD_SET_CPU_PROPERTY + "="+ SystemProperties.get(BIRD_SET_CPU_PROPERTY, "") + "\n");
                fw.write(PERSIST_PRODUCT_PROPERTY + "="+ SystemProperties.get(PERSIST_PRODUCT_PROPERTY, "") + "\n");
                fw.write(PERSIST_BRANCH_PROPERTY + "="+ SystemProperties.get(PERSIST_BRANCH_PROPERTY, "") + "\n");
                fw.write(PERSIST_CPU_MODEL_PROPERTY + "="+ SystemProperties.get(PERSIST_CPU_MODEL_PROPERTY, "") + "\n");
            }
            if (BIRD_SAVE_IMEI_WHEN_FACTORY_RESET) {
                fw.write(FACTORY_RESET_SEND_BROADCAST_PROPERTY + "=" + "1" + "\n");
                fw.write(FACTORY_RESET_IMEI1_PROPERTY + "=" + "1" + "\n");
                fw.write(FACTORY_RESET_IMEI2_PROPERTY + "=" + "1" + "\n");
                fw.write(FACTORY_RESET_MEID_PROPERTY + "=" + "1" + "\n");
                fw.write(SET_IMEI1_PROPERTY + "="+ SystemProperties.get(SET_IMEI1_PROPERTY, "") + "\n");
                fw.write(SET_IMEI2_PROPERTY + "="+ SystemProperties.get(SET_IMEI2_PROPERTY, "") + "\n");
                if (MTK_C2K_SUPPORT) {
                    fw.write(SET_MEID_PROPERTY + "="+ SystemProperties.get(SET_MEID_PROPERTY, "") + "\n");
                }
            }
            if (BIRD_SYSTEM_APP_FOR_DM) {
                fw.write(PERSIST_YUNOS_PROPERTY + "="+ SystemProperties.get(PERSIST_YUNOS_PROPERTY, "") + "\n");
                fw.write(PERSIST_ANDROID_PROPERTY + "="+ SystemProperties.get(PERSIST_ANDROID_PROPERTY, "") + "\n");
                fw.write(PERSIST_BUILD_YUNOS_PROPERTY + "="+ SystemProperties.get(PERSIST_BUILD_YUNOS_PROPERTY, "") + "\n");
                

                if (BIRD_SYSTEM_APP_DM_SWITCH) {
                    fw.write(DM_SYS_SWITCH_PROPERTY + "="+ SystemProperties.get(DM_SYS_SWITCH_PROPERTY, "") + "\n");
                    fw.write(DM_SYS_ROM_INTERNAL_PROPERTY + "="+ SystemProperties.get(DM_SYS_ROM_INTERNAL_PROPERTY, "") + "\n");
                    fw.write(DM_SYS_RAM_MIN_PROPERTY + "="+ SystemProperties.get(DM_SYS_RAM_MIN_PROPERTY, "") + "\n");
                    fw.write(DM_SYS_RAM_MAX_PROPERTY + "="+ SystemProperties.get(DM_SYS_RAM_MAX_PROPERTY, "") + "\n");
                    fw.write(DM_PERSIST_CPU_MODEL_PROPERTY + "="+ SystemProperties.get(DM_PERSIST_CPU_MODEL_PROPERTY, "") + "\n");
                }
            }
            /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171026 begin*/
            
            /*[BIRD][BIRD_SYSTEM_APP_SET_LCD]chengshujiang 20160624 begin*/
            String lcdValue = SystemProperties.get("persist.sys.tt.lcd_setdef");
            if (lcdValue != null && lcdValue.length() > 2) {
                fw.write("persist.sys.tt.lcd_setdef" + "="+ SystemProperties.get("persist.sys.tt.lcd_setdef", "") + "\n");
                fw.write("persist.sys.tt.lcd_set" + "="+ SystemProperties.get("persist.sys.tt.lcd_set", "") + "\n");
            }
            /*[BIRD][BIRD_SYSTEM_APP_SET_LCD]chengshujiang 20160624 end*/
            /*[BIRD][BIRD_CAMERA_PIXEL_FRAUD][相机差值作假]huangzhangbin 20180306 begin*/
            String cameraValue = SystemProperties.get("persist.sys.tt.camera_set");
            if (cameraValue != null && cameraValue.length() > 0) {
                fw.write("persist.sys.tt.camera_setdef" + "="+ SystemProperties.get("persist.sys.tt.camera_setdef", "") + "\n");
                fw.write("persist.sys.tt.camera_set" + "="+ SystemProperties.get("persist.sys.tt.camera_set", "") + "\n");
            }
            /*[BIRD][BIRD_CAMERA_PIXEL_FRAUD][相机差值作假]huangzhangbin 20180306 end*/
            fw.write("BIRD_IS_FACTORY_RESET=yes" + "\n");
            fw.flush();
        } catch (Exception e) {
        
        } finally {
            try {
                if(fw != null) {
                    fw.close();
                }
            }catch (Exception e1) {
                
            }
        }
        SystemProperties.set("persist.sys.is_first_boot","1");
    }
    /*[BIRD][BIRD_SAVE_IMEI_WHEN_FACTORY_RESET][手动写码恢复出厂设置也要保留][zhangaman][20170913]end*/
}
