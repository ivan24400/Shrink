package pebble.shrink;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ShareResource extends AppCompatActivity {

    public String TAG = "Share Resource";

    private static TextView deviceName, deviceStatus, freeSpace;
    public static Spinner mpriority;
    private static EditText mfreeSpace;
    public static Button connect;
    public static boolean isConnect = false;

    private WifiScanner wifiScanner;
    private static IntentFilter intentFilter;

    public static Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_resource);

        getSupportActionBar().setTitle(R.string.sr_title);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        WifiOperations.initWifiOperations(ShareResource.this);
        handler = new Handler(Looper.getMainLooper());

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

    public void initDeviceStat() {
        (new Thread(new Runnable() {

            @Override
            public void run() {
                String metaData = DeviceOperations.getDeviceInfo(ShareResource.this);
                final long fs = Long.parseLong(metaData.split("::")[0]);

                SlaveDeviceService.freeSpace = fs;
                SlaveDeviceService.batteryClass = metaData.split("::")[1].charAt(0);

                ShareResource.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(SlaveDeviceService.batteryClass == 'A'){
                            ShareResource.mpriority.setSelection(1);
                        }else{
                            ShareResource.mpriority.setSelection(0);
                        }
                        ShareResource.deviceName.setText(getString(R.string.sr_device_name, Settings.Secure.getString(getContentResolver(), "bluetooth_name")));
                        ShareResource.freeSpace.setText(getString(R.string.sr_freespace, fs));
                        ShareResource.mfreeSpace.setText(Long.toString(fs));
                    }
                });
            }
        })).start();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "on Pause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "on Resume");
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "on stop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "on destroy");
        isConnect = false;
        if (wifiScanner != null) {
            unregisterReceiver(wifiScanner);
            wifiScanner = null;
        }
        NotificationUtils.removeNotification();

        WifiOperations.stop();
        super.onDestroy();
    }

    public static void setConnected(final Context context, final boolean state) {
        isConnect = state;
                if (state) {
                    deviceStatus.setText(context.getString(R.string.sr_device_status,"Connected"));
                    connect.setText(context.getString(R.string.sr_disconnect));
                    mpriority.setEnabled(!state);
                    mfreeSpace.setEnabled(!state);
                } else {
                    deviceStatus.setText(context.getString(R.string.sr_device_status, "Disconnected"));
                    connect.setText(context.getString(R.string.sr_connect));
                    mpriority.setEnabled(state);
                    mfreeSpace.setEnabled(state);
                }
    }

    public void clickSRconnect(View view) {
        Log.d(TAG, "clickconnect " + isConnect);
        if (!isConnect) {
            if (Long.parseLong(mfreeSpace.getText().toString()) > SlaveDeviceService.freeSpace) {
                Toast.makeText(this, R.string.sr_err_maxspace, Toast.LENGTH_LONG).show();
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

}
