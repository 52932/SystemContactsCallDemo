package com.example.callplusdemo;

import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import cn.rongcloud.callplus.api.RCCallPlusClient;
import cn.rongcloud.callplus.api.RCCallPlusMediaType;
import cn.rongcloud.callplus.api.RCCallPlusSession;
import cn.rongcloud.callplus.api.callback.IRCCallPlusEventListener;
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
                RCCallPlusMediaType.VIDEO, "callid12345678", System.currentTimeMillis());

            SystemContactsManger.getInstance().queryCallLog(MainActivity.this, remoteUserPhone);
        }
    }

    private void imLogin(String token) {
        //todo 有可能在本端用户登录前就有其他用户给他发起了呼叫请求
        //todo 当本端用户登录成功，SDK就会将通话信息通过onReceivedCall回调返回给APP层  所以需要在登录前注册该监听
        RCCallPlusClient.getInstance().setCallPlusEventListener(new IRCCallPlusEventListener() {
            @Override
            public void onReceivedCall(RCCallPlusSession callSession, String extra) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("bugtags","MainActivity--onReceivedCall--->callId: " + callSession.getCallId());
                        CallPlusActivity.startCallPlusActivity(MainActivity.this, 0, "");
                    }
                });
            }
        });


        connectIM(token, new ConnectCallback() {
            @Override
            public void onSuccess(String t) {
                showToast("IM登录成功，UserId: "+t);
                SessionManager.getInstance().put(CURRENT_USER_TOKEN_KEY, token);

                //不存在需要接听的通话时就跳转页面。这个判断逻辑是为了防止和 onReceivedCall 中重复启动页面加的
                if (RCCallPlusClient.getInstance().getCurrentCallSession() == null) {
                    CallPlusActivity.startCallPlusActivity(MainActivity.this, 0, "");
                }
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