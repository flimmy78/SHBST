package com.shbst.bst.tftdisplay_15_h.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.shbst.bst.tftdisplay_15_h.utils.Constants;


/**
 * 滚动字幕view
 * Created by hegang on 2016-11-21.
 */
public class KONEScrollingText extends TextView{

    public KONEScrollingText(Context context) {
        super(context);
        createView(context);
    }

    public KONEScrollingText(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public KONEScrollingText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        createView(context);
    }

    private void createView(Context context) {
        AssetManager assetManager = context.getAssets();
        Typeface font = Typeface.createFromAsset(assetManager, Constants.KONE_TEXTVIEW);
        setTypeface(font);
        setTextColor(Color.WHITE);
        requestFocus();
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        if (focused) {
            super.onWindowFocusChanged(focused);
        }
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}