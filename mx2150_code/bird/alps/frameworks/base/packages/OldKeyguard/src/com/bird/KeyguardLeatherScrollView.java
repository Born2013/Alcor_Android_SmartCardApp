package com.android.keyguard;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.os.SystemProperties;
import android.provider.Settings;
import android.widget.RelativeLayout;

/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]begin*/
public class KeyguardLeatherScrollView extends DefinedScrollView {
	private BirdKeyguardLeatherStandByScreen mLeatherStandbyView;
	private View mLeatherMusic;

	public KeyguardLeatherScrollView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public KeyguardLeatherScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}

	private void init(Context context) {
		// TODO Auto-generated method stub
        mLeatherStandbyView = (BirdKeyguardLeatherStandByScreen) View.inflate(context,
        	R.layout.zzzz_keyguard_leather_analogclock, null);
        mLeatherMusic = (View)View.inflate(context,
        	R.layout.zzzzz_keyguard_leather_music_view, null);

        addView(mLeatherStandbyView);
        addView(mLeatherMusic);
        RelativeLayout standbyView = (RelativeLayout)mLeatherStandbyView.findViewById(R.id.bird_standby_view);
        RelativeLayout musicView = (RelativeLayout)mLeatherMusic.findViewById(R.id.bird_music_view);
        mLeatherStandbyView.updateTime();
	}

}
/*[BIRD][BIRD_SMALL_VIEW_WINDOW_STYLE][皮套风格1][zhangaman][20170524]end*/
