package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Distributor implements Runnable {

    private static String TAG = "Distributor";

    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1 && allocatedSpace = 8, algorithm = 1
    private static final int MAX_DEVICES_COUNT = 9;
    private static int deviceCount = 0;

    private static ServerSocket server;
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    public static List<MasterDevice> deviceList = new LinkedList<>();
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
                MasterDevice masterDevice = new MasterDevice(client);
                deviceList.add(masterDevice);
                executor.execute(masterDevice);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"after executor shutdown");
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        WifiOperations.stop();
        try {
            if(server != null) {
            server.close();
        }
        if(executor != null){
            executor.shutdownNow();
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void updateDeviceCount(final boolean isIncrement){
        if(isIncrement){
            deviceCount++;
        }else{
            deviceCount--;
        }

        CompressFile.cfHandler.post(new Runnable() {
            @Override
            public void run() {
                context.tvTotalDevice.setText(context.getString(R.string.cf_total_devices,deviceCount));

            }
        });

    }
}
