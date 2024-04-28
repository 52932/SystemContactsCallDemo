package com.example.callplusdemo;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.rongcloud.calllib.api.RCCallPlusOrder;
import cn.rongcloud.callplus.api.RCCallPlusCallRecord;
import cn.rongcloud.callplus.api.RCCallPlusClient;
import cn.rongcloud.callplus.api.RCCallPlusCode;
import cn.rongcloud.callplus.api.RCCallPlusConfig;
import cn.rongcloud.callplus.api.RCCallPlusLocalVideoView;
import cn.rongcloud.callplus.api.RCCallPlusMediaType;
import cn.rongcloud.callplus.api.RCCallPlusMediaTypeChangeResult;
import cn.rongcloud.callplus.api.RCCallPlusReason;
import cn.rongcloud.callplus.api.RCCallPlusRecordInfo;
import cn.rongcloud.callplus.api.RCCallPlusRemoteVideoView;
import cn.rongcloud.callplus.api.RCCallPlusRenderMode;
import cn.rongcloud.callplus.api.RCCallPlusResultCode;
import cn.rongcloud.callplus.api.RCCallPlusSession;
import cn.rongcloud.callplus.api.RCCallPlusSummaryMessageContent;
import cn.rongcloud.callplus.api.RCCallPlusType;
import cn.rongcloud.callplus.api.RCCallPlusUser;
import cn.rongcloud.callplus.api.RCCallPlusUserSessionStatus;
import cn.rongcloud.callplus.api.callback.IRCCallPlusEventListener;
import cn.rongcloud.callplus.api.callback.IRCCallPlusResultListener;
import io.rong.imlib.IRongCoreCallback.ConnectCallback;
import io.rong.imlib.IRongCoreEnum.ConnectionErrorCode;
import io.rong.imlib.IRongCoreEnum.DatabaseOpenStatus;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Message;
import java.util.ArrayList;
import java.util.List;

public class CallPlusActivity extends Base {

    /**
     * @param startSource -1：由点击联系人信息中的通话记录启动，0 :由MainActivity页面启动，1：由点击通话记录启动
     */
    public static void startCallPlusActivity(Context context, int startSource, String remoteUserPhone) {
        Log.e("bugtags", "startCallPlusActivity-->startSource: " +startSource +" remoteUserPhone: " +remoteUserPhone);
        Intent intent = new Intent(context, CallPlusActivity.class);
        intent.putExtra(SystemContactsManger.START_SOURCE, startSource);
        intent.putExtra(SystemContactsManger.REMOTE_USER_PHONE_NUMBER, remoteUserPhone);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private FrameLayout mLocalVideoViewFrameLayout, mRemoteVideoViewFrameLayout;
    private EditText mEditRemoteUserId;
    TextView tvData, tvMediaType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("bugtags", "CallPlusActivity----onCreate");
        setContentView(R.layout.activity_call_plus);
        tvData = findViewById(R.id.tvData);
        tvMediaType = findViewById(R.id.tvMediaType);
        tvData.setText("当前登录的用户Id：" +  RongCoreClient.getInstance().getCurrentUserId());
        mLocalVideoViewFrameLayout = findViewById(R.id.frameLayoutLocalVideoView);
        mRemoteVideoViewFrameLayout = findViewById(R.id.frameLayoutRemoteVideoView);
        mEditRemoteUserId = findViewById(R.id.editRemoteUserId);

        SystemContactsManger.getInstance().addAccount(CallPlusActivity.this);

        Intent intent = getIntent();
        int startSource = intent.getIntExtra(SystemContactsManger.START_SOURCE, -1);
        if (startSource==-1) {
            String currentUserToken = SessionManager.getInstance().getString(CURRENT_USER_TOKEN_KEY);
            if (TextUtils.isEmpty(currentUserToken)) {
                showToast("之前没有登录过IM，无法自动登录");
                return;
            }
            parseContactInfo(intent,CallPlusActivity.this);
        } else if (startSource==0){
            String remoteUserId = SessionManager.getInstance().getString(REMOTE_USER_KEY);
            mEditRemoteUserId.setText(remoteUserId);

            setCallPlusListener();
            initCallPlus();
        } else if (startSource == 1){
            String remoteUserPhone = intent.getStringExtra(SystemContactsManger.REMOTE_USER_PHONE_NUMBER);
            String currentUserToken = SessionManager.getInstance().getString(CURRENT_USER_TOKEN_KEY);
        }
    }

