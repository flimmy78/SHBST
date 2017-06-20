package com.shbst.bst.tftdisplay_15_h.utils;

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import com.shbst.bst.tftdisplay_15_h.R;

import java.io.File;
import java.io.IOException;

/**
 * Created by yuyh on 2015/11/4.
 */
public class FileUtils {

    /**
     * 检查SD卡是否存在
     *
     * @return 存在返回true，否则返回false
     */
    public static boolean isSdcardReady() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        return sdCardExist;
    }

    /**
     * 获得SD路径
     *
     * @return
     */
    public static String getSdcardPath() {
        return Environment.getExternalStorageDirectory().toString() + File.separator;
    }

    /**
     * 获取缓存路径
     *
     * @param context
     * @return
     */
    public static String getCachePath(Context context) {
        File cacheDir = context.getCacheDir();//文件所在目录为getFilesDir();
        return cacheDir.getPath() + File.separator;
    }

    /**
     * 根据文件路径 递归创建文件
     *
     * @param file
     */
    public static void createDipPath(String file) {
        String parentFile = file.substring(0, file.lastIndexOf("/"));
        File file1 = new File(file);
        File parent = new File(parentFile);
        if (!file1.exists()) {
            parent.mkdirs();
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
/**
 //     * 获取手机内视频文件目录
 //     */
//    private List<String> sdCardVideo() {
//        videoPathList = new ArrayList<>();
//        File file = new File(themePath + VIDEO_PATH);
//        File[] subFile = file.listFiles();
//        for (int iFileLength = 0; iFileLength < subFile.length; iFileLength++) {
//            // 判断是否为文件夹
//            if (!subFile[iFileLength].isDirectory()) {
//                String filename = subFile[iFileLength].getName();
//                // 判断是否为MP4结尾
//                if (filename.trim().toLowerCase().endsWith(".mp4") || filename.trim().toLowerCase().endsWith(".rmvb")) {
//                    videoPathList.add(subFile[iFileLength].getPath());
//                    Log.i("MqttServiceData", "sdCardVideo: " + subFile[iFileLength].getPath());
//                }
//            }
//        }
//        return videoPathList;
//    }

    /**
     * 开启View闪烁效果
     */
    private void startFlick(final View view) {
        if (null == view) {
            return;
        }
        Animation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(500);
        alphaAnimation.setInterpolator(new LinearInterpolator());
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(alphaAnimation);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.clearAnimation();
                view.setBackgroundResource(R.drawable.kone_arrow_null);
            }
        }, 5000);
    }
//    private void netAnimation(boolean flag) {
//        if (flag) {
//            animation = AnimationUtils.loadAnimation(this, R.anim.rotate);
//            LinearInterpolator lin = new LinearInterpolator();
//            animation.setInterpolator(lin);
//            animation.setFillAfter(!animation.getFillAfter());
//            lift_network.startAnimation(animation);
//        } else {
//            if (animation != null) {
//                lift_network.clearAnimation();
//            }
//        }
//
//    }
}
