package com.shbst.bst.tftdisplay_15_h.BusParameter.SendBean;

import java.util.Arrays;

/**
 * Created by hegang on 2016-09-22.
 */
public class LiftSensor {
//    public String timestamp;
//    public String sensorCode;
//    public String Value;
//    public String alarmCode;

    @Override
    public String toString() {
        return "LiftSensor{" +
                "sensorType=" + sensorType +
                ", sensorData=" + Arrays.toString(sensorData) +
                '}';
    }

    public int sensorType;
    public int sensorData [];


}
