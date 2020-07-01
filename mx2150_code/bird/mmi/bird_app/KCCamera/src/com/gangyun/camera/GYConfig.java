package com.gangyun.camera;

import android.util.Log;

/**
* config system/build.prop file. eg:
# GangYun BOKEH function 1:open, 0:close
ro.gangyun_camera_bokeh=1
ro.gangyun_bokeh_support_front=true
# 960 * 540 = 518400
ro.gangyun_bokeh_max_pixels=518400
ro.gangyun_bokeh_max_radius=140
ro.gangyun_bokeh_max_level=100
ro.gangyun_bokeh_scope=30
NOTE:
Caused by: java.lang.IllegalArgumentException: key.length > 31
*/
public class GYConfig {

        public static final String TAG = "GYConfig";
	    public static final boolean BOKEH_OPEN = isOpenBokeh();
        public static final boolean BOKEH_SUPPORT_FRONT = false; 
        public static final int BOKEH_UPPER_LIMIT_PIXELS = getBokehUpperLimitPixels();
        public static final int BOKEH_MAX_RADIUS = getBokehMaxRadius();//[0-
        public static final int BOKEH_MAX_LEVEL = getBokehMaxLevel();//[0-100]
        public static final int BOKEH_SCOPE = getBokehScope();//[0-100)

        public static final boolean BOKEH_SUPPORT_FRONT_DEFAULT = true; 
        public static final int BOKEH_UPPER_LIMIT_PIXELS_DEFAULT = 960 * 540;
        public static final int BOKEH_MAX_RADIUS_DEFAULT = 120;//[0-
        public static final int BOKEH_MAX_LEVEL_DEFAULT = 100;//[0-100]
        public static final int BOKEH_SCOPE_DEFAULT = 50;//[0-100)
        
		public static final boolean BOKEH_SEEKBAR_APERTURE = false;
		public static final boolean BOKEH_FACE = false;
		public static final boolean BOKEH_FOCUS_MOVING_SHOW_APRTURE = true;		
		public static final boolean BOKEH_NO_PLAY_FOCUS_SOUND = true;

        public static boolean isOpenBokeh() {
            boolean isOpen = "1".equals(android.os.SystemProperties.get("ro.gangyun_camera_bokeh"));
            Log.v(TAG, "gangyun11 isOpenBokeh:" + isOpen);
	        return isOpen;
        }

        public static boolean isBokehSupportFront() {
            boolean isBokehSupportFront = android.os.SystemProperties.getBoolean("ro.gangyun_bokeh_support_front", BOKEH_SUPPORT_FRONT_DEFAULT);
            Log.v(TAG, "gangyun11 isBokehSupportFront:" + isBokehSupportFront);
	        return isBokehSupportFront;
        }

        public static int getBokehUpperLimitPixels() {
            String pixels = android.os.SystemProperties.get("ro.gangyun_bokeh_max_pixels");
            int defaultMaxPixels = BOKEH_UPPER_LIMIT_PIXELS_DEFAULT;
            if (!android.text.TextUtils.isEmpty(pixels)) {
               try {
                   defaultMaxPixels = Integer.valueOf(pixels);
	       } catch (Exception e) {
		   Log.e(TAG, "gangyun11 getBokehUpperLimitPixels:" + e.getMessage());
	       }
            }
            Log.v(TAG, "gangyun11 getBokehUpperLimitPixels:" + defaultMaxPixels);
	        return defaultMaxPixels;
        }

        public static int getBokehMaxRadius() {
            String pixels = android.os.SystemProperties.get("ro.gangyun_bokeh_max_radius");
            int defaultVaule = BOKEH_MAX_RADIUS_DEFAULT;
            int defaultMaxPixels = defaultVaule;
            if (!android.text.TextUtils.isEmpty(pixels)) {
               try {
                   defaultMaxPixels = Integer.valueOf(pixels);
                   if (defaultMaxPixels < 0) {
                       defaultMaxPixels = defaultVaule;
                   }
	       } catch (Exception e) {
		   Log.e(TAG, "gangyun11 getBokehMaxRadius:" + e.getMessage());
	       }
            }
            Log.v(TAG, "gangyun11 getBokehMaxRadius:" + defaultMaxPixels);
	        return defaultMaxPixels;
        }

        public static int getBokehMaxLevel() {
            String pixels = android.os.SystemProperties.get("ro.gangyun_bokeh_max_level");
            int defaultVaule = BOKEH_MAX_LEVEL_DEFAULT;
            int defaultMaxPixels = defaultVaule;
            if (!android.text.TextUtils.isEmpty(pixels)) {
               try {
                   defaultMaxPixels = Integer.valueOf(pixels);
                   if (defaultMaxPixels < 0 ||defaultMaxPixels > 100) {
                       defaultMaxPixels = defaultVaule;
                   }
	       } catch (Exception e) {
		   Log.e(TAG, "gangyun11 getBokehMaxLevel:" + e.getMessage());
	       }
            }
            Log.v(TAG, "gangyun11 getBokehMaxLevel:" + defaultMaxPixels);
	        return defaultMaxPixels;
        }

        public static int getBokehScope() {
            String pixels = android.os.SystemProperties.get("ro.gangyun_bokeh_scope");
            int defaultVaule = BOKEH_SCOPE_DEFAULT;
            int defaultMaxPixels = defaultVaule;
            if (!android.text.TextUtils.isEmpty(pixels)) {
               try {
                   defaultMaxPixels = Integer.valueOf(pixels);
		   if (defaultMaxPixels < 0 ||defaultMaxPixels > 100) {
                       defaultMaxPixels = defaultVaule;
                   }
	       } catch (Exception e) {
		   Log.e(TAG, "gangyun11 getBokehScope:" + e.getMessage());
	       }
            }
            Log.v(TAG, "gangyun11 getBokehScope:" + defaultMaxPixels);
	        return defaultMaxPixels;
        }

}
