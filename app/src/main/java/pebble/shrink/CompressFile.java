package pebble.shrink;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class CompressFile extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener{

    public static TextView totalDevice, logView;
    private static final int FILE_CHOOSE_REQUEST=9;

    private String TAG = "CompressFile";
    public static String fileToCompress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);

        totalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        logView = (TextView) findViewById(R.id.tvCFlogView);

        P2pOperations.initNetwork(CompressFile.this);
        P2pOperations.dbReceiver = new DeviceBroadcastReceiver(CompressFile.this,P2pOperations.nChannel,P2pOperations.nManager);

        P2pOperations.createGroup();

    }

    public void updateThisDevice(WifiP2pDevice dev) {
        P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, CompressFile.this);
    }

    public void resetData() {
        totalDevice.setText(getResources().getString(R.string.cf_total_devices, 0));
    }

    public void onClickCompress(View view) {
        if(fileToCompress != null) {
            if (Integer.parseInt(totalDevice.getText().toString().split(": ")[1]) == 0) {

            } else {

            }
        }
    }

    public void onClickChooseFile(View view){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent,FILE_CHOOSE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent intent){
        if(requestCode == FILE_CHOOSE_REQUEST){
            if(resultCode == RESULT_OK){
                fileToCompress = intent.getData().getPath();
                Toast.makeText(CompressFile.this,fileToCompress,Toast.LENGTH_LONG);
            }
        }else{
            Log.d(TAG,"Invalid file");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "on pause");
        unregisterReceiver(P2pOperations.dbReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "on Resume");
        registerReceiver(P2pOperations.dbReceiver, P2pOperations.intentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "on Stop");
        P2pOperations.removeGroup();
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, CompressFile.this);
        } else {
            Log.d(TAG, "wifip2pinfo is null");
        }
    }

    @Override
    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
        if (wifiP2pGroup == null) {
            Log.d(TAG, "wifiP2pgroup is null");
            return;
        }
        int deviceCount = 0;
        Log.d(TAG, Arrays.toString(wifiP2pGroup.getClientList().toArray()));
        for (WifiP2pDevice device : wifiP2pGroup.getClientList()) {
            if(device !=null)   deviceCount++;
        }

        CompressFile.totalDevice.setText(getResources().getString(R.string.cf_total_devices, deviceCount));
    }
}
