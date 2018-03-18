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
    public static boolean isConnect = false;

    private WifiScanner wifiScanner;
    private static IntentFilter intentFilter;

    private static DeviceSlave deviceSlave;
    public static Thread deviceSlaveThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        getSupportActionBar().setTitle(R.string.sr_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        WifiOperations.initWifiOperations(ShareResource.this);

        deviceName = (TextView) findViewById(R.id.tvSRdeviceName);
        deviceStatus = (TextView) findViewById(R.id.tvSRdeviceStatus);
        freeSpace = (TextView) findViewById(R.id.tvSRfreespace);
        mfreeSpace = (EditText) findViewById(R.id.etSRsetFreespace);
        connect = (Button) findViewById(R.id.btSRconnect);
        mpriority = (Spinner) findViewById(R.id.spSRsetPriority);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        initFreeSpace();

    }

    public void initFreeSpace(){
        (new Thread(new Runnable(){

            @Override
            public void run() {
                String metaData = DeviceOperations.getDeviceInfo(ShareResource.this);
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
        Log.d(TAG, "on Resume");
        wifiScanner = new WifiScanner(ShareResource.this);
        registerReceiver(wifiScanner,intentFilter);
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "on stop");
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"on destroy");
        isConnect = false;
        if(wifiScanner != null) {
            unregisterReceiver(wifiScanner);
            wifiScanner = null;
        }
        if(deviceSlaveThread != null){
            deviceSlaveThread.interrupt();
        }
        WifiOperations.stop();
        super.onDestroy();
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
                WifiOperations.startScan();
            }
        } else {
            // Disconnect
            isConnect = false;
            WifiOperations.setWifiEnabled(false);
        }
    }

}
