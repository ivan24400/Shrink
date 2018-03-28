package pebble.shrink;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

// Master threads
public class MasterDevice implements Runnable {

    private static final String TAG = "MasterDevice";

    private long freeSpace;
    private long allocatedSpace;
    private long compressedSize;
    private char battery;
    private int rank;

    private boolean isFileAvailable = false;
    private Context context;
    private Object sync,masterSync;

    private boolean isLastChunk = false;
    private byte[] buffer = new byte[DataTransfer.BUFFER_SIZE];


    private InputStream in;
    private OutputStream out;
    private Socket client;

    public MasterDevice(Context c,Socket s) throws IOException {
        this.client = s;
        this.context = c;
        sync = new Object();
    }

    public void setAllocatedSpace(long as) {
        allocatedSpace = as;
    }

    public long getAllocatedSpace(){ return allocatedSpace; }

    public long getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

    public int getRank(){ return rank; }

    public void setRank(int r){ rank = r; }

    public void setLastChunk(boolean state){ isLastChunk = state; }

    public synchronized void notifyMe(Object thread){
        Log.d(TAG,"notify Me");
        isFileAvailable = true;
        masterSync = thread;
        synchronized (sync){
            sync.notify();
        }

      try{
          synchronized (masterSync) {
              while (isFileAvailable) {
                  masterSync.wait();
              }
          }
      }catch (InterruptedException e){
          e.printStackTrace();
          Thread.currentThread().interrupt();
      }
    }

    private void initMetaData() throws IOException {

        // Receiving metadata
        in.read(buffer, 0, DataTransfer.HEADER_SIZE);
        Log.d(TAG,"read: "+ Arrays.toString(buffer));

        // Read Free Space
        int i = 0; // free space base
        while (i < 8) { // free space length is 8 bytes
            freeSpace = (freeSpace << 8)| (long)(buffer[i++] & 0xFF); // Big Endian
        }

        // Read Battery
        battery = (char) buffer[i]; // battery is 1 byte

        Log.d(TAG, "free space " + Long.toString(freeSpace) + " , battery " + battery);


    }

    @Override
    public void run() {
        try {
            in = client.getInputStream();
            out = client.getOutputStream();

            initMetaData();

            // Wait until previous devices send their data
            synchronized (sync){
            while(!isFileAvailable){
                try{
                    sync.wait();
                }catch (InterruptedException e){}
            }
            }

            // Sending meta data
           int i = 0;
            int shift = 56;
            while (i < 8) {
                buffer[i++] = (byte) ((allocatedSpace >> shift) & 0xFF);
                shift = shift - 8;
            }

            byte meta = 0;
            if(isLastChunk){
                meta = (byte)(meta | 0x80);
            }
            if(CompressFile.getAlgorithm() == CompressionUtils.DEFLATE){
                meta = (byte)(meta | 0x00);
            }else{
                meta = (byte)(meta | 0x01);
            }

            buffer[i] = meta;

            out.write(buffer, 0, DataTransfer.HEADER_SIZE);
            out.flush();
            Log.d(TAG,"sent: "+Arrays.toString(buffer));


            // Send File chunk
            DataTransfer.transferChunk(allocatedSpace,out);
            isFileAvailable = false;
            synchronized (masterSync){
                masterSync.notify();
            }
            Log.d(TAG,"send file chunk");

            // Read Compressed Size
            in.read(buffer,0,8); // sizeof long
            i = 0; // free space base
            while (i < 8) { // free space length is 8 bytes
                compressedSize = (compressedSize << 8)| (long)(buffer[i++] & 0xFF); // Big Endian
            }
            DistributorService.dcrWorker();
            Log.d(TAG,"read compressed size "+compressedSize);

            // Wait until previous devices send their data
            synchronized (sync) {
                while (!isFileAvailable) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            Log.d(TAG,"finished waiting");
            out.write(DataTransfer.READY);
            out.flush();

            // Write to file
            DataTransfer.receiveChunk(compressedSize,in);
            isFileAvailable = false;
            synchronized (masterSync){
                masterSync.notify();
            }
            out.write(DataTransfer.READY);
            out.flush();
            Log.d(TAG,"receive compressed file chunk");
            // End of process
            client.close();

        } catch (Exception e) {
            Log.d(TAG, "Connection failed");
            e.printStackTrace();

            try {
                client.close();
            } catch (IOException f) {
                f.printStackTrace();
            }
        }finally{
            CompressFile.handler.post(new Runnable() {
                @Override
                public void run() {
                    CompressFile.updateDeviceCount(context,false);
                }
            });
        }

    }
}
