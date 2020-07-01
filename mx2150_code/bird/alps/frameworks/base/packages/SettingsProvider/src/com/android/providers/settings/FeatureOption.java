package com.android.providers.settings;

import android.os.SystemProperties;

public class FeatureOption {
	//[BIRD][BIRD_SHOW_BATTERY_PERCENT][状态栏上显示电池电量百分比][dingjiayuan][20160519]begin
	public static final boolean BIRD_SHOW_BATTERY_PERCENT = getValue("ro.bird_show_battery_percent");
	public static final boolean BIRD_SHOW_BATTERY_PERCENT_ON = getValue("ro.bird_show_bp_value");
	//[BIRD][BIRD_SHOW_BATTERY_PERCENT][状态栏上显示电池电量百分比][dingjiayuan][20160519]end
    
    //[BIRD][BIRD_UNKNOWN_SOURCE_ENABLE_DEFAULT][默认打开 设置->安全->未知来源][luye][20160518] BEGIN
    public static final boolean BIRD_UNKNOWN_SOURCE_ENABLE_DEFAULT = getValue("ro.bird_unknown_src_default");
    //[BIRD][BIRD_UNKNOWN_SOURCE_ENABLE_DEFAULT][默认打开 设置->安全->未知来源][luye][20160518] END

	//[BIRD][BIRD_WIFI_DEFAULT_ON][Wifi默认开启][pangmeizhou][20160603]begin
	public static final boolean BIRD_WIFI_DEFAULT_ON = getValue("ro.bd_wifi_default_on");
	//[BIRD][BIRD_WIFI_DEFAULT_ON][Wifi默认开启][pangmeizhou][20160603]end
	/*[BIRD][BIRD_DIRECT_WITH_PROXIMITY][智能体感][yangbo][20150605]BEGIN */
	public static final boolean BIRD_DIRECT_WITH_PROXIMITY = getValue("ro.bird_direct_with_proximity");
    public static final boolean BIRD_DIRECT_TURN_ON_DEFAULT_VALUE = getValue("ro.b_direct_turn_on_def");
    public static final boolean BIRD_SMS_DIRECT_SEND_CALL_DEFAULT_VALUE = getValue("ro.b_sms_direct_call_def");
    public static final boolean BIRD_CONTACT_DIRECT_SEND_CALL_DEFAULT_VALUE = getValue("ro.b_contact_direct_call_def");
    public static final boolean BIRD_DIRECT_ANSWER_CALL_DEFAULT_VALUE = getValue("ro.b_direct_answer_call_def");
    public static final boolean BIRD_DIRECT_ANSWER_CALL_WITH_SPEAKER_ON_DEFAULT_VALUE = getValue("ro.b_direct_speaker_on_def");
    public static final boolean BIRD_DIRECT_PHONE_SILENT_DEFAULT_VALUE = getValue("ro.b_direct_phone_silent_def");
    public static final boolean BIRD_DIRECT_LAUNCHER_PAGE_TURNING_DEFAULT_VALUE = getValue("ro.b_direct_page_turning_def");
    public static final boolean BIRD_DIRECT_PHOTO_SWITCH_DEFAULT_VALUE = getValue("ro.b_direct_photo_switch_def");
    public static final boolean BIRD_DIRECT_UNLOCK_DEFAULT_VALUE = getValue("ro.b_direct_unlock_def");
    public static final boolean BIRD_DIRECT_MUSIC_NEXT_SONG_DEFAULT_VALUE = getValue("ro.b_direct_next_song_def");
    public static final boolean BIRD_DIRECT_CAMERA_DEFAULT_VALUE = getValue("ro.b_direct_camera_def");
    /*[BIRD][BIRD_DIRECT_WITH_PROXIMITY][智能体感][yangbo][20150605]END */
    //[BIRD][BIRD_TWENTY_FOUR_HOURS_DEFAULT][默认使用24小时格式][yangheng][20161117] BEGIN
    public static final boolean BIRD_TWENTY_FOUR_HOURS_DEFAULT = getValue("ro.bird_twenty_four_default");
    //[BIRD][BIRD_TWENTY_FOUR_HOURS_DEFAULT][默认使用24小时格式][yangheng][20161117] END

