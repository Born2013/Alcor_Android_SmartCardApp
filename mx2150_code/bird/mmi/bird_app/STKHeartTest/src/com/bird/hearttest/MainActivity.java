package com.sensortek.stkhealthcare2;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.text.format.Time;
import java.util.Calendar;
import com.sensortek.stkhealthcare2.provider.Heart;
import android.content.ContentResolver;
import android.os.Build;
import android.view.View;
import android.graphics.Color;
import android.app.ActionBar;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.content.Context;
import android.view.Gravity;
//import hwdroid.dialog.AlertDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
//import hwdroid.dialog.DialogInterface;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.provider.Settings;


public class MainActivity extends Activity implements PopMenu.OnPopMenuListener{

    private static final String TAG = "HeartMainActivity";
    public static final String SHARED_NAME = "heart_name";
    public static final String CAPTION = "caption";
    private RelativeLayout mActionHelp;
    private RelativeLayout mActionMenu;
    private PopMenu mMenu;
    public static final int HISTORY_ITEM_DELETE = 400;
    private DeleteThread mDeleteThread;
    private View decorView;
    private PulseFragment mPulseFragment;
    private HeartFragment mHeartFragment;
    private int mStartx;
    private int mStarty;
    private View mHeartView;
    private RelativeLayout mTitleRelativeLayout;
    private boolean mEffective = false;
    private boolean mSlideY = false;
    private int mPulseFragmentHeight;
    private int mOffset = 90;
    private int mMaxDistance = 300;
    private int mDifference;
    private int STATE_TOP = 0;
    private int STATE_BOTTOM = 1;
    private int mCircleState = STATE_BOTTOM;
    private int mCurrentY;
    private boolean mHasReached = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPulseFragment = (PulseFragment)getFragmentManager().findFragmentById(R.id.circle_fragment);
        mHeartFragment = (HeartFragment)getFragmentManager().findFragmentById(R.id.history_value_fragment);
        mHeartView = mPulseFragment.getView();
        final Intent dlgIntent = new Intent(this, NoticeDialog.class);
        SharedPreferences pref = getSharedPreferences(SHARED_NAME, MODE_PRIVATE);
        boolean showCaption = pref.getBoolean(CAPTION, false);
        if (!showCaption) {
            startActivity(dlgIntent);   	
        }
        mActionHelp= (RelativeLayout)findViewById(R.id.action_help);
        if(mActionHelp != null) {
            mActionHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(dlgIntent);
                    MainActivity.this.overridePendingTransition(R.anim.slide_in_top, R.anim.slide_no);
                }
            });
        }
        
        mActionMenu= (RelativeLayout)findViewById(R.id.action_menu);
        if(mActionMenu != null) {
            mActionMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mMenu == null) {
                        Context context = MainActivity.this;
                        mMenu = PopMenu.build(context, Gravity.TOP);
                        mMenu.setOnIemClickListener(MainActivity.this);
                        mMenu.addItem(HISTORY_ITEM_DELETE, context.getString(R.string.clear_data));
                    }
                    
                    mMenu.setEnable(HISTORY_ITEM_DELETE,!Heart.isEmpty(getContentResolver()) 
                                                       && PulseFragment.mState != PulseFragment.WAIT_FONT
                                                       && PulseFragment.mState != PulseFragment.DETECTION);
                    mMenu.show();
                }
            });
        }
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
	}

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        //Settings.System.putInt(getContentResolver(), Settings.System.BIRD_HEART_RATE_STATUS, 1);
        mMaxDistance = getResources().getDimensionPixelOffset(R.dimen.slide_max_diatance);
    }

    @Override
    protected void onPause() {
    	// TODO Auto-generated method stub
    	super.onPause();
       //Settings.System.putInt(getContentResolver(),Settings.System.BIRD_HEART_RATE_STATUS,0);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mMenu != null) {
            mMenu.immediateDismiss();
        }
    }

    @Override
    public void onMenuItemClick(int id) {
        switch (id) {
            case HISTORY_ITEM_DELETE:
                doDelAction();
                break;

            default:
                Log.e(TAG, "unhandled button clicked");
                break;
        }
    }


    private void doDelAction() {
        String message = getResources().getString(R.string.clear_all_data);;
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle(R.string.clear_data);
        build.setMessage(message);
        build.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                mDeleteThread = new DeleteThread();
                mDeleteThread.start();
            }
        });
        build.setNegativeButton(R.string.cancel,null);
        AlertDialog dialog = build.create();
        dialog.show();
    }

    private class DeleteThread extends Thread {
        @Override
        public void run() {
            Heart.deleteHearts(MainActivity.this.getContentResolver());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mEffective = false;
                mSlideY = false;
                mHasReached = false;
                if (PulseFragment.mState == PulseFragment.NO_START || PulseFragment.mState == PulseFragment.DETECTION_COMPLETE) {
                    mStartx = (int) ev.getX();  
                    mStarty = (int) ev.getY();  
                    mPulseFragmentHeight = mPulseFragment.getView().getMeasuredHeight();
                    int HeartFragmentheight = mHeartFragment.getView().getMeasuredHeight();
                    int HeartFragmentwidth = mHeartFragment.getView().getMeasuredWidth();
                    int HeartFragmentTop = mHeartFragment.getView().getTop();
                    int HeartFragmentLeft = mHeartFragment.getView().getLeft();
                    mTitleRelativeLayout = mHeartFragment.getTitleLayout();
                    if (mTitleRelativeLayout != null) {
                        int titleHeight = mTitleRelativeLayout.getMeasuredHeight();
                        if (mStartx > HeartFragmentLeft && mStartx < HeartFragmentLeft + HeartFragmentwidth
                            && mStarty > HeartFragmentTop - mOffset && mStarty < HeartFragmentTop + titleHeight + mOffset) {
                            mEffective = true;
                        } else {
                            mEffective = false;
                        }                  
                    } else {
                        mEffective = false;
                    }
                } else {
                    mEffective = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mEffective) {
                    final int currentX = (int) ev.getX();  
                    final int currentY = (int) ev.getY(); 
                    if (mCircleState == STATE_TOP) {
                        mDifference = currentY - mStarty;
                    } else {
                        mDifference = mStarty - currentY;
                    }
                    if (!mHasReached) {
                        if (Math.sqrt(Math.pow((currentX - mStartx),2) + Math.pow((currentY - mStarty),2)) >= 60) {
                            mHasReached = true;
                            if (Math.abs(currentY - mStarty) > Math.abs(currentX - mStartx)) {
                                mSlideY = true;
                            } else {
                                mSlideY = false;
                                mEffective = false;
                            }
                        } else {
                            mSlideY = false;
                        }
                    }
                    if (mEffective && mSlideY) {
                        if (mDifference > mMaxDistance) {
                            mDifference = mMaxDistance;
                        } else if (mDifference < 0) {
                            mDifference = 0;
                        }
                        LayoutParams lp = (LayoutParams) mPulseFragment.getView().getLayoutParams();
                        if (mCircleState == STATE_TOP) {
                            mCurrentY = mPulseFragmentHeight + mDifference;
                        } else {
                            mCurrentY = mPulseFragmentHeight - mDifference;
                        }
                        lp.height = mCurrentY;
                        mPulseFragment.getView().setLayoutParams(lp);
                        if (mCircleState == STATE_TOP) {
                            mPulseFragment.changeAlpha((float)mDifference/mMaxDistance);
                        } else {
                            mPulseFragment.changeAlpha(1f - (float)mDifference/mMaxDistance);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:              
                if (mEffective && mSlideY) {
                    int endy;
                    if (mCircleState == STATE_BOTTOM) {
                        if (mDifference > mMaxDistance / 4) {
                            endy = mPulseFragmentHeight - mMaxDistance;
                            mCircleState = STATE_TOP;
                        } else {
                            endy = mPulseFragmentHeight;
                            mCircleState = STATE_BOTTOM;
                        }
                    } else {
                        if (mDifference > mMaxDistance / 4) {
                            endy = mPulseFragmentHeight + mMaxDistance;
                            mCircleState = STATE_BOTTOM;
                        } else {
                            endy = mPulseFragmentHeight;
                            mCircleState = STATE_TOP;
                        }                  
                    }
                    startAnimator(endy);
                }
                break;
            default:
                break;
        }
        if (mEffective && mSlideY) {
            return true;
        } else {
            return super.dispatchTouchEvent(ev);
        }
        
    }

    private void startAnimator(int endy) {
        final int lastValue = endy;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(mCurrentY, endy);
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int CurrentValue = (int) animation.getAnimatedValue();
                int difference = Math.abs(CurrentValue - lastValue);
                LayoutParams lp = (LayoutParams) mPulseFragment.getView().getLayoutParams();
                lp.height = CurrentValue;
                if (mCircleState == STATE_BOTTOM) {
                    mPulseFragment.changeAlpha(1f - (float)difference/mMaxDistance);
                } else {
                    mPulseFragment.changeAlpha((float)difference/mMaxDistance);
                }
                mPulseFragment.getView().setLayoutParams(lp);
            }
        });
        valueAnimator.start();
    }

    public void startToBottomAnimator() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(mPulseFragment.getView().getMeasuredHeight(), mPulseFragment.getView().getMeasuredHeight() + mMaxDistance);
        valueAnimator.setDuration(500);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int CurrentValue = (int) animation.getAnimatedValue();
                LayoutParams lp = (LayoutParams) mPulseFragment.getView().getLayoutParams();
                lp.height = CurrentValue;
                if (mCircleState == STATE_BOTTOM) {
                    mPulseFragment.changeAlpha((float)CurrentValue/mMaxDistance);
                } else {
                    mPulseFragment.changeAlpha(1f - (float)CurrentValue/mMaxDistance);
                }
                mPulseFragment.getView().setLayoutParams(lp);
            }
        });
        valueAnimator.start();
        mCircleState = STATE_BOTTOM;
    }

    public boolean isTop() {
        if (mCircleState == STATE_TOP) {
            return true;
        } else {
            return false;
        }
    }
}
