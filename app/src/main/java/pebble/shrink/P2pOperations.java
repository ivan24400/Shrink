package pebble.shrink;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

public class P2pOperations {

    public static String DEVICE_NAME = null;
    public static boolean isP2pOn = false;

    public static IntentFilter intentFilter = new IntentFilter();

    public static WifiP2pManager.Channel nChannel;
    public static WifiP2pManager nManager;
    public static WifiManager wManager = null;

    public static DeviceBroadcastReceiver dbReceiver;

    public static Distributor distributor;

    public static ProgressDialog progress;
    private static String TAG = "P2pOperations";
    private static Context context;

    public static File inputFile,outputFile;

    public static void initNetwork(Context c) {

        context = c;
        startWifi();

        progress = new ProgressDialog(c);
        progress.setCancelable(false);
        progress.setIndeterminate(true);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        nManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        nChannel = nManager.initialize(context, context.getMainLooper(), null);

    }

    public static void initiateDiscovery() {
        displayProgress(context, "Press Back to cancel", "Finding peers ...");
        nManager.discoverPeers(nChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovery Success");
            }

            @Override
            public void onFailure(int i) {
                Log.d(TAG, "Discovery failure " + errToString(i));
            }
        });
    }

    public static boolean connect(WifiP2pDevice device,int goIntent) {
        P2pOperations.displayProgress(context, "Press back to cancel", "Connecting to: " + device.deviceName);

        WifiP2pConfig config = new WifiP2pConfig();
        config.groupOwnerIntent = goIntent;
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        nManager.connect(nChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isP2pOn = true;
            }

            @Override
            public void onFailure(int reason) {
                isP2pOn = false;
                Toast.makeText(context, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        return isP2pOn;
    }

    public static void createGroup() {
        if (isP2pOn) {
            removeGroup();
        }


            DEVICE_NAME = Settings.Secure.getString(context.getContentResolver(),"bluetooth_name");
            Log.d(TAG,"DEVICE_NAME "+DEVICE_NAME);

            setDeviceName("SHRINK_GO_" + Integer.toString(Distributor.getServerPort()));


            Log.d(TAG, "local port \t" + Integer.toString(Distributor.server.getLocalPort()));

        P2pOperations.nManager.createGroup(P2pOperations.nChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isP2pOn = true;
                Log.d(TAG, "Create group success");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Create group failed");
                isP2pOn = false;
            }
        });

        distributor = new Distributor();
        (new Thread(distributor)).start();
    }

    public static void removeGroup() {
        // For Master and Slave DeviceMaster
        if (isP2pOn) {
            distributor.stop();
            nManager.removeGroup(nChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "remove group success");
                    setDeviceName(DEVICE_NAME);
                    stopWifi();
                    isP2pOn = false;
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "remove group fail " + errToString(i));
                }
            });
        }
    }

    public static void displayProgress(Context c, String title, String msg) {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        progress = progress.show(c, title, msg, true,
                true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    public static String getDeviceStatus(int deviceStatus) {
        Log.d(TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    public static String getMyIpAddress() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getDisplayName().matches(".*p2p.*")) {
                    for (Enumeration<InetAddress> inet = intf.getInetAddresses(); inet.hasMoreElements(); ) {
                        InetAddress inetAddress = inet.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            String addr = inetAddress.getHostAddress();
                            if (addr.indexOf(':') < 0)
                                return addr;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "NA";
    }

    public static void setDeviceName(String name) {
        if(name != null) {
            try {
                Class[] paramTypes = new Class[3];
                paramTypes[0] = WifiP2pManager.Channel.class;
                paramTypes[1] = String.class;
                paramTypes[2] = WifiP2pManager.ActionListener.class;
                Method setDeviceName = P2pOperations.nManager.getClass().getMethod("setDeviceName", paramTypes);
                setDeviceName.setAccessible(true);

                Object[] args = new Object[3];
                args[0] = nChannel;
                args[1] = name;
                args[2] = new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "set device name success");
                    }

                    @Override
                    public void onFailure(int i) {
                        Log.d(TAG, "set device name failure " + i);
                    }
                };
                setDeviceName.invoke(nManager, args);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startWifi() {
        wManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wManager.isWifiEnabled()) {
            wManager.setWifiEnabled(true);
        }
    }

    public static void stopWifi(){
        if (wManager != null && wManager.isWifiEnabled()) {
            wManager.setWifiEnabled(false);
        }
    }

    public static String errToString(int error) {
        switch (error) {
            case WifiP2pManager.BUSY:
                return "BUSY";
            case WifiP2pManager.ERROR:
                return "ERROR";
            case WifiP2pManager.P2P_UNSUPPORTED:
                return "P2p Not supported !";
        }
        return "ERROR";
    }

    // deviceName, String ip, String cpu, String freeSpace, String battery

    public static String getDeviceInfo() {

        // Free space in Bytes

        long freeSpace = 0;
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            freeSpace = statFs.getBlockSizeLong() * statFs.getAvailableBlocks();
        }else {
            freeSpace = (long)statFs.getBlockSize() * (long)statFs.getAvailableBlocks();
        }
            //  Battery
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            float batteryPercent = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) / batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            char batteryClass;
            if(batteryPercent > ShareResource.batteryAlowerLimit){
                batteryClass = 'A';
            }else if(batteryPercent > ShareResource.batteryBlowerLimit && batteryPercent < ShareResource.batteryAlowerLimit){
                batteryClass = 'B';
            }else {
                batteryClass = 'C';
            }

            Log.d(TAG,"Battery: "+batteryPercent+", Battery Class "+batteryClass+", FreeSpace: "+freeSpace);
            return Long.toString(freeSpace) + "::" + Character.toString(batteryClass);
    }
}
