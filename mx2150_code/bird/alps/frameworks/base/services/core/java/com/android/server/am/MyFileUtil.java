package com.android.server.am;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.AtomicFile;
import java.nio.charset.StandardCharsets;
import android.os.FileUtils;
import static android.os.Process.SYSTEM_UID;
import static android.os.Process.PACKAGE_INFO_GID;
import libcore.io.IoUtils;
//[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] BEGIN
import android.util.Log;
import com.android.internal.R;
//[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] END

public class MyFileUtil {

	public static void writeInternal(ArrayList<AppInfo> mPackageslist) {
		synchronized (mPackageslist) {
			AtomicFile file = getFile();
			FileOutputStream f = null;
			try {
				f = file.startWrite();
				BufferedOutputStream out = new BufferedOutputStream(f);
				FileUtils.setPermissions(file.getBaseFile().getPath(), 0640,
						SYSTEM_UID, PACKAGE_INFO_GID);
				StringBuilder sb = new StringBuilder();
				if (mPackageslist != null) {
					for (int i = 0; i < mPackageslist.size(); i++) {
						AppInfo info = new AppInfo();
						info = mPackageslist.get(i);
						sb.setLength(0);
						sb.append(info.getPackageName());
						sb.append(' ');
						sb.append(info.getAutoEnable() ? "true" : "false");
						sb.append('\n');
						out.write(sb.toString().getBytes(
								StandardCharsets.US_ASCII));
					}
				}
				out.flush();
				file.finishWrite(f);
			} catch (IOException e) {
				if (f != null) {
					file.failWrite(f);
				}
				
			}
		}

	}
    
	public static ArrayList<AppInfo> readLP() throws IOException {
		ArrayList<AppInfo> mAutoList = new ArrayList<AppInfo>();
		AtomicFile file = getFile();
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(file.openRead());
			StringBuffer sb = new StringBuffer();
			while (true) {
				AppInfo info = new AppInfo();
				String packageName = readToken(in, sb, ' ');
				if (packageName == null) {
					break;
				}
				String mAutoEnable = readToken(in, sb, '\n');
				info.setPackageName(packageName);
				info.setAutoEnable(mAutoEnable.equals("true") ? true : false);
				mAutoList.add(info);
			}
		} catch (FileNotFoundException expected) {

		} catch (IOException e) {

		} finally {
            IoUtils.closeQuietly(in);
        }
		return mAutoList;

	}

	public static String readToken(InputStream in, StringBuffer sb, char endOfToken)
			throws IOException {
		sb.setLength(0);
		while (true) {
			int ch = in.read();
			if (ch == -1) {
				if (sb.length() == 0) {
					return null;
				}
				throw new IOException("Unexpected EOF");
			}
			if (ch == endOfToken) {
				return sb.toString();
			}
			sb.append((char) ch);
		}
	}
	
	public static AtomicFile getFile() {
		File dataDir = Environment.getDataDirectory();
		File systemDir = new File(dataDir, "system");
		File fname = new File(systemDir, "autoapp.list");
		return new AtomicFile(fname);
	}
	public static void getAppNotSystem(Context context) throws IOException {
	    List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
	    ArrayList<AppInfo> applist = new ArrayList<AppInfo>();
		//[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] BEGIN
	    String[] defalutonApp = context.getResources().getStringArray(R.array.automanager_defaluton);
		//[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] END
		if(packages != null) {
			for(int i=0;i<packages.size();i++) { 
				PackageInfo packageInfo = packages.get(i); 
				if((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)==0) {//非系统应用
					AppInfo mAppInfo =new AppInfo();  
					mAppInfo = ContainsAppInfo(packageInfo);
					if(mAppInfo == null || mAppInfo.getPackageName() == null){
					    //[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] BEGIN
					    mAppInfo.setPackageName(packageInfo.packageName);
						mAppInfo.setAutoEnable(false);
					    for(int j=0 ;j<defalutonApp.length;j++) {
	                        if(defalutonApp[j] != null && packageInfo.packageName.equals(defalutonApp[j])) {
	                            mAppInfo.setAutoEnable(true);
	                            break;
			                } 
		                }
		                //[BIRD][BIRD_KUSAI_AUTO_MANAGER][库塞风格自启动管理,增加白名单][chenguangxiang][20170720] END
					} else if(mAppInfo.getPackageName().equals("")) {
                    	mAppInfo.setPackageName(packageInfo.packageName); 
						mAppInfo.setAutoEnable(false);
                	}
					applist.add(mAppInfo);					
				}			
			}
		}
		
		writeInternal(applist);
	}
	public static AppInfo ContainsAppInfo(PackageInfo pkgInfo) throws IOException {
		ArrayList<AppInfo> applist = new ArrayList<AppInfo>();
		AppInfo info = new AppInfo();
		applist = readLP();
		if(applist != null && applist.size() > 0) {
			for(int i = 0; i < applist.size(); i++ ) {
				if(pkgInfo.packageName.equals(applist.get(i).getPackageName())){
					info = applist.get(i);
				}
			}
		}
		return info;
	}
}
