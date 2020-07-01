/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;

public class BirdScreenshotHelpActivity extends Activity {

    private final static String TAG = "BirdScreenshotHelpActivityTAG";

    private ImageView mAltImageView0;
    private ImageView mAltImageView1;
    private ImageView  mSlideImageView;
    private TextView mTipsTextView;
    private Animation mSlideOutAnimation;
    private Animation mSlideInAnimation;
    private Animation mShakeUpDownAnimation;

    private DisplayMetrics mDisplayMetrics;
    private int mNextPage = 1;
    private int[] mWhichGuide;
    private String mGuideTipsHeader;
    private float yDown = 2048.0f;
    private float yMove;

    final static int[] FULL_GUIDE_DRAWABLE = {
        R.drawable.bird_screenshot_guide_0, 
        R.drawable.bird_screenshot_guide_full1, 
        R.drawable.bird_screenshot_guide_full2, 
        R.drawable.bird_screenshot_guide_full3, 
    };

    final static int[] REGIONAL_GUIDE_DRAWABLE = {
        R.drawable.bird_screenshot_guide_0, 
        R.drawable.bird_screenshot_guide_regional1, 
        R.drawable.bird_screenshot_guide_regional2, 
        R.drawable.bird_screenshot_guide_regional3, 
    };

    final static int[] LONG_GUIDE_DRAWABLE = {
        R.drawable.bird_screenshot_guide_0, 
        R.drawable.bird_screenshot_guide_long1, 
        R.drawable.bird_screenshot_guide_long2, 
        R.drawable.bird_screenshot_guide_long3, 
        R.drawable.bird_screenshot_guide_long4, 
        R.drawable.bird_screenshot_guide_long5, 
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        int intExtra = intent.getIntExtra("bird_which_guide", 1);

        if (intExtra == 3) {
            mWhichGuide = LONG_GUIDE_DRAWABLE;
            mGuideTipsHeader = "bird_guide_long_tip";
        } else if (intExtra == 2) {
            mWhichGuide = REGIONAL_GUIDE_DRAWABLE;
            mGuideTipsHeader = "bird_guide_regional_tip";
        } else {
            mWhichGuide = FULL_GUIDE_DRAWABLE;
            mGuideTipsHeader = "bird_guide_full_tip";
        }

        setContentView(R.layout.bird_screenshot_help_activity);

        mDisplayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(mDisplayMetrics);

        //init pictures
        mAltImageView0 = (ImageView) findViewById(R.id.bird_screenshot_guider_alt0);
        mNextPage = 0;
        mAltImageView0.setImageBitmap(createReflectedImage());
        mAltImageView1 = (ImageView) findViewById(R.id.bird_screenshot_guider_alt1);

        //arrow
        mSlideImageView = (ImageView) findViewById(R.id.bird_screenshot_slidedown);
        mShakeUpDownAnimation = AnimationUtils.loadAnimation(this, R.anim.bird_up_and_down_anim);
        mSlideImageView.setAnimation(mShakeUpDownAnimation);

        //init text
        mTipsTextView = (TextView) findViewById(R.id.bird_screenshot_des);
        int textId = getResources().getIdentifier(mGuideTipsHeader + mNextPage, "string", "com.android.systemui");
        mTipsTextView.setText(textId);
        mNextPage = 1;

        mSlideOutAnimation = AnimationUtils.loadAnimation(this, R.anim.bird_guide_out_anim);
        mSlideInAnimation = AnimationUtils.loadAnimation(this, R.anim.bird_guide_in_anim);
        mSlideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //when it is Odd number, catching up will show mAltImageView1.such as 1, 3, 5, 7
                //when it is Even number, catching up will show mAltImageView0. such as 0, 2, 4, 6
                int textId = getResources().getIdentifier(mGuideTipsHeader + mNextPage, "string", "com.android.systemui");
                mTipsTextView.setText(textId);
                Log.i(TAG, "BIRD_SUPER_SCREENSHOT_SUPPORT, next will show is " + mNextPage);
                if (mNextPage % 2 == 1) {
                    mAltImageView1.setImageBitmap(createReflectedImage());
                    mAltImageView1.setVisibility(View.VISIBLE);
                    mAltImageView1.startAnimation(mSlideInAnimation);
                } else {
                    mAltImageView0.setImageBitmap(createReflectedImage());
                    mAltImageView0.setVisibility(View.VISIBLE);
                    mAltImageView0.startAnimation(mSlideInAnimation);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //set the last view gone 
                if (mNextPage % 2 == 1) {
                    mAltImageView0.setVisibility(View.GONE);
                } else {
                    mAltImageView1.setVisibility(View.GONE);
                }

                //finish anim, refresh nextpage need +1
                mNextPage += 1;

                //if (mWhichGuide != null && mNextPage == mWhichGuide.length) {
                //    mShakeUpDownAnimation.cancel();
                //}
            }
        });

        //底部渐变色
        ImageView shadowImageView = (ImageView) findViewById(R.id.bird_screenshot_shadow);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) shadowImageView.getLayoutParams();
        lp.height = (int) mDisplayMetrics.density * 115;
        shadowImageView.setLayoutParams(lp);
    }

    public Bitmap createReflectedImage() {
        //make sure mWhichGuide not null, and has suitable value.
        if (mWhichGuide == null || mNextPage >= mWhichGuide.length ) {
            return BitmapFactory.decodeResource(getResources(), R.drawable.bird_screenshot_guide_0);
        }
        Bitmap originalImage = BitmapFactory.decodeResource(getResources(), mWhichGuide[mNextPage]);

        final int reflectionGap = 0;

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, 0, width, height, matrix, false);
        Bitmap newBitmap = Bitmap.createBitmap(width, height + (int) mDisplayMetrics.density * 115, Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);

        //原始图
        canvas.drawBitmap(originalImage, 0, 0, null);
        //Paint defaultPaint = new Paint();
        //canvas.drawRect(0, height, width, height, defaultPaint);

        //倒影图
        canvas.drawBitmap(reflectionImage, 0, height, null);//起始x, y

        //绘制渐变
        Paint paint = new Paint();
        //起始x，渐变开始y，终点x，终点y
        LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0, newBitmap.getHeight(),
                0x30000000, 0x00ffffff,
                TileMode.MIRROR);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawRect(0, height, width, newBitmap.getHeight(), paint);

        return newBitmap;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i(TAG, "user touch Activity " + event.getY());
        if (event.getY() < 100) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                yDown = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                yMove = event.getY();
                if (yMove - yDown > 20 && mWhichGuide != null && mNextPage < mWhichGuide.length) {
                    if (mNextPage % 2 == 0) {
                        mAltImageView1.startAnimation(mSlideOutAnimation);
                    } else {
                        mAltImageView0.startAnimation(mSlideOutAnimation);
                    }
                } else if (yMove - yDown > 20 && mWhichGuide != null && mNextPage == mWhichGuide.length) {

                    finish();
                }
                break;
            case MotionEvent.ACTION_UP:
                yDown = 2048.0f;
                break;

            default:
                break;

        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
