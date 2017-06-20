package com.shbst.bst.tftdisplay_15_h.MqttService.Bean;

/**
 * Created by hegang on 2016-12-10.
 */
public class MqttParamsBean {
    private static MqttParamsBean mqttParamsBean = null;
    public static MqttParamsBean getMqttParamsBean() {
        if (mqttParamsBean == null) {
            mqttParamsBean = new MqttParamsBean();
        }
        return mqttParamsBean;
    }
    public String layout;
    public String displaytype;
    public String fullscreen;
    public String screentime;
    public String sybrightness;
    public String brightness;
    public String volume;
    public String uid;
    public int typ;
    public int mDate;
    public int mText;
    public int mScrolltext;
    public int imageinterval;
    public int datemode;
    public int timemode;
    @Override
    public String toString() {
        return "MqttParamsBean{" +
                "layout='" + layout + '\'' +
                ", displaytype='" + displaytype + '\'' +
                ", fullscreen='" + fullscreen + '\'' +
                ", screentime='" + screentime + '\'' +
                ", sybrightness='" + sybrightness + '\'' +
                ", brightness='" + brightness + '\'' +
                ", volume='" + volume + '\'' +
                ", uid='" + uid + '\'' +
                ", typ=" + typ +
                ", mDate=" + mDate +
                ", mText=" + mText +
                ", mScrolltext=" + mScrolltext +
                '}';
    }
}
