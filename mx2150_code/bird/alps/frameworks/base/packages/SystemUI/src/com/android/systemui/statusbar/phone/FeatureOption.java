package com.android.systemui.statusbar.phone;

import android.os.SystemProperties;

public class FeatureOption {
	
	//[BIRD][BIRD_SHOW_BATTERY_PERCENT][状态栏上显示电池电量百分比][dingjiayuan][20160519]begin
	public static final boolean BIRD_SHOW_BATTERY_PERCENT = getValue("ro.bird_show_battery_percent");
	//[BIRD][BIRD_SHOW_BATTERY_PERCENT][状态栏上显示电池电量百分比][dingjiayuan][20160519]end

    //[BIRD][BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS][自动调节亮度可配置显示/隐藏][dingjiayuan][20160520]begin
	public static final boolean BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS = getValue("ro.bird_remove_auto_brightness");
	//[BIRD][BIRD_REMOVE_AUTO_ADJUST_BRIGHTNESS][自动调节亮度可配置显示/隐藏][dingjiayuan][20160520]end

    //[BIRD_ONBACKPRESS_TAKE_PHOTO][在锁屏亮屏界面长按back打开相机并拍照][hongzhihao][20170613]begin
	public static final boolean BIRD_ONBACKPRESS_TAKE_PHOTO = getValue("ro.bird_back_take_photo");
	//[BIRD_ONBACKPRESS_TAKE_PHOTO][在锁屏亮屏界面长按back打开相机并拍照][hongzhihao][20170613]end
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
