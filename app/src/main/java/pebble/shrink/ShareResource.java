package pebble.shrink;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ShareResource extends AppCompatActivity implements PeerListListener, ConnectionInfoListener {

    private static final int CHANNEL_ID = 23456;
    public static float batteryPercent;
    private static Intent batteryIntent;
    public String TAG = "Share Resource";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder nBuilder;

    private WifiP2pDevice myDevice;
    private String goDeviceName;

    private static TextView logs, deviceName, freeSpace;
    private static Spinner priority;
    private static EditText mfreeSpace;
    private static Button connect;

    public static Socket client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        logs = (TextView) findViewById(R.id.tvSRlogView);
        deviceName = (TextView) findViewById(R.id.tvSRdeviceName);
        freeSpace = (TextView) findViewById(R.id.tvSRfreespace);
        mfreeSpace = (EditText) findViewById(R.id.etSRsetFreespace);
        connect = (Button) findViewById(R.id.btSRconnect);
        priority = (Spinner) findViewById(R.id.spSRsetPriority);

        setupNotification();


        Log.d(TAG, "Battery percent" + batteryPercent);
        P2pOperations.initNetwork(ShareResource.this);
    }


    public void setupNotification() {
        nBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.android).setContentTitle("Shrink").setContentText("Running in background").setOngoing(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, ShareResource.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        nBuilder.setContentIntent(pendingIntent);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "on Resume");
        P2pOperations.dbReceiver = new DeviceBroadcastReceiver(ShareResource.this, P2pOperations.nChannel, P2pOperations.nManager);
        registerReceiver(P2pOperations.dbReceiver, P2pOperations.intentFilter);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on Pause");
        super.onPause();
        unregisterReceiver(P2pOperations.dbReceiver);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "on stop");
        clickExitGroup(null);
        super.onStop();
    }

    public void resetData() {
        deviceName.setText(getResources().getString(R.string.sr_device_name,"NA"));
        freeSpace.setText(getResources().getString(R.string.sr_freespace,0));
        mfreeSpace.setText(getResources().getString(R.string.empty));

    }

    public void updateThisDevice(WifiP2pDevice dev) {
        this.myDevice = dev;
        deviceName.setText(getResources().getString(R.string.device_name, dev.deviceName, P2pOperations.getDeviceStatus(dev.status)));
        Log.d(TAG, "update device " + dev.deviceAddress);
    }

    public void clickJoinGroup(View view) {
        P2pOperations.initiateDiscovery();
    }

    public void clickExitGroup(View view) {
        resetData();
        if (notificationManager != null) {
            notificationManager.cancel(CHANNEL_ID);
        }
        P2pOperations.nManager.removeGroup(P2pOperations.nChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + P2pOperations.errToString(reasonCode));
            }

            @Override
            public void onSuccess() {
                P2pOperations.isP2pOn = false;
                Log.d(TAG, "DisConnected Successfully");
                exit.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        Log.d(TAG, "on peers available " + P2pOperations.isP2pOn);
        if ((P2pOperations.progress != null) && P2pOperations.progress.isShowing())
            P2pOperations.progress.dismiss();

        for (WifiP2pDevice dev : wifiP2pDeviceList.getDeviceList()) {
            logs.append("\nFound Device: " + dev.deviceName);
            if (!P2pOperations.isP2pOn) {
                if (dev.deviceName.matches("DVZN_GO_.*")) {
                    goDeviceName = dev.deviceName;
                    P2pOperations.connect(dev,0);
                } else {
                    goDeviceName = "NA";
                }
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d(TAG, "on connection info available " + wifiP2pInfo.toString());
        connect.setText(getResources().getString(R.string.sr_disconnect));
        P2pOperations.isP2pOn = true;
        logs.setText("Connected to " + goDeviceName);
        connectToGroup(wifiP2pInfo.groupOwnerAddress, Integer.parseInt(goDeviceName.split("_")[2]));
    }

    private void connectToGroup(final InetAddress serverAddress, final int port) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    client = new Socket(serverAddress, port);
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    PrintWriter out = new PrintWriter(client.getOutputStream());
                    out.flush();
                    Log.d(TAG, "Sending data");
                    out.println(P2pOperations.getDeviceInfo());
                    out.flush();
                    String data = in.readLine();
                    logs.append("Compressing " + data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread client = new Thread(runnable);
        client.start();
    }

}
