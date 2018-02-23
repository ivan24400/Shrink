package pebble.shrink;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Ivan on 21-09-2017.
 */

public class Distributor {

    private static final int MAX_DEVICES_COUNT = 9;
    public static ServerSocket server;
    public static List<Device> deviceList;
    private static String TAG = "Distributor";
    private static Executor executor = Executors.newFixedThreadPool(MAX_DEVICES_COUNT);

    public static void acceptDevices() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket client = server.accept();
                    Log.d(TAG, "Connected " + client.getInetAddress());
                    deviceList.add(new Device(client));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void closeGroup() {
        if (server != null) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
