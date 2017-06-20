package com.shbst.bst.tftdisplay_15_h.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;
import com.shbst.bst.tftdisplay_15_h.service.SwiFTP.ConfigureActivity;
import com.shbst.bst.tftdisplay_15_h.service.SwiFTP.Defaults;
import com.shbst.bst.tftdisplay_15_h.service.SwiFTP.FTPServerService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

/**
 * SDAT server protocol.
 *
 * @author Yong Hao
 */
public class SDAT_Server extends Service {
    private static final String LOG_TAG = "SDAT_Server";
    private static final int DATA_MAX_LENGTH = 8192;                /* 帧长度，目前1024Byte已足够 */
    private static final String CODEC = "UTF-8";                    /* 协议通信中的字符编码格式 */
    private static final int SDAT_SERVER_PORT = 8000;               /* 固定端口号用于客户端广播查找 */
    private static final String FTP_SERVER_USERNAME = "SDAT";       /* FTP服务器用户名 */
    private static final String FTP_SERVER_PASSWORD = "Ruby9527";   /* FTP服务器密码 */
    private static final int FTP_SERVER_PORT = 2121;                /* FTP服务器端口号 */
    private static final String VIDEO = "/video";                  /* 固定Video存放地址 */
    private static final String AUDIO = "/video";                   /* 固定Audio存放地址 */
    private static final String PICTURE = "/picture";              /* 固定Picture存放地址 */
    private static final String PREFERENCE_NAME = "Account";        /* XML配置文件名（用于保存登录密码） */
    private static final String PASSWORD_KEY = "password";          /* 配置文件中，密码的KEY */
    private static final String DEFAULT_MD5
            = "4b7cf4a55014e185813e644502ea9";                      /* 默认密码asdfg的MD5值 */
    private static final int CLIENT_MAX = 4;  /* 最多连接客户端数量 */

    private WifiManager.MulticastLock UdpLock;
    private LocalBinder binder = new LocalBinder();
    private EventBus InfoBus =EventBus.getDefault();
    private Socket socket;
    private DatagramSocket UdpSocket;
    private OutputStream[] OutputArray = new OutputStream[CLIENT_MAX];                         /* 记录已建立连接的输出流，用于响应对应客户端的数据 */

