package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by Ivan on 15-03-2018.
 */

public class DeviceSlave implements Runnable {

    private static final String TAG = "DeviceSlave";

    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;

    public static long freeSpace,allocateSpace;
    public static char batteryClass;

    public DeviceSlave(final InetAddress addr, final int port) throws IOException{
        this.socket = new Socket(addr,port);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    @Override
    public void run() {
        try{
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.putLong(freeSpace);
            buffer.put((byte)(batteryClass & 0xFF));

            out.write(buffer.array(),0,buffer.position());

            byte[] asBuffer = new byte[8];
            allocateSpace = in.read(asBuffer,0,8);

            Log.d(TAG,"Allocated Space is "+allocateSpace);

            socket.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
