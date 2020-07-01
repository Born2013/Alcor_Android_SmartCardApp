package com.bird.ElectronicCard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SmsMessage;

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

public class SaleInfoReceiver extends BroadcastReceiver {
	private Timer timer;
	private TimerTask task;
	public static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	public Context mContext;
	public static Long elapsedRealtime = (long) (2 * 1 * 60); // 单位秒
	private static final String SALE_INFO_PATH = "/protect_f/saleinfo";
	private String TAG = "SaleInfoReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		mContext = context;
		updateTimes();
		
		Log.d(TAG, "!getReadDataNew() = " + !getReadDataNew() + ";   shar = " + Util.getSharedPreference(mContext));

		if (Util.getSharedPreference(mContext)
				&& ACTION_SMS_RECEIVED.equals(intent.getAction())
				&& !getReadDataNew()) {
				
			Bundle carryContent = intent.getExtras();
			if (carryContent != null) {
				StringBuilder sb = new StringBuilder();
				// 通过pdus获取接收到的所有短信息，获取短信内容
				Object[] pdus = (Object[]) carryContent.get("pdus");
				// 构建短信对象数组
				SmsMessage[] mges = new SmsMessage[pdus.length];
				for (int i = 0, len = pdus.length; i < len; i++) {
					// 获取单条短信内容，以pdu格式存，并生成短信对象
					mges[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}
				if (sb != null) Log.d(TAG, "0  sb.toString() = " + sb.toString());
				for (SmsMessage mge : mges) {
					// sb.append("短信来自："
					// ).append(mge.getDisplayOriginatingAddress()).append("\n")
					// .append("短信内容：").append(mge.getMessageBody()).append("\n");
					Date sendDate = new Date(mge.getTimestampMillis());
					SimpleDateFormat format = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					sb.append(format.format(sendDate));
				}
				Log.d(TAG, "1  sb.toString() = " + sb.toString());
				WriteProtectFFakeValue(SALE_INFO_PATH, sb.toString());

			}
		}
	}

	/**
	 * 判断时间是不是20开头 是 return true 否 return false
	 * 
	 * @return
	 */
	public boolean getReadDataNew() {
		if (readProtectFFakeText(SALE_INFO_PATH, 30) != null) {
			Log.d(TAG, "readProtectFFakeText(SALE_INFO_PATH,30) = " + readProtectFFakeText(SALE_INFO_PATH, 30));
			return readProtectFFakeText(SALE_INFO_PATH, 30).length() >= 2
					&& readProtectFFakeText(SALE_INFO_PATH, 30).substring(0, 2).equals("20");
		} else {
			return false;
		}
	}

	void updateTimes() {
		long ut = SystemClock.elapsedRealtime() / 1000;
		if (ut == 0) {
			ut = 1;
		}

		if (ut >= elapsedRealtime) {
			Log.d(TAG, " == = = = == 单次开机6小时 == = = = ==  ");
			Util.setSharedPreference(mContext, true);
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
			mfr.close();
			Log.d(TAG, "protect_f write sucess");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
