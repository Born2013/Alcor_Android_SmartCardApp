package com.example.smartcardapp;

import java.util.HashMap;
import java.util.Iterator;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
//import android.view.Menu;
//import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;






import amlib.ccid.Debug;
//import com.alcormicro.lib.Debug;
import amlib.hw.HWType;
import amlib.ccid.Reader;
import amlib.hw.HardwareInterface;
import amlib.ccid.Error;


public class TestActivity extends ActionBarActivity {
	private Reader mReader;
	private boolean mSlotStatus;
	private HardwareInterface mMyDev;
	private UsbDevice mUsbDev;
	private UsbManager mManager;
	private PendingIntent mPermissionIntent;
	private Button mListButton;
	//private Button mOpenButton;
	private Button mConnectionButton;
	private Button mATRButton;
	private Button mAPDUButton;
	private Button mProtocolButton;
	private Button mSwitchButton;
	private Button mPowerOffButton;
	private ArrayAdapter<String> mReaderAdapter;
	private TextView mTextViewReader;
//	private TextView mTextViewOpen;
	private TextView mTextViewConnect;
	private TextView mTextViewATR; 
	private TextView mTextViewGetApdu;
	private EditText mEditTextApdu;
	//private EditText mEditTextMode;
	private TextView mTextProtocol;
	private TextView mTextMode;
	
	private Spinner mModeSpinner;
	private Spinner mReaderSpinner;
	private ArrayAdapter<String> mModeList;
//	private ArrayAdapter<String> mModeAdapter;
	private String mStrMessage;
	private final String mode2 = "I2c Mode";
	private final String mode3 = "SLE4428 Mode";
	//private String strTemp;
	private Thread mThread;
	private Context mContext;
	private Handler mHotplugHandler;

		//private StringBuffer strTemp;
	private static final String TAG = "Alcor-Test";
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	/* = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String msgString = msg.getData().getString("CardHotplug");
            Toast.makeText(mContext, msgString,Toast.LENGTH_SHORT).show();
        }
    };*/
	private final boolean CARD_REMOVED = false;
	private final boolean CARD_INSERTED = true;
	
