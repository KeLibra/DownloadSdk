package com.kezy.noticelib;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 *
 * @author peter
 * @date 2018/7/4
 */
public class NotificationsManager {
    public final static int NOTIFICATION_SAMPLE = 0;
    public final static int NOTIFICATION_ACTION = 1;
    public final static int NOTIFICATION_REMOTE_INPUT = 2;
    public final static int NOTIFICATION_BIG_PICTURE_STYLE = 3;
    public final static int NOTIFICATION_BIG_TEXT_STYLE = 4;
    public final static int NOTIFICATION_INBOX_STYLE = 5;
    public final static int NOTIFICATION_MEDIA_STYLE = 6;
    public final static int NOTIFICATION_MESSAGING_STYLE = 7;
    public final static int NOTIFICATION_PROGRESS = 8;
    public final static int NOTIFICATION_CUSTOM_HEADS_UP = 9;
    public final static int NOTIFICATION_CUSTOM = 10;

    public final static String ACTION_SIMPLE = "com.android.peter.notificationdemo.ACTION_SIMPLE";
    public final static String ACTION_ACTION = "com.android.peter.notificationdemo.ACTION_ACTION";
    public final static String ACTION_REMOTE_INPUT = "com.android.peter.notificationdemo.ACTION_REMOTE_INPUT";
    public final static String ACTION_BIG_PICTURE_STYLE = "com.android.peter.notificationdemo.ACTION_BIG_PICTURE_STYLE";
    public final static String ACTION_BIG_TEXT_STYLE = "com.android.peter.notificationdemo.ACTION_BIG_TEXT_STYLE";
    public final static String ACTION_INBOX_STYLE = "com.android.peter.notificationdemo.ACTION_INBOX_STYLE";
    public final static String ACTION_MEDIA_STYLE = "com.android.peter.notificationdemo.ACTION_MEDIA_STYLE";
    public final static String ACTION_MESSAGING_STYLE = "com.android.peter.notificationdemo.ACTION_MESSAGING_STYLE";
    public final static String ACTION_PROGRESS = "com.android.peter.notificationdemo.ACTION_PROGRESS";
    public final static String ACTION_CUSTOM_HEADS_UP_VIEW = "com.android.peter.notificationdemo.ACTION_CUSTOM_HEADS_UP_VIEW";
    public final static String ACTION_CUSTOM_VIEW = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_LOVE = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_LOVE";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_PRE = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_PRE";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_PLAY_OR_PAUSE = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_PLAY_OR_PAUSE";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_NEXT = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_NEXT";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_LYRICS = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_LYRICS";
    public final static String ACTION_CUSTOM_VIEW_OPTIONS_CANCEL = "com.android.peter.notificationdemo.ACTION_CUSTOM_VIEW_OPTIONS_CANCEL";

    public final static String ACTION_YES = "com.android.peter.notificationdemo.ACTION_YES";
    public final static String ACTION_NO = "com.android.peter.notificationdemo.ACTION_NO";
    public final static String ACTION_DELETE = "com.android.peter.notificationdemo.ACTION_DELETE";
    public final static String ACTION_REPLY = "com.android.peter.notificationdemo.ACTION_REPLY";
    public final static String REMOTE_INPUT_RESULT_KEY = "remote_input_content";

    public final static String EXTRA_OPTIONS = "options";
    public final static String MEDIA_STYLE_ACTION_DELETE = "action_delete";
    public final static String MEDIA_STYLE_ACTION_PLAY = "action_play";
    public final static String MEDIA_STYLE_ACTION_PAUSE = "action_pause";
    public final static String MEDIA_STYLE_ACTION_NEXT = "action_next";
    public final static String ACTION_ANSWER = "action_answer";
    public final static String ACTION_REJECT = "action_reject";


    private static volatile NotificationsManager sInstance = null;

    private NotificationsManager() {
    }

    public static NotificationsManager getInstance() {
        if(sInstance == null) {
            synchronized (NotificationsManager.class) {
                if(sInstance == null) {
                    sInstance = new NotificationsManager();
                }
            }
        }
        return sInstance;
    }


    public void sendProgressViewNotification(Context context,NotificationManager nm, int progress, long taskId) {
        //创建通知
        @SuppressLint({"NewApi", "LocalSuppress"}) Notification.Builder nb = new Notification.Builder(context, NotificationChannels.DEFAULT)
                //设置通知左侧的小图标
                .setSmallIcon(R.mipmap.ic_reply)
                //设置通知标题
                .setContentTitle("Downloading...")
                //设置通知内容
                .setContentText(progress + "%")
                //设置显示通知时间
                .setShowWhen(true)
                .setAutoCancel(true)
                .setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)
                //设置点击通知时的响应事件
//                .setContentIntent(pi)
                .setProgress(100,progress,false);
        //发送通知
        nm.notify((int) taskId, nb.build());
    }



    public void clearAllNotification(NotificationManager nm) {
        nm.cancelAll();
    }

    public void  clearNotificationById(NotificationManager nm, int id) {
        nm.cancel(id);
    }

}
