package com.kezy.downloadlib.impls;

/**
 * @Author Kezy
 * @Time 2021/6/29
 * @Description
 */
public interface IStatusObserver {

    void onStart(String onlyKey, boolean isRestart, long totalSize);
    void onPause(String onlyKey);
    void onContinue(String onlyKey);
    void onRemove(String onlyKey);
    void onProgress(String onlyKey, int progress);
    void onError(String onlyKey);
    void onSuccess(String onlyKey, String dowloadPath);
    void onInstallBegin(String onlyKey);
}
