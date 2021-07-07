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

import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadEngine;
import com.kezy.downloadlib.impls.IDownloadStatusListener;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * 下载实现类
 */
public class DownloadEngineManager implements IDownloadEngine {


    public interface RunnableStatus {
        int STARTED = 1;     //开始
        int DOWNLOADING = 2; // 正在下载
        int FINISHED = 3;    //完成
        int STOPPED = 4;     //暂停
        int ERROR = 5;       //错误
        int DELETE = 6;      // 删除
    }


    private static final String TAG = "DownloadEngineManager";
    private boolean mConnected = false;

    private Context mContext;
    @Nullable
    private DownloadService mDownloadService;

    private Map<String, DownloadRunnable> onlyKeyRunnableMap = new HashMap<>();

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

    public DownloadEngineManager(Context context) {
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



    @Override
    public void startDownload(Context context,String downloadUrl, String onlyKey) {
        if (!checkConnectionStatus(context)) {
            return;
        }

        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }
        if (!onlyKeyRunnableMap.containsKey(onlyKey)) {
            DownloadRunnable runnable = new DownloadRunnable(context, downloadUrl, new StatusChangeHandler(onlyKey));
            runnable.isRunning = true;
            runnable.downloadStatus = RunnableStatus.STARTED;
            onlyKeyRunnableMap.put(onlyKey, runnable);

        } else {
            onlyKeyRunnableMap.get(onlyKey).isRunning = true;
            onlyKeyRunnableMap.get(onlyKey).downloadStatus = RunnableStatus.DOWNLOADING;
        }

        mDownloadService.threadPool.submit(onlyKeyRunnableMap.get(onlyKey));
    }

    @Override
    public void startDownloadWithPath(Context context, String url, String onlyKey, String path) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }

        if (!onlyKeyRunnableMap.containsKey(onlyKey)) {
            DownloadRunnable runnable = new DownloadRunnable(context, url,  new StatusChangeHandler(onlyKey));
            runnable.savePath = path;
            onlyKeyRunnableMap.put(onlyKey, runnable);
        }

        mDownloadService.threadPool.submit(onlyKeyRunnableMap.get(onlyKey));
    }

    @Override
    public void startDownloadWithName(Context context, String url, String onlyKey, String name) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }

        if (!onlyKeyRunnableMap.containsKey(onlyKey)) {
            DownloadRunnable runnable = new DownloadRunnable(context, url,  new StatusChangeHandler(onlyKey));
            runnable.name = name;
            onlyKeyRunnableMap.put(onlyKey, runnable);
        }

        mDownloadService.threadPool.submit(onlyKeyRunnableMap.get(onlyKey));
    }

    @Override
    public void startDownloadWithNameAndPath(Context context, String url, String onlyKey, String name, String path) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService == null || mDownloadService.threadPool == null) {
            return;
        }

        if (!onlyKeyRunnableMap.containsKey(onlyKey)) {
            DownloadRunnable runnable = new DownloadRunnable(context, url,  new StatusChangeHandler(onlyKey));
            runnable.name = name;
            runnable.savePath = path;
            onlyKeyRunnableMap.put(onlyKey, runnable);
        }
        mDownloadService.threadPool.submit(onlyKeyRunnableMap.get(onlyKey));

    }

    @Override
    public void pauseDownload(Context context, String onlyKey) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        onlyKeyRunnableMap.get(onlyKey).isRunning = false;
        onlyKeyRunnableMap.get(onlyKey).downloadStatus = RunnableStatus.STOPPED;
    }

    @Override
    public void continueDownload(Context context, String onlyKey) {

        startDownload(context, null, onlyKey);
    }

    @Override
    public void deleteDownload(Context context, String onlyKey) {

        onlyKeyRunnableMap.get(onlyKey).downloadStatus = RunnableStatus.DELETE;
        onlyKeyRunnableMap.get(onlyKey).isRunning = false;
        String filePath = onlyKeyRunnableMap.get(onlyKey).savePath;
        if (filePath != null && new File(filePath).exists()) {
            new File(filePath).delete();
        }
        File tempDownloadPath = DownloadUtils.getTempDownloadPath(onlyKeyRunnableMap.get(onlyKey).savePath, onlyKeyRunnableMap.get(onlyKey).name);
        if (tempDownloadPath.exists()) {
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

    public class StatusChangeHandler extends Handler {

        private String mOnlyKey;

        public StatusChangeHandler(String onlyKey) {
            this.mOnlyKey = onlyKey;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.i("-------------msg", " ------- handleMessage : " + msg.toString());

            switch (msg.what) {
                case DOWN_OK:
                    Log.i("-------------msg", " ------- 2222 下载完成 task URL : " + onlyKeyRunnableMap.get(mOnlyKey));
                    // 下载完成，点击安装
                    Log.e("----------msg", " ------- 下载完成22 ----fileName   " + onlyKeyRunnableMap.get(mOnlyKey).savePath);
                    handleDownloadSuccess(mOnlyKey, onlyKeyRunnableMap.get(mOnlyKey).savePath, onlyKeyRunnableMap.get(mOnlyKey).name);
                    handleInstallBegin(mOnlyKey);
                    break;

                case DOWN_START:
                    Log.e("----------msg", " ------- DOWN_START ----   ");
                    handleStart(mOnlyKey, onlyKeyRunnableMap.get(mOnlyKey).tempSize != 0, msg.arg1);
                    break;
                case DOWN_ERROR:
                    Log.e("----------msg", " ------- err ----   ");
                    handleError(mOnlyKey);
                    break;
                case DOWNLOAD_ING:
                    Log.e("----------msg", " ------- ing ----   " + msg.arg2);
                    handleProgress(mOnlyKey, msg.arg1, msg.arg2);
                    break;
                case REQUEST_TIME_OUT:
                    Log.e("----------msg", " ------- REQUEST_TIME_OUT ----   ");
                    handleError(mOnlyKey);
                    break;
                case HANDLER_PAUSE:
                    Log.e("----------msg", " ------- HANDLER_PAUSE ----   ");
                    handlePause(mOnlyKey);
                    break;
                case HANDLER_REMOVE:
                    Log.e("----------msg", " ------- HANDLER_REMOVE ----   ");
                    handleRemove(mOnlyKey);
                    break;
                default:
                    break;
            }
        }
    }


    private void handleRemove(String onlyKey) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onRemove(onlyKey);
        }
        Log.d(TAG, "handleRemove   " + onlyKey);
    }

    private void handlePause(String onlyKey) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onPause(onlyKey);
        }
        Log.d(TAG, "handlePause   " + onlyKey);
    }

    private void handleProgress(String onlyKey, long totalSize, int progress) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onProgress(onlyKey,totalSize, progress);
        }
        Log.d(TAG, "handleProgress   " + onlyKey);
    }

    private void handleError(String onlyKey) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onError(onlyKey);
        }
        Log.d(TAG, "handleError   " + onlyKey);
    }

    private void handleDownloadSuccess(String onlyKey, String path, String apkName) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onSuccess(onlyKey, path,  apkName);
        }
        Log.d(TAG, "handleDownloadSuccess   " + onlyKey);
    }

    private void handleStart(String onlyKey, boolean isRestart, int totalSize) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onStart(onlyKey, isRestart, totalSize);
        }

        Log.d(TAG, "handleStart   " + onlyKey + " ----- isRestart = " + isRestart);
    }

    private void handleInstallBegin(String onlyKey) {
        for (IDownloadStatusListener l : mDownloadServiceStatueListeners) {
            l.onInstallBegin(onlyKey);
        }
    }
}
