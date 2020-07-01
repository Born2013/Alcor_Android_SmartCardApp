package com.android.server;

import com.bird.clam.ClamNative;
import android.content.Context;
import android.app.IHallcoverManager;

public class HallcoverManagerService extends IHallcoverManager.Stub{

	private final Context mContext;

	HallcoverManagerService(Context context) {
		super();
		mContext = context;
	}

	
	public boolean readClamState(){
		return ClamNative.readClamState();
	}
}
