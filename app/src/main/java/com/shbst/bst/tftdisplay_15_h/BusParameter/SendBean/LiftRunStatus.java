package com.shbst.bst.tftdisplay_15_h.BusParameter.SendBean;

/**
 * Created by hegang on 2016-09-22.
 */
public class LiftRunStatus {

    public String equipmentMode;
    public String UpsPower;

    @Override
    public String toString() {
        return "LiftRunStatus{" +
                "equipmentMode='" + equipmentMode + '\'' +
                ", UpsPower='" + UpsPower + '\'' +
                '}';
    }

    public String getEquipmentMode() {
        return equipmentMode;
    }

    public String getUpsPower() {
        return UpsPower;
    }
}
