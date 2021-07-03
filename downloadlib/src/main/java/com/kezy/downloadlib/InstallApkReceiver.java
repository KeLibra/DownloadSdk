package com.kezy.downloadlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.kezy.downloadlib.impls.IInstallListener;

/**
 * @Author Kezy
 * @Time 2021/7/1
 * @Description
 */
public class InstallApkReceiver extends BroadcastReceiver {

    private static IInstallListener mListener;
    private static InstallApkReceiver mReceiver = new InstallApkReceiver();
    private static IntentFilter mIntentFilter;

    public static void registerReceiver(Context context, IInstallListener listener) {
        if (context == null) {
            return;
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addDataScheme("package");
        mIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        mIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        mIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        context.registerReceiver(mReceiver, mIntentFilter);

        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("------------msg", " --------  安装了------------ intent = " + intent.getAction());
        if (intent == null || intent.getData() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            if (mListener != null) {
                mListener.onInstall(intent.getData().getSchemeSpecificPart());
            }
        }
    }


    public static void unregisterReceiver(Context context) {
        if (context != null) {
            context.unregisterReceiver(mReceiver);
        }
    }
}
