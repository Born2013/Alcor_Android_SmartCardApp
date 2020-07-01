package com.bird.calendarUtil;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Date;
/*[BIRD_OLD_PHONE_NEW_BEF]huangzhangbin 20170802 begin*/
public class CalendarUtil {

    //  星期
    private static String[] week = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    //  农历月份
    private static String[] lunarMonth = {"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"};
    //  农历日
    private static String[] lunarDay = {"初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};

    /**
     * 获得当天time点时间戳
     */
    public static long getSignTime(int time) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, time);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return (cal.getTimeInMillis() / 1000);
    }

    /**
     * 时间段格式化 hh:mm:ss 用来做倒计时
     */
    public static String timeFormat(long time) {
        int hours = (int) time / 3600;
        String hourStr;
        if (hours < 10) {
            hourStr = "0" + hours;

        } else {
            hourStr = hours + "";
        }
        int min = (int) (time - hours * 3600) / 60;
        String minStr;
        if (min < 10) {
            minStr = "0" + min;

        } else {
            minStr = min + "";
        }
        int second = (int) (time - (time / 60) * 60);
        String secondStr;
        if (second < 10) {
            secondStr = "0" + second;

        } else {
            secondStr = second + "";
        }
        String timeStr = (hourStr + ":" + minStr + ":" + secondStr);

        return timeStr;
    }

    /**
     * 获取年月日 格式yyyy-MM-dd
     */
    public static String getDate() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    /**
     * 获取年、月 格式 yyyy-MM
     *
     * @return
     */
    public static String getMonth() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        String month = simpleDateFormat.format(date);
        return month;
    }

    /**
     * 获取当月日期
     * @return Day of month
     */
    public static String getDay() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day+"";
    }

    /**
     * 获取当月日期
     * @return month
     */
    public static String getMonthDay() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.MONTH) + 1;
        return day+"";
    }

    /**
     * 获取星期几
     */
    public static String getWeek() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return week[dayOfWeek - 1];
    }

    /**
     * 获取农历月份
     * @return
     */
    // public static String getLunarMonth() {
    //     Calendar calendar = Calendar.getInstance();
    //     int year = calendar.get(Calendar.YEAR);
    //     int month = calendar.get(Calendar.MONTH) + 1;
    //     int day = calendar.get(Calendar.DAY_OF_MONTH);
    //     int[] lunarDate = LunarCalendar.solarToLunar(year, month, day);
    //     return lunarMonth[lunarDate[1] - 1];
    // }

    /**
     * 获取农历日
     * @return
     */
    // public static String getLunarDay() {
    //     Calendar calendar = Calendar.getInstance();
    //     int year = calendar.get(Calendar.YEAR);
    //     int month = calendar.get(Calendar.MONTH) + 1;
    //     int day = calendar.get(Calendar.DAY_OF_MONTH);
    //     int[] lunarDate = LunarCalendar.solarToLunar(year, month, day);
    //     return lunarDay[lunarDate[2] - 1];
    // }

    public static int getLunarDayPosition() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int[] lunarDate = LunarCalendar.solarToLunar(year, month, day);
        return lunarDate[2] - 1;
    }

    public static String getLunarMonth(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int[] lunarDate = LunarCalendar.solarToLunar(year, month, day);
        return lunarMonth[lunarDate[1] - 1];
    }

    public static String getLunarDay(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int[] lunarDate = LunarCalendar.solarToLunar(year, month, day);
        return lunarDay[lunarDate[2] - 1];
    }



}
/*[BIRD_OLD_PHONE_NEW_BEF]huangzhangbin 20170802 end*/
