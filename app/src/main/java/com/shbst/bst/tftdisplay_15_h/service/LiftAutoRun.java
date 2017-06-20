package com.shbst.bst.tftdisplay_15_h.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

public class LiftAutoRun extends Service {
    private static final String LOG_TAG = "LiftAutoRun";
    private static final int MAX_FLOOR = 20;
    private static final int MIN_FLOOR = 1;
    private static final int STAY_TIME = 16;  /* 电梯停站时间，单位：秒 */
    private static final int DIM_TIME = 15;

    private LocalBinder binder = new LocalBinder();
    private EventBus InfoBus = EventBus.getDefault();
    private boolean KeepRunning = false;        /* 本service主线程是否运行 */
    private int TimerCnt = 0;
    private int FloorStayCnt = 0;
    private int DimCnt = 0;
    LiftInfo lift = new LiftInfo();   /* 电梯实时状态对象 */
//    private int PrimitiveBrightness;
//    private int DimBrightness;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "The service has been created.");
        KeepRunning = true;
        new Thread(AutoMainThread).start();
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
    }

    /******************本service的主线程*********************/
    private Runnable AutoMainThread = new Runnable() {
        @Override
        public void run() {
            Log.d(LOG_TAG, "Service thread itself is running......");

            Timer PingTimer = new Timer();
            PingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    TimerCnt++;
                }
            }, 100, 100);

            lift.setFloor(1);
            lift.setArrow(0);
            lift.setFunction(0);
            InfoBus.post(lift); /* 发送初始状态 */

            while (KeepRunning) {
                if (TimerCnt >= 20) {   /* 1秒计时到 */
                    TimerCnt = 0;
                    if (lift.getArrow() == 0) {
                        FloorStayCnt++;
                        DimCnt++;
                        if (DimCnt == DIM_TIME) {
                            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 65);
                            Log.d(LOG_TAG, "Reduce the brightness!");
                        } else if (FloorStayCnt == STAY_TIME) {    /* 停站时间 */
                            FloorStayCnt = 0;
                            DimCnt = 0;
                            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
                            Log.d(LOG_TAG, "Improve the brightness!");
                            if (lift.getFloor() == MIN_FLOOR) {
                                lift.setArrow(1);
                            } else if (lift.getFloor() == MAX_FLOOR) {
                                lift.setArrow(2);
                            }
                        }
                    } else if (lift.getArrow() == 1) {
                        lift.setFloor(lift.getFloor() + 1);
                        if (lift.getFloor() == MAX_FLOOR) {
                            lift.setArrow(0);
                        } else {
                            if (lift.getFunction() + 1 > 5) {
                                lift.setFunction(0);
                            } else {
                                lift.setFunction(lift.getFunction() + 1);
                            }
                        }
                    } else if (lift.getArrow() == 2) {
                        lift.setFloor(lift.getFloor() - 1);
                        if (lift.getFloor() == MIN_FLOOR) {
                            lift.setArrow(0);
                            lift.setFunction(0);
                        } else {
                            if (lift.getFunction() + 1 > 5) {
                                lift.setFunction(0);
                            } else {
                                lift.setFunction(lift.getFunction() + 1);
                            }
                        }
                    }
                    InfoBus.post(lift);
                } else {
                    wrapSleep(50);
                }
            }

            Log.d(LOG_TAG, "Service thread itself is over.");
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

    /* 电梯信息 */
    public class LiftInfo {
        private int floor;
        /**
         * 箭头状态：
         * 0--不显示
         * 1--上箭头滚动
         * 2--下箭头滚动
         */
        private int Arrow;

        /**
         * 箭头状态：
         * 0--不显示
         * 1--消防
         * 2--退出服务
         * 3--优先服务
         * 4--司机服务
         * 5--超载
         */
        private int function;

        public void setFloor(int floor) {
            this.floor = floor;
        }

        public int getFloor() {
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
