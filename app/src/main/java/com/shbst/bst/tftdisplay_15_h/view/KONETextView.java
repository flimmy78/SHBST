package com.shbst.bst.tftdisplay_15_h.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.shbst.bst.tftdisplay_15_h.utils.Constants;


/**
 * KONE 字体
 * Created by hegang on 2016-11-21.
 */
public class KONETextView extends TextView {
    public KONETextView(Context context) {
        super(context);
        init(context);
    }

    public KONETextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public KONETextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    private void init(Context context){
        AssetManager assetManager = context.getAssets();
        Typeface font = Typeface.createFromAsset(assetManager, Constants.KONE_TEXTVIEW);
        setTypeface(font);
        setTextColor(Color.WHITE);
    }

}
