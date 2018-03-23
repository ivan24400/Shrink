package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class DistributorService extends Service {

    private static String TAG = "DistributorService";

    public static final String ACTION_START_FOREGROUND = "ps.DistributorService.start";
    public static final String ACTION_STOP_FOREGROUND = "ps.DistributorService.stop";

    private static final int MAX_DEVICES_COUNT = 9;

    private static ServerSocket server;
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    public static List<MasterDevice> deviceList = new LinkedList<>();
    private static boolean isStopped = false;


    private synchronized boolean isStopped() {
        return this.isStopped;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getAction().equals(ACTION_START_FOREGROUND)) {

            Intent nintent = new Intent(DistributorService.this, CompressFile.class);
            NotificationUtils.startNotification(DistributorService.this, nintent);
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DistributorService.this.startForeground(NotificationUtils.NOTIFICATION_ID,NotificationUtils.notification);

                        //t-> writeheader
                        DataTransfer.initFile(CompressFile.fileToCompress, CompressFile.fileToCompress + ".dcrz");

                        server = new ServerSocket(0);
                        WifiOperations.setWifiApSsid(DistributorService.this.getString(R.string.sr_ssid) + "_" + server.getLocalPort());
                        WifiOperations.setWifiApEnabled(true);

                        while (!isStopped()) {
                            Socket client = server.accept();
                            CompressFile.updateDeviceCount(DistributorService.this,true);
                            Log.d(TAG, "Connected " + client.getInetAddress());

                            if (isStopped()) {
                                Log.d(TAG, "Server is stopped");
                                return;
                            }

                            MasterDevice masterDevice = new MasterDevice(DistributorService.this,client);
                            deviceList.add(masterDevice);
                            executor.execute(masterDevice);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    stop();
                    Log.d(TAG, "after executor shutdown");
                }
            })).start();
        }else if(intent.getAction().equals(ACTION_STOP_FOREGROUND)){
            stop();
        }

        return START_NOT_STICKY;
    }

    public static void startDistribution(){
        (new Thread(new Runnable() {
            @Override
            public void run() {

                    boolean areMoreDevices = true;
                    for (int i = 1; i <= deviceList.size() && areMoreDevices; i++) {
                        for (MasterDevice device : deviceList) {
                            if(device.getAllocatedSpace() == 0) {
                                areMoreDevices = false;
                                break;
                            }
                            if (device.getRank() == i ) {
                                    device.notifyMe(this);
                                    break;
                                }
                            }
                        }
                    }

        })).start();
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
            stopForeground(false);
            stopSelf();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
