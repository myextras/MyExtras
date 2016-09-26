package au.com.myextras;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(getClass().getName(), intent.getAction() + " received");
        }

        SyncService.requestSync(context);
    }

}
