/*BIRD_SMALL_VIEW_WINDOW yeyunfeng 20160301 begin*/
package com.android.keyguard;

import android.content.Context;
import android.widget.RelativeLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
import android.os.SystemProperties;
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/

public class KeyguardLeatherHostView extends KeyguardLeatherHostViewBase {
	private static final String TAG = "KeyguardLeatherHostView";

	private KeyguardLeatherCoverView mClock;
    public static final int XIAMI_SONG_PREPARE = 1001;
    public static final int XIAMI_SONG_PLAY = 1002;
	/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
    private KeyguardLeatherCoverViewStyle1 mViewStyle1;
    public static final boolean isSmallWinsowStyle1 = SystemProperties.get("ro.bird_hall_leather_style").equals("style1");
	/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
	@Override
	public void onCoverClosed() {
		if(mClock != null){
			mClock.onCoverClosed();
		}
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
		if(isSmallWinsowStyle1 && mViewStyle1 != null){
			mViewStyle1.onCoverClosed();
		}
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
	}
	
	@Override
	public void onCoverOpen() {
		if(mClock != null){
			mClock.onCoverOpen();
		}
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
		if(isSmallWinsowStyle1 && mViewStyle1 != null){
			mViewStyle1.onCoverOpen();
		}
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
	}

	public KeyguardLeatherHostView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public KeyguardLeatherHostView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
        if (isSmallWinsowStyle1) {
            mViewStyle1 = (KeyguardLeatherCoverViewStyle1) findViewById(R.id.zzzz_bird_clock_and_music);
            mViewStyle1.setVisibility(View.VISIBLE);
        } else {
            mClock = (KeyguardLeatherCoverView) findViewById(R.id.clock_bird);
            mClock.setVisibility(View.VISIBLE);
        }
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
	}
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        /*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
        if (isSmallWinsowStyle1 && mViewStyle1 != null && mViewStyle1.getVisibility() == View.VISIBLE) {
            KeyguardLeatherScrollView scrollView = (KeyguardLeatherScrollView)mViewStyle1.findViewById(R.id.bird_scroll_view);
            scrollView.onInterceptTouchEvent(event);
            return true;
        } else {
            return mClock.onTouchEvent(event);
        }
		/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
    }
}
