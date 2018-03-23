package pebble.shrink;

import android.app.Service;
import android.content.Context;
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
    private static int workerCount = 0;
    private static ServerSocket server;
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    public static List<MasterDevice> deviceList = new LinkedList<>();
    private static boolean isStopped = false;


    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized static void incrWorker(){
        workerCount++;
    }

    public synchronized static void dcrWorker(){
        workerCount--;
        if(workerCount == 0){
            gatherResults();
        }
    }

    public synchronized static int getWorkerCount(){
        return workerCount;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if(intent.getAction().equals(ACTION_START_FOREGROUND)) {

            Intent nintent = new Intent(DistributorService.this, CompressFile.class);
            NotificationUtils.startNotification(DistributorService.this, nintent);
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DistributorService.this.startForeground(NotificationUtils.NOTIFICATION_ID,NotificationUtils.notification);

                        // Write Header
                        CompressionUtils.writeHeader(intent.getIntExtra(CompressionUtils.cmethod,0),CompressFile.fileToCompress);

                        DataTransfer.initFiles(true, CompressFile.fileToCompress, CompressFile.fileToCompress + ".dcrz");

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
                    CompressFile.cfHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(DistributorService.this.getString(R.string.completed));
                        }
                    });
                    stop();
                    Log.d(TAG, "after executor shutdown");
                }
            })).start();
        }else if(intent.getAction().equals(ACTION_STOP_FOREGROUND)){
            stop();
        }

        return START_NOT_STICKY;
    }

    public static void startDistribution(final Context context){

        (new Thread(new Runnable() {
            @Override
            public void run() {
                CompressFile.cfHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.setEnabledWidget(false);
                        NotificationUtils.updateNotification(context.getString(R.string.distributing));
                    }
                });
                    for (int i = 1; i <= deviceList.size() ; i++) {
                            if(deviceList.get(i).getAllocatedSpace() == 0) {
                                break;
                            }else {
                                deviceList.get(i).notifyMe(this);
                            }
                    }
                CompressFile.cfHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationUtils.updateNotification(context.getString(R.string.compressing));
                    }
                });
                    }

        })).start();
    }

    public synchronized static void gatherResults(){
        CompressFile.cfHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationUtils.updateNotification(NotificationUtils.getContext().getString(R.string.gather));
            }
        });
        (new Thread(new Runnable() {
            @Override
            public void run() {
                for (MasterDevice device : deviceList) {
                    if(device.getAllocatedSpace() == 0){
                        break;
                    }else {
                        device.notifyMe(this);
                    }
                }
            }
        })).start();
    }

    public synchronized void stop() {
        this.isStopped = true;
        WifiOperations.stop();
        DataTransfer.releaseFiles();
        try {
            if(server != null) {
                server.close();
            }
            if(executor != null){
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
            CompressFile.cfHandler.post(new Runnable() {
                @Override
                public void run() {
                    CompressFile.setEnabledWidget(true);
                }
            });
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
