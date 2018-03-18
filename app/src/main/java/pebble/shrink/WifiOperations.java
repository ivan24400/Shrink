package pebble.shrink;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Ivan on 18-03-2018.
 */

public class WifiOperations {

    private Context context;
    private static WifiManager manager;
    private static WifiConfiguration configuration;

    private static final String passwd = "#1a2b3c4d";

    private static Method getWifiApState;
    private static Method isWifiApEnabled;
    private static Method setWifiApEnabled;
    private static Method getWifiApConfiguration;

    private static InetAddress addr;
    private static int port;

    static {
        // lookup methods and fields not defined publicly in the SDK.
        Class<?> cls = WifiManager.class;
        for (Method method : cls.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.equals("getWifiApState")) {
                getWifiApState = method;
            } else if (methodName.equals("isWifiApEnabled")) {
                isWifiApEnabled = method;
            } else if (methodName.equals("setWifiApEnabled")) {
                setWifiApEnabled = method;
            } else if (methodName.equals("getWifiApConfiguration")) {
                getWifiApConfiguration = method;
            }
        }
    }

    public WifiOperations(Context c){
        this.context = c;
        this.manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
    }

    public static void setWifiSsid(String ssid){
        configuration = new WifiConfiguration();
        configuration.SSID = "\"".concat(ssid).concat("\"");
        configuration.hiddenSSID = true;
        configuration.preSharedKey = passwd;
        configuration.priority = 40;
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
        configuration.status = WifiConfiguration.Status.ENABLED;
    }

    public boolean setWifiApEnabled(boolean state){
      try {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
             return (boolean) setWifiApEnabled.invoke(manager, configuration, state);
          } else {
              Toast.makeText(context,R.string.err_os_not_supported,Toast.LENGTH_LONG).show();
              return false;
          }
      }catch (Exception e){
          e.printStackTrace();
          return false;
      }
    }

public static WifiManager getWifiManager(){
    return manager;
}
    public static void setWifiEnabled(boolean state) {
        if (!state) {
            if(manager.isWifiEnabled()){
                manager.setWifiEnabled(false);
            }
        }else{
            if(!manager.isWifiEnabled()){
                manager.setWifiEnabled(true);
            }
            manager.disconnect();
            manager.enableNetwork(configuration.networkId,true);
            manager.reconnect();
        }
    }

    public static void refreshDeviceCount(final Context context){
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
                CompressFile.cfHandler.post(new Runnable(){

                    @Override
                    public void run(){
                        CompressFile.tvTotalDevice.setText(context.getString(R.string.cf_total_devices,totalDevices.get()));
                    }
                });

            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

}
