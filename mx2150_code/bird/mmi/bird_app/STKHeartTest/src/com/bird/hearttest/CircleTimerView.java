package com.sensortek.stkhealthcare2;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.SweepGradient;
import android.graphics.Matrix;
import android.graphics.CornerPathEffect;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class CircleTimerView extends View {
	
	private final String TAG = "CircleTimerView"; 
    private int mAboveCircleBeginColor;
    private int mAboveCircleEndColor;
    private int mBackGroundColor;
    private float mCurrentColor;
    private static final int[]colors = {0xffffbd32,0xfffd404c,0xffffbd32};
    private long mIntervalTime = 0;
    private long mIntervalStartTime = -1;
    private long mMarkerTime = -1;
    private long mCurrentIntervalTime = 0;
    private long mAccumulatedTime = 0;
    private boolean mPaused = false;
    private boolean mAnimate = false;
    private static float mStrokeSize;
    private static float mDotRadius;
    private final Paint mPaint = new Paint();
    private final Paint mFill = new Paint();
    private final Paint mCirclePaint = new Paint();
    private final Paint mInsidePaint = new Paint();
    private final RectF mArcRect = new RectF();
    private final RectF mInsideArcRect = new RectF();
    private float mScreenDensity;
	private long mTickTimeNow;
	public float redPercent;
    private int mWidth, mHeight;
    private float xRadius = 0;
    @SuppressWarnings("unused")
    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void setIntervalTime(long t) {
        mIntervalTime = t;
        postInvalidate();
    }

    public void setMarkerTime(long t) {
        mMarkerTime = t;
        postInvalidate();
    }

    public void reset() {
        mIntervalStartTime = -1;
        mMarkerTime = -1;
        postInvalidate();
        mTickTimeNow = 0;
    }
    public void startIntervalAnimation() {
        mIntervalStartTime = getTimeNow();
        mAnimate = true;
        invalidate();
        mPaused = false;
        Log.d(TAG,"startIntervalAnimation mIntervalStartTime = "+mIntervalStartTime);
    }
    
    public void stopIntervalAnimation() {
        mAnimate = false;
        mIntervalStartTime = -1;
        mAccumulatedTime = 0;
        invalidate();
    }

    public boolean isAnimating() {
        return (mIntervalStartTime != -1);
    }

    public void pauseIntervalAnimation() {
        mAnimate = false;
        mAccumulatedTime += getTimeNow() - mIntervalStartTime;
        mPaused = true;
    }

    public void abortIntervalAnimation() {
        mAnimate = false;
    }

    public void setPassedTime(long time, boolean drawRed) {
        // The onDraw() method checks if mIntervalStartTime has been set before drawing any red.
        // Without drawRed, mIntervalStartTime should not be set here at all, and would remain at -1
        // when the state is reconfigured after exiting and re-entering the application.
        // If the timer is currently running, this drawRed will not be set, and will have no effect
        // because mIntervalStartTime will be set when the thread next runs.
        // When the timer is not running, mIntervalStartTime will not be set upon loading the state,
        // and no red will be drawn, so drawRed is used to force onDraw() to draw the red portion,
        // despite the timer not running.
        mCurrentIntervalTime = mAccumulatedTime = time;
        if (drawRed) {
            mIntervalStartTime = getTimeNow();
        }
        postInvalidate();
    }



    private void init(Context c) {
        Resources resources = c.getResources();
        mStrokeSize = resources.getDimensionPixelOffset(R.dimen.circle_paint_strokesize);
        float dotDiameter = resources.getDimensionPixelOffset(R.dimen.circle_paint_strokesize);;
        initPaint();
        mBackGroundColor = Color.parseColor("#fae7e7");
        mAboveCircleBeginColor = Color.parseColor("#ffbd32");
        Log.d(TAG,"mAboveCircleBeginColor = "+mAboveCircleBeginColor);
        mScreenDensity = resources.getDisplayMetrics().density;
        mFill.setAntiAlias(true);
        mFill.setStyle(Paint.Style.FILL);
        mFill.setColor(mAboveCircleBeginColor);
        mDotRadius = dotDiameter / 2f;
    }

    private void initPaint() {
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(mStrokeSize);
        
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeSize);

        mInsidePaint.setAntiAlias(true);
        mInsidePaint.setStyle(Paint.Style.FILL);
        mInsidePaint.setColor(Color.parseColor("#ffffff"));
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        int xCenter = getWidth() / 2 + 1;
        int yCenter = getHeight() / 2;
        float radius = Math.min(xCenter, yCenter) - mStrokeSize;
        xRadius = radius > xRadius ? radius : xRadius;
        mArcRect.top = yCenter - radius;
        mArcRect.bottom = yCenter + radius;
        mArcRect.left =  xCenter - xRadius;
        mArcRect.right = xCenter + xRadius;
        
        mInsideArcRect.top = yCenter - radius;
        mInsideArcRect.bottom = yCenter + radius;
        mInsideArcRect.left =  xCenter - xRadius;
        mInsideArcRect.right = xCenter + xRadius;
        Matrix matrix = new Matrix();
        matrix.setRotate(-90, xCenter, yCenter);
        SweepGradient sweepGradient = new SweepGradient(xCenter, yCenter, colors, null);
        sweepGradient.setLocalMatrix(matrix);
        mCirclePaint.setShader(sweepGradient);
        canvas.drawArc(mArcRect, -90, 360, true, mInsidePaint);
        if (mIntervalStartTime == -1 && !PulseFragment.mIsCompleted) {
            mPaint.setColor(mBackGroundColor);
            //canvas.drawCircle (xCenter, yCenter, radius, mPaint);
            canvas.drawArc(mArcRect, -90, 360, false, mPaint);
        } else {
            if (mAnimate) {
                mCurrentIntervalTime = getTimeNow() - mIntervalStartTime + mAccumulatedTime;
            }
            redPercent = (float)mCurrentIntervalTime / (float)mIntervalTime;   
            redPercent = redPercent > 1 ? 1 : redPercent;
            float whitePercent = 1 - (redPercent > 1 ? 1 : redPercent);
            canvas.drawArc (mArcRect, 270, + redPercent * 360 , false, mCirclePaint);

            // draw white arc here
            mPaint.setColor(mBackGroundColor);
            canvas.drawArc(mArcRect, 270 + (1 - whitePercent) * 360,
                    whitePercent * 360, false, mPaint);

            drawRedDot(canvas, 0, xCenter, yCenter, radius);
            drawRedDot(canvas, redPercent, xCenter, yCenter, radius);
        }
        if (mAnimate) {
            invalidate();
        }
   }

    protected void drawRedDot(
            Canvas canvas, float degrees, int xCenter, int yCenter, float radius) {
        int color = getCurrentColor(degrees,colors);
        mFill.setColor(color);
        float dotPercent;
        dotPercent = 270 + degrees * 360;
        final double dotRadians = Math.toRadians(dotPercent);
        canvas.drawCircle(xCenter + (float) (radius * Math.cos(dotRadians)),
                yCenter + (float) (radius * Math.sin(dotRadians)), mDotRadius, mFill);
    }


    public void readFromSharedPref() {
        mPaused = false;
        mIntervalTime = 0;
        mIntervalStartTime = -1;
        mCurrentIntervalTime = 0;
        mAccumulatedTime = 0;
        mMarkerTime = -1;
        mAnimate = (mIntervalStartTime != -1 && !mPaused);
    }
    
    // unit: ms
    public long getTimeNow() {
        //return SystemClock.elapsedRealtime();
    	return mTickTimeNow;
    }
 
    public void setTimeNow(long t) {
    	mTickTimeNow = t;
    }
    
    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by the any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(
            float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }  

    private int getCurrentColor(float percent, int[] colors) {
        float[][] f = new float[colors.length][3];
        for (int i = 0; i < colors.length; i++) {
            f[i][0] = (colors[i] & 0xff0000) >> 16;
            f[i][1] = (colors[i] & 0x00ff00) >> 8;
            f[i][2] = (colors[i] & 0x0000ff);
        }
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < f.length; j++) {
                if (f.length == 1 || percent == j / (f.length - 1f)) {
                    result = f[j];
                } else {
                    if (percent > j / (f.length - 1f) && percent < (j + 1f) / (f.length - 1)) {
                        result[i] = f[j][i] - (f[j][i] - f[j + 1][i]) * (percent - j / (f.length - 1f)) * (f.length - 1f);
                    }
                }
            }
        }
        return Color.rgb((int) result[0], (int) result[1], (int) result[2]);
    }
}
