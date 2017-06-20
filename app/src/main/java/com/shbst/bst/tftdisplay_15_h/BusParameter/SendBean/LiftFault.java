package com.shbst.bst.tftdisplay_15_h.BusParameter.SendBean;

/**
 * Created by hegang on 2016-09-22.
 */
public class LiftFault {

    public String carLoad;
    public String faultCode;
    public String faultDescription;
    public String doorStatus;
    public String carPostion;
    public String equipmentMode;
    public String equipmentMovingStatus;
    public String floor;
    public String timestamp;

    @Override
    public String toString() {
        return "LiftFault{" +
                "carLoad='" + carLoad + '\'' +
                ", faultCode='" + faultCode + '\'' +
                ", faultDescription='" + faultDescription + '\'' +
                ", doorStatus='" + doorStatus + '\'' +
                ", carPostion='" + carPostion + '\'' +
                ", equipmentMode='" + equipmentMode + '\'' +
                ", equipmentMovingStatus='" + equipmentMovingStatus + '\'' +
                ", floor='" + floor + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
