package com.bird.studentsos;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.R.integer;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.bird.studentsos.R;
import android.app.ActivityManager;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Call;
import android.os.SystemProperties;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
public class StudentSosService extends Service {
    public static final String TAG = "gaowei0621";
    public static final String[] dualSimTypes = { "subscription",
            "Subscription", "com.android.phone.extra.slot", "phone",
            "com.android.phone.DialingMode", "simId", "simnum", "phone_type",
            "simSlot" };
    public static Context mContext;
    public int EMEGERCY_NUM_MAX = 0;
    static MediaPlayer mSosPlayer = null;
    private TelephonyManager mTelephonyMgr = null;
    private TelecomManager mTelecomManager = null;
    private CallManager mCM;
    public String[] phoneNums;
    public String[] phoneNames;
    public static final String FAMILY_NAME = "familyname";
    public static final String FAMILY_NUM = "familynum";
    public static final String[] SOS_S = {
            FAMILY_NAME,
            FAMILY_NUM
    };
    public static final Uri mUri =
            Uri.parse("content://com.xycm.schoolbased.FamilyContentProvider/familynum");
    private static final int PHONE_STATE_CHANGED = 102;
    private boolean mIsSosSuccess = false;
    private boolean mIsDialing = false;
    private boolean mIsHasSIM = true; 
    public static ContentResolver mResolver;
    /*[bug-10512][BIRD_OLD_PHONE][老人模式紧急电话通话中,打开相机拍照,拍完相片后,按HOME键退出,紧急报警声会响一下]huangzhangbin 20170414 begin*/
    private boolean mCanPalyTone;
    /*[bug-10512][BIRD_OLD_PHONE][老人模式紧急电话通话中,打开相机拍照,拍完相片后,按HOME键退出,紧急报警声会响一下]huangzhangbin 20170414 end*/
    public StudentSosService(){
    }
    /**
     * onBind Intent message
     * 
     * onBind
     * 
     * @param
     */
    @Override
    public IBinder onBind(Intent msg) {
        return null;
    }

    public static void initIsDoubleTelephone(Context context) {
        boolean isDouble = true;
        Method method = null;
        Object result_0 = null;
        Object result_1 = null;
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        try {
            method = TelephonyManager.class.getMethod("getSimStateGemini",
                    new Class[] { int.class });
            // 获取SIM卡1
            result_0 = method.invoke(tm, new Object[] { new Integer(0) });
            // 获取SIM卡2
            result_1 = method.invoke(tm, new Object[] { new Integer(1) });
        } catch (SecurityException e) {
            isDouble = false;
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            isDouble = false;
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            isDouble = false;
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            isDouble = false;
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            isDouble = false;
            e.printStackTrace();
        } catch (Exception e) {
            isDouble = false;
            e.printStackTrace();
        }
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        Editor editor = sp.edit();
        if (isDouble) {
            editor.putBoolean("ISDOUBLE", true);
            if (result_0.toString().equals("5")
                    && result_1.toString().equals("5")) {
                if (!sp.getString("SIMCARD", "2").equals("0")
                        && !sp.getString("SIMCARD", "2").equals("1")) {
                    editor.putString("SIMCARD", "0");
                }
                editor.putBoolean("SIMCARD_1", true);
                editor.putBoolean("SIMCARD_2", true);
            } else if (!result_0.toString().equals("5")
                    && result_1.toString().equals("5")) {// 卡二可用
                if (!sp.getString("SIMCARD", "2").equals("0")
                        && !sp.getString("SIMCARD", "2").equals("1")) {
                    editor.putString("SIMCARD", "1");
                }
                editor.putBoolean("SIMCARD_1", false);
                editor.putBoolean("SIMCARD_2", true);
            } else if (result_0.toString().equals("5")
                    && !result_1.toString().equals("5")) {// 卡一可用
                if (!sp.getString("SIMCARD", "2").equals("0")
                        && !sp.getString("SIMCARD", "2").equals("1")) {
                    editor.putString("SIMCARD", "0");
                }
                editor.putBoolean("SIMCARD_1", true);
                editor.putBoolean("SIMCARD_2", false);
            } else {// 两个卡都不可用(飞行模式会出现这种种情况)
                editor.putBoolean("SIMCARD_1", false);
                editor.putBoolean("SIMCARD_2", false);
            }
        } else {
            // 保存为单卡手机
            editor.putString("SIMCARD", "0");
            editor.putBoolean("ISDOUBLE", false);
        }
        editor.commit();
    }

