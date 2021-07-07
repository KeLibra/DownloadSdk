package com.kezy.downloadlib.impls;

/**
 * @Author Kezy
 * @Time 2021/6/29
 * @Description
 */
public interface IDownloadStatusListener {

    void onStart(String onlyKey, boolean isRestart, long totalSize);
    void onPause(String onlyKey);
    void onRemove(String onlyKey);
    void onProgress(String onlyKey,long totalSize, int progress);
    void onError(String onlyKey);
    void onSuccess(String onlyKey, String downloadPath, String name);
    void onInstallBegin(String onlyKey);
}
