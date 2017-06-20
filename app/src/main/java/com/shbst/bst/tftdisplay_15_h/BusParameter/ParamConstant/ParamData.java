package com.shbst.bst.tftdisplay_15_h.BusParameter.ParamConstant;

/**
 * Created by hegang on 2016-09-22.
 */
public class ParamData {
    /**
     *  1. 当前楼层值
     *
     *  2. 箭头状态（e_ARROWSTA_T类型枚举）   0-不显示  1-上箭头  2-下箭头 3 -上箭头滚动  4-下箭头滚动  5-上箭头闪烁  6-下箭头闪烁
     *
     *  3. 到站状态（e_ARRIVESTA_T类型枚举）  0-未到站  1-上到站钟  2-下到站钟  3-上到站灯  4-下到站灯 5-上到站钟与上到站灯 6-下到站钟与下到站灯
     *
     *  4. 常规状态（e_LIFTNORSTA_T类型枚举） 0--无状态  1--故障  2--检修  3--锁梯  4--司机  5--直驶  6--消防  7--安全  8--满载  9--超载
     *
     *  5 .传感器位置编号（e_SENSORPOS_T类型枚举）0--底层  1--顶层  2--轿厢  3--1层位置  4--2层位置  以此类推
     *
     *  6. 控制方式（e_CTRLMODE_T类型枚举）  0--呼梯模式   1--照明调节模式
     *
     */

    public static String IMX6_COM = "/dev/ttymxc1";
    public static int BaudRate = 9600;

    public static String LIFTINFORMATION  = "LiftInformation";       // 电梯基本信息
    public static String LIFTRUNSTATUS    = "LiftRunStatus";         // 电梯运行状态
    public static String LIFTSENSOR       = "LiftSensor";            // 传感器状态

    public static String LIFTFAULT     = "Fault";          // 电梯故障
    public static String LIFTALARM     = "Alarm";          // 电梯告警

    public static byte ESC = 0x1B;


    public static int Sensor_Tem_Max = 80;     //  温度  最大值  单位
    public static int Sensor_Tem_Min = -30;    //  温度  最小值

    public static int Sensor_Humidity_Min = 30;    //  湿度  最小值  单位 %
    public static int Sensor_Humidity_Max = 100;   //  湿度  最大值

    public static int Sensor_Light_Max = 100;   //  光感  最大值  单位 LX
    public static int Sensor_Light_Min = 10;    //  光感  最小值

    public static int Sensor_Noise_Max = 80;    //  噪音  最大值  单位 DB
    public static int Sensor_Noise_Min = 30;    //  噪音  最小值

    public static int Sensor_Vibration_Max = 25;    //  震动  最大值  单位 CM/SS
    public static int Sensor_Vibration_Min = 5;    //  震动  最小值

    /**************************************  温度   *********************************/
    public static int TEM = 0;

    public static String Sensor_Tem_InCar = "TC";    // 轿厢内
    public static String Sensor_Tem_InWell_1 = "THI1";  // 井道内1
    public static String Sensor_Tem_InWell_2 = "THI2";  // 井道内2
    public static String Sensor_Tem_InWell_3 = "THI3";  // 井道内3
    public static String Sensor_Tem_OutWell = "THO";   // 井道内
    public static String Sensor_Tem_RedundancyBackup = "";      // 冗余备份

    /**************************************  湿度   *********************************/

    public static String Sensor_Humidity_InCar = "HC";    // 轿厢内
    public static String Sensor_Humidity_InWell = "HHI";  // 井道内
    public static String Sensor_Humidity_OutWell = "HHO";   // 井道内
    public static String Sensor_Humidity_RedundancyBackup = "";      // 冗余备份


    /**************************************  光感   *********************************/

    public static String Sensor_Light_InCar = "LC";    // 轿厢内
    public static String Sensor_Light_InWell = "LH1";  // 井道内
    public static String Sensor_Light_RedundancyBackup = "";      // 冗余备份


    /**************************************  噪音   *********************************/

    public static String Sensor_Noise_InCarRun = "NCR";    // 轿厢内运行
    public static String Sensor_Noise_InCarDoor = "NCD";  // 轿厢内开关门
    public static String Sensor_Noise_Hoistway_Top_Host = "NHM";   // 井道顶部/主机
    public static String Sensor_Noise_RedundancyBackup = "";      // 冗余备份

    /**************************************  震动   *********************************/

    public static String Sensor_Vibration_HCar = "VCH";    // 轿厢水平
    public static String Sensor_Vibration_VCar = "VCV";    // 轿厢垂直
    public static String Sensor_Vibration_RedundancyBackup = "";      // 冗余备份

    /**************************************人体红外 *********************************/

    public static String Sensor_PIR_InCar = "IC";               // 轿厢内
    public static String Sensor_PIR_HallDoor_1 = "ID1";         // 厅门外 （1）
    public static String Sensor_PIR_HallDoor_2 = "ID2";         // 厅门外 （2）
    public static String Sensor_PIR_HallDoor_3 = "ID3";         // 厅门外 （3）
    public static String Sensor_PIR_HallDoor_4 = "ID4";         // 厅门外 （4）
    public static String Sensor_PIR_TopCar = "ICT";             // 轿厢顶部
    public static String Sensor_PIR_BottomCar = "ICB";          // 轿厢低部
    public static String Sensor_PIR_RedundancyBackup = "";      // 冗余备份

    public static final String FLOOR = "floor";    //
    public static final String RUN = "run";
    public static final String LIGHT = "light";

    public static final String MAINFLOOR = "mainfloor";
    public static final String ARROWCHANGE = "arrowchange";
}
