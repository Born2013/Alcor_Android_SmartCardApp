package com.bird.lrcview;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.text.TextUtils;
import android.util.Log;

/**
 * 每行歌词的实体类，实现了Comparable接口，方便List<LrcRow>的sort排序
 * 
 */
public class LrcRow implements Comparable<LrcRow> {

    /** 开始时间 为00:10:00 ***/
    private String timeStr;
    /** 开始时间 毫米数 00:10:00 为10000 **/
    private int time;
    /** 歌词内容 **/
    private String content;
    /** 该行歌词显示的总时间 **/
    private int totalTime;

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(int totalTime) {
        this.totalTime = totalTime;
    }

    public String getTimeStr() {
        return timeStr;
    }

    public void setTimeStr(String timeStr) {
        this.timeStr = timeStr;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LrcRow() {
        super();
    }

    public LrcRow(String timeStr, int time, String content) {
        super();
        this.timeStr = timeStr;
        this.time = time;
        this.content = content;
    }

    /**
     * 将歌词文件中的某一行 解析成一个List<LrcRow> 因为一行中可能包含了多个LrcRow对象 比如
     * [03:33.02][00:36.37]当鸽子不再象征和平 ，就包含了2个对象
     * 
     * @param lrcLine
     * @return
     */
    public static final List<LrcRow> createRows(String lrcLine) {

        String str = new String(lrcLine);

        Integer[] result = new Integer[0]; // first we assume that there's no
        String[] resultTime = new String[0]; // first we assume that there's no

        List<Integer> list = new ArrayList<Integer>();
        list.clear();
        List<String> listTime = new ArrayList<String>();
        listTime.clear();

        Scanner scn = new Scanner(str);
        String tmp = null;
        int tmpTimeValue = 0; // millisecond value

        // scan by regular expression, and calculate each millisecond value
        // there could be more than one timeTag for each lyric sentence.
        tmp = scn
                .findInLine("\\[\\d\\d:[0-5]\\d\\.\\d\\d\\]|\\[\\d\\d:[0-5]\\d\\]"); // [mm:ss.xx]
                                                                                     // or
                                                                                     // [mm:ss]
        while (null != tmp) {
            if (10 == tmp.length()) { // [mm:ss.xx]
                tmpTimeValue = Integer.parseInt(tmp.substring(1, 3)) * 60000 // minutes
                        + Integer.parseInt(tmp.substring(4, 6)) * 1000 // second
                        + Integer.parseInt(tmp.substring(7, 9)); // m second

            } else if (7 == tmp.length()) { // [mm:ss]
                tmpTimeValue = Integer.parseInt(tmp.substring(1, 3)) * 60000 // minutes
                        + Integer.parseInt(tmp.substring(4, 6)) * 1000; // second
            }

            list.add(Integer.valueOf(tmpTimeValue));
            listTime.add(tmp.replace("[", "").replace("]", ""));
            tmp = scn
                    .findInLine("\\[\\d\\d:[0-5]\\d\\.\\d\\d\\]|\\[\\d\\d:[0-5]\\d\\]"); // next
                                                                                         // one
        }

        Integer[] integerArray = list.toArray(result);
        String[] stringArray = listTime.toArray(resultTime);
        int len = integerArray.length;
        List<LrcRow> lrcRows = null;
        if (len != 0) {
            lrcRows = new ArrayList<LrcRow>();
            for (int k = 0; k < len; k++) {
                LrcRow lrcRow = new LrcRow(stringArray[k],
                        integerArray[k].intValue(), resolveLrc(lrcLine));
                lrcRows.add(lrcRow);
            }
        }
        return lrcRows;
    }

    /*****
     * 对于每一句歌词可能有两种格式：[mm:ss.xx] or [mm:ss] 将每一句的时间去除，得到歌词部分
     */
    private static String resolveLrc(String lrcLine) {
        String str = new String(lrcLine);

        // remove all the valid time tags ([mm:ss.xx]) with an empty string
        str = str.replaceAll(
                "\\[\\d\\d:[0-5]\\d\\.\\d\\d\\]|\\[\\d\\d:[0-5]\\d\\]", "");
        // remove all extended time tags (<mm:ss.xx> format)
        str = str.replaceAll(
                "\\<\\d\\d:[0-5]\\d\\.\\d\\d\\>|\\<\\d\\d:[0-5]\\d\\>", "");

        str.trim();
        return str;
    }

    /****
     * 把歌词时间转换为毫秒值 如 将00:10.00 转为10000
     * 
     * @param timeStr
     * @return
     */
    private static int formatTime(String timeStr) {
        timeStr = timeStr.replace('.', ':');
        String[] times = timeStr.split(":");

        return Integer.parseInt(times[0]) * 60 * 1000
                + Integer.parseInt(times[1]) * 1000
                + Integer.parseInt(times[2]);
    }

    @Override
    public int compareTo(LrcRow anotherLrcRow) {
        return (int) (this.time - anotherLrcRow.time);
    }

    @Override
    public String toString() {
        return "LrcRow [timeStr=" + timeStr + ", time=" + time + ", content="
                + content + "]";
    }

}
