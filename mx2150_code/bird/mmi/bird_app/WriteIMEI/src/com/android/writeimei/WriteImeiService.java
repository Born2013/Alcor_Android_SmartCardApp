/*[BIRD_NVRAM_SAVE_IMEI]: wushiyong 20160325 add begin */
package com.android.writeimei;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences; 
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.android.writeimei.FeatureOption;
import android.os.AsyncResult;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.text.TextUtils;


public class WriteImeiService extends Service {

    private Phone phone = null; 

    private static final String SET_IMEI1_PROPERTY = "persist.sys.imei1";
    private static final String SET_IMEI2_PROPERTY = "persist.sys.imei2";
    private static final String SET_MEID_PROPERTY = "persist.sys.meid1";

    private static boolean isonCDMAonCard1 = false;
    private static boolean isonCDMAonCard2 = false;
    private int cdma_phone_id = 0;
    private int gsm_phone_id = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String writeService = intent.getStringExtra("send_writeimei");
            android.util.Log.d("shujiang","BootCompleteReceiver: writeService000:"+writeService);
            if (writeService == null) {
                return super.onStartCommand(intent, flags, startId);
            }
            android.util.Log.d("shujiang","BootCompleteReceiver: writeService222:"+writeService);
            android.util.Log.d("shujiang","receive com.android.action.WRITE_IMEI");
            getCDMAInfo();
            resetSaveProperty();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void resetSaveProperty() {

        String mImei1 = SystemProperties.get(SET_IMEI1_PROPERTY, "");
        String mImei2 = SystemProperties.get(SET_IMEI2_PROPERTY, "");

        writeIMEI1(mImei1);
        writeIMEI2(mImei2);

        if (FeatureOption.MTK_C2K_SUPPORT) {
            String mMeid = SystemProperties.get(SET_MEID_PROPERTY, "");
            writeMEID(mMeid);
        }
        android.util.Log.d("shujiang","BootCompleteReceiver resetSaveProperty");
        // setAirplaneModeOn(true);
        // mHander.sendEmptyMessage(SET_AIRPLANE_MODE_OFF);
    }

