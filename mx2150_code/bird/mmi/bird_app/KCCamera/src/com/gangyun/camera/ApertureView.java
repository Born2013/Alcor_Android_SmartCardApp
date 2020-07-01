package com.gangyun.camera;

import android.R.integer;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Xfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.camera.R;

/**
 * 上下滑动可以调节光圈大小；
 * 调用setApertureChangedListener设置光圈值变动监听接口；
 * 绘制的光圈最大直径将填满整个view
 * 
 */
public class ApertureView extends View {

    public interface ApertureChanged {
        public void onApertureChanged(float newapert);
    }

    private static final float ROTATE_ANGLE = 30;
    private static final String TAG = "ApertureView";
    private static final float COS_30 = 0.866025f;
	private static final int WIDTH = 100; // 当设置为wrap_content时测量大小
	private static final int HEIGHT = 100;
    private int mCircleRadius, mOutCirecleRadius;
    private int mBladeColor;
    private int mBackgroundColor;
    private int mSpace;
    private float mMaxApert = 1;
	private float mMinApert = 0.15f;
	private float mCurrentApert = 2f;

    private PointF[] mPoints = new PointF[6];
    private Bitmap mBlade;
    private Paint mPaint;
    private Path mPath;
    private ApertureChanged mApertureChanged;
    private Paint outCirclePaint;

    private float mPrevX;
    private float mPrevY;
    private int outCircleDelta = 13;//外圆与内圆的距离
    private int outCirclePaintStrokeWidth = 3;//外圆线条大小
    private int defaultBladeColor = 0xCC797B78;
    private int defaultBackgroundColor = 0xFFFFFFFF;
    private int defaultSpace = 5;
    private int outCircleColor = 0xFF388A7B;

    public ApertureView(Context context, AttributeSet attrs) {
	 super(context, attrs);
	/*
       
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ApertureView);
        outCircleColor = getResources().getColor(R.color.gy_seekbar_progress_color);
        defaultBladeColor = getResources().getColor(R.color.gy_aperture_blade_color);
        defaultBackgroundColor = getResources().getColor(R.color.gy_aperture_background_color);
        mSpace = (int) array.getDimension(R.styleable.ApertureView_blade_space, defaultSpace);
        mBladeColor = array.getColor(R.styleable.ApertureView_blade_color, defaultBladeColor);
        mBackgroundColor = array.getColor(R.styleable.ApertureView_background_color, defaultBackgroundColor);
        array.recycle();
        Log.d("lyh", " init ");
*/

        init();
    }

