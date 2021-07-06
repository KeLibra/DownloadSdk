package com.kezy.downloadlib.downloader;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.kezy.downloadlib.bean.DownloadInfo;
import com.kezy.downloadlib.common.DownloadUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;

import static com.kezy.downloadlib.common.DownloadConstants.DOWNLOAD_APK_PATH;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.DOWNLOAD_ING;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.DOWN_ERROR;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.DOWN_OK;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.DOWN_START;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.HANDLER_PAUSE;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.HANDLER_REMOVE;
import static com.kezy.downloadlib.downloader.DownloadServiceManage.REQUEST_TIME_OUT;

/**
 * @Author Kezy
 * @Time 2021/7/5
 * @Description 下载的runnable ， 用线程池调用
 */
public class DownloadRunnable implements Runnable {


    private final WeakReference<DownloadServiceManage.UpdateHandler> weakHandler;
    private final WeakReference<Context> weakContext;


    public String name;
    public int downloadStatus = -1;
    public boolean isRunning = true;
    public String savePath;


    private String downloadUrl;

    public long tempSize;
    private long totalSize;

    private int progress;
    private double speed;


    public DownloadRunnable(Context context, String downloadUrl, DownloadServiceManage.UpdateHandler handler) {
        weakHandler = new WeakReference<>(handler);
        weakContext = new WeakReference<>(context);
        this.downloadUrl = downloadUrl;
    }


    @Override
    public void run() {
        Log.d("mydownload", "start: " + name + ", @ ");
        Message message = null;
        if (weakHandler == null || weakHandler.get() == null) {
            return;
        }
        Handler handler = weakHandler.get();
        try {
            if (!DownloadUtils.checkSdcardMounted()) {
                return;
            }

            long downloadSize = downloadUpdateFile(handler);

            if (downloadSize == Integer.MAX_VALUE) {
                message = Message.obtain();
                if (downloadStatus == DownloadInfo.Status.DELETE) {
                    message.what = HANDLER_REMOVE;
                } else {
                    message.what = HANDLER_PAUSE;
                    downloadStatus = DownloadInfo.Status.STOPPED;
                }

            } else if (downloadSize > 0) {
                // 下载成功
                message = Message.obtain();
                message.what = DOWN_OK;
                downloadStatus = DownloadInfo.Status.FINISHED;

                Log.e("----------msg", " ------- 下载完成 ---- downloadSize " + downloadSize);
            } else {
                downloadStatus = DownloadInfo.Status.ERROR;
                message = Message.obtain();
                message.what = DOWN_ERROR;
                Log.d("mydownload", "downloadCoutn" + downloadSize);
            }
        } catch (SocketTimeoutException e) {
            message = Message.obtain();
            message.what = REQUEST_TIME_OUT;
            downloadStatus = DownloadInfo.Status.ERROR;
        } catch (IOException e) {
            message = Message.obtain();
            message.what = DOWN_ERROR;
            downloadStatus = DownloadInfo.Status.ERROR;
        } finally {
            Log.d("-----msg mydownload"," --- :finally -- " + (message == null ? "null" : message.what));
            handler.sendMessage(message);
        }
    }

