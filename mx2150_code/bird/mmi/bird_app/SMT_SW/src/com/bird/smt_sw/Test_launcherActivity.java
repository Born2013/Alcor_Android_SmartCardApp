package com.bird.smt_sw;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.util.Log;

import android.telephony.TelephonyManager;
//import com.mediatek.common.telephony.ITelephonyEx;
//import com.android.internal.telephony.Phone;
//import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.os.SystemProperties;

public class Test_launcherActivity extends MMINewActivity {
    /** Called when the activity is first created. */
	
	private static final String TAG = "TEST_LAUNCHER";
	private TestAdapter mAdapter;
	public static final String TOKEN = "mmitest.token";
	private SharedPreferences preferences;
	private SharedPreferences.Editor editor;
	
	public static final int PASS = 1;
	public static final int FAIL = 2;
	
	private TextView result;
	private TextView conneted_num;
	private TextView count_view;
	private TextView write_success_hint;
	private TextView barcode_view;
			
	private TelephonyManager mTelMgr;
    private ITelephonyEx mTelEx;
	private String newBarcode = " ";
	private static final int BARCODE_FLAG_POS = 11;
	private boolean isNewBarcode;
	private Handler mHandler = new Handler();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(R.string.test_report);
        result = (TextView) findViewById(R.id.result);
        conneted_num = (TextView) findViewById(R.id.wifi_connet_num);
        count_view = (TextView) findViewById(R.id.count);
        if(!FeatureOption.BIRD_SMT_SW_ACCELERATOR_WIFI_BLUETOOTH){//小板软件不要 WIFI 蓝牙测试
            conneted_num.setVisibility(View.INVISIBLE);
            count_view.setVisibility(View.INVISIBLE);
        }
        write_success_hint = (TextView) findViewById(R.id.write_success);
        barcode_view = (TextView) findViewById(R.id.barcode_string);
        
        mTelMgr = (TelephonyManager) this.getSystemService(this.TELEPHONY_SERVICE);
		mTelEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
		
	newBarcode = getNewBarcode();
	Log.d(TAG, "newBarcode = "+newBarcode);
        //final Intent intent_ser = new Intent("com.bird.smt_sw.MMITestBootupService");
        //intent_ser.setAction("com.bird.smt_sw.MMITestBootupService");
       // startService(intent_ser);
        
        Log.d(TAG, "@onCreate");
        preferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        editor = preferences.edit();

