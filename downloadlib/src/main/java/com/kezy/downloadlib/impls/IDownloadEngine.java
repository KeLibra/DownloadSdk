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
    void startDownload(Context context, String url, String onlyKey);
    void startDownloadWithPath(Context context, String url, String onlyKey, String path);
    void startDownloadWithName(Context context, String url, String onlyKey, String name);
    void startDownloadWithNameAndPath(Context context, String url, String onlyKey, String name, String path);

    //暂停下载
    void pauseDownload(Context context, String onlyKey);

    // 继续下载
    void continueDownload(Context context,String onlyKey);

    // 删除下载
    void deleteDownload(Context context, String onlyKey);

    void destroy();
}
