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

    @Nullable
    private DownloadService mDownloadService;

    private DownloadInfo mInfo;

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
    public void bindDownloadInfo(DownloadInfo info) {
        Log.e("----------", " -------- bindDownloadInfo " + mDownloadService);
        mInfo = info;
    }

    @Override
    public DownloadInfo getInfo() {
        return mInfo;
    }

    @Override
    public void bindStatusListener(IDownloadStatusListener listener) {
        if (!mDownloadServiceStatueListeners.contains(listener)) {
            mDownloadServiceStatueListeners.add(listener);
        }
    }


    @Override
    public void startDownload(Context context) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }
        mInfo.isRunning = true;
        mInfo.status = DownloadInfo.Status.DOWNLOADING;
        mInfo.retryCount = 0;
        mDownloadService.threadPool.submit(new DownloadRunnable(context, mInfo, mHandler));
    }

    @Override
    public void pauseDownload(Context context) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        mInfo.isRunning = false;
        mInfo.status = DownloadInfo.Status.STOPPED;
    }

    @Override
    public void continueDownload(Context context) {

        startDownload(context);
    }

    @Override
    public void deleteDownload(Context context) {

        mInfo.status = DownloadInfo.Status.DELETE;
        mInfo.isRunning = false;
        String filePath = mInfo.getSavePath();
        if (filePath != null && new File(filePath).exists()) {
            new File(filePath).delete();
        }
        File tempDownloadPath = DownloadUtils.getTempDownloadPath(mInfo);
        if (tempDownloadPath != null && tempDownloadPath.exists()) {
            tempDownloadPath.delete();
        }
    }


    @Override
    public void installApk(Context context) {
        if (mDownloadService != null) {
            DownloadUtils.installApk(mContext, mInfo.getSavePath());
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
            DownloadInfo task = (DownloadInfo) msg.obj;
            if (task == null) {
                return;
            }

            if (mInfo != null) {
                mInfo.status = task.status;
                mInfo.isRunning = task.isRunning;
                mInfo.progress = task.progress;
                mInfo.totalSize = task.totalSize;
                mInfo.tempSize = task.tempSize;
            }

            switch (msg.what) {
                case DOWN_OK:
                    Log.i("-------------msg", " ------- 2222 下载完成 task URL : " + task.url);
                    // 下载完成，点击安装
                    Log.e("----------msg", " ------- 下载完成22 ----fileName   " + task.getSavePath());
                    DownloadUtils.installApk(mContext, task.getSavePath());
                    handleDownloadSuccess(mInfo);
                    handleInstallBegin(mInfo);
                    break;

                case DOWN_START:
                    Log.e("----------msg", " ------- DOWN_START ----   ");
                    handleStart(mInfo, task.tempSize != 0);
                    break;
                case DOWN_ERROR:
                    Log.e("----------msg", " ------- err ----   ");
                    handleError(mInfo);
                    break;
                case DOWNLOAD_ING:
                    Log.e("----------msg", " ------- ing ----   " + task.progress);
                    handleProgress(mInfo);
                    break;
                case REQUEST_TIME_OUT:
                    Log.e("----------msg", " ------- REQUEST_TIME_OUT ----   ");
                    handleError(mInfo);
                    break;
                case HANDLER_PAUSE:
                    Log.e("----------msg", " ------- HANDLER_PAUSE ----   ");
                    handlePause(mInfo);
                    break;
                case HANDLER_REMOVE:
                    Log.e("----------msg", " ------- HANDLER_REMOVE ----   ");
                    handleRemove(mInfo);
                    break;
                default:
                    break;
            }
        }
    }


    private void handleRemove(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onRemove(info.onlyKey());
        }
        Log.d(TAG, "handleRemove   " + info);
    }

    private void handlePause(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onPause(info.onlyKey());
        }
        Log.d(TAG, "handlePause   " + info);
    }

    private void handleProgress(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onProgress(info.onlyKey(), info.progress);
        }
        Log.d(TAG, "handleProgress   " + info);
    }

    private void handleError(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onError(info.onlyKey());
        }
        Log.d(TAG, "handleError   " + info);
    }

    private void handleDownloadSuccess(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onSuccess(info.onlyKey(), info.path);
        }
        Log.d(TAG, "handleDownloadSuccess   " + info);
    }

    private void handleStart(DownloadInfo info, boolean isRestart) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onStart(info.onlyKey(), isRestart, info.totalSize);
        }

        Log.d(TAG, "handleStart   " + info);
    }

    private void handleInstallBegin(DownloadInfo info) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onInstallBegin(info.onlyKey());
        }
    }
}
