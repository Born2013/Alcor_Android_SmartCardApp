/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.screenshot;

import android.app.Instrumentation;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification.BigPictureStyle;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.input.InputManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.Display;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.systemui.R;

import android.content.pm.ActivityInfo;
import android.view.KeyEvent;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import java.util.List;
import android.app.KeyguardManager;

/**
 * TODO:
 *    2.如何加速，如何拼接完全；
 */
class BirdGlobalScreenshot extends Activity{
    private static final String TAG = "BirdGlobalScreenshot";
    
    private final int mPreviewWidth;
    private final int mPreviewHeight;
    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private WindowManager.LayoutParams mCoverParams;
    private NotificationManager mNotificationManager;
    private KeyguardManager mKeyguardManager;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
    private Bitmap mScreenBitmap;
    private BackListenerRelativeLayout mScreenshotLayout;
    private View mCoverView;
    private ImageView mBackgroundView;
    private CropView mScreenshotView;
    private ImageView mScreenshotFlash;
    private AnimatorSet mScreenshotAnimation;

    private int mNotificationIconSize;
    private AsyncTask<Void, Void, Void> mSaveInBgTask;

    private MediaActionSound mCameraSound;

    private ImageView mOverlayView;
    private RelativeLayout mUserOptionLayout;
    private LinearLayout mCancelLayout;
    private LinearLayout mSaveLayout;
    private ImageButton mShareBtn;
    private ImageButton mPreBtn;
    private LinearLayout mNextLayout;
    private LinearLayout mCheckLayout;
    private LinearLayout mTipsForNotSupportLayout;
    private TextView mTipsString;
    private int mPageCount;
    private ImageView mNextSrcImageView;
    private ImageView mCheckSrcImageView;
    private static final int MAX_CROP_PAGE_COUNT = 10;
    public static int mScreenWidth;
    public static int mScreenHeight;
    private Bitmap mScreenLastBitmapTemp;
    private Bitmap mScreenNextBitmap;
    Runnable mFinisher;
    private static final int SCREENSHOT_NEXT_CLICK_MSG = 0;
    private static final int SCREENSHOT_NEXT_FINISH_MSG = 1;
    private static final int SCREENSHOT_NEXT_WAITTOFINISH_MSG = 2;
    private static final int SCREENSHOT_QUIT_LONG_SCREENSHOT_MSG = 3;
    private float mDownY; //屏幕高度的3/4点
    private int mMovement;//大约半个屏幕
    private boolean isScrollToEnd;
    private int[] mPixels1, mPixels2, mPixels3, mPixels4, mPixels1Bc, mPixels2Bc, mPixels3Bc, mPixels4Bc;
    private final int mCompareWidth = 200;
    private final int mCompareStartX = 30;
    private int mCompareHeight;
    private int mCompareMaxHeight;
    private int mDividingHeight;
    private int mXpoint;
    int offSetYY;
    private int mEndY;
    private int mScrollCount;
    private boolean mChangeDistance;
    private boolean mEndScreenShot;
    private boolean mLongScreentShot;
    private boolean mTouch;
   
    String[] mTags = new String[]{"bird_cancel_btn","bird_save_btn", "bird_share_btn",
            "bird_pre_btn", "bird_next_btn", "bird_check_btn"};
    final Runnable mHide = new Runnable() {
        @Override
        public void run() {
            Animation toastExitAnimation = AnimationUtils.loadAnimation(mContext, R.anim.bird_toast_exit);
            mTipsForNotSupportLayout.startAnimation(toastExitAnimation);
        }
    };
    

    /**
     * @param context everything needs a context :(
     */
    public BirdGlobalScreenshot(Context context) {
        Resources r = context.getResources();
        mContext = context;
        mDisplayMatrix = new Matrix();
        // Setup the window that we are going to use
        mWindowLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);

