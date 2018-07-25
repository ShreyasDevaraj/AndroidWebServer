package lift.com.androidwebserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by sdevaraj on 6/1/2018.
 */

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, HttpsService.class);
        context.startService(serviceIntent);
        Log.e("TAG", "Starting service after reboot ");
    }
}
