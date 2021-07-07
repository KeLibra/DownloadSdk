package com.kezy.downloadlib.downloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Log.e(TAG, " --------- onCreate");
        mBinder = new Binder();
        threadPool = Executors.newFixedThreadPool(5);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
