package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

/**
 * Created by Ivan on 15-03-2018.
 */

public class DataTransferService extends Service {

    private static final String TAG = "DataTransferService";

    public static final String ACTION_DATA_TRANSFER = "pebble.shrink.DataTransferService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(ACTION_DATA_TRANSFER)) {
            NotificationUtils.startNotification(DataTransferService.this, intent, getString(R.string.initializing));
            CompressFile.btCompress.setEnabled(false);
            (new Thread(new Runnable() {
                @Override
                public void run() {

                }
            })).start();
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
