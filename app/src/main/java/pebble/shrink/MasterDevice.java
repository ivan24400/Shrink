package pebble.shrink;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MasterDevice implements Runnable {

    private static final String TAG = "MasterDevice";
    private String threadName;
    private long freeSpace;
    private long allocatedSpace;
    private long compressedSize;
    private char battery;
    private int hbPort;
    private boolean ishbClose = false;

    private boolean isFileAvailable = false;
    private boolean isOperationOn = false;
    private Context context;
    private Object sync, masterSync;

    private boolean isLastChunk = false;
    private byte[] buffer = new byte[DataTransfer.BUFFER_SIZE];

    private InputStream in;
    private OutputStream out;
    private Socket master,slave;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public MasterDevice(Context c, Socket s) throws IOException {
        this.master = s;
        this.context = c;
        sync = new Object();
    }

    public void setAllocatedSpace(long as) {
        allocatedSpace = as;
    }

    public long getAllocatedSpace() {
        return allocatedSpace;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

    public String getName() {
        return threadName;
    }


    public void setLastChunk(boolean state) {
        isLastChunk = state;
    }

    /**
     * Tells this thread to continue processing
     * @param thread Parent thread
     */
    public synchronized void notifyMe(Object thread) {
        Log.d(TAG, threadName+": notify Me");
        isFileAvailable = true;
        masterSync = thread;
        synchronized (sync) {
            sync.notify();
        }

        try {
            synchronized (masterSync) {
                while (isFileAvailable) {
                    masterSync.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Receive slave device info and start heartbeat
     * @throws IOException
     */
    private void initMetaData() throws IOException {

        // Receiving metadata
        in.read(buffer, 0, DataTransfer.HEADER_SIZE + 4);

        // Read Free Space
        int i = 0; // free space base
        while (i < 8) { // free space length is 8 bytes
            freeSpace = (freeSpace << 8) | (long) (buffer[i++] & 0xFF); // Big Endian
        }

        // Read Battery
        battery = (char) buffer[i++]; // battery is 1 byte

        int j = 0;
        while(j < 4){
            hbPort = (hbPort << 8) | (int) (buffer[i++] & 0xFF);
            j++;
        }

        Log.d(TAG, "free space " + Long.toString(freeSpace) + " , battery " + battery+ ", port "+hbPort);
        if(freeSpace <= 0 || battery >= 'C' || hbPort <= 0){
            throw new IOException("Invalid values");
        }
        slave = new Socket(master.getInetAddress(),hbPort);
        slave.setSoTimeout(DataTransfer.HEARTBEAT_TIMEOUT);

        Runnable hb = new Runnable() {
            @Override
            public void run() {
               try {
                    if(slave != null) {
                        Log.d(TAG,threadName+" slave alive "+slave.isConnected());

                        if(slave.getInputStream().read() < 0){
                            throw new IOException("slave died");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                   DistributorService.deviceList.remove(MasterDevice.this);
                    try {
                        if(master != null) {
                            master.close();
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    ishbClose = true;
                    CompressFile.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            CompressFile.updateDeviceCount(context, false);
                        }
                    });
                    executor.shutdown();
                }
            }
        };
        executor.scheduleAtFixedRate(hb,0,DataTransfer.HEARTBEAT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends and receive data from corresponding slave device.
     */
    @Override
    public void run() {
        try {
            in = master.getInputStream();
            out = master.getOutputStream();
            threadName = Thread.currentThread().getName();
            initMetaData();

            // Wait until previous devices send their data
            synchronized (sync) {
                while (!isFileAvailable) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            isOperationOn = true;
            // Sending meta data
            int i = 0;
            int shift = 56;
            while (i < 8) {
                buffer[i++] = (byte) ((allocatedSpace >> shift) & 0xFF);
                shift = shift - 8;
            }

            byte meta = 0;
            if (isLastChunk) {
                meta = (byte) (meta | 0x80);
            }
            if (CompressFile.getAlgorithm() == CompressionUtils.DEFLATE) {
                meta = (byte) (meta | 0x00);
            } else {
                meta = (byte) (meta | 0x01);
            }

            buffer[i] = meta;

            out.write(buffer, 0, DataTransfer.HEADER_SIZE);
            out.flush();
            Log.d(TAG, threadName+" allocatedSpace: " +allocatedSpace);


            // Send File chunk
            DataTransfer.transferChunk(allocatedSpace, out);
            isFileAvailable = false;
            synchronized (masterSync) {
                masterSync.notify();
            }
            Log.d(TAG, threadName+"send file chunk");

            // Read Compressed Size
            in.read(buffer, 0, 8); // sizeof long
            i = 0; // free space base
            while (i < 8) { // free space length is 8 bytes
                compressedSize = (compressedSize << 8) | (long) (buffer[i++] & 0xFF); // Big Endian
            }
            DistributorService.dcrWorker();

            // Wait until previous devices send their data
            synchronized (sync) {
                while (!isFileAvailable) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            Log.d(TAG,threadName+": receiving compressed chunk: "+compressedSize);
            out.write(DataTransfer.READY);
            out.flush();

            // Write to file
            DataTransfer.receiveChunk(compressedSize, in);
            isFileAvailable = false;
            synchronized (masterSync) {
                masterSync.notify();
            }
            out.write(DataTransfer.READY);
            out.flush();

            // End of process
            master.close();
            slave.close();
            slave = null;

        } catch (Exception e) {
            Log.d(TAG, "Connection failed");
            e.printStackTrace();
            try {
                master.close();
                slave.close();
                slave = null;
            } catch (IOException f) {
                f.printStackTrace();
            }

            if(isOperationOn && DistributorService.isDistributorActive){
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context,context.getString(R.string.err_device_failed),Toast.LENGTH_SHORT).show();
                    }
                });
                Intent tintent = new Intent(context,DistributorService.class);
                tintent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
                context.startService(tintent);
            }
        } finally {
            Log.d(TAG,threadName+" done");
            try {
                executor.shutdownNow();
                executor.awaitTermination(3,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!ishbClose) {
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.updateDeviceCount(context, false);
                    }
                });
            }

        }
    }
    @Override
    public String toString(){
        return threadName;
    }
}
