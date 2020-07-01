/*[BIRD][BIRD_SYSTEM_ENCRYPT][防刷机]xujing 20170628 begin */
package android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.math.BigInteger;
import java.security.MessageDigest;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

/**
 * @hide
 */
public class SystemEncryptUtils {
    /**
     * @hide
     */
    public static final String MD5_FILE = "/system/etc/.sysInfo";
    /**
     * @hide
     */
    public static final String APP_FOLDER = "/system/app";
    /**
     * @hide
     */
    public static final String PRIVATE_FOLDER = "/system/priv-app";
    /**
     * @hide
     */
    public static final String TAG = "SystemEncrypt_core";
    /**
     * @hide
     */
    private static boolean DEBUG = true;
    private static final HashMap<String, String> HASH = new HashMap<String, String>();
    private static final String ENCRYPT_KEY = "nbbswcruanjianmmigongnengkaifazu";
    private static final String ENCODE = "UTF-8"; // GBK

    private static List<File> mApksList = new ArrayList<File>();

    /**
     * @hide
     */
    public static boolean init() {
        File file = new File(MD5_FILE);
        FileReader bin = null;
        BufferedReader br = null;
        try {
            bin = new FileReader(file);
            br = new BufferedReader(bin);
            String line = null;

            while ((line = br.readLine()) != null) {
                // /^\s*/
                // \\s+[^\\s]+\\s+
                Log.d(TAG, "line =" + line);
                /* 多个空格转换为一个空格 */
                line = line.replaceAll("\\s+ ", " ");
                String[] strs = line.split(" ");
                // String[] strs = line.split("  ");
                HASH.put(strs[1], strs[0]);
            }
        } catch (Exception e) {
            // ~ 所有检测失败的问题均在FxService中进行处理
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 检测目录下文件的md5是否与出场时计算出来的一致。
     * 
     * @hide
     * 
     * @param 目录名
     * 
     * @return 检测结果，通过返回true,失败返回false.
     */
    public static boolean doCheck(String dir) {

        File folder = new File(dir);
        if (folder != null && folder.isDirectory()) {

            File[] child_folders = folder.listFiles();
            Log.d(TAG, "doCheck - child_folders:  " + child_folders);
            File[] child_files = null;
            if(child_folders != null) {
                for (File child_folder : child_folders) {
                    child_files = child_folder.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String child_filename) {
                            if (DEBUG) {
                                Log.d(TAG, "filename :" + child_filename + "  contains :" + child_filename.contains(".apk"));
                            }
                            if (child_filename.contains(".apk")) {
                                return true;
                            }
                            return false;
                        }
                    });
                    if (child_files != null && child_files[0] != null) {
                      mApksList.add(child_files[0]);
                    }
                }
            }
            Log.d(TAG, "mApksList.size() = " + mApksList.size());

            for (int i = 0;i < mApksList.size() ;i++) {
                File child_file_apk = mApksList.get(i);
                String fileName = child_file_apk.getName();
                if (DEBUG) {
                    Log.d(TAG, "fileName :" + fileName);
                    Log.d(TAG, "child_file_apk :" + child_file_apk);
                }
                String md5 = getFileMD5WithKey(child_file_apk, ENCRYPT_KEY);
                Log.d(TAG, fileName + ":newmd5 = " + md5);
                Log.d(TAG, fileName + ":newmd6 = " + HASH.get(fileName));
                Log.d(TAG, "    ");
                if (!md5.equals(HASH.get(fileName))) {
                    /*
                     * if(md5.contains(HASH.get(fileName)) || (
                     * HASH.containsKey(fileName) &&
                     * HASH.get(fileName).contains(md5))) { if(DEBUG) {
                     * Log.d(TAG, "md5 check " + fileName + " contains!" +
                     * " file md5 :" + md5 + "  HASH md5 :" +
                     * HASH.get(fileName)); } continue; }
                     */
                    if (DEBUG) {
                        Log.d(TAG, "md5 check " + fileName + " failed!" + " file md5 :" + md5 + "  HASH md5 :"+ HASH.get(fileName));
                    }
                    // ~ 所有检测失败的问题均在FxService中进行处理
                    throw new RuntimeException("md5 check " + fileName + " failed!" + " file md5 :" + md5 + "  HASH md5 :" + HASH.get(fileName));
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "md5 check " + fileName + " pass !");
                    }
                }
            }
        }
        return true;
    }

    /**
     * 获取文件的md5
     * 
     * @param 文件
     * 
     * @hide
     * 
     * @return 文件的md5值
     */
    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        byte[] md = digest.digest();
        int j = md.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
        /*
         * BigInteger bigInt = new BigInteger(1, digest.digest()); return
         * bigInt.toString(16);
         */
    }

    /**
     * 获取文件带key的md5值
     * 
     * @param 文件
     * 
     * @hide
     * 
     * @return 文件的md5值
     */
    public static String getFileMD5WithKey(File file, String aKey) {

        if (!file.isFile()) {
            return null;
        }

        byte k_ipad[] = new byte[64];
        byte k_opad[] = new byte[64];
        byte keyb[];

        try {
            keyb = aKey.getBytes(ENCODE);
        } catch (UnsupportedEncodingException e) {
            keyb = aKey.getBytes();
        }

        Arrays.fill(k_ipad, keyb.length, 64, (byte) 54);
        Arrays.fill(k_opad, keyb.length, 64, (byte) 92);

        for (int i = 0; i < keyb.length; i++) {
            k_ipad[i] = (byte) (keyb[i] ^ 0x36);
            k_opad[i] = (byte) (keyb[i] ^ 0x5c);
        }

        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(k_ipad);
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
            byte dg[] = digest.digest();
            digest.reset();
            digest.update(k_opad);
            digest.update(dg, 0, 16);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f' };
        byte[] md = digest.digest();
        int j = md.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = md[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
    }
}
/* [BIRD][BIRD_SYSTEM_ENCRYPT][防刷机]xujing 20170628 end */
