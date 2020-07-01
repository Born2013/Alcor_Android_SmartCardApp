package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
/**
 * Created by liuenshuo on 2016/11/20.
 */
public class BirdMarqureeTextView extends TextView {
    public BirdMarqureeTextView(Context context) {
        super(context);
    }

    public BirdMarqureeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BirdMarqureeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BirdMarqureeTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    @Override
    public boolean isFocused(){
        return true;

    }
}
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
