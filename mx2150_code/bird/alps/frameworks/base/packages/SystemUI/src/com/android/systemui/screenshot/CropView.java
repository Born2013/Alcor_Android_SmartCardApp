/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;

public class CropView extends ImageView {
    private static final String TAG = "BirdCropView";

    private RectF mImageBounds = new RectF();//原图部分
    private RectF mScreenBounds = new RectF();
    private RectF mScreenImageBounds = new RectF();
    private RectF mScreenCropBounds = new RectF();//裁剪部分
    private Rect mShadowBounds = new Rect();

    private Bitmap mBitmap;
    private Paint mPaint = new Paint();

    private NinePatchDrawable mShadow;
    //private CropObject mCropObj = null;
    private Drawable mCropIndicator;
    private int mIndicatorSize;
    private int mRotation = 0;
    private boolean mMovingBlock = false;
    private Matrix mDisplayMatrix = null;
    private Matrix mDisplayMatrixInverse = null;
    private boolean mDirty = false;

    private float mPrevX = 0;
    private float mPrevY = 0;
    private float mSpotX = 0;
    private float mSpotY = 0;
    private boolean mDoSpot = false;

    private int mShadowMargin = 15;
    private int mMargin = 32;
    private int mOverlayShadowColor = 0xCF000000;
    private int mOverlayWPShadowColor = 0x5F000000;
    private int mWPMarkerColor = 0x7FFFFFFF;
    private int mMinSideSize = 90;
    private int mTouchTolerance = 40;
    private float mDashOnLength = 20;
    private float mDashOffLength = 10;
    Paint mWhiteStrongPaint = new Paint();

    private static int mMinTwoSideDistance = 80;
    boolean mIsIndivatorsAllGone = false;
    boolean mIsRegionalAction = true;
    boolean mIsLongHeader = true;
    int mEdgeSelected = CropObject.MOVE_NONE;
    int mBlockTolerance = 80;

/// M: [BUG.MODIFY] @{
/*    private enum Mode {
        NONE, MOVE
    }*/
    private enum Mode {
        NONE, MOVE, MODIFY_TOUCH_POINTER
    }
/// @}

    private Mode mState = Mode.NONE;


