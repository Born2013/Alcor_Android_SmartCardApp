package com.android.keyguard;

import android.os.SystemProperties;

public class FeatureOptions {

    /*[BIRD][BIRD_FREEZE_PHONE_WHEN_SLOT_PERMANENTLY_LOCKED][当卡槽被完全锁死(锁网剩余次数为0)时，手机将无法使用][yangbo][20170506]BEGIN */
    public static final String BIRD_PREF_PHONE_FREEZE = "bird_phone_freeze";
    public static final boolean BIRD_FREEZE_PHONE_WHEN_SLOT_PERMANENTLY_LOCKED = getValue("ro.bd_freeze_phone_support");
    /*[BIRD][BIRD_FREEZE_PHONE_WHEN_SLOT_PERMANENTLY_LOCKED][当卡槽被完全锁死(锁网剩余次数为0)时，手机将无法使用][yangbo][20170506]END */

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }

}
