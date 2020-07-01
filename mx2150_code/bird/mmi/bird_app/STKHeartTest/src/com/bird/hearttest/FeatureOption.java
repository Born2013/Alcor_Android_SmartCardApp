package com.sensortek.stkhealthcare2;

import android.os.SystemProperties;

/**
* modify caoyuangui
*/
public class FeatureOption {

    public static final boolean KC_MTK_THEMEMANAGER_APP = getValue("ro.bd_common_mk_kusai");
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }

}
