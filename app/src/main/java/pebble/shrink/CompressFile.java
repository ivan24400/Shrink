package pebble.shrink;

import android.content.Intent;
import android.content.IntentFilter;
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

    private static Spinner spAlgorithm;

    public static String fileToCompress;

    public static Handler cfHandler;

    private static IntentFilter intentFilter;
    private static WifiReceiver wifiReceiver;

    private static Distributor distributor;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);
        cfHandler = new Handler(Looper.getMainLooper());

        getSupportActionBar().setTitle(R.string.cf_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        tvTotalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        tvFileName = (TextView) findViewById(R.id.tvCFfileName);
        btCompress = (Button) findViewById(R.id.btCFcompress);
        spAlgorithm = (Spinner) findViewById(R.id.spCFmethod);
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

            Intent intent = new Intent(this, CompressionService.class);
            intent.putExtra(CompressionUtils.cmethod, method);
            intent.putExtra(CompressionUtils.cfile, fileToCompress);

            if (Integer.parseInt(tvTotalDevice.getText().toString().split(": ")[1]) == 0) {
                // If no devices are connected
                CompressionUtils.isLocal = true;
                WifiOperations.setWifiApEnabled(false);

                intent.setAction(CompressionUtils.ACTION_COMPRESS_LOCAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startService(intent);
            } else {
                // If one or more devices are connected
                CompressionUtils.isLocal = false;
                intent.setAction(CompressionUtils.ACTION_COMPRESS_REMOTE);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startService(intent);
            }
        } else {
            Toast.makeText(this, "First choose a file !", Toast.LENGTH_SHORT).show();
        }
    }

    public static int getAlgorithm(){
        return spAlgorithm.getSelectedItemPosition();
    }

    public void onClickReceiverSwitch(View view){
      if(((Switch)view).isChecked()){
          distributor = new Distributor(CompressFile.this);
          (new Thread(distributor)).start();
          btChooseFile.setEnabled(false);
          spAlgorithm.setEnabled(false);
        }else{
          if(distributor != null){
              distributor.stop();
          }
          btChooseFile.setEnabled(true);
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
                fileToCompress = intent.getStringExtra(FileChooser.EXTRA_FILE_PATH);
                tvFileName.setText(getString(R.string.cf_file_name, fileToCompress));
                TaskAllocation.setFileSize((new File(fileToCompress)).length());
            }
        } else {
            Log.d(TAG, "Invalid file");
        }
    }

    public static void setEnabledWidget(boolean state){
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
        if(distributor != null){
            distributor.stop();
        }
        NotificationUtils.removeNotification();

        super.onDestroy();

    }

}
