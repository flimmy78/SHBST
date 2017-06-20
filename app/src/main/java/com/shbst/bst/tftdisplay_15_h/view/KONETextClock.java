package com.shbst.bst.tftdisplay_15_h.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.shbst.bst.tftdisplay_15_h.utils.Constants;


/**
 * 时钟显示
 * Created by hegang on 2016-11-21.
 */
public class KONETextClock extends TextView {
    public KONETextClock(Context context) {
        super(context);
        init(context);
    }

    public KONETextClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public KONETextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化控件
     * @param context 上下文
     */
    private void init(Context context){
        AssetManager assetManager = context.getAssets();
        Typeface font = Typeface.createFromAsset(assetManager, Constants.KONE_TEXTVIEW);
        setTypeface(font);
        setTextColor(Color.WHITE);
    }


}