    public ApertureView(Context contexts) {
        super(contexts);
        outCircleColor = getResources().getColor(R.color.gy_seekbar_progress_color);
        defaultBladeColor = getResources().getColor(R.color.gy_aperture_blade_color);
        defaultBackgroundColor = getResources().getColor(R.color.gy_aperture_background_color);
        mSpace = defaultSpace;
        mBladeColor = 0xFFDFDFDF; //defaultBladeColor
        mBackgroundColor = 0xFFFFFFFF; //defaultBackgroundColor
        //array.recycle();
        
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mPaint.setAntiAlias(true);
        for (int i = 0; i < 6; i++) {
            mPoints[i] = new PointF();
        }
        outCirclePaint = new Paint(); //设置一个笔刷大小是3的黄色的画笔
        outCirclePaint.setColor(outCircleColor);
        outCirclePaint.setStrokeJoin(Paint.Join.ROUND);
        outCirclePaint.setStrokeCap(Paint.Cap.ROUND);
        outCirclePaint.setStrokeWidth(outCirclePaintStrokeWidth);
        outCirclePaint.setAntiAlias(true);
        outCirclePaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int paddX = getPaddingLeft() + getPaddingRight();
        int paddY = getPaddingTop() + getPaddingBottom();
        mCircleRadius = widthSpecSize - paddX < heightSpecSize - paddY ? (widthSpecSize - paddX) / 2
                : (heightSpecSize - paddY) / 2;
        if (widthSpecMode == MeasureSpec.AT_MOST
                && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(WIDTH, HEIGHT);
            mCircleRadius = (WIDTH - paddX) / 2;
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(WIDTH, heightSpecSize);
            mCircleRadius = WIDTH - paddX < heightSpecSize - paddY ? (WIDTH - paddX) / 2
                    : (heightSpecSize - paddY) / 2;
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, HEIGHT);
            mCircleRadius = widthSpecSize - paddX < HEIGHT - paddY ? (widthSpecSize - paddX) / 2
                    : (HEIGHT - paddY) / 2;
        }
	    mOutCirecleRadius = mCircleRadius;
        mCircleRadius = mCircleRadius - outCircleDelta;
        if (mCircleRadius < 1) {
            mCircleRadius = 1;
        }
        mPath = new Path();
        mPath.addCircle(0, 0, mCircleRadius, Path.Direction.CW);
        createBlade();
    }

    //yangheng
    private void drawLeftTop(Canvas canvas) {
        int x = (getWidth() - 120) / 2;
        int y = (getWidth() - 120) / 2;
        canvas.drawLine(-50, 50, -20, 50, outCirclePaint);
        canvas.drawLine(-50, 50, -50, 20, outCirclePaint);
    }

    private void drawRightTop(Canvas canvas) {
        int x = getWidth() - (getWidth() - 120) / 2;
        int y = (getWidth() - 120) / 2;
        canvas.drawLine(50, 50, 20, 50, outCirclePaint);
        canvas.drawLine(50, 50, 50, 20, outCirclePaint);
    }

    private void drawLeftBottom(Canvas canvas) {
        int x = (getWidth() - 120) / 2;
        int y = getHeight() - (getWidth() - 120) / 2;
        canvas.drawLine(-50, -50, -20, -50, outCirclePaint);
        canvas.drawLine(-50, -50, -50, -20, outCirclePaint);
    }

    private void drawRightBottom(Canvas canvas) {
        int x = getWidth() - (getWidth() - 120) / 2;
        int y = getHeight() - (getWidth() - 120) / 2;
        canvas.drawLine(50, -50, 20, -50, outCirclePaint);
        canvas.drawLine(50, -50, 50, -20, outCirclePaint);
    }
    //yangheng

    @Override
    public void onDraw(Canvas canvas) {
        canvas.save();
        calculatePoints();
        canvas.translate(getWidth() / 2, getHeight() / 2);
        //旋转十字星
        //canvas.rotate(ROTATE_ANGLE * (mCurrentApert - mMinApert) / (mMaxApert - mMinApert));
        //drawOutCircle(canvas);
        
        //yangheng
        drawLeftTop(canvas);
        drawRightTop(canvas);
        drawLeftBottom(canvas);
        drawRightBottom(canvas);
        //yangheng
        
        //drawCross(canvas);
        canvas.clipPath(mPath);
        //canvas.drawColor(mBackgroundColor);
        for (int i = 0; i < 6; i++) {
            canvas.save();
            canvas.translate(mPoints[i].x, mPoints[i].y);
            canvas.rotate(-i * 60);
            //canvas.drawBitmap(mBlade, 0, 0, mPaint);
            canvas.restore();
        }
        canvas.restore();
    }

    /**
     * 画外圆
     *
     * @param canvas
     */
    private void drawOutCircle(Canvas canvas) {
        int radius = mOutCirecleRadius - outCirclePaintStrokeWidth;
        canvas.drawCircle(0, 0, radius, outCirclePaint); //画圆圈
    }

    /**
     * 画十字星
     *
     * @param canvas
     */
    private void drawCross(Canvas canvas) {
         int radius = mOutCirecleRadius - outCirclePaintStrokeWidth;
        canvas.drawLine(0, mCircleRadius, 0, radius, outCirclePaint);
        canvas.drawLine(-mCircleRadius, 0, -radius, 0, outCirclePaint);
        canvas.drawLine(0, -mCircleRadius, 0, -radius, outCirclePaint);
        canvas.drawLine(mCircleRadius, 0, radius, 0, outCirclePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            return false;
        }
		if(GYConfig.BOKEH_SEEKBAR_APERTURE){
			return true;
		}
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mPrevX = event.getX();
            mPrevY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
            float diffx = Math.abs((event.getX() - mPrevX));
            float diffy = Math.abs((event.getY() - mPrevY));
            if (diffy > diffx) {
                float diff = (float) Math.sqrt(diffx * diffx + diffy * diffy)
                        / mCircleRadius * mMaxApert;
                if (event.getY() > mPrevY) {
                    setCurrentApert(mCurrentApert - diff);
                } else {
                    setCurrentApert(mCurrentApert + diff);
                }
                mPrevX = event.getX();
                mPrevY = event.getY();
            }
            break;
        default:
            break;
        }
        return true;
    }
	
		 public  Bitmap getSrcBitmap(int w, int h) {
		 Bitmap bm = Bitmap.createBitmap(getWidth(),getHeight(), Config.ARGB_8888);
	     Canvas c = new Canvas(bm);
	     c.translate(getWidth() / 2, getHeight() / 2);
	     c.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG)); 
		 for (int i = 0; i < 6; i++) {
			c.save();
			c.translate(mPoints[i].x, mPoints[i].y);
			c.rotate(-i * 60);
			c.drawBitmap(mBlade, 0, 0, mPaint);
			c.restore();
		}
		return bm;
	}

	 public Bitmap getDstBitmap(int w, int h) {
	        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        Canvas c = new Canvas(bm);
	        c.drawCircle(getWidth() / 2, getHeight() / 2, mCircleRadius, mPaint);
	        return bm;
	 }
	 
	public Bitmap getApertureBitmap(int w, int h) {
		Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bm);
		
		Rect srcRect  = new Rect(0, 0, w, h);
      
        Bitmap dstBitmap = getDstBitmap(w, h);
        Bitmap srcBitmap = getSrcBitmap(w, h);
        
        c.drawBitmap(dstBitmap, 0, 0, mPaint);
        mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        c.drawBitmap(srcBitmap, srcRect, srcRect, mPaint);
      
        mPaint.setXfermode(null);
		return bm;
	}
	


	 
	 public Bitmap getRectBitmap(int w, int h) {
	        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        Canvas c = new Canvas(bm);
	        Path path = new Path();
			
			path.moveTo(mSpace / 2 / COS_30, mSpace);
			path.lineTo(w, h);
			path.lineTo(w, mSpace);
			path.close();
			mPaint.setColor(mBladeColor);
			c.drawPath(path, mPaint);
	        return bm;
	 }
	 

    private void calculatePoints() {
        if (mCircleRadius - mSpace <= 0) {
            Log.e(TAG, "the size of view is too small and Space is too large");
            return;
        }
        float curRadius = mCurrentApert / mMaxApert * (mCircleRadius - mSpace);
        mPoints[0].x = curRadius / 2;
        mPoints[0].y = -curRadius * COS_30;
        mPoints[1].x = -mPoints[0].x;
        mPoints[1].y = mPoints[0].y;
        mPoints[2].x = -curRadius;
        mPoints[2].y = 0;
        mPoints[3].x = mPoints[1].x;
        mPoints[3].y = -mPoints[1].y;
        mPoints[4].x = -mPoints[3].x;
        mPoints[4].y = mPoints[3].y;
        mPoints[5].x = curRadius;
        mPoints[5].y = 0;
    }

    private void createBlade() {
		mBlade = Bitmap.createBitmap(mCircleRadius,
				(int) (mCircleRadius * 2 * COS_30), Config.ARGB_8888);
		Canvas canvas = new Canvas(mBlade);
		mPaint.setAntiAlias(true);
		Bitmap srcBitmap = getRectBitmap(mBlade.getWidth(), mBlade.getHeight());
		canvas.drawRect(0, 0, mBlade.getWidth(), mBlade.getHeight(), mPaint);
		mPaint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(srcBitmap, 0, 0, mPaint);
		mPaint.setXfermode(null);

	}

    /**
     * 设置光圈片的颜色
     * @param bladeColor
     */
    public void setBladeColor(int bladeColor) {
        mBladeColor = bladeColor;
    }

    /**
     * 设置光圈背景色
     */
    public void setBackgroundColor(int backgroundColor) {
        mBackgroundColor = backgroundColor;
    }

    /**
     * 设置光圈片之间的间隔
     * @param space
     */
    public void setSpace(int space) {
        mSpace = space;
    }

    /**
     * 设置光圈最大值
     * @param maxApert
     */
    public void setMaxApert(float maxApert) {
        mMaxApert = maxApert;
    }

    /**
     * 设置光圈最小值
     * @param mMinApert
     */
    public void setMinApert(float mMinApert) {
        this.mMinApert = mMinApert;
    }

    public float getCurrentApert() {
        return mCurrentApert;
    }

    public void setCurrentApert(float currentApert) {
        if (currentApert > mMaxApert) {
            currentApert = mMaxApert;
        }
        if (currentApert < mMinApert) {
            currentApert = mMinApert;
        }
        if (mCurrentApert == currentApert) {
            return;
        }
        mCurrentApert = currentApert;
        invalidate();
        if (mApertureChanged != null) {
            mApertureChanged.onApertureChanged(currentApert);
        }
    }

    public void setApertureChangedListener(ApertureChanged listener) {
        mApertureChanged = listener;
    }

}
