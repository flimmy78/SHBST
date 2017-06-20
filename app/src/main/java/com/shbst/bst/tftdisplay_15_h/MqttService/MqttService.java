package com.shbst.bst.tftdisplay_15_h.MqttService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.shbst.androiddevicesdk.DeviceSDK;
import com.shbst.androiddevicesdk.beans.Constants;
import com.shbst.androiddevicesdk.beans.MediaEvent;
import com.shbst.androiddevicesdk.beans.TalkEvent;
import com.shbst.androiddevicesdk.listener.FileDownloadAdapter;
import com.shbst.androiddevicesdk.utils.NetworkUtils;
import com.shbst.androiddevicesdk.utils.ShellUtils;
import com.shbst.androiddevicesdk.widget.DeviceSDKAdapter;
import com.shbst.androiddevicesdk.widget.MqttAdapter;
import com.shbst.androiddevicesdk.widget.VisualIntercomAdapter;
import com.shbst.bst.tftdisplay_15_h.MainActivity;
import com.shbst.bst.tftdisplay_15_h.MqttService.Bean.LiftLayoutParams;
import com.shbst.bst.tftdisplay_15_h.MqttService.Bean.MqttParamsBean;
import com.shbst.bst.tftdisplay_15_h.utils.DeviceUtils;
import com.shbst.bst.tftdisplay_15_h.utils.PrefUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 *
 * Created by hegang on 2016-12-08.
 */
public class MqttService {
    private static final String TAG = "MqttServiceData";
    private static final int TIME_OUT = 6;
//    private static String MAC = "17:05:22:10:vv:01";
//    private static String MAC = "17:05:22:01:10:VV";
//    private static String MAC = "17:05:22:02:10:HH";

    private static String MAC = "17:06:06:04:bo:xh";
//    private static String MAC = "17:06:06:03:CS:2V";
    public static final String _pending = "_pending";    // 连接iot

    public static final String _layout = "layout";  //布局文件
    public static final String _displaytype = "displaytype";  // 布局显示方向
    public static final String _fullscreen = "fullscreen";    // 视频全屏控制
    public static final String _screentime = "screentime";    // 屏保时间
    public static final String _sybrightness = "sybrightness";// 背光亮度
    public static final String _brightness = "brightness";    // 背光
    public static final String _volume = "volume";            // 背景音亮
    public static final String _reset = "reset";            // 一键恢复默认
    public static final String _webTitle = "title";            //标题文字
    public static final String _webscrolltext = "scrolltext";            // 滚动字幕
    public static final String _date = "date";            //日期时间
    public static final String _imageinterval="imageinterval";   //图片轮播间隔时间 单位：秒
    public static final String _datemode = "datemode";           //日期格式
    public static final String _timemode = "timemode";           //时间格式



    public static final String themePath = Environment.getExternalStorageDirectory().getAbsolutePath(); // 资源文件根目录
    public static final String ARROW_PATH = "/arrow/";      //箭头资源文件目录
    public static final String DESKTOP_PATH = "/desktop/";  // 背景资源目录
    public static final String FUNCTION_PATH = "/function/"; // 功能图片资源目录

    public static final String PICTURE_PTTH = "/Resource/";   //图片资源目录
    public static final String AUDIO_PTTH = "/music/";   //图片资源目录
    public static final String resourcePATH = "/Resource/";       //视频资源目录
    public static final String downloadPATH = "/resDownload/";       //视频资源目录

    public static final String PICTURES = "picture";
    public static final String VIDEO = "video";
    public static final String AUDIO = "audio";
    public static final String TEXT = "text";

    public static final String TEXT_TITLE = "titles";
    public static final String TEXT_SCROL = "scrolltexts";
    public static final String web_url = "url";
    public static final String TEXT_SUB = "subtitle";

    MqttParamsBean mqttParamsBean = MqttParamsBean.getMqttParamsBean();

    int index = 0;
    private static final String PRODUCT_ID = //"57ad2e07e3484223ba60e128c51762a2";    //无线网络控制器
            "c81c3a1a628f4608aa9722fc74ddb028";     //贝斯特测试产品
    Context mContext;
    IntentFilter filter;
    EventBus mqttBus = EventBus.getDefault();
    MqttUtils mqttUtils;
    public static MqttService mqtt_client = null;
    String iotResourceType = "";

