package com.kezy.downloadlib.impls;

/**
 * @Author Kezy
 * @Time 2021/7/1
 * @Description
 */
public interface IDownloadTaskListener {

    void onStart(String onlyKey, boolean isRestart);
    void onPause(String onlyKey);
    void onRemove(String onlyKey);
    void onProgress(String onlyKey);
    void onError(String onlyKey);
    void onSuccess(String onlyKey);
    void onInstallBegin(String onlyKey);
    void onInstallSuccess(String onlyKey);
}
