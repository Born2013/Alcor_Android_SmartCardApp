package com.android.server.am;

import android.graphics.drawable.Drawable;


public class AppInfo {
	private String mAppName;
	private String mPkgName;
	private String mInfo;
	public Drawable mIcon;
	private boolean mAutoEnable = false; 
	public String mAutoEnableInfo;
	public void setAppName(String appName) {
		this.mAppName = appName;
	}
	public void setPackageName(String pkgName) {
		this.mPkgName = pkgName;
	}
	public void setInfo(String info) {
		this.mInfo = info;
	}
	public void setIcon(Drawable icon) {
		this.mIcon = icon;
	}
	public void setAutoEnable(boolean enable) {
		this.mAutoEnable = enable;
	}
	public void setAutoEnableInfo(String enableInfo) {
		this.mAutoEnableInfo = enableInfo;
	}
	public String getName() {
		return mAppName;
	}
	public String getPackageName() {
		return mPkgName;
	}
	public String getInfo() {
		return mInfo;
	}	
	public Drawable getIcon() {
		return mIcon;
	}
	public boolean getAutoEnable() {
		return mAutoEnable;
	}
	public String getAutoEnableInfo() {
		return mAutoEnableInfo;
	}
}

