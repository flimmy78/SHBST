package com.shbst.bst.tftdisplay_15_h.BusParameter.SendBean;

/**
 * Created by hegang on 2016-09-22.
 */
public class LiftAlarm {
    public String sensorCode;
    public String alarmCode;
    public String alarmDescription;

    @Override
    public String toString() {
        return "LiftAlarm{" +
                "sensorCode='" + sensorCode + '\'' +
                ", alarmCode='" + alarmCode + '\'' +
                ", alarmDescription='" + alarmDescription + '\'' +
                '}';
    }
}
