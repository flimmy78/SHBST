package com.shbst.bst.tftdisplay_15_h.activity;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.shbst.bst.tftdisplay_15_h.MqttService.Bean.LiftLayoutParams;
import com.shbst.bst.tftdisplay_15_h.MqttService.MqttService;
import com.shbst.bst.tftdisplay_15_h.MyApplication;
import com.shbst.bst.tftdisplay_15_h.R;
import com.shbst.bst.tftdisplay_15_h.service.KONE_TransformProtocol;
import com.shbst.bst.tftdisplay_15_h.service.SDAT_Server;
import com.shbst.bst.tftdisplay_15_h.utils.ACache;
import com.shbst.bst.tftdisplay_15_h.utils.ConfigurationParams;
import com.shbst.bst.tftdisplay_15_h.utils.Constants;
import com.shbst.bst.tftdisplay_15_h.utils.CopyUpdataFile;
import com.shbst.bst.tftdisplay_15_h.utils.DeleteDirectory;
import com.shbst.bst.tftdisplay_15_h.utils.NetUtil;
import com.shbst.bst.tftdisplay_15_h.utils.PrefUtils;
import com.shbst.bst.tftdisplay_15_h.utils.UpdataManager;
import com.shbst.bst.tftdisplay_15_h.view.KONEScrollingText;
import com.shbst.bst.tftdisplay_15_h.view.KONETextClock;
import com.shbst.bst.tftdisplay_15_h.view.KONETextView;
import com.shbst.bst.tftdisplay_15_h.view.ScreenSurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by hegang on 2017-05-16.
 */

public class MediaScreenActivity extends BaseActivity {
    private static final String TAG = "eventBusMainActivity";
    EventBus eventBus = EventBus.getDefault();
    KONE_TransformProtocol.LocalBinder binder = null;
    KONETextView lift_floor, textTipView, lift_title;
    ImageView lift_floor_arrow;
    KONEScrollingText lift_scrollingText;
    ScreenSurfaceView lift_video;
    ImageView lift_function, lift_network;
    LinearLayout lift_title_date;
    KONETextClock lift_time, lift_date;
    WebView lift_webview;

    int arrowFlag = 0;

    int functionImage[] = {R.drawable.lift_null, R.drawable.lift_fireman, R.drawable.lift_outof, R.drawable.lift_priority,
            R.drawable.lift_attendant, R.drawable.lift_overload, R.drawable.lift_network};
    int functionImage1[] = {R.drawable.lift_null, R.drawable.zn_lift_fireman, R.drawable.zn_lift_outof, R.drawable.zn_lift_priority,
            R.drawable.zn_lift_attendant, R.drawable.zn_lift_overload, R.drawable.zn_lift_network};

    RelativeLayout tft_main;
    SDAT_Server.LiftInfo liftinfo = new SDAT_Server.LiftInfo();

    Animation animation;
    boolean newworkFlag = true;

    Params params = new Params();
    String ARROW_PATH = "/arrow/";        // 箭头资源文件目录
    String DESKTOP_PATH = "/desktop/";    // 背景资源目录
    String FUNCTION_PATH = "/function/";  // 功能图片资源目录
    String PICTURE_PTTH = "/picture/";    // 图片资源目录

    String fillScreen = "1";  //0  全屏显示  1  非全屏
    static String crossScreenNow = "";
    String scroll = "Welcome to KONE . Have a nice day.  Welcome to KONE . Have a nice day.";
    String title = "Shanghai Mart";

    Drawable upArrow;
    Drawable downArrow;
    Drawable noneArrow;
    MyApplication myApplication;
    boolean startDownload = false;

    private boolean isImagePlayType = false;    // 设置图片是单个播放还是循环播放

    private List<String> imageList;
    private int imageRunTime = 3;  //图片轮播时间  单位：s


    private String lastFloor = "";   // 上一次楼层位置
    private String nowFloor = "";    // 当前楼层位置

    ConfigurationParams configurationParams;
    ProgressDialog dialog;

    MqttService mqttservice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_10_v);
