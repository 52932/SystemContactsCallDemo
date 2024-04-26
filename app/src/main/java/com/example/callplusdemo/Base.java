package com.example.callplusdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import io.rong.imlib.IRongCoreCallback.ConnectCallback;
import io.rong.imlib.IRongCoreEnum.ConnectionErrorCode;
import io.rong.imlib.IRongCoreEnum.DatabaseOpenStatus;
import io.rong.imlib.RongCoreClient;
import java.util.ArrayList;
import java.util.List;

public class Base extends Activity {

    private static final int REQUEST_CODE_AUDIO = 1000;
    private static final int REQUEST_CODE_AUDIO_VIDEO = 1001;
    private static final int REQUEST_CODE_INTERNET = 1002;
    List<String> unGrantedPermissions;
    public final String CURRENT_USER_TOKEN_KEY = "CURRENT_USER_TOKEN_KEY";
    public final String REMOTE_USER_KEY = "REMOTE_USER_KEY";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        try {
            Configuration config = new Configuration();
            config.setToDefaults();
            res.updateConfiguration(config, res.getDisplayMetrics());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public void checkAudioVideoPermission() {
        String[] permissions = {
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
//            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.AUTHENTICATE_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "android.permission.WRITE_SYNC_SETTINGS",
            "android.permission.READ_SYNC_SETTINGS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.ANSWER_PHONE_CALLS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.PROCESS_OUTGOING_CALLS"
        };
        checkPermissions(permissions, REQUEST_CODE_AUDIO_VIDEO);
    }

    private void checkPermissions(String[] permissions, int requestCode) {
        unGrantedPermissions = new ArrayList();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (unGrantedPermissions.isEmpty()) {
           showToast("有权限");
        } else {
            // 部分权限未获得，重新请求获取权限
            String[] array = new String[unGrantedPermissions.size()];
            ActivityCompat.requestPermissions(
                this, unGrantedPermissions.toArray(array), requestCode);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        unGrantedPermissions.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                unGrantedPermissions.add(permissions[i]);
            }
        }
        for (String permission : unGrantedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, "权限不足：" + permission, Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {permission}, 0);
            }
        }
    }

    public void checkPermission() {
        for (String permission : unGrantedPermissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, "权限不足：" + permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void showToast(final String msg) {
        postUIThread(() -> Toast.makeText(Base.this, msg, Toast.LENGTH_LONG).show());
    }

    public boolean isFinish() {
        return isFinishing() || isDestroyed();
    }

    protected void postUIThread(final Runnable run) {
        if (isFinish()) {
            return;
        }
        runOnUiThread(
            () -> {
                if (isFinish() || run == null) {
                    return;
                }
                run.run();
            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            showToast("别点返回按钮");
        }
        return false;
    }

    //todo  从 App Server 获取 UserID 对应的 Token。用户测试本Demo登录，不能和 USER_2_TOKEN 一致
    public final String USER_1_TOKEN = "";

    //todo 从 App Server 获取 UserID 对应的 Token。用户测试本Demo登录，不能和 USER_1_TOKEN 一致
    public final String USER_2_TOKEN = "";

    /**
     * TODO: 请替换成您自己申请的 AppKey
     */
    public final String APP_KEY = "";

    protected void connectIM(String token, ConnectCallback connectCallback) {
        RongCoreClient.getInstance().logout();
        RongCoreClient.getInstance().disconnect();
        RongCoreClient.init(Base.this.getApplicationContext(), APP_KEY, false);
        RongCoreClient.connect(token, new ConnectCallback() {
            @Override
            public void onSuccess(String t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (connectCallback == null) {
                            return;
                        }
                        connectCallback.onSuccess(t);
                    }
                });
            }

            @Override
            public void onError(ConnectionErrorCode e) {
                if (connectCallback == null) {
                    return;
                }
                connectCallback.onError(e);
            }

            @Override
            public void onDatabaseOpened(DatabaseOpenStatus code) {
                if (connectCallback == null) {
                    return;
                }
                connectCallback.onDatabaseOpened(code);
            }
        });
    }
}
