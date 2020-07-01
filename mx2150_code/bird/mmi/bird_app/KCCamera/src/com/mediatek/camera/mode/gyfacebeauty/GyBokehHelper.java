/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.camera.mode.gyfacebeauty;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import java.lang.ref.WeakReference;
import android.graphics.Rect;
import com.android.camera.R;

import com.mediatek.camera.AdditionManager;
import com.mediatek.camera.ICameraAddition;
import com.mediatek.camera.ICameraContext;
import com.mediatek.camera.mode.CameraMode;
import android.graphics.ImageFormat;
import com.mediatek.camera.platform.ICameraAppUi.SpecViewType;
import com.mediatek.camera.platform.ICameraAppUi.ViewState;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.AutoFocusMvCallback;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice.cFbOriginalCallback;
import com.mediatek.camera.platform.ICameraView;
import com.mediatek.camera.platform.IFocusManager;
import com.mediatek.camera.platform.IFileSaver.FILE_TYPE;
import com.mediatek.camera.setting.SettingConstants;
import com.mediatek.camera.util.Log;
import com.mediatek.camera.util.Util;
import android.content.Context;
import android.app.Activity;
import junit.framework.Assert;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import android.view.MotionEvent; 
import java.io.IOException;
import com.mediatek.camera.platform.ICameraDeviceManager;
import com.mediatek.camera.platform.ICameraDeviceManager.ICameraDevice;
import android.hardware.Camera.Size;
import com.android.camera.CameraActivity;
import android.hardware.Camera.PreviewCallback;
import com.android.camera.CameraManager;
import android.opengl.GLSurfaceView;
import com.android.camera.ui.PreviewSurfaceView;
import com.gangyun.camera.GangyunCameraAperture;
import android.widget.RelativeLayout;
import com.android.camera.ui.RotateLayout;
import android.widget.FrameLayout;
import com.gangyun.camera.GYImage;
import com.gangyun.camera.GYSurfaceView;
import com.gangyun.camera.GYConfig;

import android.graphics.Matrix;
import com.android.camera.FocusManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ImageView;
public class GyBokehHelper implements GangyunCameraAperture.OnGyProgressChangedListener{
    private static final String TAG = "GYLog GyBokehHelper";
    //private CameraActivity mContext;
    private WeakReference<CameraActivity> mwActivity;
    private Parameters mParameters;
    private Handler mHandler;
	private static GyBokehHelper mGyBokehHelper;
    private GYImage mGYImage;
    private GYSurfaceView mGLSurfaceView;
    private PreviewSurfaceView mSurfaceView;
	private int gyPreviewW=0,gyPreviewH=0,gyx=0,gyy=0,gylevel=80,gyradius=80,gyradiusMin=50,gyradiusMax=160,gynBoderPower=30,gylevelMax=100;
	private int mFrameWidth, mFrameHeight;
	private boolean isOpen = false;
	private boolean isHide = false;
    private ICameraDevice mICameraDevice;
	private float radiusTime = 0;

    private FocusManager mFocusManager;
    private RotateLayout mFocusIndicatorRotateLayout;
    private GangyunCameraAperture gyCameraAperture;
    private static SeekBar gySeekBar;
    private static ImageView dualGuide;
    private boolean guideShow = false;
    private TextView gyCoverTags;

	
    public GyBokehHelper(CameraActivity context) {
       // mContext = context;
		mwActivity = new WeakReference<CameraActivity>(context);
		//[BIRD][GANGYUN_BOKEH_SUPPORT][双摄 光圈默认值][chengaungxiang][20180103] BEGIN
		gyradius = context.getResources().getInteger(R.integer.bird_gangyun_gyradius_default);
		//[BIRD][GANGYUN_BOKEH_SUPPORT][双摄 光圈默认值][chengaungxiang][20180103] END
    }