//        setContentView(R.layout.activity_main_15_v);
        // 注册EventBus
        eventBus.register(MediaScreenActivity.this);
        configurationParams = ConfigurationParams.getConfigurationParams(MediaScreenActivity.this);

        // 初始化布局控件
        initView();
        // 初始化KONE 通用箭头图片
        upArrow = getResources().getDrawable(R.drawable.up);
        downArrow = getResources().getDrawable(R.drawable.down);
        noneArrow = getResources().getDrawable(R.drawable.kone_arrow_null);

        // 初始化web云端网络服务
        mqttservice = MqttService.Open_IOT_Client(MediaScreenActivity.this);

        // 初始化电梯柜信号传输服务a
        initDataService();
    }

    /**
     * 初始化布局控件
     */
    private void initView() {
        dialog = new ProgressDialog(this);
        imageList = new ArrayList<>();
        lift_title_date = (LinearLayout) findViewById(R.id.lift_title_date);
        lift_network = (ImageView) findViewById(R.id.lift_network);
        lift_title = (KONETextView) findViewById(R.id.lift_title);
        textTipView = (KONETextView) findViewById(R.id.textTipView);
        tft_main = (RelativeLayout) findViewById(R.id.tft_main);
        lift_floor = (KONETextView) findViewById(R.id.lift_floor);
        lift_floor_arrow = (ImageView) findViewById(R.id.lift_arrow);
        lift_function = (ImageView) findViewById(R.id.lift_function);
        lift_scrollingText = (KONEScrollingText) findViewById(R.id.lift_scrollingText);
        lift_time = (KONETextClock) findViewById(R.id.lift_time);
        lift_date = (KONETextClock) findViewById(R.id.lift_date);

        showWebView();
        // 设置标题以及滚动字幕的文字 判断本地是否存在上次保存过得文字信息
        if (PrefUtils.getString(this, "Title", title) != null) {
            lift_title.setText(PrefUtils.getString(this, "Title", title));
        }
        if (PrefUtils.getString(this, "Word", scroll) != null) {
            lift_scrollingText.setText(PrefUtils.getString(this, "Word", scroll));
        }
        lift_video = (ScreenSurfaceView) findViewById(R.id.lift_video);
    }

    @Override
    protected void onStop() {
        super.onStop();
        newworkFlag = false;
        if (lift_video != null) {
            lift_video.stopSurfacView();
        }
    }

    public void onEventMainThread(UpdataManager.CopyBean copyBean) {

        if (copyBean.type.equals("start")) {
            newProgress();
        }
        Log.i(TAG, "UpdataManager: " + copyBean.toString());
        if (copyBean.type.equals("completed")) {
            startCopyProgress(Integer.parseInt(copyBean.info));
        }
        if (copyBean.type.equals("over")) {
            File path = new File(copyBean.info);
            Log.i(TAG, "copyBean:copyEnd " + copyBean.info);
            try {
                FileInputStream fis = new FileInputStream(path);
                configurationParams.parseXML(fis);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            stopCopyProgress();
        }
    }

    private void newProgress() {
        dialog.setTitle("Upgrade ...");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
        dialog.show();
    }

    private void startCopyProgress(int progress) {
        if (dialog == null) {
            newProgress();
        }
        dialog.setProgress(progress);
    }

    private void stopCopyProgress() {
        if (dialog == null) {
            newProgress();
        }
        dialog.hide();
    }

    /**
     * 操作显示网页
     */
    private void showWebView() {
        lift_webview = (WebView) findViewById(R.id.media_webview);
        lift_webview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        lift_webview.getSettings().setUseWideViewPort(true);//web1就是你自己定义的窗口对象。
        lift_webview.getSettings().setLoadWithOverviewMode(true);
        lift_webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.i(TAG, "onPageFinished: " + url);
                lift_video.stopSurfacView();
                lift_video.setVisibility(View.INVISIBLE);
                lift_webview.setVisibility(View.VISIBLE);
                setVideoFullscreen(view, PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen));
            }
        });
    }

    /**
     * 默认布局
     *
     * @return 默认布局list
     */
    public static List<LiftLayoutParams> getLayoutList() {
        Log.i(TAG, "getLayoutList: " + crossScreenNow);

        if (crossScreenNow.equals("0")) {
            List<LiftLayoutParams> listLayout_H = new ArrayList<>();
            Log.i(TAG, "getLayoutList: ------------listLayout_H----");
            listLayout_H.add(new LiftLayoutParams("arrow", "220", "245", "219", "180"));
            listLayout_H.add(new LiftLayoutParams("date", "220", "50", "1690", "124"));
            listLayout_H.add(new LiftLayoutParams("floor", "658", "400", "0", "419"));
            listLayout_H.add(new LiftLayoutParams("function", "242", "100", "206", "820"));
            listLayout_H.add(new LiftLayoutParams("text", "1260", "185", "658", "925"));
            listLayout_H.add(new LiftLayoutParams("time", "200", "80", "1690", "42"));
            listLayout_H.add(new LiftLayoutParams("title", "700", "100", "658", "47"));
            listLayout_H.add(new LiftLayoutParams("video", "1260", "710", "658", "185"));
            return listLayout_H;
        }
        if (crossScreenNow.equals("1")) {
            List<LiftLayoutParams> listLayout_V = new ArrayList<>();
            Log.i(TAG, "getLayoutList: --------------listLayout_V--");
            listLayout_V.add(new LiftLayoutParams("arrow", "138", "153", "37", "124"));
            listLayout_V.add(new LiftLayoutParams("date", "150", "38", "460", "50"));
            listLayout_V.add(new LiftLayoutParams("floor", "300", "230", "209", "60"));
            listLayout_V.add(new LiftLayoutParams("function", "170", "80", "20", "290"));
            listLayout_V.add(new LiftLayoutParams("text", "600", "35", "0", "745"));
            listLayout_V.add(new LiftLayoutParams("time", "150", "60", "460", "12"));
            listLayout_V.add(new LiftLayoutParams("title", "370", "100", "32", "14"));
            listLayout_V.add(new LiftLayoutParams("video", "600", "365", "0", "372"));
            return listLayout_V;
        }
        return null;
    }

    /**
     * 设置布局坐标信息
     *
     * @param params 每一个控件的大小位置
     */
    void setLayoutParams(LiftLayoutParams params) {
        switch (params.type) {
            case "arrow":
                lift_floor_arrow.setX(Float.parseFloat(params.x));
                lift_floor_arrow.setY(Float.parseFloat(params.y));
                RelativeLayout.LayoutParams arrowParams = new RelativeLayout.LayoutParams(Integer.parseInt(params.width),
                        Integer.parseInt(params.height));
                lift_floor_arrow.setLayoutParams(arrowParams);
                break;
            case "date":
                lift_date.setX(Float.parseFloat(params.x));
                lift_date.setY(Float.parseFloat(params.y));
                lift_date.setWidth(Integer.parseInt(params.width));
                lift_date.setHeight(Integer.parseInt(params.height));
                break;
            case "floor":
                lift_floor.setX(Float.parseFloat(params.x));
                lift_floor.setY(Float.parseFloat(params.y));
                lift_floor.setWidth(Integer.parseInt(params.width));
                lift_floor.setHeight(Integer.parseInt(params.height));
                break;
            case "function":
                lift_function.setX(Float.parseFloat(params.x));
                lift_function.setY(Float.parseFloat(params.y));
                RelativeLayout.LayoutParams functionParams = new RelativeLayout.LayoutParams(Integer.parseInt(params.width),
                        Integer.parseInt(params.height));
                lift_function.setLayoutParams(functionParams);
                break;
            case "text":
                lift_scrollingText.setX(Float.parseFloat(params.x));
                lift_scrollingText.setY(Float.parseFloat(params.y));
                lift_scrollingText.setWidth(Integer.parseInt(params.width));
                lift_scrollingText.setHeight(Integer.parseInt(params.height));
                break;
            case "time":
                lift_time.setX(Float.parseFloat(params.x));
                lift_time.setY(Float.parseFloat(params.y));
                lift_time.setWidth(Integer.parseInt(params.width));
                lift_time.setHeight(Integer.parseInt(params.height));
                break;
            case "title":
                lift_title.setX(Float.parseFloat(params.x));
                lift_title.setY(Float.parseFloat(params.y));
                lift_title.setWidth(Integer.parseInt(params.width));
                lift_title.setHeight(Integer.parseInt(params.height));
                break;
            case "video":
                setVideoFill(lift_video, Integer.parseInt(params.width), Integer.parseInt(params.height), Integer.parseInt(params.x),
                        Integer.parseInt(params.y));
                break;
        }
    }

    //
    private String upMediaDir = "Resource";
    public String downloadPATH = "resDownload/multimedia/";       //视频资源目录
    private String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

    private String lastPath = "";
    private String nowPath = "";


    /**
     * usb 升级文件配置信息
     *
     * @param event
     */
    public void onEventMainThread(final ConfigurationParams.Params event) {
        Gson gson = new Gson();
        Log.i(TAG, "ConfigurationParams: " + event.toString());
        if (event.type.equals("reset")) {
            if (event.info.equals("true")) {
                restoreDefault();
            }
        }
        if (event.type.equals("volume")) {
            params.volume = event.info;
            PrefUtils.setString(this, "JSON", gson.toJson(params));
            setSysVolume(Integer.parseInt(params.volume));
        }

        if (event.type.equals("brightness")) {
            params.brightness = event.info;
            PrefUtils.setString(this, "JSON", gson.toJson(params));
            setBrightness(Integer.parseInt(params.brightness));
        }
        if (event.type.equals("fullscreen")) {
            PrefUtils.setString(this, "fullscreen", event.info);
            String rType = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
//                Log.i(TAG, "onEventMainThread: "+event.info);

            if (rType.equals(Constants.WebUrl)) {
                setVideoFullscreen(lift_webview, event.info);
            } else {
                setVideoFullscreen(lift_video, event.info);
            }
            lift_title.setVisibility(View.VISIBLE);
            lift_time.setVisibility(View.VISIBLE);
            lift_date.setVisibility(View.VISIBLE);
            lift_scrollingText.setVisibility(View.VISIBLE);
            sText = false;
            titleText = false;
            mDataTime = false;

        }
        if (event.type.equals("scrollingarea")) {
            if (event.info.equals("true")) {
                setScrollTextShow("1");
            } else {
                setScrollTextShow("0");
            }
        }
        if (event.type.equals("titlearea")) {
            if (event.info.equals("true")) {
                setTitleShow("1");
            } else {
                setTitleShow("0");
            }
        }
        if (event.type.equals("timearea")) {
            if (event.info.equals("true")) {
                setDateTimeShow("1");
            } else {
                setDateTimeShow("0");
            }
        }

        if (event.type.equals("stageone")) {
            try {
                JSONObject json = new JSONObject(event.info);
                if (json.has("time")) {
                    String time = json.getString("time");
                    Log.i(TAG, "stageone: " + time);
                    params.stageFirst = time;
                    PrefUtils.setString(this, "JSON", gson.toJson(params));
                }
                if (json.has("brightness")) {
                    String brightness = json.getString("brightness");
                    params.standbyBrightness = brightness;
                    PrefUtils.setString(this, "JSON", gson.toJson(params));
                    Log.i(TAG, "stageone: " + brightness);
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (event.type.equals("stagetwo")) {
            try {
                JSONObject json = new JSONObject(event.info);
                if (json.has("time")) {
                    String time = json.getString("time");
                    Log.i(TAG, "stagetwo: " + time);
                    params.stageSecond = time;
                    PrefUtils.setString(this, "JSON", gson.toJson(params));
                }
                if (json.has("brightness")) {
                    String brightness = json.getString("brightness");
                    params.standbyBrightnessSecond = brightness;
                    PrefUtils.setString(this, "JSON", gson.toJson(params));
                    Log.i(TAG, "stagetwo: " + brightness);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (event.type.equals("time")) {
            Log.i(TAG, "time: " + event.info);
        }
        if (event.type.equals("data")) {
            Log.i(TAG, "data: " + event.info);
        }

        if (event.type.equals("title")) {
            PrefUtils.setString(this, "Title", event.info);
            lift_title.setText(event.info);
//            Intent intent = new Intent();
//            intent.setAction("com.jack.accessibility");
//
//            String path = basePath+downloadPATH+"MediaScreen.apk";
//            Log.i(TAG, "sendBroadcast: "+path);
//            String apkData[] = {path,"MediaScreen.apk"};
//
//            intent.putExtra("apk",apkData);
//            sendBroadcast(intent);
        }

        if (event.type.equals("scrollingtext")) {
            PrefUtils.setString(this, "Word", event.info);
            lift_scrollingText.setText(event.info);
        }
        if (event.type.equals("interval")) {
            Log.i(TAG, "onEventMainThread: " + event.info);
            imageRunTime = Integer.parseInt(event.info);
        }
        if (event.type.equals("picture")) {
            lift_webview.setVisibility(View.INVISIBLE);
            lift_video.setVisibility(View.VISIBLE);
            lift_video.setBackground(null);

            DeleteDirectory.delAllFile(basePath+ upMediaDir);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    String path = basePath + downloadPATH + event.info;
                    final File resFile = new File(path);
                    Log.i(TAG, "run: resource---" + path);
                    final String resFilePath = basePath+ upMediaDir +"/"+ event.info;
                    PrefUtils.setString(MediaScreenActivity.this, Constants.rType, event.type);
                    nowPath = event.info;
                    if(!lastPath.equals(nowPath)){
                        lastPath = nowPath;
                        imageHandler.removeMessages(0);
                    }

                    CopyUpdataFile.getInstance().copyFileToPath(path, basePath + upMediaDir, new CopyUpdataFile.CppyFileCallBack() {
                                @Override
                                public void startCopy() {
                                    Log.i(TAG, "CopyOver: startCopy");
                                }

                                @Override
                                public void copyCompleted(long size) {
                                    Log.i(TAG, "copyCompleted: " + size);
                                }

                                @Override
                                public void CopyOver(boolean over, String error) {
                                    Log.i(TAG, "CopyOver: CopyOver");
                                    if (!over) {
                                        Log.i(TAG, "CopyOver: 文件复制异常");
                                        return;
                                    } else {
                                        imageHandler.sendEmptyMessageDelayed(0, 1000 * 2);
                                        Log.i(TAG, "ConfigurationParams: picture"+event.info);
                                    }
                                }
                            }
                    );
                    resFile.delete();
                }
            }, 2000);

//            upAPK("");

        }
        if (event.type.equals("video")) {
            lift_webview.setVisibility(View.INVISIBLE);
            lift_video.setVisibility(View.VISIBLE);
            lift_video.setBackground(null);

            DeleteDirectory.delAllFile(basePath+ upMediaDir);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    String path = basePath + downloadPATH + event.info;
                    final File resFile = new File(path);
                    Log.i(TAG, "run: resource---" + path);
                    final String resFilePath = basePath+ upMediaDir +"/"+ event.info;

                    PrefUtils.setString(MediaScreenActivity.this, Constants.rType, event.type);
                    PrefUtils.setString(MediaScreenActivity.this, Constants.rPath, resFilePath);

                    CopyUpdataFile.getInstance().copyFileToPath(path, basePath + upMediaDir, new CopyUpdataFile.CppyFileCallBack() {
                                @Override
                                public void startCopy() {
                                    Log.i(TAG, "CopyOver: startCopy");
                                }

                                @Override
                                public void copyCompleted(long size) {
                                    Log.i(TAG, "copyCompleted: " + size);
                                }
                                @Override
                                public void CopyOver(boolean over, String error) {
                                    Log.i(TAG, "CopyOver: CopyOver");
                                    if (!over) {
                                        Log.i(TAG, "CopyOver: 文件复制异常");
                                        return;
                                    } else {
                                        textTipView.setText("");
                                        lift_video.setVisibility(View.VISIBLE);
                                        lift_video.setVideoPath(resFilePath);
                                        lift_webview.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }
                    );
                    resFile.delete();
                }
            }, 2000);
        }
    }

    Handler imageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (getImageList().size() > 0) {
                lift_webview.setVisibility(View.INVISIBLE);
                lift_video.setVisibility(View.VISIBLE);
                lift_video.setRunImageList(getImageList(), imageRunTime * 1000);
            }
        }
    };

    public void onEventMainThread(KONE_TransformProtocol.LiftInfo event) {
        if (crossScreenNow.equals("0")) {
            if (event.getFloor().length() == 3) {
                lift_floor.setTextSize(190);
            } else {
                lift_floor.setTextSize(230);
            }
        } else {
            if (event.getFloor().length() == 3) {
                lift_floor.setTextSize(190);
            } else {
                lift_floor.setTextSize(230);
            }
        }
        nowFloor = event.getFloor();  //电调当前所在楼层

        if (!nowFloor.equals(lastFloor)) {
            lastFloor = nowFloor;
            isFloorChange = 0;
            floorChange(false);
        } else {
            floorChange(true);
        }
        lift_floor.setText(event.getFloor());

        arrowRunDirection(event.getArrow());
//        setLiftFunction(2);
        if (newworkFlag) {
            setLiftFunction(event.getFunction());
        } else {
            if (event.getFunction() != 0) {
                setLiftFunction(event.getFunction());
            }
        }
    }

    /**
     * 判断楼层是否发生变化
     *
     * @param isChange true 电梯正在运行 false 电梯停止运行
     */
    private int isFloorChange = 0;

    private void floorChange(boolean isChange) {

        if (isFloorChange == 0) {
            if (isChange) {
                EnergySavingHandler.sendEmptyMessageDelayed(0, 1000 * Integer.parseInt(params.stageFirst));
                isFloorChange = 1;
            } else {
                isFloorChange = 0;

                setBrightness(Integer.parseInt(params.brightness));
                //清除节能模式
                EnergySavingHandler.removeMessages(0);
                secondEnergySavingHandler.removeMessages(0);
            }
        }
    }

    /**
     * 设置第一阶段屏保节能模式
     */
    Handler EnergySavingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            setBrightness(Integer.parseInt(params.standbyBrightness));  //设置第一阶段背光亮度
            //第一阶段节能模式开启之后，开始计时进入第二阶段屏保模式
            secondEnergySavingHandler.sendEmptyMessageDelayed(0, 1000 * Integer.parseInt(params.stageSecond));
        }
    };
    /**
     * 设置第二阶段屏保节能模式
     */
    Handler secondEnergySavingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //设置长时间未使用的屏保亮度
            setBrightness(Integer.parseInt(params.standbyBrightnessSecond));
        }
    };

    /**
     * 接收单片机协议，用于处理时间日期
     *
     * @param liftDateTime 时间类
     */
    public void onEventMainThread(KONE_TransformProtocol.liftDateTime liftDateTime) {
        String mYear = liftDateTime.mYear;
        String mMonth = liftDateTime.mMonth;
        String mDay = liftDateTime.mDay;
        String mHour = liftDateTime.mHour;
        String mMinute = liftDateTime.mMinute;
        String mSecond = liftDateTime.mSecond;
        lift_time.setText(mHour + ":" + mMinute);
        lift_date.setText(mDay + "." + mMonth + "." + mYear);

    }

    private void sendLayoutP(final LiftLayoutParams layoutParams, String i) {
        Log.i(TAG, "sendLayoutP----------: " + layoutParams.toString());
        ACache.get(this).put(layoutParams.type + i, layoutParams);
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                setLayoutParams(layoutParams);
            }
        }, 100);
    }

    private LiftLayoutParams setLayoutParams(JSONObject layout, String key, String type) {

        LiftLayoutParams layoutParams = new LiftLayoutParams();
        try {
            JSONObject object = new JSONObject(String.valueOf(layout.getJSONObject(key)));
            layoutParams.type = type;
            layoutParams.width = object.getString("width");
            layoutParams.height = object.getString("height");
            layoutParams.x = object.getString("x");
            layoutParams.y = object.getString("y");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return layoutParams;
    }

    /**
     * 更新设备端布局显示
     *
     * @param layout 所有布局坐标
     * @param index
     */
    private void updateLayoutView(JSONObject layout, int index) {
        Iterator<String> iterator = layout.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            switch (key) {
                case "Arrow":
                    LiftLayoutParams paramsArrow = setLayoutParams(layout, key, "arrow");
                    sendLayoutP(paramsArrow, String.valueOf(index));
                    break;
                case "Date":
                    LiftLayoutParams paramsDate = setLayoutParams(layout, key, "date");
                    sendLayoutP(paramsDate, String.valueOf(index));
                    break;
                case "Floor":
                    LiftLayoutParams paramsFloor = setLayoutParams(layout, key, "floor");
                    sendLayoutP(paramsFloor, String.valueOf(index));
                    break;
                case "Function":
                    LiftLayoutParams paramsFunction = setLayoutParams(layout, key, "function");
                    sendLayoutP(paramsFunction, String.valueOf(index));
                    break;
                case "Text":
                    LiftLayoutParams paramsText = setLayoutParams(layout, key, "text");
                    sendLayoutP(paramsText, String.valueOf(index));
                    break;
                case "Time":
                    LiftLayoutParams paramsTime = setLayoutParams(layout, key, "time");
                    sendLayoutP(paramsTime, String.valueOf(index));
                    break;
                case "Title":
                    LiftLayoutParams paramsTitle = setLayoutParams(layout, key, "title");
                    sendLayoutP(paramsTitle, String.valueOf(index));
                    break;
                case "Video":
                    LiftLayoutParams paramsVideo = setLayoutParams(layout, key, "video");
                    sendLayoutP(paramsVideo, String.valueOf(index));
                    break;
            }
        }
    }
    /**
     * 接收处理IOT 端发送的所有数据进行解析处理
     *
     * @param event 消息体
     */
    public void onEventMainThread(final MqttService.MqttInfo event) {
//        Log.i(TAG, "onEventMainThread: " + event.toString());
        final Gson gson = new Gson();
        switch (event.type) {
            case MqttService._layout:
                if (event.info.equals("0") || event.info.equals("1")) {
                    crossScreenNow = event.info;
                    PrefUtils.setString(this, "crossScreen", event.info);
                    setOrientationInit(event.info);
                } else {
                    try {
                        org.json.JSONObject jsonObject = new org.json.JSONObject(event.info);
                        if (jsonObject.has("layoutH") && jsonObject.has("layoutV")) {
                            if (crossScreenNow.equals("0")) {
                                JSONObject layout_h = jsonObject.getJSONObject("layoutH");
                                updateLayoutView(layout_h, 0);
                            } else {
                                JSONObject layout_v = jsonObject.getJSONObject("layoutV");
                                updateLayoutView(layout_v, 1);
                            }
                        } else {
                            LiftLayoutParams layoutParams = gson.fromJson(event.info, LiftLayoutParams.class);
                            if (crossScreenNow.equals("0")) {
                                sendLayoutP(layoutParams, "0");
                            } else {
                                sendLayoutP(layoutParams, "1");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MqttService._displaytype:
                crossScreenNow = event.info;
                PrefUtils.setString(this, "crossScreen", event.info);
                setOrientationInit(event.info);
                break;
            case MqttService._fullscreen:
                PrefUtils.setString(this, "fullscreen", event.info);
                String rType = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
//                Log.i(TAG, "onEventMainThread: "+event.info);

                if (rType.equals(Constants.WebUrl)) {
                    setVideoFullscreen(lift_webview, event.info);
                } else {
                    setVideoFullscreen(lift_video, event.info);
                }
                lift_title.setVisibility(View.VISIBLE);
                lift_time.setVisibility(View.VISIBLE);
                lift_date.setVisibility(View.VISIBLE);
                lift_scrollingText.setVisibility(View.VISIBLE);
                sText = false;
                titleText = false;
                mDataTime = false;

                break;
            case MqttService._screentime:
                params.stageFirst = event.info;
                PrefUtils.setString(this, "JSON", gson.toJson(params));
                setScreenOffTime(Integer.parseInt(params.stageFirst), 0);
                break;
            case MqttService._sybrightness:
                params.standbyBrightness = event.info;
                PrefUtils.setString(this, "JSON", gson.toJson(params));
                setScreenOffTime(Integer.parseInt(params.standbyBrightness), 0);
                break;
            case MqttService._brightness:
                params.brightness = event.info;
                PrefUtils.setString(this, "JSON", gson.toJson(params));
                setBrightness(Integer.parseInt(params.brightness));
                break;
            case MqttService._volume:
                params.volume = event.info;
                PrefUtils.setString(this, "JSON", gson.toJson(params));
                setSysVolume(Integer.parseInt(params.volume));
                break;
            case MqttService.TEXT_TITLE:
                PrefUtils.setString(this, "Title", event.info);
                lift_title.setText(event.info);
                break;
            case MqttService.TEXT_SCROL:
                PrefUtils.setString(this, "Word", event.info);
                lift_scrollingText.setText(event.info);
                break;
            case MqttService.web_url:

                PrefUtils.setString(MediaScreenActivity.this, Constants.rType, Constants.WebUrl);
                PrefUtils.setString(MediaScreenActivity.this, Constants.WebUrl, event.info);
                lift_webview.loadUrl(event.info);
                break;
            case "progress":
                textTipView.setText(event.info);
                break;
            case "started":

                break;
            case "completed":

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final MqttService.CompletedResource resource = gson.fromJson(event.info, MqttService.CompletedResource.class);

                        final File resFile = new File(resource.rPath);
//                        Log.i(TAG, "run: resFilePath---" + resource.rPath);
                        final String resFilePath = Constants.ThemePath + MqttService.resourcePATH + resFile.getName();
//                        Log.i(TAG, "run: resFilePath---" + resFilePath + "   " + resource.rType);
                        if (!resource.rType.equals("APK")) {
                            PrefUtils.setString(MediaScreenActivity.this, Constants.rType, resource.rType);
                            PrefUtils.setString(MediaScreenActivity.this, Constants.rPath, resFilePath);
                        }
                        CopyUpdataFile.getInstance().copyFileToPath(resource.rPath, Constants.ThemePath + MqttService.resourcePATH, new CopyUpdataFile.CppyFileCallBack() {
                            @Override
                            public void startCopy() {
                                Log.i(TAG, "CopyOver: 开始复制文件");
                            }

                            @Override
                            public void copyCompleted(long size) {
                                Log.i(TAG, "copyCompleted: " + size);
                            }

                            @Override
                            public void CopyOver(boolean over, String error) {
                                Log.i(TAG, "CopyOver: 文件复制完成");
                                if (!over) {
                                    Log.i(TAG, "CopyOver: 文件复制异常");
                                    return;
                                } else {
                                    switch (resource.rType) {
                                        case MqttService.PICTURES:
                                            textTipView.setText("");
                                            Log.i(TAG, "completed  run: " + event.info);
                                            lift_webview.setVisibility(View.INVISIBLE);
                                            lift_video.setVisibility(View.VISIBLE);
                                            imageHandler.removeMessages(0);
                                            imageHandler.sendEmptyMessageDelayed(0, 1000 * 2);
                                            break;
                                        case MqttService.VIDEO:
                                            textTipView.setText("");
//                                setVideoPlay(event.info, sdCardVideo().size());
//                                            Log.i(TAG, "completed  run: " + resFilePath);
                                            lift_video.setVisibility(View.VISIBLE);
                                            lift_video.setVideoPath(resFilePath);
                                            lift_webview.setVisibility(View.INVISIBLE);
                                            break;
                                        case MqttService.AUDIO:
                                            textTipView.setText("");
                                            lift_video.setVisibility(View.VISIBLE);
                                            lift_webview.setVisibility(View.INVISIBLE);
                                            break;
                                        case "APK":
                                            textTipView.setText("");
                                            upAPK(resFilePath);
                                            break;
                                    }
                                    resFile.delete();
                                }
                            }
                        });
                    }
                }, 2000);

                break;
            case "Network":
                if (event.info.equals("false")) {
                    setLiftFunction(6);
                    newworkFlag = false;
                } else {
                    newworkFlag = true;
                    try {
                        binder.sendSystemDate(true);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case MqttService._reset:
                if (event.info.equals("1")) {
                    textTipView.setText("");
                    restoreDefault();
                }
                break;
            case MqttService._date:
                setDateTimeShow(event.info);
                break;
            case MqttService._webTitle:
                setTitleShow(event.info);
                break;
            case MqttService._webscrolltext:
                setScrollTextShow(event.info);
                break;
            case "imageDownImage":
                int i = Integer.parseInt(event.info);
                if (i > 1) {
                    isImagePlayType = true;
                } else {
                    isImagePlayType = false;
                }
                break;
        }
    }

    //获取当前本图片路径
    private List<String> getImageList() {
        String path = Constants.ThemePath + MqttService.resourcePATH;
        File file = new File(path);
        List<String> mPathList = new ArrayList<String>();
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File mFrameFile : files) {

                String fileName = mFrameFile.getName();
                String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (prefix.equals("jpg") || prefix.equals("png") || prefix.equals("bmp")) {
                    Log.i(TAG, "getImageList: " + mFrameFile.getAbsolutePath());
                    mPathList.add(mFrameFile.getAbsolutePath());
                }

            }
        }
        return mPathList;
    }

    /**
     * 设置时间日期显示
     *
     * @param data 判断显示参数
     */
    private void setDateTimeShow(String data) {
        Log.i(TAG, "设置时间日期显示: " + data);
        if (data.equals("0")) {
            mDataTime = true;
            lift_time.setVisibility(View.INVISIBLE);
            lift_date.setVisibility(View.INVISIBLE);
            if (titleText && sText) {
                //设置左半边视频全屏显示
                setFillVideoShow();
            } else if (titleText && !sText) {
                //设置视频上半边显示完全
                setTopVideoShow();
            }
        } else {
            mDataTime = false;
            lift_time.setVisibility(View.VISIBLE);
            lift_date.setVisibility(View.VISIBLE);
            if (sText) {
                //下半边显示完全
                setBottomVideoShow();
            } else {
                //显示正常
                setTrueVideoShow();
            }
        }
    }

    /**
     * 设置标题文字显示
     *
     * @param data 显示参数
     */
    private void setTitleShow(String data) {

        if (data.equals("0")) {
            //标题文字不显示
            titleText = true;
            lift_title.setVisibility(View.INVISIBLE);
            //在标题不显示的情况下，判断日期和滚动文字是否都在显示
            if (mDataTime && sText) {
                //设置左半边视频全屏显示
                setFillVideoShow();
            } else if (mDataTime && !sText) {
                //设置视频上半边显示完全
                setTopVideoShow();
            }
        } else {
            titleText = false;
            lift_title.setVisibility(View.VISIBLE);
            if (sText) {
                //下半边显示完全
                setBottomVideoShow();
            } else {
                //显示正常
                setTrueVideoShow();
            }
        }
    }

    /**
     * 设置视频全显示
     */
    private void setFillVideoShow() {
        Log.i(TAG, "setFillVideoShow: " + fillScreen);
        String rTypesShow = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        String fullscreenShow = PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen);
        if (rTypesShow.equals(Constants.WebUrl)) {
            setVideoAllScreen(lift_webview, fullscreenShow);
        } else {
            setVideoAllScreen(lift_video, fullscreenShow);
        }
    }

    /**
     * 设置视频上半边显示
     */
    private void setTopVideoShow() {
        Log.i(TAG, "setTopVideoShow: " + fillScreen);
        String rTypesShow = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        String fullscreenShow = PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen);
        if (rTypesShow.equals(Constants.WebUrl)) {
            setVideoTopScreen(lift_webview, fullscreenShow);
        } else {
            setVideoTopScreen(lift_video, fullscreenShow);
        }
    }

    /**
     * 设置视频下半边显示完全
     */
    private void setBottomVideoShow() {
        Log.i(TAG, "setBottomVideoShow: " + fillScreen);
        String rTypesShow = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        String fullscreenShow = PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen);
        if (rTypesShow.equals(Constants.WebUrl)) {
            setVideoHalfScreen(lift_webview, fullscreenShow);
        } else {
            setVideoHalfScreen(lift_video, fullscreenShow);
        }
    }

    /**
     * 设置视频正常显示
     */
    private void setTrueVideoShow() {
        Log.i(TAG, "setBottomVideoShow: " + fillScreen);
        String rTypesShow = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        String fullscreenShow = PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen);
        if (rTypesShow.equals(Constants.WebUrl)) {
            setVideoFullscreen(lift_webview, fullscreenShow);
        } else {
            setVideoFullscreen(lift_video, fullscreenShow);
        }
    }

    /**
     * 设置滚动文字是否显示
     *
     * @param data 判断显示参数
     */
    private void setScrollTextShow(String data) {
        String rTypesShow = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        String fullscreenShow = PrefUtils.getString(MediaScreenActivity.this, "fullscreen", fillScreen);
        if (data.equals("0")) {
            sText = true;
            lift_scrollingText.setVisibility(View.INVISIBLE);
            //当标题和日期都没有显示的时候
            if (titleText && mDataTime) {
                if (rTypesShow.equals(Constants.WebUrl)) {
                    //右半边视频全屏显示
                    setVideoAllScreen(lift_webview, fullscreenShow);
                } else {
                    //右半边视频全屏显示
                    setVideoAllScreen(lift_video, fullscreenShow);
                }
            } else {
                //否则视频区域下半边显示完全
                if (rTypesShow.equals(Constants.WebUrl)) {
                    setVideoHalfScreen(lift_webview, fullscreenShow);
                } else {
                    setVideoHalfScreen(lift_video, fullscreenShow);
                }
            }
        } else {
            //当滚动文字显示的时候
            sText = false;
            lift_scrollingText.setVisibility(View.VISIBLE);
            //当标题和日期都没有显示的时候
            if (mDataTime && titleText) {
                setTopVideoShow();
            } else {
                setTrueVideoShow();
            }
        }
    }

    boolean sText = false;          // 判断滚动文字是否显示
    boolean titleText = false;      // 判断标题文字是否显示
    boolean mDataTime = false;      // 判断时间日期是否显示

    /**
     * 视频下半边全屏
     *
     * @param view
     * @param info
     */
    private void setVideoHalfScreen(View view, String info) {
        if (crossScreenNow.equals("0")) {
            if (info.equals("1")) {
                setVideoFill(view, 500, 508, 300, 108);
            }
        } else if (crossScreenNow.equals("1")) {
            if (info.equals("1")) {
                setVideoFill(view, 768, 660, 0, 383);
            }
        }
    }

    /**
     * 视频顶部
     *
     * @param view view
     * @param info info
     */
    private void setVideoTopScreen(View view, String info) {
        Log.i(TAG, "setVideoTopScreen: crossScreenNow  " + crossScreenNow);
        if (crossScreenNow.equals("0")) {
            if (info.equals("1")) {
                setVideoFill(view, 500, 488, 300, 0);
            }
        }
    }

    /**
     * 设置视频上全屏
     *
     * @param view view
     * @param info 显示方向
     */
    private void setVideoAllScreen(View view, String info) {
        Log.i(TAG, "setVideoAllScreen: crossScreenNow  " + crossScreenNow);
        if (crossScreenNow.equals("0")) {
            if (info.equals("1")) {
                setVideoFill(view, 500, 600, 300, 0);
            }
        }
    }


    /**
     * 打开文件
     */
    private void openFile(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //设置intent的Action属性
        intent.setAction(Intent.ACTION_VIEW);
        //获取文件file的MIME类型
        String type = getMIMEType(file);
        //设置intent的data和Type属性。
        intent.setDataAndType(/*ur i*/Uri.fromFile(file), type);
        //跳转
        startActivity(intent);
    }

    /**
     * 根据文件后缀名获得对应的MIME类型。
     */
    private String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
   /* 获取文件的后缀名*/
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private String[][] MIME_MapTable = {
            //{后缀名，MIME类型}
            {".apk", "application/vnd.android.package-archive"},
            {"", "*/*"}
    };

    /**
     * 一键恢复默认
     */
    private void restoreDefault() {
        if (sText) {
            lift_scrollingText.setVisibility(View.VISIBLE);
        }
        if (titleText) {
            lift_title.setVisibility(View.VISIBLE);
        }
        if (mDataTime) {
            lift_time.setVisibility(View.VISIBLE);
            lift_date.setVisibility(View.VISIBLE);
        }
        lift_webview.setVisibility(View.INVISIBLE);
        lift_video.setVisibility(View.VISIBLE);
        PrefUtils.setString(MediaScreenActivity.this, Constants.rType, Constants.VIDEO);
        PrefUtils.setString(MediaScreenActivity.this, Constants.rPath, Constants.DefaultPath);
        PrefUtils.setString(MediaScreenActivity.this, "fullscreen", "1");
        PrefUtils.setString(MediaScreenActivity.this, "crossScreen", Constants.CrossScreen);
//        setVideoFullscreen(lift_video, "1");
        crossScreenNow = PrefUtils.getString(this, "crossScreen", Constants.CrossScreen);
        setOrientation(crossScreenNow);
        PrefUtils.setString(this, "Title", title);
        PrefUtils.setString(this, "Word", scroll);

        lift_video.setVideoPath(Constants.DefaultPath);
        lift_title.setText(title);
        lift_scrollingText.setText(scroll);

        File restoreFile = new File(Constants.ThemePath + Constants.RESOURCE_FILE);
        boolean delete = DeleteDirectory.deleteDir(restoreFile);
        if (delete) {

        }
    }

    /**
     * 设置布局配置信息
     */
    private void paramsData() {
        List<LiftLayoutParams> layoutList = getLayoutList();
        for (int i = 0; i < layoutList.size(); i++) {
            setLayoutParams(layoutList.get(i));
        }
    }

    private void initLayout() {
        List<LiftLayoutParams> layoutList = getLayoutList();
        for (int i = 0; i < layoutList.size(); i++) {
            LiftLayoutParams ss = null;
            if (crossScreenNow.equals("0")) {
                ss = (LiftLayoutParams) ACache.get(MediaScreenActivity.this).getAsObject(layoutList.get(i).type + "0");
            } else {
                ss = (LiftLayoutParams) ACache.get(MediaScreenActivity.this).getAsObject(layoutList.get(i).type + "1");
            }
            if (ss == null) {
                setLayoutParams(layoutList.get(i));
            } else {
                setLayoutParams(ss);
            }
        }
    }

    private void setVideoFullscreen(View view, String info) {
//        Log.i(TAG, "setVideoFullscreen: " + info);
        if (crossScreenNow.equals("0")) {
            if (info.equals("0")) {
                setVideoFill(view, 800, 600, 0, 0);
            }
            if (info.equals("1")) {
                setVideoFill(view, 500, 380, 300, 108);
            }
        }
        if (crossScreenNow.equals("1")) {
            if (info.equals("0")) {
                setVideoFill(view, 600, 800, 0, 0);
            }
            if (info.equals("1")) {
                setVideoFill(view, 600, 365, 0, 372);
            }
        }
    }

    /**
     * 屏幕旋转
     *
     * @param orientation 方向
     * @return true
     */
    boolean setOrientation(String orientation) {
        Log.i(TAG, "setOrientation: ture " + orientation);
        String rType = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);

        if (orientation.equals("1")) {
            initLayout();
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        if (orientation.equals("0")) {
            initLayout();
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        if (rType.equals(Constants.WebUrl)) {
            setVideoFullscreen(lift_webview, PrefUtils.getString(this, "fullscreen", fillScreen));
        } else {
            setVideoFullscreen(lift_video, PrefUtils.getString(this, "fullscreen", fillScreen));
        }
        return true;
    }

    boolean setOrientationInit(String orientation) {
        Log.i(TAG, "setOrientationInit: ture " + orientation);
        String rType = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);
        ACache.get(this).clear();
        if (orientation.equals("1")) {
            paramsData();
//            setVideoFullscreen(lift_video,PrefUtils.getString(this, "fullscreen", fillScreen));
//            Log.i(TAG, "setOrientation: ----------setView_WHXY_V--------");
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        if (orientation.equals("0")) {
            paramsData();
//            setVideoFullscreen(lift_video,PrefUtils.getString(this, "fullscreen", fillScreen));
//            Log.i(TAG, "setOrientation: ---------setView_WHXY_H---------");
            if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        if (rType.equals(Constants.WebUrl)) {
            setVideoFullscreen(lift_webview, PrefUtils.getString(this, "fullscreen", fillScreen));
        } else {
            setVideoFullscreen(lift_video, PrefUtils.getString(this, "fullscreen", fillScreen));
        }
        return true;
    }

    /**
     * 设置背光时间  毫秒
     *
     * @param paramInt 背光时间
     * @param dark     背光暗下去的亮度
     */
    public void setScreenOffTime(int paramInt, int dark) {
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, (paramInt / 1000));
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    private void mLog(String data) {
        Log.i(TAG, "---->: " + data);
    }

    /**
     * 设置背光亮度
     *
     * @param brightness 亮度
     */
    public void setBrightness(int brightness) {
        if (brightness <= 0) {
            brightness = 1;
        }
        if (brightness > 255) {
            brightness = 255;
        }
        int bri = (int) Math.ceil((brightness * 255) / 100);
        mLog(String.valueOf(bri) + "  " + brightness);
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, bri);
    }

    // 设置系统音量
    void setSysVolume(int index) {
        if (index == 0) {
            index = 1;
        }
        int i = (int) Math.ceil((index / 5));

        AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, i, AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * 获得锁屏时间  毫秒
     */
    private int getScreenOffTime() {
        int screenOffTime = 0;
        try {
            screenOffTime = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Exception localException) {

        }
        return screenOffTime;
    }

    /**
     * 获取屏幕的亮度
     */
    public int getScreenBrightness() {
        int value = 0;
        ContentResolver cr = MediaScreenActivity.this.getContentResolver();
        try {
            value = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
        }
        return value;
    }

    /**
     * 获取手机声音
     *
     * @return
     */
    private int getAudioManager() {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return max;
    }

    // 初始化本地音量 亮度背光 横竖显示 视频全屏等参数信息
    public void getParamsData() {
        params.volume = String.valueOf(getAudioManager());
        params.brightness = String.valueOf(100);
        params.stageFirst = "90";                //屏保时间间隔
        params.standbyBrightness = String.valueOf(20);
        params.standbyBrightnessSecond = "0";
        params.isCrossScreen = crossScreenNow;
        params.isFillScreen = fillScreen;
        params.stageSecond = "1200";    //设置第二阶段屏保亮度间隔时间
    }

    private void setVideoFill(View view, int w, int h, int x, int y) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(w, h);
        view.setX(x);
        view.setY(y);
        view.setLayoutParams(layoutParams);         //将宽高信息设置到控件中
    }

    /**
     * 设置功能图标显示
     *
     * @param image index
     */
    int imageIndex = -1;

    void setLiftFunction(int image) {

        if (imageIndex != image) {

            lift_function.setImageDrawable(getResources().getDrawable(functionImage[image]));
            imageIndex = image;
        }
//        Log.i(TAG, "setLiftFunction: "+image);
    }

    /**
     * 初始化电梯控制柜信号服务
     */
    public void initDataService() {
        Intent deviceIntent = new Intent(MediaScreenActivity.this, KONE_TransformProtocol.class);
        bindService(deviceIntent, connection, BIND_AUTO_CREATE);
    }

    //连接service
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (KONE_TransformProtocol.LocalBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    /**
     * 判断电梯运行方向
     *
     * @param info 1 箭头上行  2 箭头下行
     */
    public void arrowRunDirection(int info) {

        if (arrowFlag != info) {
            if (info == 0) {
                lift_floor_arrow.setImageDrawable(noneArrow);
            }
            //箭头上行
            if (info == 1 || info == 3) {
                lift_floor_arrow.setImageDrawable(upArrow);
            }
            //箭头下行
            if (info == 2 || info == 4) {
                lift_floor_arrow.setImageDrawable(downArrow);
            }
            arrowFlag = info;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 检测屏幕的方向：纵向或横向
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 默认播放视频文件
        lift_video.setHolderView();

        String rType = PrefUtils.getString(this, Constants.rType, Constants.VIDEO);

        if (rType.equals(Constants.VIDEO)) {
            lift_video.setVideoPath(PrefUtils.getString(this, Constants.rPath, Constants.DefaultPath));
        } else if (rType.equals(Constants.PICTURES)) {
            imageHandler.removeMessages(0);
            imageHandler.sendEmptyMessageDelayed(0, 1000 * 2);
        } else if (rType.equals(Constants.WebUrl)) {
            if (NetUtil.getNetWork()) {
                lift_webview.loadUrl(PrefUtils.getString(this, Constants.WebUrl, ""));
            } else {
                lift_video.setVisibility(View.VISIBLE);
                lift_webview.setVisibility(View.INVISIBLE);
                lift_video.setVideoPath(Constants.DefaultPath);
            }
        }

        // 默认旋转方向
        crossScreenNow = PrefUtils.getString(this, "crossScreen", Constants.CrossScreen);
        setOrientation(crossScreenNow);
        getParamsData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        newworkFlag = false;
        lift_video.stopSurfacView();
        unbindService(connection);
    }
    /**
     * Web请求参数
     */
    private class Params {
        public String layout; // 布局文件
        public String volume; // 音量
        public String brightness; // 屏保亮度
        public String standbyBrightness;   // 背光亮度
        public String standbyBrightnessSecond;   // 背光亮度
        public String stageFirst;    // 第一阶段屏保时间
        public String stageSecond;         // 第二阶段屏保时间
        public String isFillScreen;  // 视频全屏    0 全屏 1 半
        public String isCrossScreen; // 横竖显示   0 横显  1 竖显
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    Intent intent = new Intent();
    private void upAPK(String filePath) {

        if(intent == null){intent = new Intent();}
        Log.i(TAG, "upAPK: 发送广播升级apk");
        intent.setAction("zhouwc.example.com.apkupdatedemo");

        String path = basePath + downloadPATH + "MediaScreen.apk";
        intent.putExtra("apk", path);
        sendBroadcast(intent);

    }
}
