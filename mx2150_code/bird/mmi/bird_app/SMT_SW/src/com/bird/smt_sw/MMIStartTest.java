package com.bird.smt_sw;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import com.mediatek.common.featureoption.FeatureOption;
import android.content.Context;
import android.provider.Settings;
import android.net.wifi.WifiManager;
/*[BIRD][双mic测试][luye][20160723]begin*/
import android.media.AudioSystem;
/*[BIRD][双mic测试][luye][20160723]end*/

public class MMIStartTest extends MMINewActivity {

	private static final String TAG = "MMIStartTest";
	public static TestAdapter mAdapter; 
	public static final String TOKEN = "mmitest.token";
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
        private Button mStartButton;
	////
	LocationListener mLoclistener;
    LocationManager mLocationManager = null;
    private WifiManager wifiManager = null;  //
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
        Log.d(TAG, "@onCreate");
        setTitle(R.string.start_test);
        setContentView(R.layout.start_test);

        mStartButton = (Button) findViewById(R.id.start_test);
        
        mAdapter = new TestAdapter(this);
        
        preferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        editor = preferences.edit();
        //editor.clear();
        //editor.commit();
		for(int i = 0; i<=mAdapter.getTestItemSize() - 1; i++){
			editor.putInt(mAdapter.getTestItemName(i), 0);
            editor.commit();
		}
        mStartButton.setOnClickListener(mStartListener);

////
        mLoclistener = new LocationListener() {

            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
            	Log.d(TAG, "#onLocationChanged  location = "+location);
                
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };
        if (mLocationManager == null) {   
        	mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);   
        } 
        if(FeatureOption.BIRD_SMT_SW_GPS) {
            getLocation();
        }
        if (wifiManager == null) {  
            wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        }
        enableWifi();
/* 
        for(int i = mAdapter.getDeviceClassSize() - 1; i>= 0; i--){
            Intent intent = new Intent(MMIStartTest.this, mAdapter.getDeviceClassAtIndex(i));
            intent.putExtra(TOKEN, i);
            startActivity(intent);
        }
*/
	}
	
	private void enableWifi() {
		if (!wifiManager.isWifiEnabled() && (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING))
		{
			Log.d(TAG, "enable wifi");
			wifiManager.setWifiEnabled(true);
		}
	}
	/*[BIRD][双mic测试][luye][20160723]begin*/
	@Override
	protected void onResume() {
	    super.onResume();
	    int micTestResult = preferences.getInt("mic_test_result",0);
		//Log.i("luye","micTestResult = "+micTestResult);
		if(micTestResult != 0){
			editor.putInt("mic_test_result", 0);
			editor.commit();
			AudioSystem.setParameters("SET_MIC_CHOOSE=0");
		}
	
	}
	/*[BIRD][双mic测试][luye][20160723]end*/
	
    private void getLocation() {
    
        boolean isEnabled = Settings.Secure.isLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER);
		Log.d(TAG, "isEnabled = "+isEnabled);

		if(!isEnabled)
		    Settings.Secure.setLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER, true);
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
        
        mLocationManager.requestLocationUpdates(provider, 1000, 0, mLoclistener);
    }
    
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
        if (FeatureOption.BIRD_SMT_SW_GPS && (mLocationManager != null)) {   
        	mLocationManager.removeUpdates(mLoclistener);   
        }
	}
	
	private View.OnClickListener mStartListener = new View.OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
	        for(int i = mAdapter.getDeviceClassSize() - 1; i>= 0; i--){
	            Intent intent = new Intent(MMIStartTest.this, mAdapter.getDeviceClassAtIndex(i));
	            intent.putExtra(TOKEN, i);
	            startActivity(intent);
	        }
		}
	};
}
