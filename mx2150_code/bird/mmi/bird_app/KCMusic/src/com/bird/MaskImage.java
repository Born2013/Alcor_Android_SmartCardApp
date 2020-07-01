package com.bird;

import android.widget.ImageView;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
/*[bug-null][gaowei][20170704]add*/
public class MaskImage extends ImageView{
        private static final String TAG = "MaskImage";
        RuntimeException mException;
        public MaskImage(Context context) {
            super(context);
            
        }

        public MaskImage(Context context, AttributeSet attrs) {
                super(context, attrs);
        }

        public void setMaskBg(Bitmap original, int maskResId) {
            if (mException != null) 
                    throw mException;
            
            //获取遮罩层图片
            Bitmap mask = BitmapFactory.decodeResource(getResources(), maskResId);
            int width = original.getWidth();
            int height = original.getHeight();
            int newWidth = mask.getWidth();
            int newHight = mask.getHeight();
            Matrix m = new Matrix();
            m.postScale((float)newWidth/width, (float)newHight/height);
            // Bitmap mask = Bitmap.createBitmap(maskRes, 0, 0, width, height, m, false);
            original = Bitmap.createBitmap(original, 0, 0, width, height, m, false);
            Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Config.ARGB_8888);
            
            //将遮罩层的图片放到画布中
            Canvas mCanvas = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            //设置两张图片相交时的模式 
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            mCanvas.drawBitmap(original, 0, 0, null);
            mCanvas.drawBitmap(mask, 0, 0, paint);
            paint.setXfermode(null);
            setImageBitmap(result);
            setScaleType(ScaleType.FIT_XY); 
        }
}