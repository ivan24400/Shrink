package pebble.shrink;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class CompressFile extends AppCompatActivity {

    private String TAG = "CompressFile";

    private static final int FILE_CHOOSE_REQUEST = 9;

    public static TextView tvTotalDevice;
    public static Button btCompress;
    private static TextView tvFileName;

    private static Spinner spMethod;

    public static String fileToCompress;

    public static Handler cfHandler;

    private static IntentFilter intentFilter;
    private static WifiReceiver wifiReceiver;

    private static Distributor distributor;

    public CompressFile() {
        cfHandler = new Handler();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);

        distributor = new Distributor(CompressFile.this);
        WifiOperations.initWifiOperations(CompressFile.this);

        (new Thread(distributor)).start();

        getSupportActionBar().setTitle(R.string.cf_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        tvTotalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        tvFileName = (TextView) findViewById(R.id.tvCFfileName);
        btCompress = (Button) findViewById(R.id.btCFcompress);
        spMethod = (Spinner) findViewById(R.id.spCFmethod);

        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver,intentFilter);

    }

    public void resetData() {
        tvTotalDevice.setText(getResources().getString(R.string.cf_total_devices, 0));
    }

    public void onClickCompress(View view) {
        if (fileToCompress != null) {
            int method = spMethod.getSelectedItemPosition();

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
                Log.d(TAG, "File name: " + fileToCompress);
            }
        } else {
            Log.d(TAG, "Invalid file");
        }
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
        tvFileName.setText(getString(R.string.cf_file_name, "NA"));
        fileToCompress = null;
        unregisterReceiver(wifiReceiver);
        distributor.stop();
        WifiOperations.stop();
        super.onDestroy();

    }
}
