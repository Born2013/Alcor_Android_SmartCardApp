package com.android.keyguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.android.keyguard.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Created by root on 17-8-4.
 */

public class BirdOldMusicAblum extends View {


    public void setmAblum(Bitmap mAblum) {
        this.mAblum = mAblum;
        postInvalidate();
    }

    Bitmap mAblum;
    private Paint mPaint;

    BitmapFactory.Options mOptions;
    private int mWidth;
    private int mHeight;


    public BirdOldMusicAblum(Context context) {
        this(context, null);
    }

    public BirdOldMusicAblum(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BirdOldMusicAblum(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BirdOldMusicAblum(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaint();

        if (mOptions == null) {
            mOptions = new BitmapFactory.Options();
            mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        mOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.drawable.oldphone_panel_mask, mOptions);
        mWidth = mOptions.outWidth;
        mHeight = mOptions.outHeight;
    }


    private void initPaint() {
        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setAntiAlias(true);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //fitsSize();

    }

    private void fitsSize() {
        /*mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mOptions.inJustDecodeBounds = false;
        
        if (mAblum == null) {
            mAblum = BitmapFactory.decodeResource(getResources(), R.drawable.oldphone_panel_default, mOptions);
        }
        int mAlblumW = mAblum.getHeight();
        int mAlblumH = mAblum.getHeight();
        if ((mAlblumW > mWidth) && (mAlblumH > mHeight)) {
                mAblum = ThumbnailUtils.extractThumbnail(mAblum, mWidth, mHeight);
        }*/
        
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mAblum !=null) {
            RoundDrawable drawable =  ZoomDrawable(mAblum,mWidth,mHeight);
            drawable.setBounds(new Rect(getLeft(), getTop(), getLeft() + mWidth, getTop() + mHeight));
            drawable.draw(canvas);
        } 
    }


    private RoundDrawable ZoomDrawable (Bitmap bitmap,int deswidth,int desHeight) {
        
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = deswidth;
        int newHeight = desHeight;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return new RoundDrawable(resizedBitmap);
    }

}
