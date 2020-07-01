package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.widget.Scroller;
import android.os.PowerManager;
import android.os.SystemClock;
/**
 * 自定义一个ScrollView实现屏幕的上下滑动
 */
public class DefinedScrollView extends ViewGroup {
	// 用来平滑过渡各个页面之间的切换,需要进行平滑的滚动，就要重写其ComputeScroll方法
	private Scroller mScroller;
	// 用来检测用户的手势，或者也可用GestureDetector
	private VelocityTracker mVelocityTracker;

	// 当前的屏幕索引
	private int mCurrentScreen;

	// 翻屏的页数
	private int curentPage = 0;
	// 默认的屏幕索引
	private int mDefaultScreen = 0;

	// 屏幕闲置时
	private static final int TOUCH_STATE_REST = 0;
	// 屏幕滚动时
	private static final int TOUCH_STATE_SCROLLING = 1;

	// 滚动速度
	private static final int SNAP_VELOCITY = 600;
	// 触屏状态为当前屏幕闲置
	private int mTouchState = TOUCH_STATE_REST;
	private static final int MIN_FLING_VELOCITY = 400; // dips
	private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
	// 触屏溢出
	private int mTouchSlop;
	/*
	 * // 最后的横向手势坐标 private float mLastMotionX;
	 */
	// 最后的纵向手势坐标
	private float mLastMotionY;

	private PageListener pageListener;
	private int mMinimumVelocity;
	private int mMaximumVelocity;
    private Context mContext;
    private PowerManager mPowerManager;

