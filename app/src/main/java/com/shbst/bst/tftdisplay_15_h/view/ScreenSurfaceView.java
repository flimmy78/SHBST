package com.shbst.bst.tftdisplay_15_h.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by hegang on 2017-02-21.
 */
public class ScreenSurfaceView extends SurfaceView implements SurfaceHolder.Callback, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnPreparedListener {
    private static final String TAG = "ScreenSurfaceView";
    private Context mContext;
    private SurfaceHolder holder;
    private MediaPlayer player;
    private AnimationDrawable anim;
    private int runTime = 3000;
    public ScreenSurfaceView(Context context) {
        super(context);
        mContext = context;
    }

    public ScreenSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public ScreenSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    //加载图片
    private Drawable showImage(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inPurgeable = true;
        options.inSampleSize = 2;//图片宽高都为原来的二分之一，即图片为原来的四分之一
        options.inInputShareable = true;
        options.inJustDecodeBounds = false;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(imagePath, options);
        } catch (OutOfMemoryError e) {

        }
        BitmapDrawable drawable = new BitmapDrawable(bitmap);
        return drawable;
    }

    /**
     * 设置显示为图片
     *
     * @param path 图片路径
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setPicturesView(String path) {
        try {
            if (player.isPlaying()) {
                //如果播放就先暂停 ，重置播放器
                player.stop();
                player.reset();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        setBackground(null);

        Drawable drawable = showImage(path);

        setBackground(drawable);

    }

    /**
     * 设置显示为视频view
     */
    public void setHolderView() {
        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //下面开始实例化MediaPlayer对象
        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnPreparedListener(this);
    }

    /**
     * 启动轮播
     * @param imageList 图片路径
     * @param runTime   间隔时间
     */
    public void setRunImageList( List<String> imageList,int runTime){
        try {
            if (player.isPlaying()) {
                //如果播放就先暂停 ，重置播放器
                player.stop();
                player.reset();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        this.runTime = runTime;
        if(anim != null){
            tryRecycleAnimationDrawable(anim);
            anim = new AnimationDrawable();
            System.gc();
            runFarm(imageList);
        }else{
            anim = new AnimationDrawable();
            runFarm(imageList);
        }
    }

    /**
     * 设置图片轮播效果
     * @param pathList 图片路径
     */
    private void runFarm(List<String> pathList) {
        if (pathList != null) {
            for (int i = 0; i < pathList.size(); i++) {
                Drawable drawable = showImage(pathList.get(i));
                anim.addFrame(drawable, runTime);
            }
            anim.setOneShot(false);
            setBackgroundDrawable(anim);
            anim.start();
        } else {
            anim.stop();
            anim = null;
        }
    }

    /**
     * AnimationDrawable动画图片资源回收
     *
     * @param animationDrawable 动画
     */
    private void tryRecycleAnimationDrawable(AnimationDrawable animationDrawable) {
        if (animationDrawable != null) {
            animationDrawable.stop();
            for (int i = 0; i < animationDrawable.getNumberOfFrames(); i++) {
                Drawable frame = animationDrawable.getFrame(i);
                if (frame instanceof BitmapDrawable) {
                    ((BitmapDrawable) frame).getBitmap().recycle();
                }
                frame.setCallback(null);
            }
            animationDrawable.setCallback(null);
        }
    }

    /**
     * 设置视频文件路径
     *
     * @param dataPath 视频文件路径
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setVideoPath(String dataPath) {
        try {
            // 判断视频是否在播放
            if (player.isPlaying()) {
                //如果播放就先暂停 ，重置播放器
                player.stop();
                player.reset();
            }
//            player = MediaPlayer.create(mContext, Uri.parse(dataPath));
            //设置资源路径
            setBackground(null);
            player.setDataSource(dataPath);
            // 准备
            player.prepare();
            //开始播放
            player.start();

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 结束正在播放的视频
     * 隐藏显示的图片
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void stopSurfacView() {
        // 判断视频是否在播放
        if (player.isPlaying()) {
            //如果播放就先暂停 ，重置播放器
            player.stop();
            player.reset();
        }
        //设置资源路径
        setBackground(null);
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //在这里我们指定MediaPlayer在当前的Surface中进行播放
        player.setDisplay(holder);
        //在指定了MediaPlayer播放的容器后，我们就可以使用prepare或者prepareAsync来准备播放了
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        player.start();

    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        // 当一些特定信息出现或者警告时触发
        // 判断视频是否在播放
        if (player.isPlaying()) {
            //如果播放就先暂停 ，重置播放器
            player.stop();
            player.reset();
        }
        switch (i) {

            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                break;
            case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                break;
            case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        player.start();
    }
}