    private enum handler_case {
        handler_receive_mqtt_message,
        handler_connect_to_m2m,
        handler_disconnect_from_m2m,
        handler_send_mqtt_messaage,
        handler_receive_server_message,
        handler_sign_up_success,
        handler_sign_up_error,
        handler_sign_up_timeout,
        handler_get_m2m_server_success,
        handler_get_m2m_server_error,
        handler_get_m2m_server_timeout
    }

    public MqttService(Activity context) {
        mContext = context;
        mqttUtils = new MqttUtils(context);
        mqttParamsBean.sybrightness = "20";
        mqttParamsBean.volume = String.valueOf(mqttUtils.getAudioManager());
        mqttParamsBean.screentime = String.valueOf(mqttUtils.getScreenOffTime());
        mqttParamsBean.displaytype = PrefUtils.getString(context, "crossScreen", com.shbst.bst.tftdisplay_15_h.utils.Constants.CrossScreen);
        mqttParamsBean.fullscreen = PrefUtils.getString(context, "fullscreen", "0");
        mqttParamsBean.brightness = String.valueOf(mqttUtils.getScreenBrightness());
        mqttParamsBean.mDate = 1;
        mqttParamsBean.mText = 1;
        mqttParamsBean.mScrolltext = 1;
        initEvent();
    }

