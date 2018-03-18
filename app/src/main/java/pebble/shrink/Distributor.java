package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ivan on 21-09-2017.
 */

public class Distributor implements Runnable{

    public static final int HEADER_SIZE = 9; // freeSpace = 8, battery = 1

    private static final int MAX_DEVICES_COUNT = 9;
    public static ServerSocket server;
    public static List<DeviceMaster> deviceList = new LinkedList<>();
    private static String TAG = "Distributor";
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    private static boolean isStopped = false;

    public int getServerPort(){
        synchronized (this) {
            if (server != null) {
                return server.getLocalPort();
            } else {
                return -1;
            }
        }
    }

    @Override
    public void run() {
          try {
              server = new ServerSocket(0);

              while(!isStopped()) {
                Socket client = server.accept();
                Log.d(TAG, "Connected " + client.getInetAddress());
                if(isStopped()){
                    Log.d(TAG, "Server is stopped");
                    return;
                }
                executor.execute(new DeviceMaster(client));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.executor.shutdown();
    }

    private synchronized boolean isStopped(){
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
