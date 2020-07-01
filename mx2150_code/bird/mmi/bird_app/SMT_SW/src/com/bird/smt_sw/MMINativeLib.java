package com.bird.smt_sw;

public class MMINativeLib {
	static {
		System.loadLibrary("mminative_jni");
	}
	public static native boolean device_test_led_r_on_jni();
	public static native boolean device_test_led_r_off_jni();
	public static native boolean device_test_keypad_bk_turn_on_jni();
	public static native boolean device_test_keypad_bk_turn_off_jni();
	public static native String  device_test_get_barcode_jni();
	public static native boolean BreathledStart();
	public static native boolean BreathledStop();
	public static native boolean Breathled1();
	public static native boolean device_test_led_g_on_jni();
	public static native boolean device_test_led_g_off_jni();
	public static native boolean device_test_led_b_on_jni();
	public static native boolean device_test_led_b_off_jni();
}
