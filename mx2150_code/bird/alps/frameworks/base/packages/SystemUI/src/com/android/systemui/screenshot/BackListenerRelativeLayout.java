package com.android.systemui.screenshot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

public class BackListenerRelativeLayout extends RelativeLayout {

    private DispatchKeyEventListener mDispatchKeyEventListener;

    public BackListenerRelativeLayout(Context context) {
        super(context);
    }

    public BackListenerRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackListenerRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        android.util.Log.v("gaoweitest" , "BackListenerRelativeLayout get this event");
        if (mDispatchKeyEventListener != null) {
            return mDispatchKeyEventListener.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    public DispatchKeyEventListener getDispatchKeyEventListener() {
        return mDispatchKeyEventListener;
    }

    public void setDispatchKeyEventListener(DispatchKeyEventListener mDispatchKeyEventListener) {
        this.mDispatchKeyEventListener = mDispatchKeyEventListener;
    }

    //监听接口
    public static interface DispatchKeyEventListener {
        boolean dispatchKeyEvent(KeyEvent event);
    }

}
