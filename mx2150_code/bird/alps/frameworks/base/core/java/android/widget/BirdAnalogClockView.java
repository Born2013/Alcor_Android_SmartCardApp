package android.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.content.SharedPreferences;
import android.provider.Settings;
import com.android.internal.R;
import android.widget.RemoteViews.RemoteView;

/**
 * @hide
 */
@RemoteView
public class BirdAnalogClockView extends FrameLayout {
	static final String TAG = "BirdAnalogClockView";
	private LayoutInflater mLayoutInflater;
	private FrameLayout mFrameLayout;
	private Context mContext;
	private float cHeight;
	private int cNumber;
	private int cValue;
	private float firstY, secondY, Move;
	private boolean isMove = false;
	private boolean canNext = false;
	private boolean isMoving = false;
	private boolean isGuide = false;
	private View[] CLOCK = new View[7];

	public BirdAnalogClockView(Context context) {
		this(context, null);
	}

	public BirdAnalogClockView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BirdAnalogClockView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		Log.i(TAG,"BirdAnalogClockView");
		setClipChildren(false);
		mContext = context;
		int mclock = Settings.System.getInt(mContext.getContentResolver(), "mclock", 0);
		int clock = Settings.System.getInt(mContext.getContentResolver(), "clock", 1);
		if (mclock == 0) {
			isGuide = true;
			cNumber = 0;
		}
		else {
			cNumber = clock;
		}
		mLayoutInflater = LayoutInflater.from(context);
		View view = mLayoutInflater.inflate(R.layout.zzzzz_bird_analog_clock_layout, this);
		mFrameLayout = (FrameLayout) findViewById(R.id.clock_layout);

		CLOCK[0] = (AnalogClockguide)findViewById(R.id.analogClockguide);
		CLOCK[1] = (AnalogClockone) findViewById(R.id.analogClockone);
		CLOCK[2] = (AnalogClocktwo) findViewById(R.id.analogClocktwo);
		CLOCK[3] = (AnalogClockthree) findViewById(R.id.analogClockthree);
		CLOCK[4] = (AnalogClockfour) findViewById(R.id.analogClockfour);
		CLOCK[5] = (AnalogClockfive) findViewById(R.id.analogClockfive);
		CLOCK[6] = (AnalogClocksix) findViewById(R.id.analogClocksix);

		for (int i = 0; i < 7; i++) {
			CLOCK[i].setAlpha(0f);
		}
		CLOCK[cNumber].setAlpha(1f);

		mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				switch (motionEvent.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						canNext = false;
						cHeight = CLOCK[0].getWidth();
						firstY = motionEvent.getY();//得到位置的纵坐标
						if (firstY < (cHeight / 3 * 2))
							canNext = true;
						break;
					}
					case MotionEvent.ACTION_MOVE: {
						secondY = motionEvent.getY();//得到位置的纵坐标
						Move = secondY - firstY;
						if (Move > (cHeight / 3) && isMove == false && canNext == true && isMoving == false)
							nextClock();
						break;
					}
					case MotionEvent.ACTION_UP: {
						if (Math.abs(Move) < (cHeight / 10) && isMove == false) {
							Log.i(TAG,"view: "+view);
							Intent intentDate = new Intent("android.settings.DATE_SETTINGS");
							intentDate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
									| Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
							mContext.startActivity(intentDate);
						}
						isMove = false;
						break;
					}
				}
				return true;
			}
		});
	}

	public void nextClock() {
		Settings.System.putInt(mContext.getContentResolver(), "mclock", 1);
		isMoving = true;
		canNext = false;
		isMove = true;
		cNumber++;
		if (cNumber > 6)
			cNumber = 1;
		Settings.System.putInt(mContext.getContentResolver(), "clock", cNumber);
		CLOCK[cNumber].setAlpha(1f);
		AlphaAnimation alphaClockOut = new AlphaAnimation(1f, 0f);
		alphaClockOut.setInterpolator(new LinearInterpolator());
		alphaClockOut.setDuration(600);//每次动画时间
		alphaClockOut.setFillAfter(true);

		TranslateAnimation translateClockOut = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0.2f);
		translateClockOut.setInterpolator(new LinearInterpolator());
		translateClockOut.setDuration(600);//每次动画时间
		translateClockOut.setFillAfter(true);

		AnimationSet animationClockOut = new AnimationSet(false);//将动画效果放入集合中
		animationClockOut.addAnimation(alphaClockOut);
		animationClockOut.addAnimation(translateClockOut);

		animationClockOut.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if(cNumber==1 && isGuide==false)
					CLOCK[6].setAlpha(0f);
				else
					CLOCK[cNumber-1].setAlpha(0f);
				CLOCK[cNumber].setAlpha(1f);
				isMoving = false;
				isGuide = false;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		if(cNumber==1 && isGuide==false)
			CLOCK[6].startAnimation(animationClockOut);
		else 
			CLOCK[cNumber-1].startAnimation(animationClockOut);

		AlphaAnimation alphaClockIn = new AlphaAnimation(0f, 1f);
		alphaClockIn.setInterpolator(new LinearInterpolator());
		alphaClockIn.setDuration(450);//每次动画时间
		alphaClockIn.setFillAfter(true);

		ScaleAnimation scaleClockIn = new ScaleAnimation(0.0f, 1f, 0.0f, 1f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		scaleClockIn.setInterpolator(new DecelerateInterpolator());
		scaleClockIn.setDuration(450);
		
		AnimationSet animationClockIn = new AnimationSet(false);//将动画效果放入集合中
		animationClockIn.addAnimation(alphaClockIn);
		animationClockIn.addAnimation(scaleClockIn);
		animationClockIn.setStartOffset(150);
		animationClockIn.setFillAfter(true);

		CLOCK[cNumber].startAnimation(animationClockIn);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Log.i(TAG,"onDetachedFromWindow");
	}
}
