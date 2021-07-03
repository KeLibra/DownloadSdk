package com.kezy.downloadlib.impls;

import android.content.Context;

/**
 * @Author Kezy
 * @Time 2021/6/21
 * @Description task 接口
 */
public interface IDownloadEngine {

    void bindStatusListener(IDownloadStatusListener listener);

    // 开始下载
    void startDownload(Context context, String url);

    //暂停下载
    void pauseDownload(Context context, String url);

    // 继续下载
    void continueDownload(Context context, String url);

    // 删除下载
    void deleteDownload(Context context, String url);

    void installApk(Context context, String url);

    void destroy();
}
