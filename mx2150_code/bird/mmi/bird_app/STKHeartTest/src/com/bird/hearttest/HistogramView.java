package com.sensortek.stkhealthcare2;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.SweepGradient;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import java.util.List;
import java.util.ArrayList;
import com.sensortek.stkhealthcare2.provider.Heart;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Class to draw a circle for timers and stopwatches.
 * These two usages require two different animation modes:
 * Timer counts down. In this mode the animation is counter-clockwise and stops at 0.
 * Stopwatch counts up. In this mode the animation is clockwise and will run until stopped.
 */
public class HistogramView extends View {
	
    private final String TAG = "HistogramView"; 
    private final Paint mDottedPaint = new Paint();
    private final Paint mBottomPaint = new Paint();
    private final Paint mTextPaint = new Paint();
    private final Paint mRectPaint = new Paint();
    private Path mPath = new Path();
    private int mPaddingLeft = 25;
    private int mPaddingTop = 20;
    private int mPaddingBottom = 15;
    private int mBigColumnWidth = 27;
    private int mSmallColumnWidth = 15;
    private int mLeftTextPaddingLeft;
    private int mLeftStrokeSize;
    private int mBottomStrokeSize;
    private int mTopStrokeSize;
    private String[] mReferenceValue = null;
    private int mMaxValue;
    private Context mContext;
    private float mFontHeight;
    private FontMetrics mFontMetrics;
    private int mDottedCount;
    private List<Integer> mHeartValue = new ArrayList<Integer>();
    private List<String> mDate = new ArrayList<String>();
    