	private void setGyBokeyContext(CameraActivity context){
		//mContext = context;
		mwActivity = new WeakReference<CameraActivity>(context);
		mFocusManager =context.getFocusManager();

	}
	public static GyBokehHelper getInstance(CameraActivity context){		
		 Log.i(TAG, "getInstance ");
		
		if (mGyBokehHelper == null){
			mGyBokehHelper = new GyBokehHelper(context);
		}
		else{
			mGyBokehHelper.setGyBokeyContext(context);

		}
		gySeekBar = (SeekBar) context.findViewById(R.id.gyseekbar);
		dualGuide = (ImageView) context.findViewById(R.id.bird_dual_guide);
		return mGyBokehHelper;
	}
	
	public static void gyBokehrelease(){
		Log.i(TAG, " gyBokehrelease ");
		mGyBokehHelper = null;
	}

    public void showGuide() {
        dualGuide.setVisibility(android.view.View.VISIBLE);
        guideShow = true;
    }

    public void hideGuide() {
        dualGuide.setVisibility(android.view.View.GONE);
        guideShow = false;
    }

    public void updateParameters(ICameraDevice device) {
        mICameraDevice = device;
    }

  private void setGLSurfaceViewTouch(){
  if(mGLSurfaceView != null){
			mGLSurfaceView.setOnTouchListener(new View.OnTouchListener() {  
					  
				public boolean onTouch(View v, MotionEvent event) {  
					// TODO Auto-generated method stub	
					Log.i(TAG, "onTouch y:"+(int)event.getY()+" top:"+mGLSurfaceView.getTop() + " bottom:"+mGLSurfaceView.getBottom() + " getHeight:"+mGLSurfaceView.getHeight());
					mwActivity.get().getGestureRecognizer().onTouchEvent(event);
					boolean isOK = false;
					if(event.getY() > (gyCameraAperture.getApertureViewWidth()/2+20)&& event.getY() < mGLSurfaceView.getHeight()-(gyCameraAperture.getApertureViewWidth()/2+30))
						isOK = true;
					 switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
						    if(guideShow) {
						        hideGuide();
						    }
							showApertureView((int)event.getX(),(int)event.getY());
							//gyCameraAperture.gyViewHide();					
							if(isOK)
							onSingleTapUp((int)event.getX(), (int)event.getY());
							break;
						case MotionEvent.ACTION_MOVE:
				
							break;
						default:
						break;
						}					
					return isOK;  
					
					 
				}  
			}); 
    }
  }

  private void setCameraAperture(){
  	   CameraActivity mContext =mwActivity.get();
	  gyCameraAperture =  (GangyunCameraAperture)mContext.findViewById(R.id.gyCameraAperture);
		   if(gyCameraAperture!= null){   
			  gyCameraAperture.setOnGyProgressChangedListener(this);
			  gyCameraAperture.setBokehValue(gylevelMax,gylevel);
			  int len = mFocusIndicatorRotateLayout.getWidth();
			  if(len>0)
			  gyCameraAperture.setApertureViewWidth(len);
		   } 
  }
	
   private void setGyContext(CameraActivity context){
	    Log.i(TAG, "setGyContext");
		//mContext = context;

		radiusTime = (float)(((float)gyradiusMax - gyradiusMin)/gylevelMax);
		
		mwActivity = new WeakReference<CameraActivity>(context);
	       CameraActivity mContext =mwActivity.get();
		mGYImage = new GYImage(mContext);
		mSurfaceView = (PreviewSurfaceView) mContext.findViewById(R.id.camera_preview);
		mFocusIndicatorRotateLayout = (RotateLayout)mContext.findViewById(R.id.focus_indicator_rotate_layout);

		mGLSurfaceView = (GYSurfaceView)mContext.findViewById(R.id.gy_glsurfaceView);
		setGLSurfaceViewTouch();

		mGYImage.setGLSurfaceView(mGLSurfaceView);
        setCameraAperture();
	 
      gyCameraAperture =  (GangyunCameraAperture)mContext.findViewById(R.id.gyCameraAperture);
      
      gyCoverTags = (TextView)mContext.findViewById(R.id.bird_dual_cover);
	  
	    if(gySeekBar != null) {
	        gySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			    @Override
			    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		        	setGyBokehRadius(arg1);
			    }

			    @Override
			    public void onStartTrackingTouch(SeekBar arg0) {
			    // TODO Auto-generated method stub

			    }

			    @Override
			    public void onStopTrackingTouch(SeekBar arg0) {
			    // TODO Auto-generated method stub
				
			    }
		    });
	    }
	}

    //[BIRD][副摄像头被遮挡时隐藏虚化效果][yangheng][20170301] BEGIN
	private boolean isCover = true;

    public void gyShowCoverTag() {
        if(gyCoverTags != null && (isCover)) {
            gyCoverTags.setVisibility(android.view.View.VISIBLE);
            mGYImage.setParameter(gyradius,0,gynBoderPower);
            isCover = false;
        }
    }

    public void gyHideCoverTag() {
        if(gyCoverTags != null && (!isCover)) {
            gyCoverTags.setVisibility(android.view.View.GONE);
            mGYImage.setParameter(gyradius,gylevel,gynBoderPower);
            isCover = true;
        }
    }
	//[BIRD][副摄像头被遮挡时隐藏虚化效果][yangheng][20170301] END

    public void gyShowSeekber() {
        if(gySeekBar != null) {
            Log.i("yangheng", "showseekber");
            gySeekBar.setVisibility(android.view.View.VISIBLE);
        }
    }

	public boolean isBokenOpen(){
		if(mGYImage != null)
			return true;
		else
			return false;

	}
	public void gyBokehShow(){
	  CameraActivity mContext =mwActivity.get();
	  if(mContext == null ){
	  	Log.e(TAG, "gyBokehShow: the context is null!");
	  	return;
	  }

	  CameraManager.CameraProxy mCameraProxy = mContext.getCameraDevice();
	  if(mCameraProxy == null ){
	  	Log.e(TAG, "gyBokehShow: the mCameraProxy is null!");
	  	return;
	  }

	  Parameters mParameters = mCameraProxy.getParameters();
	  if(mParameters == null ){
	  	Log.e(TAG, "gyBokehShow: the mParameters is null!");
	  	return;
	  }
	  Log.i(TAG, "gyBokehShow isOpen:"+isOpen);
	  isHide = false;
	  if(isOpen ){	  	
	  	  return ;
	  }
	  gyShowSeekber();
	  isOpen = true;
	  if(mGYImage == null){
	  	    Log.i(TAG, "gyshow mGYImage new");
         	setGyContext(mContext);
			mGYImage.SetScanType(3);
      	int mmode = mParameters.getPreviewFormat();
			int yuvmode = 0;
			if(mmode == ImageFormat.YV12){
				yuvmode = 3;
			}
			else if(mmode == ImageFormat.NV21){
				yuvmode = 2;
			}
			
		    mGYImage.SetCameraYUVMode(yuvmode);
	  	}
	  
		if (mContext.getCameraDevice() != null && mContext.getCameraDevice().getCamera() != null && mContext.getCameraDevice().getCamera().getInstance() != null){
			GYSurfaceView mGLSurfaceView1 = (GYSurfaceView)mContext.findViewById(R.id.gy_glsurfaceView);
			if(mGLSurfaceView1 != null &&  mGLSurfaceView1 != mGLSurfaceView){
					Log.i(TAG, "mGLSurfaceView11 change");		
					mGLSurfaceView = mGLSurfaceView1;	
					setGLSurfaceViewTouch();
					mGYImage.setGLSurfaceView(mGLSurfaceView);	
					mSurfaceView = (PreviewSurfaceView) mContext.findViewById(R.id.camera_preview);		
					setCameraAperture();
			}
			 Log.i(TAG, "gyshow 11");		
			((com.android.camera.actor.PhotoActor)mContext.getCameraActor()).stopFaceDetection();
			((com.android.camera.actor.PhotoActor)mContext.getCameraActor()).stopPreview();
			mContext.getCameraDevice().setPreviewDisplayAsync(null);
			initBlurParameters();
		  
			mGYImage.SetCameraID(mContext.getCameraId());
			boolean mFlipHorizontal = mContext.getCameraId()==1?true:false;
			//mGYImage.SetDegree(mContext.getDisplayOrientation(),mFlipHorizontal,false);

                     //Log.i(TAG, "mGLSurfaceView11 mSurfaceView:"+mSurfaceView + " mSurfaceView1" + mSurfaceView1);		
			
			if(mSurfaceView != null){
				mContext.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mSurfaceView.setVisibility(android.view.View.GONE);
						mGLSurfaceView.setVisibility(android.view.View.VISIBLE);
						mGLSurfaceView.setAspectRatio((double)gyPreviewW/(double)gyPreviewH);
						mGLSurfaceView.onResume();
					}
				});
			}

			mGYImage.setUpCamera(mContext.getCameraDevice().getCamera().getInstance(), mContext.getDisplayOrientation(), mFlipHorizontal, false);
			((com.android.camera.actor.PhotoActor)mContext.getCameraActor()).restartPreview(false);
			
			
		}
}
	