	class HotPlug  implements Runnable  {   
		private boolean isRunning = true;
        public void run() {
        	
        	Log.d(Debug.TAG, "Starting Hotplug thread...");
        	isRunning = true;
        	while(isRunning){
        		
        		byte []pCardStatus = new byte[1];
        		//Log.d(Debug.TAG, "Detecting Hotplug ");
        		/*detect card hotplug events*/
        		try{
        			Bundle countBundle = new Bundle();
    				Message msg = new Message();
                    countBundle.putString("CardHotplug", "");
        			if (mReader.getCardStatus(pCardStatus) == Error.READER_SUCCESSFUL){	
        				//Log.d(Debug.TAG,"cmd OK  mSlotStatus = " +mSlotStatus);
        				if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT ){
        					if (mSlotStatus == CARD_INSERTED) {
        						/*card removing*/
        						mSlotStatus = CARD_REMOVED;
        						countBundle.putString("CardHotplug", "SmartCard Removed!!");
        						msg.setData(countBundle);
        						Log.d(Debug.TAG, "Card REmove");                       
        					}
        				}else{
        					if (mSlotStatus == CARD_REMOVED){
        						/*card inserting*/
        						mSlotStatus = CARD_INSERTED;
        						countBundle.putString("CardHotplug", "SmartCard Inserted!!");
        						msg.setData(countBundle);
        						Log.d(Debug.TAG, "Card insert");
                                mHotplugHandler.sendMessage(msg);
        					}
        				}
        				
        			}/*end of if (mReader.getCardStatus(pCardStatus) ==*/
        			else {
        				Log.e(Debug.TAG, "error to get slogt status");
        			}
        			
        				/*
        			{
        			boolean isEvent;
        				Bundle countBundle = new Bundle();
        				Message msg = new Message();
                        countBundle.putString("CardHotplug", "");
        				if (pCardStatus[0] == 1)
        					countBundle.putString("CardHotplug", "SmartCard Inserted!!");
        					//Toast.makeText(mContext,"SmartCard Inserted!!",Toast.LENGTH_SHORT).show();
        				else
        					countBundle.putString("CardHotplug", "SmartCard Removed!!");
        					//Toast.makeText(mContext,"SmartCard Removed!!",Toast.LENGTH_SHORT).show();
        				
                         msg.setData(countBundle);
           
                         mHotplugHandler.sendMessage(msg);
        			}
        			*/
        			Thread.sleep(500);
        				//Log.d(Debug.TAG, "hotplug events " + Integer.toHexString(pCardStatus[0]));
        		}catch (InterruptedException e) {
					e.printStackTrace();
				}catch(Exception e){
            		Log.e(Debug.TAG, " Exception : " + e.getMessage());
            	} 
        	}/*end of while*/
        	Log.d(Debug.TAG, "Detecting Hotplug Exit");
        } 
        
        
        public void stopThread() 
        {
        	Log.d(Debug.TAG,"stop thread");
            this.isRunning = false;
            /*
            if (mThread != null) {
    	        if (!mThread.isInterrupted()) {
    	        	mThread.interrupt();
    	        }
    	    }
    	    */
        }
    };
    private HotPlug mTask;
  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
		setupViews();
		mSlotStatus = CARD_REMOVED;
		mContext = getApplicationContext();
		mHotplugHandler = new Handler() {
	        public void handleMessage(Message msg) {
	            super.handleMessage(msg);
	            String msgString = msg.getData().getString("CardHotplug");
	            Toast.makeText(mContext, msgString,Toast.LENGTH_SHORT).show();
	           
	        }
	    };
	    
		Log.d(TAG," onCreate");
		try {
			mMyDev = new HardwareInterface(HWType.eUSB);
			
		}catch(Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			return;
		}
		// Get USB manager
		Log.d(TAG," mManager");
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
        
        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);
        Log.d(TAG," enume");
      //  EnumeDev();
        
        
	}

	protected void onResume() {
		Log.d(TAG, "Activity onResume");
		super.onResume();
	}
	
	protected void onPause() {
		Log.d(TAG, "Activity OnPause");
		super.onPause();
	}
	
	protected void   onStop() {
		Log.d(TAG, "Activity onStop");
		if (mTask != null)
			mTask.isRunning = false;
		//mTask.stopThread();
		/*
		if (mReader != null){
			mReader.Close();
			mReader.Destroy();
		}
		*/
	
		if (mThread != null)
			mThread.interrupt();
		/*
		mMyDev = null;
		mReader = null;
		mUsbDev = null;
		mManager = null;
		mTask = null;
		*/
		//unregisterReceiver(mReceiver);
		super.onStop();
	}
	
	protected void   onDestroy() {
		unregisterReceiver(mReceiver);
		if (mMyDev != null)
			mMyDev.Close();
		if (mReader != null)
			mReader.destroy();
		super.onDestroy();
		//android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	public void setupViews(){
		String[] pMode = new String[] {mode2, mode3};
		
		mListButton = (Button)findViewById(R.id.buttonList);
		//mOpenButton = (Button)findViewById(R.id.buttonOpen);
		mConnectionButton = (Button)findViewById(R.id.buttonConnect);
		mATRButton  = (Button)findViewById(R.id.buttonATR);
		mAPDUButton = (Button)findViewById(R.id.buttonAPDU);
		mProtocolButton = (Button)findViewById(R.id.buttonProtocol);
		mSwitchButton = (Button)findViewById(R.id.buttonSwitch);
		mPowerOffButton = (Button)findViewById(R.id.buttonOff);
		
		mListButton.setEnabled(true);
		//mOpenButton.setEnabled(true);
		mConnectionButton.setEnabled(true);
		mATRButton.setEnabled(false);
		mAPDUButton.setEnabled(false);
		mProtocolButton.setEnabled(false);
		mSwitchButton.setEnabled(false);
		mPowerOffButton.setEnabled(false);
		
		mTextViewReader = (TextView)findViewById(R.id.textReader);
		//mTextViewOpen = (TextView)findViewById(R.id.textOpenResult);
		mTextViewConnect = (TextView)findViewById(R.id.textConnectResult);
    	mTextViewATR = (TextView)findViewById(R.id.textATR);
    	mTextViewGetApdu= (TextView)findViewById(R.id.textGetAPDU);
    	mEditTextApdu = (EditText)findViewById(R.id.textAPDU);
    	mEditTextApdu.setText("A0A40000023F00");
    	mTextProtocol = (TextView)findViewById(R.id.textProtocol);
    	mTextMode = (TextView)findViewById(R.id.textSwitchMode);
    
    	//mTexPowerOff = (TextView)findViewById(R.id.TextViewOff);
    	//mEditTextMode = (EditText)findViewById(R.id.textMode);
    	//mEditTextMode.setText("1");
    	mModeSpinner = (Spinner) findViewById(R.id.modeSpinner);
    	
    	mModeList = new ArrayAdapter<String>(this,  
    			android.R.layout.simple_spinner_item, pMode);  	
    	
    	mModeList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	mModeSpinner.setAdapter(mModeList);
    	
    	 // Initialize reader spinner
        mReaderAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        
        mReaderSpinner = (Spinner) findViewById(R.id.spinnerDevice);
        mReaderSpinner.setAdapter(mReaderAdapter);
        /*
        mReaderSpinner.setOnItemSelectedListener(Spinnerlistener) ;
        private OnItemSelectedListener Spinnerlistener = new OnItemSelectedListener() {
        	
    		@Override

    		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {

    		// TODO Auto-generated method stub

    		}
    		*/
        mReaderSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,int position, long arg3) {
            	String deviceName;
        		deviceName= (String) mReaderSpinner.getSelectedItem();
                if (deviceName != null) {

                     // For each device
                     for (UsbDevice device : mManager.getDeviceList().values()) {
                         if (deviceName.equals(device.getDeviceName())) {

                             // Request permission
                             mManager.requestPermission(device, mPermissionIntent);
                          
                             break;
                         }
                     }
                }
                        
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
               // TODO Auto-generated method stub
            }
        });
 
	}
	

	private void updateReader(){
		int pid; 
		int vid;
		try {
			pid = mUsbDev.getProductId();
			vid = mUsbDev.getVendorId();
		}catch(NullPointerException e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			return;
		}
		mTextViewReader.setText(""+ Integer.toHexString(vid) + " " +  Integer.toHexString(pid));
	}
	
	public void ListOnClick(View view){
		Log.d(Debug.TAG, "ListOnClick");
		EnumeDev();
	}

	public void SpinnerDeviceOnClick(View view){
		Log.d(Debug.TAG, "SpinnerDeviceOnClick");
		
		String deviceName;
		
		EnumeDev();
		deviceName= (String) mReaderSpinner.getSelectedItem();
        if (deviceName != null) {

             // For each device
             for (UsbDevice device : mManager.getDeviceList().values()) {
                 if (deviceName.equals(device.getDeviceName())) {

                     // Request permission
                     mManager.requestPermission(device, mPermissionIntent);
                  
                     break;
                 }
             }
        }
	}


	
	public void ConnectOnClick(View view){
		int status;
		Log.d(Debug.TAG, "ConnectOnClick");

		try {
    		status = mReader.connect();
    	}
        catch(Exception e){
    		
			mStrMessage = "Get Exception : " + e.getMessage();
			mTextViewConnect.setText( mStrMessage); 
			return ;
		}
    	
		if (status != Error.READER_SUCCESSFUL){
			
			mTextViewConnect.setText("Connection fail: "+ Integer.toString(status)
					+ "("+ Integer.toHexString(mReader.getCmdFailCode()) +")");
		}else{
			
			mTextViewConnect.setText("Connect successfully");
			mConnectionButton.setEnabled(false);
			mATRButton.setEnabled(true);
			mAPDUButton.setEnabled(true);
			mPowerOffButton.setEnabled(true);
			mProtocolButton.setEnabled(true);
			mTask = new HotPlug();
			mThread = new Thread(mTask);
	    	mThread.start();
		}
	
    
		
	}
	
	public void getATROnClick(View view){
		
		String atr;
		
		
		Log.d(Debug.TAG, "getATROnClick");
		try {
			atr = mReader.getAtrString();
			mTextViewATR.setText(" ATR:"+ atr);
			
			mATRButton.setEnabled(false);
			
			
		}
		catch (Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			mTextViewATR.setText(mStrMessage);
		}
	}

	public void sendAPDUkOnClick(View view){
		byte[] pSendAPDU ;
		byte[] pRecvRes = new byte[300];
		int[] pRevAPDULen = new int[1];
		String apduStr;
		int sendLen, result;

		Log.d(Debug.TAG, "readBlockOClick");
		pRevAPDULen[0] = 300;
		
		apduStr = mEditTextApdu.getText(). toString();
		pSendAPDU = toByteArray(apduStr);
		sendLen = pSendAPDU.length; 
		
		try{
			result = mReader.transmit(pSendAPDU, sendLen, pRecvRes, pRevAPDULen);
			if (result == Error.READER_SUCCESSFUL){
				 mTextViewGetApdu.setText("Receive APDU: "+ logBuffer(pRecvRes, pRevAPDULen[0]));
			}
			else{
				 mTextViewGetApdu.setText("Fail to Send APDU: " + Integer.toString(result)
						 + "("+ Integer.toHexString(mReader.getCmdFailCode()) +")");
			}
		}catch (Exception e){
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			mTextViewATR.setText(mStrMessage);
		}
		
	}
	
	
	
	public void getProtocolOnClick(View view){
		int status;
		byte []proto = new byte[1];
		Log.d(Debug.TAG, "getProtocolOnClick");

		try {
    		status = mReader.getProtocol(proto);
    	}
        catch(Exception e){
    		
			mStrMessage = "Get Exception : " + e.getMessage();
			mTextViewConnect.setText( mStrMessage); 
			return ;
		}
    	
		if (status != Error.READER_SUCCESSFUL){
			
			mTextViewConnect.setText("getProtocol fail: "+ Integer.toString(status)
					+ "("+ Integer.toHexString(mReader.getCmdFailCode()) +")");
		}else{
			mTextProtocol.setText(Integer.toString(proto[0]));
		}
	}
		
