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

    private boolean isLastChunk = true;
    private byte[] buffer = new byte[DataTransfer.BUFFER_SIZE];


    private InputStream in;
    private OutputStream out;
    private Socket client;

    public MasterDevice(Context c,Socket s) throws IOException {
        this.client = s;
        this.context = c;
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

    public void notifyMe(Object master){
        isFileAvailable = true;
        masterSync = master;
        sync.notify();
      try{
          while(isFileAvailable){
              masterSync.wait();
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

        // Sending meta data
        i = 0;
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
    }

    @Override
    public void run() {
        try {
            in = client.getInputStream();
            out = client.getOutputStream();

            initMetaData();

            if(in.read() != DataTransfer.READY){
                // READY
                throw new IOException("Invalid signal");
            }

            // Wait until previous devices send their data
            while(!isFileAvailable){
                try{
                    sync.wait();
                }catch (InterruptedException e){}
            }

            // Send File chunk
            DataTransfer.transferChunk(allocatedSpace,out);
            isFileAvailable = false;
            masterSync.notify();

            // Read Compressed Size
            in.read(buffer,0,8);
            int i = 0; // free space base
            while (i < 8) { // free space length is 8 bytes
                compressedSize = (compressedSize << 8)| (long)(buffer[i++] & 0xFF); // Big Endian
            }

            // Wait until previous devices send their data
            while(!isFileAvailable){
                try{
                    sync.wait();
                }catch (InterruptedException e){}
            }

            // Ready to receive data
            out.write(DataTransfer.READY);

            // Write to file
            DataTransfer.receiveChunk(compressedSize,in);

            // End of process
            client.close();

        } catch (IOException e) {
            Log.d(TAG, "Connection failed");
            e.printStackTrace();

            try {
                client.close();
            } catch (IOException f) {
                f.printStackTrace();
            }
        }finally{
            CompressFile.updateDeviceCount(context,false);
        }

    }
}