    @SuppressWarnings("unused")
    public HistogramView(Context context) {
        this(context, null);
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        mBigColumnWidth = mContext.getResources().getDimensionPixelOffset(R.dimen.big_column_width);
        mSmallColumnWidth = mContext.getResources().getDimensionPixelOffset(R.dimen.small_column_width);
        mPaddingLeft = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_view_padding_left);
        mPaddingTop = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_view_padding_top);
        mLeftStrokeSize = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_left_text_sixe);
        mBottomStrokeSize = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_bottome_text_sixe);
        mTopStrokeSize = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_top_text_sixe);
        mLeftTextPaddingLeft = mContext.getResources().getDimensionPixelOffset(R.dimen.histogram_left_text_padding_left);
        mDottedPaint.setAntiAlias(true);
        mDottedPaint.setStyle(Paint.Style.STROKE);
        mDottedPaint.setStrokeWidth(1);
        mDottedPaint.setColor(Color.parseColor("#33000000"));
        mDottedPaint.setPathEffect(new DashPathEffect(new float[]{8,8}, 0));

        mBottomPaint.setAntiAlias(true);
        mBottomPaint.setStyle(Paint.Style.STROKE);
        mBottomPaint.setStrokeWidth(1);
        mBottomPaint.setColor(Color.parseColor("#33000000"));

        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.parseColor("#99000000"));

        mRectPaint.setAntiAlias(true);
        mRectPaint.setStyle(Paint.Style.FILL);
        
    }

    public void refreshData(List<Heart> result) {
        mDate.clear();
        mHeartValue.clear();
        if (result != null && !result.isEmpty() && result.size() > 0) {
            for (int i = result.size() - 1;i >= 0;i--) {
                Heart heart = result.get(i);
                mDate.add(getDate(heart.dateAndTime));
                mHeartValue.add(heart.bpm);
                Log.d(TAG,"i = "+i+",heart.bpm = "+heart.bpm+",heart.dateAndTime = "+heart.dateAndTime);
            }
            
            String[] value = getResources().getStringArray(R.array.reference_value);
            mMaxValue = getMax(mHeartValue);
            int mm = (mMaxValue - 20) % 40 == 0 ? 0 : 1;
            mDottedCount = (mMaxValue - 20)/40 + mm;
            Log.d(TAG,"mMaxValue = "+mMaxValue+",(mMaxValue - 20)/40 = "+(mMaxValue - 20)/40);
            Log.d(TAG,"mm = "+mm);
            mReferenceValue = new String[mDottedCount + 1];
            for (int i = 0;i < mReferenceValue.length;i++) {
                mReferenceValue[i] = value[i];
            }
        }
        invalidate();
    }

    public String getDate(String currentTime) {  
        String[] now = currentTime.split("-"); 
        try {
            if (IsThisYear(currentTime)) {
                if (now.length >= 2) {
                    return now[1];
                }
            } else {
                String year = "";
                if (!now[0].isEmpty() && now[0].length() >= 2) {
                    year = now[0].substring(now[0].length() - 2, now[0].length());
                } else {
                    year = "";
                }
                return now[1];
            }
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }  

    public SimpleDateFormat getDateFormat() {  
        return new SimpleDateFormat("yyyy-MMdd-HH:mm");  
    }  
    
    public boolean IsThisYear(String day) throws ParseException {  
        Calendar pre = Calendar.getInstance();  
        Date predate = new Date(System.currentTimeMillis());  
        pre.setTime(predate);  
  
        Calendar cal = Calendar.getInstance();  
        Date date = getDateFormat().parse(day);  
        cal.setTime(date);  
  
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {   
            return true;  
        }  
        return false;  
    }  
    
    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        if (mDate == null || mDate.isEmpty() || mHeartValue == null || mHeartValue.isEmpty()) {
            return;
        }
        int actualHeght = height - sp2px(mPaddingTop) - sp2px(mPaddingBottom);
        Log.d(TAG,"width = "+width+",height = "+height);
        
        int singleHeight = actualHeght/mDottedCount;
        Log.d(TAG,"actualHeght = "+actualHeght+",mDottedCount = "+mDottedCount+",singleHeight = "+singleHeight);

        mTextPaint.setTextSize(sp2px(mLeftStrokeSize));
        mTextPaint.setTextAlign(Align.RIGHT);
        mFontMetrics = mTextPaint.getFontMetrics();
        mFontHeight = mFontMetrics.bottom - mFontMetrics.top;
        for (int i = 0;i <= mDottedCount ;i++) {
            Log.d(TAG,"i = "+i);
            Log.d(TAG,"height = "+(height - sp2px(mPaddingBottom) - i * singleHeight));
            canvas.drawText(mReferenceValue[i], sp2px(mLeftTextPaddingLeft), height - sp2px(mPaddingBottom) - i * singleHeight + mFontHeight / 2 - mFontMetrics.bottom,
				mTextPaint);
            mPath.reset();
            mPath.moveTo(sp2px(mPaddingLeft), height - sp2px(mPaddingBottom) - i * singleHeight);
            mPath.lineTo(width, height - sp2px(mPaddingBottom) - i * singleHeight);
            if (i == 0) {
                canvas.drawPath(mPath, mBottomPaint);
            } else {
                canvas.drawPath(mPath, mDottedPaint);
            }
        }

        for (int i = 0;i < mHeartValue.size() ;i++) {
            mTextPaint.setTextAlign(Align.CENTER);
            mTextPaint.setTextSize(sp2px(mBottomStrokeSize));
            mFontMetrics = mTextPaint.getFontMetrics();
            mFontHeight = mFontMetrics.bottom - mFontMetrics.top;
            canvas.drawText(mDate.get(i), 
                            sp2px(mPaddingLeft) + i * sp2px(mBigColumnWidth) + sp2px(mBigColumnWidth)/2,
                            height - sp2px(mPaddingBottom) / 2 + mFontHeight / 2,
                            mTextPaint);

            if (mHeartValue.get(i) > 20 && mHeartValue.get(i) <= 60) {
                mRectPaint.setColor(Color.parseColor("#22bded"));
            } else if (mHeartValue.get(i) > 60 && mHeartValue.get(i) <= 100) {
                mRectPaint.setColor(Color.parseColor("#80d214"));
            } else if (mHeartValue.get(i) > 100 && mHeartValue.get(i) <= 140) {
                mRectPaint.setColor(Color.parseColor("#ffbd32"));
            } else if (mHeartValue.get(i) > 140) {
                mRectPaint.setColor(Color.parseColor("#fd6900"));
            }
            
            float left = sp2px(mPaddingLeft)+ i * sp2px(mBigColumnWidth) + sp2px(mBigColumnWidth - mSmallColumnWidth)/2;
            float top = height - sp2px(mPaddingBottom) - ((mHeartValue.get(i) - 20) / 40) * singleHeight - ((float)((mHeartValue.get(i) - 20) % 40) / (float)40) * singleHeight;
            float right = left + sp2px(mSmallColumnWidth);
            float bottom = height - sp2px(mPaddingBottom);
            canvas.drawRect(left , top, right, bottom, mRectPaint);

            mTextPaint.setTextSize(sp2px(mTopStrokeSize));
            mFontMetrics = mTextPaint.getFontMetrics();
            mFontHeight = mFontMetrics.bottom - mFontMetrics.top;
            canvas.drawText(String.valueOf(mHeartValue.get(i)), 
                sp2px(mPaddingLeft) + i * sp2px(mBigColumnWidth) + sp2px(mBigColumnWidth)/2,
                top - mFontHeight / 2,
                mTextPaint);
        }
        
   }

	private int sp2px(int value) {
		float v = 2.24f;//mContext.getResources().getDisplayMetrics().scaledDensity;
       Log.d(TAG,"v = "+v);
		return (int) (v * value + 0.5f);
	}
    private int getMax(List<Integer> value){
        int max = value.get(0);
        for (int i=1; i < value.size(); i++) {
            if(value.get(i) > max) {
                max = value.get(i);
            }
        }
        return max;
    }

    public int getAverage() {
        if (mHeartValue != null && !mHeartValue.isEmpty() && mHeartValue.size() > 0) {
            int average = mHeartValue.get(0);
            for (int i=1; i < mHeartValue.size(); i++) {
                average += mHeartValue.get(i);
            }
            return (int)(average / mHeartValue.size());
        } else {
            return 0;
        }
    }
}
