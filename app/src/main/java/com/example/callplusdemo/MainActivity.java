package com.example.callplusdemo;

import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import cn.rongcloud.callplus.api.RCCallPlusMediaType;
import io.rong.imlib.IRongCoreCallback.ConnectCallback;
import io.rong.imlib.IRongCoreEnum.ConnectionErrorCode;
import io.rong.imlib.IRongCoreEnum.DatabaseOpenStatus;

public class MainActivity extends Base {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAudioVideoPermission();
    }

    public void mainClick(View view) {
        int id = view.getId();
        if (id == R.id.btnLoginUser1) {
            imLogin(USER_1_TOKEN);
        } else if (id == R.id.btnLoginUser2) {
            imLogin(USER_2_TOKEN);
        } else if (id == R.id.btnAddContact) {
            SystemContactsManger.getInstance().addAccount(MainActivity.this);
            SystemContactsManger.getInstance().clearAll(MainActivity.this.getApplicationContext());

            String remoteUseName = "王五";
            String remoteUserPhone = "13900000000";
            String userId = "13900000000";
            SystemContactsManger.getInstance()
                .addContact(MainActivity.this.getApplicationContext(), remoteUseName, remoteUserPhone, userId);

            showToast("添加数据成功");
        } else if (id == R.id.btnInsertCallLog) {
            String remoteUseName = "王五";
            String remoteUserPhone = "13900000000";
            SystemContactsManger.getInstance().insertCallLog(MainActivity.this.getApplicationContext(), remoteUseName, remoteUserPhone, CallLog.Calls.OUTGOING_TYPE,
                RCCallPlusMediaType.VIDEO);
        }
    }

    private void imLogin(String token) {
        connectIM(token, new ConnectCallback() {
            @Override
            public void onSuccess(String t) {
                showToast("IM登录成功，UserId: "+t);
                SessionManager.getInstance().put(CURRENT_USER_TOKEN_KEY, token);
                CallPlusActivity.startCallPlusActivity(MainActivity.this, 0, "");
            }

            @Override
            public void onError(ConnectionErrorCode e) {
                showToast("IM登录失败，ErrorCode: "+ e.name());
            }

            @Override
            public void onDatabaseOpened(DatabaseOpenStatus code) {

            }
        });
    }
}