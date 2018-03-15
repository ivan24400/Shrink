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

    private static final int MAX_DEVICES_COUNT = 9;
    public static ServerSocket server;
    public static List<DeviceMaster> deviceList = new LinkedList<>();
    private static String TAG = "Distributor";
    private static ExecutorService executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);
    private Thread thisThread;

    private static boolean isStopped = false;


    public static int getServerPort(){
        return server.getLocalPort();
    }

    @Override
    public void run() {
        synchronized (this){
            this.thisThread = Thread.currentThread();
        }
        try {
            createSocket();
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

    private void createSocket() throws IOException{
        this.isStopped = false;
        server = new ServerSocket(0);

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
