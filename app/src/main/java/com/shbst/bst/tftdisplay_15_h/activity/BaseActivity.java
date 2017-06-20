package com.shbst.bst.tftdisplay_15_h.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.shbst.bst.tftdisplay_15_h.NetBroadcastReceiver;
import com.shbst.bst.tftdisplay_15_h.utils.Constants;
import com.shbst.bst.tftdisplay_15_h.utils.PrefUtils;

/**
 * Created by hegang on 2017-02-20.
 */
public class BaseActivity extends AppCompatActivity{

    private static final String TAG = "NetBroadcastReceiver";
    public static NetBroadcastReceiver.NetEvevt evevt;
    private int DISPLAY_INDEX_1 = 1;
    private int DISPLAY_INDEX_2 = 2;
    private int DISPLAY_INDEX_3 = 3;
    /**
     * 网络类型
     */
    private int netMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreenAndHidMenu(this);
        PrefUtils.getString(this, Constants.rDisplay, String.valueOf(getDisplay()));
    }

    /**
     * 获取屏幕分辨率
     */
    public int getDisplay( ){

        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        int W1 = mDisplayMetrics.widthPixels;
        int H2 = mDisplayMetrics.heightPixels;

        return DISPLAY_INDEX_1;

    }
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //设置布局后调用这个方法
        hideMenu(this.getWindow());
        //当虚拟按键再次弹出时设置隐藏
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                hideMenu(getWindow());
            }
        });
    }

    /**
     * 设置全屏并隐藏底部导航栏菜单
     */
    public void setFullScreenAndHidMenu(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //一下为设置虚拟按键为三个小圆点
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        activity.getWindow().setAttributes(params);
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    public void hideMenu(Window window) {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = window.getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    /**
     *  隐藏虚拟按键
     */
    public void hideBar() {
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)hideBar();
    }
}
