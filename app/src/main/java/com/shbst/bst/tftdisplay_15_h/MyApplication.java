package com.shbst.bst.tftdisplay_15_h;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.shbst.androiddevicesdk.DeviceSDK;

import java.util.Locale;


/**
 * Created by hegang on 2016-12-07.
 */
public class MyApplication extends Application {

    public static MyApplication myApplication;
    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;

        DeviceSDK.getInstance().init(getApplicationContext());
    }
    /* 安装apk */
    public static void installApk(Context context, String fileName) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.parse("file://" + fileName),
                "application/vnd.android.package-archive");
        context.startActivity(intent);
    }


    /* 卸载apk */
    public static void uninstallApk(Context context, String packageName) {
        Uri uri = Uri.parse("package:" + packageName);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        context.startActivity(intent);
    }


}
