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

    static final String ACTION_START_FOREGROUND = "ps.DistributorService.start";
    static final String ACTION_STOP_FOREGROUND = "ps.DistributorService.stop";
    private static final int MAX_DEVICES_COUNT = 9;
    static Object sync = new Object();
    static boolean isDistributorActive = false;
    static boolean isServerOn = false;
    static List<MasterDevice> deviceList = new LinkedList<>();
    private static String TAG = "DistributorService";
    private static int workerCount = 0;
    private static ServerSocket server;
    private static ExecutorService executor;

    /**
     * Increment worker device count
     */
    synchronized static void incrWorker() {
        workerCount++;
    }

    /**
     * decrement worker device count
     */
    synchronized static void dcrWorker() {
        workerCount--;
        if (workerCount == 0) {
            gatherResults();
        }
    }

    /**
     * Start distributing file among slave devices
     *
     * @param context Current context
     */
    static void startDistribution(final Context context) {

        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DataTransfer.initFiles(true, CompressFile.fileToCompress, CompressFile.fileToCompress + ".dcrz");

                    // Write Header
                    CompressionUtils.writeHeader(CompressFile.getAlgorithm(), CompressFile.fileToCompress);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "start distribution: distributing");
                isDistributorActive = true;
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.setWidgetEnabled(false);
                        NotificationUtils.updateNotification(context.getString(R.string.distributing));
                    }
                });
                for (int i = 0; i < deviceList.size(); i++) {
                    if (deviceList.get(i).getAllocatedSpace() == 0) {
                        break;
                    } else {
                        Log.d(TAG, i + ": distributing to " + deviceList.get(i).getName());
                        deviceList.get(i).notifyMe(this);
                    }
                }
                Log.d(TAG, "start distribution: compressing");
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationUtils.updateNotification(context.getString(R.string.compressing));
                    }
                });
            }

        })).start();
    }

    /**
     * Receive compressed output from all slave devices
     */
    synchronized static void gatherResults() {
        Log.d(TAG, "gather results");
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
                    if (device.getAllocatedSpace() == 0) {
                        break;
                    } else {
                        Log.d(TAG, ":gathering from " + device.getName());
                        device.notifyMe(this);
                    }
                }
                isDistributorActive = false;
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        })).start();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (intent.getAction().equals(ACTION_START_FOREGROUND)) {

            Intent nintent = new Intent(DistributorService.this, CompressFile.class);
            NotificationUtils.initNotification(DistributorService.this, nintent);
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    DistributorService.this.startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.notification);
                    try {
                        CompressFile.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                CompressFile.setWidgetEnabled(false);
                            }
                        });
                        synchronized (sync) {
                            while (CompressFile.fileToCompress == null) {
                                sync.wait();
                            }
                        }

                        executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);
                        server = new ServerSocket(0);
                        WifiOperations.setWifiApSsid(DistributorService.this.getString(R.string.sr_ssid) + "_" + server.getLocalPort());
                        WifiOperations.setWifiApEnabled(true);

                        CompressFile.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                CompressFile.setWidgetEnabled(true);
                            }
                        });
                        isServerOn = true;
                        while (true) {
                            Socket client = server.accept();
                            CompressFile.updateDeviceCount(DistributorService.this, true);
                            Log.d(TAG, "Connected " + client.getInetAddress());

                            MasterDevice masterDevice = new MasterDevice(DistributorService.this, client);
                            deviceList.add(masterDevice);
                            executor.execute(masterDevice);
                        }
                    } catch (Exception e) {
                        isServerOn = false;
                        e.printStackTrace();
                    }
                    stop();
                }
            })).start();
        } else if (intent.getAction().equals(ACTION_STOP_FOREGROUND)) {
            Log.d(TAG, "action stop foreground");
            stop();
        }

        return START_NOT_STICKY;
    }

    /**
     * Stop all threads including this
     */
    synchronized void stop() {
        if (DistributorService.this != null) {
            CompressFile.handler.post(new Runnable() {
                @Override
                public void run() {
                    CompressFile.setWidgetEnabled(true);
                    if (isDistributorActive) {
                        NotificationUtils.updateNotification(DistributorService.this.getString(R.string.err_device_failed));
                    } else {
                        NotificationUtils.updateNotification(DistributorService.this.getString(R.string.completed));
                    }
                }
            });

            if (isDistributorActive) {
                DataTransfer.releaseFiles(true);
            } else {
                DataTransfer.releaseFiles(false);
            }

            isDistributorActive = false;
            WifiOperations.stop();

            try {
                if (server != null && !server.isClosed()) {
                    server.close();
                }
                if (executor != null) {
                    executor.shutdownNow();
                    executor.awaitTermination(1, TimeUnit.SECONDS);
                }

                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.setWidgetEnabled(true);
                        CompressFile.tvTotalDevice.setText(getString(R.string.cf_total_devices, 0));
                    }
                });
                deviceList.clear();
                workerCount = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            DistributorService.this.stopForeground(false);
            DistributorService.this.stopSelf();
        }
    }

    /**
     * Un neccesarily required.
     *
     * @param intent
     * @return null
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
