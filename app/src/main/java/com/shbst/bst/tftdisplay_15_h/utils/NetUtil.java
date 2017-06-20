package com.shbst.bst.tftdisplay_15_h.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.shbst.androiddevicesdk.utils.ShellUtils;


/**
 * 测试网络连接工具类
 * Created by hegang on 2017-02-28.
 */
public class NetUtil {

    /**
     * 没有连接网络
     */
    public static final int NETWORK_NONE = -1;
    /**
     * 移动网络
     */
    public static final int NETWORK_MOBILE = 0;
    /**
     * 无线网络
     */
    public static final int NETWORK_WIFI = 1;

    public static boolean getNetWork(){
        if(isMobileConnected()){
            return true;
        }else{
            return false;
        }
    }

    public static int getNetWorkState(Context context) {
        getNetWork();
//        // 得到连接管理器对象
//        ConnectivityManager connectivityManager = (ConnectivityManager) context
//                .getSystemService(Context.CONNECTIVITY_SERVICE);
//
//        NetworkInfo activeNetworkInfo = connectivityManager
//                .getActiveNetworkInfo();
//        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
//            if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_WIFI)) {
//                return NETWORK_WIFI;
//            } else if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_MOBILE)) {
//                return NETWORK_MOBILE;
//            }
//        } else {
//            return NETWORK_NONE;
//        }
//        return NETWORK_NONE;
        //结果返回值
        int netType = 0;
        //获取手机所有连接管理对象
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE);
        //获取NetworkInfo对象
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        //NetworkInfo对象为空 则代表没有网络
        if (networkInfo == null) {
            return netType;
        }
        //否则 NetworkInfo对象不为空 则获取该networkInfo的类型
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_WIFI) {
            //WIFI
            netType = 1;
        } else if (nType == ConnectivityManager.TYPE_MOBILE) {
            int nSubType = networkInfo.getSubtype();
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService
                    (Context.TELEPHONY_SERVICE);
            //3G   联通的3G为UMTS或HSDPA 电信的3G为EVDO
            if (nSubType == TelephonyManager.NETWORK_TYPE_LTE
                    && !telephonyManager.isNetworkRoaming()) {
                netType = 4;
            } else if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS
                    || nSubType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || nSubType == TelephonyManager.NETWORK_TYPE_EVDO_0
                    && !telephonyManager.isNetworkRoaming()) {
                netType = 3;
                //2G 移动和联通的2G为GPRS或EGDE，电信的2G为CDMA
            } else if (nSubType == TelephonyManager.NETWORK_TYPE_GPRS
                    || nSubType == TelephonyManager.NETWORK_TYPE_EDGE
                    || nSubType == TelephonyManager.NETWORK_TYPE_CDMA
                    && !telephonyManager.isNetworkRoaming()) {
                netType = 2;
            } else {
                netType = 2;
            }
        }
        return netType;
    }
    /**
     * 判断MOBILE网络是否可用
     * @return
     */
    public static boolean isMobileConnected() {
//        if (context != null) {
//            //获取手机所有连接管理对象(包括对wi-fi,net等连接的管理)
//            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context
//                    .CONNECTIVITY_SERVICE);
//            //获取NetworkInfo对象
//            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
//            //判断NetworkInfo对象是否为空 并且类型是否为MOBILE
//            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
//                return networkInfo.isAvailable();
//        }
//        return false;

        ShellUtils.CommandResult var1 = ShellUtils.execCmd("ping -c 1 -w 1 mqtt.drop-beats.com", false);
        boolean var2 = var1.result == 0;

        return var2;
    }

}