    //[BIRD][BIRD_CUSTOM_SYSTEM_SCREEN_LIGHT][自定义屏幕亮度][yangheng][20161119] BEGIN
	public static final boolean BIRD_CUSTOM_SYSTEM_SCREEN_LIGHT = getValue("ro.bd_custom_screen_light");
	public static final int BIRD_CUSTOM_SYSTEM_SCREEN_LIGHT_VALUE = getIntValue("ro.bd_custom_screen_light_val");
	//[BIRD][BIRD_CUSTOM_SYSTEM_SCREEN_LIGHT][自定义屏幕亮度][yangheng][20161119] END

    //[BIRD][BIRD_SMART_WAKE_UP_SCREEN][冷屏解锁][dengyang][20160526] BEGIN
    public static final boolean BIRD_SMART_WAKE_UP_SCREEN = SystemProperties.get("ro.bird_wake_up_switch").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP = SystemProperties.get("ro.bird_wake_ctp").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_GLOVE = SystemProperties.get("ro.bird_wake_glove").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_O = SystemProperties.get("ro.bird_wake_o").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_M = SystemProperties.get("ro.bird_wake_m").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_C = SystemProperties.get("ro.bird_wake_c").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_E = SystemProperties.get("ro.bird_wake_e").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_Z = SystemProperties.get("ro.bird_wake_z").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_S = SystemProperties.get("ro.bird_wake_s").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_W = SystemProperties.get("ro.bird_wake_w").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_DOUBLE = SystemProperties.get("ro.bird_wake_double").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_UP = SystemProperties.get("ro.bird_wake_up").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_DOWN = SystemProperties.get("ro.bird_wake_down").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_RIGHT = SystemProperties.get("ro.bird_wake_right").equals("1");
    public static final boolean BIRD_SMART_WAKE_CTP_LEFT = SystemProperties.get("ro.bird_wake_left").equals("1");
    
	//[BIRD][BIRD_SMART_WAKE_UP_SCREEN][冷屏解锁][dengyang][20160526] END
	//[BIRD][BIRD_SMART_WAKE_MISOPERATION_PREVENT][智能唤醒防误触功能][dengyang][20160526]BEGIN
	public static final boolean BIRD_SMART_WAKE_MISOPERATION_PREVENT = SystemProperties.get("ro.bird_wake_misopera_prevent").equals("1");
	public static final boolean BIRD_SMART_WAKE_CTP_MP = SystemProperties.get("ro.bird_wake_ctp_mp").equals("1");
	//[BIRD][BIRD_SMART_WAKE_MISOPERATION_PREVENT][智能唤醒防误触功能][dengyang][20160526]END

    //[BIRD][BIRD_DOUBLE_TAP_HOME_SLEEP][双击home键灭屏功能][qianliliang][20151012] BEGIN
    public static final boolean BIRD_DOUBLE_TAP_HOME_SLEEP = SystemProperties.get("ro.bird_double_tap_home_sleep").equals("1");
    //[BIRD][BIRD_DOUBLE_TAP_HOME_SLEEP][双击home键灭屏功能][qianliliang][20151012] END

    //[/*[BIRD_VIBRATE_WHEN_RINGING_DEFAULT][有来电时响铃并振动默认开启][hongzhihao][201601028]*/ BEGIN
    public static final boolean BIRD_VIBRATE_WHEN_RINGING_DEFAULT = SystemProperties.get("ro.bird_vibrate_when_ringing").equals("1");
    ///*[BIRD_VIBRATE_WHEN_RINGING_DEFAULT][有来电时响铃并振动默认开启][hongzhihao][201601028]*/ END