    private String location;                /* 设备安装位置 */
    private String UID;                     /* 设备唯一识别码 */
    private short Identifier_Lift = 1;      /* LIFT报文标识符 */
    //FixMe, 需要修改destination_2和destination_1，分客户端处理
    private String destination_2 = null;    /* 记录呼梯的目的层（在到达当前楼层后，再将目的层登记） */
    private String destination_1 = null;    /* 记录呼梯要先到的手机呼梯者所在楼层 */
    private String CurrentFloor;            /* 电梯当前所在楼层（注意，不是手机呼梯者当前所在楼层） */
    private boolean KeepRunning = false;    /* 本service主线程是否运行(同时也控制已建立连接的子线程) */
    private int ClientCnt = 0;              /* 已连接的客户端数量 */

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "The service has been created.");
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "The service has been destroyed.");
        KeepRunning = false;
        UdpSocket.close();  /* 同过关闭Socket，使UDP阻塞状态产生异常 */
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

        /**
         * 获得本service给其他组件发送事件所用的EventBus。
         * @return 本service的EventBus。
         */
        public EventBus getEventBus() {
            return InfoBus;
        }

        /**
         * 启动service的主线程，用于等待客户端连接。
         * @param _location 本设备安装位置。
         * @param _UID      本设备的识别码。
         */
        public void start(String _location, String _UID) {
            if (KeepRunning) {
                Log.e(LOG_TAG, "The service main thread is running, can not repeat start the service.");
                return;
            }

            location = _location;
            UID = _UID;
            KeepRunning = true;
            new Thread(ProtocolMainThread).start();
        }

        /**
         * 发送电梯运行信息。
         * 在收到CONNECT_SUCCESS类型的事件后，要调用一次该函数，发送电梯当前状态。
         * 之后如果电梯状态变化，还要再次调用该函数。
         * @param info 电梯状态信息。
         */
        public void sendLiftInfo(final LiftInfo info) {
            if (ClientCnt == 0) {
                Log.e(LOG_TAG, "In the disconnected state, cannot send lift info!");
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int LoadLength;
                    byte[] load;
                    int i, k;

                    LoadLength = info.getLength() + 3;
                    load = new byte[LoadLength];

                    load[0] = (byte) (Identifier_Lift >>> 8);
                    load[1] = (byte) Identifier_Lift;
                    load[2] = 1;
                    for (k = 3, i = 0; i < info.getLength(); i++, k++) {
                        load[k] = info.getInfo()[i];
                    }

                    /* 电梯运行信息需要发送给每一个已连接的客户端 */
                    for (i = 0; i < ClientCnt; i++) {
                        sendFrameClient((byte) 6, load, LoadLength, i);
                    }

                    int floor = info.getInfo()[0] * 1000 + info.getInfo()[1] * 100 + info.getInfo()[2] * 10 + info.getInfo()[3];
                    CurrentFloor = String.valueOf(floor);    /* 更新电梯当前楼层 */

                    /* 如果电梯已到达destination_1，再将目的层destination_2登记到控制柜 */
                    if (destination_1 != null) {
                        if (CurrentFloor.equals(destination_1)) {
                            InfoBus.post(new InfoEvent(0, InfoEvent.CALL, destination_2));
                            destination_1 = null;
                            destination_2 = null;
                        }
                    }
                }
            }).start();
        }

        /**
         * 通知客户端，呼梯是否成功(呼梯响应)。
         * @param floor 已确认呼梯的楼层。
         * @param client 对应的客户端。
         */
        public void confirmCallFloor(final String floor, final int client) {
            if (ClientCnt == 0) {
                Log.e(LOG_TAG, "In the disconnected state, cannot send confirmCallFloor!");
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] load = floor.getBytes(CODEC);
                        sendFrameClient((byte) 17, load, load.length, client);/* 确认呼梯使用报文编号17 */
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        /**
         * 发送JSON数据到客户端。
         * @param json 需要发送的JSON数据。
         * @param client 对应的客户端。
         */
        public void sendJSON(final String json, final int client) {
            if (ClientCnt == 0) {
                Log.e(LOG_TAG, "In the disconnected state, unable to send JSON!");
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] load = json.getBytes(CODEC);
                        sendFrameClient((byte) 9, load, load.length, client);    /* 服务端发送JSON到客户端，使用报文编号9 */
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /******************EventBus--自定义事件*****************/
    public class InfoEvent {
        /* 事件类型(本类属性type) */
        public static final String CALL = "Call";                       /* info数据为呼梯楼层 */
        public static final String CONNECT_SUCCESS = "Connect success"; /* 连接成功，通知其他组件。info为空白字符串"" */
        public static final String FILE_UPLOAD = "File upload";         /* info数据为:JSON_UploadFileInform */
        public static final String JSON_INFO = "JSON info";             /* info数据为JSON格式的字符串 */
        public static final String UPDATE_JSON = "Update JSON";         /* 更新界面配置，通知其他组件。info为空白字符串"" */

        private int client;     /* 客户端编号 */
        private String type;
        private String info;

        public InfoEvent(int client, String type, String info) {
            this.client = client;
            this.type = type;
            this.info = info;
        }

        public int getClient() {
            return client;
        }

        public String getType() {
            return type;
        }

        public String getInfo() {
            return info;
        }
    }
    /**************END(EventBus--自定义事件)*****************/

    /******************本service的主线程*********************/
    private Runnable ProtocolMainThread = new Runnable() {
        private ServerSocket server;

        @Override
        public void run() {
            try {
                /* 建立FTP服务端，用于文件的上传或下载 */
                FTP_StartServer();

                /* 检查是否已有设备密码的MD5值。如果没有，将默认MD5值写入配置文件 */
                SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
                String md5 = settings.getString(PASSWORD_KEY, null);
                if (md5 == null) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(PASSWORD_KEY, DEFAULT_MD5);
                    editor.apply();
                }

                WifiManager manager = (WifiManager) SDAT_Server.this.getSystemService(Context.WIFI_SERVICE);
                UdpLock = manager.createMulticastLock(LOG_TAG + "_UDP lock");

                /* socket绑定同样的端口只能绑定一次，绑定多次同一个端口会报错 */
                UdpSocket = new DatagramSocket(SDAT_SERVER_PORT);
                server = new ServerSocket(SDAT_SERVER_PORT);

                while (KeepRunning) {
                    if (!UdpConnect()) {
                        break;
                    }

                    Log.d(LOG_TAG, "TCP: Wait connected by client......");
                    socket = server.accept();   /* 阻塞等待客户端连接 */
                    Log.d(LOG_TAG, "TCP: A new client detected!");

                    if (ClientCnt < CLIENT_MAX) {
                        /* 与客户端通讯，建立独立的子线程 */
                        new Thread(new ProtocolSubThread()).start();
                    } else {
                        Log.e(LOG_TAG, "The client number is full!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            FTP_StopServer();
            Log.d(LOG_TAG, "Service main thread itself is over.");
        }

        private boolean UdpConnect() {
            try {
                byte[] receive = new byte[64];      /* mobile端第一个数据应该是“Do you copy”，64个字节足够接收 */
                DatagramPacket ReceivePacket = new DatagramPacket(receive, receive.length);
                UdpLock.acquire();

                while (true) {
                    if (!UdpLock.isHeld()) {
                        Log.e(LOG_TAG, "UDP: The MulticastLock is not hold!");
                    }
                    Log.d(LOG_TAG, "UDP: Wait the client connect......");
                    UdpSocket.receive(ReceivePacket);      /* 阻塞等待接收 */
                    Log.d(LOG_TAG, "UDP: Received the data from client!");
                    byte[] data = decryption_1(ReceivePacket.getData(), ReceivePacket.getLength());
                    if (!(new String(data, 0, data.length, CODEC).equals("Do you copy"))) {
                        Log.e(LOG_TAG, "UDP: The first datagram is not \" Do you copy\", is "
                                + "\"" + new String(data, 0, data.length, CODEC) + "\"");
                        continue;
                    }

                    /* 发送设备信息（安装位置和唯一识别码） */
                    JSON_DeviceInfo info = new JSON_DeviceInfo();
                    info.setLocation(location);
                    info.setUID(UID);
                    Gson json = new Gson();
                    String StrSend = json.toJson(info);
                    byte[] send = StrSend.getBytes(CODEC);
                    byte[] EncryptedSend = encryption_1(send, send.length);
                    DatagramPacket SendPacket =
                            new DatagramPacket(EncryptedSend,
                                    EncryptedSend.length,
                                    InetAddress.getByName(ReceivePacket.getAddress().getHostAddress()),
                                    ReceivePacket.getPort());
                    UdpSocket.send(SendPacket);

                    /* 等待确认信息(此处不能做超时判断，因为无法确定client端何时会登录) */
                    Log.d(LOG_TAG, "Waiting to confirm whether choose the current device......");
                    UdpSocket.receive(ReceivePacket);
                    data = decryption_1(ReceivePacket.getData(), ReceivePacket.getLength());
                    if (new String(data, 0, data.length, CODEC).equals(UID)) {
                        /* 当前设备被选中，结束UDP阶段 */
                        Log.d(LOG_TAG, "The current device has been selected!");
                        UdpLock.release();
                        return true;
                    } else {
                        /* 当前设备未被选中，继续UDP等待 */
                        Log.d(LOG_TAG, "The current device has not been selected!");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "UDP: Catch exception!");
                UdpLock.release();
                KeepRunning= false; /* UDP的任何异常都需要让Service主线程结束，同时结束已连接的子线程 */
                return false;
            }
        }

        /* 用于UDP传输中的数据解密 */
        private byte[] decryption_1(byte[] data, int length) {
            byte[] ret = new byte[length];
            int i;
            for (i = 0; i < length; i++) {
                ret[i] = data[i];
            }

            return ret;//TODO, 需要加入算法
        }

        /* 用于UDP传输中的数据加密 */
        private byte[] encryption_1(byte[] data, int length) {
            byte[] ret = new byte[length];
            int i;
            for (i = 0; i < length; i++) {
                ret[i] = data[i];
            }

            return ret;//TODO, 需要加入算法
        }

        /********************SwiFTP 相关函数*********************/
        /* 设置FTPServer的部分参数(其余参数在Defaults.java) */
        private void setConfigure() {
            SharedPreferences settings = getSharedPreferences(Defaults.getSettingsName(), Defaults.getSettingsMode());
            SharedPreferences.Editor editor = settings.edit();

            editor.putString(ConfigureActivity.USERNAME, FTP_SERVER_USERNAME);
            editor.putString(ConfigureActivity.PASSWORD, FTP_SERVER_PASSWORD);
            editor.putInt(ConfigureActivity.PORTNUM, FTP_SERVER_PORT);
            editor.putString(ConfigureActivity.CHROOTDIR, Environment.getExternalStorageDirectory().getAbsolutePath());
            Log.d(LOG_TAG, "FTP Server Directory:" + Environment.getExternalStorageDirectory().getAbsolutePath());

            editor.putBoolean(ConfigureActivity.ACCEPT_WIFI, Defaults.acceptWifi);
            editor.putBoolean(ConfigureActivity.ACCEPT_NET, Defaults.acceptNet);
            editor.putBoolean(ConfigureActivity.STAY_AWAKE, Defaults.stayAwake);
            editor.commit();
        }

        private void FTP_StartServer() {
            Log.d(LOG_TAG, "SwiFTP server start.");

            setConfigure();
            Intent intent = new Intent(SDAT_Server.this, FTPServerService.class);
            if (!FTPServerService.isRunning()) {
                startService(intent);
            }
        }

        private void FTP_StopServer() {
            Log.d(LOG_TAG, "SwiFTP server stop.");

            Intent intent = new Intent(SDAT_Server.this, FTPServerService.class);
            stopService(intent);
        }
        /******************END(SwiFTP 相关函数)******************/
    };
    /***************END(本service的主线程)*******************/

    /****************为每一个已连接的客户端建立的子线程*******************/
    private class ProtocolSubThread implements Runnable {
        ReceiveThreadClass ReceiveThread;   /* 子线程建立的接收线程 */
        private Timer PingTimer;            /* 子线程建立的计时线程 */
        private OutputStream output;

        private short PingTime;              /* 心跳时间 */
        private short PingCnt;               /* 心跳计时 */
        private int Client;
        private boolean isConnected = false; /* 是否已进入到正常连接状态（TCP连接已建立，并且已收到客户端的CONNECT报文） */

        @Override
        public void run() {
            try {
                Client = ClientCnt;
                Log.d(LOG_TAG, "Service sub thread is running, client number is " + Client);

                output = socket.getOutputStream();
                OutputArray[ClientCnt++] = output;

                byte[] receive = new byte[DATA_MAX_LENGTH];
                int ReceiveNumber = DATA_MAX_LENGTH;
                Frame frame = new Frame(receive, ReceiveNumber);

                /* 开启接收数据的线程 */
                ReceiveThread = new ReceiveThreadClass(frame);
                new Thread(ReceiveThread).start();

                /* 处理接收的数据 */
                processFrame(frame);
                Log.d(LOG_TAG, "Service sub thread is over, client number is " + Client);
                ClientCnt--;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void processFrame(Frame frame) {
            try {
                Frame ReceivedFrame;
                int FrameType;
                /* cancel之后的Timer不能再次schedule，因此每一个新的连接，必须new一个新的Timer */
                PingTimer = new Timer();

                while (KeepRunning) {   /* 如果service主线程结束，则所有子线程也要结束。 */
                    if (frame.isNewFrame()) {
                        ReceivedFrame = frame;
                        frame.dealFrame();
                        clearPingCnt();
                        FrameType = ReceivedFrame.getFrameType();

                        if (isConnected) {
                            if (FrameType == 1) {
                                /* 在已经连接的状态下，再次收到CONNECT报文，断开连接 */
                                Log.e(LOG_TAG, "Received CONNECT frame again in connected status!");
                                break;
                            } else if (FrameType == 3) {
                                processFrame_PINGREQ();
                            } else if (FrameType == 5) {
                                Log.d(LOG_TAG, "The client request to disconnect!");
                                break;
                            } else if (FrameType == 7) {
                                processFrame_LIFTACK(ReceivedFrame);
                            } else if (FrameType == 8) {
                                processFrame_JSON(ReceivedFrame);
                            } else if (FrameType == 11) {
                                processFrame_STREAM(ReceivedFrame);
                            } else if (FrameType == 13) {
                                processFrame_STREAMRESULT(ReceivedFrame);
                            } else if (FrameType == 14) {
                                processFrame_DEBUG(ReceivedFrame);
                            } else if (FrameType == 15) {
                                processFrame_DEBUGACK(ReceivedFrame);
                            } else if (FrameType == 16) {
                                processFrame_CALL(ReceivedFrame);
                            } else if (FrameType == 20) {//FixMe,临时增加
                                InfoBus.post(new InfoEvent(Client, InfoEvent.UPDATE_JSON, ""));
                            } else{
                                Log.e(LOG_TAG, "Received unknown frame type:" + FrameType);
                            }
                        } else {
                            if (FrameType == 1) {
                                if (processFrame_CONNECT(ReceivedFrame) == 1) {
                                    break; /* 账户验证失败，需要退出 */
                                } else {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Thread.sleep(300);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                            /* 连接成功，通知其他组件，发送一次电梯当前状态 */
                                            InfoBus.post(new InfoEvent(Client, InfoEvent.CONNECT_SUCCESS, ""));
                                            Log.d(LOG_TAG, "Client connect successful!");

                                            PingCnt = 0;
                                            PingTimer.schedule(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    addPingCnt();
                                                }
                                            }, 1000, 1000);
                                            Log.d(LOG_TAG, "Timer is running......");
                                        }
                                    }).start();
                                }
                            } else {    /* 第一个报文不是CONNECT，断开连接 */
                                Log.e(LOG_TAG, "The first frame is not CONNECT!");
                                break;
                            }
                        }
                    } else {
                        Thread.sleep(15);   /* 短暂睡眠，让出CPU占用 */
                    }

                    if (isConnected) {
                        if (isTimeout()) {
                            Log.e(LOG_TAG, "Connection timeout!");
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                isConnected = false;
                socket.close();
                PingTimer.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendFrame(byte type, byte[] load, int LoadLength) {
            try {
                byte[] data = new byte[DATA_MAX_LENGTH];
                int LoadStart;
                int X = LoadLength;
                byte encodedByte;
                int i = 1;

                data[0] = type;

                do {
                    encodedByte = (byte) (X % 128);
                    X = X / 128;
                    if (X > 0) {
                        encodedByte = (byte) (encodedByte | 128);
                    }
                    data[i++] = encodedByte;
                } while (X > 0);

                LoadStart = i;
                for (i = 0; i < LoadLength; i++) {
                    if (LoadStart >= DATA_MAX_LENGTH) {
                        Log.e(LOG_TAG, "The frame is too long to send! Frame type is " + type);
                        return;
                    }
                    data[LoadStart++] = load[i];
                }

                output.write(data, 0, LoadStart);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /* 处理第一帧连接建立的报文 */
        private int processFrame_CONNECT(Frame frame) {
            int LoadBegin;
            LoadBegin = frame.getLoadBeginPosition();

            /* 获得心跳时间 */
            PingTime = (short) (frame.data[LoadBegin] * 256 + frame.data[LoadBegin + 1]);
            Log.d(LOG_TAG, "Get the ping time: " + PingTime);

            /* 检验密码的正确性 */
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
            String md5 = settings.getString(PASSWORD_KEY, null);
            boolean PasswordCheck = false;
            try {
                Gson json = new Gson();
                JSON_Account account
                        = json.fromJson(new String(frame.getLoad(), 2, frame.getLoadLength()-2, CODEC),/* 跳过2个字节的心跳数据 */
                        JSON_Account.class);
                if (account.getPassword().equals(md5)) {
                    PasswordCheck = true;
                    Log.d(LOG_TAG, "MD5 check success.");
                } else {
                    PasswordCheck = false;
                    Log.e(LOG_TAG, "MD5 check failed! Received MD5 value is " + account.getPassword()
                            + ", and the current MD5 value is " + md5);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            /* 返回CONNACK报文 */
            byte[] send = new byte[1];
            if (PasswordCheck) {
                send[0] = 0;
                isConnected = true;
            } else {
                send[0] = 1;
                isConnected = false;
            }

            sendFrame((byte) 2, send, 1);

            if (isConnected) {
                return 0;
            } else {
                return 1;
            }
        }

        /* 处理客户端发来的心跳请求 */
        private void processFrame_PINGREQ() {
            sendFrame((byte) 4, null, 0);
        }

        /* 处理客户端发来的电梯信息报文响应 */
        private void processFrame_LIFTACK(Frame frame) {
            short identifier;
            byte[] load = frame.getLoad();

            identifier = (short) ((load[0] << 8) + load[1]);
            if (identifier == Identifier_Lift) {
                if (frame.data[4] != 0) {
                    Log.e(LOG_TAG, "FrameType_7," + "The frame data check error!");
                }
            } else {
                Log.e(LOG_TAG, "FrameType_7," + "The frame identifier is error:" + identifier
                        + ". Original data: load[0]=" + load[0] + ", load[1]=" + load[1]);
            }
        }

        /* 处理JSON数据帧 */
        private void processFrame_JSON(Frame frame) {
            try {
                /* 发送给其他组件，JSON格式的数据*/
                InfoBus.post(new InfoEvent(Client, InfoEvent.JSON_INFO,
                        new String(frame.getLoad(), 0, frame.getLoadLength(), CODEC)));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        /* 客户端请求上传文件 */
        private void processFrame_STREAM(Frame frame) {
            final long RESERVE_SPACE = 1024 * 1024;     /* 保留的存储空间 */

            try {
                Gson json = new Gson();
                JSON_UploadFileInfo fileInfo = json.fromJson(
                        new String(frame.getLoad(), 0, frame.getLoadLength(), CODEC), JSON_UploadFileInfo.class);
                String type = fileInfo.getFileType();
                long length = fileInfo.getFileLength();
                byte[] load = new byte[1];
                String theme = Environment.getExternalStorageDirectory().getAbsolutePath();
                File file = new File(theme);
                if (!file.exists()) {
                    file.mkdir();
                }
                if ("Video".equals(type)) {
                    String directory = Environment.getExternalStorageDirectory().getAbsolutePath()
                            + VIDEO;
                    File tmp = new File(directory);
                    long FreeSpace = tmp.getFreeSpace();

                    if (length < FreeSpace - RESERVE_SPACE) {
                        load[0] = 1;
                    } else {
                        load[0] = 0;
                    }
                } else if ("Audio".equals(type)) {
                    String directory = Environment.getExternalStorageDirectory().getAbsolutePath()
                            + AUDIO;
                    File tmp = new File(directory);
                    long FreeSpace = tmp.getFreeSpace();

                    if (length < FreeSpace - RESERVE_SPACE) {
                        load[0] = 1;
                    } else {
                        load[0] = 0;
                    }
                } else if ("Picture".equals(type)) {
                    String directory = Environment.getExternalStorageDirectory().getAbsolutePath()
                            + PICTURE;
                    File tmp = new File(directory);
                    long FreeSpace = tmp.getFreeSpace();

                    if (length < FreeSpace - RESERVE_SPACE) {
                        load[0] = 1;
                    } else {
                        load[0] = 0;
                    }
                } else if ("Word".equals(type)) {
                    load[0] = 1;
                }

                sendFrame((byte) 12, load, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* 处理客户端发来的文件传输结果 */
        private void processFrame_STREAMRESULT(Frame frame) {
            try {
                Gson json = new Gson();
                JSON_TransFileResult fileInfo = json.fromJson(
                        new String(frame.getLoad(), 0, frame.getLoadLength(), CODEC), JSON_TransFileResult.class);

                /* 只有传输成功，才需要通知其他组件。传输失败造成的临时文件由Client端清理。 */
                if (fileInfo.getResult()) {
                    JSON_UploadFileInform inform = new JSON_UploadFileInform();
                    inform.setFileType(fileInfo.getFileType());
                    inform.setStatus("end");
                    inform.setFileName(fileInfo.getFileName());
                    json = new Gson();
                    String tmp = json.toJson(inform);
                    InfoBus.post(new InfoEvent(Client, InfoEvent.FILE_UPLOAD, tmp));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        /* 处理调试信息 */
        private void processFrame_DEBUG(Frame frame) {
        }

        /* 调试信息应答 */
        private void processFrame_DEBUGACK(Frame frame) {
        }

        /* 处理呼梯请求 */
        private void processFrame_CALL(Frame frame) {
            byte[] load = frame.getLoad();

            /* 先发出当前层的呼梯，待电梯到达当前层后，再发出目的层呼梯 */
            try {
                Gson json = new Gson();
                JSON_CallFloor callFloor = json.fromJson(new String(load, 0, frame.getLoadLength(), CODEC), JSON_CallFloor.class);
                destination_1 = callFloor.getCurrentFloor();
                destination_2 = callFloor.getDestination();

                /* 如果当前楼层和呼梯手机在同一楼层，则只发送呼梯的目的楼层 */
                if (CurrentFloor.equals(destination_1)) {
                    InfoBus.post(new InfoEvent(Client, InfoEvent.CALL, destination_2));
                    destination_1 = null;
                    destination_2 = null;
                } else {
                    InfoBus.post(new InfoEvent(Client, InfoEvent.CALL, destination_1));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        private void addPingCnt() {
            PingCnt++;
        }

        private void clearPingCnt() {
            PingCnt = 0;
        }

        private boolean isTimeout() {
            return (PingCnt >= PingTime * 2);
        }
    };

    /* 指定将信息发给某个客户端 */
    private void sendFrameClient(byte type, byte[] load, int LoadLength, int client) {
        try {
            byte[] data = new byte[DATA_MAX_LENGTH];
            int LoadStart;
            int X = LoadLength;
            byte encodedByte;
            int i = 1;

            data[0] = type;

            do {
                encodedByte = (byte) (X % 128);
                X = X / 128;
                if (X > 0) {
                    encodedByte = (byte) (encodedByte | 128);
                }
                data[i++] = encodedByte;
            } while (X > 0);

            LoadStart = i;
            for (i = 0; i < LoadLength; i++) {
                if (LoadStart >= DATA_MAX_LENGTH) {
                    Log.e(LOG_TAG, "The frame is too long to send! Frame type is " + type);
                    return;
                }
                data[LoadStart++] = load[i];
            }

            OutputArray[client].write(data, 0, LoadStart);
            OutputArray[client].flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 该线程只用来接收数据 */
    private class ReceiveThreadClass implements Runnable {
        private InputStream input;
        private Frame frame;

        public ReceiveThreadClass(Frame frame) {
            this.frame = frame;
        }

        @Override
        public void run() {
            try {
                input = socket.getInputStream();
                byte[] receive = new byte[DATA_MAX_LENGTH];
                int ReceiveNumber;

                Log.d(LOG_TAG, "Receive thread(ID:" + Thread.currentThread().getId() + ") is running......");
                while ((ReceiveNumber = input.read(receive)) != -1) {
                    frame.setFrame(receive, ReceiveNumber);

                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(LOG_TAG, "Receive thread(ID:" + Thread.currentThread().getId() + ") catch exception, is over.");
            }
        }
    }

    private class Frame {
        public byte[] data;
        public int length;
        private boolean NewFrame = false;

        public Frame(byte[] data, int length) {
            this.data = data;
            this.length = length;
        }

        public synchronized void setFrame(byte[] data, int length) {
            this.data = data;
            this.length = length;
            NewFrame = true;
        }

        public synchronized boolean isNewFrame() {
            return NewFrame;
        }

        public synchronized void dealFrame() {
            NewFrame = false;
        }

        public byte getFrameType() {
            return data[0];
        }

        public int getLoadLength() {
            int multiplier = 1;
            int value = 0;
            int i = 1;
            byte encodedByte;

            do {
                encodedByte = data[i++];
                value += (encodedByte & 127) * multiplier;
                multiplier *= 128;
                if (multiplier > 128 * 128 * 128) {
                    Log.e(LOG_TAG, "Invalid LoadLength!");
                    value = 0;
                    return value;
                }
            } while ((encodedByte & 128) != 0);

            return value;
        }

        public int getLoadBeginPosition() {
            int ret = 0;

            if ((data[1] & 0x80) == 0x00) {
                ret = 2;
            } else if (((data[1] & 0x80) == 0x80) && ((data[2] & 0x80) == 0x00)) {
                ret = 3;
            } else if (((data[1] & 0x80) == 0x80) && ((data[2] & 0x80) == 0x80)
                    && ((data[3] & 0x80) == 0x00)) {
                ret = 4;
            } else if (((data[1] & 0x80) == 0x80) && ((data[2] & 0x80) == 0x80)
                    && ((data[3] & 0x80) == 0x80) && ((data[4] & 0x80) == 0x00)) {
                ret = 5;
            }

            return ret;
        }

        public byte[] getLoad() {
            byte[] ret = new byte[DATA_MAX_LENGTH];
            int i;
            int LoadLength = getLoadLength();
            int LoadBegin = getLoadBeginPosition();

            for (i = 0; i < LoadLength; i++) {
                ret[i] = data[LoadBegin++];
            }

            return ret;
        }
    }

    public static class LiftInfo {
        private final int LENGTH = 9;
        private byte[] info;

        public LiftInfo() {
            info = new byte[LENGTH];
        }

        public byte[] getInfo() {
            return info;
        }

        public int getLength() {
            return LENGTH;
        }

        public void setFloor(byte thousand, byte hundred, byte ten, byte one) {
            info[0] = thousand;
            info[1] = hundred;
            info[2] = ten;
            info[3] = one;
        }

        public void setFloorEffect(byte effect) {
            info[4] = effect;
        }

        public void setArrow(byte arrow) {
            info[5] = arrow;
        }

        public void setArrowEffect(byte effect) {
            info[6] = effect;
        }

        public void setStatus(byte status) {
            info[7] = status;
        }

        public void setStatusEffect(byte effect) {
            info[8] = effect;
        }
    }

    /*********************JSON类数据流定义***********************/
    /* 由服务端发送给客户端的设备信息 */
    private class JSON_DeviceInfo {
        /* 如果location和UID都是NULL，表示未发现任何设备。 */
        private String location;
        private String UID;

        public void setLocation(String location) {
            this.location = location;
        }

        public String getLocation() {
            return location;
        }

        public void setUID(String UID) {
            this.UID = UID;
        }

        public String getUID() {
            return UID;
        }
    }

    private class JSON_Account {
        private String username;
        private String password;

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }
    }

    private class JSON_CallFloor {
        private String CurrentFloor;
        private String Destination;

        public void setCurrentFloor(String floor) {
            this.CurrentFloor = floor;
        }

        public String getCurrentFloor() {
            return CurrentFloor;
        }

        public void setDestination(String destination) {
            this.Destination = destination;
        }

        public String getDestination() {
            return Destination;
        }
    }

    /* 文件信息。用于客户端请求上传文件时，发送给服务端。 */
    public class JSON_UploadFileInfo {
        private String FileType;    /* 文件类型：Video,Audio,Picture,Word */
        private String FileName;    /* 文件名(文件类型为Word时，文件名就是要传输的字符串) */
        private long FileLength;    /* 文件大小 */

        public void setFileType(String fileType) {
            FileType = fileType;
        }

        public String getFileType() {
            return FileType;
        }

        public void setFileName(String fileName) {
            FileName = fileName;
        }

        public String getFileName() {
            return FileName;
        }

        public void setFileLength(long fileLength) {
            FileLength = fileLength;
        }

        public long getFileLength() {
            return FileLength;
        }
    }

    /* 通知其他组件。某个类型的文件要开始上传（或上传结束） */
    public class JSON_UploadFileInform {
        private String FileType;
        private String status;      /* begin,end */
        private String FileName;    /* 传输结束时，新的文件名。（或者是新的字符串） */

        public void setFileType(String fileType) {
            FileType = fileType;
        }

        public String getFileType() {
            return FileType;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setFileName(String fileName) {
            FileName = fileName;
        }

        public String getFileName() {
            return FileName;
        }
    }

    /* 文件传输结果。通知服务端，服务端可以据此判断是否删除保留新文件，删除旧文件。 */
    public class JSON_TransFileResult {
        private String FileName;
        private String FileType;
        private boolean Result;     /* true--传输成功，false--传输失败 */

        public void setFileName(String fileName) {
            FileName = fileName;
        }

        public String getFileName() {
            return FileName;
        }

        public void setFileType(String fileType) {
            FileType = fileType;
        }

        public String getFileType() {
            return FileType;
        }

        public void setResult(boolean result) {
            Result = result;
        }

        public boolean getResult() {
            return Result;
        }
    }
    /******************END(JSON类数据流定义)********************/
}
