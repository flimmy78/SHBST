package com.shbst.bst.tftdisplay_15_h.BusParameter;

public class IotDownload {
    public boolean cmdType = true;  //true- 通知Http下载  false - 通知UiModuel下载完成
    public int resType;
    public int index;
    public String resPath;

    public IotDownload(int resType, String resPath) {
        this.resType = resType;
        this.index = 0;
        this.resPath = resPath;
    }

    public IotDownload(int resType, int index, String resPath) {
        this.resType = resType;
        this.index = index;
        this.resPath = resPath;
    }
}
