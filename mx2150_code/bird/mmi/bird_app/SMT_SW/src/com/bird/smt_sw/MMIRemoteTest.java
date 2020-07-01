package com.bird.smt_sw;

import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import android.os.Handler;
import android.os.Message;
import com.etek.ircomm.RemoteComm;
import android.widget.Toast;
import android.view.KeyEvent;
import android.content.ComponentName;

//[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]begin
public class MMIRemoteTest extends MMINewActivity {
    private static final String TAG = "MMIRemoteTest";
    private Button correctButton;
    private Button errorButton;
    private Button resetButton;
    private TextView mRemoteInfo;
    private File sendFile;
    private FileWriter mFileWriter = null;
    private Button test;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mEditor;
    private FileReader mFileReader;
    private static final String ET_IR_SEND = "/sys/class/etek/sec_ir/ir_send";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_info);
        setTitle(R.string.remote);
        mPreferences = getSharedPreferences("mmitest", MODE_PRIVATE);
        mEditor = mPreferences.edit();
        mRemoteInfo = (TextView) findViewById(R.id.note_title);
        correctButton = (Button) findViewById(R.id.correct);
        errorButton = (Button) findViewById(R.id.error);
        resetButton = (Button) findViewById(R.id.reset);
        sendFile = new File(ET_IR_SEND);
        test = (Button) findViewById(R.id.send);
        test.setVisibility(View.GONE);
        correctButton.setOnClickListener(mCorrectButtonHandler);
        errorButton.setOnClickListener(mErrorButtonHandler);
        resetButton.setOnClickListener(mRestdButtonHandler);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (RemoteComm.initRemote() == false) {
            Toast toast = Toast.makeText(this, R.string.chip_open_failed, Toast.LENGTH_LONG);
            toast.show();
        }
        char[] buf = new char[20];
        Boolean ret = readData(buf);
        mRemoteInfo.setText(getResources().getString(R.string.remote_version) + String.valueOf(buf));
        RemoteComm.finishRemote();
    }

    private View.OnClickListener mCorrectButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(MMIRemoteTest.this.getString(R.string.remote), Test_launcherActivity.PASS);
            mEditor.commit();
            finish();
        }
    };

    private View.OnClickListener mErrorButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            mEditor.putInt(MMIRemoteTest.this.getString(R.string.remote), Test_launcherActivity.FAIL);
            mEditor.commit();
            finish();
        }
    };

    private View.OnClickListener mRestdButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent();
            ComponentName comp = new ComponentName(MMIRemoteTest.this, MMIRemoteTest.class);
            intent.setComponent(comp);
            startActivity(intent);
            finish();
        }
    };

    private Boolean readData(char[] buf) {
        int len;
        try {
            mFileReader = new FileReader(sendFile);
            len = mFileReader.read(buf);
            mFileReader.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(keyCode == KeyEvent.KEYCODE_BACK){//禁用back按键
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
//[BIRD][BIRD_SMT_SW_REMOTE][贴片测试_红外测试][zhangaman][20170515]end
