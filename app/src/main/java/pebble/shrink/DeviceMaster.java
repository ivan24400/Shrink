package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

// Master threads
public class DeviceMaster implements Runnable{

    public static final int HEADER_SIZE = 10; // freeSpace = 8, battery = 1
    private static final String TAG ="DeviceMaster";
    private long freeSpace;
    private long allocatedSpace;
    private char battery;
    private InputStream in;
    private OutputStream out;
    private Socket client;

    public DeviceMaster(Socket s) throws IOException {
        this.client = s;
        in = s.getInputStream();
        out = s.getOutputStream();
    }

    public void retrieveMetaData() throws IOException{
        byte[] head = new byte[HEADER_SIZE];
        in.read(head,0,HEADER_SIZE);

        // Read Free Space
        int i = 0; // free space base
        int shift = 0;
        while (i < 9) { // free space length is 9 bytes
            freeSpace = (freeSpace << shift) | head[i++]; // Big Endian
            shift = shift + 8;
        }

        // Read Battery
        battery = (char) head[i]; // battery is 1 byte
    }

    public void setAllocatedSpace(long as){ this.allocatedSpace = as; }

    public long getFreeSpace() {
        return freeSpace;
    }

    public char getBattery() {
        return battery;
    }

    public InputStream getInputStream(){ return in; }

    public OutputStream getOutputStream(){ return out; }

    @Override
    public void run() {
       try{
           retrieveMetaData();
           this.allocatedSpace = this.freeSpace/2;

           ByteBuffer buffer = ByteBuffer.allocate(8);
           buffer.putLong(this.allocatedSpace);

           out.write(buffer.array());

           client.close();

       }catch (IOException e){
           Log.d(TAG,"Unable to retrieve metadata");
           e.printStackTrace();
          try {
              client.close();
          }catch (IOException f){
              f.printStackTrace();
          }
       }

    }
}
