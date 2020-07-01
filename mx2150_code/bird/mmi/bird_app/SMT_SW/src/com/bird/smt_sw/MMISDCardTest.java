package com.bird.smt_sw;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import android.content.ComponentName;
import android.os.storage.StorageManager;
import com.mediatek.storage.StorageManagerEx;
import android.content.SharedPreferences;
import android.view.KeyEvent;

//[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]begin
public class MMISDCardTest extends MMINewActivity {
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private TextView mNote;
    private TextView sdallsize;
    private TextView sdfreesize;
    private WakeLock mWakeLock = null;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private static final String TAG = "MMISDCardTest";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdcard_test);
        setTitle(R.string.sd_card);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
        mNote = (TextView) findViewById(R.id.note_title);
        sdallsize = (TextView) findViewById(R.id.allsize);
        sdfreesize = (TextView) findViewById(R.id.freesize);

		
        if(getExternalStoragePath(this) != null){
            mNote.setText(R.string.sd_note_have);
        } else {
            mNote.setText(R.string.sd_note_no);
            sdallsize.setVisibility(View.INVISIBLE);
            sdfreesize.setVisibility(View.INVISIBLE);
            return;
        }
        String path = getExternalStoragePath(this);
		if(path != null){
            StatFs sf = new StatFs(path);
            android.util.Log.d("shujiang","path:"+path+",sf:"+sf);
            long blockSize = sf.getBlockSize();
            long blockCount = sf.getBlockCount();
            long availCount = sf.getAvailableBlocks();
            sdallsize.append(blockCount*blockSize/1024+"KB");
            sdfreesize.append(availCount*blockSize/1024+"KB");
        }
            
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

	private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMISDCardTest.this.getString(R.string.sd_card), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			mEditor.putInt(MMISDCardTest.this.getString(R.string.sd_card), Test_launcherActivity.FAIL);
			mEditor.commit();
			finish();
		}
	};
	private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
		public void onClick(View v) {
			
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMISDCardTest.this, MMISDCardTest.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_HOME){
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU){
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			Intent intent = new Intent();
			ComponentName comp = new ComponentName(MMISDCardTest.this, MMISDCardTest.class);
			intent.setComponent(comp);
			startActivity(intent);
			finish();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			mEditor.putInt(MMISDCardTest.this.getString(R.string.sd_card), Test_launcherActivity.PASS);
			mEditor.commit();
			finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}
    
    public String getExternalStoragePath(Context context) {
        String storagePath = null;
        StorageManager storageManager = null;
        storageManager = StorageManager.from(context);
        storagePath = StorageManagerEx.getExternalStoragePath();
        if (storagePath == null || storagePath.isEmpty()) {
            return null;
        }
        if (!Environment.MEDIA_MOUNTED.equals(storageManager.getVolumeState(storagePath))) {
            return null;
        }
        return storagePath;
    }
    
}
//[BIRD][BIRD_SMT_SW_SDCARD][SD卡测试][zhangaman][20160726]end