public void gyBokehSetPreviewCallback(final boolean isStartPreview){
	Log.i(TAG, "gyBokehSetPreviewCallback 1" );

	mwActivity.get().runOnUiThread(new Runnable() {
				@Override
				public void run() {
				CameraActivity mContext =mwActivity.get();
				  if(isStartPreview){
					  	if (mContext.getCameraDevice() != null){
							Size mgyPreviewSize = mContext.getParameters().getPreviewSize();		
							gyPreviewW = mgyPreviewSize.width;
							gyPreviewH = mgyPreviewSize.height;
						}
						mGLSurfaceView.setAspectRatio((double)gyPreviewW/(double)gyPreviewH);
				  	}

					//mGLSurfaceView.setVisibility(android.view.View.VISIBLE);

					if (mContext.getCameraDevice() != null && mContext.getCameraDevice().getCamera() != null && mContext.getCameraDevice().getCamera().getInstance() != null){
					Log.i(TAG, "gyBokehSetPreviewCallback 2" );
					mGYImage.setGyImagePreviewCallback(mContext.getCameraDevice().getCamera().getInstance());
					}
					else{
					Log.i(TAG, "gyBokehSetPreviewCallback null" );

					}
	    }
		});
}

   private  int clamp(int x, int min, int max) {
        if (x > max)
            return max;
        if (x < min)
            return min;
        return x;
    }
