
package com.android.systemui;

import android.os.SystemProperties;
import android.telephony.TelephonyManager;

/*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
/*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 END*/

public class FeatureOption {

    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] BEGIN
    public static final boolean BIRD_SIM_ICON_COLORFUL = getValue("ro.bird_sim_icon_colorful");
    //[BIRD][BIRD_SIM_ICON_COLORFUL][状态栏SIM卡图标着色][qianliliang][20160519] END

    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] BEGIN
    public static final boolean BIRD_DATA_IN_OUT_ACTIVITY = getValue("ro.bd_data_inout_activity");
    //[BIRD][BIRD_DATA_IN_OUT_ACTIVITY][信号栏添加上下行标识][yangheng][20161123] END

    /*[BIRD][BIRD_DIRECT_WITH_PROXIMITY][智能体感][yangbo][20150605]BEGIN */
    public static final boolean BIRD_DIRECT_WITH_PROXIMITY = getValue("ro.bird_direct_with_proximity");
    /*[BIRD][BIRD_DIRECT_WITH_PROXIMITY][智能体感][yangbo][20150605]END */
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] BEGIN
    public static final boolean BIRD_POWER_MANAGER_V2 = getValue("ro.bird_power_manager_v2");
    //[BIRD][BIRD_POWER_MANAGER_V2][省电管理V2.0][qianliliang][20160527] END

    //[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]BEGIN
    public static final boolean BIRD_IUI_FLASHLIGHT_SUPPORT = getValue("ro.bd_iui_flashlight");
    //[BIRD][BIRD_IUI_FLASHLIGHT_SUPPORT][IUI手电筒][luye][20160527]END

    //[BIRD][BIRD_REMOVE_NETWORK_ICON][去除信号图标上的网络类型图标(G/3G/4G)][pangmeizhou][20160603]begin
    public final static boolean BIRD_REMOVE_NETWORK_ICON = getValue("ro.bd_rm_network_icon");
    //[BIRD][BIRD_REMOVE_NETWORK_ICON][去除信号图标上的网络类型图标(G/3G/4G)][pangmeizhou][20160603]end
    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] BEGIN
    public static final boolean BIRD_SUPER_SCREENSHOT_SUPPORT = getValue("ro.bd_super_screenshot_spt");
    //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][qianliliang][20160903] END
    /*[BIRD][BIRD_SMALL_VIEW_WINDOW][深圳版皮套功能][pangmeizhou][20160219]BEGIN */
    public final static boolean BIRD_SMALL_VIEW_WINDOW = getValue("ro.bd_hall_leather");
    /*[BIRD][BIRD_SMALL_VIEW_WINDOW][深圳版皮套功能][pangmeizhou][20160219]END */
    /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]begin */
    public final static boolean BIRD_CUSTOMED_KEYGUARD = getValue("ro.bd_customed_keyguard");
    /*[BIRD][BIRD_CUSTOMED_KEYGUARD][客制化锁屏][zhangaman][20170222]end */

    //[BIRD][BIRD_SEATEL_4G_NOTIFICATION][Seatel运营商卡处于4G状态时通知栏中显示图片，其他运营商不显示][yangheng][20170228] BEGIN
    public static final boolean BIRD_SEATEL_4G_NOTIFICATION = getValue("ro.bd_seatel_notifi");
    //[BIRD][BIRD_SEATEL_4G_NOTIFICATION][Seatel运营商卡处于4G状态时通知栏中显示图片，其他运营商不显示][yangheng][20170228] END
    /*[BIRD][BIRD_FINGER_PRINT_HINT][录入指纹后，锁屏界面提示"按下主屏幕以解锁"][zhangaman][20170310]begin*/
    public static final boolean BIRD_FINGER_PRINT_HINT = getValue("ro.bd_finger_hint");
    /*[BIRD][BIRD_FINGER_PRINT_HINT][录入指纹后，锁屏界面提示"按下主屏幕以解锁"][zhangaman][20170310]end*/
    /*[BIRD][BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM][状态栏电量图标客制化][zhangaman][20170316]begin*/
    public static final boolean BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM = getValue("ro.bd_battery_icon_cus");
    /*[BIRD][BIRD_STATUS_BAR_BATTERY_ICON_CUSTOM][状态栏电量图标客制化][zhangaman][20170316]end*/
    //[BIRD][BIRD_REMOVE_SIGNAL_EXCLAMATION_MARK][去除信号栏中的感叹号][yangheng][201603017] BEGIN
    public static final boolean BIRD_REMOVE_SIGNAL_EXCLAMATION_MARK = getValue("ro.bd_rm_exclamation");
    //[BIRD][BIRD_REMOVE_SIGNAL_EXCLAMATION_MARK][去除信号栏中的感叹号][yangheng][201603017] END
    //[BIRD][BIRD_QS_POWER_SAVE_SUPPORT][下拉菜单界面添加“超级省电”快捷图标][yangheng][201603017] BEGIN
    public static final boolean BIRD_QS_POWER_SAVE_SUPPORT = getValue("ro.bd_qs_power_save");
    //[BIRD][BIRD_QS_POWER_SAVE_SUPPORT][下拉菜单界面添加“超级省电”快捷图标][yangheng][201603017] END
    //[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20170508] BEGIN
    public static final boolean BIRD_CLEAN_RECENT_APP_WOTU = getValue("ro.bd_clean_app_wotu");
    //[BIRD][BIRD_CLEAN_RECENT_APP_WOTU][喔图 每小时自动清理recent app][yangheng][20160508] END

    //[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] BEGIN
    public static final boolean BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE = getValue("ro.bd_volte_vowife_change");
    //[BIRD][BIRD_CHANGE_VOLTE_VOWIFI_DRAWABLE][更换vowifi和volte图标][yangheng][20170605] END

    //[BIRD][BIRD_SHOW_CHARGE_LOCKSCREEN][锁屏界面显示充电百分比][chenguangxiang][20170613] begin
    public static final boolean BIRD_SHOW_CHARGE_LOCKSCREEN = getValue("ro.bird_show_charge_lock");
    //[BIRD][BIRD_SHOW_CHARGE_LOCKSCREEN][锁屏界面显示充电百分比][chenguangxiang][20170613] end

    //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] BEGIN
    public static final boolean BIRD_SIM_ICON_SCALE_SMALLER = getValue("ro.bd_sim_icon_smaller");
    //[BIRD][BIRD_SIM_ICON_SCALE_SMALLER][网络标识 4G/3G/E改小][yangheng][20170615] END

    //[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN
    public static final boolean BIRD_AIMEI_STUDENT = getValue("ro.bd_aimei_student");
    //[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 END

    //[BIRD][BIRD_FULL_POWER_RING][充满电铃声提示][youhonggang][20170324] begin
    public static final boolean BIRD_FULL_POWER_RING = getValue("ro.bd_full_power_ring");
    //[BIRD][BIRD_FULL_POWER_RING][充满电铃声提示][youhonggang][20170324] end

    //[BIRD][BIRD_SHOW_LTE_FOR_4G][4G网络情况下网络类型图标和数据链接图标都显示为lte而不是4G][pangmeizhou][20161130]begin
    public static final boolean BIRD_SHOW_LTE_FOR_4G = getValue("ro.bd_show_lte_for_4g");
    //[BIRD][BIRD_SHOW_LTE_FOR_4G][4G网络情况下网络类型图标和数据链接图标都显示为lte而不是4G][pangmeizhou][20161130]end

    /*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
    public static final boolean BIRD_ENCRYPT_SPACE = getValue("ro.bd_encrypt_space") ;
    /*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */

    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]begin*/
    public final static boolean BIRD_KUSAI_KEYGUARD = getValue("ro.bd_kusai_keyguard");
    /*[BIRD][BIRD_KUSAI_KEYGUARD][基于原生锁屏的酷赛锁屏][zhangaman][20170623]end*/

    /*[BIRD][BIRD_KUSAI_VOLUME_DIALOG][基于原生的的酷赛音量调节][zhangaman][20170623]begin*/
    public final static boolean BIRD_KUSAI_VOLUME_DIALOG = getValue("ro.bd_kusai_volume_dialog");
    /*[BIRD][BIRD_KUSAI_VOLUME_DIALOG][基于原生的的酷赛音量调节][zhangaman][20170623]end*/
    
    /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]BEGIN */
     public static final boolean BIRD_SHOTDOWN_CHARGE_SUGGESTION = getValue("ro.bd_shutdown_charge");
    /*[BIRD][BIRD_SHOTDOWN_CHARGE_SUGGESTION][由于电池容量较大，建议在关机状态下充电，保证充电过程稳定][客户需求][yangbo][20170726]END */
    
    /*[BIRD][BIRD_LOCKSCREEN_WALLPAPER][锁屏壁纸][yangbo][20170913]BEGIN */
    public static final boolean BIRD_LOCKSCREEN_WALLPAPER = getValue("ro.bird_lockscreen_wallpaper") || getValue("ro.bd_fix_lockscreen_wallpaper");
    /*[BIRD][BIRD_LOCKSCREEN_WALLPAPER][锁屏壁纸][yangbo][20170913]END */
    
    //[BIRD][BIRD_SPN_SHOW_FIRST_CAP_OTHER_LOWER][SPN显示首字母大写,其余字母小写][LiuQishuai][20170227] BEGIN
    public static final boolean BIRD_SPN_SHOW_FIRST_CAP_OTHER_LOWER = getValue("ro.bird_spn_custom");
    //[BIRD][BIRD_SPN_SHOW_FIRST_CAP_OTHER_LOWER][SPN显示首字母大写,其余字母小写][LiuQishuai][20170227] END

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE

    //[BIRD][BIRD_FACELOCK_SCREEN] caoyuangui add begin add 20170904
    public static final boolean BIRD_FACELOCK_SCREEN = getValue("ro.bd_facelock_screen");
    //[BIRD][BIRD_FACELOCK_SCREEN] caoyuangui add end add 20170904    

    //[BIRD][BIRD_TRAFFIC_MANAGE] caoyuangui add begin add 20170904
    public static final boolean BIRD_TRAFFIC_MANAGE = getValue("ro.bd_traffic_manage");
    //[BIRD][BIRD_TRAFFIC_MANAGE] caoyuangui add end add 20170904 
    
    /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]BEGIN */
    public static final boolean BIRD_AIS_NETWORKICON_TASK = getValue("ro.bd_incall_networkicon_g");
    /*[BIRD][BIRD_AIS_NETWORKICON_TASK][拨打电话、WiFi已连接时状态栏上不要显示网络标识图标(泰国ais的要求)][yangbo][20180316]END */
    
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]BEGIN */
    public static final boolean BIRD_SETTINGS_ADD_DATE_FORMAT = getValue("ro.bird_add_date_format");
    /*[BIRD][BIRD_SETTINGS_ADD_DATE_FORMAT]["设置-日期和时间"增加"选择日期格式项"][yangbo][20180105]END */
    
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }


    /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 BEGIN*/
    private static final Uri mAppUri =
            Uri.parse("content://com.xycm.schoolbased.AppContentProvider/apps");
    private static final String COL_STATE = "state";
    private static final String[] COL_S = {
            COL_STATE
    };
    private static final String SELECTION = "pkgname='com.android.settings'";
    private static final int LOCK = 1;

    public static  boolean ISFORBID_SETTINGS = false;

    public static  boolean isForbid(Context context) {
        Cursor cursor = context.getApplicationContext().getContentResolver().query(mAppUri, COL_S, SELECTION, null, null);
         android.util.Log.d("CAOYUANGUI", "cursor = "+cursor);
        if (cursor != null) {
            int stateIndex = cursor.getColumnIndex(COL_STATE);
            android.util.Log.d("CAOYUANGUI", "stateIndex = "+stateIndex);
            while (cursor.moveToNext()) {
                int state = cursor.getInt(stateIndex);
               android.util.Log.d("CAOYUANGUI", "state = "+state);
                if (state == LOCK) {
                    return true;
                }
            }
        }
        return false;
    }

    public static  boolean isForbid(Context context, String packageName) {
        android.util.Log.d("CAOYUANGUI", "packageName = "+packageName);
        String selection = "pkgname=?";
        Cursor cursor = context.getApplicationContext().getContentResolver().query(mAppUri, COL_S, selection, new String[]{packageName}, null);
         android.util.Log.d("CAOYUANGUI", "cursor = "+cursor);
        if (cursor != null) {
            int stateIndex = cursor.getColumnIndex(COL_STATE);
            while (cursor.moveToNext()) {
                int state = cursor.getInt(stateIndex);
               android.util.Log.d("CAOYUANGUI", "state = "+state);
                if (state == LOCK) {
                    return true;
                }
            }
        }
        return false;
    }

    /*[BIRD][BIRD_AIMEI_STUDENT] caoyuangui 20170613 END*/

}