        mWindowLayoutParams.setTitle("ScreenshotAnimation");
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);

        // Get the various target sizes
        mNotificationIconSize =
            r.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        // determine the optimal preview size
        int panelWidth = 0;
        try {
            panelWidth = r.getDimensionPixelSize(R.dimen.notification_panel_width);
        } catch (Resources.NotFoundException e) {
        }
        if (panelWidth <= 0) {
            // includes notification_panel_width==match_parent (-1)
            panelWidth = mDisplayMetrics.widthPixels;
        }
        mPreviewWidth = panelWidth;
        mPreviewHeight = r.getDimensionPixelSize(R.dimen.notification_max_height);

        mScreenWidth = mDisplayMetrics.widthPixels;
        mScreenHeight = mDisplayMetrics.heightPixels;
        mDownY = (float)mScreenHeight * 3 / 4;
        Log.i(TAG, "mScreenWidth" + mScreenWidth + ", mScreenHeight" + mScreenHeight);
        // Setup the Camera shutter sound
        mCameraSound = new MediaActionSound();
        mCameraSound.load(MediaActionSound.SHUTTER_CLICK);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mCoverParams = new WindowManager.LayoutParams(
                mScreenWidth -1, mScreenHeight, 1, 0,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
    }


    public void init() {
        LayoutInflater layoutInflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Inflate the screenshot layout        
        mScreenshotLayout = (BackListenerRelativeLayout)layoutInflater.inflate(R.layout.bird_global_screenshot, null);
        mScreenshotLayout.setDispatchKeyEventListener(mDispatchKeyEventListener);
        mCoverView = layoutInflater.inflate(R.layout.bird_screen_shot_cover, null);
        mCoverView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Intercept and ignore all touch events
                Log.d(TAG,"onTouch");
                mTouch = true;
                return true;
            }
        });
        mOverlayView = (ImageView) mScreenshotLayout.findViewById(R.id.screenshot_overlay_layout);

        mUserOptionLayout = (RelativeLayout) mScreenshotLayout.findViewById(R.id.bird_useroption_layout);
        mBackgroundView = (ImageView) mScreenshotLayout.findViewById(R.id.global_screenshot_background);
        mScreenshotView = (CropView) mScreenshotLayout.findViewById(R.id.global_screenshot);

        mScreenshotView.setIsRegionalAction(true, 80);
        mScreenshotFlash = (ImageView) mScreenshotLayout.findViewById(R.id.global_screenshot_flash);

        mCancelLayout = (LinearLayout) mScreenshotLayout.findViewById(R.id.bird_cancel_btn);
        mCancelLayout.setOnClickListener(mScreenshotActionListener);
        mCancelLayout.setVisibility(View.VISIBLE);

        mSaveLayout = (LinearLayout) mScreenshotLayout.findViewById(R.id.bird_save_btn);
        mSaveLayout.setOnClickListener(mScreenshotActionListener);

        mShareBtn = (ImageButton) mScreenshotLayout.findViewById(R.id.bird_share_btn);
        mShareBtn.setImageResource(R.drawable.ic_screenshot_share);
        mShareBtn.setOnClickListener(mScreenshotActionListener);

        mPreBtn = (ImageButton) mScreenshotLayout.findViewById(R.id.bird_pre_btn);
        mPreBtn.setOnClickListener(mScreenshotActionListener);

        mNextLayout = (LinearLayout) mScreenshotLayout.findViewById(R.id.bird_next_btn);
        mNextLayout.setOnClickListener(mScreenshotActionListener);
        mNextSrcImageView = (ImageView) mScreenshotLayout.findViewById(R.id.bird_next_btn_src);

        /*
         *TODO:第一页不能进行选择与否？
         */
        mCheckLayout = (LinearLayout) mScreenshotLayout.findViewById(R.id.bird_check_btn);
        mCheckLayout.setOnClickListener(mScreenshotActionListener);
        mCheckLayout.setEnabled(false);
        mCheckSrcImageView = (ImageView) mScreenshotLayout.findViewById(R.id.bird_check_btn_src);
        mCheckSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_choose_dis);

        mTipsForNotSupportLayout = (LinearLayout) mScreenshotLayout.findViewById(R.id.bird_tips_for_not_support);
        mTipsString =  (TextView) mScreenshotLayout.findViewById(R.id.bird_tips);
        mScreenshotView.setOnScreenChangeListener(new CropView.OnScreenChangeListener() {
            public void onScreenChange() {
                mNextLayout.setEnabled(false);
                mNextSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_next_dis);
            }
        });

        mOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Intercept and ignore all touch events
                return false;
            }
        });
        mOverlayView.setVisibility(View.GONE);
        mMovement = (int) mScreenHeight * 1 / 2;
        mCompareHeight = 10;
        mCompareMaxHeight = 20;
        mDividingHeight = 5;
        mXpoint = 0;
        offSetYY = 0;
        mEndY = 0;
        mScrollCount = 70;
        mPageCount = 0;
        mChangeDistance = false;
        mEndScreenShot = false;
        mLongScreentShot = false;
        isScrollToEnd = false;
        mTouch = false;
    }
    
    boolean mIsRegionalAction = true;//regional or long
    void takeScreenshot(Runnable finisher, boolean statusBarVisible, boolean navBarVisible, String whichAction) {
        takeScreenshot(finisher, statusBarVisible, statusBarVisible);
        mIsRegionalAction = true;
        if ("long".equals(whichAction)) {
            mLongScreentShot = true;
            mIsRegionalAction = false;
            mNextLayout.setVisibility(View.VISIBLE);
            mCheckLayout.setVisibility(View.GONE);
            mScreenshotView.setIsRegionalAction(false, mScreenHeight * 1 / 4 + 80);
            mScreenshotView.setIndivatorsAllGone(false);
            mScreenshotView.setIsLongHeader(true);
            String[] topPackageName = getTopActivityName(mContext);
            if (topPackageName != null && topPackageName.length > 0) {
                if (topPackageName[0].equals("com.android.settings")) {
                    if (getCurrentSize() == 1) {
                        mDividingHeight = 4;
                    }
                    if (topPackageName[1].equals("com.android.settings.Settings")) {
                        mCompareHeight = 15;
                        mCompareMaxHeight = 26;
                    } else {
                        //mCompareMaxHeight = 18;
                    }
                }
                mChangeDistance = changeScrollDistance(topPackageName[1]);
                if (mChangeDistance) {
                    mMovement = (int)(mScreenHeight / 3);
                    mScrollCount = 50;
                }

                if (changeStartXpoint(topPackageName[1])) {
                    mXpoint = 20;
                    if (mCoverView != null) {
                        mCoverView.setVisibility(View.GONE);
                    }
                }

                if (isIgnoredActivity(topPackageName[1])) {
                    Message msg = mHandler.obtainMessage(SCREENSHOT_QUIT_LONG_SCREENSHOT_MSG);
                    mHandler.sendMessage(msg);
                }
            }
            //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][锁屏界面屏蔽长截屏][qianliliang][20160903] begin
            if (mKeyguardManager.isKeyguardLocked()) {
                Message message = mHandler.obtainMessage(SCREENSHOT_QUIT_LONG_SCREENSHOT_MSG);
                mHandler.sendMessage(message);
            }
            //[BIRD][BIRD_SUPER_SCREENSHOT_SUPPORT][超级截屏][锁屏界面屏蔽长截屏][qianliliang][20160903] end
        } else {
            mIsRegionalAction = true;
            mLongScreentShot = false;
            mNextLayout.setVisibility(View.GONE);
            mCheckLayout.setVisibility(View.GONE);
            mScreenshotView.setIndivatorsAllGone(false);
            mScreenshotView.setIsRegionalAction(true, 80);
        }
    }

    /**
     * Takes a screenshot of the current display and shows an animation.
     */
    void takeScreenshot(Runnable finisher, boolean statusBarVisible, boolean navBarVisible) {
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)

        mFinisher = finisher;

        mDisplay.getRealMetrics(mDisplayMetrics);
        final float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};

        float degrees = getDegreesForRotation(mDisplay.getRotation());
        boolean requiresRotation = (degrees > 0);
        Log.i(TAG, "requiresRotation = " + requiresRotation);
        mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (requiresRotation) {
            mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }
        mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
        Log.d(TAG,"takeScreenshot mScreenBitmap : " +mScreenBitmap);

        if (mScreenBitmap == null) {
            notifyScreenshotError(mContext, mNotificationManager);
            if (mScreenshotLayout != null && mScreenshotLayout.getParent() != null) {
                mWindowManager.removeView(mScreenshotLayout);
            }
            if (mCoverView != null && mCoverView.getParent() != null) {
                mWindowManager.removeView(mCoverView);
            }
            mScreenshotView = null;
            finisher.run();
            return;
        }

        if (requiresRotation) {
            // Rotate the screenshot to the current orientation
            Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                    mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(ss);
            c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
            c.rotate(degrees);
            c.translate(-dims[0] / 2, -dims[1] / 2);
            c.drawBitmap(mScreenBitmap, 0, 0, null);
            c.setBitmap(null);
            // Recycle the previous bitmap
            mScreenBitmap.recycle();
            mScreenBitmap = ss;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

        // Start the post-screenshot animation
        startActionWindows(finisher, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
                statusBarVisible, navBarVisible);
    }

    public Bitmap mergeBitmap(Bitmap oldBitmap, Bitmap nextBitmap) {
        if (null == nextBitmap) {
            return oldBitmap;
        }

        if (mScreenHeight - offSetYY >= mScreenHeight / mDividingHeight) {
            oldBitmap = Bitmap.createBitmap(oldBitmap, 0, 0, 
                    oldBitmap.getWidth(), (int)(oldBitmap.getHeight() - mScreenHeight / mDividingHeight) );//3/4屏幕开始合入
            Log.v(TAG, "mergeBitmap:"+oldBitmap.getHeight());
            Log.v(TAG, "mergeBitmap:offSetYY"+offSetYY);
            nextBitmap = Bitmap.createBitmap(nextBitmap, 0, offSetYY, 
                    nextBitmap.getWidth(), mScreenHeight - offSetYY); 
        } 
        // create the new blank bitmap 创建一个新的和SRC长度宽度一样的位图
        Bitmap newbmp = Bitmap.createBitmap(oldBitmap.getWidth(),
                oldBitmap.getHeight() + nextBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newbmp);
        // draw bg into
        cv.drawBitmap(oldBitmap, 0, 0, null);
        // draw fg into
        cv.drawBitmap(nextBitmap, 0, oldBitmap.getHeight(), null);
        // save all clip
        cv.save(Canvas.ALL_SAVE_FLAG);
        // store
        cv.restore();
        return newbmp;
    } 


    private View.OnClickListener mScreenshotActionListener = new View.OnClickListener() {
        public void onClick(View v) {
            int tagKey = 0;
            for (int i = 0; i < 6; i++) {
                if (v.getTag() != null && v.getTag().equals(mTags[i])) {
                    Log.i(TAG, "v.getTag() = " +v.getTag());
                    tagKey = i;
                    switch(tagKey) {
                        case 0://cancel button
                            mLongScreentShot = false;
                            mFinisher.run();
                            if (mScreenshotLayout != null && mScreenshotLayout.getParent() != null) {
                                mWindowManager.removeView(mScreenshotLayout);
                            }
                            if (mCoverView != null && mCoverView.getParent() != null) {
                                mWindowManager.removeView(mCoverView);
                            }
                            mScreenshotView = null;
                            break;
                        case 1://save button
                            mLongScreentShot = false;
                            saveScreenshotInWorkerThread(mFinisher);
                            if (mScreenshotLayout != null && mScreenshotLayout.getParent() != null) {
                                mWindowManager.removeView(mScreenshotLayout);
                            }
                            if (mCoverView != null && mCoverView.getParent() != null) {
                                mWindowManager.removeView(mCoverView);
                            }
                            toastShow(R.string.screenshot_saving_title);
                            break;
                        case 2://share button
                            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("image/png");
                            sharingIntent.putExtra(Intent.EXTRA_STREAM,  "Share");
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT,  "Share");
                            Intent chooserIntent = Intent.createChooser(sharingIntent,  "Share");
                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(chooserIntent);
                            break;
                        case 3://pre button
                            break;
                        case 4://next button
                            mScreenshotLayout.setVisibility(View.GONE);
                            mScreenshotView.setIsLongHeader(false);
                            mPageCount += 1;
                            mCheckLayout.setEnabled(true);
                            //保存之前的那张图片，用于判断是否 已经快滑不下去了。
                            Log.d(TAG,"next button mScreenBitmap : " +mScreenBitmap + ",mScreenNextBitmap : "+mScreenNextBitmap);
                            if (mScreenNextBitmap == null) {
                                mScreenLastBitmapTemp = mScreenBitmap;
                            } else {
                                mScreenLastBitmapTemp = mScreenNextBitmap;
                            }
                            mCheckSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_choose_nor);

                            Message msg = mHandler.obtainMessage(SCREENSHOT_NEXT_CLICK_MSG);
                            mHandler.sendMessage(msg);
                            break;
                    }

                }
            }

        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCREENSHOT_NEXT_CLICK_MSG:
                    mUserOptionLayout.setVisibility(View.GONE);
                    mScreenshotLayout.setVisibility(View.GONE);

                    new Thread() {
                        public void run() {
                            scrollScreen((int)mDownY,mMovement);
                            Message msg = mHandler.obtainMessage(SCREENSHOT_NEXT_WAITTOFINISH_MSG);
                            mHandler.sendMessage(msg);

                            /*
                            * TODO:防止滑到底部水波纹事件发生
                            * 1.优化：可以利用这200ms，去判断是否已经冲突了，如果冲突了，就不再处理后面的了
                            *
                            */
                            SystemClock.sleep(1000);
                            msg = mHandler.obtainMessage(SCREENSHOT_NEXT_FINISH_MSG);
                            mHandler.sendMessage(msg);
                        }

                    }.start();
                    //此处利用间隙时间进行合并图片，防止占用时间。0：第一页截取中；1，第二页截取中；2.第三页截取中
                    if (mPageCount >= 2 && mScreenNextBitmap != null) {
                        mScreenBitmap = mergeBitmap(mScreenBitmap, mScreenNextBitmap);
                    }

                    break;

                case SCREENSHOT_NEXT_FINISH_MSG:
                    if (mEndScreenShot) return;
                    final float[] dims = {mScreenWidth, mScreenHeight};
                    float degrees = getDegreesForRotation(mDisplay.getRotation());//mDisplay.getRotation()依然为初次截屏状态
                    boolean requiresRotation = (degrees > 0);
                    Log.i(TAG, "requiresRotation = " + requiresRotation);
                    if (requiresRotation) {
                        // Get the dimensions of the device in its native orientation
                        mDisplayMatrix.reset();
                        mDisplayMatrix.preRotate(-degrees);
                        mDisplayMatrix.mapPoints(dims);
                        dims[0] = Math.abs(dims[0]);
                        dims[1] = Math.abs(dims[1]);
                    }
                    mScreenNextBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);
                    boolean isRorationChangeOrScreenshotNOK = false;//用于标记截屏失败 或者 屏幕转动导致的截屏失败

                    if (mScreenNextBitmap == null) {
                        isRorationChangeOrScreenshotNOK = true;
                        mScreenNextBitmap = mScreenLastBitmapTemp;
                        if (mScreenNextBitmap == null || mScreenNextBitmap.isRecycled()) {
                            notifyScreenshotError(mContext, mNotificationManager);
                            if (mScreenshotLayout != null && mScreenshotLayout.getParent() != null) {
                                mWindowManager.removeView(mScreenshotLayout);
                            }
                            if (mCoverView != null && mCoverView.getParent() != null) {
                                mWindowManager.removeView(mCoverView);
                            }
                            mScreenshotView = null;

                            mFinisher.run();
                            return;
                        }
                    }

                    if (requiresRotation && isRorationChangeOrScreenshotNOK == false) {
                        // Rotate the screenshot to the current orientation
                        Bitmap ss = Bitmap.createBitmap(mScreenWidth,
                                mScreenHeight, Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(ss);
                        c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                        c.rotate(degrees);
                        c.translate(-dims[0] / 2, -dims[1] / 2);
                        c.drawBitmap(mScreenNextBitmap, 0, 0, null);
                        c.setBitmap(null);
                        // Recycle the previous bitmap
                        mScreenNextBitmap.recycle();
                        mScreenNextBitmap = ss;
                    }
                    if (mScreenshotView == null) {
                        return;
                    }

                    RectF beforeCrop = mScreenshotView.getScreenCropBounds();
                    //判断是完全重合，还是部分重合，或是完全不重合
                    boolean isSame = startCompare();
                    boolean isUnLawful = mScreenHeight - offSetYY <= mScreenHeight / mDividingHeight;
                    Log.d(TAG,"isSame : "+isSame+",isUnLawful : "+isUnLawful);
                    if (mScreenLastBitmapTemp == null || isRorationChangeOrScreenshotNOK || isSame 
                        || isUnLawful
                        || mPageCount >= MAX_CROP_PAGE_COUNT
                        || mTouch) {//完全重合 或者 部分重合
                        mScreenNextBitmap = null;
                        isScrollToEnd = true;
                        mNextLayout.setEnabled(false);
                        mCheckLayout.setEnabled(false);
                        mNextSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_next_dis);
                        mCheckSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_choose_dis);
                        mScreenshotView.setIndivatorsAllGone(true);
                        if (isSame) {
                            mTipsString.setText(R.string.bird_screenshot_done);
                        } else if (isUnLawful || mTouch) {
                            mTipsString.setText(R.string.bird_screenshot_merge_or_scroll_fail);
                        } else if (mPageCount >= MAX_CROP_PAGE_COUNT) {
                            mTipsString.setText(R.string.bird_screenshot_maxmum_count);
                        }
                        Animation toastEnterAnimation = AnimationUtils.loadAnimation(mContext, R.anim.bird_toast_enter);
                        mTipsForNotSupportLayout.setVisibility(View.VISIBLE);
                        mTipsForNotSupportLayout.startAnimation(toastEnterAnimation);
                        mHandler.postDelayed(mHide, 2500);
                    } else {//如果完全不重合，则叠加
                        mScreenshotView.setImageBitmap(mScreenNextBitmap);
                        mScreenshotView.initialize(mScreenNextBitmap);
                    }
                    if (mPageCount == 1) {
                        //等待过程中，切割第一张图，可能会被用户截掉一部分了；切割了之后，怎么对比呢？郁闷了！
                        mScreenBitmap = Bitmap.createBitmap(mScreenBitmap, (int)beforeCrop.left, (int)beforeCrop.top, 
                                (int)(beforeCrop.right - beforeCrop.left), (int)(beforeCrop.bottom - beforeCrop.top));
                    }
                    mOverlayView.setVisibility(View.GONE);
                    mUserOptionLayout.setVisibility(View.VISIBLE);
                    break;

                case SCREENSHOT_NEXT_WAITTOFINISH_MSG:
                    if (mEndScreenShot) return;
                    mOverlayView.setVisibility(View.VISIBLE);
                    mScreenshotLayout.setVisibility(View.VISIBLE);
                    break;
                    
                case SCREENSHOT_QUIT_LONG_SCREENSHOT_MSG:
                    mNextLayout.setEnabled(false);
                    mCheckLayout.setEnabled(false);
                    mNextSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_next_dis);
                    mCheckSrcImageView.setImageResource(R.drawable.zzzzz_screenshot_choose_dis);
                    mScreenshotView.setIndivatorsAllGone(true);
                    Animation toastEnterAnimation = AnimationUtils.loadAnimation(mContext, R.anim.bird_toast_enter);
                    mTipsForNotSupportLayout.setVisibility(View.VISIBLE);
                    mTipsForNotSupportLayout.startAnimation(toastEnterAnimation);
                    mHandler.postDelayed(mHide, 2500);
                    break;
            }
        }
    };

    public void scrollScreen(int yInitPoint, int moveYOffset) {
        Log.d(TAG,"START scrollScreen yInitPoint = "+yInitPoint+",moveYOffset = "+moveYOffset);
        try {
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();
            Instrumentation inst = new Instrumentation();
            int x_point = mXpoint;
            int y_offset = 0;
            y_offset = (moveYOffset - 10) / mScrollCount;
            
            if (mEndScreenShot) {
                return;
            }
            inst.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x_point, yInitPoint, 0));

            eventTime += 10;
            inst.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_MOVE, x_point, yInitPoint - 10, 0));
            int times = 0;
            int y_move_point = yInitPoint - 10;
            long startTime = SystemClock.uptimeMillis();
            while (times++ < mScrollCount && !isScrollToEnd && !mEndScreenShot && !mTouch) {
                eventTime += 10;
                int yy = y_move_point - times * y_offset;
                inst.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                        MotionEvent.ACTION_MOVE, x_point, yy, 0));
            }
            inst.sendPointerSync(MotionEvent
                    .obtain(downTime, eventTime, MotionEvent.ACTION_UP,
                            x_point, yInitPoint - moveYOffset, 0));
            mEndY = yInitPoint - moveYOffset;
            Log.d(TAG, "exit scroll times:" + times+",mEndY = "+ mEndY);
        } catch (Exception e) {
            Log.e("Exception when sendPointerSync", e.toString());
        }
    }

    private boolean compareBitmap(int compaer_height) {
        for (int i = 0; i < 200 * compaer_height; i++) {
            if (mPixels1[i] != mPixels1Bc[i] 
                || mPixels2[i] != mPixels2Bc[i] 
                || mPixels3[i] != mPixels3Bc[i] 
                || mPixels4[i] != mPixels4Bc[i]) {
                    return false;
            }
        }
        return true;
    }
    
    private int getOffsetY(int compaer_height) {
        Log.v(TAG,"getOffsetY;"+";          A;");
        mPixels1 = new int[mCompareWidth * compaer_height];//720*5
        mPixels2 = new int[mCompareWidth * compaer_height];
        mPixels3 = new int[mCompareWidth * compaer_height];
        mPixels4 = new int[mCompareWidth * compaer_height];
        
        mPixels1Bc = new int[mCompareWidth * compaer_height];
        mPixels2Bc = new int[mCompareWidth * compaer_height];
        mPixels3Bc = new int[mCompareWidth * compaer_height];
        mPixels4Bc = new int[mCompareWidth * compaer_height];
          
        if (!mScreenBitmap.isRecycled()) {
            Log.d(TAG,"mScreenBitmap.getHeight() : "+mScreenBitmap.getHeight());
            mScreenBitmap.getPixels(mPixels1, 0, mCompareWidth,
                mCompareStartX, mScreenBitmap.getHeight() - mScreenHeight / mDividingHeight - mCompareHeight,
                mCompareWidth,  compaer_height);
            mScreenBitmap.getPixels(mPixels2, 0, mCompareWidth,
                mCompareStartX, mScreenBitmap.getHeight() - mScreenHeight / mDividingHeight,
                mCompareWidth, compaer_height);
            mScreenBitmap.getPixels(mPixels3, 0, mCompareWidth,
                mCompareStartX, mScreenBitmap.getHeight() - mScreenHeight / mDividingHeight + mCompareHeight,
                mCompareWidth, compaer_height);
            mScreenBitmap.getPixels(mPixels4, 0, mCompareWidth,
                mCompareStartX, mScreenBitmap.getHeight() - mScreenHeight / mDividingHeight + mCompareMaxHeight,
                mCompareWidth, compaer_height);
        }
        
        int offsetY = mScreenHeight / (mDividingHeight + 1);
        Log.d(TAG,"mChangeDistance : "+mChangeDistance+",mEndY : "+mEndY);
        if (mChangeDistance && mEndY > 30) {
            offsetY = mEndY - 30;
        }
        Log.d(TAG,"offsetY init : "+offsetY);
        while (offsetY < mScreenHeight - mCompareMaxHeight - compaer_height) {
            mScreenNextBitmap.getPixels(mPixels1Bc, 0, mCompareWidth,
                    mCompareStartX, offsetY - mCompareHeight,
                    mCompareWidth, compaer_height);
            mScreenNextBitmap.getPixels(mPixels2Bc, 0, mCompareWidth,
                    mCompareStartX, offsetY,
                    mCompareWidth, compaer_height);
            mScreenNextBitmap.getPixels(mPixels3Bc, 0, mCompareWidth,
                    mCompareStartX, offsetY + mCompareHeight,
                    mCompareWidth, compaer_height);
            mScreenNextBitmap.getPixels(mPixels4Bc, 0, mCompareWidth,
                    mCompareStartX, offsetY + mCompareMaxHeight,
                    mCompareWidth, compaer_height);
            if (compareBitmap(compaer_height)) {
                Log.d(TAG,"getOffsetY, break: " + offsetY);
                break;
            } 
            offsetY ++;
        }
        return offsetY;
    }
    
    private boolean startCompare() {
        String nowHash = null;
        String nextHash = null;
        nowHash = BitmapUtils.getHash(mScreenLastBitmapTemp);
        nextHash = BitmapUtils.getHash(mScreenNextBitmap);
        if (nowHash != null && nextHash != null) {
            if (bitmapDistance(nowHash, nextHash) <= 5) {
                Log.d(TAG,"startCompare TRUE");
                return true;
            }
        }
        offSetYY = getOffsetY(3);
        Log.d(TAG,"offSetYY = "+offSetYY);
        if (mScreenHeight - offSetYY == mScreenHeight / mDividingHeight) {
            Log.d(TAG,"startCompare same");
            return true;
        }
        return false;
    } 
    
    public int bitmapDistance(String s1, String s2) {
       int counter = 0;
       for (int k = 0; k < s1.length();k++) {
           if(s1.charAt(k) != s2.charAt(k)) {
               counter++;
           }
       }
       Log.v(TAG, "counter    bitmapDistance" +counter);
       return counter;
   }

    /**
     * Creates a new worker thread and saves the screenshot to the media store.
     */
    private void saveScreenshotInWorkerThread(Runnable finisher) {
        SaveImageInBackgroundData data = new SaveImageInBackgroundData();
        data.context = mContext;
        RectF crop = mScreenshotView.getScreenCropBounds();
        if (mIsRegionalAction || mPageCount == 0) {
            data.image = Bitmap.createBitmap(mScreenBitmap, (int)crop.left, (int)crop.top, 
                (int)(crop.right - crop.left), (int)(crop.bottom - crop.top));
        } else {
            //判断最后一张是否保存
            mScreenBitmap = mergeBitmap(mScreenBitmap, mScreenNextBitmap);
            if(mScreenBitmap == null) {
                toastShow(R.string.bird_donot_support_long_screenshot);
                return;
            }
            data.image = mScreenBitmap;
        } 

        data.iconSize = mNotificationIconSize;
        data.finisher = finisher;
        data.previewWidth = mPreviewWidth;
        data.previewheight = mPreviewHeight;
        if (mSaveInBgTask != null) {
            mSaveInBgTask.cancel(false);
        }
        if (data.image == null) {
            Log.d("saveScreenshotInWorkerThread", "The image is null before saving it!");
            return;
        }
        mSaveInBgTask = new SaveImageInBackgroundTask(mContext, data, mNotificationManager)
                .execute();
    }

    /**
     * @return the current display rotation in degrees
     */
    private float getDegreesForRotation(int value) {
        switch (value) {
        case Surface.ROTATION_90:
            return 360f - 90f;
        case Surface.ROTATION_180:
            return 360f - 180f;
        case Surface.ROTATION_270:
            return 360f - 270f;
        }
        return 0f;
    }

    /**
     * Starts the animation after taking the screenshot
     */
    private void startActionWindows(final Runnable finisher, int w, int h, boolean statusBarVisible,
            boolean navBarVisible) {
        // Add the view for the animation

        mScreenshotView.setImageBitmap(mScreenBitmap);
        mScreenshotView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intercept and ignore all touch events

            }
        });

        // Setup the animation with the screenshot just taken
        if (mScreenshotAnimation != null) {
            mScreenshotAnimation.end();
            mScreenshotAnimation.removeAllListeners();
        }
        mWindowManager.addView(mCoverView, mCoverParams);
        mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
        mScreenshotView.initialize(mScreenBitmap);
        mBackgroundView.setAlpha(0f);
        mBackgroundView.setVisibility(View.GONE);
        mScreenshotFlash.setAlpha(0f);
        mScreenshotFlash.setVisibility(View.GONE);
        mScreenshotView.setVisibility(View.VISIBLE);
        mScreenshotLayout.post(new Runnable() {
            @Override
            public void run() {
                // Play the shutter sound to notify that we've taken a screenshot
                mCameraSound.play(MediaActionSound.SHUTTER_CLICK);
                mScreenshotView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                mScreenshotView.buildLayer();
                //mScreenshotAnimation.start();
            }
        });

        mScreenshotView.setVisibility(View.VISIBLE);//1992
    }

    void toastShow(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }

    static void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();

        // Clear all existing notification, compose the new notification and show it
        Notification.Builder b = new Notification.Builder(context)
            .setTicker(r.getString(R.string.screenshot_failed_title))
            .setContentTitle(r.getString(R.string.screenshot_failed_title))
            .setContentText(r.getString(R.string.screenshot_failed_text))
            .setSmallIcon(R.drawable.stat_notify_image_error)
            .setWhen(System.currentTimeMillis())
            .setVisibility(Notification.VISIBILITY_PUBLIC) // ok to show outside lockscreen
            .setCategory(Notification.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setColor(context.getColor(
                        com.android.internal.R.color.system_notification_accent_color));
        Notification n =
            new Notification.BigTextStyle(b)
                .bigText(r.getString(R.string.screenshot_failed_text))
                .build();
        nManager.notify(R.id.notification_screenshot, n);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    final Runnable mQuit = new Runnable() {
        @Override
        public void run() {
            if (mFinisher != null) {
                mFinisher.run();
            }
            Log.d(TAG,"mScreenshotLayout.getParent() = "+mScreenshotLayout.getParent());
            if (mScreenshotLayout != null && mScreenshotLayout.getParent() != null) {
                mWindowManager.removeView(mScreenshotLayout);
            }
            if (mCoverView != null && mCoverView.getParent() != null) {
                mWindowManager.removeView(mCoverView);
            }
            mScreenshotView = null;
        }
    };
    
    public void quitSuperScreenshot() {
        mLongScreentShot = false;
        mEndScreenShot = true;
        mScreenshotLayout.setVisibility(View.GONE);
        mHandler.postDelayed(mQuit, 2000);
    }

    private BackListenerRelativeLayout.DispatchKeyEventListener mDispatchKeyEventListener = new BackListenerRelativeLayout.DispatchKeyEventListener() {

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                Log.d(TAG, "KEYCODE_BACK");
                //quitSuperScreenshot();
                return true;
            }
            return false;
        }
    };

    public void orientationChangedQuit() {
        if(mLongScreentShot) {
            quitSuperScreenshot();
            toastShow(R.string.bird_donot_support_long_screenshot);
        }
    }

    public String[] getTopActivityName(Context context) {
        String topActivityPackage = null;
        String topActivityClass = null;
        String[] activityInfo = new String[2];
        ActivityManager activityManager = (ActivityManager) (context
                .getSystemService(android.content.Context.ACTIVITY_SERVICE));
        List<RunningTaskInfo> runningTaskInfos = activityManager
                .getRunningTasks(1);
        if (runningTaskInfos != null) {
            ComponentName f = runningTaskInfos.get(0).topActivity;
            topActivityPackage = f.getPackageName();
            topActivityClass = f.getClassName();
            activityInfo[0] = topActivityPackage;
            activityInfo[1] = topActivityClass;
        }
        Log.d(TAG, "topActivityPackage:" + topActivityPackage+",topActivityClass = "+topActivityClass);
        return activityInfo;
    }

    private boolean isIgnoredActivity(String className) {
        if (className.equals("com.android.deskclock.alarms.AlarmActivity")
            || className.equals("com.android.soundrecorder.SoundRecorder")
            || className.equals("com.android.launcher3.Launcher")
            || className.equals("com.android.gallery3d.app.GalleryActivity")) {
            return true;
        }
        return false;
    }

    private boolean changeScrollDistance(String className) {
        if (className.equals("com.android.contacts.activities.PeopleActivity")
            || className.equals("com.android.dialer.DialtactsActivity")) {
            return true;
        }
        return false;
    }

    private int getCurrentSize() {
        Log.d(TAG,"getCurrentSize mScreenWidth : "+mScreenWidth);
        if (mScreenWidth <= 540) {
            return 1;
        } else {
            return 0;
        }
    }

    private boolean changeStartXpoint(String className) {
        if (className.equals("com.bird.wallpaper.HomeScreenWallpaperActivity")
            || className.equals("com.bird.wallpaper.LockscreenWallpaperActivity")) {
            return true;
        }
        return false;
    }
}

