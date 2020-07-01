package com.bird.smt_sw;

import com.android.internal.app.IMediaContainerService;
//import com.android.internal.telephony.Phone;



import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.Build;
//import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.os.SystemProperties;

//[BIRD][BIRD_SERIAL_NUM_FROM_BARCODE][通过写码工具写BarCode后,设置-关于手机-状态信息-序列号与写入的BarCode一致][dingjiayuan][20160718]begin
import android.os.Build;
//[BIRD][BIRD_SERIAL_NUM_FROM_BARCODE][通过写码工具写BarCode后,设置-关于手机-状态信息-序列号与写入的BarCode一致][dingjiayuan][20160718]end
public class MMIProduceTest extends MMINewActivity {

	private static final String TAG = "MMIProduceTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;

	private TextView mBarCode_tv;
	private TextView mImei1_tv;
	private TextView mImei2_tv;
	private TextView mSim1_tv;
	private TextView mSim2_tv;
	private TelephonyManager mTelMgr;
	
	////
	private ITelephonyEx mTelEx;
	private TelephonyManagerEx mTelMgrEx;
	String mBarCode = null;

	String mImei1;

	String mImei2;

	String mSim1;

	String mSim2;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private StorageManager mStorageManager = null;
    private static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    private static final int MSG_CONNECTED = 1;
    private String mSDCardPath = null; 
    private String mSDCard2Path = null;
    private boolean mSDCardMounted = true; 
    private boolean mSDCard2Mounted = false; 
    private TextView mSdCard;
    private TextView mInternalStorageTextView;
    
    private TextView SwVersion;
    private TextView SwVersionValue;
    
    public static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(
            DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
	private IMediaContainerService mDefaultContainer;
    final private ServiceConnection mDefContainerConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            final IMediaContainerService imcs = IMediaContainerService.Stub
            .asInterface(service);
            mDefaultContainer = imcs;
            mHandler.sendEmptyMessageDelayed(MSG_CONNECTED, 2000);
            //mHandler.sendEmptyMessage(MSG_CONNECTED);
            
        }

