package com.shbst.bst.tftdisplay_15_h.BusParameter.ParamConstant;

/**
 * 传感器状态
 * Created by hegang on 2016-09-23.
 */

public class CodeData {
    public String getCodeData(int sType, int locate ) {
        switch (sType) {
            case 0x01:   // 温度
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_Tem_InCar;
                    case 1:
                        return ParamData.Sensor_Tem_InWell_1;
                    case 2:
                        return ParamData.Sensor_Tem_InWell_2;
                    case 3:
                        return ParamData.Sensor_Tem_InWell_3;
                    case 4:
                        return ParamData.Sensor_Tem_OutWell;
                    case 5:
                        return ParamData.Sensor_Tem_RedundancyBackup;
                    default:
                        break;
                }
                break;
            case 0x02:   //湿度
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_Humidity_InCar;
                    case 1:
                        return ParamData.Sensor_Humidity_InWell;
                    case 2:
                        return ParamData.Sensor_Humidity_OutWell;
                    case 3:
                        return ParamData.Sensor_Humidity_RedundancyBackup;
                    default:
                        break;
                }
                break;
            case 0x03:    //光感
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_Light_InCar;
                    case 1:
                        return ParamData.Sensor_Light_InWell;
                    case 2:
                        return ParamData.Sensor_Light_RedundancyBackup;
                    default:
                        break;
                }
                break;
            case 0x04:
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_Noise_InCarRun;
                    case 1:
                        return ParamData.Sensor_Noise_InCarDoor;
                    case 2:
                        return ParamData.Sensor_Noise_Hoistway_Top_Host;
                    case 3:
                        return ParamData.Sensor_Noise_RedundancyBackup;
                    default:
                        break;
                }
                break;
            case 0x05:    //震动
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_Vibration_HCar;
                    case 1:
                        return ParamData.Sensor_Vibration_VCar;
                    case 2:
                        return ParamData.Sensor_Vibration_RedundancyBackup;
                    default:
                        break;
                }
                break;
            case 0x06:   //人体红外
                switch (locate) {
                    case 0:
                        return ParamData.Sensor_PIR_InCar;
                    case 1:
                        return ParamData.Sensor_PIR_HallDoor_1;
                    case 2:
                        return ParamData.Sensor_PIR_HallDoor_2;
                    case 3:
                        return ParamData.Sensor_PIR_HallDoor_3;
                    case 4:
                        return ParamData.Sensor_PIR_HallDoor_4;
                    case 5:
                        return ParamData.Sensor_PIR_TopCar;
                    case 6:
                        return ParamData.Sensor_PIR_BottomCar;
                    case 7:
                        return ParamData.Sensor_PIR_RedundancyBackup;
                    default:
                        break;
                }
                break;
        }
        return null;
    }
}
