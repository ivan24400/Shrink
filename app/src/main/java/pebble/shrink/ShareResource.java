package pebble.shrink;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ShareResource extends AppCompatActivity implements PeerListListener, ConnectionInfoListener {


    public String TAG = "Share Resource";

    private String goDeviceName;

    private static TextView deviceName, freeSpace;
    private static Spinner mpriority;
    private static EditText mfreeSpace;
    private static Button connect;
    private static boolean isConnect = true;

    public static final int batteryBlowerLimit = 31;
    public static final int batteryAlowerLimit = 61;

    private static DeviceSlave deviceSlave;

    public static Socket client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        getSupportActionBar().setTitle(R.string.sr_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        deviceName = (TextView) findViewById(R.id.tvSRdeviceName);
        freeSpace = (TextView) findViewById(R.id.tvSRfreespace);
        mfreeSpace = (EditText) findViewById(R.id.etSRsetFreespace);
        connect = (Button) findViewById(R.id.btSRconnect);
        mpriority = (Spinner) findViewById(R.id.spSRsetPriority);

        initFreeSpace();

        P2pOperations.initNetwork(ShareResource.this);
        P2pOperations.dbReceiver = new DeviceBroadcastReceiver(ShareResource.this,P2pOperations.nChannel,P2pOperations.nManager);

    }

    public void initFreeSpace(){
        (new Thread(new Runnable(){

            @Override
            public void run() {
                String metaData = P2pOperations.getDeviceInfo();
                final long fs = Long.parseLong(metaData.split("::")[0]);

                DeviceSlave.freeSpace = fs;
                DeviceSlave.batteryClass = metaData.split("::")[1].charAt(0);

                ShareResource.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        ShareResource.freeSpace.setText(Long.toString(fs));
                        ShareResource.mfreeSpace.setText(Long.toString(fs));
                    }
                });
            }
        })).start();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on Pause");
        unregisterReceiver(P2pOperations.dbReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "on Resume");
        registerReceiver(P2pOperations.dbReceiver, P2pOperations.intentFilter);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "on stop");
        P2pOperations.removeGroup();
        super.onStop();
    }

    public void resetData() {
        deviceName.setText(getResources().getString(R.string.sr_device_name,"NA","NA"));
        freeSpace.setText(getResources().getString(R.string.sr_freespace,0));
        mfreeSpace.setText(getResources().getString(R.string.empty));

    }

    public void updateThisDevice(WifiP2pDevice dev) {
        deviceName.setText(getResources().getString(R.string.sr_device_name, dev.deviceName, P2pOperations.getDeviceStatus(dev.status)));
        freeSpace.setText(getResources().getString(R.string.sr_freespace,
                Integer.getInteger(
                        P2pOperations.getDeviceInfo().split("::")[1])
        ));
        Log.d(TAG, "update device " + dev.deviceAddress);
    }

    public void clickSRconnect(View view) {
        if(isConnect){
            if(Long.parseLong(mfreeSpace.getText().toString()) > DeviceSlave.freeSpace){

            } else{
                P2pOperations.initiateDiscovery();
            }
        } else {
            // Disconnect
           P2pOperations.removeGroup();
            if(!P2pOperations.isP2pOn){
                connect.setText(getResources().getString(R.string.sr_connect));
                isConnect = true;
            }
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        Log.d(TAG, "on peers available " + P2pOperations.isP2pOn);
        if ((P2pOperations.progress != null) && P2pOperations.progress.isShowing())
            P2pOperations.progress.dismiss();

        for (WifiP2pDevice dev : wifiP2pDeviceList.getDeviceList()) {
            if (!P2pOperations.isP2pOn) {
                if (dev.deviceName.matches("SHRINK_GO_.*")) {
                    goDeviceName = dev.deviceName;
                    P2pOperations.connect(dev,0);
                    if(P2pOperations.isP2pOn){
                        isConnect = false;
                        connect.setText(getResources().getString(R.string.sr_disconnect));
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d(TAG, "on connection info available " + wifiP2pInfo.toString());
        try {
            deviceSlave = new DeviceSlave(wifiP2pInfo.groupOwnerAddress, Integer.parseInt(goDeviceName.split("_")[2]));
        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(this,R.string.m_connect_failed,Toast.LENGTH_SHORT).show();
            return;
        }
        connect.setText(getResources().getString(R.string.sr_disconnect));
        (new Thread(deviceSlave)).start();
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread client = new Thread(runnable);
        client.start();
    }

}
