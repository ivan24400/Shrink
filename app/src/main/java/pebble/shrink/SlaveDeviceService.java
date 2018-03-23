package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class SlaveDeviceService extends Service {

    private static final String TAG = "SlaveDeviceService";

    public static final String EXTRA_PORT = "ps.SlaveDeviceService.port";

    private static final String tmp_file= Environment.getExternalStorageDirectory()+"Shrink/tmp.dat";
    private static final String tmpc_file= Environment.getExternalStorageDirectory()+"Shrink/tmp.dat.dcrz";

    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;
    private static int algorithm;
    private static byte[] buffer = new byte[DataTransfer.BUFFER_SIZE];

    public static long freeSpace, allocatedSpace, compressedSize;
    public static char batteryClass;

    private static void initMetaData() throws IOException{

        int i = 0;
        int shift = 56;
        while (i < 8) {
            buffer[i] = (byte) ((freeSpace >> shift )& 0xFF);
            shift = shift - 8;
            i++;
        }
        buffer[i] = ((byte) (batteryClass & 0xFF));

        out.write(buffer, 0, DataTransfer.HEADER_SIZE);
        out.flush();
        Log.d(TAG,"sent: "+Arrays.toString(buffer));

        in.read(buffer, 0,  DataTransfer.HEADER_SIZE);
        Log.d(TAG,"read: "+ Arrays.toString(buffer));

        i = 0;
        while (i < 8) {
            allocatedSpace = (allocatedSpace << 8 ) | (long)(buffer[i++] & 0xFF);
        }
        algorithm = buffer[i];
        Log.d(TAG, "Allocated Space is " + Long.toString(allocatedSpace)+ " algorithm is "+algorithm);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        (new Thread(new Runnable(){

            @Override
            public void run() {
                try{
                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent1 = new Intent(SlaveDeviceService.this,ShareResource.class);
                            NotificationUtils.startNotification(SlaveDeviceService.this,intent1);
                            ShareResource.setConnected(SlaveDeviceService.this,true);
                        }
                    });

                    SlaveDeviceService.this.startForeground(NotificationUtils.NOTIFICATION_ID,NotificationUtils.notification);

                    InetAddress addr = InetAddress.getByName(SlaveDeviceService.this.getString(R.string.sr_server_ip));
                    int port = intent.getIntExtra(EXTRA_PORT,-1);

                    Log.d(TAG,"device slave socket "+addr+" @ "+port);

                    socket = new Socket(addr, port);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();

                   initMetaData();

                   DataTransfer.initFile(tmpc_file,tmp_file);

                    out.write(DataTransfer.READY);
                    out.flush();

                    // Receive chunk from master device
                    DataTransfer.receiveChunk(allocatedSpace,in);

                    // Compress chunk
                    CompressionUtils.compress(algorithm,tmp_file);

                    // Send compressed size to master device.
                    compressedSize = (new File(tmpc_file)).length();
                    int i = 0;
                    int shift = 56;
                    while (i < 8) {
                        buffer[i] = (byte) ((compressedSize >> shift )& 0xFF);
                        shift = shift - 8;
                        i++;
                    }
                    out.write(buffer,0,8);
                    out.flush();

                    // Receive ready signal from master
                    if(in.read() != DataTransfer.READY){
                        throw new IOException("Invalid signal");
                    }

                    // Send compressed output to master
                    DataTransfer.transferChunk(compressedSize,out);

                    // End of process
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }finally{
                    WifiOperations.setWifiEnabled(false);
                    stopForeground(false);
                    stopSelf();

                    ShareResource.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(SlaveDeviceService.this.getString(R.string.completed));
                            ShareResource.setConnected(SlaveDeviceService.this,false);
                        }
                    });
                }
            }
        })).start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
