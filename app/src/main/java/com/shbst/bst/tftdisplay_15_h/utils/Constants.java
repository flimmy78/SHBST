package com.shbst.bst.tftdisplay_15_h.utils;

import android.os.Environment;

/**
 * Created by hegang on 2016-11-21.
 */
public class Constants {
    // 获取文件根目录
    public static final String ThemePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    // 视频资源目录
    public static final String VIDEO_PATH = "/Movies/";
    //web 端下发资源目录
    public static final String RESOURCE_FILE = "/Resource/";
    // 默认播放视频
    public static final String DefaultPath = ThemePath+VIDEO_PATH+"KONE.mp4";
    // 视频横竖显d
    public static final String CrossScreen = "0";  //  横竖显示   0 横显  1 竖显
    // 字体格式
    public static final String KONE_TEXTVIEW = "KONE Information_v12.otf";
    //媒体区域播放的文件类型
    public static final String rType = "rType";
    // 媒体区域播放的文件路径
    public static final String rPath = "rPath";

    public static final String rDisplay = "displayNumber";

    public static final String WebUrl = "WebUrl";
    public static final String PICTURES = "picture";
    public static final String VIDEO = "video";
}