public void showApertureView(int x,int y){
	Log.i(TAG, "showApertureView x:"+x + " y:"+y);
    if(isHide){
       return;
	}
	if(x == -1 && y== -1){
		x = mFrameWidth/2;
		y = mFrameHeight/2;
	}
	if(GYConfig.BOKEH_FOCUS_MOVING_SHOW_APRTURE){
		onSingleTapUp(x,y);
	}
	
    if(gyCameraAperture != null){
		gyCameraAperture.gyShowView();
		RelativeLayout.LayoutParams ps =(RelativeLayout.LayoutParams)gyCameraAperture.getLayoutParams();
		int h =gyCameraAperture.getHeight();
		int w = gyCameraAperture.getWidth();
		if (h == 0 || w == 0) {
		   return;
		}

		int left = 0;
		int top = (int)y-h/2;
		int seekbarW = w - gyCameraAperture.mApertureViewWidth;
		int previewTop = 0;// mGLSurfaceView.getSurfacePreviewMarginTop();
		int previewHeight = (int) gyPreviewH;
		int previewWidth = (int) mFrameWidth;
		boolean convert = ((int) x + gyCameraAperture.mApertureViewWidth)  >= mFrameWidth ? true : false;
		if(top<mGLSurfaceView.getTop()){
			Log.d(TAG, "gangyun11 showApertureView top = " + top + " gytop1:"+mGLSurfaceView.getTop());
                //    top = mGLSurfaceView.getTop();
			Log.d(TAG, "gangyun11 1 showApertureView top = " + top + " gytop1:"+mGLSurfaceView.getTop());
		}
		if (convert) {
	       	left =clamp((int) x - gyCameraAperture.mApertureViewWidth / 2 - seekbarW, 0, previewWidth - w);
				ps.setMargins(left, top,0, 0);
		} else {
		      left = clamp((int) x - gyCameraAperture.mApertureViewWidth / 2, 0, previewWidth - w);
			  ps.setMargins(left, top, 0, 0);
		}
	
		//top =  clamp((int) y - h / 2, 0, previewHeight - gyCameraAperture.mApertureViewWidth);
		Log.d(TAG, "gangyun11 showApertureView w = " + w + " h = " + h + ", left:" + left + ", top:" + top + ",  previewTop:" + previewTop + ", x:" + x + ", y:" + y + ", previewWidth:" + previewWidth + ", previewHeight:" + previewHeight+"  1convert:"+convert + " gytop1:"+mGLSurfaceView.getTop()+ " seekbarW:"+seekbarW + " getTop:"+mGLSurfaceView.getTop() + " getBottom:"+mGLSurfaceView.getBottom());
	
		//ps.setMargins((int)(x - gyCameraAperture.mApertureViewWidth/2),	(int)(y-h/2), 0, 0);
		int[] rules = ps.getRules();
		rules[RelativeLayout.CENTER_IN_PARENT] =0;
		gyCameraAperture.requestLayout();
		gyCameraAperture.convertSeekbarLeft(convert);
	
		
    }
}

