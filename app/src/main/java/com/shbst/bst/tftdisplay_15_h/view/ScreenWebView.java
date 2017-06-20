package com.shbst.bst.tftdisplay_15_h.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by hegang on 2017-02-22.
 */
public class ScreenWebView extends WebView {
    public interface WebViewDisplayOver{
        void After();
    }

    WebViewDisplayOver df;

    public void setPlayFinish(WebViewDisplayOver playFinish) {
        this.df = playFinish;
    }
    public ScreenWebView(Context context) {
        super(context);
    }

    public ScreenWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScreenWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        df.After();
    }
}
