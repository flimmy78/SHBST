package com.shbst.bst.tftdisplay_15_h.utils;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zhouwenchao on 复制文件
 */
public class CopyUpdataFile {
    private ReentrantLock CompressFileLock = new ReentrantLock();   //  锁,  防止多多线程访问造成文件异常
    private String errorTag = "";  // 默认没有异常
    //    private Handler handler = new Handler(Looper.myLooper());
    private final String enter = "\n";
    public final String error0 = "发生了未知异常";
    public final String error1 = "无读写文件权限或文件不存在";
    public final String error2 = "有正在复制的线程，请等待复制完成";
    public final String error3 = "创建文件或文件夹异常";
    public final String error4 = "复制文件流异常";
    public final String error5 = "关闭文件流异常";

    public static long fileSize = 0;  //文件大小
    private CopyUpdataFile() {
    }

    private static class CopyFileIns {
        private static final CopyUpdataFile COPY_FILE = new CopyUpdataFile();
    }

    public static CopyUpdataFile getInstance() {
        return CopyFileIns.COPY_FILE;
    }

    /**
     * 将异常信息进行拼接
     *
     * @param error
     */
    private void apendError(String error) {
        if (TextUtils.isEmpty(errorTag)) {
            errorTag += error;
        } else {
            errorTag += enter;
            errorTag += error;
        }
    }


    public synchronized void copyFileToPath(String path, String toPath, CppyFileCallBack callBack) {
        if (!CompressFileLock.isLocked()) {
            CompressFileLock.lock();
            errorTag = "";
        } else {
            errorTag = "";
            apendError(error2);
            if (callBack != null) {
                callBack.CopyOver(false, errorTag);
            }
        }
        final File sourceFile = new File(path);

        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (hasSDCard && sourceFile.exists()) {  // 有权限且文件存在
            try {
                long filesInDir = getTotalSizeOfFilesInDir(sourceFile); //复制文件总大小
                Log("要复制的文件大小："+filesInDir);
                fileSize = filesInDir;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            if (callBack != null) {

                callBack.startCopy();
            }
            startCopy(path, toPath, callBack);
        } else {
            if (CompressFileLock.isLocked()) {
                CompressFileLock.unlock();
            }
            apendError(error1);
            if (callBack != null) {
                callBack.CopyOver(false, errorTag);
            }
        }
    }

    private void startCopy(String path, String toPath, CppyFileCallBack callBack) {

        boolean copyOver = false;
        try {
            File sourceFile = new File(path);
            if (sourceFile.isDirectory()) {  // 如果是文件夹，迭代
                Log.i("sourceFile", "startCopy: "+sourceFile.length()+"   getName  "+sourceFile.getName());
                File[] files = sourceFile.listFiles();
                for (int i = 0; i < files.length; i++) {
                    copyDir(files[i].getAbsolutePath(), toPath + File.separator,callBack);  // 第一次不用加Name
                }
            } else {
                copyFile(path, toPath + File.separator,callBack); // 如果是文件的话，直接传入路径复制文件
            }
            copyOver = true;
        } catch (FileNotFoundException e) {
            apendError(error4);
            copyOver = false;
        } catch (IOException e) {
            apendError(error4);
            copyOver = false;
        } catch (Exception e) {
            apendError(error0);
            copyOver = false;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException | NullPointerException e) {
                apendError(error5);
            }
            if (CompressFileLock.isLocked()) {
                CompressFileLock.unlock();
            }
            if (callBack != null) {
                callBack.CopyOver(copyOver, errorTag);
            }
        }
    }


