package com.example.callplusdemo;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.telecom.TelecomManager;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import cn.rongcloud.calllib.api.RCCallPlusOrder;
import cn.rongcloud.callplus.api.RCCallPlusCallRecord;
import cn.rongcloud.callplus.api.RCCallPlusClient;
import cn.rongcloud.callplus.api.RCCallPlusCode;
import cn.rongcloud.callplus.api.RCCallPlusMediaType;
import cn.rongcloud.callplus.api.RCCallPlusRecordInfo;
import cn.rongcloud.callplus.api.RCCallPlusType;
import cn.rongcloud.callplus.api.callback.IRCCallPlusResultListener;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.IRongCoreListener.ConnectionStatusListener.ConnectionStatus;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class SystemContactsManger {

    private static class Holder {
        static SystemContactsManger utils = new SystemContactsManger();
    }

    private SystemContactsManger() {}
    public static SystemContactsManger getInstance() {
        return Holder.utils;
    }

    private final String ACCOUNT_NAME = "SystemContactsCallDemo";
    //必须和 app/src/main/res/xml/sync_contacts.xml 文件中内容一致
    private final String ACCOUNT_TYPE = "com.example.callplusdemo";

    //必须和 app/src/main/res/xml/contacts.xml 文件中内容一致
    //需要在 AndroidManifest.xml 中使用 intent-filter 注册
    public final String AUDIO_CALL = "vnd.android.cursor.item/vnd.com.example.callplusdemo.audiocall";

    //必须和 app/src/main/res/xml/contacts.xml 文件中内容一致
    //需要在 AndroidManifest.xml 中使用 intent-filter 注册
    public final String VIDEO_CALL = "vnd.android.cursor.item/vnd.com.example.callplusdemo.videocall";
    private TelephonyManager mTelephonyManager;
    private MyPhoneStateListener mPhoneStateListener;
    private MyCallStateListener mCallStateListener;
    public static final String START_SOURCE = "ACTIVITY_START_SOURCE";
    public static final String REMOTE_USER_PHONE_NUMBER = "remoteUserPhoneNumber";


    private final String[] SELECTION_ARGS = new String[]{ACCOUNT_TYPE};
    private final String[] PROJECTION = {
        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.TYPE,
        ContactsContract.CommonDataKinds.Phone.LABEL,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        RawContacts.ACCOUNT_TYPE
    };

    public void addAccount(Context context) {
        try {
            Account account = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
            AccountManager.get(context).addAccountExplicitly(account, "", null);
        } catch (Exception e) {
            Log.e("bugtags", "SystemContactsManger-->addAccount()-->exception: " +e.getMessage());
        }
    }

    /**
     * 添加联系人 并将自定义信息插入联系人下的记录中
     *
     * @param remoteUseName 远端用户名称
     * @param remoteUserPhone 远端用户电话号码
     * @param remoteUserId  远端用户Id
     */
    public void addContact(Context context, String remoteUseName, String remoteUserPhone, String remoteUserId) {
        if (!hasContactsPermission(context)) {
            return;
        }

        deleteOld(context, remoteUserId);

        ArrayList<ContentProviderOperation> providerOperationList = new ArrayList<>();

        ContentProviderOperation operationId = ContentProviderOperation.
            newInsert(appendQueryParameter(RawContacts.CONTENT_URI))
            .withYieldAllowed(true)
            .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
            .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .withValue(RawContacts.SYNC2, remoteUserId)
            .build();
        providerOperationList.add(operationId);

        ContentProviderOperation operationUngroupedVisible =   ContentProviderOperation.newInsert(
                appendQueryParameter(ContactsContract.Settings.CONTENT_URI))
            .withYieldAllowed(true)
            .withValue(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
            .withValue(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .withValue(ContactsContract.Settings.UNGROUPED_VISIBLE, 1)
            .build();
        providerOperationList.add(operationUngroupedVisible);

        ContentProviderOperation operationName = ContentProviderOperation.newInsert(appendQueryParameter(ContactsContract.Data.CONTENT_URI))
            .withYieldAllowed(true)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, remoteUseName)
            .build();
        providerOperationList.add(operationName);

        ContentProviderOperation operationPhoneNumber=ContentProviderOperation.newInsert(appendQueryParameter(ContactsContract.Data.CONTENT_URI))
            .withYieldAllowed(true)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, remoteUserPhone)
            .build();
        providerOperationList.add(operationPhoneNumber);

        // custom mimeType
        ContentProviderOperation operationVoiceRawContactId = ContentProviderOperation.newInsert(appendQueryParameter(ContactsContract.Data.CONTENT_URI))
            .withYieldAllowed(true)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, AUDIO_CALL)
            .withValue(ContactsContract.Data.DATA1, remoteUserPhone)
            .withValue(ContactsContract.Data.DATA2, ACCOUNT_NAME +" Voice Call " + remoteUserPhone)
            .withValue(ContactsContract.Data.DATA3, remoteUserId)
            .build();
        providerOperationList.add(operationVoiceRawContactId);

        ContentProviderOperation operationVideoRawContactId = ContentProviderOperation.newInsert(appendQueryParameter(ContactsContract.Data.CONTENT_URI))
            .withYieldAllowed(true)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, VIDEO_CALL)
            .withValue(ContactsContract.Data.DATA1, remoteUserPhone)
            .withValue(ContactsContract.Data.DATA2, ACCOUNT_NAME +" Video Call " + remoteUserPhone)
            .withValue(ContactsContract.Data.DATA3, remoteUserId)
            .build();
        providerOperationList.add(operationVideoRawContactId);


        ContentResolver contentResolver = context.getContentResolver();

        try {
            ContentProviderResult[] contentProviderResults = contentResolver.applyBatch(ContactsContract.AUTHORITY, providerOperationList);
            for (ContentProviderResult contentProviderResult : contentProviderResults) {
                //Log.e("bugtags", "contentProviderResult: " +contentProviderResult.toString());
            }
        } catch (OperationApplicationException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private Uri appendQueryParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    }

    private boolean hasContactsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        }

        Cursor cursor = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION,null,null,null);
            if (cursor == null || cursor.getCount() == 0) {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return true;
    }

    private void deleteOld(Context context, String id) {
        Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build();

        ContentResolver contentResolver = context.getContentResolver();

        String[] selectionArgs = new String[]{id};
        contentResolver.delete(rawContactUri, RawContacts.SYNC2 + " = ?", selectionArgs);
//        Log.d("bugtags", "SystemContactsManger-->deleteOld()-->id: " + id);
    }

    public void clearAll(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(RawContacts.CONTENT_URI, RawContacts.ACCOUNT_TYPE + " = ?", SELECTION_ARGS);
        Log.d("bugtags", "SystemContactsManger-->clearAll()");
    }

    /**
     * 向通话记录中插入自定义数据
     *
     * @param remoteUserName 远端用户名称
     * @param remoteUserPhone   远端用户电话号码
     * @param callId
     * @param type
     * The type of the call (incoming, outgoing or missed).
     * <P>Type: INTEGER (int)</P>
     *
     * <p>
     * Allowed values:
     * <ul>
     * <li>CallLog.Calls.INCOMING_TYPE</li>
     * <li>CallLog.Calls.OUTGOING_TYPE</li>
     * <li>CallLog.Calls.MISSED_TYPE</li>
     * <li>CallLog.Calls.VOICEMAIL_TYPE</li>
     * <li>CallLog.Calls.REJECTED_TYPE</li>
     * <li>CallLog.Calls.BLOCKED_TYPE</li>
     * <li>CallLog.Calls.ANSWERED_EXTERNALLY_TYPE</li>
     * </ul>
     * </p>
     */
    public void insertCallLog(Context context, String remoteUserName , String remoteUserPhone, int type, RCCallPlusMediaType mediaType, String callId, long callEndTime) {
        Log.d("bugtags", "start-insert-->callId: " +callId + " ,remoteUserPhone: " +remoteUserPhone +" ,callEndTime: " +callEndTime);

        //在通讯录查询是否存在该联系人，若存在则把名字信息也插入到通话记录中
        String name = remoteUserName;
        String[] cols = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        //设置查询条件
        String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + "='"+remoteUserPhone+"'";
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            cols, selection, null, null);
        int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
        if (cursor.getCount()>0){
            cursor.moveToFirst();
            name = cursor.getString(nameFieldColumnIndex);
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(CallLog.Calls.CACHED_NAME, name);
        values.put(CallLog.Calls.NUMBER, remoteUserPhone);
        values.put(CallLog.Calls.TYPE, type);
        values.put(CallLog.Calls.DATE, callEndTime);
        values.put(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, callId);

        ContentResolver contentResolver = context.getContentResolver();
        Uri insertedUri = contentResolver.insert(CallLog.Calls.CONTENT_URI, values);
    }

    public boolean closedSystemCallPage(Context context) {
        boolean callSuccess = false;
        int androidSdkVersion = android.os.Build.VERSION.SDK_INT;
        Log.d("bugtags", "endCall---androidSdkVersion: " + androidSdkVersion);
        try {
            if (androidSdkVersion >= android.os.Build.VERSION_CODES.P && context.checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                // >=Android 9,需打开 Manifest.permission.ANSWER_PHONE_CALLS 权限
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                telecomManager.endCall();
                callSuccess = true;
                Log.d("bugtags", "telecomManager.endCall() finish");
            } else {
                // 1.获取TelephonyManager
                // 2.获取TelephonyManager.class
                // 3.反射调用TelephonyManager的 getITelephony方法获取ITelephony
                // 4.挂断电话ITelephony.endCall
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                Class c = Class.forName(tm.getClass().getName());
                Method m = c.getDeclaredMethod("getITelephony");
                m.setAccessible(true);
                com.android.internal.telephony.ITelephony telephonyService = (com.android.internal.telephony.ITelephony) m.invoke(tm);
                if (telephonyService != null) {
                    callSuccess = telephonyService.endCall();
                    Log.d("bugtags", " telephonyService.endCall finish");
                } else {
                    Log.e("bugtags", " telephonyService.endCall telephonyService is empty");
                }
            }
        } catch (Exception e) {
        }
        return callSuccess;
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);

            Log.e("bugtags", "MyPhoneStateListener-->onCallStateChanged--->state: " + state +" phoneNumber: " +phoneNumber+ " ,currentThread: " + Thread.currentThread().getName());

            if (state == TelephonyManager.CALL_STATE_IDLE) {
            } else if (state == TelephonyManager.CALL_STATE_RINGING) {
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                //startCallPlusActivity(phoneNumber);
            } else if (state == TelephonyManager.CALL_COMPOSER_STATUS_ON) {
            } else if (state == TelephonyManager.CALL_COMPOSER_STATUS_OFF) {

            }
        }
    }

    @RequiresApi(api = VERSION_CODES.S)
    private static class MyCallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            switch (state){
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i("bugtags", "MyCallStateListener-->手机状态：空闲状态");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
            }
        }
    }

    /**
     * 废弃该方法 使用 {@link CallPlusPhoneStateReceiver } 监听
     */
    public void registerPhoneStateListener(Context context) {
//        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            mCallStateListener = new MyCallStateListener();
//            mTelephonyManager.registerTelephonyCallback(this.mContext.getMainExecutor(), mCallStateListener);
        } else {
//            mPhoneStateListener = new MyPhoneStateListener();
//            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
//            Log.e("bugtags", "registerPhoneStateListener-->listener: " +mPhoneStateListener.hashCode());
        }
    }

    public void unRegisterPhoneStateListener() {
        try {
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(null, PhoneStateListener.LISTEN_CALL_STATE);
            }
            mPhoneStateListener = null;
            mTelephonyManager = null;
        } catch (Exception e) {
        }
        Log.e("bugtags", "unRegisterPhoneStateListener");
    }

    public void queryCallLog(Context context, String remoteUserPhone) {
        Log.d("bugtags","queryCallLog-->remoteUserPhone: " +remoteUserPhone);
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver == null) {
            Log.e("bugtags","queryCallLog-->ContentResolver is empty");
            return;
        }

        String[] projection = {
           Calls.NUMBER,
            Calls.DATE,
            Calls.DURATION,
            Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,
        };

        String selection = Calls.NUMBER + "='"+remoteUserPhone+"'";
        Cursor cursor = contentResolver.query(Calls.CONTENT_URI, projection, selection, null, null);

        if (cursor == null) {
            Log.e("bugtags","queryCallLog-->Cursor is empty");
            return;
        }

        try {
            if (cursor.getCount() <= 0) {
                Log.e("bugtags","queryCallLog-->Cursor getCount: " + cursor.getCount());
                return;
            }
            while (cursor.moveToNext()) {
                for (int i = 0; i < cursor.getColumnNames().length; i++) {
//                    String info = cursor.getString(i);
//                    Log.e("bugtags","queryCallLog-->info: " +info +" ," + cursor.getColumnNames()[i]);

                    int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    String number = cursor.getString(numberIndex);

                    int dataIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                    long date = cursor.getLong(dataIndex);
                    //
                    int PHONE_ACCOUNT_COMPONENT_NAMEIndex = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME);
                    String PHONE_ACCOUNT_COMPONENT_NAME = cursor.getString(PHONE_ACCOUNT_COMPONENT_NAMEIndex);
                    Log.e("bugtags","queryCallLog-->number: " + number +" , custom : " + PHONE_ACCOUNT_COMPONENT_NAME);
                }
            }
        } catch (Exception e) {
            Log.e("bugtags","queryCallLog-->Exception: " +e.getMessage());
        } finally {
            cursor.close();
        }
    }

    public void getCallRecordsFromServer(Context context) {
        IRongCoreListener.ConnectionStatusListener.ConnectionStatus connectionStatus = RongCoreClient.getInstance().getCurrentConnectionStatus();
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            Log.e("bugtags", "onGetCallRecordsFromServer-->getCurrentConnectionStatus: " + connectionStatus.name());
            return;
        }

        RCCallPlusClient.getInstance().setCallPlusResultListener(new IRCCallPlusResultListener() {
            @Override
            public void onGetCallRecordsFromServer(RCCallPlusCode code, RCCallPlusRecordInfo record, RCCallPlusOrder order) {
                IRCCallPlusResultListener.super.onGetCallRecordsFromServer(code, record, order);
                Log.d("bugtags", "onGetCallRecordsFromServer-->getCompletedCallRecords: " + record.getCompletedCallRecords().size());
                for (RCCallPlusCallRecord callRecord : record.getCompletedCallRecords()) {
                    if (callRecord.getCallType() == RCCallPlusType.MULTI) {
                        return;
                    }

                    //todo 此 Demo 演示的用户信息都为登录的融云用户Id
                    //todo 正常开发下，需要使用APP侧维护的用户信息
                    String remoteUserId = "";
                    for (String callUserId : callRecord.getCallUserIds()) {
                        remoteUserId = callUserId;
                        //这里如果需要拿到用户的姓名。可以使用融云IM的API 根据用户Id查询到相关信息
                    }

                    Log.d("bugtags", "onGetCallRecordsFromServer--callId: " + callRecord.getCallId()  +" remoteUserId :" + remoteUserId);

                    if (!contains(context, remoteUserId, callRecord.getCallId())) {
                        addContact(context, remoteUserId, remoteUserId, remoteUserId);

                        String callerUserId = callRecord.getCallerUserId();
                        int type = CallLog.Calls.INCOMING_TYPE;
                        if (TextUtils.equals(callerUserId, RongCoreClient.getInstance().getCurrentUserId())) {
                            type = CallLog.Calls.OUTGOING_TYPE;
                        }

                        long callEndTime = callRecord.getEndTime() == 0 ? callRecord.getStartTime() : callRecord.getEndTime();
                        if (callEndTime == 0) {
                            callEndTime = callRecord.getSyncTime();
                        }
                        insertCallLog(context, remoteUserId, remoteUserId, type, callRecord.getMediaType(), callRecord.getCallId(), callEndTime);
                    }
                }
            }
        });

        /**
         * 获取当前用户通话记录。
         *
         * <p>调用该方法后会触发以下回调：
         *
         * <ul>
         *   <li>该方法内部为异步执行。本地用户通过注册的 {@link
         *       RCCallPlusClient#setCallPlusResultListener(IRCCallPlusResultListener)} 监听的 {@link
         *       IRCCallPlusResultListener#onGetCallRecordsFromServer(RCCallPlusCode,
         *       RCCallPlusRecordInfo, RCCallPlusOrder)} 方法获取执行结果。
         * </ul>
         *
         * @param syncTime 同步时间戳(单位：毫秒) 。首次可传 -1（0和-1效果一致），会根据 `order` 指定的查询顺序，返回最近的 n 条或最远的 n 条数据。
         * @param count 查询的条数
         * @param order 查询排序规则。默认为正序
         */
        RCCallPlusClient.getInstance().getCallRecordsFromServer(0, 1000, RCCallPlusOrder.ASCENDING);
    }

    /**
     * 通过用户remoteUserPhone 和 callId 判断该条记录是否已经插入过通话记录
     *
     * @param context
     * @param remoteUserPhone
     * @param callId
     * @return true:已经存在通话记录中。 false：不存在通话记录中
     */
    private boolean contains(Context context, String remoteUserPhone, String callId) {
        if (TextUtils.isEmpty(remoteUserPhone)) {
            return true;
        }
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver == null) {
            Log.e("bugtags","queryCallLog-->ContentResolver is empty");
            return false;
        }

        String[] projection = {
            Calls.NUMBER,
            Calls.DATE,
            Calls.DURATION,
            Calls.TYPE,
            CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,
        };

        String selection = Calls.NUMBER + "='"+remoteUserPhone+"'";
        Cursor cursor = contentResolver.query(Calls.CONTENT_URI, projection, selection, null, null);

        if (cursor == null) {
            Log.e("bugtags","queryCallLog-->Cursor is empty");
            return false;
        }
        boolean contains = false;

        try {
            if (cursor.getCount() <= 0) {
                Log.e("bugtags","queryCallLog-->Cursor getCount: " + cursor.getCount());
                return false;
            }
            while (cursor.moveToNext()) {

                for (int i = 0; i < cursor.getColumnNames().length; i++) {
//                    String info = cursor.getString(i);
//                    Log.e("bugtags","queryCallLog-->info: " +info +" ," + cursor.getColumnNames()[i]);

                    int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    String number = cursor.getString(numberIndex);

                    int dataIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                    long date = cursor.getLong(dataIndex);

                    int callIdIndex = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME);
                    String call_Id = cursor.getString(callIdIndex);

                    if (TextUtils.equals(number, remoteUserPhone) && TextUtils.equals(call_Id, callId)) {
                        contains = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("bugtags","queryCallLog-->Exception: " +e.getMessage());
        } finally {
            cursor.close();
        }

        return contains;
    }
}
