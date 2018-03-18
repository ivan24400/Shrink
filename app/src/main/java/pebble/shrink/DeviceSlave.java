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

    private static InetAddress addr;
    private static int port;
    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;

    public static long freeSpace,allocateSpace;
    public static char batteryClass;

    public DeviceSlave(final InetAddress a, final int p){
        this.addr = a;
        this.port = p;
    }

    @Override
    public void run() {
        try{
            this.socket = new Socket(addr,port);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();

            byte[] buffer = new byte[10];
            int i=0;
            int shift = 28;
            while(i<8){
                buffer[i++] = (byte)((freeSpace >> shift) & 0xFF);
                shift = shift - 8;
            }
            buffer[i] = ((byte)(batteryClass & 0xFF));

            out.write(buffer,0,Distributor.HEADER_SIZE);

            in.read(buffer,0,8);

            i=0;
            shift = 0;
            while(i < 8){
                allocateSpace = (allocateSpace << shift) |  buffer[i++];
                shift = shift + 8;
            }

            Log.d(TAG,"Allocated Space is "+allocateSpace);

            socket.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