        /*//xuzhenguo as last activity
        mAdapter = new TestAdapter(this);
        
        preferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        editor = preferences.edit();
        editor.clear();
        editor.commit();
        
        for(int i = mAdapter.getDeviceClassSize() - 1; i>= 0; i--){
            Intent intent = new Intent(Test_launcherActivity.this, mAdapter.getDeviceClassAtIndex(i));
            intent.putExtra(TOKEN, i);
            startActivity(intent);
        }       
        */
        IntentFilter iFilter = new IntentFilter("com.bird.SMT.SET_BARCODE");
        registerReceiver(mReceiver, iFilter);
    }

        private String getNewBarcode(){
            
            String mBarCode = null;
            ////String mBarCode = mTelMgr.getSN();
            //try {
		        //mBarCode = mTelEx.getSerialNumber();
            //} catch (RemoteException ex) {
            //    ex.printStackTrace();        
            //    mBarCode = null;
            //}
            mBarCode = SystemProperties.get("gsm.serial");
            if (mBarCode == null) {
                Log.d(TAG, "mBarCode == null");
                mBarCode = " ";
                //return;
            }
            char[] t = mBarCode.toCharArray();
            if(BARCODE_FLAG_POS <= mBarCode.length() - 1)
	        t[BARCODE_FLAG_POS]=(char) (t[BARCODE_FLAG_POS]+1);
		
	    return new String(t);
        }
        
        private void writeBarcode(String barcode){
        
                String snString1[] = new String[2]; 
		snString1[0]= "AT+EGMR=1,5" + "," + "\"" + barcode + "\"";
		snString1[1]="+EGMR:";
		Log.d(TAG, "snString1[0]="+snString1[0]);
		Log.d(TAG, "snString1[1]="+snString1[1]);
		mTelMgr.invokeOemRilRequestString(snString1, 0);//Phone.GEMINI_SIM_1
        }
        
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		int val;
		boolean isPass = true;
		StringBuffer sb = new StringBuffer("");
		Log.d(TAG, "@MMITestBootupService mCount ="+MMITestBootupService.mCount);
		StringBuffer count = new StringBuffer(" ");
		count.append(MMITestBootupService.mCount);
		//conneted_num.append(count);
		count_view.setText(count);
		
        isNewBarcode = preferences.getBoolean("isNewBarcode", false);
        Log.d(TAG, "isNewBarcode ="+isNewBarcode);

        //[BIRD][所有测试均通过会导致停止运行][qianliliang][20160804] BEGIN
        if (MMIStartTest.mAdapter != null) {
            for(int i = 0; i<=MMIStartTest.mAdapter.getTestItemSize() - 1; i++){
                val = preferences.getInt(MMIStartTest.mAdapter.getTestItemName(i), 0);
                Log.d(TAG, "@test val ="+val);
                if(val == FAIL){
                    isPass = false;
                    sb.append(MMIStartTest.mAdapter.getTestItemName(i));
                    sb.append(":");
                    sb.append(getString(R.string.error));
                    sb.append("\n");
                }else if(val == 0){
                    Log.d(TAG, "@@val == 0");
                    isPass = false;
                    sb.append(MMIStartTest.mAdapter.getTestItemName(i));
                    sb.append(":");
                    sb.append(getString(R.string.error));
                    sb.append("\n");
                }
            }
        }
        //[BIRD][所有测试均通过会导致停止运行][qianliliang][20160804] END

		Log.d(TAG, "@sb="+sb.toString());
		if(true == isPass){
			result.setText(R.string.all_pass);
			
		        //if(!isNewBarcode){
		       //     writeBarcode(newBarcode);
		       // }
		       // editor.putBoolean("isNewBarcode", true);
		       // editor.commit();
		} else {
			result.setText(sb.toString());
			//conneted_num.setVisibility(View.INVISIBLE);
		}
                mHandler.postDelayed(mRunnable, 2000);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mHandler.removeCallbacks(mRunnable);
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	
        //[测试报告界面 上侧键 从头重测，下侧键 返回home][xuzhenguo][20121226]
    	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
		        Log.d(TAG, "KEYCODE_HOME"+android.os.Process.myPid());
			Intent intent = new Intent(Intent.ACTION_MAIN, null);
			intent.addCategory(Intent.CATEGORY_HOME);
			//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();//测试报告界面，点击home键返回待机后，再点击程序图标 重新测试
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Log.d(TAG, "KEYCODE_VOLUME_UP");
	        //editor.clear();
	       // editor.commit();
	        
	        for(int i = MMIStartTest.mAdapter.getDeviceClassSize() - 1; i>= 0; i--){
	            Intent intent = new Intent(Test_launcherActivity.this, MMIStartTest.mAdapter.getDeviceClassAtIndex(i));
	            intent.putExtra(TOKEN, i);
	            startActivity(intent);
	        }
	        finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			Log.d(TAG, "KEYCODE_VOLUME_DOWN  :writeBarcode");
                        if(!isNewBarcode){
		            writeBarcode(newBarcode);
		        }
		        editor.putBoolean("isNewBarcode", true);
		        editor.commit();
			return true;
		}
		    
		return super.onKeyDown(keyCode, event);
	}
	
	private BroadcastReceiver mReceiver =  new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if ("com.bird.SMT.SET_BARCODE".equals(action)){
                            Log.d(TAG, "Receiver   SET_BARCODE ");
                            write_success_hint.setVisibility(View.VISIBLE);
                           // barcode_view.setVisibility(View.VISIBLE);
                            //barcode_view.setText(snString);
                           
                        }
		}
		
	};
	
	private Runnable mRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG, "mRunnable  run");
			if (MMITestBootupService.mCount  <= 10){
			    StringBuffer count = new StringBuffer(" ");
		            count.append(MMITestBootupService.mCount);
		            //conneted_num.append(count);
		            count_view.setText(count);
			    mHandler.postDelayed(mRunnable, 3000);
			} else {
			    mHandler.removeCallbacks(mRunnable);
			}
		}
		
	};
}
