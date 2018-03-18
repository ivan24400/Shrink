package pebble.shrink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
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

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ShareResource extends AppCompatActivity {

    public String TAG = "Share Resource";

    private String goDeviceName;

    private static TextView deviceName, deviceStatus,freeSpace;
    private static Spinner mpriority;
    private static EditText mfreeSpace;
    private static Button connect;
    private static boolean isConnect = false;

    private static WifiOperations wifiOperations;
    private WifiScanner wifiScanner;
    private static IntentFilter intentFilter;

    private static DeviceSlave deviceSlave;

    public static Socket client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        getSupportActionBar().setTitle(R.string.sr_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        deviceName = (TextView) findViewById(R.id.tvSRdeviceName);
        deviceStatus = (TextView) findViewById(R.id.tvSRdeviceStatus);
        freeSpace = (TextView) findViewById(R.id.tvSRfreespace);
        mfreeSpace = (EditText) findViewById(R.id.etSRsetFreespace);
        connect = (Button) findViewById(R.id.btSRconnect);
        mpriority = (Spinner) findViewById(R.id.spSRsetPriority);

        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        wifiOperations = new WifiOperations(ShareResource.this);
        initFreeSpace();

    }

    public void initFreeSpace(){
        (new Thread(new Runnable(){

            @Override
            public void run() {
                String metaData = "12345678::B";
                final long fs = Long.parseLong(metaData.split("::")[0]);

                DeviceSlave.freeSpace = fs;
                DeviceSlave.batteryClass = metaData.split("::")[1].charAt(0);

                ShareResource.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ShareResource.freeSpace.setText(getString(R.string.sr_freespace,fs));
                        ShareResource.mfreeSpace.setText(Long.toString(fs));
                    }
                });
            }
        })).start();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on Pause");
        if(wifiScanner != null) {
            unregisterReceiver(wifiScanner);
            wifiScanner = null;
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        wifiScanner = new WifiScanner(ShareResource.this);
        registerReceiver(wifiScanner,intentFilter);

        super.onResume();
        Log.d(TAG, "on Resume");
    }

    @Override
    public void onStop() {
        Log.d(TAG, "on stop");
        isConnect = false;
        if(wifiScanner != null) {
            unregisterReceiver(wifiScanner);
            wifiScanner = null;
        }
        super.onStop();
    }

    public void resetData() {
        deviceStatus.setText(getString(R.string.sr_device_status,"NA"));
    }

    public void clickSRconnect(View view) {
        Log.d(TAG,"clickconnect "+isConnect);
        if(!isConnect){
            if(Long.parseLong(mfreeSpace.getText().toString()) > DeviceSlave.freeSpace){
                Toast.makeText(this,R.string.sr_err_maxspace,Toast.LENGTH_LONG).show();
            } else{
                WifiOperations.getWifiManager().startScan();
            }
        } else {
            // Disconnect

        }
    }

}
