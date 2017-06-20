package com.shbst.bst.tftdisplay_15_h;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;


/**
 * Created by Administrator on 2016/7/22.
 * 开机启动APP
 */
public class StartUpReceiver extends BroadcastReceiver {
    PackageManager packageManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2=new Intent(context,MainActivity.class);
        intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        packageManager=context.getPackageManager();
        intent2 = packageManager.getLaunchIntentForPackage("com.shbst.bst.tftdisplay_15_h");
        context.startActivity(intent2);
    }
}
