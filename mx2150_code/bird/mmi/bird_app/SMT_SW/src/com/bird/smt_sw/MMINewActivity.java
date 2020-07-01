package com.bird.smt_sw;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.Context;

public class MMINewActivity extends Activity {

        private WakeLock mWakeLock = null;
        
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		return;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    if (event.getRepeatCount() > 0 && event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
	        return true;
	    }
	    return super.dispatchKeyEvent(event);
	}
	
	@Override
	public void onAttachedToWindow() {
		//this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);
		super.onAttachedToWindow();
	}
	
    @Override
    protected void onResume() {

        super.onResume();
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
        }
    }
    
    @Override
    protected void onPause() {

        super.onPause();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