    //[BIRD][BIRD_DEFAULE_DISPLAY_SLEEP][自定义默认屏幕休眠时间][yangheng][20170313] BEGIN
    public static final boolean BIRD_DEFAULE_DISPLAY_SLEEP = getValue("ro.bd_def_display_sleep");
    public static final int BIRD_DEFAULE_DISPLAY_SLEEP_VALUE = SystemProperties.getInt("ro.bd_def_display_sleep_val", 60000);
    //[BIRD][BIRD_DEFAULE_DISPLAY_SLEEP][自定义默认屏幕休眠时间][yangheng][20170313] END

    //[BIRD][BIRD_DIAL_PAD_TONES_OFF][拨号键盘提示音默认关闭][yangheng][20170321] BEGIN
    public static final boolean BIRD_DIAL_PAD_TONES_OFF = getValue("ro.bd_dial_pad_tones_off");
    //[BIRD][BIRD_DIAL_PAD_TONES_OFF][拨号键盘提示音默认关闭][yangheng][20170321] END

    //[BIRD][BIRD_TOUCH_SOUNDS_OFF][触摸提示音默认关闭][yangheng][20170321] BEGIN
    public static final boolean BIRD_TOUCH_SOUNDS_OFF = getValue("ro.bd_touch_sounds_off");
    //[BIRD][BIRD_TOUCH_SOUNDS_OFF][触摸提示音默认关闭][yangheng][20170321] END

    //[BIRD][BIRD_VIBRATE_ON_TOUCH_OFF][点按时振动默认关闭][yangheng][20170321] BEGIN
    public static final boolean BIRD_VIBRATE_ON_TOUCH_OFF = getValue("ro.bd_vibrate_on_touch_off");
    //[BIRD][BIRD_VIBRATE_ON_TOUCH_OFF][点按时振动默认关闭][yangheng][20170321] END

    //[BIRD][BIRD_SCREEN_LOCKING_SOUNDS_OFF][屏幕锁定提示音默认关闭][yangheng][20170321] BEGIN
    public static final boolean BIRD_SCREEN_LOCKING_SOUNDS_OFF = getValue("ro.bd_screen_lock_sounds_off");
    //[BIRD][BIRD_SCREEN_LOCKING_SOUNDS_OFF][屏幕锁定提示音默认关闭][yangheng][20170321] END
    /*[BIRD][BIRD_RECENTS_APP_BLUR][模糊显示最近任务列表的应用][zhangaman][20170620]begin*/
    public final static boolean BIRD_RECENTS_APP_BLUR = getValue("ro.bd_recents_blur");
    public final static boolean BIRD_RECENTS_APP_BLUR_DEFAULT_VALUE = getValue("ro.bd_recents_blur_def");
    /*[BIRD][BIRD_RECENTS_APP_BLUR][模糊显示最近任务列表的应用][zhangaman][20170620]end*/
    
    //[BIRD][BIRD_DATA_ROAMING_ENABLE_DEFAULT][默认勾选DATA ROAMING][xuze][20150909] BEGIN
	public static final boolean BIRD_DATA_ROAMING_ENABLE_DEFAULT = SystemProperties.get("ro.bird_roaming_enable_default").equals("1");
	//[BIRD][BIRD_DATA_ROAMING_ENABLE_DEFAULT][默认勾选DATA ROAMING][xuze][20150909] END
    /*[BIRD][BIRD_CALL_AUTO_RECORD_DEF][通话自动录音按钮默认关闭][luye][20160413] BEGIN*/
    public static final boolean BIRD_CALL_AUTO_RECORD_DEF = SystemProperties.get("ro.bd_call_auto_record_def").equals("1");
    /*[BIRD][BIRD_CALL_AUTO_RECORD_DEF][通话自动录音按钮默认关闭][luye][20160413] END*/
    private static int getIntValue(String key) {
    	return SystemProperties.getInt(key, 255);
   	}

    /* get the key's value*/
    private static boolean getValue(String key) {
  	     return SystemProperties.get(key).equals("1");
    }

}

