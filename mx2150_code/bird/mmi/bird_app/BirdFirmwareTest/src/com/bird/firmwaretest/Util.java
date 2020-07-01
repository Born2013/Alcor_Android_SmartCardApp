package com.bird.firmwaretest;

import java.util.ArrayList;

/**
 * Created by root on 17-3-17.
 */
public class Util {
    public final static String SELECT_TEST_OPTIONS = "select_test_name";
    public final static String SELECT_TEST_TIMES = "select_test_time";

    public final static String FINISH_TEST = "com.bird.firmwaretest.FINISH_TEST";
    public final static int SUB_CAMERA_TEST = 1;
    public final static int MAIN_CAMERA_TEST = 2;
    /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 begin*/
    public final static int FLASHLIGHT_OPEN_TEST = 3;
    public final static int FLASHLIGHT_CLOSE_TEST = 4;
    /*[bug-6907][进行老化测试，闪光灯只在开始老化测试时亮一下，后面就没有在亮了]huangzhangbin 20180307 end*/
    public final static String TIME_FORMAT = "time_format";
}
