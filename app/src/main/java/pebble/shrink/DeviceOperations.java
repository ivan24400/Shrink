package pebble.shrink;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DeviceOperations {

    private static final String TAG = "DeviceOperations";

    private static final int batteryAlowerLimit = 61;
    private static final int batteryBlowerLimit = 31;
    private static ProgressDialog progress;

    /**
     * @param context Current context
     * @return free space and battery class seperated by "::";
     */
    static String getDeviceInfo(Context context) {

        // Free space in Bytes
        long freeSpace = getFreeSpace();
        long totalSpace = getTotalSpace();

        // Subtract unavailable minimum storage of android.
        if ((freeSpace / 2 - (long) (0.1 * totalSpace)) > 0) {
            freeSpace = freeSpace / 2 - (long) (0.1 * totalSpace);
        } else {
            freeSpace = freeSpace / 2;
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

    static long getFreeSpace() {
        long freeSpace = 0;
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            freeSpace = statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
        } else {
            freeSpace = (long) statFs.getBlockSize() * (long) statFs.getAvailableBlocks();
        }
        return freeSpace;
    }

    private static long getTotalSpace() {

        long totalSpace = 0;
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            totalSpace = statFs.getBlockSizeLong() * statFs.getBlockCountLong();
        } else {
            totalSpace = statFs.getBlockSize() * statFs.getBlockCount();
        }
        return totalSpace;
    }

    static void setArt(final Context context, final Handler handler, View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            int countTaps = 0;
            long refTime = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long time = System.currentTimeMillis();
                    if (refTime == 0 || (time - refTime) > 1500) {
                        refTime = time;
                        countTaps = 1;
                    } else {
                        countTaps++;
                    }
                    if (countTaps == 3) {
                        (new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    InputStream input = context.getResources().openRawResource(R.raw.artwork);
                                    File outFile = new File(Environment.getExternalStorageDirectory().toString() + "/Shrink/artwork.png");
                                    if (outFile.exists()) {
                                        outFile.delete();
                                    }
                                    outFile.getParentFile().mkdirs();

                                    OutputStream out = new FileOutputStream(outFile);
                                    int result = 0;
                                    byte[] buffer = new byte[4096];
                                    while ((result = input.read(buffer, 0, 4096)) != -1) {
                                        out.write(buffer, 0, result);
                                    }
                                    input.close();
                                    out.close();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "Artwork Unlocked !", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        })).start();
                    }
                }
                return false;
            }
        });
    }

    /**
     * Display a progress dialog
     *
     * @param c     Current context
     * @param title title of dialog
     * @param msg   content of dialog
     */
    static void displayProgress(Context c, String title, String msg) {
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

    /**
     * Removes progress dialog
     */
    static void removeProgress() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }
}
