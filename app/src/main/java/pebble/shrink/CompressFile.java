package pebble.shrink;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class CompressFile extends AppCompatActivity {

    private String TAG = "CompressFile";

    private static final int FILE_CHOOSE_REQUEST = 9;

    public static TextView tvTotalDevice;
    public static Button btCompress, btChooseFile;

    private static TextView tvFileName;
    private static int deviceCount = 0;

    private static Spinner spAlgorithm;
    private static Switch swRemote;
    public static String fileToCompress;

    public static Handler handler;

    private static IntentFilter intentFilter;
    private static WifiReceiver wifiReceiver;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);
        handler = new Handler(Looper.getMainLooper());

        getSupportActionBar().setTitle(R.string.cf_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        tvTotalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        tvFileName = (TextView) findViewById(R.id.tvCFfileName);
        btCompress = (Button) findViewById(R.id.btCFcompress);
        spAlgorithm = (Spinner) findViewById(R.id.spCFmethod);
        swRemote = (Switch)findViewById(R.id.swCFdone);
        btChooseFile = (Button)findViewById(R.id.btCFchooseFile);

        WifiOperations.initWifiOperations(CompressFile.this);

        wifiReceiver = new WifiReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");

        registerReceiver(wifiReceiver,intentFilter);

    }

    public void onClickCompress(View view) {
        if (fileToCompress != null) {
            int method = spAlgorithm.getSelectedItemPosition();

            if (!swRemote.isChecked()) {
                // If no devices are connected
                CompressionUtils.isLocal = true;
                WifiOperations.setWifiApEnabled(false);

                Intent intent = new Intent(this, CompressionService.class);
                intent.putExtra(CompressionUtils.cmethod, method);
                intent.putExtra(CompressionUtils.cfile, fileToCompress);
                intent.setAction(CompressionUtils.ACTION_COMPRESS_LOCAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startService(intent);
            } else {
                // If one or more devices are connected
                CompressionUtils.isLocal = false;
                TaskAllocation ta = new TaskAllocation();
                if(!ta.allocate()){
                    Toast.makeText(this,getString(R.string.err_task_allocation),Toast.LENGTH_SHORT).show();
                    return;
                }
                DistributorService.startDistribution(this);
            }
        } else {
            Toast.makeText(this, "First choose a file !", Toast.LENGTH_SHORT).show();
        }
    }

    public static int getAlgorithm(){
        return spAlgorithm.getSelectedItemPosition();
    }

    public void onClickReceiverSwitch(View view){
        Intent tintent = new Intent(this,DistributorService.class);
        tintent.putExtra(CompressionUtils.cmethod, spAlgorithm.getSelectedItemPosition());

        if(((Switch)view).isChecked()){

            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                ((Switch)view).setChecked(true);
                Toast.makeText(this,getString(R.string.err_os_not_supported),Toast.LENGTH_SHORT).show();
                return;
            }

            tintent.setAction(DistributorService.ACTION_START_FOREGROUND);
            startService(tintent);

          spAlgorithm.setEnabled(false);
        }else{
            tintent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
            startService(tintent);

          spAlgorithm.setEnabled(true);
      }
    }

    public void onClickChooseFile(View view) {
        Intent intent = new Intent(this, FileChooser.class);
        startActivityForResult(intent, FILE_CHOOSE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CHOOSE_REQUEST) {
            if (resultCode == RESULT_OK) {
                synchronized(DistributorService.sync){
                    fileToCompress = intent.getStringExtra(FileChooser.EXTRA_FILE_PATH);
                    DistributorService.sync.notify();
                }
                tvFileName.setText(getString(R.string.cf_file_name, fileToCompress));
                TaskAllocation.setFileSize((new File(fileToCompress)).length());
            }
        } else {
            Log.d(TAG, "Invalid file");
        }
    }

    public static synchronized void updateDeviceCount(final Context c, final boolean isIncrement){
        if(isIncrement){
            deviceCount++;
        }else{
            deviceCount--;
            if(deviceCount == 0){
                setWidgetEnabled(true);
                CompressFile.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        CompressFile.swRemote.setChecked(false);
                    }
                });
            }
        }
        CompressFile.handler.post(new Runnable() {
            @Override
            public void run() {
                CompressFile.tvTotalDevice.setText(c.getString(R.string.cf_total_devices,deviceCount));

            }
        });

    }

    public static void setWidgetEnabled(boolean state){
        btCompress.setEnabled(state);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on pause");
        super.onPause();
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "on Resume");

    }

    @Override
    public void onStop() {
        Log.d(TAG, "on Stop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        unregisterReceiver(wifiReceiver);
        deviceCount = 0;

       Intent intent = new Intent(this,DistributorService.class);
        intent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
        startService(intent);

        NotificationUtils.removeNotification();

        super.onDestroy();

    }

}