    /**
     * 复制文件夹 迭代
     *
     * @param path   原始路径
     * @param toPath 复制到哪里。。
     */
    private void copyDir(String path, String toPath, CppyFileCallBack callBack) throws IOException {
        File sourceFile = new File(path);
        Log.i("sourceFile", "copyDir:sourceFile  "+sourceFile);
        if (sourceFile.isDirectory()) {  // 如果是文件夹，迭代
            File[] files = sourceFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                copyDir(files[i].getAbsolutePath(), toPath + sourceFile.getName() + File.separator,callBack);
            }
        } else {
            copyFile(path, toPath,callBack); // 如果是文件的话，直接传入路径复制文件
        }
    }

    FileInputStream inputStream = null;
    FileOutputStream outputStream = null;

    private void copyFile(String path, String toPath, CppyFileCallBack callBack) throws IOException {
        File file = new File(path);
        File toFile = new File(toPath + file.getName());

        boolean hasSDCard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (hasSDCard && file.exists()) {  // 有权限且文件存在
            if (!toFile.exists()) {  //  如果文件不存在
                try {
                    File dir = new File(toFile.getParent());
                    dir.mkdirs();
                    toFile.createNewFile();
                } catch (RuntimeException e) {
                    apendError(error3);
                } catch (IOException e) {
                    apendError(error3);
                }
            } else {
                deleteFileSafely(toFile);
                copyFile(path, toPath,callBack);
                return;
            }
            inputStream = new FileInputStream(file);
            outputStream = new FileOutputStream(toFile);
            byte[] buff = new byte[4096];
            int index;
            long fileIndex = 0;

            while ((index = inputStream.read(buff)) > 0) {
                outputStream.write(buff, 0, index);
                fileIndex = fileIndex+index;
                if(fileSize != 0){
                    if(callBack != null){
                        callBack.copyCompleted((long) (((float)fileIndex/(float)fileSize)*100));
                    }
                }
            }
        } else {
            apendError(error1);
        }
    }



    /**
     * @param file 要删除的文件
     */
    private boolean deleteFileSafely(File file) {
        if (file != null && file.exists()) {
            File tmp = getTmpFile(file, System.currentTimeMillis(), -1);
            if (file.renameTo(tmp)) {    // 将源文件重命名
                return tmp.delete();  //  删除重命名后的文件
            } else {
                return file.delete();
            }
        }
        return false;
    }


    private File getTmpFile(File file, long time, int index) {
        File tmp;
        if (index == -1) {
            tmp = new File(file.getParent() + File.separator + time);
        } else {
            tmp = new File(file.getParent() + File.separator + time + "(" + index + ")");
        }
        if (!tmp.exists()) {
            return tmp;
        } else {
            return getTmpFile(file, time, index >= 1000 ? index : ++index);
        }
    }


    public interface CppyFileCallBack {
        void startCopy();

        void copyCompleted(long size);

        void CopyOver(boolean over, String error);

    }


    private  long getTotalSizeOfFilesInDir(final File file)
            throws InterruptedException, ExecutionException, TimeoutException {
        final ExecutorService service = Executors.newFixedThreadPool(100);
        try {
            long total = 0;
            final List<File> directories = new ArrayList<File>();
            directories.add(file);
            while (!directories.isEmpty()) {
                final List<Future<SubDirectoriesAndSize>> partialResults = new ArrayList<Future<SubDirectoriesAndSize>>();
                for (final File directory : directories) {
                    partialResults.add(service
                            .submit(new Callable<SubDirectoriesAndSize>() {
                                public SubDirectoriesAndSize call() {
                                    return getTotalAndSubDirs(directory);
                                }
                            }));
                }
                directories.clear();
                for (final Future<SubDirectoriesAndSize> partialResultFuture : partialResults) {
                    final SubDirectoriesAndSize subDirectoriesAndSize = partialResultFuture
                            .get(100, TimeUnit.SECONDS);
                    directories.addAll(subDirectoriesAndSize.subDirectories);
                    total += subDirectoriesAndSize.size;
                }
            }
            return total;
        } finally {
            service.shutdown();
        }
    }
     class SubDirectoriesAndSize {

        private  List<File> subDirectories = null;
        private  long size = 0;

        public  SubDirectoriesAndSize(final long totalSize,
                                      final List<File> theSubDirs) {
            size = totalSize;
            subDirectories = Collections.unmodifiableList(theSubDirs);
        }
    }

    private  SubDirectoriesAndSize getTotalAndSubDirs(final File file) {
        long total = 0;
        final List<File> subDirectories = new ArrayList<File>();
        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            if (children != null)
                for (final File child : children) {
                    if (child.isFile())
                        total += child.length();
                    else
                        subDirectories.add(child);
                }
        }
        return new SubDirectoriesAndSize(total, subDirectories);
    }

    private static void Log(String data){
        Log.i("CopyUpdataFile", "Log: "+data);
    }
}
