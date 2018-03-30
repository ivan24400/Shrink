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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;


public class DistributorService extends Service {

    private static String TAG = "DistributorService";

    public static final String ACTION_START_FOREGROUND = "ps.DistributorService.start";
    public static final String ACTION_STOP_FOREGROUND = "ps.DistributorService.stop";

    public static Object sync = new Object();

    private static final int MAX_DEVICES_COUNT = 9;
    private static int workerCount = 0;
    private static ServerSocket server;
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    public static List<MasterDevice> deviceList = new LinkedList<>();

    public synchronized static void incrWorker(){
        workerCount++;
    }

    public synchronized static void dcrWorker(){
        workerCount--;
        if(workerCount == 0){
            gatherResults();
        }
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

                        CompressFile.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                CompressFile.setWidgetEnabled(false);
                            }
                        });
                        synchronized(sync) {
                            while (CompressFile.fileToCompress == null) {
                                sync.wait();
                            }
                        }
                        // Write Header
                        CompressionUtils.writeHeader(intent.getIntExtra(CompressionUtils.cmethod,0),CompressFile.fileToCompress);

                        DataTransfer.initFiles(true,CompressFile.fileToCompress, CompressFile.fileToCompress + ".dcrz");

                        server = new ServerSocket(0);
                        WifiOperations.setWifiApSsid(DistributorService.this.getString(R.string.sr_ssid) + "_" + server.getLocalPort());
                        WifiOperations.setWifiApEnabled(true);

                        CompressFile.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                CompressFile.setWidgetEnabled(true);
                            }
                        });

                        while (true) {
                            Socket client = server.accept();
                            CompressFile.updateDeviceCount(DistributorService.this,true);
                            Log.d(TAG, "Connected " + client.getInetAddress());

                            MasterDevice masterDevice = new MasterDevice(DistributorService.this,client);
                            deviceList.add(masterDevice);
                            executor.execute(masterDevice);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (RejectedExecutionException e){
                        e.printStackTrace();
                    }
                    CompressFile.handler.post(new Runnable() {
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
            Log.d(TAG,"action stop foreground");
            stop();
        }

        return START_NOT_STICKY;
    }

    public static void startDistribution(final Context context){

        (new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"start distribution: distributing");
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.setWidgetEnabled(false);
                        NotificationUtils.updateNotification(context.getString(R.string.distributing));
                    }
                });
                    for (int i = 0; i < deviceList.size() ; i++) {
                            if(deviceList.get(i).getAllocatedSpace() == 0) {
                                break;
                            }else {
                                deviceList.get(i).notifyMe(this);
                            }
                    }
                    Log.d(TAG,"start distribution: compressing");
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationUtils.updateNotification(context.getString(R.string.compressing));
                    }
                });
                    }

        })).start();
    }

    public synchronized static void gatherResults(){
        CompressFile.handler.post(new Runnable() {
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
        WifiOperations.stop();
        DataTransfer.releaseFiles();
        try {
            if(server != null && !server.isClosed()) {
                server.close();
            }
            if(executor != null){
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
            CompressFile.handler.post(new Runnable() {
                @Override
                public void run() {
                    CompressFile.setWidgetEnabled(true);
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
