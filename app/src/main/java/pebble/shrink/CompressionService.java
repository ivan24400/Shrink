package pebble.shrink;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


public class CompressionService extends Service {

    private static final String TAG = "CompressionUtils";
    @Override
    public int onStartCommand(Intent intent,int flag,int startId){
        Log.d(TAG,"onstartcommand: start");
        if(intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_LOCAL)){
            NotificationUtil.startNotification(this,CompressFile.class,getString(R.string.compressing));

            CompressionUtils.writeHeader(intent.getIntExtra(CompressionUtils.cmethod,0)
                    ,intent.getStringExtra(CompressionUtils.cfile));

            CompressionUtils.compress(intent.getIntExtra(CompressionUtils.cmethod,0)
                    ,intent.getStringExtra(CompressionUtils.cfile));

            //NotificationUtil.stopNotification();
            stopSelf();
            Log.d(TAG,"onstartcommand: end");

        }else if(intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_WRITE_HEADER)) {

        }else if(intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_REMOTE)){

        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
