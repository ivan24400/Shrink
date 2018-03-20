package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


public class CompressionService extends Service {

    private static final String TAG = "CompressionUtils";


    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        Log.d(TAG, "onstartcommand: start");
        if (intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_LOCAL)) {

            NotificationUtils.startNotification(CompressionService.this, intent, getString(R.string.compressing));
            CompressFile.btCompress.setEnabled(false);

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    CompressionService.this.startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.notification);
                    CompressionUtils.writeHeader(intent.getIntExtra(CompressionUtils.cmethod, 0)
                            , intent.getStringExtra(CompressionUtils.cfile));

                    CompressionUtils.compress(intent.getIntExtra(CompressionUtils.cmethod, 0)
                            , intent.getStringExtra(CompressionUtils.cfile));

                    CompressFile.cfHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(getString(R.string.completed));
                            CompressFile.btCompress.setEnabled(true);
                        }
                    });
                    stopSelf();
                }
            })).start();


            Log.d(TAG, "onstartcommand: end");

        } else if (intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_REMOTE)) {
            Intent dtIntent = new Intent(this, DataTransferService.class);
            dtIntent.setAction(DataTransferService.ACTION_DATA_TRANSFER);

        }else if (intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_REMOTE_LOCAL)){

        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        NotificationUtils.stopNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
