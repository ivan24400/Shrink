package pebble.shrink;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by Ivan on 15-09-2017.
 */

public class DeviceGroupListener implements WifiP2pManager.GroupInfoListener, WifiP2pManager.ConnectionInfoListener {

    private Context context;
    private String TAG = "DeviceGroupListener";


    public DeviceGroupListener(Context c) {
        this.context = c;
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup == null) {
            Log.d(TAG, "wifiP2pgroup is null");
            return;
        }
        int deviceCount = 0;
        Log.d(TAG, Arrays.toString(wifiP2pGroup.getClientList().toArray()));
        for (WifiP2pDevice device : wifiP2pGroup.getClientList()) {
            deviceCount++;
        }

        CompressFile.totalDevice.setText(context.getResources().getString(R.string.cf_total_devices, deviceCount));
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d(TAG, wifiP2pInfo.toString());
    }
}
