package com.bird.smt_sw;

import java.io.IOException;
import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
//import com.mediatek.common.featureoption.FeatureOption;

public class MMITestBootupService extends Service {

	private static final String TAG = "MMITestReceiver";
	private MediaPlayer mediaPlayer;
	
	public static WifiManager wifiManager;  //private
	private BluetoothAdapter mBluetoothAdapter;
	
	private TestAdapter mAdapter;
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	public static final String TOKEN = "mmitest.token";
	private Handler mHandler;	
	private Runnable mRunnable;
	public static int mCount;
	private List<WifiConfiguration> wifiConfigurations;
    private int netID;
    private boolean connect = false;
	private NetworkInfo  mNetworkInfo;
	private State mState;
	private ConnectivityReceiver mConnectivityReceiver;
	private WifiReceiver mWifiReceiver;
	private boolean mScanResultIsAvailable;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mAdapter = new TestAdapter(this);
        preferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        editor = preferences.edit();
        Intent intent = new Intent(this, MMIStartTest.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(intent);//[BIRD_SMT_SW][工厂贴片软件，开机不自动进入测试界面][xuzhenguo]
        /*
        editor.clear();
        editor.commit();
        for(int i = mAdapter.getDeviceClassSize() - 1; i>= 0; i--){
            Intent intent = new Intent(this, mAdapter.getDeviceClassAtIndex(i));
            intent.putExtra(TOKEN, i);
            startActivity(intent);
        }
        */
		mediaPlayer = MediaPlayer.create(this, R.raw.test);
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		//audioManager.setMode(AudioManager.MODE_IN_CALL);
		//audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
		//audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
		//AudioManager.FLAG_PLAY_SOUND);
		audioManager.setMode(AudioManager.MODE_NORMAL);
		Log.d(TAG, "@onCreate MODE:" + audioManager.getMode());

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		
		//mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		//playMP3();
		mCount = 0;
		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		mediaPlayer.release();

		mediaPlayer = null;

		//recoverMode();
		
		unregisterReceiver(mConnectivityReceiver);
		mHandler.removeCallbacks(mRunnable);
		unregisterReceiver(mWifiReceiver);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Log.d(TAG, "onStart");
		//playMP3();//去掉 开机后播放一段音乐
		if (!wifiManager.isWifiEnabled() && (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING))
		{
			Log.d(TAG, "enable wifi");
			wifiManager.setWifiEnabled(true);
		}
		//if (!mBluetoothAdapter.isEnabled()){
		//	Log.d(TAG, "enable bluetooth");
		//	mBluetoothAdapter.enable();
		//}
                boolean isEnabled = Settings.Secure.isLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER);
		Log.d(TAG, "isEnabled = "+isEnabled);
			
