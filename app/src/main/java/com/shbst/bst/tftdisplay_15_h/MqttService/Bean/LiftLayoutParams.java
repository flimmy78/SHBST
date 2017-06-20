package com.shbst.bst.tftdisplay_15_h.MqttService.Bean;

import java.io.Serializable;

/**
 * Created by hegang on 2016-12-11.
 */
public class LiftLayoutParams implements Serializable {
    public String type;
    public String width;
    public String height;
    public String x;
    public String y;

    @Override
    public String toString() {
        return "LiftLayoutParams{" +
                "type='" + type + '\'' +
                ", width='" + width + '\'' +
                ", height='" + height + '\'' +
                ", x='" + x + '\'' +
                ", y='" + y + '\'' +
                '}';
    }

    public LiftLayoutParams() {
    }

    public LiftLayoutParams(String type, String width, String height, String x, String y) {
        this.type = type;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
}