    public void callPlusActivityClick(View view) {
        int id = view.getId();
        if (id == R.id.btnStartCall) {
            String remoteUserId = mEditRemoteUserId.getText().toString().trim();
            if (TextUtils.isEmpty(remoteUserId)) {
                showToast("远端用户Id不能为空");
                return;
            }

            SessionManager.getInstance().put(REMOTE_USER_KEY, remoteUserId);
            //此处以视频通话为例
            startCall(remoteUserId, RCCallPlusMediaType.VIDEO);
        } else if (id == R.id.btnEnableMicrophoneTrue) {
            RCCallPlusClient.getInstance().enableSpeaker(true);
        } else if (id == R.id.btnEnableMicrophoneFalse) {
            RCCallPlusClient.getInstance().enableSpeaker(false);
        } else if (id == R.id.btnHangupCall) {
            RCCallPlusClient.getInstance().hangup();
        } else if (id == R.id.btnFinish) {
            //todo 如果已经在通话，则挂断 然后反初始化CallPlus
            RCCallPlusClient.getInstance().unInit();
            //关闭当前Activity
            finish();
        }
    }

    private void startCall(String remoteUserId,  RCCallPlusMediaType mediaType) {
        tvMediaType.setText("当前通话为：" + mediaType.name());
        if (mediaType == RCCallPlusMediaType.VIDEO) {
            //todo 打开摄像头采集，请提前完成摄像头、麦克风权限的动态申请
            RCCallPlusClient.getInstance().startCamera();
            RCCallPlusClient.getInstance().enableMicrophone(true);
        }

        setLocalVideoView();

        //创建远端视图对象 remoteUserId为远端用户userId
        RCCallPlusRemoteVideoView remoteVideoView = new RCCallPlusRemoteVideoView(remoteUserId, this.getApplicationContext(), false);
        //FIT: 视频帧通过保持宽高比(可能显示黑色边框)来缩放以适应视图的大小
        remoteVideoView.setRenderMode(RCCallPlusRenderMode.FIT);
        //因为远端视图显示在最顶层，为了防止远端视频视图被底部控件遮挡，所以添加如下设置：
        remoteVideoView.setZOrderOnTop(true);
        remoteVideoView.setZOrderMediaOverlay(true);

        List<RCCallPlusRemoteVideoView> remoteVideoViewList = new ArrayList<>(); remoteVideoViewList.add(remoteVideoView);
        //设置远端视图给SDK
        RCCallPlusClient.getInstance().setVideoView(remoteVideoViewList);

        FrameLayout.LayoutParams remoteVideoViewParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        remoteVideoViewParams.gravity = Gravity.CENTER_HORIZONTAL;
        //将远端视图添加到XML中显示
        //示例代码中 mRemoteVideoViewFrameLayout 为 android.widget.FrameLayout 对象
        mRemoteVideoViewFrameLayout.removeAllViews();
        mRemoteVideoViewFrameLayout.addView(remoteVideoView, remoteVideoViewParams);

        List<String> userIds = new ArrayList<>();
        userIds.add(remoteUserId);//todo remoteUserId 为被呼叫的远端用户userId
        RCCallPlusType callType = RCCallPlusType.PRIVATE;//PRIVATE: 单人通话，MULTI：多人通话
        /**
         * 开始发起呼叫
         * 该方法内部为异步执行，结果回调是注册的{@link RCCallPlusClient#setCallPlusResultListener(IRCCallPlusResultListener)} 监听的 {@link IRCCallPlusResultListener#onStartCall(RCCallPlusCode, String, List)}方法<br>
         */
        RCCallPlusClient.getInstance().startCall(userIds, callType, mediaType, null, "startCallExtra");
    }