    public static MqttService Open_IOT_Client(Activity context) {
        if (mqtt_client == null) {
            mqtt_client = new MqttService(context);
            MAC = DeviceUtils.getMac();
        }
        return mqtt_client;
    }
    private void initEvent() {
        try{
            filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(receiver, filter); //注册网络监听服务
            DeviceSDK.getInstance().setDeviceSDKListener(deviceSDKAdapter); //注册回调接口
            DeviceSDK.getInstance().setMqttListener(mqttAdapter);
            DeviceSDK.getInstance().setVisualIntercomListener(visualIntercomAdapter);
            DeviceSDK.getInstance().setFileDownloadAdapter(new FileDownloadAdapter() {
                @Override
                public void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    mqttBus.post(new MqttInfo("pending", "start"));
                }

                @Override
                public void started(final BaseDownloadTask task) {
                    mqttBus.post(new MqttInfo("started", "started"));
                }

                @Override
                public void connected(final BaseDownloadTask task, boolean isContinue, int soFarBytes, int totalBytes) {
                    Log.d(TAG, "connected -> " + task.getTag());
                    mqttBus.post(new MqttInfo("connected", "connected"));
                }
                @Override
                public void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    int progress = (int) (((float) soFarBytes / (float) totalBytes) * 100);
                    Log.i(TAG, "progress: " + progress);
                    mqttBus.post(new MqttInfo("progress", progress + "%"));
                }

                @Override
                public void blockComplete(final BaseDownloadTask task) throws Throwable {
                    Log.d(TAG, "blockComplete -> " + task.getTag());
                }

                @Override
                public void retry(final BaseDownloadTask task, Throwable ex, int retryingTimes, int soFarBytes) {
                    Log.d(TAG, "download retry -> " + task.getTag());
                }

                @Override
                public void completed(final BaseDownloadTask task) {
                    Gson gson = new Gson();
                    CompletedResource resource = new CompletedResource(iotResourceType, task.getPath());
                    Log.d(TAG, "download completed -> " + resource.toString());
                    mqttBus.post(new MqttInfo("completed", gson.toJson(resource)));
                }

                @Override
                public void paused(final BaseDownloadTask task, int soFarBytes, int totalBytes) {
                    Log.d(TAG, "download paused -> " + task.getTag());
                }

                @Override
                public void error(final BaseDownloadTask task, final Throwable e) {
                    Log.d(TAG, "download error -> " + task.getTag());
                    Log.d(TAG, "download error getMessage-> " +e.getMessage());
                }

                @Override
                public void warn(final BaseDownloadTask task) {
                    Log.d(TAG, "download warn -> " + task.getTag());
                }
            });
            //registDevice();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "run: " + isAvailableByPing(mContext));
                    while (!isAvailableByPing(mContext)) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {

                        }
                    }
                    registerDevice();
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            if (!isAvailableByPing(mContext)) {
//                        Toast.makeText(mContext,"无网路连接",Toast.LENGTH_SHORT).show();
                                mqttBus.post(new MqttInfo("Network", "false"));
//                            upstateText("Network ------------false");
                            } else {
//                        Toast.makeText(mContext,"网路连接",Toast.LENGTH_SHORT).show();
                                mqttBus.post(new MqttInfo("Network", "true"));
                                //registDevice();
                                DeviceSDK.getInstance().downloadRemainTasks();
//                            upstateText("Network ------------true");
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }).start();
        }catch (Exception e)
        {
        }
    }

    public static boolean isAvailableByPing(Context var0) {
        ShellUtils.CommandResult var1 = ShellUtils.execCmd("ping -c 1 -w 1 kone.drop-beats.com", false);
        boolean var2 = var1.result == 0;
        if (var1.errorMsg != null) {
            //LogUtils.d("isAvailableByPing errorMsg", var1.errorMsg);
        }
        if (var1.successMsg != null) {
            // LogUtils.d("isAvailableByPing successMsg", var1.successMsg);
        }
        return var2;
    }

    //正在注册设备信息
    private void registerDevice() {
        // Toast.makeText(mContext,"正在注册设备信息",Toast.LENGTH_SHORT).show();
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                upstateText("正在注册设备信息...");
                mHandler.sendEmptyMessageDelayed(handler_case.handler_sign_up_timeout.ordinal(), TIME_OUT * 1000);
                DeviceSDK.getInstance().deviceSignUp(PRODUCT_ID, MAC);
            }
        });
    }

    VisualIntercomAdapter visualIntercomAdapter = new VisualIntercomAdapter() {
        @Override
        public void onTalkPrepared() {
        }

        @Override
        public void onTalkStatusChange(TalkEvent event) {
            super.onTalkStatusChange(event);
        }

        @Override
        public void onMediaStatusChange(MediaEvent event) {
            super.onMediaStatusChange(event);
        }

        @Override
        public void onChannelError(final String description) {
        }
        @Override
        public void onChannelClosed(final String description) {
        }
    };

    DeviceSDKAdapter deviceSDKAdapter = new DeviceSDKAdapter() {
        @Override
        public void onDeviceSignUp(int resultCode, String resultMessage) {
            mHandler.removeMessages(handler_case.handler_sign_up_timeout.ordinal());
            if (Constants.Result_Success == resultCode) {
                Message message = new Message();
                message.what = handler_case.handler_sign_up_success.ordinal();
                mHandler.sendMessage(message);
            } else {
                Message message = new Message();
                message.what = handler_case.handler_sign_up_error.ordinal();
                message.obj = resultMessage;
                mHandler.sendMessage(message);
            }
        }
        @Override
        public void onGetM2MServer(int resultCode, String resultMessage) {
            mHandler.removeMessages(handler_case.handler_get_m2m_server_timeout.ordinal());
            //Toast.makeText(mContext,"onGetM2MServer",Toast.LENGTH_SHORT).show();
            if (Constants.Result_Success == resultCode) {
                Message message = new Message();
                message.what = handler_case.handler_get_m2m_server_success.ordinal();
                mHandler.sendMessage(message);
            } else {
                Message message = new Message();
                message.what = handler_case.handler_get_m2m_server_error.ordinal();
                message.obj = resultMessage;
                mHandler.sendMessage(message);
            }
        }
    };

    MqttAdapter mqttAdapter = new MqttAdapter() {
        @Override
        public void onConnection(int result, final String message) {
            if (Constants.Result_Success == result) {
                mHandler.sendEmptyMessage(handler_case.handler_connect_to_m2m.ordinal());
            }
        }

        @Override
        public void onDisconnection(int result, final String message) {
            mHandler.sendEmptyMessage(handler_case.handler_disconnect_from_m2m.ordinal());
        }

        @Override
        public void onSend(int result, String message, final String json) {

        }

        @Override
        public void onReceive(int result, String message, String jsonMessage, int cmd, String uid) {

            if (Constants.Result_Success == result) {
                Message message1 = new Message();
                message1.what = handler_case.handler_receive_mqtt_message.ordinal();
                Bundle bundle = new Bundle();
                bundle.putString("json", jsonMessage);
                bundle.putInt("cmd", cmd);
                bundle.putString("uid", uid);
                message1.setData(bundle);
                mHandler.sendMessage(message1);
            }
        }

        @Override
        public void onReceiveServerMsg(int result, String serverMsg) {
            if (Constants.Result_Success == result) {
                Message message = new Message();
                message.what = handler_case.handler_receive_server_message.ordinal();
                message.obj = serverMsg;
                mHandler.sendMessage(message);
            }
        }

        @Override
        public void upgrade(String url) {
            Log.d("mainactivity", "url -> " + url);
            String urlapk[] = {url};
            iotResourceType = "APK";
            DeviceSDK.getInstance().startDownload(urlapk, themePath + downloadPATH);
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handler_case handlerCase = handler_case.values()[msg.what];
            switch (handlerCase) {
                case handler_sign_up_success:
//                    Toast.makeText(mContext,"注册设备成功",Toast.LENGTH_SHORT).show();
                    upstateText("注册设备成功！");
                    upstateText("正在获取m2m服务器信息...");
                    mHandler.sendEmptyMessageDelayed(handler_case.handler_get_m2m_server_timeout.ordinal(), TIME_OUT * 1000);
                    DeviceSDK.getInstance().getM2MServer(MAC);
                    break;
                case handler_sign_up_error:
                    String signError = (String) msg.obj;
                    upstateText("注册失败： " + signError);
//                    Toast.makeText(mContext,"注册失败",Toast.LENGTH_SHORT).show();
                    mqttBus.post(new MqttInfo("connect", "error"));
                    break;
                case handler_sign_up_timeout:
//                    Toast.makeText(mContext,"注册设备超时",Toast.LENGTH_SHORT).show();
                    upstateText("注册设备超时");
                    break;
                case handler_get_m2m_server_success:
                    upstateText("成功获取m2m服务器信息！");
//                    Toast.makeText(mContext,"启动mqtt.......连接",Toast.LENGTH_SHORT).show();
                    upstateText("启动mqtt连接...");
                    mqttBus.post(new MqttInfo("connect", "ok"));
                    if (DeviceSDK.getInstance().isMqttConnected()) {
                        DeviceSDK.getInstance().disconnectMqtt();
                    }
                    DeviceSDK.getInstance().startServerMsgListener(MAC);
//                    Toast.makeText(mContext,"启动mqtt连接________ok",Toast.LENGTH_SHORT).show();
                    break;
                case handler_get_m2m_server_error:
                    String getError = (String) msg.obj;
                    upstateText("获取失败：" + getError);
//                    Toast.makeText(mContext,"handler_get_m2m_server_error",Toast.LENGTH_SHORT).show();
                    break;
                case handler_get_m2m_server_timeout:
                    upstateText("获取m2m服务器信息超时");
                    break;
                case handler_connect_to_m2m:
                    upstateText("成功连接云服务器！");
//                    Toast.makeText(mContext,"成功连接云服务器",Toast.LENGTH_SHORT).show();
                    break;
                case handler_receive_mqtt_message:
                    Log.i(TAG, "------------>>>>>>>>>: " + msg.getData().toString());
                    String rJson = msg.getData().getString("json");
                    String rUid = msg.getData().getString("uid");
                    mqttParamsBean.uid = rUid;
                    mqttParamsBean.typ = 1;

                    List<LiftLayoutParams> layoutList = MainActivity.getLayoutList();
                    Gson gson = new Gson();
                    mqttParamsBean.layout = gson.toJson(layoutList);
                    Log.i(TAG, "handleMessage: "+rJson);
                    updateValue(rJson);
                    upstateText(mqttParamsBean.toString());
                    createACK(mqttParamsBean, 1);
                    break;
                case handler_disconnect_from_m2m:
//                    Toast.makeText(mContext,"已断开m2m连接！",Toast.LENGTH_SHORT).show();
                    upstateText("已断开m2m连接！");
                    break;
                case handler_receive_server_message:
                    String serverMessage = (String) msg.obj;
                    try {
                        JSONObject jsonObject = new JSONObject(serverMessage);
                        String url = jsonObject.getString("firmware_url");
                        String vendor = jsonObject.getString("vendor");
                        String hardware_type = jsonObject.getString("hardware_type");
                        String hardware_version = jsonObject.getString("hardware_version");
                        String firmware_type = jsonObject.getString("firmware_type");
                        String firmware_version = jsonObject.getString("firmware_version");
                        String time = jsonObject.getString("time");
                    } catch (JSONException e) {

                    }
                    break;
            }
        }
    };

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action) || ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                Log.d(TAG, "receive action: " + action);
                if (NetworkUtils.isConnected(mContext)) {
                    if (!isAvailableByPing(mContext)) {
//                        Toast.makeText(mContext,"无网路连接",Toast.LENGTH_SHORT).show();
                        mqttBus.post(new MqttInfo("Network", "false"));
                    } else {
//                        Toast.makeText(mContext,"网路连接",Toast.LENGTH_SHORT).show();
                        mqttBus.post(new MqttInfo("Network", "true"));
                        //registDevice();
                        DeviceSDK.getInstance().downloadRemainTasks();
                    }
                }
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                DeviceSDK.getInstance().disconnectMqtt();
            }
        }
    };


    private void upstateText(String log) {
//        Log.i(TAG, " : " + log);
    }

    private void updateValue(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("items")) {
                JSONObject items = jsonObject.getJSONObject("items");
                Iterator<String> iterator = items.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    switch (key) {
                        case _layout:
                            mqttBus.post(new MqttInfo(_layout, items.getString(key)));
                            mqttParamsBean.layout = items.getString(key);
                            break;
                        case _displaytype:
                            mqttBus.post(new MqttInfo(_displaytype, items.getString(key)));
                            mqttParamsBean.displaytype = items.getString(key);
                            break;
                        case _fullscreen:
                            mqttBus.post(new MqttInfo(_fullscreen, items.getString(key)));
                            mqttParamsBean.fullscreen = items.getString(key);
                            break;
                        case _screentime:
                            mqttBus.post(new MqttInfo(_screentime, items.getString(key)));
                            mqttParamsBean.screentime = items.getString(key);
                            break;
                        case _sybrightness:
                            mqttBus.post(new MqttInfo(_sybrightness, items.getString(key)));
                            mqttParamsBean.sybrightness = items.getString(key);
                            break;
                        case _brightness:
                            mqttBus.post(new MqttInfo(_brightness, items.getString(key)));
                            mqttParamsBean.brightness = items.getString(key);
                            break;
                        case _volume:
                            mqttBus.post(new MqttInfo(_volume, items.getString(key)));
                            mqttParamsBean.volume = items.getString(key);
                            break;
                        case _reset:
                            DeviceSDK.getInstance().stopDownload();
//                            Log.i(TAG, "updateValue: "+items.getInt(key));
                            mqttBus.post(new MqttInfo(_reset, String.valueOf(items.getInt(key))));
                            resetDefult(true);
                            break;
                        case _webTitle:
                            mqttBus.post(new MqttInfo(_webTitle, String.valueOf(items.getInt(key))));
                            mqttParamsBean.mText = Integer.parseInt(items.getString(key));
                            break;
                        case _webscrolltext:
                            mqttBus.post(new MqttInfo(_webscrolltext, String.valueOf(items.getInt(key))));
                            mqttParamsBean.mScrolltext = Integer.parseInt(items.getString(key));
                            break;
                        case _date:
                            mqttBus.post(new MqttInfo(_date, String.valueOf(items.getInt(key))));
                            mqttParamsBean.mDate = Integer.parseInt(items.getString(key));
                            break;
                        case _imageinterval:

                            break;
                    }
                }
            } else if (jsonObject.has("resources") && jsonObject.has("type")) {
                urllist = new ArrayList<>();   //下载图片列表
//                JSONObject itemObject = new JSONObject();
                upstateText("-----resources-------" + jsonObject.toString());
                JSONArray jsonArray = jsonObject.getJSONArray("resources");
                int imageIndex = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject itemArrow = jsonArray.getJSONObject(i);
                    String url[] = {itemArrow.getString("url")};

                    switch (itemArrow.getString("catalog")) {
                        case PICTURES:
                            iotResourceType = PICTURES;
                            upstateText("PICTURES" + itemArrow.getString("url"));
                            urllist.add(itemArrow.getString("url"));
                            imageIndex++;
                            break;
                        case VIDEO:
                            iotResourceType = VIDEO;
                            upstateText("VIDEO" + itemArrow.getString("url"));
                            DeviceSDK.getInstance().startDownload(url, themePath + downloadPATH);
                            break;
                        case AUDIO:
                            iotResourceType = AUDIO;
                            upstateText("AUDIO" + itemArrow.getString("url"));
                            DeviceSDK.getInstance().startDownload(url, themePath + downloadPATH);
                            break;
                        case TEXT:
                            String catalogtype = itemArrow.getString("catalogtype");
                            upstateText("text" + itemArrow.getString("url"));
                            if (catalogtype.equals("title")) {
                                mqttBus.post(new MqttInfo(TEXT_TITLE, itemArrow.getString("url")));
                            } else if (catalogtype.equals("scrolltext")) {
                                mqttBus.post(new MqttInfo(TEXT_SCROL, itemArrow.getString("url")));
                            } else if (catalogtype.equals(TEXT_SUB)) {
                                mqttBus.post(new MqttInfo(TEXT_SUB, itemArrow.getString("url")));
                            }else if(catalogtype.equals(web_url)){
                                mqttBus.post(new MqttInfo(web_url, itemArrow.getString("url")));
                            }
                            break;
                        case web_url:
                            mqttBus.post(new MqttInfo(web_url, itemArrow.getString("url")));
                            break;
                    }
                }
                upstateText("imageDownImage: "+imageIndex);
                mqttBus.post(new MqttInfo("imageDownImage", String.valueOf(imageIndex)));
                startDownImageGroup(urllist);
            }
        } catch (JSONException e) {

        }
    }

    private void startDownImageGroup(List<String> url) {
            String path[] = new String[url.size()];
        for (int i = 0; i < url.size(); i++) {
            path[i] = url.get(i);
            Log.i(TAG, "startDownImageGroup: "+path[i]);
        }
        try {
            Thread.sleep(1000);
            DeviceSDK.getInstance().startDownload(path, themePath + downloadPATH);
        } catch (InterruptedException e) {

        }
    }

    List<String> urllist;
    /**
     * 返回一键恢复设置结果
     *
     * @param flag true 为 恢复成功
     */
    public void resetDefult(boolean flag) {
        if (flag) {
            createACK(mqttParamsBean, 0);
        }
    }
    public void createACK(MqttParamsBean paramsBean, int id) {
        JSONObject jsonObject = new JSONObject();
        String time = String.valueOf(System.currentTimeMillis());
        JSONObject items = new JSONObject();
        try {
            items.put(_layout, paramsBean.layout);
            items.put(_displaytype, paramsBean.displaytype);
            items.put(_fullscreen, paramsBean.fullscreen);
            items.put(_screentime, paramsBean.screentime);
            items.put(_sybrightness, paramsBean.sybrightness);
            items.put(_brightness, paramsBean.brightness);
            items.put(_volume, paramsBean.volume);
            items.put(_date, paramsBean.mDate);
            items.put(_webTitle, paramsBean.mText);
            items.put(_webscrolltext, paramsBean.mScrolltext);


            if (id == 0) {
                items.put(_reset, 0);
            }
            jsonObject.put("items", items);
            jsonObject.put("time", time);
            upstateText("------------createACK------try---------");
        } catch (JSONException e) {

        }
        upstateText("------------createACK--------catch-------");
        DeviceSDK.getInstance().sendMessage(jsonObject.toString().getBytes(), paramsBean.typ, paramsBean.uid);
    }

    //Mqtt 发送消息
    public class MqttInfo {
        public String type;
        public String info;

        public MqttInfo(String type, String info) {
            this.type = type;
            this.info = info;
        }

        @Override
        public String toString() {
            return "MqttInfo{" +
                    "type='" + type + '\'' +
                    ", info='" + info + '\'' +
                    '}';
        }
    }

    public class CompletedResource {
        public String rType;
        public String rPath;

        public CompletedResource(String rType, String rPath) {
            this.rType = rType;
            this.rPath = rPath;
        }
        @Override
        public String toString() {
            return "completedResource{" +
                    "rType='" + rType + '\'' +
                    ", rPath='" + rPath + '\'' +
                    '}';
        }
    }
    public void unregisterReceiver(){
        mContext.unregisterReceiver(receiver);
    }
}
