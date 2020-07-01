package com.bird.firmwaretest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TestResultActivity extends Activity implements View.OnClickListener {
    private Button mReStartTestBtn;
    private Button mExitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_result_activity);

        mReStartTestBtn = (Button) findViewById(R.id.restart_btn);
        mExitBtn = (Button) findViewById(R.id.exit_btn);

        mReStartTestBtn.setOnClickListener(this);
        mExitBtn.setOnClickListener(this);
        
        mReStartTestBtn.setVisibility(View.GONE);
        mExitBtn.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.restart_btn:
                finish();
                break;
            case R.id.exit_btn:
                onBackPressed();
                break;
        }

    }
}
