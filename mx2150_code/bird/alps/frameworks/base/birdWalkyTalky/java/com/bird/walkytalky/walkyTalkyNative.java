package com.bird.walkytalky;

/**
 * Created by root on 6/17/17.
 */
public class walkyTalkyNative {
    static {
        System.loadLibrary("walkytalk");
    }
    public static native int openWt();
    public static native int closeWt();
    //public static native int setVolume(String attention);
    public static native int setGroup(String attention);
    public static native int setVolume(String attention);
}
