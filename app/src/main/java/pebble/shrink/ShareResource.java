package pebble.shrink;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ShareResource extends AppCompatActivity {

    static final String TAG = "Share Resource";
    static final String tmp_file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Shrink/tmp.dat";
    static final String tmpc_file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Shrink/tmp.dat.dcrz";

    static Spinner mpriority;
    static Button connect;
    static boolean isConnect = false;
    static Handler handler;
    static EditText mfreeSpace;
    private static TextView deviceName, deviceStatus, freeSpace;
    private static IntentFilter intentFilter;
    private WifiScanner wifiScanner;
    static AppCompatActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        getSupportActionBar().setTitle(R.string.sr_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        WifiOperations.initWifiOperations(ShareResource.this);
        handler = new Handler(Looper.getMainLooper());
        activity = this;
        deviceName = (TextView) findViewById(R.id.tvSRdeviceName);
        deviceStatus = (TextView) findViewById(R.id.tvSRdeviceStatus);
        freeSpace = (TextView) findViewById(R.id.tvSRfreespace);
        mfreeSpace = (EditText) findViewById(R.id.etSRsetFreespace);
        connect = (Button) findViewById(R.id.btSRconnect);
        mpriority = (Spinner) findViewById(R.id.spSRsetPriority);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        initDeviceStat();

        wifiScanner = new WifiScanner();
        registerReceiver(wifiScanner, intentFilter);
    }

    /**
     * Initialize text views with their corresponding values
     */
    private void initDeviceStat() {
        (new Thread(new Runnable() {

            @Override
            public void run() {
                String metaData = DeviceOperations.getDeviceInfo(ShareResource.this);
                final long fs = Long.parseLong(metaData.split("::")[0]);

                SlaveDeviceService.freeSpace = fs / 2;
                SlaveDeviceService.batteryClass = metaData.split("::")[1].charAt(0);

                ShareResource.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (SlaveDeviceService.batteryClass == 'A') {
                            ShareResource.mpriority.setSelection(1);
                        } else {
                            ShareResource.mpriority.setSelection(0);
                        }
                        ShareResource.deviceName.setText(getString(R.string.sr_device_name, Settings.Secure.getString(getContentResolver(), "bluetooth_name")));
                        ShareResource.freeSpace.setText(getString(R.string.sr_freespace, SlaveDeviceService.freeSpace));
                        ShareResource.mfreeSpace.setText(Long.toString(SlaveDeviceService.freeSpace));
                    }
                });
            }
        })).start();
    }

    /**
     * Modifiy enabled status of widgets
     *
     * @param context Current context
     * @param state   enabled or disabled
     */
    public static void setConnected(final Context context, final boolean state) {
        isConnect = state;
        mpriority.setEnabled(!state);
        mfreeSpace.setEnabled(!state);
        if (state) {
            deviceStatus.setText(context.getString(R.string.sr_device_status, "Connected"));
            connect.setText(context.getString(R.string.sr_disconnect));
        } else {
            deviceStatus.setText(context.getString(R.string.sr_device_status, "Disconnected"));
            connect.setText(context.getString(R.string.sr_connect));
        }
    }


    /**
     * Connect to master device
     *
     * @param view Current view
     */
    public void clickSRconnect(View view) {
        if (!isConnect) {
            if (mfreeSpace.getText().toString().isEmpty()) {
                Toast.makeText(this, R.string.sr_err_maxspace, Toast.LENGTH_SHORT).show();
                return;
            }
            if (Long.parseLong(mfreeSpace.getText().toString()) == 0 || Long.parseLong(mfreeSpace.getText().toString()) > SlaveDeviceService.freeSpace) {
                Toast.makeText(this, R.string.sr_err_maxspace, Toast.LENGTH_SHORT).show();

            } else {
                DeviceOperations.displayProgress(ShareResource.this, getString(R.string.p_title), getString(R.string.p_scanning));
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        WifiOperations.startScan();
                    }
                })).start();
            }

        } else {
            // Disconnect
            WifiOperations.setWifiEnabled(false);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        isConnect = false;
        if (wifiScanner != null) {
            unregisterReceiver(wifiScanner);
            wifiScanner = null;
        }
        //NotificationUtils.removeNotification();

        WifiOperations.stop();
        super.onDestroy();
    }

}
