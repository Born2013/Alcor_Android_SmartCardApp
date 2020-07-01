package com.sensortek.stkhealthcare2; 


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.text.TextUtils;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class progressView extends View {
	
    private final String TAG = "progressView"; 
    private final Paint mLinePaint = new Paint();
    private final Paint mTextPaint = new Paint();
    private final Paint mCirclePaint = new Paint();
    private final Paint mPaint = new Paint();
    private final int mPadding = 30;
    private int mStrokeWidth = 12;
    private final int mCircleWidth = 3;
    private int mCurrentDataTextSize = 16;
    private final int mScaleTextSize = 10;
    private final int mRadius = 5;
    private Context mContext;
    private FontMetrics mFontMetrics;
    private float mFontHeight;
    private int mCurrentValue;
    private float mOffset = 0.4f;
    
    @SuppressWarnings("unused")
    public progressView(Context context) {
        this(context, null);
    }

    public progressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mStrokeWidth = mContext.getResources().getDimensionPixelOffset(R.dimen.progress_strokesize);
        mCurrentDataTextSize = mContext.getResources().getDimensionPixelOffset(R.dimen.progress_current_data_strokesize);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(mStrokeWidth);   

        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Align.CENTER);
        
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(sp2px(mCircleWidth));  
    }


   
    @Override
    public void onDraw(Canvas canvas) {
        if (!PulseFragment.mIsCompleted) {
            return;
        }
        int width = getWidth();
        int height = getHeight(); 
        int actualWidth = width - sp2px(mPadding) * 2;
        int averageWidth = actualWidth / 4;
        float y = height / 2;
        float circleFloatX = 0;
        if (!TextUtils.isEmpty(String.valueOf(mCurrentValue))) {
            mTextPaint.setTextSize(sp2px(mCurrentDataTextSize));
            mFontMetrics = mTextPaint.getFontMetrics();
            mFontHeight = mFontMetrics.bottom - mFontMetrics.top;
            int surplus = (mCurrentValue - 20) % 40;
            int count = (mCurrentValue - 20) / 40 ;
            int num = count +  (surplus == 0 ? -1 : 0);
            
            mTextPaint.setColor(getCurrentColor(num));
            circleFloatX = sp2px(mPadding) + count * averageWidth + ((float)surplus / 40) * averageWidth;
            canvas.drawText(String.valueOf(mCurrentValue), circleFloatX, y - mStrokeWidth / 2 - mFontHeight / 2,
				mTextPaint);
            
            mCirclePaint.setColor(getCurrentColor(num));
            canvas.drawCircle(circleFloatX, y, sp2px(mRadius), mCirclePaint);
        }
        for (int i = 0;i < 4;i++) {   
            mLinePaint.setColor(getCurrentColor(i));
            mPaint.setColor(getCurrentColor(i));
            if (i == 0) {
                canvas.drawCircle(sp2px(mPadding), y, mStrokeWidth / 2, mPaint);
            } else if (i == 3) {
                canvas.drawCircle(sp2px(mPadding) + 4 * averageWidth, y, mStrokeWidth / 2, mPaint);
            }
            int startX = sp2px(mPadding) + i * averageWidth;
            int endX = startX + averageWidth;
            float circleStartX = circleFloatX - sp2px(mRadius) - sp2px(mCircleWidth) / 2;
            float circleEndX = circleFloatX + sp2px(mRadius) + sp2px(mCircleWidth) / 2;
            if (circleStartX > startX && circleStartX <= endX && circleEndX <= endX) {
                canvas.drawLine(startX, y, circleStartX + mOffset, y, mLinePaint);
                canvas.drawLine(circleEndX - mOffset, y, endX, y, mLinePaint);
            } else if (circleStartX > startX && circleStartX <= endX  && circleEndX > endX) {
                canvas.drawLine(startX, y, circleStartX + mOffset, y, mLinePaint);
            } else if (circleStartX <=  startX && circleEndX > startX && circleEndX <= endX) {
                canvas.drawLine(circleEndX - mOffset, y, endX, y, mLinePaint);
            } else {
                canvas.drawLine(startX, y, startX + averageWidth, y, mLinePaint);
            }
        }

        mTextPaint.setTextSize(sp2px(mScaleTextSize));
        mTextPaint.setColor(Color.parseColor("#000000"));
        mFontMetrics = mTextPaint.getFontMetrics();
        mFontHeight = mFontMetrics.bottom - mFontMetrics.top;
        canvas.drawText("60", sp2px(mPadding) + averageWidth, y + mStrokeWidth / 2 + mFontHeight,
				mTextPaint);
        canvas.drawText("100", sp2px(mPadding) + 2 * averageWidth, y + mStrokeWidth / 2 + mFontHeight,
				mTextPaint);
        canvas.drawText("140", sp2px(mPadding) + 3 * averageWidth, y + mStrokeWidth / 2 + mFontHeight,
				mTextPaint);

    }

	private int sp2px(int value) {
		float v = mContext.getResources().getDisplayMetrics().scaledDensity;
		return (int) (v * value + 0.5f);
	}

    private int getCurrentColor(int value) {
        if (value == 0) {
            return Color.parseColor("#22bded");
        } else if (value == 1) {
            return Color.parseColor("#80d214");
        } else if (value == 2) {
            return Color.parseColor("#ffbd32");
        } else if (value == 3) {
            return Color.parseColor("#fd6900");
        } else {
            return Color.parseColor("#000000");
        }
    }

    public void refreshCurrentValue(int value) {
        mCurrentValue = value;
        invalidate();
    }
}