    private void parseContactInfo(Intent intent, Context context) {
        String type = intent.getType();
        Log.d("bugtags", "parseContactInfo--->type : "+ type);
        if ((!TextUtils.equals(type, SystemContactsManger.getInstance().AUDIO_CALL) &&
            !TextUtils.equals(type, SystemContactsManger.getInstance().VIDEO_CALL))) {
            //接收到的跳转信息不是CallPlus通话记录插入的 不做处理
            //开发者可以在这里展示其他页面信息
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            //接收到的跳转信息不是CallPlus通话记录插入的 不做处理
            //开发者可以在这里展示其他页面信息
            return;
        }

        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int data3 = cursor.getColumnIndex(ContactsContract.Data.DATA3);
                int data1 = cursor.getColumnIndex(ContactsContract.Data.DATA1);
                //本示例demo只用到了用户Id 以便用于发起通话
                String userId = cursor.getString(data3);
                mEditRemoteUserId.setText(userId);

                SessionManager.getInstance().put(REMOTE_USER_KEY, userId);

                String phoneNumber = cursor.getString(data1);

                RCCallPlusMediaType mediaType = RCCallPlusMediaType.AUDIO;
                if (TextUtils.equals(type, SystemContactsManger.getInstance().AUDIO_CALL)) {
                    mediaType = RCCallPlusMediaType.AUDIO;
                } else if (TextUtils.equals(type, SystemContactsManger.getInstance().VIDEO_CALL)) {
                    mediaType = RCCallPlusMediaType.VIDEO;
                }
                String currentUserToken = SessionManager.getInstance().getString(CURRENT_USER_TOKEN_KEY);
                RCCallPlusMediaType finalMediaType = mediaType;
                connectIM(currentUserToken, new ConnectCallback() {
                    @Override
                    public void onSuccess(String t) {
                        setCallPlusListener();
                        initCallPlus();

                        tvData.setText("当前登录的用户Id：" +  RongCoreClient.getInstance().getCurrentUserId());
                        startCall(userId, finalMediaType);
                        showToast("已发起通话");
                    }

                    @Override
                    public void onError(ConnectionErrorCode e) {
                        showToast("IM登录失败，ErrorCode: "+ e.name());
                    }

                    @Override
                    public void onDatabaseOpened(DatabaseOpenStatus code) {

                    }
                });
            } else {
                //接收到的跳转信息不是CallPlus通话记录插入的 不做处理
                //开发者可以在这里展示其他页面信息
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
            //接收到的跳转信息不是CallPlus通话记录插入的 不做处理
            //开发者可以在这里展示其他页面信息
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
    }

    private void initCallPlus() {
        RCCallPlusConfig config = RCCallPlusConfig.Builder.create().build();
        /**
         * 初始化并设置通话全局配置，重复调用该方法时SDK内部会重新初始化<b/>
         *
         * @param config 设置通话全局配置<b/>
         * @return 方法调用后同步返回结果，可以在这里得到初始化是否成功<b/>
         */
        RCCallPlusResultCode resultCode = RCCallPlusClient.getInstance().init(config);
    }

