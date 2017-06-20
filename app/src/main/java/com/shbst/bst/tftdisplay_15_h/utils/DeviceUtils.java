package com.shbst.bst.tftdisplay_15_h.utils;

import android.util.Log;

import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * Created by hegang on 2017-02-23.
 */
public class DeviceUtils {

    /**
     * 获取设备端mac地址
     *
     * @return mac地址
     */
    public static String getMac() {
        String macSerialCode = "";
        String strLine = "";
        StringBuilder sb = new StringBuilder();
        String[] macSeg = null;
        int MACString_Length = 17;
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; ; ) {
                strLine = input.readLine();
                if (strLine.length() == MACString_Length) {
                    macSeg = strLine.trim().split(":");
                    if (macSeg.length == 6) {
                        for (int i = 0; i < 6; i++) {
                            sb.append(macSeg[i]);
                        }
                        macSerialCode = sb.toString();
                        break;
                    }

                }
            }
        }catch (Exception ex) {

            return chageMac("00:00:00:00:00:00");

        }
        String mac = macSerialCode.toLowerCase();
        Log.i("mac", "getMac: "+mac);
        return chageMac(mac);
    }

    private static String chageMac(String mac){
        String input = mac;
        String regex = "(.{2})";
        input = input.replaceAll (regex, "$1:");
        return input.substring(0,input.length()-1);

    }
}
