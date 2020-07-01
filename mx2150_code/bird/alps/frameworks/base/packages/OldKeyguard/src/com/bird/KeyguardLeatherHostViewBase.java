package com.android.keyguard;

import android.content.Context;
import android.widget.RelativeLayout;
import android.util.AttributeSet;

public abstract class KeyguardLeatherHostViewBase extends RelativeLayout {
	
	public KeyguardLeatherHostViewBase(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public KeyguardLeatherHostViewBase(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	abstract public void onCoverClosed();

	abstract public void onCoverOpen();

}
