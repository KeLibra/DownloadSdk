package com.kezy.downloadlib.task;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;


import com.kezy.downloadlib.DownloadInfo;
import com.kezy.downloadlib.InstallApkReceiver;
import com.kezy.downloadlib.downloader.DownloadServiceManage;
import com.kezy.downloadlib.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadEngine;
import com.kezy.downloadlib.impls.IDownloadStatusListener;
import com.kezy.downloadlib.impls.IDownloadTaskListener;
import com.kezy.downloadlib.impls.IInstallListener;
import com.kezy.downloadlib.impls.ITaskImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Kezy
 * @Time 2021/6/30
 * @Description
 */
public class TaskManager {

    private Context mContext;
    private Map<String, DownloadTask> mKeyTaskMap = new HashMap<>();

    private IDownloadTaskListener mTaskListener;

    private TaskManager() {
        loadLocalData();
    }

    private static class INSTANCE {
        private static TaskManager instance = new TaskManager();
    }

    public static TaskManager getInstance() {
        return INSTANCE.instance;
    }


    public ITaskImpl createDownloadTask(Context context, DownloadInfo info) {
        if (context == null || info == null) {
            return null;
        }
        this.mContext = context.getApplicationContext();
        DownloadTask task = getTaskByOnlyKey(info.onlyKey());
        if (task == null) {
            task = new DownloadTask(context, info);
            updateTask(task);
        }
        return task;
    }


    public void updateTask(DownloadTask task) {
        if (task == null || task.mDownloadInfo == null) {
            return;
        }
        mKeyTaskMap.put(task.createDownloadKey(), task);
        saveLocalData();
    }

    public void removeTask(DownloadTask task) {
        if (task == null || task.mDownloadInfo == null) {
            return;
        }
        mKeyTaskMap.remove(task.createDownloadKey());
    }

    public DownloadTask getTaskByOnlyKey(String onlyKey) {
        for (Map.Entry<String, DownloadTask> entry : mKeyTaskMap.entrySet()) {
            if (entry != null && entry.getKey().endsWith(onlyKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void loadLocalData() {
        Log.e("--------msg", "--------1 load local data ");
    }

    private void saveLocalData() {
        Log.e("--------msg", "--------2 save local data ");
    }

    private class DownloadTask implements ITaskImpl {
        public IDownloadEngine mTaskManager;
        public DownloadInfo mDownloadInfo;

        public DownloadTask(Context context,DownloadInfo info) {
            mTaskManager = new DownloadServiceManage(context);
            ((DownloadServiceManage)mTaskManager).bindDownloadInfo(info);
            this.mDownloadInfo = info;
            InstallApkReceiver.registerReceiver(context, thisListener);
        }

        @Override
        public void start(Context context) {
            if (mTaskManager!= null) {
                mTaskManager.startDownload(context, mDownloadInfo.url);
            }
        }

        @Override
        public void pause(Context context) {
            if (mTaskManager!= null) {
                mTaskManager.pauseDownload(context, mDownloadInfo.url);
            }
        }

        @Override
        public void reStart(Context context){
            if (mTaskManager!= null) {
                mTaskManager.continueDownload(context, mDownloadInfo.url);
            }
        }

        @Override
        public void remove(Context context) {
            if (mTaskManager!= null) {
                mTaskManager.deleteDownload(context, mDownloadInfo.url);
            }
        }

        @Override
        public void install(Context context) {
            if (mTaskManager != null) {
                mTaskManager.installApk(context, mDownloadInfo.url);
            }
        }

        @Override
        public int getStatus() {
            if (mTaskManager == null) {
                return 0;
            }
            return mDownloadInfo.status;
        }

        @Override
        public DownloadInfo getInfo() {
                return mDownloadInfo;
        }

        @Override
        public String createDownloadKey() {
            return mDownloadInfo.onlyKey();
        }

        @Override
        public void addTaskListener(IDownloadTaskListener taskListener) {
            mTaskListener = taskListener;
            if (mTaskManager != null) {
                mTaskManager.bindStatusListener(new IDownloadStatusListener() {
                    @Override
                    public void onStart(String onlyKey, boolean isRestart, long totalSize) {
                        mDownloadInfo.status = DownloadInfo.Status.STARTED;
                        mDownloadInfo.totalSize = totalSize;
                        if (taskListener != null) {
                            taskListener.onStart(mDownloadInfo.onlyKey(), isRestart);
                        }
                    }

                    @Override
                    public void onPause(String onlyKey) {
                        mDownloadInfo.status = DownloadInfo.Status.STOPPED;
                        if (taskListener != null) {
                            taskListener.onPause(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onContinue(String onlyKey) {
                        mDownloadInfo.status = DownloadInfo.Status.DOWNLOADING;
                        if (taskListener != null) {
                            taskListener.onContinue(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onRemove(String onlyKey) {
                        mDownloadInfo.status = DownloadInfo.Status.DELETE;
                        if (taskListener != null) {
                            taskListener.onRemove(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onProgress(String onlyKey, int progress) {
                        mDownloadInfo.status = DownloadInfo.Status.DOWNLOADING;
                        mDownloadInfo.tempSize = (long) (mDownloadInfo.totalSize * progress / 100);
                        mDownloadInfo.progress = progress;
                        if (taskListener != null) {
                            taskListener.onProgress(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onError(String onlyKey) {
                        mDownloadInfo.status = DownloadInfo.Status.ERROR;
                        if (taskListener != null) {
                            taskListener.onError(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onSuccess(String onlyKey, String path) {
                        mDownloadInfo.status = DownloadInfo.Status.FINISHED;
                        mDownloadInfo.path = path;
                        mDownloadInfo.packageName = DownloadUtils.getPackageNameByFilepath(mContext, path);
                        if (taskListener != null) {
                            taskListener.onSuccess(mDownloadInfo.onlyKey());
                        }
                    }

                    @Override
                    public void onInstallBegin(String onlyKey) {
                        mDownloadInfo.status = DownloadInfo.Status.FINISHED;
                        if (taskListener != null) {
                            taskListener.onInstallBegin(mDownloadInfo.onlyKey());
                        }
                    }
                });
            }
        }

        @Override
        public void openApp(Context context) {
            DownloadUtils.startAppByPackageName(context, mDownloadInfo.packageName);
        }
    }

    private IInstallListener thisListener = new IInstallListener() {
        @Override
        public void onInstall(String packageName) {
            if (mKeyTaskMap != null) {
                for (Map.Entry<String, DownloadTask> entry : mKeyTaskMap.entrySet()) {
                    if (entry != null && entry.getValue() !=null && entry.getValue().mDownloadInfo != null) {
                        if (TextUtils.equals(packageName, entry.getValue().mDownloadInfo.packageName)) {
                            if (mTaskListener != null) {
                                entry.getValue().mDownloadInfo.status = DownloadInfo.Status.INSTALLED;
                                mTaskListener.onInstallSuccess(entry.getValue().mDownloadInfo.onlyKey());
                            }
                        }
                    }
                }
            }
        }
    };

    public void destroy(){
        InstallApkReceiver.unregisterReceiver(mContext);
    }
}
