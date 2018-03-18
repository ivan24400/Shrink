package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

/**
 * Created by Ivan on 18-03-2018.
 */

public class WifiScanner extends BroadcastReceiver{

    private Context context;

    public WifiScanner(Context c){
        this.context = c;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        switch(action) {
            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:

                List<ScanResult> result = WifiOperations.getWifiManager().getScanResults();
                for (ScanResult scan : result) {
                    Log.d(getClass().getSimpleName(), "ssid found: " + scan.toString());
                    if (scan.SSID.contains(context.getString(R.string.sr_ssid))) {
                        WifiOperations.setWifiSsid(scan.SSID);
                        WifiOperations.setWifiEnabled(true);
                    }
                }
                break;

            case ConnectivityManager.CONNECTIVITY_ACTION:

                ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo netInfo = conMan.getActiveNetworkInfo();
                if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.d("WifiReceiver", "Have Wifi Connection "+netInfo.toString());
                } else {
                    Log.d("WifiReceiver", "Don't have Wifi Connection");
                }
        }
    }
}
