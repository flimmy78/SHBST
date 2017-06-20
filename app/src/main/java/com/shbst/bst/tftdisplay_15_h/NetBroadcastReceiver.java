package com.shbst.bst.tftdisplay_15_h;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.shbst.bst.tftdisplay_15_h.activity.BaseActivity;
import com.shbst.bst.tftdisplay_15_h.utils.NetUtil;

/**
 * Created by hegang on 2017-02-28.
 */
public class NetBroadcastReceiver extends BroadcastReceiver {
    public NetEvevt evevt = BaseActivity.evevt;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            int netWorkState = NetUtil.getNetWorkState(context);
            // 接口回调传过去状态的类型

            try {
                evevt.onNetChange(netWorkState);
                Log.i("fuck", "onReceive: "+netWorkState);
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }


    // 自定义接口
    public interface NetEvevt {
        public void onNetChange(int netMobile);
    }
}
