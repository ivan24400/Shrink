package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

// Master threads
public class DeviceMaster implements Runnable {

    private static final String TAG = "DeviceMaster";
    private static CompressFile context;
    private long freeSpace;
    private long allocatedSpace;
    private char battery;
    private InputStream in;
    private OutputStream out;
    private Socket client;

    public DeviceMaster(CompressFile cf, Socket s) throws IOException {
        context = cf;
        this.client = s;
        this.freeSpace = 0;
    }

    private void retrieveMetaData() throws IOException {
        byte[] head = new byte[Distributor.HEADER_SIZE];
        in.read(head, 0, Distributor.HEADER_SIZE);
        Log.d(TAG,"read: "+ Arrays.toString(head));

        // Read Free Space
        int i = 0; // free space base
        int shift = 0;
        while (i < 8) { // free space length is 8 bytes
            Log.d(TAG,"i = "+i+" shift "+shift+" head: "+head[i]+" freespace "+freeSpace);
            freeSpace = (freeSpace << 8)| (long)(head[i++] & 0xFF); // Big Endian
        }

        // Read Battery
        battery = (char) head[i]; // battery is 1 byte

        Log.d(TAG, "free space " + Long.toString(freeSpace) + " , battery " + battery);
    }

    public void setAllocatedSpace(long as) {
        this.allocatedSpace = as;
    }

    public long getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void run() {
        try {
            in = client.getInputStream();
            out = client.getOutputStream();

            retrieveMetaData();
            this.allocatedSpace = this.freeSpace / 2;
            Log.d(TAG,"allocated "+allocatedSpace+"  freespace "+freeSpace);

            byte[] buffer = new byte[Distributor.HEADER_SIZE];

            int i = 0;
            int shift = 56;
            while (i < 8) {
                buffer[i++] = (byte) ((allocatedSpace >> shift) & 0xFF);
                Log.d(TAG," i "+i+" shift "+shift+" buffer "+buffer[i]);
                shift = shift - 8;
            }

            out.write(buffer, 0, 8);
            out.flush();
            Log.d(TAG,"sent: "+Arrays.toString(buffer));

            client.close();

        } catch (IOException e) {
            Log.d(TAG, "Unable to retrieve metadata");
            e.printStackTrace();

            try {
                client.close();
            } catch (IOException f) {
                f.printStackTrace();
            }
        }finally{
            Distributor.updateDeviceCount(false);
        }

    }
}
