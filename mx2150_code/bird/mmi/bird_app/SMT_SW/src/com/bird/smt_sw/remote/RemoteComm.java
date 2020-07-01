package com.etek.ircomm;

import android.util.Log;

/*[BIRD][BIRD_FACTORY_REMOTE][工厂装配测试_红外测试][zhangaman][20170515]begin*/
public class RemoteComm {
	private final static String ET4003_CONTROL_SEND_CODE_1 = "53";
	private final static int ET4003_CONTROL_SEND_CODE = 0x53;
	private final static String ET4003_CONTROL_SEND_CODE_2 = "55";
	private final static String TAG = "RemoteComm";
	private static final String libSoName = "ETEKIRCore";
	static {
		System.loadLibrary(libSoName);
	}

	/**
	 * 
	 * @param remoteData
	 */
	public static void sendRemote(String remoteData) {
		if (remoteData == null) {
			return;
		}
		byte[] sendData = hexStringToBytes(remoteData);
             //Log.i("aaman","sendData = "+sendData);
		int length = sendData.length;
		sendIRCode(sendData, length);
	}

	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();

		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

    	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}


	/**
	 * 
	 * 
	 * @return
	 */
	public static boolean initRemote() {
		int ret = IRinit();
		if (ret == 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 *  
	 */
	public static void finishRemote() {
		Finish();
	}

	/**
	 *  
	 * 
	 * @return
	 */
	public static String getLearnData() {
		byte[] learnData = readLearnIRCode();
		String lrnDtStr = ET4003_CONTROL_SEND_CODE_2
				+ bytesToHexString(learnData);
		return lrnDtStr;
	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}
	/**
	 *  
	 * 
	 * @param timeout
	 * @return -1 
	 */
	public static int remoteLearnStart(int timeout) {
		learnIRCodeStart();
		setLearnTimeout(timeout);
		int status = learnIRCodeMain();
		Log.v(TAG, "learn remote result: " + status);
		return status;
	}

	/**
	 *  
	 */
	public static void remoteLearnStop() {
		learnIRCodeStop();
	}

	/**
	 * 
	 * 
	 * @param data
	 * @param custom
	 * @param code_type
	 * @return
	 */
	public static String encodeRemoteData(String data, String custom,
			String code_type) {

		String temp = custom + data;
		byte[] data1 = hexStringToBytes(temp);
		if (data1 == null) {
			return null;
		}
		byte[] encodeData = Encode(data1, code_type);
		temp = bytesToHexString(encodeData);
		temp = ET4003_CONTROL_SEND_CODE_1 + temp;
		return temp;
	}

	public native static void sendIRCode(byte[] data, int length);

	public native static void learnIRCodeStart();

	public native static void learnIRCodeStop();

	public native static byte[] readLearnIRCode();

	public native static void sendLearnCode(byte[] data);

	public native static int IRinit();

	public native static void Finish();

	public native static void setLearnTimeout(int time);

	public native static int learnIRCodeMain();

	public native static byte[] Encode(byte[] data1, String data2);

	public native static byte[] getAirData(int[] data1);

}
/*[BIRD][BIRD_FACTORY_REMOTE][工厂装配测试_红外测试][zhangaman][20170515]end*/

