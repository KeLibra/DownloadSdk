package com.kezy.downloadlib.task;

import android.app.NotificationManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.receiver.InstallApkReceiver;
import com.kezy.downloadlib.downloader.DownloadEngineManager;
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
    private DownloadInfo mInfo;

    private NotificationManager mNotifyManager;

    @Override
    public DownloadInfo getInfo() {
        return mInfo;
    }
    public DownloadTask(Context context, DownloadInfo info) {
        mTaskManager = new DownloadEngineManager(context);
        this.mContext = context;
        this.mInfo = info;
        if (mContext != null) {
            mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        InstallApkReceiver.registerReceiver(context, thisListener);
    }

    @Override
    public String createDownloadKey() {
        return mInfo.onlyKey();
    }

    @Override
    public void start(Context context) {
        if (mTaskManager != null) {
            if (!TextUtils.isEmpty(mInfo.name) && !TextUtils.isEmpty(mInfo.path)) {
                mTaskManager.startDownloadWithNameAndPath(context, mInfo.url, mInfo.onlyKey(), mInfo.name, mInfo.path);
                return;
            }
            if (!TextUtils.isEmpty(mInfo.name)) {
                mTaskManager.startDownloadWithName(context, mInfo.url, mInfo.onlyKey(), mInfo.name);
                return;
            }
            if (!TextUtils.isEmpty(mInfo.path)) {
                mTaskManager.startDownloadWithPath(context, mInfo.url, mInfo.onlyKey(), mInfo.path);
                return;
            }

            mTaskManager.startDownload(context, mInfo.url, mInfo.onlyKey());
        }
    }

    @Override
    public void pause(Context context) {
        if (mTaskManager != null) {
            mTaskManager.pauseDownload(context, mInfo.onlyKey());
        }
    }

    @Override
    public void reStart(Context context) {
        if (mTaskManager != null) {
            mTaskManager.continueDownload(context, mInfo.onlyKey());
        }
    }

    @Override
    public void remove(Context context) {
        if (mTaskManager != null) {
            mTaskManager.deleteDownload(context, mInfo.onlyKey());
        }
    }

    @Override
    public void install(Context context) {
        DownloadUtils.installApk(mContext, mInfo.getSavePath());
    }

    @Override
    public int getStatus() {
        return mInfo.status;
    }


    @Override
    public void addTaskListener(IDownloadTaskListener taskListener) {
        mTaskListener = taskListener;
        if (mTaskManager != null) {
            mTaskManager.bindStatusListener(new IDownloadStatusListener() {
                @Override
                public void onStart(String onlyKey, boolean isRestart, long totalSize) {
                    mInfo.status = DownloadInfo.Status.STARTED;
                    mInfo.totalSize = totalSize;
                    if (taskListener != null) {
                        taskListener.onStart(mInfo.onlyKey(), isRestart);
                    }
                }

                @Override
                public void onPause(String onlyKey) {
                    mInfo.status = DownloadInfo.Status.STOPPED;
                    if (taskListener != null) {
                        taskListener.onPause(mInfo.onlyKey());
                    }
                }

                @Override
                public void onContinue(String onlyKey) {
                    mInfo.status = DownloadInfo.Status.DOWNLOADING;
                    if (taskListener != null) {
                        taskListener.onContinue(mInfo.onlyKey());
                    }
                }

                @Override
                public void onRemove(String onlyKey) {
                    mInfo.status = DownloadInfo.Status.DELETE;
                    if (taskListener != null) {
                        taskListener.onRemove(mInfo.onlyKey());
                    }
                }

                @Override
                public void onProgress(String onlyKey, long totalSize, int progress) {
                    mInfo.status = DownloadInfo.Status.DOWNLOADING;
                    if (mInfo.totalSize <= 0) {
                        mInfo.totalSize = totalSize;
                    }
                    mInfo.tempSize = (long) (mInfo.totalSize * progress / 100);
                    mInfo.progress = progress;
                    if (taskListener != null) {
                        taskListener.onProgress(mInfo.onlyKey());
                    }
                    NotificationsManager.getInstance().sendProgressViewNotification(mContext, mNotifyManager, mInfo.progress, mInfo.timeId);
                }

                @Override
                public void onError(String onlyKey) {
                    mInfo.status = DownloadInfo.Status.ERROR;
                    if (taskListener != null) {
                        taskListener.onError(mInfo.onlyKey());
                    }
                }

                @Override
                public void onSuccess(String onlyKey, String path, String name) {
                    mInfo.path = path;
                    mInfo.name = name;
                    mInfo.status = DownloadInfo.Status.FINISHED;
                    Log.e("----------msg", " ---------- 下载完成， path = " + mInfo.getSavePath());
                    mInfo.packageName = DownloadUtils.getPackageNameByFilepath(mContext, mInfo.getSavePath());
                    if (taskListener != null) {
                        taskListener.onSuccess(mInfo.onlyKey());
                    }
                    DownloadUtils.installApk(mContext, mInfo.getSavePath());
                    NotificationsManager.getInstance().clearNotificationById(mNotifyManager, (int) mInfo.timeId);
                }

                @Override
                public void onInstallBegin(String onlyKey) {
                    mInfo.status = DownloadInfo.Status.FINISHED;
                    if (taskListener != null) {
                        taskListener.onInstallBegin(mInfo.onlyKey());
                    }
                }
            });
        }
    }

    @Override
    public void openApp(Context context) {
        DownloadUtils.startAppByPackageName(context, mInfo.packageName);
    }


    private IInstallListener thisListener = new IInstallListener() {
        @Override
        public void onInstall(String packageName) {
            if (TextUtils.equals(packageName, mInfo.packageName)) {
                // 安装回调
                if (mTaskListener != null) {
                    mInfo.status = DownloadInfo.Status.INSTALLED;
                    mTaskListener.onInstallSuccess(mInfo.onlyKey());
                }
            }
        }
    };
}
