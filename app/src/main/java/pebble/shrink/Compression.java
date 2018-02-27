package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;


public class Compression extends Service {

    @Override
    public int onStartCommand(Intent intent,int flag,int startId){

        if(intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_WRITE_HEADER)){

        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
