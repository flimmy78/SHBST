package com.shbst.bst.tftdisplay_15_h.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by hegang on 2016-10-13.
 */
public class DataString {
    private static String mYear;
    private static String mMonth;
    private static String mDay;
    private static String mTime;
    private static String mWay;

    public static String[] StringData() {

        SimpleDateFormat formatter = new SimpleDateFormat("HH:MM");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        mTime = formatter.format(curDate);
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        mYear = String.valueOf(c.get(Calendar.YEAR)); // 获取当前年份
        mMonth = String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份
        mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));// 获取当前月份的日期号码
        mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        if ("1".equals(mWay)) {
            mWay = "日";
        } else if ("2".equals(mWay)) {
            mWay = "一";
        } else if ("3".equals(mWay)) {
            mWay = "二";
        } else if ("4".equals(mWay)) {
            mWay = "三";
        } else if ("5".equals(mWay)) {
            mWay = "四";
        } else if ("6".equals(mWay)) {
            mWay = "五";
        } else if ("7".equals(mWay)) {
            mWay = "六";
        }
        Log.i("aaaaaaaaaaaaaa", "StringData: " + mYear + "年" + mMonth + "月" + mDay + "日" + "/时间：" + mTime + "/星期" + mWay);
//        mYear + "年" + mMonth + "月" + mDay+"日"+"/时间："+mTime+"/星期"+mWay;
        String[] timeData = {mYear, mMonth, mDay, mTime, mWay};
        return timeData;
    }

}
