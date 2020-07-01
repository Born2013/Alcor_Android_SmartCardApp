package com.android.server;

import android.os.SystemProperties;

public final class FeatureOption {

    //[BIRD][BIRD_BAN_INSTALL_THIRD_APP][禁止安装第三方APP，但是手机自带APP能正常升级]
    public static final boolean BIRD_BAN_INSTALL_THIRD_APP = getValue("ro.bd_ban_install_third_app");
    //[BIRD][BIRD_BAN_INSTALL_THIRD_APP][禁止安装第三方APP，但是手机自带APP能正常升级]

    //[BIRD][BIRD_HOOK_NEXT_MUSIC][music播放音乐时长按进入下一首][hongzhihao][20170607]
    public static final boolean BIRD_HOOK_NEXT_MUSIC = getValue("ro.bd_hook_next music");
    //[BIRD][BIRD_HOOK_NEXT_MUSIC][music播放音乐时长按进入下一首][hongzhihao][20170607]

    /*[BIRD][BIRD_DEFAULT_SOUGOU_INPUT_NOTIFICATION_IGNORED][搜狗输入法默认屏蔽通知][zhangaman][20170705]begin*/
    public static final boolean BIRD_DEFAULT_SOUGOU_INPUT_NOTIFICATION_IGNORED = getValue("ro.bd_ignored_sougou");
    /*[BIRD][BIRD_DEFAULT_SOUGOU_INPUT_NOTIFICATION_IGNORED][搜狗输入法默认屏蔽通知][zhangaman][20170705]end*/
    
    /*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
    public static final boolean BIRD_ENCRYPT_SPACE = getValue("ro.bd_encrypt_space");
    /*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */
    
    /*[BIRD][BIRD_DISPLAY_ADD_STAY_ON_OPTION]["设置-显示-休眠"下增加"关闭"(表示屏幕常亮)选项][yangbo][20170724]BEGIN */
    public static final boolean BIRD_DISPLAY_ADD_STAY_ON_OPTION = getValue("ro.bd_display_add_stay_on") ;
    /*[BIRD][BIRD_DISPLAY_ADD_STAY_ON_OPTION]["设置-显示-休眠"下增加"关闭"(表示屏幕常亮)选项][yangbo][20170724]END */
    
	/*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]BEGIN */
    public static final boolean BIRD_ZX_SHOUFUBAO_APPS = getValue("ro.bird_zx_shb_apps");
    /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]END */
	
	//[BIRD][BIRD_ADD_TURBO_BATTERY][添加省電工具Turbo Battery][yangheng][20170812] BEGIN
    public static final boolean BIRD_ADD_TURBO_BATTERY = getValue("ro.bd_add_turbo_battery");
    //[BIRD][BIRD_ADD_TURBO_BATTERY][添加省電工具Turbo Battery][yangheng][20170812] END
    
    /*[BIRD][BIRD_GLOBALACTIONS_ADD_SCREENSHOT][长按电源键弹出的对话框中增加"截屏"][yangbo][20180106]BEGIN */
    public static final boolean BIRD_GLOBALACTIONS_ADD_SCREENSHOT = getValue("ro.bd_actions_add_screenshot");
    /*[BIRD][BIRD_GLOBALACTIONS_ADD_SCREENSHOT][长按电源键弹出的对话框中增加"截屏"][yangbo][20180106]END */
    
    /*[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][锁网特殊需求，详见TASK-6722][yangbo][20180131]BEGIN */
    public static final boolean BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE = getValue("ro.bird_mulit_simlock_viettel");
    /*[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][锁网特殊需求，详见TASK-6722][yangbo][20180131]END */
    
    /*[BIRD][BIRD_MY_MM_GMS_FORCE_ENGLISH][系统语言为缅甸语时，GMS包中应用显示为英文][yangbo][20180211]BEGIN */
    public static final boolean BIRD_MY_MM_GMS_FORCE_ENGLISH = getValue("ro.bd_my_mm_gms_force_english");
    /*[BIRD][BIRD_MY_MM_GMS_FORCE_ENGLISH][系统语言为缅甸语时，GMS包中应用显示为英文][yangbo][20180211]END */

    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
