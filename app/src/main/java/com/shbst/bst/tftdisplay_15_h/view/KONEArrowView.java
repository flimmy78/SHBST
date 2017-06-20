package com.shbst.bst.tftdisplay_15_h.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import com.shbst.bst.tftdisplay_15_h.R;

import java.io.File;


public class KONEArrowView extends View {
    private Bitmap mBitmap;
    private Bitmap mDirectionBitmap;
    private Paint mBitPaint;
    private Context mContext;
    private Rect mSrcRect, mDestRect;

    private int mViewWidth, mViewHeight;
    private int mImageWidth, mImageHeight;
    public int offsetRate;
    final static private float TOTALLSCROLLFREGMENT = 65.0f;


    public KONEArrowView(Context context) {
        super(context);
        mContext = context;
        initPaint();
        initBitmap();
        offsetRate = 0;
    }
    public KONEArrowView(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
        initPaint();
        initBitmap();
    }

    private void initPaint() {
        mBitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitPaint.setFilterBitmap(true);
        mBitPaint.setDither(true);
    }

    public void setArrowNull(){
        mDirectionBitmap = ((BitmapDrawable) ContextCompat.getDrawable(mContext, R.drawable.kone_arrow_null)).getBitmap();
        mImageWidth = mDirectionBitmap.getWidth();
        mImageHeight = mDirectionBitmap.getHeight();
        mBitmap = mDirectionBitmap;
        mSrcRect = new Rect(0, 0, mImageWidth, mImageHeight);
        mDestRect = new Rect(0, 0, mViewWidth, mViewHeight);
    }
    public void setArrowDown(String pictureUrl){
        File file = new File(pictureUrl);
        if (file.exists()) {
            mDirectionBitmap  = BitmapFactory.decodeFile(pictureUrl);
        }else{
            mDirectionBitmap = ((BitmapDrawable) ContextCompat.getDrawable(mContext, R.drawable.kone_arrow_down)).getBitmap();
        }
        mImageWidth = mDirectionBitmap.getWidth();
        mImageHeight = mDirectionBitmap.getHeight();
        mBitmap = mDirectionBitmap;
        mSrcRect = new Rect(0, 0, mImageWidth, mImageHeight);
        mDestRect = new Rect(0, 0, mViewWidth, mViewHeight);
    }
    public void setArrowUp(String pictureUrl){

        File file = new File(pictureUrl);
        if (file.exists()) {
            mDirectionBitmap  = BitmapFactory.decodeFile(pictureUrl);
        }else{
            mDirectionBitmap = ((BitmapDrawable) ContextCompat.getDrawable(mContext, R.drawable.kone_arrow_down)).getBitmap();
        }
        mImageWidth = mDirectionBitmap.getWidth();
        mImageHeight = mDirectionBitmap.getHeight();
        mBitmap = mDirectionBitmap;
        mSrcRect = new Rect(0, 0, mImageWidth, mImageHeight);
        mDestRect = new Rect(0, 0, mViewWidth, mViewHeight);
    }
    private void initBitmap() {
        if(mDirectionBitmap == null){
            setArrowNull();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
        mDestRect.right = mDestRect.left + w;
        mDestRect.bottom = mDestRect.top + h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(offsetRate <= 0||offsetRate >= TOTALLSCROLLFREGMENT){
            mSrcRect.left = mSrcRect.top = 0;
            mSrcRect.right = mImageWidth;
            mSrcRect.bottom = mImageHeight;

            mDestRect.left = mSrcRect.top = 0;
            mDestRect.right = mViewWidth;
            mDestRect.bottom = mViewHeight;

            canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, mBitPaint);
        }else{
            int mOffsetValue = (int)(offsetRate*mImageHeight/TOTALLSCROLLFREGMENT);
            int vOffsetValue = (int)((1.0 - offsetRate/TOTALLSCROLLFREGMENT)*mViewHeight);

            mSrcRect.left = mSrcRect.top = 0;
            mSrcRect.right = mImageWidth;
            mSrcRect.bottom = mOffsetValue;
            mDestRect.left = 0;
            mDestRect.top = vOffsetValue + 5;
            mDestRect.right = mViewWidth;
            mDestRect.bottom = mViewHeight;
            canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, mBitPaint);

            mSrcRect.left = 0;
            mSrcRect.top = mOffsetValue;
            mSrcRect.right = mImageWidth;
            mSrcRect.bottom = mImageHeight ;

            mDestRect.left = mDestRect.top = 0;
            mDestRect.right = mViewWidth;
            mDestRect.bottom = vOffsetValue;
            canvas.drawBitmap(mBitmap, mSrcRect, mDestRect, mBitPaint);
        }
    }


}