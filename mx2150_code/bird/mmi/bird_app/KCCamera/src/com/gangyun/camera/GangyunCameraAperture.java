package com.gangyun.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.util.Log;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.android.camera.R;
import com.gangyun.camera.GYConfig;

public class GangyunCameraAperture extends LinearLayout implements OnSeekBarChangeListener ,ApertureView.ApertureChanged{
        private static final String TAG = "GangyunCameraAperture";
		private ApertureView mApertureView;
	    private FilterSeekBar mFilterSeekBar, mFilterSeekBar2;
        private Handler mHandler;
        private OnGyProgressChangedListener mListener;
        private final static int ON_HIDE_VIEW = 0;
		private int seekbarmax = 80;
        private int progress = 0;
		public int mApertureViewWidth = 150;
		private boolean isViewShow = false;
		private final int viewShowTime = 3000;
		public int mSeekBarViewWidth = 120;
	    public GangyunCameraAperture(Context context, AttributeSet attrs) {
		super(context, attrs);
        mApertureView = new ApertureView(context);
        mFilterSeekBar = (FilterSeekBar) LayoutInflater.from(context).inflate(R.layout.gy_seekbar, null);//new FilterSeekBar(context);
        mFilterSeekBar.setOnSeekBarChangeListener(this);
		//mFilterSeekBar.setProgressDrawable();
		mFilterSeekBar2 = (FilterSeekBar) LayoutInflater.from(context).inflate(R.layout.gy_seekbar, null);
		mFilterSeekBar2.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		    	mFilterSeekBar.setProgressAndThumb(arg1);
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub

			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			// TODO Auto-generated method stub
				if (mHandler != null) {
				    mHandler.sendEmptyMessageDelayed(ON_HIDE_VIEW,  viewShowTime);
				}		
			}
		});
        //LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
		LinearLayout.LayoutParams ps1 = new LinearLayout.LayoutParams(mApertureViewWidth, LinearLayout.LayoutParams.MATCH_PARENT);
		ps1.setMargins(1,0,1,0);
		addView(mFilterSeekBar2, ps);
		addView(mApertureView, ps1);
		addView(mFilterSeekBar, ps);
        convertSeekbarLeft(false);
        mHandler = new MainHandler(context.getMainLooper());
	   }
	    
	   public GangyunCameraAperture(Context context) {
            super(context);
       }

   
       public GangyunCameraAperture(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

   public void convertSeekbarLeft(boolean left) {
                if (left) {
                    mFilterSeekBar2.setVisibility(android.view.View.VISIBLE);
                    mFilterSeekBar.setVisibility(android.view.View.GONE);
                }else{

		    mFilterSeekBar2.setVisibility(android.view.View.GONE);
                    mFilterSeekBar.setVisibility(android.view.View.VISIBLE);
                }
       }

       public int getApertureViewWidth(){
           return mApertureViewWidth;
       }

	    public interface OnGyProgressChangedListener {
	        void onGyProgressChanged(int arg1);
			void onGyApertureChanged(int arg1);
	    }
	    
	    public void setOnGyProgressChangedListener(OnGyProgressChangedListener listener) {
	        mListener = listener;
			mApertureView.setApertureChangedListener(this);
					
	    }


	    public void setApertureViewWidth(int width) {
			if(mApertureView != null){
				ViewGroup.LayoutParams layout = mApertureView.getLayoutParams();
                layout.width = width;
                layout.height = width;
				mApertureViewWidth = width;
			}
	    }
	

	    public void setBokehValue(int max,int value) {
			if(mFilterSeekBar != null){
				mFilterSeekBar.setMax(max);
				mFilterSeekBar.setProgressAndThumb(value);
               	mFilterSeekBar2.setMax(max);
				mFilterSeekBar2.setProgressAndThumb(value);
				if(!GYConfig.BOKEH_SEEKBAR_APERTURE){ 
					mApertureView.setCurrentApert((1f/100)*value);
				}
			}

	    }
				
       private class MainHandler extends Handler {

        public MainHandler(Looper looper) {
               super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
           switch (msg.what) {
            // this msg just used for VFB,so if you want use cFB,please be
            // careful
              case ON_HIDE_VIEW:
                   gyViewHide();
                   break;

               default:
                   break;
            }
        }
      }

    public void gyViewHide() {
        isViewShow = false;
        this.setVisibility(View.GONE);
    }

    public void gyShowView() {
        isViewShow = true;
        mHandler.removeMessages(ON_HIDE_VIEW);
        this.setVisibility(View.VISIBLE);
        mHandler.sendEmptyMessageDelayed(ON_HIDE_VIEW, viewShowTime);
    }


	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		if(GYConfig.BOKEH_SEEKBAR_APERTURE){
			mApertureView.setCurrentApert((1f/100)*arg1);
		}
	 	mListener.onGyProgressChanged(arg1);
         if (mHandler != null) {
             mHandler.removeMessages(ON_HIDE_VIEW);
         }
        mFilterSeekBar2.setProgressAndThumb(arg1);
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
              if (mHandler != null) {
                  mHandler.sendEmptyMessageDelayed(ON_HIDE_VIEW,viewShowTime);
              }		
	}
	
	@Override
	public void onApertureChanged(float newapert) {
		// TODO Auto-generated method stub
	//	 Log.v(TAG,  "onApertureChanged newapert:"+newapert);
		 if(isViewShow){
		    mListener.onGyApertureChanged((int)(newapert*100));
			mHandler.removeMessages(ON_HIDE_VIEW);
            mHandler.sendEmptyMessageDelayed(ON_HIDE_VIEW,viewShowTime);
   }
		 
}
		
	private int mXPos, mYPost;
	public void setPos(int x, int y){
		mXPos = x;
		mYPost = y;
	}

}
