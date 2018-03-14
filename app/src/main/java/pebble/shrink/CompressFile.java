package pebble.shrink;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.Arrays;

public class CompressFile extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener, WifiP2pManager.GroupInfoListener{

    private String TAG = "CompressFile";

    private static final int FILE_CHOOSE_REQUEST=9;

    public static TextView tvTotalDevice;
    public static Button btCompress;
    private static TextView tvFileName;

    private static Spinner spMethod;

    public static String fileToCompress;

    public static Handler cfHandler;

    public CompressFile(){
        cfHandler = new Handler();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);

        getSupportActionBar().setTitle(R.string.cf_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        tvTotalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        tvFileName = (TextView) findViewById(R.id.tvCFfileName);
        btCompress = (Button) findViewById(R.id.btCFcompress);
        spMethod = (Spinner) findViewById(R.id.spCFmethod);

        P2pOperations.initNetwork(CompressFile.this);
        P2pOperations.dbReceiver = new DeviceBroadcastReceiver(CompressFile.this,P2pOperations.nChannel,P2pOperations.nManager);

        P2pOperations.createGroup();

    }

    public void updateThisDevice(WifiP2pDevice dev) {
        P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, CompressFile.this);
    }

    public void resetData() {
        tvTotalDevice.setText(getResources().getString(R.string.cf_total_devices, 0));

    }

    public void onClickCompress(View view) {
        if(fileToCompress != null) {
            int method = spMethod.getSelectedItemPosition();

            Intent intent = new Intent(this,CompressionService.class);
            intent.putExtra(CompressionUtils.cmethod,method);
            intent.putExtra(CompressionUtils.cfile,fileToCompress);

            if (Integer.parseInt(tvTotalDevice.getText().toString().split(": ")[1]) == 0) {
                CompressionUtils.isLocal=true;
                P2pOperations.removeGroup();

                intent.setAction(CompressionUtils.ACTION_COMPRESS_LOCAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startService(intent);
            } else {
                CompressionUtils.isLocal=false;
                intent.setAction(CompressionUtils.ACTION_COMPRESS_WRITE_HEADER);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NotificationUtils.startNotification(new CompressionService(),intent,getString(R.string.compressing));

            }
        } else {
            Toast.makeText(this,"First choose a file !",Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickChooseFile(View view){
        Intent intent = new Intent(this,FileChooser.class);

        startActivityForResult(intent,FILE_CHOOSE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent intent){
        if(requestCode == FILE_CHOOSE_REQUEST){
            if(resultCode == RESULT_OK){
                fileToCompress = intent.getStringExtra(FileChooser.EXTRA_FILE_PATH);
                tvFileName.setText(getString(R.string.cf_file_name,fileToCompress));
                Log.d(TAG,"File name: "+fileToCompress);
            }
        }else{
            Log.d(TAG,"Invalid file");
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on pause");
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
        Log.d(TAG, "on Stop");
        tvFileName.setText(getString(R.string.cf_file_name,"NA"));
        fileToCompress = null;

        P2pOperations.removeGroup();
        super.onStop();
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
        CompressFile.tvTotalDevice.setText(getResources().getString(R.string.cf_total_devices, deviceCount));
    }
}
