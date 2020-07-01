package com.sensortek.stkhealthcare2; 

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.CornerPathEffect;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class lineChartView extends View{

    public static final String TAG = "lineChartView";
    private boolean isStarted = false;
    private int xScale=5; 
    private int yScale=1;
    private int mPadding = 50;
    private int mMaxDataSize;  
    private int mUpHeight = 24;
    private int mDownHeight = 18;
    private boolean mHasHeart = false;
    private int mStrokeWidth;
    private List<Integer> mData=new ArrayList<Integer>();
    private int num = 0;
    private int linenum = 0;
    private Context mContext;
    private Paint mPaint=new Paint();
    private Paint mCirclePaint = new Paint();

    public lineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }
    
    private void init() {
        mStrokeWidth = mContext.getResources().getDimensionPixelOffset(R.dimen.line_chart_strokesize);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth); 
        mPaint.setAntiAlias(true);  
        mPaint.setColor(Color.parseColor("#fd404c"));
        mPaint.setPathEffect(new CornerPathEffect(10));
 
        mCirclePaint.setStyle(Paint.Style.FILL); 
        mCirclePaint.setAntiAlias(true);  
        mCirclePaint.setColor(Color.parseColor("#fd404c"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float yPoint = getHeight()/2;
        Path path = new Path();
        if (isStarted) {
            preparData();  
        } 
        else if (mData != null){
            mData.clear();
            return;
        }
        if (mData == null || mData.isEmpty() || mData.size() <= 0) {
            return;
        }
        Log.d(TAG,"mData.size() = "+mData.size()+",mMaxDataSize = "+mMaxDataSize);
        path.moveTo(0, yPoint + mData.get(0)*yScale);
        for (int i = 1; i < mData.size(); i++) {
            path.lineTo(i*xScale, yPoint + mData.get(i)*yScale);
        }
        canvas.drawPath(path, mPaint);
        canvas.drawCircle((mData.size() - 1) * xScale, yPoint + mData.get((mData.size() - 1))*yScale, mStrokeWidth / 2, mCirclePaint);
        invalidate();

    }

    private int sp2px(int value) {
        float v = mContext.getResources().getDisplayMetrics().scaledDensity;
        return (int) (v * value + 0.5f);
    }

    public void startAnimation() {
        if (isStarted) {
            return;
        }
        isStarted = true;
        invalidate();
    }

    public void stopAnimation() {
        isStarted = false;
        linenum = 0;
        mHasHeart = false;
        mData.clear();
    }
    
	public void preparData() {
            int height = getHeight();
            int width = getWidth();
            int actualWidth = width - sp2px(mPadding);
            mMaxDataSize = actualWidth/xScale;
            if (mHasHeart) {
              linenum ++;
              if (linenum == 1 || linenum == 3) {
                  mData.add(-sp2px(mUpHeight)/2);  
              } else if (linenum == 2) {
                  mData.add(-sp2px(mUpHeight));  
              } else if (linenum == 4) {
                  mData.add(0);  
              } else if (linenum == 5 || (linenum == 7)) {
                  mData.add(sp2px(mDownHeight)/2); 
              } else if (linenum == 6) {
                  mData.add(sp2px(mDownHeight));
              } else if ((linenum == 8)) {
                  linenum = 0;
                  mHasHeart = false;
                  mData.add(0);
              }
            } else {
                mData.add(0);  
            }  
            if(mData != null && !mData.isEmpty() && mData.size() > mMaxDataSize){
                mData.remove(0); 
            }
		
	}

    public void hasHeart() {
        mHasHeart = true;
    }
}
