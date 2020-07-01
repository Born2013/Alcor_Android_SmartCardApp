
package com.android.keyguard;


import android.os.SystemProperties;
import android.telephony.TelephonyManager;

public class FeatureOption {
    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]begin*/
    public final static boolean BIRD_KUSAI_KEYGUARD = getValue("ro.bd_kusai_keyguard");
    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]end*/

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
