package com.kezy.downloadlib.downloader;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadEngine;
import com.kezy.downloadlib.impls.IDownloadStatusListener;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import static com.kezy.downloadlib.downloader.DownloadService.DOWNLOAD_APK_AD_ID;
import static com.kezy.downloadlib.downloader.DownloadService.DOWNLOAD_APK_NAME;
import static com.kezy.downloadlib.downloader.DownloadService.DOWNLOAD_APK_URL;


/**
 */
public class DownloadServiceManage implements IDownloadEngine {

    private boolean mConnected = false;

    private Context mContext;

    @Nullable
    private DownloadService mDownloadService;

    private DownloadInfo mInfo;

    private IDownloadStatusListener mListener;

    public DownloadServiceManage(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
            init(mContext);
        }
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        Log.e("----------", " -------- init ");
        context.bindService(new Intent(context, DownloadService.class), mConn, Context.BIND_AUTO_CREATE);
    }

    public void bindDownloadInfo(DownloadInfo info) {

        Log.e("----------", " -------- bindDownloadInfo " + mDownloadService);
        if (mDownloadService != null) {
            mDownloadService.setDownloadInfo(info);
        } else {
            mInfo = info;
        }
    }

    @Override
    public void bindStatusListener(IDownloadStatusListener listener) {
        if (mDownloadService != null) {
            mDownloadService.addDownloadStatueListener(listener);
        } else {
            mListener = listener;
        }
    }

    public DownloadInfo getInfo() {
        if (mDownloadService == null) {
            Log.i("-------msg", " v2 manager info " + mInfo);
            return mInfo;
        }
        Log.i("-------msg", " v2 manager info " + mDownloadService.getDownloadInfoByUrl(mInfo.url));
        return mDownloadService.getDownloadInfoByUrl(mInfo.url);
    }

    @Override
    public void startDownload(Context context, String url) {
        goDownloadApk();
    }

    @Override
    public void pauseDownload(Context context, String url) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService != null) {
            mDownloadService.pauseDownload(mInfo.url);
        }
    }

    @Override
    public void continueDownload(Context context, String url) {

        if (!checkConnectionStatus(context)) {
            goDownloadApk();
            return;
        }
        if (mDownloadService != null) {
            mDownloadService.startDownload(mInfo.url);
        }
    }

    @Override
    public void deleteDownload(Context context, String url) {
        if (!checkConnectionStatus(context)) {
            return;
        }
        if (mDownloadService != null) {
            mDownloadService.removeDownload(mInfo.url);
        }
    }

    public int getStatus(Context context, String url) {
        if (mDownloadService == null) {
            return mInfo.status;
        }
        return mDownloadService.getStatueByUrl(mInfo.url);
    }

    @Override
    public void installApk(Context context, String url) {
        if (mDownloadService != null) {
            DownloadUtils.installApk(mContext, mDownloadService.getDownloadSavePath(mInfo.url));
        }
    }

    @Override
    public void destroy() {
        if (mDownloadService != null) {
            mDownloadService.removeAllListener();
            mDownloadService.unbindService(mConn);
        }
        unBindDownloadService(mContext);
    }


    public void unBindDownloadService(Context context) {
        try {
            if (mConnected) {
                context.unbindService(mConn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnected = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e("----------", " -------- onServiceConnected ");
            if (service instanceof DownloadService.Binder) {
                mConnected = true;
                mDownloadService = ((DownloadService.Binder) service).getService();
                mDownloadService.setDownloadInfo(mInfo);
                mDownloadService.addDownloadStatueListener(mListener);
            }
        }
    };

    private boolean checkConnectionStatus(Context context) {
        if (!mConnected || mDownloadService == null) {
            init(context);
            return false;
        }
        return true;
    }

    @Nullable
    public String getDownloadSavePath(String url) {

        if (mDownloadService != null) {
            return mDownloadService.getDownloadSavePath(url);
        }

        return mInfo.path;
    }

    public boolean isDowning(String url) {

        if (mDownloadService != null) {
            return mDownloadService.isDowning(url);
        }
        return mInfo.status == DownloadInfo.Status.DOWNLOADING;
    }



    public void downLoadAPK(final String downUrl, String fileName) {
        if (TextUtils.isEmpty(downUrl)) {
            return;
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = getFileNameByDownLoadUrl(downUrl);
        }
        try {
            goDownloadApk(URLDecoder.decode(downUrl, "utf-8"), fileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    private String getFileNameByDownLoadUrl(String downloadUrl) {
        if (TextUtils.isEmpty(downloadUrl)) {
            return System.currentTimeMillis() + "";
        }

        URL url = null;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (url == null) {
            return System.currentTimeMillis() + "";
        }

        String fileName = new File(url.getPath()).getName();
        if (TextUtils.isEmpty(fileName)) {
            return System.currentTimeMillis() + "";
        }

        if (fileName.length() > 50) {
            fileName = fileName.substring(0, 50);
        }

        return fileName;
    }

    private void goDownloadApk(String downloadUrl, String fileName) {
        if (mContext == null) {
            return;
        }
        if (!mConnected) {
            init(mContext);
        }

        Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(DOWNLOAD_APK_URL, downloadUrl);
        if (!TextUtils.isEmpty(fileName)) {
            intent.putExtra(DOWNLOAD_APK_NAME, fileName);
        } else {
            intent.putExtra(DOWNLOAD_APK_NAME, getFileNameByDownLoadUrl(downloadUrl));
        }
        mContext.startService(intent);
    }

    private void goDownloadApk() {
        if (mContext == null) {
            return;
        }
        if (!mConnected) {
            init(mContext);
        }

        if (mInfo == null || TextUtils.isEmpty(mInfo.url)) {
            return;
        }
        Intent intent = new Intent(mContext, DownloadService.class);
        intent.putExtra(DOWNLOAD_APK_URL, mInfo.url);
        if (!TextUtils.isEmpty(mInfo.name)) {
            intent.putExtra(DOWNLOAD_APK_NAME, mInfo.name);
        } else {
            intent.putExtra(DOWNLOAD_APK_NAME, getFileNameByDownLoadUrl(mInfo.url));
        }
        intent.putExtra(DOWNLOAD_APK_AD_ID, mInfo.adId);
        mContext.startService(intent);
    }
}
