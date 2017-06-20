package com.shbst.bst.tftdisplay_15_h.MqttService;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

/**
 * Created by hegang on 2016-12-10.
 */
public class MqttUtils {
    Context context;

    public MqttUtils(Context context) {
        this.context = context;
    }

    /**
     * 获取手机声音
     * @return
     */
    public int getAudioManager(){
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max   = mAudioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
        int current = mAudioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
        return max;
    }
    /**
     * 获取屏幕的亮度
     */
    public int getScreenBrightness() {
        int value = 0;
        ContentResolver cr = context.getContentResolver();
        try {
            value = Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {

        }
        return value;
    }
    /**
     * 获得锁屏时间  毫秒
     */
    public int getScreenOffTime(){
        int screenOffTime=0;
        try{
            screenOffTime = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        }
        catch (Exception localException){
        }
        return screenOffTime;
    }
}
