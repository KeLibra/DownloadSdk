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
    public DownloadInfo mDownloadInfo;
    private Context mContext;
    private IDownloadTaskListener mTaskListener;

    private NotificationManager mNotifyManager;

    public DownloadTask(Context context, DownloadInfo info) {
        mTaskManager = new DownloadServiceManage(context);
        ((DownloadServiceManage) mTaskManager).bindDownloadInfo(info);
        this.mDownloadInfo = info;
        this.mContext = context;
        if (mContext != null) {
            mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        InstallApkReceiver.registerReceiver(context, thisListener);
    }

    @Override
    public void start(Context context) {
        if (mTaskManager != null) {
            mTaskManager.startDownload(context, mDownloadInfo.url);
        }
    }

    @Override
    public void pause(Context context) {
        if (mTaskManager != null) {
            mTaskManager.pauseDownload(context, mDownloadInfo.url);
        }
    }

    @Override
    public void reStart(Context context) {
        if (mTaskManager != null) {
            mTaskManager.continueDownload(context, mDownloadInfo.url);
        }
    }

    @Override
    public void remove(Context context) {
        if (mTaskManager != null) {
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
                    NotificationsManager.getInstance().sendProgressViewNotification(mContext, mNotifyManager, mDownloadInfo.progress, mDownloadInfo.timeId);
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
                    mDownloadInfo.packageName = DownloadUtils.getPackageNameByFilepath(mContext, mDownloadInfo.getSavePath());
                    if (taskListener != null) {
                        taskListener.onSuccess(mDownloadInfo.onlyKey());
                    }
                    NotificationsManager.getInstance().clearNotificationById(mNotifyManager, (int) mDownloadInfo.timeId);
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


    private IInstallListener thisListener = new IInstallListener() {
        @Override
        public void onInstall(String packageName) {
            if (TextUtils.equals(packageName, mDownloadInfo.packageName)) {
                // 安装回调
                if (mTaskListener != null) {
                    mDownloadInfo.status = DownloadInfo.Status.INSTALLED;
                    mTaskListener.onInstallSuccess(mDownloadInfo.onlyKey());
                }
            }
        }
    };
}