public void gyBokehHide(){
	Log.i(TAG, "gyBokehHide");
	isHide = true;
	if(gyCameraAperture != null){
		gyCameraAperture.gyViewHide();
	}
	gyHideCoverTag();
	if(gySeekBar != null) {
	    gySeekBar.setVisibility(android.view.View.GONE);
	}
}


public void gyBokehclose(boolean needStartPrview){
	Log.i(TAG, "gyBokehclose needStartPrview:"+needStartPrview + " isOpen:"+isOpen + " mGLSurfaceView:"+mGLSurfaceView);
	CameraActivity mContext =mwActivity.get();
	if(mGLSurfaceView != null && isOpen){

		mContext.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mGLSurfaceView.onPause();
				if(gyCameraAperture != null){
					gyCameraAperture.gyViewHide();
				}
				mGYImage.gyImagestop();
			}
		});
		if (needStartPrview && !(mContext.getCameraActor() instanceof com.android.camera.actor.PhotoActor)){
		   if (mContext.getCameraDevice() != null && mContext.getCameraDevice().getCamera() != null){
			   mContext.getCameraDevice().setPreviewDisplayAsync(mSurfaceView.getHolder());
			   try{			   	      
					mContext.getCameraDevice().getCamera().getInstance().setPreviewTexture(null);
			   }catch(IOException e){
				   //
			   } 
			   ((com.android.camera.actor.PhotoActor)mContext.getCameraActor()).restartPreview(true);
			 
		   }
		}
		else{
			if (mContext.getCameraDevice() != null && mContext.getCameraDevice().getCamera() != null){
				mContext.getCameraDevice().getCamera().getInstance().setPreviewCallback(null);

			}
		}
		if (needStartPrview){
			mContext.runOnUiThread(new Runnable() {
				@Override
				public void run() {
				  mGLSurfaceView.setVisibility(android.view.View.GONE);			 				
				  mSurfaceView.setVisibility(android.view.View.VISIBLE);
				
				}
			});
		}
	   isOpen = false;
    //[BIRD][TASK-3793][相机在双摄模式拍照后，按home键返回到桌面时，会概率性提示“相机已停止运行”][yangheng][20170309] BEGIN
    } else {
        if (mContext.getCameraDevice() != null && mContext.getCameraDevice().getCamera() != null) {
            mContext.getCameraDevice().getCamera().getInstance().setPreviewCallback(null);
        }
    //[BIRD][TASK-3793][相机在双摄模式拍照后，按home键返回到桌面时，会概率性提示“相机已停止运行”][yangheng][20170309] BEGIN
    }
}

