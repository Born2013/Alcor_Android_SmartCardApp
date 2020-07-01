
package com.mediatek.camera.mode.gyfacebeauty;

public class BirdDualCameraNative {

    public static native boolean readSubCameraState();
    public static native boolean writeFrontCameraState();
    public static native boolean writeBackCameraState();
    public static native boolean readSubBackCameraState();
    public static native boolean openSmain();
    public static native boolean closeSmain();
    
    static {
        System.loadLibrary("dual_camera_jni");
    }
}
