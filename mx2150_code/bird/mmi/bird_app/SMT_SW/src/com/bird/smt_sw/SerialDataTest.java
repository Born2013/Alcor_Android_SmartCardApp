package com.bird.smt_sw;

import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.SharedPreferences;

public class SerialDataTest extends MMINewActivity {

    private TextView mTextView = null;
    private static final String TAG = "Cardreader";
    private static final int BUF_SIZE = 65544;
    char status[] = {0x65,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x65};
    char power[] = {0x62,0x00,0x00,0x00,0x00,0x00,0x02,0x00,0x00,0x00,0x60};
    char[] buf_cmd_read = new char[BUF_SIZE];
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_test);
        setTitle(R.string.serial_test);
        mTextView = (TextView) findViewById(R.id.mesage);
        
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
	}

    private boolean flag = false;
    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(SerialDataTest.this.getString(R.string.serial_test), Test_launcherActivity.PASS);
            mEditor.commit();
            if(flag == false) {
                finish();
            }
        }
    };
        
    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(SerialDataTest.this.getString(R.string.serial_test), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };
    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {       
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(SerialDataTest.this, SerialDataTest.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
        }
    };

    @Override
	protected void onPause() {
		Log.d(TAG, "Activity OnPause");
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "Activity onResume");
		super.onResume();
        String message = "";
        int result = api.cardread.com.cardreadso.CardReadApi.CardreaderWriteDataToUart(status,11);
        if (result == -1) {
            message = "write fail";
        } else {
            message = "write success";
        }
        //Toast.makeText(this, ""+result, Toast.LENGTH_LONG).show();
        result = api.cardread.com.cardreadso.CardReadApi.CardreaderReadDataFromUart(buf_cmd_read);
        if (result == -1) {
            message = message + ", read fail.";
        } else {
            message = message + ", read success.";
            for (int i = 0; i < result; i++) {
                if (i == 0)
                    message = message + " \n" + Integer.toHexString(buf_cmd_read[i]);
                else {
                    message = message + ", " + Integer.toHexString(buf_cmd_read[i]);
                }
            }
        }
        //Toast.makeText(this, ""+CardReadApi.CardreaderReadDataFromUart(buf_cmd_read) + /*", buf_cmd_read = " + Arrays.toString(buf_cmd_read) + */", buf_cmd_read[0] = " + Integer.toHexString(buf_cmd_read[0]), Toast.LENGTH_LONG).show();
        String message1 = "";
        result = api.cardread.com.cardreadso.CardReadApi.CardreaderWriteDataToUart(power,11);
        if (result == -1) {
            message1 = "write fail";
        } else {
            message1 = "write success";
        }
        //Toast.makeText(this, ""+result, Toast.LENGTH_LONG).show();
        result = api.cardread.com.cardreadso.CardReadApi.CardreaderReadDataFromUart(buf_cmd_read);
        if (result == -1) {
            message1 = message1 + ", read fail.";
        } else {
            message1 = message1 + ", read success.";
            for (int i = 0; i < result; i++) {
                if (i == 0)
                    message1 = message1 + " \n" + Integer.toHexString(buf_cmd_read[i]);
                else {
                    message1 = message1 + ", " + Integer.toHexString(buf_cmd_read[i]);
                }
            }
        }
        String resultString = message + "\n" + message1;
        mTextView.setText(resultString);
	}

}