public void onSingleTapUp(int x, int y){
	Log.i(TAG, "onSingleTapUp x:"+x+" y:"+y);
	gyx = x;
	gyy = y;
	if(mGLSurfaceView != null && mGYImage != null){
		mGYImage.setPos(x, y);
	}

}

public int getRadiu(){
	 return gyradius;
}

public int getPower(){
	return gylevel;
}

public int getBorderPower(){
	return gynBoderPower;
}

public int getPosX(){
	return gyx;
}

public int getPosY(){
	return gyy;
}

public int getFrameWidth(){
	return mFrameWidth;
}

public int getFrameHeight(){
	return mFrameHeight;
}


	//[BIRD][副摄像头被遮挡时隐藏虚化效果][yangheng][20170301] BEGIN
	@Override
	public void onGyProgressChanged(int arg1) {
		// TODO Auto-generated method stub
   		if(mGYImage != null && isCover){
   	   		gylevel = arg1;
	   		//gyradius = gyradiusMin+(int)(radiusTime*arg1);
	   		Log.i(TAG, "onGyProgressChanged   gylevel:"+gylevel+ "   gyradius:"+gyradius+ "    radiusTime:"+radiusTime);
	   		mGYImage.setParameter(gyradius,gylevel,gynBoderPower);
   		}		
	}

	@Override
	public void onGyApertureChanged(int arg1) {
		// TODO Auto-generated method stub
		if(mGYImage != null && isCover){
	   		// gylevel = arg1;
		   	gyradius = gyradiusMin+(int)(radiusTime*arg1);
		   	Log.i(TAG, "onGyApertureChanged    gylevel:"+gylevel+ "    gyradius:"+gyradius+ "   radiusTime:"+radiusTime);
		   	mGYImage.setParameter(gyradius,gylevel,gynBoderPower);
	   	}
	}
	//[BIRD][副摄像头被遮挡时隐藏虚化效果][yangheng][20170301] END

    public void setGyBokehRadius(int radius) {
        if(mGYImage != null) {
            gyradius = radius;
            Log.i(TAG, "onGyProgressChanged   gylevel:"+gylevel+ "   gyradius:"+gyradius+ "    radiusTime:"+radiusTime);
            mGYImage.setParameter(gyradius,gylevel,gynBoderPower);
        }
    }

public void setGyBokehLevel(int level){

 Log.i(TAG, "setGyBokehLevel arg1:"+level);
if(mGYImage != null){
	gylevel = level;
	mGYImage.setParameter(gyradius,gylevel,gynBoderPower);
}

}
  private void initBlurParameters(){
  	   Log.i(TAG, "initBlurParameters");
	   CameraActivity mContext =mwActivity.get();
	   if (mContext.getParameters() != null){
		   Size mgyPreviewSize = mContext.getParameters().getPreviewSize();		   
		   gyPreviewW = mgyPreviewSize.width;
		   gyPreviewH = mgyPreviewSize.height;
		   mFrameWidth =  mContext.getPreviewFrameWidth();
		   mFrameHeight = mContext.getPreviewFrameHeight();		   
		   
		   gyx = mFrameWidth/2;
		   gyy = mFrameHeight/2;
		   mGYImage.setPos(gyx, gyy);
		   mGYImage.setParameter(gyradius,gylevel,gynBoderPower);

		   Log.i(TAG, "mGLSurfaceView is w:"+gyPreviewW+" h:"+gyPreviewH+" FrameWidth:"+ mFrameWidth + " FrameHeight:" + mFrameHeight);
	   }
  }

}
