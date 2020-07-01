package com.sensortek.stkhealthcare2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.widget.RelativeLayout;

public class NoticeDialog extends Activity {

    private ImageView mDialog;
    private CheckBox cbNeverShow;
    private RelativeLayout mActionClose;
    private final int delayTimeMS = 800;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notice_dialog);
        mActionClose= (RelativeLayout)findViewById(R.id.action_close);
        if(mActionClose != null) {
            mActionClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor editor = getSharedPreferences(MainActivity.SHARED_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(MainActivity.CAPTION,true);
                    editor.commit();                 
                    finish();
                    overridePendingTransition(0, R.anim.slide_out_bottom);
                }
            });
        }
    }

	@Override
	public void onBackPressed() {
	}
}
