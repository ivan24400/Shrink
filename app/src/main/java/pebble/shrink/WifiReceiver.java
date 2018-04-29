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
        if (intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {

            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            Log.d(TAG, "wifi hotspot state: " + state);
            if(state%10 == WifiManager.WIFI_STATE_DISABLED){
                if(CompressFile.swRemote.isChecked()){
                    CompressFile.swRemote.setChecked(false);
                }else if(state%10 == WifiManager.WIFI_STATE_ENABLED){
                    CompressFile.swRemote.setChecked(true);
                }
            }
        }
    }
}
