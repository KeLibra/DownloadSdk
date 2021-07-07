package com.kezy.downloadlib.downloader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadStatusListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.kezy.downloadlib.common.DownloadConstants.DOWNLOAD_APK_AD_ID;
import static com.kezy.downloadlib.common.DownloadConstants.DOWNLOAD_APK_NAME;
import static com.kezy.downloadlib.common.DownloadConstants.DOWNLOAD_APK_URL;

/**
 * @Author Kezy
 * @Time 2021/6/22
 * @Description 专门用来下载的服务
 */
public class DownloadService extends Service {

    private static final String TAG = "-----msg download";
    private Binder mBinder;


    public ExecutorService threadPool; // 下载线程池

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class Binder extends android.os.Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("---------msg", " --------- onCreate");
        mBinder = new Binder();
        threadPool = Executors.newFixedThreadPool(5);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
