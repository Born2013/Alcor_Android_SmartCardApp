package com.mediatek.settingslib;

import android.os.SystemProperties;

public class FeatureOption {
    /// M: Add for MTK new feature DUN profile. @ {
    public static final boolean MTK_Bluetooth_DUN = getValue("bt.profiles.dun.enabled");
    /// @ }
    
    /*[BIRD][BIRD_MY_MM_GMS_FORCE_ENGLISH][系统语言为缅甸语时，"设置-日期和时间-选择时区"进入后显示为英文][yangbo][20180226]BEGIN */
    public static final boolean BIRD_MY_MM_GMS_FORCE_ENGLISH = getValue("ro.bd_my_mm_gms_force_english");
    /*[BIRD][BIRD_MY_MM_GMS_FORCE_ENGLISH][系统语言为缅甸语时，"设置-日期和时间-选择时区"进入后显示为英文][yangbo][20180226]END */
    

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
