package com.kezy.downloadlib.impls;

import android.content.Context;

import com.kezy.downloadlib.bean.DownloadInfo;

/**
 * @Author Kezy
 * @Time 2021/6/21
 * @Description task 接口
 */
public interface IDownloadEngine {

    void bindDownloadInfo(DownloadInfo info);

    DownloadInfo getInfo();

    void bindStatusListener(IDownloadStatusListener listener);

    // 开始下载
    void startDownload(Context context);

    //暂停下载
    void pauseDownload(Context context);

    // 继续下载
    void continueDownload(Context context);

    // 删除下载
    void deleteDownload(Context context);

    void installApk(Context context);

    void destroy();
}
