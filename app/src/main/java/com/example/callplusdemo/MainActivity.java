package com.example.callplusdemo;

import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import cn.rongcloud.callplus.api.RCCallPlusClient;
import cn.rongcloud.callplus.api.RCCallPlusMediaType;
import cn.rongcloud.callplus.api.RCCallPlusSession;
import cn.rongcloud.callplus.api.callback.IRCCallPlusEventListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreCallback.ConnectCallback;
import io.rong.imlib.IRongCoreEnum.ConnectionErrorCode;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.IRongCoreEnum.DatabaseOpenStatus;
import io.rong.imlib.IRongCoreListener.ConnectionStatusListener.ConnectionStatus;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Conversation.ConversationType;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;


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
        } else if (id == R.id.btnSendTextMessage) {
            SendTextMessage();
        } else if (id == R.id.btnStartCallPlusActivity) {
            if (RongCoreClient.getInstance().getCurrentConnectionStatus() != ConnectionStatus.CONNECTED) {
                showToast("IM未登录，请先登录");
                return;
            }
            CallPlusActivity.startCallPlusActivity(MainActivity.this, 0, "");
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
                        showToast("有人呼叫，自动跳转到通话页面");
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

    private void SendTextMessage() {
        //会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
        String targetId = "";
        String str = "测试消息消息内容";
        ConversationType conversationType = Conversation.ConversationType.PRIVATE;
        TextMessage messageContent = TextMessage.obtain(str);

        Message message = Message.obtain(targetId, conversationType, messageContent);

        //当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
        String pushContent = str;
        //远程推送附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
        String pushData = "";

        RongCoreClient.getInstance().sendMessage(message, pushContent, pushData, new IRongCoreCallback.ISendMessageCallback() {

            @Override
            public void onAttached(Message message) {
            }

            @Override
            public void onSuccess(Message message) {
                showToast("发生文本成功,MessageId: " + message.getMessageId());
            }

            @Override
            public void onError(Message message, CoreErrorCode coreErrorCode) {
                showToast("发生文本失败： " + coreErrorCode.name());
            }
        });
    }
}