    private static final int SET_AIRPLANE_MODE_OFF = 10001;
    private Handler mHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case SET_AIRPLANE_MODE_OFF:
                if (getAirplaneMode()) {          
                    setAirplaneModeOn(false);
                    //SystemProperties.set(BLUETOOTH_HOSTNAME_PROPERTY, BluetoothText);
                    mHander.removeMessages(SET_AIRPLANE_MODE_OFF);
                } else {      
                    mHander.sendEmptyMessageDelayed(SET_AIRPLANE_MODE_OFF, 500);
                }
                break;
            default:
                break;
            }
        }
    };

    private void setAirplaneModeOn(boolean enabling) {
        Log.i("shujiang", "setAirplaneModeOn:" + enabling);
        // Change the system setting
        Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enabling ? 1
                : 0);
        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        sendBroadcast(intent);
    }

    private boolean getAirplaneMode(){  
        int isAirplaneMode = Settings.System.getInt(getContentResolver(),  
                              Settings.System.AIRPLANE_MODE_ON, 0) ;  
        return (isAirplaneMode == 1)?true:false;  
    }  

    private void getCDMAInfo() {
        TelephonyManager telephonyManager =
                (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        if (FeatureOption.MTK_C2K_SUPPORT) {
            int SubId1[] = SubscriptionManager.getSubId(0);
            int SubId2[] = SubscriptionManager.getSubId(1);
            int card1_phonetype = telephonyManager.getCurrentPhoneType(SubId1[0]);
            int card2_phonetype = telephonyManager.getCurrentPhoneType(SubId2[0]);
            if (card1_phonetype== TelephonyManager.PHONE_TYPE_NONE
                && card2_phonetype== TelephonyManager.PHONE_TYPE_CDMA) {

                isonCDMAonCard1 = false;
                isonCDMAonCard2 = true;
                gsm_phone_id = SubscriptionManager.getPhoneId(SubId1[0]);
                cdma_phone_id = SubscriptionManager.getPhoneId(SubId2[0]);
            }

            if (card1_phonetype == TelephonyManager.PHONE_TYPE_CDMA){ /*UIM+NONE*/
                isonCDMAonCard1 = true;
                isonCDMAonCard2 = false;
                cdma_phone_id = SubscriptionManager.getPhoneId(SubId1[0]);
                gsm_phone_id = SubscriptionManager.getPhoneId(SubId2[0]);
            }

            if (card1_phonetype != TelephonyManager.PHONE_TYPE_NONE 
                && card1_phonetype != TelephonyManager.PHONE_TYPE_CDMA
                && card2_phonetype == TelephonyManager.PHONE_TYPE_CDMA) { /*CMCC+UIM || NONE +UIM*/

                isonCDMAonCard1 = false;
                isonCDMAonCard2 = true;
                gsm_phone_id = SubscriptionManager.getPhoneId(SubId1[0]);
                cdma_phone_id = SubscriptionManager.getPhoneId(SubId2[0]);
            }

            if (card1_phonetype != TelephonyManager.PHONE_TYPE_NONE 
                && card1_phonetype != TelephonyManager.PHONE_TYPE_CDMA
                && card2_phonetype != TelephonyManager.PHONE_TYPE_CDMA) { /*NONE+CMCC || CMCC+NONE | CMCC+CMCC*/
                
                isonCDMAonCard1 = false;
                isonCDMAonCard2 = false;
            }
        }
    }

    private  Handler mResponseHander = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar;
            ar= (AsyncResult) msg.obj;
            switch(msg.what) {
                case 1:
                    if (ar.exception == null) {
                        //"Write IMEI1 Success"
                        Log.d("shujiang", "BootCompleteReceiver Write IMEI1 Success");
                    } else {
                        //"Write IMEI1 Fail"
                        Log.d("shujiang", "BootCompleteReceiver Write IMEI1 Fail");
                    }   
                    SystemProperties.set("persist.sys.reset_imei1", "0");       
                break;
                case 2:
                    if (ar.exception == null) {
                        //"Write IMEI2 Success"
                        Log.d("shujiang", "BootCompleteReceiver Write IMEI2 Success");
                    } else {
                        //"Write IMEI2 Fail"
                        Log.d("shujiang", "BootCompleteReceiver Write IMEI2 Fail");
                    }      
                    SystemProperties.set("persist.sys.reset_imei2", "0");    
                break;

                //MEID
                /*[BIRD][MTK_C2K_SUPPORT][chengshujiang]20160730 begin*/
                case 3:
                    if (ar.exception == null) {
                        //"Write MEID Success"
                        Log.d("shujiang", "BootCompleteReceiver Write MEID Success");
                    } else {
                        //"Write MEID Fail"
                        Log.d("shujiang", "BootCompleteReceiver Write MEID Fail");
                    }
                    SystemProperties.set("persist.sys.reset_meid", "0");
                break;
                /*[BIRD][MTK_C2K_SUPPORT][chengshujiang]20160730 end*/
                default:
                break;
            } 
        }
    };

    public void writeIMEI1(String IMEI1) {
        if (IMEI1 == null || TextUtils.isEmpty(IMEI1)) {
            return;
        }
        String AttachedAT1[] = {"AT+EGMR=1,7,"+"\""+IMEI1+"\"",""};
        String AttachedAT1_INV[] = {"AT+EGMR=1,10,"+"\""+IMEI1+"\"",""};

        if (isonCDMAonCard1 || isonCDMAonCard2) {
            phone = PhoneFactory.getPhone(gsm_phone_id);
        } else {
            phone = PhoneFactory.getDefaultPhone();
        }

        if (isonCDMAonCard2) {
            phone.invokeOemRilRequestStrings(AttachedAT1_INV, mResponseHander.obtainMessage(1));
        } else {
            phone.invokeOemRilRequestStrings(AttachedAT1, mResponseHander.obtainMessage(1));
        }
    }

    public void writeIMEI2(String IMEI2) {
        if (IMEI2 == null || TextUtils.isEmpty(IMEI2)) {
            return;
        }
        String AttachedAT2[] = {"AT+EGMR=1,10,"+"\""+IMEI2+"\"",""};
        String AttachedAT2_INV[] = {"AT+EGMR=1,7,"+"\""+IMEI2+"\"",""};
        if (isonCDMAonCard1 || isonCDMAonCard2) {
            phone = PhoneFactory.getPhone(gsm_phone_id);
        }else{
            phone = PhoneFactory.getDefaultPhone();
        }

        if (isonCDMAonCard2) {
            phone.invokeOemRilRequestStrings(AttachedAT2_INV, mResponseHander.obtainMessage(2)); 
        } else {
            phone.invokeOemRilRequestStrings(AttachedAT2, mResponseHander.obtainMessage(2)); 
        }
    }

    /*[BIRD][MTK_C2K_SUPPORT][chengshujiang]20160730 begin*/
    public void writeMEID(String MEID){
        if (MEID == null || TextUtils.isEmpty(MEID)) {
            return;
        }
        String AttachedCommand = "7268324842763108";
        String AttachedAT3[] = {"AT+VMOBID=0,"+"\""+ AttachedCommand +"\""+ ",2,"+"\""+MEID+"\"",""};

        if (isonCDMAonCard1 || isonCDMAonCard2) {
            phone = PhoneFactory.getPhone(cdma_phone_id);
        }else {
            phone = PhoneFactory.getPhone(1);
        }

        try {
            phone.invokeOemRilRequestStrings(AttachedAT3, mResponseHander.obtainMessage(3));
        } catch (NullPointerException ee) {
            ee.printStackTrace();
        }
    }
}
/*[BIRD_NVRAM_SAVE_IMEI]: wushiyong 20160325 add end */