        public void onServiceDisconnected(ComponentName name) {
        	mDefaultContainer = null;            
        }
    };

    private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what){
			    case MSG_CONNECTED:
			    	updateUI();
			    break;
			}
			
			super.handleMessage(msg);
		}
	    
    };
    
    private void updateUI(){
        String[] pathlist = mStorageManager.getVolumePaths();
        Log.d(TAG, "!pathlist  size = "+pathlist.length);
        
		if(pathlist != null){
        	if(pathlist.length >= 2){
        		mSDCardPath = pathlist[0];
        		mSDCard2Path = pathlist[1];
        		Log.d(TAG, "pathlist[0] = "+pathlist[0]);
                Log.d(TAG, "pathlist[1] = "+pathlist[1]);
        		//mNote.setText(R.string.sd_note_have_have_external);
        	}else if(pathlist.length == 1){
        		mSDCardPath = pathlist[0];
                Log.d(TAG, "pathlist[0] = "+pathlist[0]);
        		//mNote.setText(R.string.sd_note_have);
        	}
        } else {
            return;
        }
		
        mSDCardMounted = checkSDCardMount(mSDCardPath);
        mSDCard2Mounted = checkSDCardMount(mSDCard2Path);
        Log.d(TAG, "@mSDCardMounted ="+mSDCardMounted+"mSDCard2Mounted ="+mSDCard2Mounted);
        long[] stats = {0};
        
        long[] stats2 = {0};
        if(pathlist.length == 1 && mSDCardMounted){
                    	
        	try {
        		//stats2 =  mDefaultContainer.getFileSystemStats(mSDCard2Path);
        		stats = mDefaultContainer.getFileSystemStats(mSDCardPath);
        	} catch (RemoteException e) {
        		Log.d(TAG, "Problem in container service " + e);
        	}
            //mSdCard.setVisibility(View.GONE);
            mInternalStorageTextView.append(stats[0]/1024+"KB");
        }
        if (mSDCardMounted == true && mSDCard2Mounted == true){
        	
        	try {
        		stats2 =  mDefaultContainer.getFileSystemStats(mSDCard2Path);
        		stats = mDefaultContainer.getFileSystemStats(mSDCardPath);
        	} catch (RemoteException e) {
        		Log.d(TAG, "Problem in container service " + e);
        	}
            /*
			if(SystemProperties.get("ro.mtk_2sdcard_swap").equals("1")) {
               mSdCard.append(stats[0]/1024+"KB");
			} else {
               mSdCard.append(stats2[0]/1024+"KB");
			}*/
            mInternalStorageTextView.append(stats[0]/1024+"KB");
            mSdCard.append(stats2[1]/1024+"KB");

       	  // mNote2.setText(R.string.sd2_note_have);	
           
        } else {

        }        
    }
    
    private boolean checkSDCardMount(String mountPoint) { 
        if(mountPoint == null){ 
            return false; 
        } 
        String state = null; 
        state = mStorageManager.getVolumeState(mountPoint); 
        return Environment.MEDIA_MOUNTED.equals(state); 
    } 
    
	@Override
	public void onCreate(Bundle savedInstanceState)
	{

		Log.d(TAG, "MMITestProduceTestResult  onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.produce_test);
		setTitle(R.string.produce_info);

        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
		
        mBarCode_tv = (TextView) findViewById(R.id.bar_code);
        mImei1_tv = (TextView) findViewById(R.id.imei1);
        mImei2_tv = (TextView) findViewById(R.id.imei2);
        mSim1_tv = (TextView) findViewById(R.id.sim1);
        mSim2_tv = (TextView) findViewById(R.id.sim2);
        mSdCard = (TextView) findViewById(R.id.sdcard);
        mInternalStorageTextView = (TextView) findViewById(R.id.internal_storage);
        SwVersion = (TextView) findViewById(R.id.sw_version);
        SwVersionValue = (TextView) findViewById(R.id.sw_version_value);
        
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        Intent service = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        this.bindService(service, mDefContainerConn,
                Context.BIND_AUTO_CREATE);

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
		mTelMgr = (TelephonyManager) this.getSystemService(this.TELEPHONY_SERVICE);
        mTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        mTelMgrEx = TelephonyManagerEx.getDefault();
        
		if (mTelMgr == null)
			return;
        //try {
		    //mBarCode = mTelEx.getSerialNumber();
        //} catch (RemoteException ex) {
        //    ex.printStackTrace();        
        //    mBarCode = null;
        //}
        mBarCode = SystemProperties.get("gsm.serial");
		if (mBarCode == null || mBarCode.equals(""))
		{
            //[BIRD][BIRD_SERIAL_NUM_FROM_BARCODE][通过写码工具写BarCode后,设置-关于手机-状态信息-序列号与写入的BarCode一致][dingjiayuan][20160718]begin
            String serial = Build.SERIAL;
			if (SystemProperties.get("ro.bd_serial_num_from_barcode").equals("1") && (serial != null) && !serial.equals("") ) {
                mBarCode = serial;
            } else {
			    mBarCode = " ";
            }
            //[BIRD][BIRD_SERIAL_NUM_FROM_BARCODE][通过写码工具写BarCode后,设置-关于手机-状态信息-序列号与写入的BarCode一致][dingjiayuan][20160718]end
		}

		mBarCode_tv.append(": ");

		mBarCode_tv.append(mBarCode);

		//mImei1 = mTelMgr.getDeviceId();
		mImei1 = mTelMgr.getImei(PhoneConstants.SIM_ID_1);//[BUG #6715][dengyang][工具写码和手动写码提示成功，但在工厂模式测试中IMEI不显示][20160507]
		if (mImei1 == null)
		{

			mImei1 = " ";

		}

		mImei1_tv.append(" ");

		mImei1_tv.append(mImei1);

		////mImei2 = mTelMgr.getDeviceIdGemini(PhoneConstants.GEMINI_SIM_2);
		//mImei2 = mTelMgrEx.getDeviceId(PhoneConstants.SIM_ID_2);
        mImei2 = mTelMgr.getImei(PhoneConstants.SIM_ID_2);//[BUG #6715][dengyang][工具写码和手动写码提示成功，但在工厂模式测试中IMEI不显示][20160507]
		if (mImei2 == null)
		{

			mImei2 = " ";

		}

		mImei2_tv.append(" ");

		mImei2_tv.append(mImei2);

		//mSim1 = mTelMgr.getSubscriberIdGemini(PhoneConstants.GEMINI_SIM_1);
        mSim1 = mTelMgr.getSubscriberId(PhoneConstants.SIM_ID_1);
		
		if (mSim1 == null)

		{

			mSim1_tv.append(getString(R.string.sim_nok));

		}

		else
		{

			mSim1_tv.append(mSim1);

		}
		////mSim2 = mTelMgr.getSubscriberIdGemini(PhoneConstants.GEMINI_SIM_2);
        mSim2 = mTelMgr.getSubscriberId(PhoneConstants.SIM_ID_2);
		if (mSim2 == null)

		{

			mSim2_tv.append(getString(R.string.sim_nok));

		}

		else

		{

			mSim2_tv.append(mSim2);
         }
         SwVersion.setText(R.string.sw_version);
         SwVersionValue.setText(Build.DISPLAY);
         Log.d(TAG, "Build.DISPLAY ="+Build.DISPLAY);
    }

    @Override
    protected void onResume() {
    	Log.d(TAG, "MMITestProduceTestResult  onResume");
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.unbindService(mDefContainerConn);
	}
	
    private boolean flag = false;
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIProduceTest.this.getString(R.string.produce_info), Test_launcherActivity.PASS);
			mEditor.commit();

if(flag == false)
	//MMINativeLib.device_test_keypad_bk_turn_on_jni();
    finish();
		}
		
	};
		
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIProduceTest.this.getString(R.string.produce_info), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			//Intent intent = null;	
			//intent = new Intent(MMITestProduceTestResult.this,MMITestProduceTestResult.class);
			//	startActivityForResult(intent, 0);
			//1124 xuzhenguo			
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIProduceTest.this, MMIProduceTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
		}
	};
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMIProduceTest.this, MMIProduceTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIProduceTest.this.getString(R.string.produce_info), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
