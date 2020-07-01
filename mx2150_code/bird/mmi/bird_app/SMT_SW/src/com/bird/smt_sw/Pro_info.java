package com.bird.smt_sw;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
//import android.os.IPowerManager;
import android.os.Message;
//import android.os.Power;
//import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
//import com.mediatek.common.featureoption.FeatureOption;
//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
import com.android.fmradio.FmNative;
import android.media.MediaPlayer;
import android.media.AudioManager;
//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END

public class Pro_info extends MMINewActivity implements OnClickListener{

	private static final String TAG = "Pro_info";
    private Button correctButton;
    private Button resetButton;
    private TextView tAccelerator;
    private TextView tWifi;
    private TextView tBluetooth;
    private TextView tProximity;
    private TextView tHideProximity;
    private TextView tGps;
    private boolean flag1 = false;
    private boolean flag2 = false;
    private boolean flag3 = false;
    private boolean flag4 = false;
    private boolean flag5 = false;
    
    private Timer timer;
    private BluetoothAdapter mBluetoothAdapter;
    
    private  SensorManager mSensorManager;  
    private  SensorEventListener mSensorListener;  
    private  Sensor mSensor;  
    private int mRate = SensorManager.SENSOR_DELAY_NORMAL;  
    private float value = -999f;
    
    private  SensorManager sensorManager;  
    private  SensorEventListener sensorListener;  
    private  Sensor sensor;  
    
    private WifiManager wifiManager;
    private List<WifiConfiguration> wifiConfigurations;
    
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
    private int mCount;
    //PowerManager pm;
    
    LocationListener mLoclistener;
    LocationManager mLocationManager = null;
    public static int mNumSatelates;
    //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
    private int FIXED_STATION_FREQ = 962;//918;
    private MediaPlayer mMP = null;
    public float mFreq = (float)88.0;
    boolean isStartChecFM = false;
    private TextView mFMCheckStatus;
    //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pro_info);
		setTitle(R.string.first_test);
		mCount = 0;
		//pm = (PowerManager) getSystemService(POWER_SERVICE);
		
        correctButton = (Button) findViewById(R.id.correct);
        resetButton = (Button) findViewById(R.id.reset);
        
        tAccelerator = (TextView) findViewById(R.id.Accelerator);
        tWifi = (TextView) findViewById(R.id.Wifi);
        tBluetooth = (TextView) findViewById(R.id.Bluetooth);
        tProximity = (TextView) findViewById(R.id.Proximity);
        tHideProximity = (TextView) findViewById(R.id.Hide_Proximity);
        tGps = (TextView) findViewById(R.id.Gps);
        
