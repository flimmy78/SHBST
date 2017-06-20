package com.shbst.bst.tftdisplay_15_h.BusParameter.SendBean;

/**
 * Created by hegang on 2016-09-22.
 */

public class LiftMessage {
    public  String floor;
    public  String equipmentMovingStatus;
    public  String arrival;

    @Override
    public String toString() {
        return "LiftMessage{" +
                "floor='" + floor + '\'' +
                ", equipmentMovingStatus='" + equipmentMovingStatus + '\'' +
                ", arrival='" + arrival + '\'' +
                '}';
    }

    public String getFloor() {
        return floor;
    }

    public String getEquipmentMovingStatus() {
        return equipmentMovingStatus;
    }

    public String getArrival() {
        return arrival;
    }
}