    /***
     * 下载文件
     *
     * @throws IOException
     */
    private long downloadUpdateFile(Handler handler) throws IOException {

        double downloadSpeed;
        long speedTemp = tempSize;
        long mUpDateTimerMillis = 0;
        int down_step = 1;// 提示step
        long downloadedLength = 0;// 已经下载好的大小
        int updateCount = 0;// 百分比

        Log.v("--------msg", "检测task的状态 ---  isRunning " + isRunning);
        if (!isRunning) // 检测task的状态
        {
            return Integer.MAX_VALUE;
        }

        downloadStatus = DownloadInfo.Status.DOWNLOADING;

        boolean isRestart = tempSize != 0;
        if (handler != null) {
            Message message = Message.obtain();
            message.what = DOWN_START;
            downloadStatus = DownloadInfo.Status.STARTED;
            handler.sendMessage(message);
        }
        Log.v("--------msg v2", " ------ isRestart = " + isRestart);

        long curSize = tempSize;
        HttpURLConnection connection = null;
        InputStream in = null;
        RandomAccessFile out = null;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36");
            connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            connection.setRequestProperty("Accept-Language", "zh-CN");
            connection.setRequestProperty("Charset", "UTF-8");

            connection.setInstanceFollowRedirects(true);// 设置重定向问题
            connection.setConnectTimeout(20000);
            if (tempSize > 0) // 检测临时文件的大小
            {
                // 如果本地緩存文件被清除,则重新下载
                if (tempSize > 0 && tempSize < totalSize) {
                    String range = String.format("bytes=%d-%d", curSize, totalSize - 1);
                    connection.setRequestProperty("Range", range);
                } else {
                    tempSize = 0;
                }
            }
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_PARTIAL) {
                // 读取文件信息
                String filelen = connection.getHeaderField("Content-length");
                String filerange = connection.getHeaderField("Content-Range");
                if (!TextUtils.isEmpty(filerange)) {
                    int index = filerange.lastIndexOf('/');
                    filerange = filerange.substring(index + 1);
                    filelen = filerange;
                }

                long file_len = 0;
                try {
                    file_len = Long.valueOf(filelen);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (tempSize > 0) {
                    if (file_len != totalSize) {
                        throw new Exception("file size error!try!");
                    }
                } else {
                    totalSize = file_len;
                }
            } else if (HttpURLConnection.HTTP_MOVED_PERM == status || HttpURLConnection.HTTP_SEE_OTHER == status || HttpURLConnection.HTTP_MOVED_TEMP == status) {
                throw new Exception("download url change!");
            } else {
                throw new Exception("network error!");
            }

            // 获取文件名称
            if (TextUtils.isEmpty(name)) {
                String connUrl = connection.getURL().toString();
                name = URLDecoder.decode(connUrl.substring(connUrl.lastIndexOf("/") + 1), "utf-8");
                name = name.substring(name.lastIndexOf("/") + 1);
            }
            // 创建下载目录
            if (TextUtils.isEmpty(savePath)) {
                // 更新apk下载目录
                String path = getDiskCachePath();
                if (TextUtils.isEmpty(path)) {
                    return 0;
                }

                savePath = path + "/" + DOWNLOAD_APK_PATH;
                Log.d("-------msg", "保存的地址是   " + path);
            } else if (!new File(savePath).canWrite()) {
                return 0;
            }

            File dlPath = new File(savePath);
            // 文件不存在，并且文件夹创建失败
            if (!dlPath.exists() && !dlPath.mkdirs())
                return 0;

            // 删除更新目录下原先的apk文件
            if (dlPath.isDirectory()) {
                File[] childFiles = dlPath.listFiles();
                if (childFiles != null && childFiles.length > 0) {
                    int size = childFiles.length;
                    for (int i = 0; i < size; i++) {
                        if (name.equals(childFiles[i].getName())) {
                            childFiles[i].delete();
                        }
                    }
                }
            }

            File file = DownloadUtils.getTempDownloadPath(savePath, name);
            Log.v("---------msg", " ----- download save   getTempDownloadPath.length()  = " + file.length() + " ---- path = " + file.getAbsolutePath());
            if (file != null && file.length() <= 0) {
                Log.e("---------msg", " ----- download save   被清空了进度， 需要重新下载  = ");
                tempSize = 0;
                progress = 0;
                curSize = 0;
            }
            int nread;
            byte[] buffer = new byte[4096];
            in = new BufferedInputStream(connection.getInputStream());
            out = new RandomAccessFile(file, "rw"); // 用来访问那些保存数据记录的文件的，你就可以用seek
            out.seek(curSize); // 方法来访问记录
            try {
                Log.v("--------msg", "正在下载， -----------  222222222222  检测task的状态 ---  isRunning " + isRunning);
                while (isRunning && (nread = in.read(buffer, 0, buffer.length)) > 0) {
                    out.write(buffer, 0, nread);
                    // 设置临时文件长度
                    curSize += nread;
                    tempSize = curSize;

                    long l = 0;
                    if (totalSize > 0) {
                        l = curSize * 100 / totalSize;
                    }
                    if (updateCount == 0 || (l - down_step) >= updateCount) {
                        updateCount += down_step;
                        if (updateCount > progress) {
                            progress = updateCount;
//                            handleDownloadProgressUpdate(url, updateCount);
                            Log.v("-------msg", "handler = " + handler + " ------ progress = " + updateCount);
                            if (handler != null) {
                                Message message = Message.obtain();
                                message.what = DOWNLOAD_ING;
                                message.arg1 = (int) totalSize;
                                message.arg2 = progress;
                                downloadStatus = DownloadInfo.Status.DOWNLOADING;
                                handler.sendMessage(message);
                            }
                        }
                    }
                    if (System.currentTimeMillis() - mUpDateTimerMillis > 1000) {
                        downloadSpeed = (curSize - speedTemp) * 1000 / (System.currentTimeMillis() - mUpDateTimerMillis);  //计算下载速度
                        speed = downloadSpeed;         //下载速度赋值
                        speedTemp = curSize;
                        mUpDateTimerMillis = System.currentTimeMillis();
                    }
                }
            } catch (Exception e) {
                return -1;
            }

            if (!isRunning) {
                if (downloadStatus != DownloadInfo.Status.DELETE) {
                    downloadStatus = 0;
                }
                return Integer.MAX_VALUE;
            }
            // 如果下载完成
            if (totalSize == tempSize || totalSize == 0) {
                if (!TextUtils.isEmpty(name) && (name.endsWith(".apk") || name.endsWith(".APK"))) {
                    file.renameTo(new File(savePath, name));
                } else {
                    file.renameTo(new File(savePath, name + ".apk"));
                }
                Log.e("---------msg", " ---- 下载完成 file ------ " + file.getPath());
                downloadedLength = tempSize;
                isRunning = false;
            } else {
                tempSize = 0;
                totalSize = 0;
                isRunning = false;
                File[] childFiles = dlPath.listFiles();
                if (childFiles != null && childFiles.length > 0) {
                    int size = childFiles.length;
                    for (int i = 0; i < size; i++) {
                        String filenames = name + ".temp";
                        if (filenames.equals(childFiles[i].getName())) {
                            childFiles[i].delete();
                        }
                    }
                }
                return Integer.MAX_VALUE;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != in) {
                    // 关闭输入流
                    in.close();
                }
                if (null != out) {
                    // 关闭输出流
                    out.close();
                }
                if (null != connection) {
                    // 断开网络连接
                    connection.disconnect();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        return downloadedLength;
    }

    @Nullable
    public String getDiskCachePath() {
        if (weakContext == null || weakContext.get() == null) {
            return null;
        }
        Context context = weakContext.get();

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            if (context.getExternalCacheDir() == null) {
                if (context.getCacheDir() == null) {
                    return null;
                }
                return context.getCacheDir().getPath();
            }
            return context.getExternalCacheDir().getPath();
        } else {
            if (context.getCacheDir() != null) {
                return context.getCacheDir().getPath();
            }
            return null;
        }
    }
}
