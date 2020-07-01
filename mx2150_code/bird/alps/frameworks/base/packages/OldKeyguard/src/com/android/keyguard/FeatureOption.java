
package com.android.keyguard;


import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.provider.Settings;


public class FeatureOption {

    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]begin*/
    public final static boolean BIRD_KUSAI_KEYGUARD = getValue("ro.bd_kusai_keyguard");
    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]end*/

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }

   public static boolean isOldPhoneUI (Context context) {
            return getValue("ro.bird_old_phone") && Settings.System.getInt(context.getContentResolver(), "is_oldphoneluancher_mode", 0)==1;
   }

}
