package com.kezy.downloadsdk;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kezy.downloader.ITaskImpl;
import com.kezy.downloader.bean.DownloadInfo;
import com.kezy.downloader.task.DownloadTaskManager;
import com.kezy.downloadlib.common.DownloadUtils;
import com.kezy.downloadlib.impls.IDownloadTaskListener;
import com.kezy.noticelib.NotificationChannels;

public class MainActivity extends AppCompatActivity {

    private Button btnApi, btnXima, btnDownload;

    private String url_113MB = "https://js.a.kspkg.com/bs2/fes/kwai-android-ANDROID_KS_LDM_SJYY_CPA_NJYSJLLQKJB-gifmakerrelease-9.1.11.18473_x32_a35aec.apk";
    private String url_35MB = "http://b.xzfile.com/apk3/xgmfxsv1.0.9.241_downcc.com.apk";

    private ProgressBar pbBar;
    private TextView tvPb;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        pbBar = findViewById(R.id.probar);
        btnDownload = findViewById(R.id.btn_download);
        tvPb = findViewById(R.id.tv_pb);

        NotificationChannels.createAllNotificationChannels(MainActivity.this);


        ITaskImpl task = DownloadTaskManager.getInstance().createDownloadTask(MainActivity.this,
                new DownloadInfo
                        .Builder(url_35MB, 0)
                        .build());

        btnApi = findViewById(R.id.btn_api);
        btnApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task.start(MainActivity.this);
            }
        });


        btnXima = findViewById(R.id.btn_xima);

        btnXima.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("-----------msg", "  -==== 1111 " + task.getStatus());
//                Log.e("-----------msg", "  -====  22222 " + task1.getStatus());

               task.install(MainActivity.this);
            }
        });


        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int status = task.getStatus();
                switch (status) {
                    case DownloadInfo.Status.WAITING:
                    case DownloadInfo.Status.DELETE:
                        task.start(MainActivity.this);
                        break;
                    case DownloadInfo.Status.STARTED:
                    case DownloadInfo.Status.DOWNLOADING:
                        task.pause(MainActivity.this);
                        break;
                    case DownloadInfo.Status.FINISHED:
                        task.install(MainActivity.this);
                        break;
                    case DownloadInfo.Status.STOPPED:
                    case DownloadInfo.Status.ERROR:
                        task.reStart(MainActivity.this);
                        break;
                    case DownloadInfo.Status.INSTALLED:
                        task.openApp(MainActivity.this);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + status);
                }
            }
        });

        task.addTaskListener(new IDownloadTaskListener() {
            @Override
            public void onStart(String onlyKey, boolean isRestart) {

                Log.v("--------msg", " ---- isRestart . " + isRestart);
                btnDownload.setText("?????????...");
                if (isRestart) {
                    return;
                }
                pbBar.setProgress(0);
                tvPb.setText("0 % - " + DownloadUtils.getFileSize(task.getInfo().tempSize) + "/" + DownloadUtils.getFileSize(task.getInfo().totalSize));
            }

            @Override
            public void onPause(String onlyKey) {
                btnDownload.setText("??????");
            }

            @Override
            public void onRemove(String onlyKey) {

            }

            @Override
            public void onProgress(String onlyKey) {
                btnDownload.setText("?????????...");
                pbBar.setProgress(task.getInfo().progress);
                tvPb.setText(task.getInfo().progress + " % - " + DownloadUtils.getFileSize(task.getInfo().tempSize) + "/" + DownloadUtils.getFileSize(task.getInfo().totalSize));
            }

            @Override
            public void onError(String onlyKey) {
                btnDownload.setText("??????");
            }

            @Override
            public void onSuccess(String onlyKey) {
                btnDownload.setText("??????");
                pbBar.setProgress(100);
                tvPb.setText("100 % - " + DownloadUtils.getFileSize(task.getInfo().tempSize) + "/" + DownloadUtils.getFileSize(task.getInfo().totalSize));
            }

            @Override
            public void onInstallBegin(String onlyKey) {
            }

            @Override
            public void onInstallSuccess(String onlyKey) {
                btnDownload.setText("??????");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DownloadTaskManager.getInstance().destroy();
    }


}