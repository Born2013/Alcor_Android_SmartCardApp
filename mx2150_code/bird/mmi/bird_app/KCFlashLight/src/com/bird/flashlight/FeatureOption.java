package com.bird.flashlight;

import android.os.SystemProperties;

/**
 * M: Control of Site navigation
 */
public class FeatureOption {
	public static final boolean BIRD_FLASHLIGHT_SOS_ON = getValue("ro.bird_flashlight_sos_on");
	//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]begin
	public static final boolean BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT = getValue("ro.bd_flash_no_exit");
	//[BIRD][BIRD_CLOSE_FLASHLIGHT_WITHOUT_EXIT][IUIFlashLight关闭手电筒不退出][chenguangxiang][20170323]end
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
