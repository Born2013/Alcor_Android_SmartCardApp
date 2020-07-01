package com.android.keyguard;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Created by root on 17-8-5.
 */

public class RoundDrawable extends Drawable {


    /**
     * 显示图片
     */
    private Bitmap mBitmap;
    /**
     * BitmapShader
     */
    private BitmapShader mBitmapShader;
    /**
     * 画笔
     */
    private Paint mPaint;

    private RectF rectF;


    public RoundDrawable(Bitmap bitmap) {
        this.mBitmap = bitmap;

        mBitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setShader(mBitmapShader);

    }


    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        rectF = new RectF(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(rectF, 4,4, mPaint);
    }


    @Override
    public int getIntrinsicHeight() {
        return mBitmap.getHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmap.getWidth();
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

   public Bitmap getBitmap(){
          return mBitmap;
   }

  public boolean isRecycled() {
       return mBitmap.isRecycled();
  }
  
  public void recycle() {
        mBitmap.recycle();
  }


}
