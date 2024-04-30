package com.example.callplusdemo;

import android.content.Intent;
import android.os.Build;
import cn.rongcloud.rtc.api.RCRTCEngine;
import cn.rongcloud.rtc.api.RCRTCRoom;
import com.example.callplusdemo.service.CallNotificationService;
import io.rong.push.RongPushPlugin;

public class App extends android.app.Application {

    private static App INSTANCE;
    private int mActiveCount = 0;
    private int mAliveCount = 0;
    private boolean isActive;

    @Override
    public void onCreate() {
        INSTANCE = this;
        super.onCreate();
        SessionManager.initContext(this);

        RongPushPlugin.init(this);
    }

    public static App getApplication() {
        return INSTANCE;
    }

    private void notifyChange() {
        if (mActiveCount > 0) {
            if (!isActive) {
                isActive = true;
                // APP进入前台,关闭前台服务
                stopNotificationService();
            }
        } else {
            if (isActive) {
                isActive = false;
                // APP被切入后台，判断是否在音视频房间中;
                RCRTCRoom room = RCRTCEngine.getInstance().getRoom();
                if (room != null) {
                    Intent service = new Intent(this, CallNotificationService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(service);
                    } else {
                        startService(service);
                    }
                }
            }
        }
    }

    private void stopNotificationService() {
        stopService(new Intent(this, CallNotificationService.class));
    }
}
