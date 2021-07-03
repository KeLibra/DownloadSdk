package com.kezy.downloadlib.impls;

import android.content.Context;

import com.kezy.downloadlib.DownloadInfo;

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