    public CropView(Context context) {
        super(context);
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        //mShadow = (NinePatchDrawable) rsc.getDrawable(R.drawable.geometry_shadow);
        mCropIndicator = rsc.getDrawable(R.drawable.zzzzz_camera_crop);
        mIndicatorSize = (int) rsc.getDimension(R.dimen.crop_indicator_size);
        mShadowMargin = (int) rsc.getDimension(R.dimen.shadow_margin);
        mMargin = (int) rsc.getDimension(R.dimen.preview_margin);
        mMinSideSize = (int) rsc.getDimension(R.dimen.crop_min_side);
        mOverlayShadowColor = (int) rsc.getColor(R.color.crop_shadow_color);
        mOverlayWPShadowColor = (int) rsc.getColor(R.color.crop_shadow_wp_color);
        mWPMarkerColor = (int) rsc.getColor(R.color.crop_wp_markers);
        mDashOnLength = rsc.getDimension(R.dimen.wp_selector_dash_length);
        mDashOffLength = rsc.getDimension(R.dimen.wp_selector_off_length);

        mWhiteStrongPaint.setStyle(Paint.Style.STROKE);
        mWhiteStrongPaint.setColor(Color.WHITE);
        mWhiteStrongPaint.setStrokeWidth(10);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float[] point = getPoint(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case (MotionEvent.ACTION_DOWN):
                if (mState == Mode.NONE) {
                    if (selectEdgeBlock(x, y) != 0){
                        Log.i(TAG, "select Edge now !" + x + ", " + y);
                    }
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.MOVE;
                }
                break;
            case (MotionEvent.ACTION_UP):
                if (mState == Mode.MOVE) {
                    //mCropObj.selectEdgeBlock(CropObject.MOVE_NONE);
                    mMovingBlock = false;
                    mPrevX = x;
                    mPrevY = y;
                    mState = Mode.NONE;
                }
                break;
            case (MotionEvent.ACTION_MOVE):
                if (mState == Mode.MOVE) {
                    float dx = x - mPrevX;
                    float dy = y - mPrevY;
                    mPrevX = x;
                    mPrevY = y;
                    moveSelection(dx, dy, mMovingBlock);
                }
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    //作用1：设置是否长截图
    //作用2：如果是区域截图则要求两边大于等于80；如果长截图，因为上下有部分合并，所以距离得超过合并距离
    void setIsRegionalAction (boolean isRegionalAction, int minDistance) {
        mIsRegionalAction = isRegionalAction;
        mMinTwoSideDistance = minDistance;
    }

    //判断是否是长截图开始一张图
    void setIsLongHeader (boolean isLongHeader) {
        mIsLongHeader = isLongHeader;
    }

    //如果是长截图，
    void setIndivatorsAllGone(boolean isIndivatorsAllGone) {
        mIsIndivatorsAllGone = isIndivatorsAllGone;
        invalidate();
    }

    //获取截取的高度
    public RectF getScreenCropBounds(){
        return mScreenCropBounds;
    }

    void moveSelection(float dx, float dy, boolean isMovingBlock){
        if (mIsIndivatorsAllGone) {
            return;
        }
        if (!isMovingBlock) {
            if ((mEdgeSelected & CropObject.MOVE_TOP) != 0 && (mIsRegionalAction || mIsLongHeader)) {
                if (mScreenCropBounds.top + dy + mMinTwoSideDistance > mScreenCropBounds.bottom) {
                    //必须离截取框下边mMinTwoSideDistance
                    mScreenCropBounds.top = mScreenCropBounds.bottom - mMinTwoSideDistance;
                } else if (mScreenCropBounds.top + dy <= 0) {
                    //必须不能低于原始图
                    mScreenCropBounds.top= mImageBounds.left;
                } else {
                    mScreenCropBounds.top = mScreenCropBounds.top + dy;
                }
                return;
            } else if ((mEdgeSelected & CropObject.MOVE_BOTTOM) != 0 && (mIsRegionalAction || !mIsLongHeader)) {
                if (mScreenCropBounds.bottom + dy  < mScreenCropBounds.top + mMinTwoSideDistance) {
                    //必须离截取框上边mMinTwoSideDistance
                    mScreenCropBounds.bottom = mScreenCropBounds.top + mMinTwoSideDistance;
                } else if (mScreenCropBounds.bottom + dy >= mImageBounds.bottom) {
                    //必须不能超过原始图
                    mScreenCropBounds.bottom= mImageBounds.bottom;
                } else {
                    mScreenCropBounds.bottom = mScreenCropBounds.bottom + dy;
                }
                mOnScreenChangeListener.onScreenChange();
            }else if ((mEdgeSelected & CropObject.MOVE_LEFT) != 0 && mIsRegionalAction) {
                if (mScreenCropBounds.left + dx + mMinTwoSideDistance > mScreenCropBounds.right) {
                    //必须离截取框右边mMinTwoSideDistance
                    mScreenCropBounds.left = mScreenCropBounds.right - mMinTwoSideDistance;
                } else if (mScreenCropBounds.left + dx <= 0) {
                    //必须不能低于原始图
                    mScreenCropBounds.left = mImageBounds.left;
                } else {
                    mScreenCropBounds.left = mScreenCropBounds.left + dx;
                }
            } else if ((mEdgeSelected & CropObject.MOVE_RIGHT) != 0 && mIsRegionalAction) {
                if (mScreenCropBounds.right + dx < mScreenCropBounds.left + mMinTwoSideDistance ) {
                    //必须离截取框左边mMinTwoSideDistance
                    mScreenCropBounds.right = mScreenCropBounds.left + mMinTwoSideDistance;
                } else if (mScreenCropBounds.right + dx >= mImageBounds.right) {
                    //必须不能超过原始图
                    mScreenCropBounds.right= mImageBounds.right;
                } else {
                    mScreenCropBounds.right = mScreenCropBounds.right + dx;
                }
            }

        } else if (mIsRegionalAction && isMovingBlock) {
            if (mScreenCropBounds.top + dy >= mImageBounds.top
                    && mScreenCropBounds.bottom + dy <= mImageBounds.bottom
                    && mScreenCropBounds.left + dx >= mImageBounds.left
                    && mScreenCropBounds.right + dx <= mImageBounds.right) {
                mScreenCropBounds.top = mScreenCropBounds.top + dy;
                mScreenCropBounds.bottom = mScreenCropBounds.bottom + dy;
                mScreenCropBounds.left = mScreenCropBounds.left + dx;
                mScreenCropBounds.right = mScreenCropBounds.right + dx;
            }
        }
    }

    int selectEdgeBlock(float x, float y){
        RectF cropped = mScreenCropBounds;

        float left = Math.abs(x - cropped.left);
        float right = Math.abs(x - cropped.right);
        float top = Math.abs(y - cropped.top);
        float bottom = Math.abs(y - cropped.bottom);

        mEdgeSelected = CropObject.MOVE_NONE;
        // Check left or right.
        if ((left <= mTouchTolerance) && ((y + mTouchTolerance) >= cropped.top)
                && ((y - mTouchTolerance) <= cropped.bottom) && (left < right)) {
            mEdgeSelected |= CropObject.MOVE_LEFT;
        }
        else if ((right <= mTouchTolerance) && ((y + mTouchTolerance) >= cropped.top)
                && ((y - mTouchTolerance) <= cropped.bottom)) {
            mEdgeSelected |= CropObject.MOVE_RIGHT;
        }

        Log.i(TAG, "selectEdgeBlock, left = " +left + ", right = " + right);
        // Check top or bottom.
        if ((top <= mTouchTolerance) && ((x + mTouchTolerance) >= cropped.left)
                && ((x - mTouchTolerance) <= cropped.right) && (top < bottom)) {
            mEdgeSelected |= CropObject.MOVE_TOP;
        }
        else if ((bottom <= mTouchTolerance) && ((x + mTouchTolerance) >= cropped.left)
                && ((x - mTouchTolerance) <= cropped.right)) {
            mEdgeSelected |= CropObject.MOVE_BOTTOM;
        }
        Log.i(TAG, "selectEdgeBlock, top= " +top + ", bottom = " + bottom);

        if (mEdgeSelected == 0) {
            if (left >= mBlockTolerance
                    && right >= mBlockTolerance
                    && top >= mBlockTolerance
                    && bottom >= mBlockTolerance) {
                mMovingBlock = true;
            }
        }
        return mEdgeSelected;
    }

    public void initialize(Bitmap image) {
        mBitmap = image;
        mScreenCropBounds = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mBitmap == null) {
            super.onDraw(canvas);
            mScreenCropBounds = new RectF(0 , 0, canvas.getWidth(), canvas.getHeight());
            return;
        } 

        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        if (mDisplayMatrix == null ) {

            mDisplayMatrix = new Matrix();
            mDisplayMatrix.reset();
        }

        mImageBounds = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

        Paint sbp = new Paint();
        sbp.setColor(mOverlayShadowColor);
        sbp.setStyle(Paint.Style.FILL);

        canvas.drawBitmap(mBitmap, mDisplayMatrix, mPaint);//绘制原图

        CropDrawingUtils.drawShadows(canvas, sbp, mScreenCropBounds, mImageBounds);//绘制去掉灰色部分

        CropDrawingUtils.drawOuterLineBounds(canvas, mImageBounds);//绘制初始框

        CropDrawingUtils.drawInnerLineBounds(canvas, mScreenCropBounds);//绘制内部线

        // 绘制四个角
        // CropDrawingUtils.drawIndicators(canvas, mCropIndicator, mIndicatorSize,
        //    mScreenCropBounds, true, 2, mWhiteStrongPaint, true);

        //绘制四边小圆点
        if (!mIsIndivatorsAllGone) {
            CropDrawingUtils.drawIndicators(canvas, mCropIndicator, mIndicatorSize,
                mScreenCropBounds, false, 2, mWhiteStrongPaint, mIsRegionalAction, mIsLongHeader);
        }
    }

    private float[] getPoint(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            float[] touchPoint = {
                    event.getX(), event.getY()
            };
            return touchPoint;
        } else {
            float[] touchPoint = {
                    (event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2
            };
            return touchPoint;
        }
    }

    //设置监听，用于编辑长截图最后一页时，不再可以继续滑动
    public interface OnScreenChangeListener {
        void onScreenChange();
    }

    private OnScreenChangeListener mOnScreenChangeListener;

    public void setOnScreenChangeListener(OnScreenChangeListener onScreenChangeListener) {
        mOnScreenChangeListener = onScreenChangeListener;
    }
}
