package com.android.internal.telephony;

import android.os.SystemProperties;

public final class FeatureOption {

	/*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]BEGIN */
	public static final boolean BIRD_VOICE_MAIL_NUMBER_FROM_SIM = getValue("ro.bird_voicemail_from_sim");;
	/*[BIRD][BIRD_VOICE_MAIL_NUMBER_FROM_SIM][要求插入不同国家SIM卡时自动读取对应voicemail][yangbo][20150905]END */
    
    //[BIRD][BIRD_DEFAULT_DATA_ON][默认开启数据连接][qianliliang][20160622] BEGIN
    public static final boolean BIRD_DEFAULT_DATA_ON = getValue("ro.bird_default_data_on");
    //[BIRD][BIRD_DEFAULT_DATA_ON][默认开启数据连接][qianliliang][20160622] END
    
    /*[BIRD][BIRD_CLOSE_SIM_LANGUAGE][系统默认语言不随sim卡内置语言改变][hongzhihao][20161026]begin*/
    public static final boolean BIRD_CLOSE_SIM_LANGUAGE = getValue("ro.bd_close_sim_language");
    /*[BIRD][BIRD_CLOSE_SIM_LANGUAGE][系统默认语言不随sim卡内置语言改变][hongzhihao][20161026]end*/
   
    //[BIRD][BIRD_EWC_VETTEL][VETTEL定制销售追踪][zhangaman][20170405]begin
    public static final boolean BIRD_EWC_VETTEL = getValue("ro.bird_ewc_vettel");
    //[BIRD][BIRD_EWC_VETTEL][VETTEL定制销售追踪][zhangaman][20170405]end 
    
    /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]BEGIN */
    public static final boolean BIRD_ZX_SHOUFUBAO_APPS = getValue("ro.bird_zx_shb_apps");
    /*[BIRD][BIRD_ZX_SHOUFUBAO_APPS][中兴守护宝][yangbo][20170803]END */

    //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]begin
    public static final boolean BIRD_GSM_CANNOT_CALLOUT = getValue("ro.bd_gsm_cannot_callout");
    //[BIRD][BIRD_GSM_CANNOT_CALLOUT][Volte,Vowifi特殊号码无法拨出][hongzhihao][20170819]end
    
    //[BIRD][BIRD_VOICE_MESSAGE_CHANGE][语音信箱问题 添加宏控][yangheng][20170616] BEGIN
    public static final boolean BIRD_VOICE_MESSAGE_CHANGE = getValue("ro.bd_voice_message_change");
    //[BIRD][BIRD_VOICE_MESSAGE_CHANGE][语音信箱问题 添加宏控][yangheng][20170616] END
    
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] BEGIN
    public static final boolean BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE = getValue("ro.bird_mulit_simlock_viettel");
    //[BIRD][BIRD_MULTI_SIMLOCK_VIETTEL_IMPROVE][Viettel双卡锁网需求改进][chenguangxiang][20171221] END
    
	private static boolean getValue(String key) {
  	     return SystemProperties.get(key).equals("1");
    }
}
