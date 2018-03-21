package pebble.shrink;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class DeviceOperations {

    public static final int batteryAlowerLimit = 61;
    public static final int batteryBlowerLimit = 31;

    private static final String TAG = "DeviceOperations";

    public static ProgressDialog progress;


    public static String getDeviceInfo(Context context) {

        // Free space in Bytes

        long freeSpace = 0,totalSpace = 0;
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpace = statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
            totalSpace = statFs.getBlockSizeLong() * statFs.getBlockCountLong();
        } else {
            freeSpace = (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
            totalSpace = statFs.getBlockSize() * statFs.getBlockCount();
        }

        // Subtract unavailable minimum storage of android.
        if((freeSpace/2 - (long)0.1*totalSpace) > 0){
            freeSpace = freeSpace/2 - (long)0.1*totalSpace;
        }else {
            freeSpace = freeSpace/2;
        }
        //  Battery
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float batteryPercent = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) /
                (float) batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (int) (batteryPercent * 100);

        char batteryClass;
        if (batteryPercent > batteryAlowerLimit) {
            batteryClass = 'A';
        } else if (batteryPercent > batteryBlowerLimit && batteryPercent < batteryAlowerLimit) {
            batteryClass = 'B';
        } else {
            batteryClass = 'C';
        }

        Log.d(TAG, "Battery: " + batteryPercent + ", Battery Class " + batteryClass + ", FreeSpace: " + freeSpace);
        return Long.toString(freeSpace) + "::" + Character.toString(batteryClass);
    }

    public static void displayProgress(Context c, String title, String msg) {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        progress = progress.show(c, title, msg, true,
                true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
                });
    }


    public static void removeProgress() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }
}