    public boolean carteDouble() {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        boolean sim1 = sp.getBoolean("SIMCARD_1", false);
        boolean sim2 = sp.getBoolean("SIMCARD_2", false);
        return sim1 && sim2;

    }

    /**
     * create the service
     * 
     * onCreate
     * 
     * @param
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"=======onCreate========");
        mContext = StudentSosService.this;
        if (mResolver == null) {
            mResolver = mContext.getContentResolver();
        }
        quertFamilyNum();
        mCM = PhoneApp.getInstance().mCM;
        mCM.registerForPreciseCallStateChanged(mHandler, PHONE_STATE_CHANGED, null); 
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }
    
    /**
     * start the service
     * 
     * onStart
     * 
     * @param
     */
    @Override
    public int onStartCommand(Intent intent, int startID, int arg) {
        super.onStart(intent, startID);
        Log.d(TAG, "=====onStartCommand=====");
        new SosThread(this).start();
        return super.onStartCommand(intent, startID, arg);
    }
    
    private final long SLEEP_MS = 5*1000;
    private class SosThread extends Thread{
        private StudentSosService mSer = null;
        public SosThread(StudentSosService ser){
            mSer = ser;
        }
        @Override
        public void run() {
            Log.i(TAG, "-->SosThread.run()");
            try{
                playSosTone();
                sleep(1000);//sleep(SLEEP_MS);
                //实现拨号循环
                while (!mIsSosSuccess && EMEGERCY_NUM_MAX != 0 && mIsHasSIM) {
                    Log.i(TAG, "to call familyNum util that is success!!!");
                    for(int idx=0; idx < EMEGERCY_NUM_MAX; idx ++){
                        if (!mIsSosSuccess) {
                            if (!TextUtils.isEmpty(phoneNums[idx]) && !isInterrupted()){
                                Log.i(TAG, "==========mTelephonyMgr.getCallState()========="+mTelephonyMgr.getCallState());
                                Log.i(TAG, "==========TelephonyManager.CALL_STATE_IDLE========="+TelephonyManager.CALL_STATE_IDLE);
                                while (mIsDialing || mTelephonyMgr.getCallState() != TelephonyManager.CALL_STATE_IDLE){
                                    Log.i(TAG, "==========sleep=========");
                                    if (mIsSosSuccess) {
                                        break;
                                    }
                                    sleep(1000);
                                }
                                if (!isInterrupted() && !mIsSosSuccess){
                                    startCall(idx);
                                }
                            }
                        } else {
                            Log.i(TAG, "the call is success ,to interrupt this thread!");
                            interrupt();
                            break;
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                Log.i(TAG, "when success or no num to stopSelf!!!"+EMEGERCY_NUM_MAX);
                stopSosTone();
                mSer.stopSelf();                
            }
        }
        
    }
    
    private void startCall(int idx){
        mIsDialing = true;
        Log.d(TAG,"startCall idx ="+idx+",phoneNums = "+phoneNums[idx]+",phoneNames = "+phoneNames[idx]);
        Uri localUri = null;
        if (phoneNames[idx] == "" || phoneNames[idx].equals("")) {
            localUri = Uri.parse("tel:" + phoneNums[idx]);
        } else {
            localUri = Uri.parse("tel:" + phoneNames[idx]);
        }
        if(PhoneNumberUtils.isEmergencyNumber(phoneNums[idx])){
            android.util.Log.d(TAG, "call emergency number");
            Intent call=new Intent(Intent.ACTION_CALL_EMERGENCY, Uri.parse("tel:" + phoneNums[idx]));
            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(call);    
        }else{
            Intent call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
                    + phoneNums[idx]));
            
            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            call.putExtra("newCallInterface", "yes");

            initIsDoubleTelephone(getApplicationContext());
            if (carteDouble()) {
                for (int i = 0; i < dualSimTypes.length; i++) {
                    
                    Log.i(TAG, "dualSimTypes[i] =  "+dualSimTypes[i]);
                    call.putExtra(dualSimTypes[i], 2);
                }
            }
            //双卡默认卡一拨打电话
            if (isMultiSim(mContext)) {
                List<PhoneAccountHandle> phoneAccountHandleList = mTelecomManager.getCallCapablePhoneAccounts();
                call.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleList.get(0));
            }
            Log.i(TAG, "-->startActivity() intent =  "+call);
            startActivity(call);            
        }
    }

