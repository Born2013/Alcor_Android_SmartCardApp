
package com.bird.flashlight;

/*[BIRD_NEW_FLASHLIGHT] add chengshujiang 20170307 begin*/
public class BirdFlashlightNative {

    public static native boolean isFlashLightOn();

    static {
        System.loadLibrary("flash_light_jni");
    }
}
/*[BIRD_NEW_FLASHLIGHT] add chengshujiang 20170307 end*/
