package com.kezy.downloader;

import android.content.Context;

import com.kezy.downloader.bean.DownloadInfo;
import com.kezy.downloadlib.impls.IDownloadTaskListener;

/**
 * @Author Kezy
 * @Time 2021/6/22
 * @Description
 */
public interface ITaskImpl {


    DownloadInfo getInfo();
    // 下载的key
    String createDownloadKey();

    void start(Context context);

    void pause(Context context);

    void reStart(Context context);

    void remove(Context context);

    void install(Context context);

    int getStatus();

    void addTaskListener(IDownloadTaskListener listener);

    void openApp(Context context);

}
