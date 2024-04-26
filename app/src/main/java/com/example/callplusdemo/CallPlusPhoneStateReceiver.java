package com.example.callplusdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallPlusPhoneStateReceiver extends BroadcastReceiver {

    private static final String TAG = "PhoneStateReceiver";
    // 21以上会回调两次(状态值一样)
    private static String twice = "";
    private TelephonyManager mTelephonyManager;

    public int getCallState(Context context) {
        if (context == null) {
            return -1;
        }

        if (mTelephonyManager == null) {
            mTelephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager.getCallState();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (!intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.e("bugtags", "CallPlusPhoneStateReceiver--->action: " +action);
            return;
        }

        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        Log.i("bugtags", "CallPlusPhoneStateReceiver--->phoneNumber-->: " + phoneNumber);

        SystemContactsManger.getInstance().closedSystemCallPage(context);

        Intent intentStart = new Intent("com.example.callplusdemo.action.call");
        intentStart.putExtra(SystemContactsManger.START_SOURCE, 1);
        intentStart.putExtra(SystemContactsManger.REMOTE_USER_PHONE_NUMBER, phoneNumber);
        intentStart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentStart);
    }
}
