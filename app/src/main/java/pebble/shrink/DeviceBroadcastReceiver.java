package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class DeviceBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = "DeviceBroadcastReceiver";

    private CompressFile compressFile;
    private ShareResource shareResource;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;

    public DeviceBroadcastReceiver(CompressFile group, WifiP2pManager.Channel c, WifiP2pManager m) {
        this.compressFile = group;
        this.channel = c;
        this.manager = m;
    }

    public DeviceBroadcastReceiver(ShareResource sr, WifiP2pManager.Channel c, WifiP2pManager m) {
        this.shareResource = sr;
        this.channel = c;
        this.manager = m;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                Log.d(TAG, context.getClass().getSimpleName() + " Wifi p2p CONNECTION_CHANGED");

                if (manager == null) {
                    return;
                }
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    manager.requestConnectionInfo(channel, shareResource);
                } else if (shareResource != null) {
                    shareResource.resetData();
                } else {
                    compressFile.resetData();
                }
                break;

            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                Log.d(TAG, context.getClass().getSimpleName() + " Wifi p2p peers changed");
                if (manager != null) {
                    manager.requestPeers(channel, shareResource);
                }
                break;

            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                Log.d(TAG, context.getClass().getSimpleName() + " Wifi p2p state changed");

                if ((intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                } else {
                    if (shareResource != null) {
                        shareResource.resetData();
                    } else {
                        compressFile.resetData();
                    }
                }
                break;

            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                Log.d(TAG, context.getClass().getSimpleName() + " WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                if (shareResource != null) {
                    shareResource.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                } else {
                    compressFile.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                }
                break;
        }

    }
}