        if(!FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH){//小板软件不要 WIFI 蓝牙测试
            tAccelerator.setVisibility(View.INVISIBLE);
            tWifi.setVisibility(View.INVISIBLE);
            tBluetooth.setVisibility(View.INVISIBLE);
        }
        if(!FeatureOption.BIRD_SMT_SW_PROXIMITY){
            tProximity.setVisibility(View.INVISIBLE);
            tHideProximity.setVisibility(View.INVISIBLE);
        }
        if(!FeatureOption.BIRD_SMT_SW_GPS)
            tGps.setVisibility(View.INVISIBLE);
        correctButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        
        tAccelerator.append(getString(R.string.wait_test));
        tWifi.append(getString(R.string.wait_test));
        tBluetooth.append(getString(R.string.wait_test));
        tProximity.append(getString(R.string.wait_test));
        
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null){   
            // 设备不支持蓝牙    
    		if (!mBluetoothAdapter.isEnabled()){
    			Log.d(TAG, "enable bluetooth");
    			mBluetoothAdapter.enable();
    			
    		}
        }   
		
		initProximity();
		
		initAccelerator();

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled() && (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING))
		{
			Log.d(TAG, "enable wifi");
			wifiManager.setWifiEnabled(true);
		}
		if(FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH)//小板软件不要 WIFI 蓝牙测试
		searchNet();
        timer = new Timer();
        timer.schedule(mTask, 30000);
        
        mLoclistener = new LocationListener() {

            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
            	Log.d(TAG, "#onLocationChanged  location = "+location);
                
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        if(FeatureOption.BIRD_SMT_SW_GPS)
        getGpsTestResult();
        //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
		mFMCheckStatus = (TextView) findViewById(R.id.FMCheckStatus);
		String fmString = getResources().getText(R.string.fm).toString() + getResources().getText(R.string.wait_test).toString();	
		mFMCheckStatus.setText(fmString);
		if(!FeatureOption.BIRD_SMT_SW_FM){
			mFMCheckStatus.setVisibility(View.GONE);	 			
		}
		//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
    }
	
	private void initAccelerator() {
		// TODO Auto-generated method stub
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
					if(mCount <= 1){
						Log.d(TAG, "accelerator onSensorChanged");

		                flag1 = true;
						Message msg = new Message();
						msg.what = 1;
						mHandler.sendMessageDelayed(msg, 2000);
						mCount++;
					}
				}

			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub

			}
		};
		boolean suc = sensorManager.registerListener(sensorListener, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		Log.d(TAG, "initAccelerator: "+ suc);
	}

	private void initProximity() {
		// TODO Auto-generated method stub
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mSensorListener = new SensorEventListener()
		{
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy)
			{
			}

			@Override
			public void onSensorChanged(SensorEvent event)
			{

				Log.d(TAG, "onSensorChanged");

				if (event.sensor.getType() != Sensor.TYPE_PROXIMITY)
					return;
				
				value = event.values[SensorManager.DATA_X];
				Log.d(TAG, "values=" + value);
				if (value < 0.5)				{
	                flag4 = true;
					Message msg = new Message();
					msg.what = 4;
					mHandler.sendMessageDelayed(msg, 1000);
				}
			}
		};

		mSensorManager.registerListener(mSensorListener, mSensor, mRate);	
		
		
	}

	private void searchNet(){

		Log.d(TAG, "@ searchNet");
		StringBuilder acessWifi = new StringBuilder("");
		StringBuilder configuredWifi = new StringBuilder("");
		
		while(wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING){
			try {
				Thread.currentThread();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(wifiManager.startScan()){
			wifiConfigurations = wifiManager.getConfiguredNetworks();
			List<ScanResult> list = wifiManager.getScanResults();

			
			Log.d(TAG, "--list size ="+list.size());
			Log.d(TAG, "! wifiConfigurations size ="+wifiConfigurations.size());
			if(list.size() >= 2){
				
	            flag2 = true;
				Message msg = new Message();
				msg.what = 2;
				mHandler.sendMessageDelayed(msg, 500);
			} else {
				Log.d(TAG, "handler set flag2");
				mEditor.putInt(Pro_info.this.getString(R.string.wifi_test), Test_launcherActivity.FAIL);
				mEditor.commit();
				tWifi.setText(R.string.wifi_test);
				
				tWifi.append(getString(R.string.item_fail));					
				tWifi.setTextColor(getResources().getColor(R.drawable.RED));
			}
			for (ScanResult wifi : list)
			{
		
				acessWifi.append(wifi.SSID+"\n");
				
			}


			// �õ��Ѿ����ӵ�����
			for (WifiConfiguration wifiConfiguration : wifiConfigurations)
			{

				configuredWifi.append(wifiConfiguration.SSID + "\n");

			}
		}

    }
	
	private TimerTask mTask = new TimerTask() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, "timetask  run");
			//Message msg1 = new Message();
			//msg1.what = 3;
			//mHandler.sendMessage(msg1);
			mBluetoothAdapter.cancelDiscovery();
		}
	};

	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			//Log.d(TAG, "msg.what = "+ msg.what+"flag3 ="+ flag3);
			switch (msg.what) {
			case 1:
				if(flag1){
					Log.d(TAG, "handler set flag1");
					mEditor.putInt(Pro_info.this.getString(R.string.accelerator_test), Test_launcherActivity.PASS);
					mEditor.commit();
					tAccelerator.setText(R.string.accelerator_test);
					
					tAccelerator.append(getString(R.string.item_pass));					
					tAccelerator.setTextColor(getResources().getColor(R.drawable.GREEN));
				}
				break;

			case 2:
				if(flag2){
					Log.d(TAG, "handler set flag2");
					mEditor.putInt(Pro_info.this.getString(R.string.wifi_test), Test_launcherActivity.PASS);
					mEditor.commit();
					tWifi.setText(R.string.wifi_test);
					
					tWifi.append(getString(R.string.item_pass));					
					tWifi.setTextColor(getResources().getColor(R.drawable.GREEN));
				}
				break;
			case 3:
				if(flag3){
					Log.d(TAG, "handler set flag3");
					mEditor.putInt(Pro_info.this.getString(R.string.bluetooth_test), Test_launcherActivity.PASS);
					mEditor.commit();
					tBluetooth.setText(R.string.bluetooth_test);
					
					tBluetooth.append(getString(R.string.item_pass));					
					tBluetooth.setTextColor(getResources().getColor(R.drawable.GREEN));
					
				}
				
				break;
			case 4:
				if(flag4){
					Log.d(TAG, "handler set flag4");
					mEditor.putInt(Pro_info.this.getString(R.string.proximity_test), Test_launcherActivity.PASS);
					mEditor.commit();
					tProximity.setText(R.string.proximity_test);
					tProximity.append(getString(R.string.item_pass));					
					tProximity.setTextColor(getResources().getColor(R.drawable.GREEN));
					tHideProximity.setVisibility(View.INVISIBLE);
				}
				break;
			case 5:
				if(!flag5 && mNumSatelates >= 2){
					Log.d(TAG, "handler set flag5");
	            	                flag5 = true;
	                                mEditor.putInt(Pro_info.this.getString(R.string.gps), Test_launcherActivity.PASS);
			                mEditor.commit();
			
			                tGps.setText(R.string.gps_test_pass);
			                tGps.setTextColor(getResources().getColor(R.drawable.GREEN));
				} else if(!flag5 && mNumSatelates <2){
	            	                flag5 = true;
	                                mEditor.putInt(Pro_info.this.getString(R.string.gps), Test_launcherActivity.FAIL);
			                mEditor.commit();
			
			                tGps.setText(R.string.gps_test_fail);
			                tGps.setTextColor(getResources().getColor(R.drawable.RED));
				}
				break;
		      //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
			  case 6:
			    startCheckFm();
			    break;
			  //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
			}
			super.handleMessage(msg);
			
		}
		
	};
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.correct:
			//mEditor.putInt(Pro_info.this.getString(R.string.Pro_info), Test_launcherActivity.PASS);
			//mEditor.commit();
			finish();
			//pm.setBacklightBrightness(Power.BRIGHTNESS_OFF);
			/*
	        try {
	            IPowerManager power = IPowerManager.Stub.asInterface(
	                    ServiceManager.getService("power"));
	            if (power != null) {
	                power.setBacklightBrightness(Power.BRIGHTNESS_OFF);
	            }
	        } catch (RemoteException doe) {
	            
	        } 
	        */
			break;
			
		case R.id.reset:
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(Pro_info.this, Pro_info.class);
			intent.setComponent(comp);

			startActivity(intent);
			finish();
			break;

    	}
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		timer.cancel();
		//mBluetoothAdapter.cancelDiscovery();
		
		mSensorManager.unregisterListener(mSensorListener);
		sensorManager.unregisterListener(sensorListener);
	}

	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unregisterReceiver(mReceiver);
		//unregisterReceiver(mWifiStateChangedReceiver);
		//mHandler.removeCallbacks(searchWifi);
		mHandler.removeCallbacks(discoverBluetooth);
		mBluetoothAdapter.cancelDiscovery();//1118
		if(FeatureOption.BIRD_SMT_SW_GPS)
		unregisterListener();
		mLocationManager.removeGpsStatusListener(statusListener);
		//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
        if(FeatureOption.BIRD_SMT_SW_FM && isStartChecFM){
	       stopCheckFm();
        }
		//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(TAG, "@ Pro_info onresume");
		
		mHandler.postDelayed(discoverBluetooth, 3000); 
		//mBluetoothAdapter.startDiscovery();		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);	
		
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); 
		registerReceiver(mReceiver, filter); 

		//IntentFilter iFilter = new IntentFilter();
		//iFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		//registerReceiver(mWifiStateChangedReceiver, iFilter);
                registerListener();
                if(FeatureOption.BIRD_SMT_SW_GPS)
                getLocation();
                mLocationManager.addGpsStatusListener(statusListener);
        //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN  
        if(FeatureOption.BIRD_SMT_SW_FM){
            isStartChecFM = true;
			Message msg = new Message();
			msg.what = 6;
			mHandler.sendMessageDelayed(msg, 1000);
            //startCheckFm();
        }
		//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
	}
	//[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]BEGIN
     private void startCheckFm(){

		boolean openresult = FmNative.openDev();

		Log.d(TAG, "openresult=" + openresult );

		boolean powerup = FmNative.powerUp((float)FIXED_STATION_FREQ/10);

		Log.d(TAG, "powerup=" + powerup );


		mMP = new MediaPlayer();
		try {
			mMP.setDataSource("THIRDPARTY://MEDIAPLAYER_PLAYERTYPE_FM");
            mMP.setAudioStreamType(AudioManager.STREAM_MUSIC);// caoyuangui change AudioManager.STREAM_FM to AudioManager.STREAM_MUSIC
			mMP.prepare();
		} catch (IllegalArgumentException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mMP.start();
	
		Log.d(TAG, "mFreq=" +mFreq );
		boolean turnResult = FmNative.tune(mFreq);
		Log.d(TAG, "turnResult=" + turnResult );

		//Log.d(TAG, "readRssi=" + FmNative.readRssi());	

		boolean fmCheckResult = openresult && powerup && turnResult;
		
		if(fmCheckResult){
			String fmStringOK = getResources().getText(R.string.fm).toString() + getResources().getText(R.string.item_pass).toString();
			mFMCheckStatus.setTextColor(getResources().getColor(R.drawable.GREEN));		
			mFMCheckStatus.setText(fmStringOK);	
			mEditor.putInt(Pro_info.this.getString(R.string.fm), Test_launcherActivity.PASS);
			mEditor.commit();

		}else{
			String fmStringError = getResources().getText(R.string.fm).toString() + getResources().getText(R.string.item_fail).toString();
			mFMCheckStatus.setTextColor(getResources().getColor(R.drawable.RED));		
			mFMCheckStatus.setText(fmStringError);	
			mEditor.putInt(Pro_info.this.getString(R.string.fm), Test_launcherActivity.FAIL);
			mEditor.commit();
		}
       }

       private void stopCheckFm(){
        if(mMP != null && mMP.isPlaying()) {
            mMP.stop();
        }
        FmNative.powerDown(0);
        FmNative.closeDev();
       }
       //[BIRD][FM测试暂时改为c院 自动判断方式，放到wifi 蓝牙测试项一起][xuzhenguo][20150717]END
	private Runnable discoverBluetooth = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(true){//(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON){
				Log.d(TAG, "#discoverBluetooth");
				mBluetoothAdapter.startDiscovery();
			}
		}
	};
	private boolean isFound =false;
	private final BroadcastReceiver mReceiver = new BroadcastReceiver()
	{

		public void onReceive(Context context, Intent intent)
		{

			String action = intent.getAction();
            Log.d(TAG, "@ receiver action="+action);
			// When discovery finds a device

			if (BluetoothDevice.ACTION_FOUND.equals(action))
			{
				Message msg1 = new Message();
				msg1.what = 3;
				mHandler.sendMessage(msg1);
				flag3 = true;
				isFound = true;
				mBluetoothAdapter.cancelDiscovery();

			}else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				
				Log.d(TAG, "BluetoothAdapter.ACTION_DISCOVERY_FINISHED");
				if(!isFound){
						BluetoothDevice device = intent

				.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(null == device){
					Log.d(TAG, "@null == device");
					mEditor.putInt(Pro_info.this.getString(R.string.bluetooth_test), Test_launcherActivity.FAIL);
					mEditor.commit();
					tBluetooth.setText(R.string.bluetooth_test);
					
					tBluetooth.append(getString(R.string.item_fail));					
					tBluetooth.setTextColor(getResources().getColor(R.drawable.RED));
				}
				}

			}

		}

	};

	private Runnable searchWifi = new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, "#run");
			
			searchNet();
		}
	};
	
	private BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String mAction = intent.getAction();
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(mAction)){
				
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
				Log.d(TAG, "$$wifiState ="+wifiState);
				switch (wifiState) {
				case WifiManager.WIFI_STATE_DISABLED:
					Log.d(TAG, "$$close");
					mHandler.removeCallbacks(searchWifi);
					
					break;
				case WifiManager.WIFI_STATE_DISABLING:
					
					break;
				case WifiManager.WIFI_STATE_ENABLED:

					wifiManager.startScan();
					mHandler.postDelayed(searchWifi, 6000);
					//searchNet();
					break;
					
				case WifiManager.WIFI_STATE_ENABLING:
					
					break;
					
				case WifiManager.WIFI_STATE_UNKNOWN:
					break;
				
				}
			}
			
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
			ComponentName comp = new ComponentName(Pro_info.this, Pro_info.class);
			intent.setComponent(comp);

			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

    private void registerListener() {   

        if (mLocationManager == null) {   
        	mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);   
        }            
    }  

    private void unregisterListener() {   

        if (mLocationManager != null) {   
        	mLocationManager.removeUpdates(mLoclistener);   
        }   
    }
    
    private void getLocation() {

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_HIGH); 

        String provider = mLocationManager.getBestProvider(criteria, true);      

        if(provider==null)
        {
        	return;
        }
        
        Log.d(TAG, "@provider = "+provider);
        Location location = mLocationManager.getLastKnownLocation(provider); 
        Log.d(TAG, "@@location = "+location);
        
        mLocationManager.requestLocationUpdates(provider, 1000, 0, mLoclistener);
    }
    
	private List<GpsSatellite> numSatelliteList = new ArrayList<GpsSatellite>(); // 卫星信号   
    
    private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {  
        public void onGpsStatusChanged(int event) { // GPS状态变化时的回调，如卫星数   
        	Log.i(TAG, "!GpsStatus Changed");
            LocationManager locationManager = (LocationManager)Pro_info.this.getSystemService(Context.LOCATION_SERVICE);  
            GpsStatus status = locationManager.getGpsStatus(null); //取当前状态   
            String satelliteInfo = updateGpsStatus(event, status);  
            Log.i(TAG, "!GpsStatus Changed satelliteInfo ="+satelliteInfo);
            //satellites_tv.setText(null);  
           // satellites_tv.setText(satelliteInfo);  
        }  
    };  
  
    private String updateGpsStatus(int event, GpsStatus status) {  
        StringBuilder sb2 = new StringBuilder("");  
        String num = getResources().getString(R.string.num_satellites);
        if (status == null) {  
            //sb2.append("搜索到卫星个数：" +0);  
            sb2.append(num +0);
        } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {  
            int maxSatellites = status.getMaxSatellites();  
            Iterator<GpsSatellite> it = status.getSatellites().iterator();  
            numSatelliteList.clear();  
            int count = 0;  
            while (it.hasNext() && count <= maxSatellites) {  
                GpsSatellite s = it.next();  
                numSatelliteList.add(s);  
                count++;  
            }  
            //sb2.append("搜索到卫星个数：" + numSatelliteList.size());  
            sb2.append(num + numSatelliteList.size());
            mNumSatelates = numSatelliteList.size();
            if(mNumSatelates >= 2) {
		Message msg = new Message();
		msg.what = 5;
		mHandler.sendMessageDelayed(msg, 2000);
            }
        }  
        return sb2.toString();  
    }  
    
	private void getGpsTestResult() {
		// TODO Auto-generated method stub
		mNumSatelates = 0;
		Message msg = new Message();
		msg.what = 5;
		mHandler.sendMessageDelayed(msg, 20000);
	}
	
}
