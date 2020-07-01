package android.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import com.android.internal.R;
import android.util.AttributeSet;
import android.view.View;

public class AnalogClockguide extends View {
	
	private Context mContext;
	private final Paint mPaint = new Paint();
	private float width;
	private float height;

	public AnalogClockguide(Context context) {
		this(context, null);
	}

	public AnalogClockguide(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AnalogClockguide(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mContext = context;
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		width = getWidth();
		height = getHeight();
		super.onDraw(canvas);
		Bitmap guide = getBmpScale(mContext, R.drawable.zzzzz_widget_clock_guide, width, width);
		canvas.drawBitmap(guide, 0, (height - width) / 2, mPaint);
	}

	public Bitmap getBmpScale(Context mContext, int id, float cell_width, float cell_height) {
		Bitmap viewBg = BitmapFactory.decodeResource(mContext.getResources(), id);
		Matrix matrix = new Matrix();
		int width = viewBg.getWidth();//获取资源位图的宽
		int height = viewBg.getHeight();//获取资源位图的高
		float w = cell_width / viewBg.getWidth();
		float h = cell_height / viewBg.getHeight();
		matrix.postScale(w, h);//获取缩放比例
		Bitmap dstbmp = Bitmap.createBitmap(viewBg, 0, 0, width, height, matrix, true); //根据缩放比例获取新的位图
		return dstbmp;
	}
}

