/*[BIRD_SMALL_VIEW_WINDOW] zhangaman 20140731 begin */
package com.bird.clam;

public class ClamNative {

	static {
		System.loadLibrary("clamjni");
	}
	
	public static native boolean readClamState();
}
/*[BIRD_SMALL_VIEW_WINDOW] zhangaman 20140731 end */
