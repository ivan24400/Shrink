package pebble.shrink;

import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Ivan on 17-02-2018.
 */

public class CompressFile extends AppCompatActivity {

    public static TextView totalDevice, logView;
    private String TAG = "CompressFile";
    private DeviceGroupListener dg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compress_activity);

        dg = new DeviceGroupListener(CompressFile.this);
        totalDevice = (TextView) findViewById(R.id.tvCFtotalDevices);
        logView = (TextView) findViewById(R.id.tvCFlogView);


        P2pOperations.initNetwork(CompressFile.this);
        P2pOperations.createGroup();

    }

    public void updateThisDevice(WifiP2pDevice dev) {
        P2pOperations.nManager.requestGroupInfo(P2pOperations.nChannel, dg);
    }

    public void resetData() {
        totalDevice.setText(getResources().getString(R.string.cf_total_devices, 0));
    }

    public void onClickCompress(View view) {

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
