package com.bird.smt_sw;

import android.os.SystemProperties;

public class FeatureOption {

    public static final boolean MTK_GEMINI_SUPPORT = SystemProperties.get("ro.mtk_gemini_support").equals("1");
    public static final boolean BIRD_SMT_SW = getValue("ro.bird_smt_sw");
    public static final boolean BIRD_SMT_SW_PRODUCE_INFO = getValue("ro.bird_smt_sw_proinfo");
    public static final boolean BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH = getValue("ro.bird_smt_sw_wifibt");
    public static final boolean BIRD_SMT_SW_GPS = getValue("ro.bird_smt_sw_gps");
    public static final boolean BIRD_SMT_SW_VIBRATER = getValue("ro.bird_smt_sw_vibrater");
    public static final boolean BIRD_SMT_SW_PROXIMITY = getValue("ro.bird_smt_sw_proxim");
    public static final boolean BIRD_SMT_SW_FM = getValue("ro.bird_smt_sw_fm");
    public static final boolean BIRD_SMT_SW_MAINBOARD_MIC = getValue("ro.bird_smt_sw_mainmic");
    public static final boolean BIRD_SMT_SW_SUB_MIC = getValue("ro.bird_smt_sw_submic");
    public static final boolean BIRD_SMT_SW_SPEAKER = getValue("ro.bird_smt_sw_speaker");
    public static final boolean BIRD_SMT_SW_RECEIVER = getValue("ro.bird_smt_sw_receiver");
    public static final boolean BIRD_SMT_SW_BREATH_LED = getValue("ro.bird_smt_sw_brthled");
    public static final boolean BIRD_SMT_SW_LED = getValue("ro.bird_smt_sw_led");
    public static final boolean BIRD_SMT_SW_KEYBOARD = getValue("ro.bird_smt_sw_keybrd");
    /*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]begin*/
    public static final boolean BIRD_SMT_SW_HALL_SENSOR_SUPPORT = getValue("ro.bird_smt_sw_hall");
    /*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]end*/
    public static final boolean BIRD_SMT_SW_SMALL_VERSION_KEYBOARD = false;//getValue("ro.bird_smt_sw_smkeybrd");
    public static final boolean BIRD_SMT_SW_CAMERA_TEST = getValue("ro.bird_smt_sw_camera");
    public static final boolean BIRD_SMT_SW_OTG = getValue("ro.bird_smt_sw_otg");
    public static final boolean BIRD_SMT_SW_LIGHT_SENSOR = getValue("ro.bird_smt_sw_light");
    public static final boolean BIRD_SMT_SW_SUB_FLASHLIGHT = getValue("ro.bird_smt_sw_subtorch");
    public static final boolean BIRD_SMT_SW_EARPHONE_AND_MIC_LOOP = getValue("ro.bird_smt_sw_earloop");//[BIRD]add by zhangaman 20150717
    //[BIRD][BIRD_SMT_SW_MAGNETIC_SENSOR][指南针测试][qianliliang][20160722] BEGIN
    public static final boolean BIRD_SMT_SW_MAGNETIC_SENSOR = getValue("ro.bird_smt_sw_magnetic");
    //[BIRD][BIRD_SMT_SW_MAGNETIC_SENSOR][指南针测试][qianliliang][20160722] END
    //[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]begin
    public static final boolean BIRD_SMT_SW_SDCARD = getValue("ro.bird_smt_sw_sdcard");
    //[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]end
    //[BIRD][BIRD_SMT_SW_FINGER_PRINT_SENEOR][贴片测试_指纹传感器测试][zhangaman][20160815]begin
    public static final boolean BIRD_SMT_SW_FINGER_PRINT_SENEOR = getValue("ro.bird_smt_sw_finger");
    //[BIRD][BIRD_SMT_SW_FINGER_PRINT_SENEOR][贴片测试_指纹传感器测试][zhangaman][20160815]end

