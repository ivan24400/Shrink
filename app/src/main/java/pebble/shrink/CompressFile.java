package pebble.shrink;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
    static TextView tvTotalDevice;
    static Button btCompress, btChooseFile;
    static String fileToCompress;
    static Handler handler;
    static Switch swRemote;
    private static int deviceCount = 0;
    private static Spinner spAlgorithm;
    private static WifiReceiver wifiReceiver;
    private TextView tvFileName;

    /**
     * Return user selected algorithm
     *
     * @return algorithm
     */
    static int getAlgorithm() {
        return spAlgorithm.getSelectedItemPosition();
    }

    /**
     * Represents total connected slave devices
     *
     * @param c           Current context
     * @param isIncrement to increment or decrement device count
     */
    static synchronized void updateDeviceCount(final Context c, final boolean isIncrement) {
        if (isIncrement) {
            deviceCount++;
        } else if (deviceCount > 0) {
            deviceCount--;
            if (deviceCount == 0) {
                setWidgetEnabled(true);
            }
        }
        CompressFile.handler.post(new Runnable() {
            @Override
            public void run() {
                CompressFile.tvTotalDevice.setText(c.getString(R.string.cf_total_devices, deviceCount));
            }
        });
    }

    /**
     * Enable or disable widgets
     *
     * @param state to enable or disable
     */
    static void setWidgetEnabled(boolean state) {
        btCompress.setEnabled(state);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);

        getSupportActionBar().setTitle(R.string.cf_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        handler = new Handler(Looper.getMainLooper());
        fileToCompress = null;

        tvTotalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        tvFileName = (TextView) findViewById(R.id.tvCFfileName);
        btCompress = (Button) findViewById(R.id.btCFcompress);
        spAlgorithm = (Spinner) findViewById(R.id.spCFmethod);
        swRemote = (Switch) findViewById(R.id.swCFdone);
        btChooseFile = (Button) findViewById(R.id.btCFchooseFile);

        WifiOperations.initWifiOperations(CompressFile.this);

        wifiReceiver = new WifiReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");

        registerReceiver(wifiReceiver, intentFilter);

    }

    /**
     * Start compression algorithm either locally
     * or in distributed way
     *
     * @param view Current view
     */
    public void onClickCompress(View view) {
        if (fileToCompress != null) {
            int method = getAlgorithm();

            if (!swRemote.isChecked()) {
                // If no devices are connected
                CompressionUtils.isLocal = true;
                WifiOperations.setWifiApEnabled(false);

                Intent intent = new Intent(this, CompressionService.class);
                intent.putExtra(CompressionUtils.CMETHOD, method);
                intent.putExtra(CompressionUtils.CFILE, fileToCompress);
                intent.setAction(CompressionUtils.ACTION_COMPRESS_LOCAL);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startService(intent);
            } else {
                // If one or more devices are connected
                CompressionUtils.isLocal = false;
                TaskAllocation ta = new TaskAllocation();
                if (!ta.allocate()) {
                    Toast.makeText(this, getString(R.string.err_task_allocation), Toast.LENGTH_SHORT).show();
                    return;
                }
                DistributorService.startDistribution(this);
            }
        } else {
            Toast.makeText(this, getString(R.string.err_file_not_selected), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start/stop distributed compression service
     *
     * @param view Current view
     */
    public void onClickReceiverSwitch(View view) {
        Intent tintent = new Intent(this, DistributorService.class);
        tintent.putExtra(CompressionUtils.CMETHOD, getAlgorithm());

        if (((Switch) view).isChecked()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((Switch) view).setChecked(false);
                Toast.makeText(this, getString(R.string.err_os_not_supported), Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    ((Switch) view).setChecked(false);
                    Toast.makeText(this, getString(R.string.err_permission_denied), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    startActivity(intent);
                    return;
                }
            }
            if (WifiOperations.isWifiApOn()) {
                ((Switch) view).setChecked(false);
                Toast.makeText(this, getString(R.string.err_disable_wifiap), Toast.LENGTH_SHORT).show();
                return;
            }

            if (fileToCompress == null) {
                ((Switch) view).setChecked(false);
                Toast.makeText(this, getString(R.string.err_file_not_selected), Toast.LENGTH_SHORT).show();
                return;
            }
            tintent.setAction(DistributorService.ACTION_START_FOREGROUND);
            startService(tintent);
        } else {
            tintent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
            startService(tintent);
        }
    }

    /**
     * Starts a file chooser
     *
     * @param view Current view
     */
    public void onClickChooseFile(View view) {
        Intent intent = new Intent(this, FileChooser.class);
        intent.setAction(FileChooser.FILE_CHOOSER_ALL);
        startActivityForResult(intent, FILE_CHOOSE_REQUEST);
    }

    /**
     * When user optionally selects a file
     * from the file chooser
     *
     * @param requestCode custom code to verify file choose operation
     * @param resultCode  is a file selected
     * @param intent      result of file chooser activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == FILE_CHOOSE_REQUEST) {
            if (resultCode == RESULT_OK) {
                fileToCompress = intent.getStringExtra(FileChooser.EXTRA_FILE_PATH);
                File tmp = new File(fileToCompress);
                if (tmp.exists()) {
                    if (tmp.length() > DeviceOperations.getFreeSpace()) {
                        Toast.makeText(this, getString(R.string.err_insufficient_storage), Toast.LENGTH_SHORT).show();
                        fileToCompress = null;
                        return;
                    }
                    tvFileName.setText(getString(R.string.cf_file_name, tmp.getName() + " (" + tmp.length() + " B)"));
                    TaskAllocation.setFileSize(tmp.length());

                    synchronized (DistributorService.sync) {
                        DistributorService.sync.notify();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.err_file_not_found), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.d(TAG, "Invalid file");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        unregisterReceiver(wifiReceiver);
        deviceCount = 0;
        fileToCompress = null;
        if (DistributorService.isServerOn) {
            Intent intent = new Intent(this, DistributorService.class);
            intent.setAction(DistributorService.ACTION_STOP_FOREGROUND);
            startService(intent);
        }
        super.onDestroy();

    }

}
