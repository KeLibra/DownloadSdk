package com.kezy.downloadlib.impls;

import android.content.Context;


/**
 * @Author Kezy
 * @Time 2021/6/21
 * @Description task 接口
 */
public interface EngineImpl {

    class DownloadType {
        public static int TYPE_API = 100; // api下载
        public static int TYPE_XIMA = 101; // 喜马下载
    }

    void subscribe(IStatusObserver observer);


    // 下载任务id
    long getTaskId();

    // 开始下载
    void startDownload(Context context, String url);

    //暂停下载
    void pauseDownload(Context context, String url);

    // 继续下载
    void continueDownload(Context context, String url);

    // 删除下载
    void deleteDownload(Context context, String url);

    void installApk(Context context, String url);

    // 获取下载文件路径
    String getDownloadFile(Context context);

    void destroy();
}
