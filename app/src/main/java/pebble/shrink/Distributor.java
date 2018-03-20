package pebble.shrink;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ivan on 21-09-2017.
 */

public class Distributor implements Runnable {

    private static String TAG = "Distributor";

    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1
    private static final int MAX_DEVICES_COUNT = 9;
    private static int deviceCount = 0;

    private static ServerSocket server;
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);
    private static CompressFile context;
    private static boolean isStopped = false;

    public Distributor(CompressFile c) {
        context = c;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(0);
            WifiOperations.setWifiApSsid(context.getString(R.string.sr_ssid) + "_" + server.getLocalPort());
            WifiOperations.setWifiApEnabled(true);

            while (!isStopped()) {
                Socket client = server.accept();
                updateDeviceCount(true);
                Log.d(TAG, "Connected " + client.getInetAddress());
                if (isStopped()) {
                    Log.d(TAG, "Server is stopped");
                    return;
                }
                executor.execute(new DeviceMaster(context,client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.executor.shutdown();
        Log.d(TAG,"after executor shutdown");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void updateDeviceCount(final boolean isIncrement){
        if(isIncrement){
            deviceCount++;
        }else{
            deviceCount--;
        }
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                context.tvTotalDevice.setText(context.getString(R.string.cf_total_devices,deviceCount));
            }
        });
    }
}
