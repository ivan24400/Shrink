package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Ivan on 15-03-2018.
 */

public class DeviceSlave implements Runnable {

    private static final String TAG = "DeviceSlave";

    private static ShareResource context;
    private static InetAddress addr;
    private static int port;
    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;

    public static long freeSpace, allocateSpace;
    public static char batteryClass;

    public DeviceSlave(ShareResource c,final InetAddress a, final int p) {
        context = c;
        this.addr = a;
        this.port = p;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG,"device slave socket "+addr+" @ "+port);
            this.socket = new Socket(addr, port);
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();

            byte[] buffer = new byte[Distributor.HEADER_SIZE];
            int i = 0;
            int shift = 56;
            while (i < 8) {
              buffer[i] = (byte) ((freeSpace >> shift )& 0xFF);
                Log.d(TAG," i "+i+" shift "+shift+" buffer "+buffer[i]);
                shift = shift - 8;
                i++;
            }
            buffer[i] = ((byte) (batteryClass & 0xFF));

            out.write(buffer, 0, Distributor.HEADER_SIZE);
            out.flush();
            Log.d(TAG,"sent: "+Arrays.toString(buffer));

            in.read(buffer, 0, 8);
            Log.d(TAG,"read: "+ Arrays.toString(buffer));

            i = 0;
            while (i < 8) {
                allocateSpace = (allocateSpace << 8 ) | (long)(buffer[i++] & 0xFF);
            }

            Log.d(TAG, "Allocated Space is " + Long.toString(allocateSpace));

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }finally {
            context.setConnected(false);
        }
    }
}