    //[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA][贴片测试-副后摄][zhangaman][20160209]begin
    public static final boolean BIRD_SMT_SW_SUB_BACK_CAMERA = getValue("ro.bird_smt_sub_back_camera");
    //[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA][贴片测试-副后摄][zhangaman][20160209]end
    //[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]begin
    public static final boolean BIRD_SMT_SW_REMOTE = getValue("ro.bird_smt_sw_remote");
    //[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]END
    /*[BIRD][BIRD_SMT_SW_GYROSCOPE][贴片测试_陀螺仪测试][zhangaman][20170516]begin*/
    public static final boolean BIRD_SMT_SW_GYROSCOPE = getValue("ro.bird_smt_sw_gyroscope");
    /*[BIRD][BIRD_SMT_SW_GYROSCOPE][贴片测试_陀螺仪测试][zhangaman][20170516]end*/
    /*[BIRD][BIRD_SMT_SW_MARQUEE_LED_TEST][贴片测试_跑马灯测试][zhangaman][20170703]begin*/
    public static final boolean BIRD_SMT_SW_MARQUEE_LED_TEST = getValue("ro.bird_smt_sw_marquee");
    /*[BIRD][BIRD_SMT_SW_MARQUEE_LED_TEST][贴片测试_跑马灯测试][zhangaman][20170703]end*/

    /*[BIRD][BIRD_SMT_SW_PRESSURE_TEST][贴片测试_气压测试][zhangaman][20170706]begin*/
    public static final boolean BIRD_SMT_SW_PRESSURE_TEST = getValue("ro.bird_smt_sw_pressure");
    /*[BIRD][BIRD_SMT_SW_PRESSURE_TEST][贴片测试_气压测试][zhangaman][20170706]end*/

    /*[BIRD][BIRD_SMT_SW_TEMPERATURE_TEST][贴片测试_气温测试][zhangaman][20170707]begin*/
    public static final boolean BIRD_SMT_SW_TEMPERATURE_TEST = getValue("ro.bird_smt_sw_temperature");
    /*[BIRD][BIRD_SMT_SW_TEMPERATURE_TEST][贴片测试_气温测试][zhangaman][20170707]end*/

    /*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]begin*/
    public static final boolean BIRD_SMT_SW_TOGGLE_SWITCH = getValue("ro.bd_smt_sw_toggle_switch");
    /*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]end*/

    /*[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA_COVER][贴片测试_副后摄遮挡测试][zhangaman][20170724]begin*/
    public static final boolean BIRD_SMT_SW_SUB_BACK_CAMERA_COVER = getValue("ro.bd_smt_sw_sub_cam_cover");
    /*[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA_COVER][贴片测试_副后摄遮挡测试][zhangaman][20170724]end*/

    /*[BIRD][BIRD_FACTORY_CAM_FULL_DISPLAY][测试模式下相机测试全屏显示][luye][20160704]begin*/
    public final static boolean BIRD_FACTORY_CAM_FULL_DISPLAY = getValue("ro.bd_factory_cam_full_dis");
    /*[BIRD][BIRD_FACTORY_CAM_FULL_DISPLAY][测试模式下相机测试全屏显示][luye][20160704]end*/

    // [BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]begin
    public static final boolean BIRD_SMT_SW_SMARTCARD_TEST = getValue("ro.bird_smt_sw_smartcard_test");
    // [BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]end

    // [BIRD][BIRD_SERIAL_DATA_SUPPORT][串口数据测试][huangzhangbin][20180411]begin
    public static final boolean BIRD_SERIAL_DATA_SUPPORT = getValue("ro.bird_serial_test");
    // [BIRD][BIRD_SERIAL_DATA_SUPPORT][串口数据测试][huangzhangbin][20180411]end

    /*[BIRD_NFC_SUPPORT]huangzhangbin 20170309 begin*/
    public static final boolean BIRD_NFC_SUPPORT = getValue("ro.bird_nfc_support");;
    /*[BIRD_NFC_SUPPORT]huangzhangbin 20170309 end*/

    //[BIRD][BIRD_MMI_TEST_CUSTOM_KEYIMAGE][工厂贴片测试按键值和相应图片自定义][pangmeizhou][20180516]begin
	public static final boolean BIRD_MMI_TEST_CUSTOM_KEYIMAGE = getValue("ro.bird_fac_custom_km");
	//[BIRD][BIRD_MMI_TEST_CUSTOM_KEYIMAGE][工厂贴片测试按键值和相应图片自定义][pangmeizhou][20180516]end

    
    public static final boolean MTK_GPS_SUPPORT = getValue("ro.mtk_gps_support");

    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
