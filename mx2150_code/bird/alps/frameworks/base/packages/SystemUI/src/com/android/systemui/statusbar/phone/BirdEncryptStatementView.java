/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]BEGIN */
package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.android.systemui.R;

public class BirdEncryptStatementView extends LinearLayout {
    
    private final static int DURATION = 10000;
    
    private CountDownTimer mCountdownTimer;
    
    private Button mOkButton;
    
    public BirdEncryptStatementView(Context context) {
        this(context, null);
    }
    
    public BirdEncryptStatementView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    

    public BirdEncryptStatementView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.bird_encrypt_statement_view, this, true);
        mOkButton = (Button)findViewById(R.id.ok);
        mOkButton.setEnabled(false);
    }
    
    public void setOkClickListener(View.OnClickListener listener) {
        mOkButton.setOnClickListener(listener);
    }
    
    @Override
    protected void onAttachedToWindow() {
        // TODO Auto-generated method stub
        super.onAttachedToWindow();
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(DURATION, 1000) {
    
                @Override
                public void onTick(long millisUntilFinished) {
                    mOkButton.setText(getContext().getString(android.R.string.ok) + "(" + millisUntilFinished/1000 + ")");
                }
    
                @Override
                public void onFinish() {
                    mOkButton.setText(getContext().getString(android.R.string.ok));
                    mOkButton.setEnabled(true);
                }
            }.start();
        }
    }
}
/*[BIRD][BIRD_ENCRYPT_SPACE][加密空间][yangbo][20170629]END */
