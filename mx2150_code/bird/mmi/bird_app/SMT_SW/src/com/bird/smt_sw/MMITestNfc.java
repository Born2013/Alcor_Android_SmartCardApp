package com.bird.smt_sw;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.ComponentName;

public class MMITestNfc extends MMINewActivity {

	private Button correctButton;
	private Button errorButton;
	private Button resetButton;
	private TextView mNfcTestResult;
	private boolean isNfcUse = false;
	private NfcAdapter mNfcAdapter;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.nfc_test);
        setTitle(R.string.bird_nfc_title);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		isNfcUse = ifNFCUse();
		mNfcTestResult = (TextView) findViewById(R.id.nfc_test_result);
		
		if (isNfcUse) {
		    mNfcTestResult.setText(R.string.pass);
		} else {
		    mNfcTestResult.setText(R.string.fail);
		}

		correctButton = (Button) findViewById(R.id.correct);
		errorButton = (Button) findViewById(R.id.error);
		resetButton = (Button) findViewById(R.id.reset);
		correctButton.setOnClickListener(mCorrectButtonHandler);
		errorButton.setOnClickListener(mErrorButtonHandler);
		resetButton.setOnClickListener(mRestdButtonHandler);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
	}

	/**
	 * 检测工作,判断设备的NFC支持情况
	 * @return
	 */
	private Boolean ifNFCUse() {
		// TODO Auto-generated method stub
		if (mNfcAdapter == null) {
			return false;
		}
		/*
		if (mNfcAdapter != null && !mNfcAdapter.isEnabled()) {
			return false;
		}
		*/
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(MMITestNfc.this.getString(R.string.serial_test), Test_launcherActivity.PASS);
            mEditor.commit();
            finish();
        }
    };
        
    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(MMITestNfc.this.getString(R.string.serial_test), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };
    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {       
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(MMITestNfc.this, MMITestNfc.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
        }
    };

}

