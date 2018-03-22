package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class SlaveDeviceService extends Service {

    private static final String TAG = "SlaveDeviceService";

    public static final String EXTRA_PORT = "ps.SlaveDeviceService.port";

    private static Socket socket;
    private static InputStream in;
    private static OutputStream out;

    public static long freeSpace, allocateSpace;
    public static char batteryClass;


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
                            NotificationUtils.startNotification(SlaveDeviceService.this,intent1,SlaveDeviceService.this.getString(R.string.initializing));
                            ShareResource.setConnected(SlaveDeviceService.this,true);
                        }
                    });

                    InetAddress addr = InetAddress.getByName(SlaveDeviceService.this.getString(R.string.sr_server_ip));
                    int port = intent.getIntExtra(EXTRA_PORT,-1);

                    Log.d(TAG,"device slave socket "+addr+" @ "+port);

                    socket = new Socket(addr, port);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();

                    byte[] buffer = new byte[Distributor.HEADER_SIZE];
                    int i = 0;
                    int shift = 56;
                    while (i < 8) {
                        buffer[i] = (byte) ((freeSpace >> shift )& 0xFF);
                        shift = shift - 8;
                        i++;
                    }
                    buffer[i] = ((byte) (batteryClass & 0xFF));

                    out.write(buffer, 0, Distributor.HEADER_SIZE);
                    out.flush();
                    Log.d(TAG,"sent: "+Arrays.toString(buffer));

                    in.read(buffer, 0,  Distributor.HEADER_SIZE);
                    Log.d(TAG,"read: "+ Arrays.toString(buffer));

                    i = 0;
                    while (i < 8) {
                        allocateSpace = (allocateSpace << 8 ) | (long)(buffer[i++] & 0xFF);
                    }

                    Log.d(TAG, "Allocated Space is " + Long.toString(allocateSpace)+ " algorithm is "+buffer[i]);

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
