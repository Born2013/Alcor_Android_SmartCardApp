package com.bird.ElectronicCard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Build;
import android.os.SystemProperties;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.telephony.TelephonyManagerEx;

import android.os.ServiceManager;
import android.os.IBinder;
import android.os.RemoteException;

public class Util {

    private static final boolean debug = true;
    private static final boolean MTK_GEMINI_SUPPORT = SystemProperties.get("ro.mtk_gemini_support").equals("1");;
    private static String alert_msg;
    private static TelephonyManagerEx mTelephonyManagerEx;

    public static void saveMsgInfo(Context context, String msgInfo) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("GST_SUCESS", 0).edit();
        prefs.putString("GST_SUCESS_MSGINO", msgInfo);
        prefs.commit();
    }

    public static String loadMsgInfo(Context context, boolean getDefaulValues) {
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String msg = "null";
        String imei = telephonyManager.getDeviceId();
        String imei2 = "";
        String product_name = SystemProperties.get("ro.product.model");
        String project_msg = /*context.getResources().getString(
                R.string.send_message_info)*/"";
        alert_msg = /*context.getResources().getString(
                R.string.alert_dialog_message)*/"";
        if (MTK_GEMINI_SUPPORT) {
            imei = telephonyManager.getDeviceId(0);
            imei2 = telephonyManager.getDeviceId(1);
            msg = project_msg.replaceAll("IMEI", imei == null ? "imei : null" 
                    : imei + " " + product_name);
        } else {
            msg = project_msg.replaceAll("IMEI", imei == null ? "imei : null"
                    : imei + " " + product_name);
        }
        if (getDefaulValues)
            return msg;
        return prefs.getString("GST_SUCESS_MSGINO", msg);
    }

    public static void saveServerNumber(Context context, String serverNumber) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("GST_SUCESS", 0).edit();
        prefs.putString("GST_SUCESS_SERVERNUMBER", serverNumber);
        prefs.commit();
    }

    public static String loadServerNumber(Context context, boolean getDefaulValues) {
        String serverNumber = "10086";
        serverNumber = /*context.getResources().getString(R.string.server_number)*/"";
        if (getDefaulValues)
            return serverNumber;
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        return prefs.getString("GST_SUCESS_SERVERNUMBER", serverNumber);
    }

    public static void saveMAXTIME(Context context, long MAXTIME) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("GST_SUCESS", 0).edit();
        prefs.putLong("GST_SUCESS_MAXTIME", MAXTIME);
        prefs.commit();
    }

    public static long loadMAXTIME(Context context, boolean getDefaulValues) {
        long maxTime = 0;
        int mSendTime = /*context.getResources().getInteger(R.integer.send_time)*/0;
        maxTime = mSendTime * 30 * 60 * 1000;
        if (getDefaulValues)
            return maxTime;
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        return prefs.getLong("GST_SUCESS_MAXTIME", maxTime);
    }

    public static boolean isMCC(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = telephonyManager.getSubscriberId();
        log("imsi = " + imsi + ",Build.MODEL = " + Build.MODEL);
        if (imsi != null) {
            String mcc = imsi.substring(0, 3);
            log("imsi = " + imsi);
            if (mcc.equals("404") || mcc.equals("405") || mcc.equals("406")) {
                log("is india phone ");
                return true;
            } else if (mcc.equals(loadMcc(context))) {
                log("is chinese phone + loadMcc = " + loadMcc(context));
                return true;
            }
        }
        return false;
    }

    public static boolean isCanSimEnable(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            return TelephonyManager.SIM_STATE_READY == telephonyManager
                    .getSimState();
        } catch (Exception e) {

        }
        return false;
    }

    public static void saveMcc(Context context, String mcc) {
        SharedPreferences.Editor prefs = context.getSharedPreferences("GST_SUCESS", 0).edit();
        prefs.putString("GST_SUCESS_MCC", mcc);
        prefs.commit();
    }

    public static String loadMcc(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        return prefs.getString("GST_SUCESS_MCC", "0");
    }

    public static void savePhoneTimeUsed(Context context, long TIME_UESD) {
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        prefs.edit().putLong("PHONE_TIME_USED",loadPhoneTimeUsed(context) + TIME_UESD).commit();
    }

    public static long loadPhoneTimeUsed(Context context) {
        SharedPreferences pref = context.getSharedPreferences("GST_SUCESS", 0);
        long TIME_UESD = pref.getLong("PHONE_TIME_USED", 0);
        return TIME_UESD;
    }

    public static void resetPhoneTimeUsed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        prefs.edit().putLong("PHONE_TIME_USED", 0).commit();
    }

    public static void saveSendMsgStatus(Context context, boolean isSendSuccess) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(
                "GST_SUCESS", 0).edit();
        prefs.putBoolean("GST_SUCESS_SENDMSG", isSendSuccess);
        prefs.commit();
    }

    public static boolean loadSendMsgStatus(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GST_SUCESS", 0);
        return prefs.getBoolean("GST_SUCESS_SENDMSG", false);
    }

    static void updateTimes() {
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }
        log("updateTimes : " + convert(ut));
    }

    static String convert(long t) {
        int s = (int) (t % 60);
        int m = (int) ((t / 60) % 60);
        int h = (int) ((t / 3600));
        return h + ":" + pad(m) + ":" + pad(s);
    }

    private static String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private static void log(String log) {
        if (debug) Log.d("GSTSystem_Util", log);
    }

    public static String getDialogMessage() {
        return alert_msg;
    }
    
    public static void setSharedPreference(Context context,boolean value){
    	SharedPreferences shared = context.getSharedPreferences("SaleInfoReceiver", 0);
    	shared.edit().putBoolean("istime", value).commit();
    }
    public static boolean getSharedPreference(Context context){
    	SharedPreferences shared = context.getSharedPreferences("SaleInfoReceiver", 0);
    	return shared.getBoolean("istime", false);
    }
    
    public static void setSharedTime(Context context,String startTime,Long value){
    	SharedPreferences shared = context.getSharedPreferences("SaleInfoReceiver", 0);
    	shared.edit().putLong(startTime, value).commit();
    }
    public static Long getSharedTime(Context context,String startTime){
    	SharedPreferences shared = context.getSharedPreferences("SaleInfoReceiver", 0);
    	return shared.getLong(startTime, 0);
    }
}
