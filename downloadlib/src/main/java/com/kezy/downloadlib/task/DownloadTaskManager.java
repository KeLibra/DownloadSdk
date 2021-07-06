package com.kezy.downloadlib.task;

import android.content.Context;
import android.util.Log;


import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.receiver.InstallApkReceiver;
import com.kezy.downloadlib.impls.ITaskImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Kezy
 * @Time 2021/6/30
 * @Description
 */
public class DownloadTaskManager {

    private Context mContext;
    private Map<String, DownloadTask> mRunTaskMap = new HashMap<>();

    private DownloadTaskManager() {
        loadLocalData();
    }

    private static class INSTANCE {
        private static DownloadTaskManager instance = new DownloadTaskManager();
    }

    public static DownloadTaskManager getInstance() {
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
        if (task == null || task.getInfo() == null) {
            return;
        }
        mRunTaskMap.put(task.createDownloadKey(), task);
        saveLocalData();
    }

    public void removeTask(DownloadTask task) {
        if (task == null || task.getInfo() == null) {
            return;
        }
        mRunTaskMap.remove(task.createDownloadKey());
    }

    public DownloadTask getTaskByOnlyKey(String onlyKey) {
        for (Map.Entry<String, DownloadTask> entry : mRunTaskMap.entrySet()) {
            if (entry != null && entry.getKey().endsWith(onlyKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void startTask(Context context, DownloadTask task) {
        if (task != null) {
            task.start(context);
        }
    }

    public void pauseTask(Context context, DownloadTask task) {
        if (task != null) {
            task.pause(context);
        }
    }

    public void deleteTask(Context context, DownloadTask task) {
        if (task != null) {
            task.remove(context);
        }
    }

    private void loadLocalData() {
        Log.e("--------msg", "--------1 load local data ");
    }

    private void saveLocalData() {
        Log.e("--------msg", "--------2 save local data ");
    }

    public void destroy(){
        InstallApkReceiver.unregisterReceiver(mContext);
    }
}