    public boolean isMultiSim(Context context){
        boolean result = false;
        if(mTelecomManager != null){
            List<PhoneAccountHandle> phoneAccountHandleList = mTelecomManager.getCallCapablePhoneAccounts();
            Log.d(TAG,"phoneAccountHandleList.size() ="+phoneAccountHandleList.size());
            if (phoneAccountHandleList.size() == 0) {
                mIsDialing = false;
                mIsHasSIM = false;
                StudentSosService.this.stopSelf();
                return false;
            }
            result = phoneAccountHandleList.size() >= 2;
        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"------onDestroy-----");
        //sos时，无论拨号是否被禁用，均可以拨出电话
        SystemProperties.set("persist.sys.student.sos","0");
    }

    /*[紧急拨号停止运行]huangzhangbin 20170104 begin*/
    private boolean canStopTone = true;
    /*[紧急拨号停止运行]huangzhangbin 20170104 end*/

    /**
     * Plays the specified tone when Sos key down
     * 
     * call playSosTone() when key up.
     * 
     * @param keycode
     */
    public void playSosTone() {
        /*[bug-10512][BIRD_OLD_PHONE][老人模式紧急电话通话中,打开相机拍照,拍完相片后,按HOME键退出,紧急报警声会响一下]huangzhangbin 20170414 begin*/
        if (mCanPalyTone) {
            stopSosTone();
            if (mSosPlayer == null) {
                mSosPlayer = MediaPlayer.create(this, R.raw.legend_sos_tone);
            }
            mSosPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mSosPlayer.start();
            mCanPalyTone = false;
        }
        /*[bug-10512][BIRD_OLD_PHONE][老人模式紧急电话通话中,打开相机拍照,拍完相片后,按HOME键退出,紧急报警声会响一下]huangzhangbin 20170414 end*/
    }

    /**
     * Stop the Sos tone if it is played.
     */
    public void stopSosTone() {
        /*[紧急拨号停止运行]huangzhangbin 20170104 begin*/
        if (mSosPlayer != null && canStopTone) {
            canStopTone = false;
            mSosPlayer.stop();
            mSosPlayer.release();
            mSosPlayer = null;
            canStopTone = true;
        }
        /*[紧急拨号停止运行]huangzhangbin 20170104 end*/
    }

    private void updatePhoneSateChange(){
        Call fgCall = mCM.getActiveFgCall();
        if (mCM.hasActiveRingingCall()) {
            fgCall = mCM.getFirstActiveRingingCall();
        }
        final Call.State state = fgCall.getState();
        switch (state) {
            case IDLE:
                Log.d(TAG,"=======state  IDLE======>");
                mIsDialing =false;
                break;
            case ACTIVE:
                Log.d(TAG,"=======state  ACTIVE======>");
                mIsSosSuccess = true;
                mIsDialing =false;
                break;
            case DIALING:  
            case ALERTING:
                Log.d(TAG,"======state DIALING OR ALERTING");
                mIsDialing = true;
                break;
            case HOLDING:
                Log.d(TAG,"======state HOLDING====");
                mIsDialing = true;
                break;
            default:
                Log.d(TAG,"======state state===="+state);
                mIsDialing =false;
                break;
        }
        
    }

    private Handler mHandler=new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case PHONE_STATE_CHANGED:
                updatePhoneSateChange();
                break;

            default:
                break;
            }
        };
    };

    //read family_phone_number from database
    public void quertFamilyNum() {
        Cursor cursor = mResolver.query(mUri, SOS_S, null, null, null);
        Log.d(TAG, "cursor = " + cursor);
        Log.d(TAG, "count = " + cursor.getCount());
        EMEGERCY_NUM_MAX = cursor.getCount();
        phoneNums = new String[EMEGERCY_NUM_MAX];
        phoneNames = new String[EMEGERCY_NUM_MAX];
        int index = 0;
        if (cursor != null) {
            int familyName = cursor.getColumnIndex(FAMILY_NAME);
            int familyNum = cursor.getColumnIndex(FAMILY_NUM);
            Log.d(TAG,"=====familyName===="+familyName);
            Log.d(TAG,"=====familyNum===="+familyNum);
            while (cursor.moveToNext()) {
                String familyName1 = cursor.getString(familyName);
                String familyNum1 = cursor.getString(familyNum);
                Log.d(TAG,"=====familyNum1===="+familyNum1);
                phoneNums[index] = familyNum1;
                phoneNames[index] = familyName1;
                index++;
            }
        }
        if (phoneNums.length == 0) {
            Toast.makeText(mContext, R.string.bird_here_no_num, Toast.LENGTH_SHORT).show();
        }
    }
}