//	public void switchModeOnClick(View view){
//		byte mode = 0x01;
//		int result;
//		String modeString;
//		//textMode 
//		//mEditTextMode.getText().to;
//		modeString = (String)mModeSpinner.getSelectedItem();
//		Log.d(Debug.TAG ,"selected :" + modeString);
//		if ( modeString.compareTo(mode2) == 0){
//			mode = 0x2;
//			mode = 0x3;
//		}
//
//		//mode = (byte) (Integer.parseInt(mEditTextMode.getText().toString()) & 0xff);
//		try{
//			
//			if (result == error.READER_SUCCESSFUL){
//				mTextMode.setText("switch mode successfully");
//			}
//			else{
//				mTextMode.setText("switch mode Fail" + "("+ Integer.toHexString(mReader.getCmdFailCode()) +")");
//			}
//		}catch (Exception e){
//			mStrMessage = "Get Exception : " + e.getMessage();
//			Log.e(TAG, mStrMessage);
//			mTextViewATR.setText(mStrMessage);
//			
//		}
//		 
//	}
	
	
	
	
	
	public void PowerOffOnClick(View view){

		mReader.close();
		//mOpenButton.setEnabled(true);
		mConnectionButton.setEnabled(true);
		mAPDUButton.setEnabled(false);
		mSwitchButton.setEnabled(false);
		mATRButton.setEnabled(false);
		mPowerOffButton.setEnabled(false);
		mProtocolButton.setEnabled(false);
		mTask.stopThread();
		
		//mTexPowerOff.setText(Integer.toHexString(result));
	}
	/*
	public void sendEscapekOnClick(View view){
		byte[] pSendAPDU ;
		byte[] pRecvRes = new byte[300];
		int[] pRevAPDULen = new int[1];
		String apduStr;
		int sendLen, result;

		Log.d(Debug.TAG, "readBlockOClick");
		pRevAPDULen[0] = 300;
		
		apduStr = mEditTextApdu.getText().toString();
		pSendAPDU = toByteArray(apduStr);
		sendLen = pSendAPDU.length; 
		
		 result = mReader.transmit(pSendAPDU, sendLen, pRecvRes, pRevAPDULen);
		 if (result == Reader.READER_SUCCESSFUL){
			 mTextViewGetApdu.setText("Receive APDU: "+ logBuffer(pRecvRes, pRevAPDULen[0]));
		 }
		 else{
			 mTextViewGetApdu.setText("Fail to Send APDU: " + Integer.toString(result));
		 }
		 
	}
    */
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

    	public void onReceive(Context context, Intent intent) {
           	Log.d(TAG, "Broadcast Receiver");
            
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                        	mUsbDev = device;
                        	
                        	try {    		
                        			updateReader();
                        			
                        	}
                        	 catch(Exception e){
                     			mStrMessage = "Get Exception : " + e.getMessage();
                     			Log.e(TAG, mStrMessage);
                     		}
                        	if ( InitReader() != 0){
                        		Log.e(TAG, "fail to initial reader");
                        	}
                        	
                        }
                    } else {
                       	Log.d(TAG, "Permission denied for device " + device.getDeviceName());
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
           	
            	Log.d(TAG, "DEvice Detached");
            	//mOpenButton.setEnabled(true);
        		mConnectionButton.setEnabled(true);
        		mAPDUButton.setEnabled(false);
        		mSwitchButton.setEnabled(false);
        		mATRButton.setEnabled(false);
        		mPowerOffButton.setEnabled(false);
        		mTextViewReader.setText("");
                synchronized (this) {
                    // Update reader list
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                    	Log.d(TAG, "Update reader list : " + device.getDeviceName());
                    	 if(isAlcorReader(device))
                    		 mReaderAdapter.add(device.getDeviceName());
                    }   

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    
                    if (device != null) {
                        // Close reader
                    	Log.d(TAG, "Closing reader...");
                    	if (mTask != null)
                    		mTask.isRunning = false;
                    
                    }
                }/*end of  if (ACTION_USB_PERMISSION.equals(action))*/
            }
        }/*end of onReceive(Context context, Intent intent) {*/
    };
    
    private boolean isAlcorReader(UsbDevice udev){
    	if (udev.getVendorId() == 0x058f
    			&& ((udev.getProductId() == 0x9540) || (udev.getProductId() == 0x9560)
    					 || (udev.getProductId() == 0x9520)  || (udev.getProductId() == 0x9522))
    			)
	    		 return true;
    	return false;
    }
    
    private int EnumeDev()
    {
    	UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
    	
    	HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
    	Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    	Log.d(TAG," EnumeDev");
    	mReaderAdapter.clear();
    	while(deviceIterator.hasNext()){
    	    UsbDevice device = deviceIterator.next();    
    	    Log.d(TAG," "+ Integer.toHexString(device.getVendorId()) +" " +Integer.toHexString(device.getProductId()));
    	    if(isAlcorReader(device)) 
    		{       
    	    	Log.d(TAG,"Found Device");
    	    	mReaderAdapter.add(device.getDeviceName());	
    		} // end if
    	
    	}
		return 0;
    }
    
    private int InitReader()
    {
    	int Status = 0;
    	boolean init;// 
    	Log.d(Debug.TAG, "InitReader");
    	try {
    		init = mMyDev.Init(mManager, mUsbDev);
    		if (!init){
        		Log.e(TAG, "Device init fail");
        		return -1;
        	}
    		mReader = new Reader(mMyDev);
    		
    	}
        catch(Exception e){
    		
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			return -1;
		}
    	
    	
    	//mReader = new Reader();
    	
    	/*
    	try {
    		Status = mReader.init(mMyDev);		
    	}
        catch(Exception e){
    		
			mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage); 
			return -1;
		}
    	*/
    	return Status;
    }
    
    private byte[] toByteArray(String hexString) {

        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[len] = (byte) (value << 4);

                } else {

                    byteArray[len] |= value;
                    len++;
                }

                first = !first;
            }
        }

        return byteArray;
    }
    
    private String logBuffer(byte[] buffer, int bufferLength) {

        String bufferString = "";
        String dbgString = "";

        for (int i = 0; i < bufferLength; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            if (i % 16 == 0) {
                if (dbgString != "") {
//                    Log.d(LOG_TAG, dbgString);
            		bufferString += dbgString;
                    dbgString = "";
                }
            }

            dbgString += hexChar.toUpperCase() + " ";
        }

        if (dbgString != "") {
//        	Log.d(LOG_TAG, dbgString);
        	bufferString += dbgString;
        }
        
        return bufferString;
    }
}


