package com.shbst.bst.tftdisplay_15_h.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.shbst.bst.tftdisplay_15_h.SerialPort.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import de.greenrobot.event.EventBus;

public class KONE_TransformProtocol extends Service {
    private static final String LOG_TAG = "InterChipProtocol";
    private static final int FRAME_INTERVAL = 50;                   /* 帧间隔，单位：ms */
    private static final int FRAME_MAX_LENGTH = 128;                /* 接收和发送的最大帧长度 */

    private boolean KeepRunning = false;                /* 本service主线程是否运行 */
    private LocalBinder binder = new LocalBinder();
    private EventBus InfoBus = EventBus.getDefault();
    private SerialPort port;
    private Queue<byte[]> RecvFrameQueue;               /* 接收帧队列 */
    private Queue<byte[]> SendFrameQueue;   /* 发送帧队列 */
    private InputStream input;
    private LiftInfo lift;                              /* 电梯实时状态对象 */
    private liftDateTime liftDateTime;
    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "The service has been created.");
        KeepRunning = true;
        new Thread(ProtocolMainThread).start();

    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "The service has been destroyed.");
        KeepRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "The service is start.");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "The service is bind.");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "The service is unbind.");
        return true;
    }

    /* 所有提供给其他组件使用的方法，均定义于LocalBinder中 */
    public class LocalBinder extends Binder {

        public void sendSystemDate(boolean systemFlag){
            if(systemFlag){
                new Thread(setDate).start();
            }
        }
    }

    /******************
     * 本service的主线程
     *********************/
    private Runnable ProtocolMainThread = new Runnable() {
        @Override
        public void run() {

            Log.d(LOG_TAG, "Service thread itself is running......");

            try {
                RecvFrameQueue = new LinkedList<>();
                SendFrameQueue = new LinkedList<>();
                lift = new LiftInfo();
                liftDateTime = new liftDateTime();
                port = new SerialPort(new File("/dev/ttymxc1"), 9600, 0);

                new Thread(ReceiveThread).start();      /* 启动数据接收线程 */
                new Thread(SendThread).start();

                byte[] frame;
                while (KeepRunning) {
                    if ((frame = RecvFrameQueue_poll()) != null) {

                        byte[] NoneCRCData = new byte[frame.length - 3];
                        System.arraycopy(frame, 1, NoneCRCData, 0, NoneCRCData.length);
                        byte CheckValue = CRC_Check(NoneCRCData);

                        if (CheckValue == frame[frame.length - 2]) {

                            process_Frame(frame);

                        } else {
                            Log.e(LOG_TAG, "CRC check error! The received value is " + frame[frame.length - 2]
                                    + ", and the calculate value is " + CheckValue);
                        }
                    } else {

                        wrapSleep(FRAME_INTERVAL);/* 短暂睡眠，让出CPU占用 */
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            KeepRunning = false;    /* 如果是发生异常退出while循环，需要主动把KeepRunning置为false */
            try {
                input.close();      /* 关闭input数据流，ReceiveThread收到异常，并结束线程。 */
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d(LOG_TAG, "Service thread itself is over.");
        }
    };


    /* 独立的接收数据线程 */
    private Runnable ReceiveThread = new Runnable() {
        final int STX = 0x80;
        final int ETX = 0x81;

        @Override
        public void run() {
            try {
                input = port.getInputStream();
                byte[] recv = new byte[FRAME_MAX_LENGTH];
                byte RecvStatus = 0;                        /* 接收状态：0--未收到STX；1--已收到STX，正在接收DATA；2--已收到完整一帧 */
                int RecvCnt = 0;                            /* 已接收帧的字节数 */
                byte[] RecvFrame = new byte[FRAME_MAX_LENGTH];
                int i;
                int ReadByte;

                while ((ReadByte = input.read(recv)) > 0) {
                    if (RecvStatus == 0) {
                        if ((recv[0] & 0xFF) == STX) {
                            RecvCnt = 0;
                            RecvStatus = 1;
                            for (i = 0; i < ReadByte; i++) {
                                RecvFrame[RecvCnt++] = recv[i];
                                if ((recv[i] & 0xFF) == ETX) {
                                    RecvStatus = 2;
                                    break;
                                }
                            }
                        }
                    } else if (RecvStatus == 1) {
                        for (i = 0; i < ReadByte; i++) {
                            RecvFrame[RecvCnt++] = recv[i];
                            if (recv[i] == ETX) {
                                RecvStatus = 2;
                                break;
                            }
                        }
                    }
                    if (RecvStatus == 2) {
                        byte[] TmpFrame = new byte[RecvCnt];
                        for (i = 0; i < RecvCnt; i++) {
                            TmpFrame[i] = RecvFrame[i];
                        }

                        RecvFrameQueue_offer(TmpFrame);
                        RecvCnt = 0;
                        RecvStatus = 0;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /* 独立的发送数据线程（检测队列中是否有需要发送的数据，并控制发送间隔） */
    private Runnable SendThread = new Runnable() {
        @Override
        public void run() {
            try {
                OutputStream output = port.getOutputStream();

                byte[] frame;
                while (KeepRunning) {
                    if ((frame = SendFrameQueue.poll()) != null) {
                        output.write(frame);
                    }
                    wrapSleep(FRAME_INTERVAL);  /* 控制帧间隔 */
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(LOG_TAG, "Send thread itself is over.");
        }
    };
    /* 包裹异常处理后的sleep函数 */
    private void wrapSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void RecvFrameQueue_offer(byte[] frame) {
        RecvFrameQueue.offer(frame);
    }

    private synchronized byte[] RecvFrameQueue_poll() {
        return RecvFrameQueue.poll();
    }

    /* 校验函数 */
    private byte CRC_Check(byte[] data) {
        byte result = 0;

        for (int i : data) {
            result ^= i;
        }

        return (byte) (result & 0x7F);
    }

    private void process_Frame(byte[] frame) {
        if (frame[1] == 0x00) {
            Log("-----------"+Arrays.toString(frame));
            /* 处理楼层 */
            String floor = new String();
            for (int i = 2; i < 5; i++) {
                if ((frame[i] >= 0x30) && (frame[i] <= 0x39)) {
                    floor = floor.concat(String.valueOf(frame[i] - 0x30));
                } else if ((frame[i] >= 0x41) && (frame[i] <= 0x5A)) {
                    byte[] tmp = new byte[1];
                    tmp[0] = frame[i];
                    try {
                        floor = floor.concat(new String(tmp, "ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (frame[i] == 0x2D) {
                    floor = floor.concat("-");
                }
            }

            /* 处理箭头 */
            int arrow;
            if ((frame[5] & 0x01) == 0x01) {
                if ((frame[5] & 0x04) == 0x04) {
                    arrow = 3;
                } else {
                    arrow = 1;
                }
            } else if ((frame[5] & 0x02) == 0x02) {
                if ((frame[5] & 0x04) == 0x04) {
                    arrow = 4;
                } else {
                    arrow = 2;
                }
            } else {
                arrow = 0;
            }

            /* 处理功能 */
            int function = 0;
            if ((frame[6] & 0x01) == 0x01) {
                function = 4;     //ATS (司机服务)
            }  else if ((frame[7] & 0x01) == 0x01) {
                function = 1;    //FRD (消防运行)
            } else if ((frame[7] & 0x40) == 0x40) {
                function = 2;    //OSS (退出服务开关)
            }else if ((frame[7] & 0x20) == 0x20) {
                function = 3;   //PRC (内呼优先服务)
            }else if ((frame[7] & 0x02) == 0x02) {
                function = 5;   //OLF (超载)
            }else if ((frame[6] & 0x20) == 0x20) {
                function = 6;
            }
            systemNewDate = String.valueOf((frame[10])+frame[11]+frame[12]+frame[14]+frame[15]);

            liftDateTime.mYear = String.valueOf(Integer.valueOf(frame[10])+2017);
            if(frame[11]<10){
                liftDateTime.mMonth = "0"+String.valueOf(frame[11]);
            }else{
                liftDateTime.mMonth = String.valueOf(frame[11]);
            }

            liftDateTime.mDay = String.valueOf(frame[12]);

            if(frame[14]<10){
                liftDateTime.mHour = "0"+String.valueOf(frame[14]);
            }else{
                liftDateTime.mHour = String.valueOf(frame[14]);
            }
            if(frame[15]<10){
                liftDateTime.mMinute = "0"+String.valueOf(frame[15]);
            }else{
                liftDateTime.mMinute = String.valueOf(frame[15]);
            }
            liftDateTime.mSecond = String.valueOf(frame[16]);

            lift.setFloor(floor);
            lift.setArrow(arrow);
            lift.setFunction(function);

            InfoBus.post(liftDateTime);
            InfoBus.post(lift);
        }
        if(frame[1] ==0x01){
            Log("接收单片机发送时间  "+Arrays.toString(frame));
            if((frame[2] & 0xFF) == (byte)0x2A && (frame[3] & 0xFF) == (byte)0x55){
                byte[] data = new byte[6];
                data[0] = (byte) 0x80;
                data[1] = (byte) 0x01;
                data[2] = (byte) 0x55;
                data[3] = (byte) 0x2A;
                data[4] = (byte) 0x7E;
                data[5] = (byte) 0x81;
                SendFrameQueue.offer(data);
            }
            if((frame[2] & 0xFF) == (byte)0x55 && (frame[3] & 0xFF) == (byte)0x2A){
                byte[] data = new byte[6];
                data[0] = (byte) 0x80;
                data[1] = (byte) 0x01;
                data[2] = (byte) 0x2A;
                data[3] = (byte) 0x55;
                data[4] = (byte) 0x7E;
                data[5] = (byte) 0x81;
                SendFrameQueue.offer(data);
            }
        }

    }

    String systemNewDate;

    //获得当前年月日时分秒星期
    public byte[] getTime(){

        byte[] date = new byte[7];
        final Calendar c = Calendar.getInstance();

        TimeZone aDefault = TimeZone.getDefault();

        c.setTimeZone(aDefault);
        String mYear = String.valueOf(c.get(Calendar.YEAR)); // 获取当前年份
        String mMonth = String.valueOf(c.get(Calendar.MONTH) + 1);// 获取当前月份
        String mDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));// 获取当前月份的日期号码
        String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        String mHour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));//时
        String mMinute = String.valueOf(c.get(Calendar.MINUTE));//分
        String mSecond = String.valueOf(c.get(Calendar.SECOND));//秒

        date[0] = (byte) (Integer.valueOf(mYear)-2017);
        date[1] = Byte.parseByte(mMonth);
        date[2] = Byte.parseByte(mDay);
        date[3] = (byte) (Integer.valueOf(mWay)-1);
        date[4] = Byte.parseByte(mHour);
        date[5] = Byte.parseByte(mMinute);
        date[6] = Byte.parseByte(mSecond);


        return date;
    }
    //发送系统时间给单片机
    Runnable setDate = new Runnable() {
        @Override
        public void run() {
            byte[] systemDate = getTime();
            byte[] date = new byte[19];

            byte[] date_check = new byte[16];
            date[0] = (byte) 0x80;

            date[1] = (byte) 0x00;
            date[2] = (byte) 0x00;
            date[3] = (byte) 0x00;
            date[4] = (byte) 0x00;
            date[5] = (byte) 0x00;
            date[6] = (byte) 0x00;
            date[7] = (byte) 0x00;
            date[8] = (byte) 0x00;
            date[9] = (byte) 0x00;

            date[10] = systemDate[0];
            date[11] = systemDate[1];
            date[12] = systemDate[2];
            date[13] = systemDate[3];
            date[14] = systemDate[4];
            date[15] = systemDate[5];
            date[16] = systemDate[6];

            String dates =  String.valueOf((date[10])+date[11]+date[12]+date[14]+date[15]);

            System.arraycopy(date, 1, date_check, 0, date_check.length);
            date[17] = CRC_Check(date_check);
            date[18] = (byte) 0x81;
            /* 加入发送队列 */
            if(date[10] >= 0){
                if(!dates.equals(systemNewDate)){
                    Log("校准单片机时间 "+dates);
                    SendFrameQueue.offer(date);
                }
            }
        }
    };

    private void Log(String data){
//        Log.i(LOG_TAG, "Log: "+data);
    }
    public class liftDateTime{
        public String mYear;
        public String mMonth;
        public String mDay;
        public String mHour;
        public String mMinute;
        public String mSecond;
    }

    /* 电梯运行状态 */
    public class LiftInfo {
        private String floor;

        /**
         * 箭头状态：
         * 0--不显示
         * 1--上箭头      2--下箭头
         * 3--上箭头滚动  4--下箭头滚动
         * 5--上箭头闪烁  6--下箭头闪烁
         */
        private int Arrow;

        /**
         * 功能状态：
         * 0--不显示
         * 1--消防
         * 2--超载
         * 3--地震
         * 4--内呼锁定
         * 5--紧急呼叫
         * 6--优先服务
         * 7--停止服务
         * 8--司机服务
         * 9--满载
         */
        private int function;

        public void setFloor(String floor) {
            this.floor = floor;
        }

        public String getFloor() {
            return floor;
        }

        public void setArrow(int arrow) {
            Arrow = arrow;
        }

        public int getArrow() {
            return Arrow;
        }

        public void setFunction(int function) {
            this.function = function;
        }

        public int getFunction() {
            return function;
        }
    }
}
