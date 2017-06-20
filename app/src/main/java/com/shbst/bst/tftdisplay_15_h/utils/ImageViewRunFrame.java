package com.shbst.bst.tftdisplay_15_h.utils;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by hegang on 2016-07-21.
 */
public class ImageViewRunFrame {

    public void runFrame(Context context, View view, int[] imageindex, int pic) {

        AnimationDrawable anim = new AnimationDrawable();
        for (int index = 0; index < pic; index++) {
            Drawable drawable = context.getResources().getDrawable(imageindex[index]);

            anim.addFrame(drawable, 1000);
        }
        anim.setOneShot(false);
        view.setBackgroundDrawable(anim);
        anim.start();
    }
}