    private void setCallPlusListener() {
        RCCallPlusClient.getInstance().setCallPlusEventListener(new IRCCallPlusEventListener() {

            /**
             * 本端用户通过该回调接收到通话呼叫<br>
             *
             * @param callSession   通话实体信息<br>
             */
            @Override
            public void onReceivedCall(RCCallPlusSession callSession, String extra) {
                RCCallPlusSession currentCallSession = RCCallPlusClient.getInstance().getCurrentCallSession();
                if (currentCallSession != null && !TextUtils.equals(callSession.getCallId(), currentCallSession.getCallId())) {
                    //可以使用该方法判断出，有正在进行中的通话，又有第二通通话呼入的情况<br>
                    //todo 第二通通话可以直接调用 RCCallPlusClient.getInstance().accept 方法接听，SDK内部会将第一通通话挂断
                }

                //todo SDK 的回调均为子线程调用，showDialog() 方法中存在UI操作，所以切换到主线程执行
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callSession.getMediaType() == RCCallPlusMediaType.VIDEO) {
                            //todo 打开摄像头采集，请提前完成摄像头、麦克风权限的动态申请
                            RCCallPlusClient.getInstance().startCamera();
                            RCCallPlusClient.getInstance().enableMicrophone(true);
                        }

                        setLocalVideoView();//复用发起通话逻辑中的 设置本地视频渲染视图 方法

                        showDialog(CallPlusActivity.this, "收到通话，是否接听？", "接听", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                acceptCall(callSession);
                            }
                        }, "挂断", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                RCCallPlusClient.getInstance().hangup();
                            }
                        });
                    }
                });
            }

            @Override
            public void onCallEnded(RCCallPlusSession session, RCCallPlusReason reason) {
                IRCCallPlusEventListener.super.onCallEnded(session, reason);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CallPlusActivity.this,"通话结束，callId: "+session.getCallId() +" 通话结束原因："+ reason.getValue(), Toast.LENGTH_SHORT).show();

                        if (session.getCallType() == RCCallPlusType.MULTI) {
                            return;
                        }

                        //todo 此 Demo 演示的用户信息都为登录的融云用户Id
                        //todo 正常开发下，需要使用APP侧维护的用户信息
                        String remoteUseName = "";
                        String remoteUserPhone = "";
                        String remoteUserId = "";

                        for (RCCallPlusUser callPlusUser : session.getRemoteUserList()) {
                            //这里如果需要拿到用户的姓名。可以使用融云IM的API 根据用户Id查询到相关信息
                            remoteUseName = callPlusUser.getUserId();
                            remoteUserPhone = callPlusUser.getUserId();
                            remoteUserId = callPlusUser.getUserId();
                        }
                        SystemContactsManger.getInstance().addContact(CallPlusActivity.this.getApplicationContext(), remoteUseName, remoteUserPhone, remoteUserId);


                        String callerUserId = session.getCallerUserId();
                        int type = CallLog.Calls.INCOMING_TYPE;
                        if (TextUtils.equals(callerUserId, RongCoreClient.getInstance().getCurrentUserId())) {
                            type = CallLog.Calls.OUTGOING_TYPE;
                        }
                        SystemContactsManger.getInstance().insertCallLog(CallPlusActivity.this.getApplicationContext(), remoteUseName, remoteUserPhone, type, session.getMediaType(), session.getCallId(), System.currentTimeMillis());
                    }
                });
            }

            /**
             * 远端用户状态改变监听<br>
             *
             * @param callId 通话Id<br>
             * @param userId 用户Id<br>
             * @param status 该用户当前状态<br>
             * @param reason 该用户当前状态原因<br>
             */
            @Override
            public void onRemoteUserStateChanged(String callId, String userId, RCCallPlusUserSessionStatus status, RCCallPlusReason reason) {
                IRCCallPlusEventListener.super.onRemoteUserStateChanged(callId, userId, status, reason);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder("通话 ");
                        stringBuilder.append(callId).append(" 中的远端用户 ").append(userId).append(" 当前状态为 ");
                        switch (status) {
                            case IDLE:
                                stringBuilder.append("空闲");
                                break;
                            case CALLING:
                                stringBuilder.append("呼叫中");
                                break;
                            case INVITED:
                                stringBuilder.append("被邀请中");
                                break;
                            case RINGING:
                                stringBuilder.append("响铃中");
                                break;
                            case BUSY_LINE_RINGING:
                                stringBuilder.append("忙线(响铃中)");
                                break;
                            case BUSY_LINE_WAIT:
                                stringBuilder.append("忙线(通话中)");
                                break;
                            case CONNECTING:
                                stringBuilder.append("已接听，连接中");
                                break;
                            case ON_CALL:
                                stringBuilder.append("通话中");
                                break;
                            case ENDED:
                                stringBuilder.append("通话已结束");
                                break;
                            case NO_ANSWER:
                                stringBuilder.append("未应答");
                                break;
                            case MISSED:
                                stringBuilder.append("未接听");
                                break;
                            case CANCELED:
                                stringBuilder.append("已取消");
                                break;
                            case DECLINED:
                                stringBuilder.append("已拒绝");
                                break;
                            case ERROR:
                                stringBuilder.append("错误");
                                break;
                        }
                        Toast.makeText(CallPlusActivity.this, stringBuilder.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            /**
             * 远端用户麦克风状态改变监听<br>
             *
             * @param callId 通话Id<br>
             * @param userId 用户Id<br>
             * @param disabled 麦克风是否可用，true:麦克风为关闭状态。false：麦克风为开启状态。<br>
             */
            @Override
            public void onRemoteMicrophoneStateChanged(String callId, String userId, boolean disabled) {
                IRCCallPlusEventListener.super.onRemoteMicrophoneStateChanged(callId, userId, disabled);
            }

            /**
             * 远端用户摄像头状态改变监听<br>
             *
             * @param callId 通话Id<br>
             * @param userId 用户Id<br>
             * @param disabled 摄像头是否可用，true:摄像头为关闭状态。false：摄像头为开启状态。<br>
             */
            @Override
            public void onRemoteCameraStateChanged(String callId, String userId, boolean disabled) {
                IRCCallPlusEventListener.super.onRemoteCameraStateChanged(callId, userId, disabled);
            }

            /**
             * 远端用户调用请求切换媒体 {@link RCCallPlusClient#requestChangeMediaType(RCCallPlusMediaType)} 成功后收到 <br>
             *
             * @param transactionId 事务id<br>
             * @param userId 发起人id<br>
             * @param mediaType 媒体类型<br>
             */
            @Override
            public void onReceivedChangeMediaTypeRequest(String transactionId, String userId, RCCallPlusMediaType mediaType) {
                IRCCallPlusEventListener.super.onReceivedChangeMediaTypeRequest(transactionId, userId, mediaType);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder("远端用户 ");
                        stringBuilder.append(userId).append(" 请求切换媒体类型为 ").append(mediaType.name());
                        stringBuilder.append("，").append("是否同意？");
                        //todo showDialog 方法实现在快速集成文档中有示例代码
                        showDialog(CallPlusActivity.this, stringBuilder.toString(), "同意", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //响应通话中的切换媒体类型请求
                                RCCallPlusClient.getInstance().replyChangeMediaType(transactionId, true);
                                if (mediaType == RCCallPlusMediaType.AUDIO) {
                                    RCCallPlusClient.getInstance().stopCamera();
                                } else {
                                    RCCallPlusClient.getInstance().startCamera();
                                }
                            }
                        }, "拒绝", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //响应通话中的切换媒体类型请求
                                RCCallPlusClient.getInstance().replyChangeMediaType(transactionId, false);
                            }
                        });
                    }
                });
            }

            @Override
            public void onReceivedChangeMediaTypeResult(String transactionId, String userId, RCCallPlusMediaType mediaType, RCCallPlusMediaTypeChangeResult result) {
                IRCCallPlusEventListener.super.onReceivedChangeMediaTypeResult(transactionId, userId, mediaType, result);
            }

            @Override
            public void onReceivedCallRecord(RCCallPlusCallRecord record) {
                IRCCallPlusEventListener.super.onReceivedCallRecord(record);
            }

            @Override
            public void onReceivedCallPlusSummaryMessage(Message message) {
                IRCCallPlusEventListener.super.onReceivedCallPlusSummaryMessage(message);
                RCCallPlusSummaryMessageContent messageContent = (RCCallPlusSummaryMessageContent) message.getContent();
            }
        });

        RCCallPlusClient.getInstance().setCallPlusResultListener(new IRCCallPlusResultListener() {

            /**
             * 发起通话方法结果回调<br>
             *
             * @param code 方法请求结果<br>
             * @param callId 通话Id<br>
             * @param busyUserList 呼叫成功后，返回被邀请人列表中的忙线用户列表<br>
             */
            @Override
            public void onStartCall(RCCallPlusCode code, String callId, List<RCCallPlusUser> busyUserList) {
                IRCCallPlusResultListener.super.onStartCall(code, callId, busyUserList);
                showToast("发起通话 " + getCodeDescription(code));
            }

            /**
             * 接听通话结果回调<br>
             *
             * @param code 方法请求结果<br>
             * @param callId 通话Id<br>
             */
            @Override
            public void onAccept(RCCallPlusCode code, String callId) {
                IRCCallPlusResultListener.super.onAccept(code, callId);
                showToast("接听通话 " + getCodeDescription(code));
            }

            /**
             * 挂断指定通话结果回调<br>
             *
             * @param code 方法请求结果<br>
             * @param callId 通话Id<br>
             */
            @Override
            public void onHangup(RCCallPlusCode code, String callId) {
                IRCCallPlusResultListener.super.onHangup(code, callId);
                showToast("挂断通话 " + getCodeDescription(code));
            }

            /**
             * 通话中请求切换媒体类型方法结果回调<br>
             *
             * @param code 方法请求结果<br>
             * @param callId 通话Id<br>
             * @param transactionId 事务Id<br>
             * @param mediaType 媒体类型<br>
             */
            @Override
            public void onRequestChangeMediaType(RCCallPlusCode code, String callId, String transactionId, RCCallPlusMediaType mediaType) {
                IRCCallPlusResultListener.super.onRequestChangeMediaType(code, callId, transactionId, mediaType);
                showToast("通话中请求切换媒体类型 " + getCodeDescription(code));
            }

            /**
             * 响应通话中的切换媒体类型方法结果回调<br>
             *
             * @param code 方法请求结果<br>
             * @param callId 通话Id<br>
             * @param transactionId 事务Id<br>
             */
            @Override
            public void onReplyChangeMediaType(RCCallPlusCode code, String callId, String transactionId, boolean isAgreed) {
                IRCCallPlusResultListener.super.onReplyChangeMediaType(code, callId, transactionId, isAgreed);
            }
        });
    }


    /**
     * 设置本地视频渲染视图
     */
    private void setLocalVideoView() {
        //创建本地视图对象
        RCCallPlusLocalVideoView localVideoView = new RCCallPlusLocalVideoView(this.getApplicationContext());
        //FIT: 视频帧通过保持宽高比(可能显示黑色边框)来缩放以适应视图的大小
        localVideoView.setRenderMode(RCCallPlusRenderMode.FIT);
        localVideoView.setZOrderOnTop(false);
        localVideoView.setZOrderMediaOverlay(false);

        //设置本地视图给SDK
        RCCallPlusClient.getInstance().setVideoView(localVideoView);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;//在父布局中横向居中显示
        //将本地视图添加到XML中显示
        //示例代码中 mLocalVideoViewFrameLayout 为 android.widget.FrameLayout 对象
        mLocalVideoViewFrameLayout.removeAllViews();
        mLocalVideoViewFrameLayout.addView(localVideoView, params);
    }

    private void acceptCall(RCCallPlusSession callSession) {
        tvMediaType.setText("当前通话为：" + callSession.getMediaType().name());
        setRemoteUserVideoView(callSession.getRemoteUserList());

        /**
         * 开始接听通话
         * 该方法内部为异步执行，结果回调是注册的{@link RCCallPlusClient#setCallPlusResultListener(IRCCallPlusResultListener)} 监听的 {@link IRCCallPlusResultListener#onAccept(RCCallPlusCode, String)}方法<br>
         */
        RCCallPlusClient.getInstance().accept(callSession.getCallId());
    }

    /**
     * 设置远端用户视频渲染视图
     */
    private void setRemoteUserVideoView(List<RCCallPlusUser> remoteUserList) {

        List<String> removeVideoViewList = new ArrayList<>();

        List<RCCallPlusRemoteVideoView> remoteVideoViewList = new ArrayList<>();
        for (RCCallPlusUser callPlusUser : remoteUserList) {
            RCCallPlusRemoteVideoView remoteVideoView = new RCCallPlusRemoteVideoView(callPlusUser.getUserId(), this.getApplicationContext(), false);
            //视频帧通过保持宽高比(可能显示黑色边框)来缩放以适应视图的大小
            remoteVideoView.setRenderMode(RCCallPlusRenderMode.FIT);
            remoteVideoViewList.add(remoteVideoView);
            //本示例代码中，因为远端视图显示在最顶层，为了防止远端视频视图被底部控件(视图)遮挡，所以添加如下设置：
            remoteVideoView.setZOrderOnTop(true);
            remoteVideoView.setZOrderMediaOverlay(true);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            //todo 将每个远端视图(remoteVideoView)添加到XML中显示，远端为多人时，需要添加给多个控件显示，本示例代码仅展示一个远端用户情况
            mRemoteVideoViewFrameLayout.removeAllViews();
            mRemoteVideoViewFrameLayout.addView(remoteVideoView, params);

            removeVideoViewList.add(callPlusUser.getUserId());
        }

        //todo 添加远端新的视频视图前，先清理一次。因为SDK支持预先设置，防止本次通话设置的视图和开发者显示视图不致于导致的黑屏
        RCCallPlusClient.getInstance().removeVideoView(removeVideoViewList);
        /**
         * 设置远端用户视频流渲染视图给SDK
         * 若没有为远端用户设置视频渲染视图，则不会产生该用户的视频流的下行流量
         */
        RCCallPlusClient.getInstance().setVideoView(remoteVideoViewList);
    }

    private AlertDialog showDialog(Context context, String content, String positiveBtn, final DialogInterface.OnClickListener positiveListener, final String negativeBtn, final DialogInterface.OnClickListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder = builder.setMessage(content);
        builder.setCancelable(false);
        if (!TextUtils.isEmpty(positiveBtn)) {
            builder.setPositiveButton(positiveBtn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (positiveListener != null) {
                        positiveListener.onClick(dialog, which);
                    } else {
                        dialog.dismiss();
                    }
                }
            });
        } if (!TextUtils.isEmpty(negativeBtn)) {
            builder.setNegativeButton(negativeBtn, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (negativeListener != null) {
                        negativeListener.onClick(dialog, which);
                    } else {
                        dialog.dismiss();
                    }
                }
            });
        } return builder.show();
    }

    private String getCodeDescription(RCCallPlusCode code) {
        String str = ""; if (code == RCCallPlusCode.SUCCESS) {
            str = "成功";
        } else {
            str = "失败，错误原因：" + code.getValue();
        } return str;
    }
}