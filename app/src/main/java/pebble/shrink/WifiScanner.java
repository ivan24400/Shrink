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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Ivan on 18-03-2018.
 */

public class WifiScanner extends BroadcastReceiver {

    private static final String TAG = "WifiScanner";


    @Override
    public void onReceive(final Context context, Intent intent) {

        final String action = intent.getAction();
        switch (action) {

            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                Log.d(TAG,"wifi state changed");
                int ext = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN);
                switch(ext){
                    case WifiManager.WIFI_STATE_DISABLED:
                        ((ShareResource)context).setConnected(false);
                        Log.d(TAG,"WIFI STATE DISABLED");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        Log.d(TAG,"WIFI STATE DISABLING");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d(TAG,"WIFI STATE ENABLED");
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        Log.d(TAG,"WIFI STATE ENABLING");
                        break;
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        Log.d(TAG,"WIFI STATE UNKNOWN");
                        break;
                }


            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                Log.d(TAG,"wifi scan results available");
                if (!WifiOperations.isConnected()) {
                    List<ScanResult> result = WifiOperations.getWifiManager().getScanResults();
                    for (ScanResult scan : result) {
                        Log.d(getClass().getSimpleName(), "ssid found: " + scan.toString());
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
                    Log.d(TAG, "Have Wifi Connection " + netInfo.toString());

                    if (netInfo.isConnected()) {
                        final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        final WifiInfo wi = wm.getConnectionInfo();
                        if (wi != null && !wi.getSSID().trim().contains(context.getString(R.string.sr_ssid))) {
                            Log.d(TAG, "wifi different: " + wi.getSSID());
                            wm.disconnect();
                            return;

                        } else {
                            Log.d(TAG, "wifiscanner connected " + wi.getSSID());

                            ((ShareResource) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DeviceOperations.removeProgress();
                                    ((ShareResource) context).setConnected(true);
                                }
                            });
                            DeviceSlave.batteryClass = ((ShareResource) context).mpriority.getSelectedItemPosition() == 0 ? 'B' : 'A';

                            try {
                                InetAddress addr = InetAddress.getByName(context.getString(R.string.sr_server_ip));
                                Log.d(TAG, "wifiscanner server: " + addr + " , " + wi.getSSID().split("_")[1].replace("\"", ""));

                                ShareResource.deviceSlaveThread = new Thread(new DeviceSlave((ShareResource)context,addr,
                                        Integer.parseInt(wi.getSSID().split("_")[1].replace("\"", ""))));
                                ShareResource.deviceSlaveThread.start();

                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Log.d("WifiScanner", "Don't have Wifi Connection");
                    }
                }
                break;
        }
    }
}
