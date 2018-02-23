package pebble.shrink;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CreateGroup extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {
    public static TextView deviceName, my_ip, groupName, groupPassphrase;
    public static DeviceListAdapter deviceListAdapter;
    public static List<Device> devices = new ArrayList<>();
    private static ListView deviceListView;
    private String TAG = "CreateGroup";
    private DeviceGroupListener dg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.create_group));
        setSupportActionBar(toolbar);

        dg = new DeviceGroupListener(CreateGroup.this);
        deviceListView = (ListView) findViewById(R.id.lvCGdeviceList);
        deviceName = (TextView) findViewById(R.id.tvCGdeviceName);
        my_ip = (TextView) findViewById(R.id.tvCGmyIp);
        groupName = (TextView) findViewById(R.id.tvCGgroupName);
        groupPassphrase = (TextView) findViewById(R.id.tvCGgroupPassphrase);

        deviceListAdapter = new DeviceListAdapter(CreateGroup.this, R.layout.device_list_item, devices);
        deviceListView.setAdapter(deviceListAdapter);
        deviceListView.setEmptyView(findViewById(R.id.tvCGemptyList));

        P2pOperations.initNetwork(CreateGroup.this);
        P2pOperations.createGroup();
    }

    public void resetData() {
        devices.clear();
        deviceName.setText("Device: NA");
        my_ip.setText("IP Address: NA");
        groupName.setText("Group Name: NA");
        groupPassphrase.setText("Group Passphrase: NA");
    }

    public void updateThisDevice(WifiP2pDevice dev) {
        resetData();
        Log.d(TAG, dev.toString());
        deviceName.setText(getResources().getString(R.string.device_name, dev.deviceName, P2pOperations.getDeviceStatus(dev.status)));
        String tmp = P2pOperations.getMyIpAddress();
        if (!tmp.equals("NA"))
            my_ip.setText(getResources().getString(R.string.ip_address, tmp, ((dev.isGroupOwner() == true) ? "GO" : "GM")));
        else
            my_ip.setText(getResources().getString(R.string.ip_address, tmp, ""));
        P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, dg);
    }

    public void clickGroupInfo(View view) {
        if (!devices.isEmpty()) {
            P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, dg);
        } else {
            Toast.makeText(CreateGroup.this, "At least one device is required !", Toast.LENGTH_LONG);
        }
    }

    public void clickNext(View view) {
        if (!devices.isEmpty()) {
            Intent intent = new Intent(CreateGroup.this, Device.class);
            startActivity(intent);
        } else {
            Toast.makeText(CreateGroup.this, "At least one device is required !", Toast.LENGTH_LONG);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_group:
                P2pOperations.createGroup();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, dg);
        } else {
            Log.d(TAG, "wifip2pinfo is null");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "on Resume");
        //  P2pOperations.dbReceiver = new DeviceBroadcastReceiver(CreateGroup.this, P2pOperations.nChannel, P2pOperations.nManager);
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
        Distributor.closeGroup();
        P2pOperations.removeGroup();

        super.onStop();

    }
}
