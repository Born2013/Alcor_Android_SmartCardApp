package com.bird.flashlight;
//[BIRD][BIRD_FLASHLIGHT_JNI][wangjianping][手电筒接口][20140620] BEGIN
public class FlashLightJni {
     static {
		System.loadLibrary("birdflashlight_jni");
		//android.util.Log.d("wangjianping","load lib");
      }
  public static native void turn_on_flashlight();
  public static native void turn_off_flashlight();
  public static native boolean get_state_flashlight();
}
//[BIRD][BIRD_FLASHLIGHT_JNI][wangjianping][手电筒接口][20140620] END
