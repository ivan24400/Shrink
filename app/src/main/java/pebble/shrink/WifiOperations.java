package pebble.shrink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
        configuration.SSID = "\""+ssid+"\"";
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
      configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

    }

    public static void setWifiApSsid(String ssid) {
        Log.d(TAG, "setwifissis " + ssid);
        configuration = new WifiConfiguration();
        configuration.SSID = "\"" + ssid + "\"";
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
    }

    public static boolean setWifiApEnabled(boolean state) {
        isMaster = true;
        if (manager == null) {
            manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                setWifiApEnabled = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                return (Boolean) setWifiApEnabled.invoke(manager, configuration, state);
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
                    Log.d(TAG, "setwifienabled " + state);
                    if(manager != null) {
                        if (!state) {
                            if (manager.isWifiEnabled()) {
                                manager.setWifiEnabled(false);
                            }
                        } else {

                            if (!manager.isWifiEnabled()) {
                                manager.setWifiEnabled(true);
                            }

                            for (WifiConfiguration config : manager.getConfiguredNetworks()) {
                                if (config.SSID.contains(context.getString(R.string.app_name))) {
                                    manager.removeNetwork(config.networkId);
                                } else {
                                    manager.disableNetwork(config.networkId);
                                }
                            }
                            manager.saveConfiguration();
                            int res = manager.addNetwork(configuration);
                            Log.d(TAG, "network add result: " + res + " nid " + configuration.networkId);

                            manager.disconnect();
                            boolean bres = manager.enableNetwork(res, true);
                            Log.d(TAG, "network enable " + bres);
                            bres = manager.reconnect();
                            Log.d(TAG, "network reconnect " + bres);

                        }
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

    public static void stop() {
        Log.d(TAG, "stop " + isMaster);
        if (manager != null) {
            if (isMaster) {
                setWifiApEnabled(false);
            } else {
                setWifiEnabled(false);
            }
            manager = null;
        }
    }
}