	/**
	 * 在构造方法中初始化各个状态
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 *            ScrollView的默认样式
	 */
	public DefinedScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        mContext = context;
        mPowerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
		mScroller = new Scroller(context);
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		final float density = context.getResources().getDisplayMetrics().density;
		mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		// 当前屏幕即为默认的屏幕，即为第一屏
		mCurrentScreen = mDefaultScreen;
		// 是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

	}

	public DefinedScrollView(Context context, AttributeSet attrs) {
		// 0表示没有风格
		this(context, attrs, 0);
	}

	/**
	 * 横向画出每一个子view，这样所得到的view的高与屏幕高一致，宽度为getChildCount()-1个屏幕宽度的view。
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// /int childLeft = 0;
		int childTop = 0;
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childHeight = childView.getMeasuredHeight();
				childView.layout(0, childTop, childView.getMeasuredHeight(),
						childTop + childHeight);
				childTop += childHeight;
			}
		}
	}

	/**
	 * onMeasure方法在控件的父元素正要放置它的子控件时调用.它会问一个问题，“你想要用多大地方啊？”，然后传入两个参数——
	 * widthMeasureSpec和heightMeasureSpec.
	 * 必须调用setMeasuredDimension方法,否则当控件放置时会引发一个运行时异常。
	 * 
	 * setMeasuredDimension(measuredHeight, measuredWidth);
	 * 
	 * 
	 * 当我们设置width或height为fill_parent时，容器在布局时调用子view的measure方法传入的模式是EXACTLY，
	 * 因为子view会占据剩余容器的空间，所以它大小是确定的。 而当设置为wrap_content时，容器传进去的是AT_MOST,
	 * 表示子view的大小最多是多少，这样子view会根据这个上限来设置自己的尺寸。 当子view的大小设置为精确值时，容器传入的是EXACTLY,
	 * 而MeasureSpec的UNSPECIFIED模式目前还没有发现在什么情况下使用。
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// ///final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int height = MeasureSpec.getSize(heightMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}

		/**
		 * wrap_content 传进去的是AT_MOST 代表的是最大尺寸 固定数值或fill_parent 传入的模式是EXACTLY
		 * 代表的是精确的尺寸
		 */
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		// 在当前视图内容偏移至(x , y)坐标处，即显示(可视)区域位于(x , y)坐标处。
		// 将View的Content的位置移动到(x,y)，而View的大小和位置不发生改变。如果Content超出了View的范围，则超出的部分会被挡住。
		// //scrollTo(mCurrentScreen * width, 0);
		scrollTo(0, mCurrentScreen * height);
	}

	/**
	 * According to the position of current layout scroll to the destination
	 * page. 是处理当屏幕拖动到一个位置松手后的处理
	 */
	public void snapToDestination() {
		final int screenHeight = getHeight();
		final int destScreen = (getScrollY() + screenHeight / 2) / screenHeight;
		// 根据当前x坐标位置确定切换到第几屏
		snapToScreen(destScreen);
	}

	/**
	 * 拖拽到目标屏幕，滑屏 用于根据指定屏幕号切换到该屏幕
	 * 
	 * @param whichScreen
	 */
	public void snapToScreen(int whichScreen) {
		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		if (getScrollY() != (whichScreen * getHeight())) {
			final int delta = whichScreen * getHeight() - getScrollY();
			mScroller.startScroll(0, getScrollY(), 0, delta,
					Math.abs(delta) * 2);
			mCurrentScreen = whichScreen;
			if (mCurrentScreen > curentPage) {
				curentPage = whichScreen;
				if(pageListener != null){
					pageListener.page(curentPage);
				}
			} else if (mCurrentScreen < curentPage) {
				curentPage = whichScreen;
				if(pageListener != null){
					pageListener.page(curentPage);
				}
			}
			invalidate(); // Redraw the layout
		}
	}

	/**
	 * 获得当前页码
	 */
	public int getCurScreen() {
		return mCurrentScreen;
	}

	/**
	 * 当滑动后的当前页码
	 */
	public int getPage() {
		return curentPage;
	}

	public void setToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		mCurrentScreen = whichScreen;
		// /scrollTo(whichScreen * getWidth(), 0);
		scrollTo(0, whichScreen * getHeight());
	}

	/**
	 * 主要功能是计算拖动的位移量、更新背景、设置要显示的屏幕(setCurrentScreen(mCurrentScreen);)。
	 * 
	 * 
	 * 必要时由父控件调用请求或通知其一个子节点需要更新它的mScrollX和mScrollY的值。
	 * 典型的例子就是在一个子节点正在使用Scroller进行滑动动画时将会被执行。
	 * 所以，从该方法的注释来看，继承这个方法的话一般都会有Scroller对象出现。
	 */
	@Override
	public void computeScroll() {
		// 是用来判断动画是否完成，如果没有完成返回true继续执行界面刷新的操作，各种位置信息将被重新计算用以重新绘制最新状态的界面。
		/**
		 * 如果动画没有完成(mScroller.computeScrollOffset() ==
		 * true)那么就使用scrollTo方法对mScrollX、mScrollY的值进行重新计算刷新界面，
		 * 
		 * 调用postInvalidate()方法重新绘制界面，
		 * 
		 * postInvalidate()方法会调用invalidate()方法，
		 * 
		 * invalidate()方法又会调用computeScroll方法，
		 * 
		 * 就这样周而复始的相互调用，直到mScroller.computeScrollOffset() 返回false才会停止界面的重绘动作
		 */
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
	}
	private int getClientHeight() {
        return getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    }
	
	/**
	 * 处理手机屏幕的触摸事件
	 * 
	 * @event 参数event为手机屏幕触摸事件封装类的对象，其中封装了该事件的所有信息，例如触摸的位置、触摸的类型以及触摸的时间等。
	 *        该对象会在用户触摸手机屏幕时被创建。 <A
	 *        href='\"http://www.eoeandroid.com/home.php?mod=space&uid=7300\"'
	 *        target='\"_blank\"'>@return</A>
	 *        该方法的返回值机理与键盘响应事件的相同，同样是当已经完整地处理了该事件且不希望其他回调方法再次处理时返回true，否则返回false
	 *        屏幕被按下
	 *        ：当屏幕被按下时，会自动调用该方法来处理事件，此时MotionEvent.getAction()的值为MotionEvent
	 *        .ACTION_DOWN，如果在应用程序中需要处理屏幕被按下的事件，只需重新该回调方法，然后在方法中进行动作的判断即可。
	 *        屏幕被抬起：
	 *        当触控笔离开屏幕时触发的事件，该事件同样需要onTouchEvent方法来捕捉，然后在方法中进行动作判断。当MotionEvent
	 *        .getAction()的值为MotionEvent.ACTION_UP时，表示是屏幕被抬起的事件。
	 *        在屏幕中拖动：该方法还负责处理触控笔在屏幕上滑动的事件
	 *        ，同样是调用MotionEvent.getAction()方法来判断动作值是否为MotionEvent
	 *        .ACTION_MOVE再进行处理。
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            // 用来追踪触摸事件（flinging事件和其他手势事件）的速率。用obtain()函数来获得类的实例
            mVelocityTracker = VelocityTracker.obtain();
        }
        // 用addMovement(MotionEvent)函数将motion event加入到VelocityTracker类实例中
        mVelocityTracker.addMovement(event);

        final int action = event.getAction();
        final float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                // /mLastMotionX = x;
                mLastMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaY = (int) (mLastMotionY - y);
                mLastMotionY = y;
                // 在当前视图内容继续偏移(x , y)个单位，显示(可视)区域也跟着偏移(x,y)个单位
                scrollBy(0, deltaY);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
    			final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
    			// 当你使用到速率时，使用computeCurrentVelocity(int)初始化速率的单位，并获得当前的事件的速率，
    			// 然后使用getXVelocity() 或getXVelocity()获得横向和竖向的速率。
    			// 1000:你使用的速率单位.1的意思是，以一毫秒运动了多少个像素的速率， 1000表示 一秒时间内运动了多少个像素。
    			int velocityY = (int) velocityTracker.getYVelocity();
    			// 如果手指滑动速率>600并且当前屏幕切换，因为当前屏幕初始化的时候是第一屏，为0
    			if (velocityY > mMinimumVelocity && getCurScreen() > 0) {
    				snapToScreen(getCurScreen() - 1);
    				if(pageListener != null){
    					pageListener.page(curentPage);
    				}
    			} else if (velocityY < -mMinimumVelocity
    					&& getCurScreen() < getChildCount() - 1) {
    				snapToScreen(getCurScreen() + 1);
    			} else {
    				snapToDestination();
    			}
    			if (mVelocityTracker != null) {
    				mVelocityTracker.recycle();
    				mVelocityTracker = null;
    			}
    			mTouchState = TOUCH_STATE_REST;
    			break;
        }
        return true;
    }

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPowerManager == null) {
            mPowerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        }
        mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST)) {
			return true;
		}

		final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mLastMotionY = y;
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_MOVE:
			final int yDiff = (int) Math.abs(mLastMotionY - y);
			if (yDiff > mTouchSlop) {
				mTouchState = TOUCH_STATE_SCROLLING;
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return mTouchState != TOUCH_STATE_REST;
	}

	public void setPageListener(PageListener pageListener) {
		pageListener = pageListener;
	}

	public interface PageListener {
		void page(int page);
	}
}
