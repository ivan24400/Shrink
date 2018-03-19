package pebble.shrink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;


public class WifiOperations {

    private static final String TAG = "WifiOperations";

    private static Context context;
    private static WifiManager manager;
    private static WifiConfiguration configuration;

    private static final String passwd = "#1a2b3c4d";

    private static Method setWifiApEnabled;
    private static boolean isMaster = false;


    public static void initWifiOperations(Context c) {
        context = c;
        manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }


    public static void setWifiSsid(String ssid) {
        Log.d(TAG, "setwifissis " + ssid);
        configuration = new WifiConfiguration();
        configuration.SSID = "\"" + ssid + "\"";
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
       /* configuration.priority = 240000;
        configuration.hiddenSSID = false;
        configuration.preSharedKey = passwd;
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

        */
    }

    public static void setWifiApSsid(String ssid) {
        try {
            setWifiApEnabled = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "setwifissis " + ssid);
        configuration = new WifiConfiguration();
        configuration.SSID = "\"" + ssid + "\"";
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
       /* configuration.priority = 240000;
        configuration.hiddenSSID = false;
        configuration.preSharedKey = passwd;
        configuration.status = WifiConfiguration.Status.ENABLED;
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

        */
    }

    public static boolean setWifiApEnabled(boolean state) {
        isMaster = true;
        if (manager == null) {
            manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                boolean ret = (Boolean) setWifiApEnabled.invoke(manager, configuration, state);
                Log.d(TAG, "ip address " + DeviceOperations.getMyIpAddress());
                return ret;
            } else {
                Toast.makeText(context, R.string.err_os_not_supported, Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static WifiManager getWifiManager() {
        return manager;
    }

    public static void setWifiEnabled(final boolean state) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                isMaster = false;
                if (!state) {
                    if (manager.isWifiEnabled()) {
                        manager.setWifiEnabled(false);
                    }
                } else {

                    if (!manager.isWifiEnabled()) {
                        manager.setWifiEnabled(true);
                    }

                    for (WifiConfiguration config : manager.getConfiguredNetworks()) {
                        if(config.SSID.contains(context.getString(R.string.app_title))){
                            manager.removeNetwork(config.networkId);
                        }else{
                            manager.disableNetwork(config.networkId);
                        }
                    }

                    int res = manager.addNetwork(configuration);
                    Log.d(TAG, "network add result: " + res + " nid " + configuration.networkId);

                    manager.disconnect();
                    boolean bres = manager.enableNetwork(res, true);
                    Log.d(TAG, "network enable " + bres);
                    bres = manager.reconnect();
                    Log.d(TAG, "network reconnect " + bres);

                }
            }
        })).start();

    }

    public static void startScan() {
        if (manager != null) {
            if (!manager.isWifiEnabled()) {
                manager.setWifiEnabled(true);
            }
            manager.startScan();
        } else {
            Log.d(TAG, "startscan manager is null");
        }
    }

    public static boolean isConnected() {
        if (manager != null) {
            if (manager.getConnectionInfo().getSSID().contains(context.getString(R.string.sr_ssid))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void refreshDeviceCount(final Context context) {
        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final AtomicInteger totalDevices = new AtomicInteger(0);

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(500);

                                if (isReachable) {
                                    totalDevices.incrementAndGet();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(this.getClass().toString(), e.toString());
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }

                // Get a handler that can be used to post to the main thread
                CompressFile.cfHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        CompressFile.tvTotalDevice.setText(context.getString(R.string.cf_total_devices, totalDevices.get()));
                    }
                });

            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public static void stop() {
        Log.d(TAG, "stop " + isMaster);
        if (manager != null) {
            if (isMaster) {
                setWifiApEnabled(false);
            } else {
                manager.disconnect();
            }
            manager = null;
        }
    }

    public static String getDeviceStatus(NetworkInfo ni) {
        switch (ni.getState()) {
            case CONNECTED:
                return "Connected";
            case CONNECTING:
                return "Connecting";
            case DISCONNECTED:
                return "Disconnected";
            case DISCONNECTING:
                return "Disconnecting";
            case SUSPENDED:
                return "Suspended";
        }
        return "Unknown";
    }
}
