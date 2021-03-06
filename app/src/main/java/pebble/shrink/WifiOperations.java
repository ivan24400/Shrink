package pebble.shrink;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;


class WifiOperations {

    private static final String TAG = "WifiOperations";

    static boolean isMaster = false;
    private static Activity activity;
    private static WifiManager manager;
    private static WifiConfiguration configuration;
    private static Method setWifiApEnabled, getWifiApState;

    static {
        try {
            setWifiApEnabled = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            getWifiApState = WifiManager.class.getMethod("getWifiApState");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static void initWifiOperations(Activity c) {
        activity = c;
        manager = (WifiManager) activity.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }


    static void setWifiSsid(String ssid) {
        Log.d(TAG, "set wifi ssid " + ssid);
        configuration = new WifiConfiguration();
        configuration.SSID = "\"" + ssid + "\"";
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    }

    static boolean isWifiApOn() {
        try {
            if (WifiManager.WIFI_STATE_ENABLED == ((int) getWifiApState.invoke(manager) % 10)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static void setWifiApSsid(String ssid) {
        Log.d(TAG, "set wifi ssid: " + ssid);
        if (manager == null) {
            manager = (WifiManager) activity.getBaseContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        try {
            configuration = new WifiConfiguration();
            configuration.SSID = ssid;
            configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void setWifiApEnabled(boolean state) {
        isMaster = true;
        if (manager == null) {
            manager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        try {
            if ((int) getWifiApState.invoke(manager) % 10 == WifiManager.WIFI_STATE_DISABLED && !state) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, R.string.err_os_not_supported, Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (setWifiApEnabled == null || configuration == null && state) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, R.string.err_invalid_config, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                boolean ret = (Boolean) setWifiApEnabled.invoke(manager, configuration, state);
                if (!ret) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, R.string.err_wifiap_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static WifiManager getWifiManager() {
        return manager;
    }

    static void setWifiEnabled(final boolean state) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "setwifienabled " + state);
                if (manager != null) {
                    if (!state) {
                        if (manager.isWifiEnabled()) {
                            if (configuration != null) {
                                manager.removeNetwork(configuration.networkId);
                            }
                            if (!manager.setWifiEnabled(false)) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, R.string.err_wifi_failed, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    } else {
                        isMaster = false;
                        if (!manager.isWifiEnabled()) {
                            if (!manager.setWifiEnabled(true)) {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, R.string.err_wifi_failed, Toast.LENGTH_SHORT).show();
                                    }
                                });
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
            }
        })).start();
    }

    static void startScan() {
        if (manager != null) {
            if (!manager.isWifiEnabled()) {
                manager.setWifiEnabled(true);
            }
            manager.startScan();
        } else {
            Log.d(TAG, "startscan manager is null");
        }
    }

    /**
     * @return is connected to master device
     */
    static boolean isConnected() {
        return manager != null && manager.getConnectionInfo().getSSID().contains("SHRINK");
    }

    /**
     * Shutdown wifi or wifiAP
     */
    static void stop() {
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
