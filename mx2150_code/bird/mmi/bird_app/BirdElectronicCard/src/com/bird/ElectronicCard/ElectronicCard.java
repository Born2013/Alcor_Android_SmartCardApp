package com.bird.ElectronicCard;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

public class ElectronicCard extends Activity {
	private TextView mUserId;
	private TextView mDate;
	private Button mClear;
	private static final String SALE_INFO_PATH = "/protect_f/saleinfo";
	private String TAG = "SaleInfoReceiver";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mUserId = (TextView) findViewById(R.id.show_userid);
		mDate = (TextView) findViewById(R.id.show_date);
		showDate();
		mClear = (Button) findViewById(R.id.clear);
		mClear.setVisibility(View.GONE);
		mClear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				WriteProtectFFakeValue(SALE_INFO_PATH, "0");
				showDate();
			}
		});
	}

	public boolean getReadDataNew() {
		if (readProtectFFakeText(SALE_INFO_PATH, 30) != null) {
			return readProtectFFakeText(SALE_INFO_PATH, 30).length() >= 2
					&& readProtectFFakeText(SALE_INFO_PATH, 30).substring(0, 2).equals("20");
		} else {
			return false;
		}
	}

	public void showDate() {
		if (!getReadDataNew()) {
			mDate.setText(R.string.new_machine);
		} else {
			mDate.setText(readProtectFFakeText(SALE_INFO_PATH, 30));
		}
	}

	public String readProtectFFakeText(String FilePath, int ProtectFvalueLength) {
		try {
			File ProtectFFakeTextFile = new File(FilePath);
			FileInputStream istream = new FileInputStream(ProtectFFakeTextFile);
			byte[] buffer = new byte[ProtectFvalueLength];
			istream.read(buffer);
			// 最后关闭输入流和输出流
			istream.close();
			// file.close();
			// Log.d("yyf", "!!!  buffer[0] = "+(buffer[0] - '0'));
			// Log.d("yyf", "!!!  buffer[1] = "+(buffer[1] - '0'));
			// Log.d("yyf", "!!!  buffer[2] = "+(buffer[2] - '0'));
			int buf_f = buffer[0] - '0';
			int buf_s = buffer[1] - '0';
			int buf_t = buffer[2] - '0';
			int count = 0;

			for (int i = 0; i < ProtectFvalueLength; i++) {
				int j = buffer[i] - '0';
				if (j != -48) {
					count = count + 1;
				}
			}

			String res = new String(buffer);
			res = res.substring(0, count);
			if (FilePath.equals(SALE_INFO_PATH)) {
				if (buf_f == -48) {
					return null;
				} else {
					return res;
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void WriteProtectFFakeValue(String FilePath, String ProtectFvalue) {
		Log.d(TAG, "FilePath = " + FilePath);
		Log.d(TAG, "ProtectFvalue = " + ProtectFvalue);
		File ProtectFFakeFile = new File(FilePath);
		FileWriter mfr;
		try {
			Log.d(TAG, "protect_f write begin");
			mfr = new FileWriter(ProtectFFakeFile);
			mfr.write(ProtectFvalue);
			Log.d(TAG, "protect_f write sucess");
			mfr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
