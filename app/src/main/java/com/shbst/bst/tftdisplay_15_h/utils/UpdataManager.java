package com.shbst.bst.tftdisplay_15_h.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.shbst.androiddevicesdk.utils.LogUtils;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import de.greenrobot.event.EventBus;

/**
 * Created by zhouwenchao on 2017-05-23.
 */
public class UpdataManager {
    private static final String musicDir = "Music";
    private static final String videoDir = "Movies";
    private static final String upApkDir = "Apk";
    private static final String upMediaDir = "resDownload";

    private static final String update = "update";
    private static final String upXML="/mediascreen.xml";

    private static Handler handler = new Handler();
    private final static ReentrantLock CompressFileLock = new ReentrantLock();   //  锁,  防止多次点击创建多个压缩文件
    private static final String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private static final String musicDirToPath = basePath + musicDir;
    private static final String videoDirTopath = basePath + videoDir;
    private static final String upApkDirToPath = basePath + upApkDir;
    private static final String upMediaDirToPath = basePath + upMediaDir;
    private static LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
    private static boolean isCopy = false;
    private static Context context;
    private static ProgressDialog progressDialog;


    static EventBus eventBus = EventBus.getDefault();
    public static synchronized void startUpSource(String sourcePathTmp, Context contextTmp) {

        if (!CompressFileLock.isLocked()) {
            CompressFileLock.lock();
            isCopy = true;
            context = contextTmp;
            if(eventBus == null){
                eventBus.register(context);
            }
            Log("正在复制升级文件，请稍后");
            Toast.makeText(contextTmp, "正在复制升级文件，请稍后", Toast.LENGTH_LONG).show();

        } else {
            Log("复制升级文件异常，请稍后再试");
            Toast.makeText(contextTmp, "复制升级文件异常，请稍后再试", Toast.LENGTH_LONG).show();
            return;
        }
        String sourcePath = clearStr(sourcePathTmp, "file://") + File.separator;
        Log("String sourcePath   "+sourcePath+ update);

        map.clear();
        map.put(sourcePath+ update, upMediaDirToPath);
        upSource();
    }

    public static synchronized void stopCopy() {
        isCopy = false;
    }

    private static boolean errorTag = false;

    private static void upSource() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                errorTag = false;
                final Gson gson = new Gson();
                try {
                    for (final Map.Entry<String, String> entry : map.entrySet()) {
                        Log("key= " + entry.getKey() + " and value= " + entry.getValue());
                            CopyUpdataFile.getInstance().copyFileToPath(entry.getKey(), entry.getValue(), new CopyUpdataFile.CppyFileCallBack() {
                                @Override
                                public void CopyOver(boolean over, String error) {
                                    eventBus.post(new CopyBean("over",upMediaDirToPath+upXML));
                                    Log("CopyMusicOver:" + over + " error info" + error);
                                    if (!over) {
                                        errorTag = true;
                                    }
                                }
                                @Override
                                public void startCopy() {
                                    eventBus.post(new CopyBean("start","start"));
                                }
                                @Override
                                public void copyCompleted(long size) {
                                    eventBus.post(new CopyBean("completed",String.valueOf(size)));
                                }
                            });
                            if (!isCopy) {
                                errorTag=true;
                                return;
                            }
                        }
                } catch (Exception e) {
                    errorTag = true;
                    LogUtils.e(e);
                } finally {
                    final boolean finalErrorTag = errorTag;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (CompressFileLock.isLocked()) {
                                CompressFileLock.unlock();
                            }
                            isCopy = false;
                            if (finalErrorTag) {
                                eventBus.post(new CopyBean("isCopy",String.valueOf(false)));
                            } else {
                                eventBus.post(new CopyBean("isCopy",String.valueOf(true)));
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public static class CopyBean{
        public String type;
        public String info;

        public CopyBean() {
        }

        public CopyBean(String type, String info) {
            this.type = type;
            this.info = info;
        }

        @Override
        public String toString() {
            return "CopyBean{" +
                    "type='" + type + '\'' +
                    ", info='" + info + '\'' +
                    '}';
        }
    }
    /**
     * 去除 路径前 包含的 file:// 字符串
     *
     * @param str
     * @param clearStr
     * @return
     */
    private static String clearStr(String str, String clearStr) {
        byte[] bytes = str.getBytes();
        byte[] clearStrBytes = clearStr.getBytes();
        byte[] StrTitleBytes = new byte[clearStrBytes.length];
        System.arraycopy(bytes, 0, StrTitleBytes, 0, StrTitleBytes.length);
        byte[] newByteTmp = null;
        if (Arrays.equals(clearStrBytes, StrTitleBytes)) {
            newByteTmp = new byte[bytes.length - clearStrBytes.length];
            System.arraycopy(bytes, clearStrBytes.length, newByteTmp, 0, newByteTmp.length);
        }
        if (newByteTmp != null) {
            return new String(newByteTmp);
        } else {
            return str;
        }
    }
    private static void Log(String data){
        Log.i("UpdataManager", "Log: "+data);
    }



}
