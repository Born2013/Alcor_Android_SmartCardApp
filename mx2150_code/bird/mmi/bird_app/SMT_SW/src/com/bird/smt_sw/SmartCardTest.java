package com.bird.smt_sw;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import amlib.ccid.Reader;
import amlib.ccid.ReaderException;
import amlib.ccid.SCError;
import amlib.hw.HWType;
import amlib.hw.HardwareInterface;

public class SmartCardTest extends MMINewActivity {
	private UsbManager mManager;
	private ArrayList<String> mReaderArrayList = new ArrayList<String>();
	private TextView mTextViewInfo;
	private TextView mTextViewResult; 
	private static final String TAG = "AlcorTest";
	private static final int MODE_7816 = Reader.CardModeASYNC;
	private static final int INTERFACE_SMARTCARD = 0xB;
	public static final int LogTypeLogCat = 0;
	public static final int LogTypeSDKLog= 1;

    private Reader mReader;
    private HardwareInterface mMyDev;
    private byte mSlotNum;
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reader_test);
        setTitle(R.string.reader_test);
		Log.d(TAG," onCreate");
		String s="Device-info:";
		s += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
		s += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
		s += "\n Device: " + android.os.Build.DEVICE;
		s += "\n Brand: " + android.os.Build.BRAND;
		s += "\n Model (and Product): " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
		Log.d(TAG," info = "+ s);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mTextViewInfo = (TextView)findViewById(R.id.textInfo);
        mTextViewResult = (TextView)findViewById(R.id.textResult);
		mTextViewInfo.setText(s);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mMyDev = new HardwareInterface(HWType.eUSB, this.getApplicationContext());
	}

    private boolean flag = false;
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(SmartCardTest.this.getString(R.string.reader_test), Test_launcherActivity.PASS);
            mEditor.commit();
            if(flag == false) {
                finish();
            }
        }
    };
        
    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(SmartCardTest.this.getString(R.string.reader_test), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };
    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {       
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(SmartCardTest.this, SmartCardTest.class);
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
            ComponentName comp = new ComponentName(SmartCardTest.this, SmartCardTest.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            mEditor.putInt(SmartCardTest.this.getString(R.string.reader_test), Test_launcherActivity.PASS);
            mEditor.commit();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	
	public void onISO7816(){
        Log.d(TAG,"MODE_7816 == " + MODE_7816);
		switchMode(MODE_7816);
	}

	protected void onPause() {
		Log.d(TAG, "Activity OnPause");
		super.onPause();
	}
	protected void onResume() {
		Log.d(TAG, "Activity onResume");
		EnumeDev();
		onISO7816();
		super.onResume();
	}

	private int EnumeDev()
	{ 
		boolean isReaderFound = false;
		UsbDevice device = null;    
		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		Log.d(TAG," EnumeDev");
		mReaderArrayList.clear();
		while(deviceIterator.hasNext()){
			device = deviceIterator.next();   
			Log.d(TAG," "+ Integer.toHexString(device.getVendorId()) +" " +Integer.toHexString(device.getProductId()));

			if(isAlcorReader(device)) 
			{   
				isReaderFound = true;
				Log.d(TAG,"Found Device");
				mReaderArrayList.add(pid2DevName (device.getProductId())+"-"+device.getDeviceName());					
			}
			else
			{
				//mReaderArrayList.add("Not supportted reader");
			}
		}
		if (isReaderFound == false)
		{
			mTextViewResult.setText("No Supported Reader Been Found");
		}
		else
		{
			mTextViewResult.setText("");
		}
		return 0;
	}

	private boolean isAlcorReader(UsbDevice udev){
		if (udev.getVendorId() == 0x058f
				&& ((udev.getProductId() == 0x9540) 
						|| (udev.getProductId() == 0x9520)  || (udev.getProductId() == 0x9522)
						|| (udev.getProductId() == 0x9525) || (udev.getProductId() == 0x9526) 
						)
				)
			return true;
		else if (udev.getVendorId() == 0x2CE3 
				&& ((udev.getProductId() == 0x9571) || (udev.getProductId() == 0x9572) || (udev.getProductId() == 0x9563)) || (udev.getProductId() == 0x9573))
			return true;
		return false;
	}

	private boolean isSmartCardReader(UsbDevice udev){
		int intfCnt = 0, i;
		UsbInterface interf;
		intfCnt = udev.getInterfaceCount();
		Log.d(TAG," interface cnt=" + intfCnt);
		/**
		 * bug: in Android5.x , only the last plugged device has the 
		 * right correct number of interfaces, other devices report 0 interfaces.
		 */
		for (i=0 ; i<intfCnt; i++)
		{
			interf = udev.getInterface(i);
			if (interf.getInterfaceClass() == INTERFACE_SMARTCARD)
			{
				Log.d(TAG," INTERFACE_SMARTCARD");
				return true;
			}
		}
		return false;
	}


	private UsbDevice getSpinnerSelect(){
		String deviceName;
        Log.d(TAG,"getSpinnerSelect 111");
		deviceName= (String) mReaderArrayList.get(0);
        Log.d(TAG,"getSpinnerSelect 222");
		if (deviceName != null) {
			// For each device
			for (UsbDevice device : mManager.getDeviceList().values()) {
                Log.d(TAG,"getSpinnerSelect 333");
				if (deviceName.equals(pid2DevName (device.getProductId())+"-"+device.getDeviceName())) {
					return device;
				}
			}
		}
		return null;
	}

	public static String byte2String(byte[] buffer, int bufferLength) {
		String bufferString = "";
		String dbgString = "";
		for (int i = 0; i < bufferLength; i++) {
			String hexChar = Integer.toHexString(buffer[i] & 0xFF);
			if (hexChar.length() == 1) {
				hexChar = "0" + hexChar;
			}
			if (i % 16 == 0) {
				if (dbgString != "") {
					//	                    Log.d(LOG_TAG, dbgString);
					bufferString += dbgString;
					dbgString = "\n";
				}
			}
			dbgString += hexChar.toUpperCase() + " ";
		}

		if (dbgString != "") {
			//	        	Log.d(LOG_TAG, dbgString);
			bufferString += dbgString;
		}

		return bufferString;
	}

	private String pid2DevName(int pid) {
		String name = null;
		switch (pid)
		{
		case 0x9520:
		case 0x9522:
		case 0x9540:
		case 0x9525:
			name = new String("SAM Card Reader");
			break;
		case 0x9526:
			name = new String("Smart Card Reader/NFC Reader");
			break;
		case 0x9571:
			name = new String("NFC Reader");
			break;
		case 0x9572:
			name = new String("Smart Card Reader/NFC Reader");
			break;
		}
		return name; 
	}

	private void switchMode(int mode)
	{
		try{
			UsbDevice dev = getSpinnerSelect();
			if (dev == null)
			{
				Log.e(TAG, "switchMode dev null");
                mTextViewResult.setText("failed");
				return;
			}
			switch (mode)
			{
			case MODE_7816:
                Log.e(TAG, "switchMode 1111111111");
				setMode(dev);
                Log.e(TAG, "switchMode 2222222222");
				break;
			default :
                setMode(null);
				break;
			}

		}catch (Exception e){
			String mStrMessage = "Get Exception : " + e.getMessage();
			Log.e(TAG, mStrMessage);
			mTextViewResult.setText("failed");
		}
	}

    private void setMode(UsbDevice mUsbDev) {
        if (mUsbDev == null) {
            mTextViewResult.setText("Device not found");
            return;
        }
        Log.d(TAG, "mdev=" + Integer.toHexString(mUsbDev.getProductId()));
        checkSlotNumber(mUsbDev);
    }

    private void checkSlotNumber(UsbDevice uDev){
        mSlotNum = (byte)0;
        try {
            usbDev = uDev;     
            new OpenTask().execute(uDev);
        }
        catch(Exception e){
            String mStrMessage = "Get Exception : " + e.getMessage();
            Log.e(TAG, mStrMessage);
            mTextViewResult.setText("Device not found");
        }
    }

    private class OpenTask extends AsyncTask <UsbDevice, Void, Integer> {

        @Override
        protected Integer doInBackground(UsbDevice... params) {
            int status = 0;

            status = initReader() ;
            if ( status != 0){
                return status;
            }
            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != 0) {
                mTextViewResult.setText("Open fail: "+ SCError.errorCode2String(result)+". ");
                Log.e(TAG,"Open fail: "+ SCError.errorCode2String(result)+". ");
            }else{
                Log.e(TAG,"Open successfully");
                onISO7816Power();
            }
        }
    }

    private int getSlotStatus(){
        int ret = SCError.READER_NO_CARD;
        byte []pCardStatus = new byte[1];
        /*detect card hotplug events*/
        ret = mReader.getCardStatus(pCardStatus);
        if (ret == SCError.READER_SUCCESSFUL) {
            if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_ABSENT) {
                ret = SCError.READER_NO_CARD;
            } else if (pCardStatus[0] == Reader.SLOT_STATUS_CARD_INACTIVE) {
                ret = SCError.READER_CARD_INACTIVE;
            } else {
                ret = SCError.READER_SUCCESSFUL;
            }
        }
        return ret;
    }

    private UsbDevice usbDev;

    private int initReader()
    {
        int Status = 0;
        boolean init;// 
        Log.d(TAG, "initReader");
        try {
            init = mMyDev.Init(mManager, usbDev);
            if (!init){
                Log.e(TAG, "Device init fail");
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "initReader fail "+ e.getMessage());
            return -1;
        }

        try {
            mReader = new Reader(mMyDev);
            Status = mReader.open();
        }
        catch (ReaderException e)
        {
            Log.e(TAG, "initReader fail "+ e.getMessage());
            return -1;
        }
        mReader.setSlot(mSlotNum);

        return Status;
    }

    private void onISO7816Power(){
        Log.d(TAG, "7816PowerOnClick");
        int ret;
        ret = poweron();
        if (ret == SCError.READER_SUCCESSFUL){
            Log.d(TAG, "power on successfully");
            onISO7816getATR();
        }
        else if (SCError.maskStatus(ret) == SCError.READER_NO_CARD){
            Log.d(TAG, "Card Absent");
        }
        else{   
            Log.d(TAG, "power on fail:"+  SCError.errorCode2String(ret) + "("+Integer.toHexString(ret)+")");
        }
    }

    private int poweron(){
        int result = SCError.READER_SUCCESSFUL;
        Log.d(TAG,"poweron");
        //check slot status first
        result = getSlotStatus();
        switch (result){
            case SCError.READER_NO_CARD:
                mTextViewResult.setText("Card Absent");
                Log.d(TAG,"Card Absent");
                return SCError.READER_NO_CARD;
            case SCError.READER_CARD_INACTIVE:
            case SCError.READER_SUCCESSFUL:
                break;
            default://returns other error case
                return result;
        }
        result = mReader.setPower(Reader.CCID_POWERON);
        Log.d(TAG,"power on exit");
        return result;
    }

    public void onISO7816getATR(){
        String atr;
        Log.d(TAG, "getATROnClick");
        atr = mReader.getAtrString();
        mTextViewResult.setText(" ATR:" + atr);
    }

	public static byte[] toByteArray(String hexString) {

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

    @Override
    protected void  onDestroy() {
        Log.e(TAG,"onDestroy");
        if (mReader != null)
        {
            mReader.close();
            mReader = null;
        }
        if (mMyDev != null)
        {
            mMyDev.Close();
        }
        super.onDestroy();
    }
}


