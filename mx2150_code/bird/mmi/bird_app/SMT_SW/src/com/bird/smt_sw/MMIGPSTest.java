package com.bird.smt_sw;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;

import android.location.Location;

import android.location.LocationListener;

import android.location.LocationManager;

import android.os.Bundle;

import android.view.View;
import android.view.KeyEvent;
import android.widget.TextView;

import android.widget.Toast;

import android.widget.Button;

import android.provider.Settings;

import android.util.Log;

public class MMIGPSTest extends MMINewActivity {

    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private int req_token;
    TextView gps_result_tv , satellites_tv;
    LocationListener mLoclistener;
    LocationManager mLocationManager = null;
    private static final String TAG = "DEVICE_TEST_GPS";
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private boolean isPass;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps);
        setTitle(R.string.gps);
        
        isPass = false;

        gps_result_tv = (TextView) this.findViewById(R.id.gps_result);
        satellites_tv = (TextView) findViewById(R.id.tv_satellites);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        //resetButton = (Button) findViewById(R.id.reset);

        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        
        mLoclistener = new LocationListener() {

            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
            	Log.d(TAG, "#onLocationChanged  location = "+location);
               // updateToNewLocation(location);
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
       // resetButton.setOnClickListener(mRestdButtonHandler);		
    }
    
	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {

			mEditor.putInt(MMIGPSTest.this.getString(R.string.gps), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};

	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {

		public void onClick(View v) {

		        mEditor.putInt(MMIGPSTest.this.getString(R.string.gps), Test_launcherActivity.FAIL);
			mEditor.commit();
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
			//Intent intent = new Intent();
			//ComponentName comp = new ComponentName(MMIGPSTest.this, MMIGPSTest.class);
			//intent.setComponent(comp);
			
			//startActivity(intent);
			//finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMIGPSTest.this.getString(R.string.gps), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
    
    private void updateToNewLocation(Location location) {

        gps_result_tv = (TextView) this.findViewById(R.id.gps_result);
        if (location != null) {
            double  latitude = location.getLatitude();
            double longitude= location.getLongitude();  
            String latitude_str=latitude+"";
            String longitude_str=longitude+"";          

            gps_result_tv.setText(getString(R.string.longitude)
            		+longitude_str.substring(0, 8)
            		+"\n"
            		+getString(R.string.latitude)
            		+latitude_str.substring(0, 7));
        } else {
            //gps_result_tv.setText(" ");
        }
    } 
    
    protected void onResume() {

        super.onResume();
        //openGPSSettings();
        registerListener();
        getLocation();
        mLocationManager.addGpsStatusListener(statusListener);//xuzhenguo
    }

    @Override
    protected void onPause() {

        super.onPause();

        unregisterListener();
        mLocationManager.removeGpsStatusListener(statusListener);//xuzhenguo
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
        //updateToNewLocation(location);
        
        mLocationManager.requestLocationUpdates(provider, 1000, 0, mLoclistener);
    }
    
	private List<GpsSatellite> numSatelliteList = new ArrayList<GpsSatellite>(); // 卫星信号   
    
    private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {  
        public void onGpsStatusChanged(int event) { // GPS状态变化时的回调，如卫星数   
        	Log.i(TAG, "!GpsStatus Changed");
            LocationManager locationManager = (LocationManager)MMIGPSTest.this.getSystemService(Context.LOCATION_SERVICE);  
            GpsStatus status = locationManager.getGpsStatus(null); //取当前状态   
            String satelliteInfo = updateGpsStatus(event, status);  
            Log.i(TAG, "!GpsStatus Changed satelliteInfo ="+satelliteInfo);
            //satellites_tv.setText(null);  //xuzhenguo
            //satellites_tv.setText(satelliteInfo);  
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
            if(!isPass && numSatelliteList.size() >= 2){
                isPass = true;
                mEditor.putInt(MMIGPSTest.this.getString(R.string.gps), Test_launcherActivity.PASS);
		mEditor.commit();
		
		gps_result_tv.setText(R.string.gps_test_pass);
		gps_result_tv.setTextColor(getResources().getColor(R.drawable.GREEN));
            }
        }  
        return sb2.toString();  
    }  
}
