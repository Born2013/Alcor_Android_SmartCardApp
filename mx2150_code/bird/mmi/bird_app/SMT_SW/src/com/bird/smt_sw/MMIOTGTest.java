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

public class MMIOTGTest extends MMINewActivity {

	private static final String TAG = "MMIOTGTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

    private StorageManager mStorageManager = null;
    private static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    private static final int MSG_CONNECTED = 1;
    private String mSDCardPath = null; 
    private String mSDCard2Path = null;
    private boolean mSDCardMounted = true; 
    private boolean mSDCard2Mounted = false; 
    private TextView mOtg;
    private String mOTGPath = null;
    private boolean mOTGMounted = false; 
    private static final String EXTERNAL_OTG = "/mnt/usbotg";
    private static final String EXTERNAL_OTG2 = "/storage/usbotg";
    
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
        	for(int i = 0; i < pathlist.length; i++) {
        	    if(EXTERNAL_OTG.equals(pathlist[i]) || EXTERNAL_OTG2.equals(pathlist[i])) {
        	        mOTGMounted = checkSDCardMount(pathlist[i]);
        	        mOtg.append(mOTGMounted ? "OK" : "NOK");
        	        Log.d(TAG, "@pathlist[i"+i+"]"+pathlist[i]+" mOTGMounted= "+mOTGMounted);
        	        return;
        	    }
        	}
        	mOtg.append("NOK");
        	return;
        } else {
            mOtg.append("NOK");
            return;
        }
        //Log.d(TAG, "@EXTERNAL_OTG.equals(pathlist[2]); ="+EXTERNAL_OTG.equals(pathlist[2]));
        //Log.d(TAG, "@pathlist[2] ="+pathlist[2] + "checkSDCardMount(pathlist[2])" +checkSDCardMount(pathlist[2]));
      
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

		Log.d(TAG, "MMIOTGTest  onCreate");
		super.onCreate(savedInstanceState);
        setContentView(R.layout.otg_test);
		setTitle(R.string.otg_test);

        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
		
        mOtg = (TextView) findViewById(R.id.otg);
        
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        Intent service = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
        this.bindService(service, mDefContainerConn,
                Context.BIND_AUTO_CREATE);

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
        Log.d(TAG, "Build.DISPLAY ="+Build.DISPLAY);
    }

    @Override
    protected void onResume() {
    	Log.d(TAG, "MMIOTGTest  onResume");
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
			mEditor.putInt(MMIOTGTest.this.getString(R.string.otg_test), Test_launcherActivity.PASS);
			mEditor.commit();

if(flag == false)
	//MMINativeLib.device_test_keypad_bk_turn_on_jni();
    finish();
		}
		
	};
		
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMIOTGTest.this.getString(R.string.otg_test), Test_launcherActivity.FAIL);
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
			ComponentName comp = new ComponentName(MMIOTGTest.this, MMIOTGTest.class);
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
			ComponentName comp = new ComponentName(MMIOTGTest.this, MMIOTGTest.class);
			intent.setComponent(comp);
			
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIOTGTest.this.getString(R.string.otg_test), mOTGMounted ? Test_launcherActivity.PASS : Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
}
