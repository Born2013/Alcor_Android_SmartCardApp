package com.mediatek.camera.mode.gyfacebeauty;

/**
 * Created by root on 2/9/17.
 */
public class BirdDualCameraNative {
    static {
        System.loadLibrary("dual_camera_jni");
    }

    /**
     * Open Accelere device, call before power up
     *
     * @return (0,success; -1, failed)
     */
    public static native boolean readSubCameraState();

    /**
     * close Accelere device, call before power up
     *
     * @return (0,success; -1, failed)
     */
    public static native boolean writeFrontCameraState();

    /**
     * calibration
     *
     * @return (0,success; -1, failed)
     */
    public static native boolean writeBackCameraState();
}
