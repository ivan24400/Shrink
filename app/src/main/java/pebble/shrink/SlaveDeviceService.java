package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SlaveDeviceService extends Service {

    private static final String TAG = "SlaveDeviceService";

    public static final String EXTRA_PORT = "ps.SlaveDeviceService.port";

    private static int hbPort;
    private static ScheduledExecutorService executer;
    private static ServerSocket slaveHeart;
    private static Socket slave, master;
    private static InputStream in;
    private static OutputStream out, hbOut;
    private static int algorithm;
    private static boolean isLastChunk = true;
    private static byte[] buffer = new byte[DataTransfer.BUFFER_SIZE];

    public static long freeSpace, allocatedSpace, compressedSize;
    public static char batteryClass;

    private static void initMetaData() throws IOException {

        // Sending freespace and battery
        int i = 0;
        int shift = 56;
        while (i < 8) {
            buffer[i] = (byte) ((freeSpace >> shift) & 0xFF);
            shift = shift - 8;
            i++;
        }
        buffer[i++] = ((byte) (batteryClass & 0xFF));

        // hb port
        shift = 24;
        while (shift >= 0) {
            buffer[i++] = (byte) ((hbPort >> shift) & 0xFF);
            shift = shift - 8;
        }

        out.write(buffer, 0, DataTransfer.HEADER_SIZE + 4);
        out.flush();
        Log.d(TAG, "freeSpace: " + freeSpace+" battery: "+batteryClass);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        (new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent1 = new Intent(SlaveDeviceService.this, ShareResource.class);
                            NotificationUtils.initNotification(SlaveDeviceService.this, intent1);
                        }
                    });

                    Runnable hb = new Runnable() {
                        @Override
                        public void run() {
                            if (slaveHeart != null && master != null) {
                                try {
                                    hbOut.write(DataTransfer.READY);
                                    hbOut.flush();
                                    Log.d(TAG, "master alive");
                                } catch (IOException e) {
                                    Log.d(TAG, "master dead");
                                    e.printStackTrace();
                                    stop();
                                }
                            }
                        }
                    };

                    InetAddress addr = InetAddress.getByName(SlaveDeviceService.this.getString(R.string.sr_server_ip));
                    int port = intent.getIntExtra(EXTRA_PORT, -1);

                    Log.d(TAG, "device slave " + addr + " @ " + port);

                    try {
                        slaveHeart = new ServerSocket(0);
                        hbPort = slaveHeart.getLocalPort();
                        Log.d(TAG, "hbPort " + hbPort);
                        slave = new Socket(addr, port);
                        in = slave.getInputStream();
                        out = slave.getOutputStream();

                        SlaveDeviceService.batteryClass = ShareResource.mpriority.getSelectedItemPosition() == 0 ? 'B' : 'A';
                        Log.d(TAG, "freespace: " + ShareResource.mfreeSpace.getText().toString().trim().replace("\"", ""));
                        SlaveDeviceService.freeSpace = Long.parseLong(ShareResource.mfreeSpace.getText().toString().trim().replace("\"", ""));

                        initMetaData();

                        master = slaveHeart.accept();
                        Log.d(TAG, "master hb connected " + master.toString());
                        hbOut = master.getOutputStream();

                        executer = Executors.newSingleThreadScheduledExecutor();
                        executer.scheduleAtFixedRate(hb, 0, DataTransfer.HEARTBEAT_TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Receiving allocated space and algorithm type
                    in.read(buffer, 0, DataTransfer.HEADER_SIZE);

                    int i = 0;
                    while (i < 8) {
                        allocatedSpace = (allocatedSpace << 8) | (long) (buffer[i++] & 0xFF);
                    }
                    byte meta = buffer[i];
                    if ((meta & CompressionUtils.MASK_LAST_CHUNK) == CompressionUtils.MASK_LAST_CHUNK) {
                        isLastChunk = true;
                    } else {
                        isLastChunk = false;
                    }
                    if ((meta & (byte) CompressionUtils.DCRZ) == (byte) (CompressionUtils.DCRZ)) {
                        algorithm = CompressionUtils.DCRZ;
                    } else {
                        algorithm = CompressionUtils.DEFLATE;
                    }
                    Log.d(TAG, "Allocated Space is " + Long.toString(allocatedSpace) + " algorithm is " + algorithm + " islast chunk " + isLastChunk);
                    if (allocatedSpace < 1) {
                        throw new IOException("Invalid allocatedSpace value");
                    }
                    DataTransfer.initFiles(false, ShareResource.tmpc_file, ShareResource.tmp_file);

                    Log.d(TAG, "receiving data");
                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(SlaveDeviceService.this.getString(R.string.receiving));
                        }
                    });

                    // Receive chunk from master device
                    DataTransfer.receiveChunk(allocatedSpace, in);

                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(SlaveDeviceService.this.getString(R.string.compressing));
                        }
                    });

                    Log.d(TAG, "compressing file");
                    // Compress chunk
                    if (CompressionUtils.compress(algorithm, isLastChunk, ShareResource.tmp_file) != 0) {
                        throw new IOException("Compression failed");
                    }

                    // Send compressed size to master device.
                    compressedSize = (new File(ShareResource.tmpc_file)).length();
                    i = 0;
                    int shift = 56;
                    while (i < 8) {
                        buffer[i] = (byte) ((compressedSize >> shift) & 0xFF);
                        shift = shift - 8;
                        i++;
                    }
                    out.write(buffer, 0, 8);
                    out.flush();

                    Log.d(TAG, "sending data");
                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(SlaveDeviceService.this.getString(R.string.sending));
                        }
                    });

                    in.read();
                    // Send compressed output to master
                    DataTransfer.transferChunk(compressedSize, out);

                    in.read(); //READY signal

                    // End of process
                    slave.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        slave.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                } finally {
                    stop();
                }

            }
        })).start();

        return START_NOT_STICKY;
    }

    /**
     * Stop all operations
     */
    private void stop() {
        WifiOperations.setWifiEnabled(false);
        ShareResource.handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationUtils.updateNotification(getString(R.string.completed));
            }
        });
        try {
            if (executer != null) {
                executer.shutdownNow();
                executer.awaitTermination(3, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DataTransfer.deleteFiles();
        SlaveDeviceService.this.stopForeground(false);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
