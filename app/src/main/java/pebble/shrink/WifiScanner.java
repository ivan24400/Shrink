package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class WifiScanner extends BroadcastReceiver {

    private static final String TAG = "WifiScanner";

    @Override
    public void onReceive(final Context context, Intent intent) {

        final String action = intent.getAction();
        switch (action) {

            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                Log.d(TAG, "wifi state changed");
                if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_DISABLED) {
                    ShareResource.setConnected(context, false);
                    Log.d(TAG, "WIFI STATE DISABLED");
                }
                break;

            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                if (!WifiOperations.isConnected()) {
                    List<ScanResult> result = WifiOperations.getWifiManager().getScanResults();
                    for (ScanResult scan : result) {
                        if (scan.SSID.contains(context.getString(R.string.sr_ssid))) {
                            Log.d(TAG, "shrink connecting to: " + scan.SSID);
                            WifiOperations.setWifiSsid(scan.SSID);
                            WifiOperations.setWifiEnabled(true);
                        }
                    }
                }
                break;

            case ConnectivityManager.CONNECTIVITY_ACTION:

                ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = conMan.getActiveNetworkInfo();
                if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                    if (netInfo.isConnected()) {
                        final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        final WifiInfo wi = wm.getConnectionInfo();
                        if (wi != null && !wi.getSSID().trim().contains(context.getString(R.string.sr_ssid))) {
                            Log.d(TAG, "wifi scanner different: " + wi.getSSID());
                            wm.disableNetwork(wi.getNetworkId());
                            wm.disconnect();
                            return;

                        } else {
                            Log.d(TAG, "wifi scanner connected: " + wi.getSSID());

                            ((ShareResource) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DeviceOperations.removeProgress();
                                    ShareResource.setConnected(context, true);
                                }
                            });

                            Log.d(TAG, "wifi scanner server: " + wi.getSSID().split("_")[1].replace("\"", ""));

                            Intent dintent = new Intent(context, SlaveDeviceService.class);
                            dintent.putExtra(SlaveDeviceService.EXTRA_PORT, Integer.parseInt(wi.getSSID().split("_")[1].replace("\"", "")));
                            context.startService(dintent);

                        }
                    }
                } else {
                    ShareResource.setConnected(context, false);
                    Log.d("WifiScanner", "Wifi Disconnected");
                }
                break;
        }
    }
}
