package com.kezy.downloadlib.downloader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadEngine;
import com.kezy.downloadlib.impls.IDownloadStatusListener;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;



/**
 *
 */
public class DownloadServiceManage implements IDownloadEngine {

    private static final String TAG = "--------msg_d_m";
    private boolean mConnected = false;

    private Context mContext;

    private String onlyKey;

    @Nullable
    private DownloadService mDownloadService;


    public UpdateHandler mHandler;

    private List<IDownloadStatusListener> mDownloadServiceStatueListeners = new CopyOnWriteArrayList<>();

    public void removeDownloadStatueListener(IDownloadStatusListener l) {
        if (mDownloadServiceStatueListeners != null) {
            mDownloadServiceStatueListeners.remove(l);
        }
    }

    public void removeAllListener() {
        if (mDownloadServiceStatueListeners != null) {
            mDownloadServiceStatueListeners.clear();
        }
    }

    public DownloadServiceManage(Context context) {
        mHandler = new UpdateHandler(context);
        if (context != null) {
            mContext = context.getApplicationContext();
            init(mContext);
        }
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        Log.e("----------", " -------- init ");
        context.bindService(new Intent(context, DownloadService.class), mConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void bindStatusListener(IDownloadStatusListener listener) {
        if (!mDownloadServiceStatueListeners.contains(listener)) {
            mDownloadServiceStatueListeners.add(listener);
        }
    }

    DownloadRunnable downloadRunnable;

    @Override
    public void startDownload(Context context,String downloadUrl, String onlyKey) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        this.onlyKey = onlyKey;
        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }
        if (downloadRunnable == null) {
            downloadRunnable = new DownloadRunnable(context, downloadUrl, mHandler);
            downloadRunnable.isRunning = true;
        } else {
            downloadRunnable.isRunning = true;
            downloadRunnable.downloadStatus = DownloadInfo.Status.DOWNLOADING;
        }

        mDownloadService.threadPool.submit(downloadRunnable);
    }

    @Override
    public void pauseDownload(Context context, String onlyKey) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        Log.e("--------msg", " ------------ pauseDownload  111111111 ----------- " + downloadRunnable.isRunning);
        downloadRunnable.isRunning = false;
        downloadRunnable.downloadStatus = DownloadInfo.Status.STOPPED;
        Log.e("--------msg", " ------------ pauseDownload  222222222 ----------- " + downloadRunnable.isRunning);
    }

    @Override
    public void continueDownload(Context context, String onlyKey) {

        startDownload(context, null, onlyKey);
    }

    @Override
    public void deleteDownload(Context context, String onlyKey) {

        downloadRunnable.downloadStatus = DownloadInfo.Status.DELETE;
        downloadRunnable.isRunning = false;
        String filePath = downloadRunnable.savePath;
        if (filePath != null && new File(filePath).exists()) {
            new File(filePath).delete();
        }
        File tempDownloadPath = DownloadUtils.getTempDownloadPath(downloadRunnable.savePath, downloadRunnable.name);
        if (tempDownloadPath != null && tempDownloadPath.exists()) {
            tempDownloadPath.delete();
        }
    }

    @Override
    public void destroy() {
        if (mDownloadService != null) {
            mDownloadService.unbindService(mConn);
        }
        removeAllListener();
        unBindDownloadService(mContext);
    }


    public void unBindDownloadService(Context context) {
        try {
            if (mConnected) {
                context.unbindService(mConn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnected = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("----------", " -------- onServiceConnected ");
            if (service instanceof DownloadService.Binder) {
                mConnected = true;
                mDownloadService = ((DownloadService.Binder) service).getService();
            }
        }
    };

    private boolean checkConnectionStatus(Context context) {
        if (!mConnected || mDownloadService == null) {
            init(context);
            return false;
        }
        return true;
    }






    public static final int DOWN_OK = 1001;
    public static final int DOWN_ERROR = 1002;
    public static final int DOWN_START = 1003;
    public static final int DOWNLOAD_ING = 1004;
    public static final int REQUEST_TIME_OUT = 1005;
    public static final int HANDLER_PAUSE = 1006;
    public static final int HANDLER_REMOVE = 1007;

    public class UpdateHandler extends Handler {

        private Context mContext;
        public UpdateHandler(Context context) {
            mContext = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.i("-------------msg", " ------- handleMessage : " + msg.toString());


            switch (msg.what) {
                case DOWN_OK:
                    Log.i("-------------msg", " ------- 2222 下载完成 task URL : " + downloadRunnable);
                    // 下载完成，点击安装
                    Log.e("----------msg", " ------- 下载完成22 ----fileName   " + downloadRunnable.savePath);
                    handleDownloadSuccess();
                    handleInstallBegin();
                    break;

                case DOWN_START:
                    Log.e("----------msg", " ------- DOWN_START ----   ");
                    handleStart(downloadRunnable.tempSize != 0, msg.arg1);
                    break;
                case DOWN_ERROR:
                    Log.e("----------msg", " ------- err ----   ");
                    handleError();
                    break;
                case DOWNLOAD_ING:
                    Log.e("----------msg", " ------- ing ----   " + msg.arg2);
                    handleProgress(msg.arg1, msg.arg2);
                    break;
                case REQUEST_TIME_OUT:
                    Log.e("----------msg", " ------- REQUEST_TIME_OUT ----   ");
                    handleError();
                    break;
                case HANDLER_PAUSE:
                    Log.e("----------msg", " ------- HANDLER_PAUSE ----   ");
                    handlePause();
                    break;
                case HANDLER_REMOVE:
                    Log.e("----------msg", " ------- HANDLER_REMOVE ----   ");
                    handleRemove();
                    break;
                default:
                    break;
            }
        }
    }


    private void handleRemove() {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onRemove(onlyKey);
        }
        Log.d(TAG, "handleRemove   " + onlyKey);
    }

    private void handlePause() {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onPause(onlyKey);
        }
        Log.d(TAG, "handlePause   " + onlyKey);
    }

    private void handleProgress(long totalSize, int progress) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onProgress(onlyKey,totalSize, progress);
        }
        Log.d(TAG, "handleProgress   " + onlyKey);
    }

    private void handleError() {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onError(onlyKey);
        }
        Log.d(TAG, "handleError   " + onlyKey);
    }

    private void handleDownloadSuccess() {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onSuccess(onlyKey, downloadRunnable.savePath,  downloadRunnable.name);
        }
        Log.d(TAG, "handleDownloadSuccess   " + onlyKey);
    }

    private void handleStart(boolean isRestart, int totalSize) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onStart(onlyKey, isRestart, totalSize);
        }

        Log.d(TAG, "handleStart   " + onlyKey);
    }

    private void handleInstallBegin() {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onInstallBegin(onlyKey);
        }
    }
}
