package com.kezy.downloadlib.task;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;

import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.receiver.InstallApkReceiver;
import com.kezy.downloadlib.downloader.DownloadServiceManage;
import com.kezy.downloadlib.impls.IDownloadEngine;
import com.kezy.downloadlib.impls.IDownloadStatusListener;
import com.kezy.downloadlib.impls.IDownloadTaskListener;
import com.kezy.downloadlib.impls.IInstallListener;
import com.kezy.downloadlib.impls.ITaskImpl;
import com.kezy.noticelib.NotificationsManager;


/**
 * @Author Kezy
 * @Time 2021/7/1
 * @Description
 */
public class DownloadTask implements ITaskImpl {

    public IDownloadEngine mTaskManager;
    private Context mContext;
    private IDownloadTaskListener mTaskListener;

    private NotificationManager mNotifyManager;

    public DownloadTask(Context context, DownloadInfo info) {
        mTaskManager = new DownloadServiceManage(context);
        mTaskManager.bindDownloadInfo(info);
        this.mContext = context;
        if (mContext != null) {
            mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        InstallApkReceiver.registerReceiver(context, thisListener);
    }

    @Override
    public void start(Context context) {
        if (mTaskManager != null) {
            mTaskManager.startDownload(context);
        }
    }

    @Override
    public void pause(Context context) {
        if (mTaskManager != null) {
            mTaskManager.pauseDownload(context);
        }
    }

    @Override
    public void reStart(Context context) {
        if (mTaskManager != null) {
            mTaskManager.continueDownload(context);
        }
    }

    @Override
    public void remove(Context context) {
        if (mTaskManager != null) {
            mTaskManager.deleteDownload(context);
        }
    }

    @Override
    public void install(Context context) {
        if (mTaskManager != null) {
            mTaskManager.installApk(context);
        }
    }

    @Override
    public int getStatus() {
        if (mTaskManager == null || mTaskManager.getInfo() == null) {
            return 0;
        }
        return getInfo().status;
    }

    @Override
    public DownloadInfo getInfo() {
        return mTaskManager.getInfo();
    }

    @Override
    public String createDownloadKey() {
        return mTaskManager.getInfo().onlyKey();
    }

    @Override
    public void addTaskListener(IDownloadTaskListener taskListener) {
        mTaskListener = taskListener;
        if (mTaskManager != null) {
            mTaskManager.bindStatusListener(new IDownloadStatusListener() {
                @Override
                public void onStart(String onlyKey, boolean isRestart, long totalSize) {
                    getInfo().status = DownloadInfo.Status.STARTED;
                    getInfo().totalSize = totalSize;
                    if (taskListener != null) {
                        taskListener.onStart(getInfo().onlyKey(), isRestart);
                    }
                }

                @Override
                public void onPause(String onlyKey) {
                    getInfo().status = DownloadInfo.Status.STOPPED;
                    if (taskListener != null) {
                        taskListener.onPause(getInfo().onlyKey());
                    }
                }

                @Override
                public void onContinue(String onlyKey) {
                    getInfo().status = DownloadInfo.Status.DOWNLOADING;
                    if (taskListener != null) {
                        taskListener.onContinue(getInfo().onlyKey());
                    }
                }

                @Override
                public void onRemove(String onlyKey) {
                    getInfo().status = DownloadInfo.Status.DELETE;
                    if (taskListener != null) {
                        taskListener.onRemove(getInfo().onlyKey());
                    }
                }

                @Override
                public void onProgress(String onlyKey, int progress) {
                    getInfo().status = DownloadInfo.Status.DOWNLOADING;
                    getInfo().tempSize = (long) (getInfo().totalSize * progress / 100);
                    getInfo().progress = progress;
                    if (taskListener != null) {
                        taskListener.onProgress(getInfo().onlyKey());
                    }
                    NotificationsManager.getInstance().sendProgressViewNotification(mContext, mNotifyManager, getInfo().progress, getInfo().timeId);
                }

                @Override
                public void onError(String onlyKey) {
                    getInfo().status = DownloadInfo.Status.ERROR;
                    if (taskListener != null) {
                        taskListener.onError(getInfo().onlyKey());
                    }
                }

                @Override
                public void onSuccess(String onlyKey, String path) {
                    getInfo().status = DownloadInfo.Status.FINISHED;
                    getInfo().packageName = DownloadUtils.getPackageNameByFilepath(mContext, getInfo().getSavePath());
                    if (taskListener != null) {
                        taskListener.onSuccess(getInfo().onlyKey());
                    }
                    NotificationsManager.getInstance().clearNotificationById(mNotifyManager, (int) getInfo().timeId);
                }

                @Override
                public void onInstallBegin(String onlyKey) {
                    getInfo().status = DownloadInfo.Status.FINISHED;
                    if (taskListener != null) {
                        taskListener.onInstallBegin(getInfo().onlyKey());
                    }
                }
            });
        }
    }

    @Override
    public void openApp(Context context) {
        DownloadUtils.startAppByPackageName(context, getInfo().packageName);
    }


    private IInstallListener thisListener = new IInstallListener() {
        @Override
        public void onInstall(String packageName) {
            if (TextUtils.equals(packageName, getInfo().packageName)) {
                // 安装回调
                if (mTaskListener != null) {
                    getInfo().status = DownloadInfo.Status.INSTALLED;
                    mTaskListener.onInstallSuccess(getInfo().onlyKey());
                }
            }
        }
    };
}
