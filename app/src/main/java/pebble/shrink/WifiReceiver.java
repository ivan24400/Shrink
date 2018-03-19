package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ip address " + DeviceOperations.getMyIpAddress());
        if (intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
            WifiOperations.refreshDeviceCount(context);

            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                Log.d(TAG, "wifi hotspot enabled state: " + state);
            } else {
                Log.d(TAG, "wifi hotspot other state: " + state);
            }
        }
    }
}
