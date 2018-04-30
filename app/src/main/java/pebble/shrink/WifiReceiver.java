package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {

            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            if (state % 10 == WifiManager.WIFI_STATE_DISABLED) {
                if (CompressFile.swRemote.isChecked()) {
                    CompressFile.swRemote.setChecked(false);
                }
                if (DistributorService.isServerOn) {
                    Intent tintent = new Intent(context, DistributorService.class);
                    tintent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
                    context.startService(tintent);
                }
            } else if (state % 10 == WifiManager.WIFI_STATE_ENABLED && DistributorService.isServerOn) {
                if (!CompressFile.swRemote.isChecked()) {
                    CompressFile.swRemote.setChecked(true);
                }
            }
        }
    }
}
