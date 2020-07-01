package com.bird.smt_sw;

import java.util.ArrayList;


import android.content.Context;
import android.hardware.Camera;
//import com.mediatek.common.featureoption.FeatureOption;

public class TestAdapter {

	private static final String TAG = "TestAdapter";
	private Context mContext;
	private ArrayList<Integer> mDeviceName;//protected static
	@SuppressWarnings("rawtypes")
	private ArrayList<Class> mDeviceClass;
	private ArrayList<String> mTestItem;
	public TestAdapter(Context c){
		mContext = c;
		mDeviceName = new ArrayList<Integer>();
		mDeviceClass = new ArrayList<Class>();
		mTestItem = new ArrayList<String>();
		initItem();
	}

	private int initItem() {
		// TODO Auto-generated method stub
		int i = 0;



		//01
		
		if(FeatureOption.BIRD_SMT_SW_PRODUCE_INFO){
			mDeviceName.add(R.string.produce_info);
			mDeviceClass.add(MMIProduceTest.class);
			mTestItem.add(mContext.getString(R.string.produce_info));
			i++;
		}

		if(FeatureOption.BIRD_SMT_SW_LED){//[led测试项宏控制][xuzhenguo][20130626]
			mDeviceName.add(R.string.led);

			mDeviceClass.add(MMILedTest.class);

			mTestItem.add(mContext.getString(R.string.led));
			i++;
		}
		
		
		if(FeatureOption.BIRD_SMT_SW_VIBRATER){
			mDeviceName.add(R.string.vibrater);

			mDeviceClass.add(MMIVibraterTest.class);

			mTestItem.add(mContext.getString(R.string.vibrater));
			i++;
		}
		
		if(false){ //FeatureOption.MTK_GPS_SUPPORT
		        mDeviceName.add(R.string.gps);

			mDeviceClass.add(MMIGPSTest.class);

			mTestItem.add(mContext.getString(R.string.gps));
			i++;
		}
		
		if(FeatureOption.BIRD_SMT_SW_CAMERA_TEST){
			mDeviceName.add(R.string.main_camera);

			mDeviceClass.add(MMIMainCameraTest.class);

			mTestItem.add(mContext.getString(R.string.main_camera));
			i++;
		}

        //[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA][贴片测试-副后摄][zhangaman][20160209]begin
        if(FeatureOption.BIRD_SMT_SW_SUB_BACK_CAMERA){
            mDeviceName.add(R.string.sub_back_camera);
            mDeviceClass.add(MMISubBackCameraTest.class);
            mTestItem.add(mContext.getString(R.string.sub_back_camera));
            i++;
        }
        //[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA][贴片测试-副后摄][zhangaman][20160209]end

		if((Camera.getNumberOfCameras() >= 2) && FeatureOption.BIRD_SMT_SW_CAMERA_TEST){
			mDeviceName.add(R.string.sub_camera);

			mDeviceClass.add(MMISubCameraTest.class);

			mTestItem.add(mContext.getString(R.string.sub_camera));
			i++;
		}
				
		if(false){//去掉 耳机麦克录音测试
			mDeviceName.add(R.string.headset_record);

			mDeviceClass.add(MMIRecordTest.class); //MMIRecordTest.class  MMIHeadsetRecordTest.class

			mTestItem.add(mContext.getString(R.string.headset_record));
			i++;
		}
		
		
		if(false){//(FeatureOption.BIRD_SMT_SW_RECORD_MIC){//去掉mic录音测试
			mDeviceName.add(R.string.board_record);

			mDeviceClass.add(MMIRecordMainBoardMic.class);//    MMIMicRecordTest.class

			mTestItem.add(mContext.getString(R.string.board_record));
			i++;
		}

		if(FeatureOption.BIRD_SMT_SW_MAINBOARD_MIC){
			mDeviceName.add(R.string.board_mic);

			mDeviceClass.add(MMITestMainMic.class);//    MMIMicRecordTest.class

			mTestItem.add(mContext.getString(R.string.board_mic));
			i++;
		}


		if(FeatureOption.BIRD_SMT_SW_SPEAKER){
			mDeviceName.add(R.string.speaker);

			mDeviceClass.add(MMITestSpeaker.class);

			mTestItem.add(mContext.getString(R.string.speaker));
			i++;
		}

		if(FeatureOption.BIRD_SMT_SW_SUB_MIC){
			mDeviceName.add(R.string.sub_mic);

			mDeviceClass.add(MMITestSubMic.class);//    MMIMicRecordTest.class

			mTestItem.add(mContext.getString(R.string.sub_mic));
			i++;
		}

		if(FeatureOption.BIRD_SMT_SW_RECEIVER){
			mDeviceName.add(R.string.receiver);

			mDeviceClass.add(MMIReceiverTest.class);

			mTestItem.add(mContext.getString(R.string.receiver));
			i++;
		}

		if(FeatureOption.BIRD_SMT_SW_OTG){//[otg测试项]
			mDeviceName.add(R.string.otg_test);

			mDeviceClass.add(MMITestOTGOpen.class);

			mTestItem.add(mContext.getString(R.string.otg_test));
			i++;
		}

             /*[BIRD]add by zhangaman 20150717 begin */
		if(FeatureOption.BIRD_SMT_SW_EARPHONE_AND_MIC_LOOP){
			mDeviceName.add(R.string.EarphoneMicLoop);
			mDeviceClass.add(MMITestLoopdEarphoneFactory.class);
			mTestItem.add(mContext.getString(R.string.EarphoneMicLoop));
			i++;
		}
             /*[BIRD]add by zhangaman 20150717 end */
             
		if(FeatureOption.BIRD_SMT_SW_KEYBOARD){//[按键测试 宏控制][xuzhenguo][20130626]
			mDeviceName.add(R.string.keyboard);

			mDeviceClass.add(MMIKeyBoardTest.class);

			mTestItem.add(mContext.getString(R.string.keyboard));
			i++;
		}
		if(FeatureOption.BIRD_SMT_SW_BREATH_LED){//呼吸灯
			mDeviceName.add(R.string.breathled);

			mDeviceClass.add(MMIBreathLedTest.class);

			mTestItem.add(mContext.getString(R.string.breathled));
			i++;
		}
		/*     //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
		if(FeatureOption.BIRD_SMT_SW_FM){
			mDeviceName.add(R.string.fm);

			mDeviceClass.add(MMIFMTest.class);

			mTestItem.add(mContext.getString(R.string.fm));
			i++;
		}
		*/ //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
		if(FeatureOption.BIRD_SMT_SW_LIGHT_SENSOR) {
		    mDeviceName.add(R.string.light);
		    mDeviceClass.add(MMILightSensorTest.class);

			mTestItem.add(mContext.getString(R.string.light));
			i++;
		}

        //[BIRD][BIRD_SMT_SW_MAGNETIC_SENSOR][指南针测试][qianliliang][20160722] BEGIN
        if(FeatureOption.BIRD_SMT_SW_MAGNETIC_SENSOR) {
            mDeviceName.add(R.string.magnetic);
            mDeviceClass.add(MMIMagneticSensorTest.class);
            mTestItem.add(mContext.getString(R.string.magnetic));
            i++;
        }
        //[BIRD][BIRD_SMT_SW_MAGNETIC_SENSOR][指南针测试][qianliliang][20160722] END
        //[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]begin
        if(FeatureOption.BIRD_SMT_SW_SDCARD){
            mDeviceName.add(R.string.sd_card);
            mDeviceClass.add(MMISDCardTest.class);
            mTestItem.add(mContext.getString(R.string.sd_card));
            i++;
        }
        //[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]end

       //[BIRD][BIRD_SMT_SW_FINGER_PRINT_SENEOR][贴片测试_指纹传感器测试][zhangaman][20160815]begin
       if(FeatureOption.BIRD_SMT_SW_FINGER_PRINT_SENEOR) {
           mDeviceName.add(R.string.fp_sensor);
           mDeviceClass.add(MMIFpTestFactory.class);
           mTestItem.add(mContext.getString(R.string.fp_sensor));
           i++;
        }
        //[BIRD][BIRD_SMT_SW_FINGER_PRINT_SENEOR][贴片测试_指纹传感器测试][zhangaman][20160815]end
        //[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]begin
        if(FeatureOption.BIRD_SMT_SW_REMOTE){
            mDeviceName.add(R.string.remote);
            mDeviceClass.add(MMIRemoteTest.class);
            mTestItem.add(mContext.getString(R.string.remote));
            i++;
        }
        //[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]end
        /*[BIRD][BIRD_SMT_SW_GYROSCOPE][贴片测试_陀螺仪测试][zhangaman][20170516]begin*/
        if(FeatureOption.BIRD_SMT_SW_GYROSCOPE){
            mDeviceName.add(R.string.gyroscope);
            mDeviceClass.add(MMITestGyroscope.class);
            mTestItem.add(mContext.getString(R.string.gyroscope));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_GYROSCOPE][贴片测试_陀螺仪测试][zhangaman][20170516]end*/
        /*[BIRD][BIRD_SMT_SW_MARQUEE_LED_TEST][贴片测试_跑马灯测试][zhangaman][20170703]begin*/
        if(FeatureOption.BIRD_SMT_SW_MARQUEE_LED_TEST){
            mDeviceName.add(R.string.marquee_led);
            mDeviceClass.add(MMITestMarqueeLed.class);
            mTestItem.add(mContext.getString(R.string.marquee_led));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_MARQUEE_LED_TEST][贴片测试_跑马灯测试][zhangaman][20170703]end*/
        /*[BIRD][BIRD_SMT_SW_PRESSURE_TEST][贴片测试_气压测试][zhangaman][20170706]begin*/
        if(FeatureOption.BIRD_SMT_SW_PRESSURE_TEST){
            mDeviceName.add(R.string.pressure);
            mDeviceClass.add(MMITestPressure.class);
            mTestItem.add(mContext.getString(R.string.pressure));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_PRESSURE_TEST][贴片测试_气压测试][zhangaman][20170706]end*/

        /*[BIRD][BIRD_SMT_SW_TEMPERATURE_TEST][贴片测试_气温测试][zhangaman][20170707]begin*/
        if(FeatureOption.BIRD_SMT_SW_TEMPERATURE_TEST){
            mDeviceName.add(R.string.temperature);
            mDeviceClass.add(MMITestTemperature.class);
            mTestItem.add(mContext.getString(R.string.temperature));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_TEMPERATURE_TEST][贴片测试_气温测试][zhangaman][20170707]end*/
        /*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]begin*/
        if(FeatureOption.BIRD_SMT_SW_HALL_SENSOR_SUPPORT){
            mDeviceName.add(R.string.hall);
            mDeviceClass.add(MMITestHall.class);
            mTestItem.add(mContext.getString(R.string.hall));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_HALL_SENSOR_SUPPORT][贴片测试_霍尔测试][zhangaman][20170718]end*/
        /*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]begin*/
        if(FeatureOption.BIRD_SMT_SW_TOGGLE_SWITCH){
            mDeviceName.add(R.string.toggle_switch);
            mDeviceClass.add(MMITestToggleSwitch.class);
            mTestItem.add(mContext.getString(R.string.toggle_switch));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_TOGGLE_SWITCH][贴片测试_波动开关测试][zhangaman][20170724]end*/
        /*[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA_COVER][贴片测试_副后摄遮挡测试][zhangaman][20170724]begin*/
        if(FeatureOption.BIRD_SMT_SW_SUB_BACK_CAMERA_COVER){
            mDeviceName.add(R.string.sub_back_camera_cover);
            mDeviceClass.add(DeviceTestSubBackCameraCover.class);
            mTestItem.add(mContext.getString(R.string.sub_back_camera_cover));
            i++;
        }
        /*[BIRD][BIRD_SMT_SW_SUB_BACK_CAMERA_COVER][贴片测试_副后摄遮挡测试][zhangaman][20170724]end*/

        // [BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]begin
        if(FeatureOption.BIRD_SMT_SW_SMARTCARD_TEST){
            mDeviceName.add(R.string.reader_test);
            mDeviceClass.add(SmartCardTest.class);
            mTestItem.add(mContext.getString(R.string.reader_test));
            i++;
        }
        // [BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]end
        
        // [BIRD][BIRD_SERIAL_DATA_SUPPORT][串口数据测试][huangzhangbin][20180411]begin
        if(FeatureOption.BIRD_SERIAL_DATA_SUPPORT){
            mDeviceName.add(R.string.serial_test);
            mDeviceClass.add(SerialDataTest.class);
            mTestItem.add(mContext.getString(R.string.serial_test));
            i++;
        }
        // [BIRD][BIRD_SERIAL_DATA_SUPPORT][串口数据测试][huangzhangbin][20180411]end

        /*[BIRD_NFC_SUPPORT]huangzhangbin 20170309 begin*/
        if (FeatureOption.BIRD_NFC_SUPPORT) {
            mDeviceName.add(R.string.bird_nfc_title);
            mDeviceClass.add(MMITestNfc.class);
            i++;
        }
        /*[BIRD_NFC_SUPPORT]huangzhangbin 20170309 end*/

		if(FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH || FeatureOption.BIRD_SMT_SW_PROXIMITY || FeatureOption.BIRD_SMT_SW_GPS){

			mDeviceName.add(R.string.Pro_info);

			mDeviceClass.add(Pro_info.class);
			
			//mTestItem.add(mContext.getString(R.string.Pro_info));
			
			if(FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH){//小板软件不要 加速传感 WIFI 蓝牙测试
			mTestItem.add(mContext.getString(R.string.accelerator_test));
			mTestItem.add(mContext.getString(R.string.wifi_test));
			mTestItem.add(mContext.getString(R.string.bluetooth_test));
			}
			if(FeatureOption.BIRD_SMT_SW_PROXIMITY)
			mTestItem.add(mContext.getString(R.string.proximity_test));
			if(FeatureOption.MTK_GPS_SUPPORT && FeatureOption.BIRD_SMT_SW_GPS){
			mTestItem.add(mContext.getString(R.string.gps));
			}
			//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
		    if(FeatureOption.BIRD_SMT_SW_FM){
			    mTestItem.add(mContext.getString(R.string.fm));
		    }
			//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
			i++;

		}
		
		if(true){
			mDeviceName.add(R.string.test_report);

			mDeviceClass.add(Test_launcherActivity.class);

			i++;
		}
		return i;
	}
	
	public int getDeviceNameAtIndex(int index) {
		return mDeviceName.get(index);
	}
	
	@SuppressWarnings("rawtypes")
	public Class getDeviceClassAtIndex(int index) {
		return mDeviceClass.get(index);
	}
	
	public int getDeviceNameSize() {
		return mDeviceName.size();
	}
	
	public int getDeviceClassSize() {
		return mDeviceClass.size();
	}
	
	public String getTestItemName(int index) {
		return mTestItem.get(index);
	}
	
	public int getTestItemSize(){
		return mTestItem.size();
	}
}
