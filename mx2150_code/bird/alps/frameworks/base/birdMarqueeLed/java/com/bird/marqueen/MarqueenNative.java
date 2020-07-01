package com.bird.marqueen;

/**
 * Created by root on 6/17/17.
 */
public class MarqueenNative {
    static {
        System.loadLibrary("marqueen");
    }
    public static native int marqueenOpen();
    public static native int marqueenClose();
    public static native int marqueenIoctl(int cmd,int data);
}