		if(!isEnabled)
		    Settings.Secure.setLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER, true);
		/*
        List<ScanResult> netList = wifiManager.getScanResults();
        if (netList == null) {
            Log.d(TAG,"scan results are null");
            // if no scan results are available, start active scan
            wifiManager.startScanActive();
            mScanResultIsAvailable = false;
            long startTime = System.currentTimeMillis();
            while (!mScanResultIsAvailable) {
                if ((System.currentTimeMillis() - startTime) > 50*1000) {
                    return;
                }
                // wait for the scan results to be available
                synchronized (this) {
                    // wait for the scan result to be available
                    try {
                        this.wait(6*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if ((wifiManager.getScanResults() == null) ||
                            (wifiManager.getScanResults().size() <= 0)) {
                        continue;
                    }
                    mScanResultIsAvailable = true;
                }
            }
        }
        */
		wifiManager.startScan();////startScanActive();
		mHandler = new Handler();
		mRunnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.d(TAG, "@ run mCount =" +mCount);
				if(mCount <= 9){
					if(!connect){
					        Log.d(TAG, "@ run mCount  !connect");
						searchNet();
						mHandler.postDelayed(mRunnable, 5000);
					}else{
					        Log.d(TAG, "@ run mCount  connect");
						wifiManager.disableNetwork(netID);
						wifiManager.disconnect();
						connect = false;//xuzhenguo
						mHandler.postDelayed(mRunnable, 1000);
					}
				}
				//mHandler.postDelayed(mRunnable, 6000);
			}
		};
	
		//mHandler.postDelayed(mRunnable, 6000);
		
		mConnectivityReceiver = new ConnectivityReceiver();
        registerReceiver(mConnectivityReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        
        mWifiReceiver = new WifiReceiver();
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		iFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		iFilter.addAction(WifiManager.WIFI_SCAN_AVAILABLE);
		registerReceiver(mWifiReceiver, iFilter);

	}


	private void searchNet()
	{
		connect = true;
		int level , level_max = -200;
		ScanResult wifi_co = null;
			
		while(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING){
			try {
				Thread.currentThread();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		wifiManager.startScan();////startScanActive();
		List<ScanResult> list = wifiManager.getScanResults();
		Log.d(TAG, "--list  ="+list);
		if (null != list){//(wifiManager.startScan()){
			wifiConfigurations = wifiManager.getConfiguredNetworks();
			//List<ScanResult> list = wifiManager.getScanResults();

			
			Log.d(TAG, "--list size ="+list.size());
			Log.d(TAG, "! wifiConfigurations size ="+wifiConfigurations.size());
			for (ScanResult wifi : list)
			{
	
				level = wifi.level ;
				if(level > level_max && level != 0){
					level_max = level;
					wifi_co = wifi;
					//Log.d(TAG, "~ wifi_co.ssid ="+ wifi_co.SSID);
				}
				
				Log.d(TAG, "11wifi.level "+level + "  wifi.SSID "+wifi.SSID);
				
				if(false){//(wifi.SSID.equalsIgnoreCase("nbbsw_test")){
					WifiConfiguration config = new WifiConfiguration();  
			         config.allowedAuthAlgorithms.clear();       
			         config.allowedGroupCiphers.clear();       
			         config.allowedKeyManagement.clear();       
			         config.allowedPairwiseCiphers.clear();      
			         config.allowedProtocols.clear();
					 config.SSID =  "\"" + wifi.SSID + "\"";  //"\"" + wifi.SSID + "\""
					 config.preSharedKey = "\""+"nbbswtest"+"\""; //指定密码   "\""+"nbbswtest"+"\""
					
					 config.hiddenSSID = true;  
					 
					 config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);  
					 config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP); 
					 config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
					 config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);  
					 config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP); 
					 config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
					 config.allowedProtocols.set(WifiConfiguration.Protocol.WPA); 
					 config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
					 config.status = WifiConfiguration.Status.ENABLED; 
					 netID = wifiManager.addNetwork(config); 
					 boolean bRet = wifiManager.enableNetwork(netID, true); 
					 //wifiManager.reconnect();

					 Log.d(TAG, "netID ="+netID);
					 Log.d(TAG, "enableNetwork bRet = "+bRet+" config.SSID= "+config.SSID+" config.preSharedKey="+config.preSharedKey);
				}
				
			}
			if(null != wifi_co){
			 WifiConfiguration config = new WifiConfiguration(); 
		         config.allowedAuthAlgorithms.clear();       
		         config.allowedGroupCiphers.clear();       
		         config.allowedKeyManagement.clear();       
		         config.allowedPairwiseCiphers.clear();      
		         config.allowedProtocols.clear();
		         config.SSID =  "\"" + wifi_co.SSID + "\"";//"\"" + wifi.SSID + "\"";
                        // config.wepKeys[0] = ""; 
                         config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
                         //config.wepTxKeyIndex = 0; 
               
		         netID = wifiManager.addNetwork(config); 
		         boolean bRet = wifiManager.enableNetwork(netID, true); 
                         wifiManager.reconnect();
                         Log.d(TAG, "netID ="+netID);
			 Log.d(TAG, "enableNetwork bRet = "+bRet+" config.SSID= "+config.SSID+" config.preSharedKey="+config.preSharedKey);

		       }
			//Log.d(TAG, "~ wifi_co.ssid ="+ wifi_co.SSID);
		}
	}
	

	   private class ConnectivityReceiver extends BroadcastReceiver {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            Log.d(TAG,"ConnectivityReceiver: onReceive() is called with " + intent);
	            String action = intent.getAction();
	            if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
	                Log.d(TAG, "onReceive() called with " + intent);
	                return;
	            }

	            boolean noConnectivity =
	                intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

	            if (noConnectivity) {
	                mState = State.DISCONNECTED;
	                //connect = false;//
	               // mHandler.postDelayed(mRunnable, 1000);//xuzhenguo
	            } else {
	                mState = State.CONNECTED;
	            }
                Log.d(TAG, "@ mState ="+mState);
	            mNetworkInfo = (NetworkInfo)
	                intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
	            
	            if((mState == State.CONNECTED) && (mNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI)){
	            	mCount ++;
	            	//connect = true;//
	            	//mHandler.postDelayed(mRunnable, 1000);//xuzhenguo
	            	if(mCount > 9){
	            		mHandler.removeCallbacks(mRunnable);
	            	}
	            }

	        }
	    }
	   
	   private class WifiReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Log.d(TAG, "WifiReceiver onReceive action= "+action);
			if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)){
				Log.d(TAG, "SCAN_RESULTS_AVAILABLE_ACTION in");
				//mHandler.postDelayed(mRunnable, 6000);
			}else if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)){
				Log.d(TAG, "WIFI_STATE_CHANGED_ACTION in");
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
				switch(wifiState){
				case WifiManager.WIFI_STATE_ENABLED:
					Log.d(TAG, "$$WIFI  enabled");
					wifiManager.startScan();
					if(FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH)//小板软件不要 WIFI 蓝牙测试
					mHandler.postDelayed(mRunnable, 25000);//40000

					break;
				case WifiManager.WIFI_STATE_DISABLED:
					Log.d(TAG, "$$WIFI  close");
					mHandler.removeCallbacks(mRunnable);
					
					break;
					
				}
			}
		}
		   
	   }

	private void playMP3(){

		Log.d(TAG, "playMP3");
		if (mediaPlayer.isPlaying())
		{

			mediaPlayer.reset();// ����Ϊ��ʼ״̬

		}

		try
		{
			mediaPlayer.prepare();

		} catch (IllegalStateException e)
		{

			// TODO Auto-generated catch block

			e.printStackTrace();

		} catch (IOException e)
		{

			// TODO Auto-generated catch block

			e.printStackTrace();

		}// ����

		mediaPlayer.start();// ��ʼ��ָ�����

		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
		{// ��������¼�

					@Override
					public void onCompletion(MediaPlayer arg0)
					{
						Log.d(TAG, "onCompletion");

						mediaPlayer.stop();

						mediaPlayer.release();
						//recoverMode();

					}

				});

		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener()
		{// �������¼�

					@Override
					public boolean onError(MediaPlayer player, int arg1,

					int arg2)
					{
						Log.d(TAG, "onError");

						mediaPlayer.release();
						//recoverMode();

						return false;

					}

				});
	}
	
	private void recoverMode(){

		Log.d(TAG, "@recoverMode");

		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		audioManager.setMode(AudioManager.MODE_NORMAL);
	}
	
}
