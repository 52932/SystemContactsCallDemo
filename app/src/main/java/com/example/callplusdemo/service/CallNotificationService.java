package com.example.callplusdemo.service;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.callplusdemo.MainActivity;
import com.example.callplusdemo.R;

public final class CallNotificationService extends Service  {
    private static final String TAG = "CallNotificationService";
    private NotificationManager mNotificationManager;
    private static final String channelId = "CallNotificationService";
    private int notifyId = 20241101;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Log.d(TAG, "onCreate: ");
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // CallActivity.class点击通知栏后打开的Activity,该Activity的launchMode设置为:android:launchMode="singleTop"
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent;
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                pendingIntent =
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent =
                    PendingIntent.getActivity(
                        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }

            Notification.Builder builder =
                new Notification.Builder(this)
                    // R.drawable.ic_launcher :通知栏显示的图标
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(getString(R.string.app_name))
                    // 通知栏提示语
                    .setContentTitle("Touch here to return to Demo")
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setContentIntent(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setCategory(Notification.CATEGORY_CALL);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(channelId);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, "test", importance);
                notificationChannel.enableLights(false);
                notificationChannel.setLightColor(Color.GREEN);
                notificationChannel.enableVibration(false);
                notificationChannel.setSound(null, null);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
            //            mNotificationManager.notify(notifyId, builder.build());

            builder.setPriority(Notification.PRIORITY_LOW);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            }

            notification = builder.build();
            notification.defaults = Notification.DEFAULT_ALL;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notifyId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        | ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
            } else {
                startForeground(notifyId, notification);
            }
            Log.d(TAG, "onCreate: startForeground");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        startForeground(notifyId, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(notifyId, notification);
        return super.onStartCommand(intent, flags, startId);
    }
}
