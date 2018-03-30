package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;


public class CompressionService extends Service {

    private static final String TAG = "CompressionUtils";

    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        Log.d(TAG, "onstartcommand: start");
        if (intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_LOCAL)) {

            Intent tmp = new Intent(CompressionService.this,CompressFile.class);

            NotificationUtils.startNotification(CompressionService.this, tmp);
            CompressFile.setWidgetEnabled(false);

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    CompressionService.this.startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.notification);

                    CompressFile.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(CompressionService.this.getString(R.string.compressing));
                        }
                    });

                    CompressionUtils.writeHeader(intent.getIntExtra(CompressionUtils.cmethod, 0)
                            , intent.getStringExtra(CompressionUtils.cfile));

                    if(CompressionUtils.compress(intent.getIntExtra(CompressionUtils.cmethod, 0)
                            , true,intent.getStringExtra(CompressionUtils.cfile)) != 0){
                        CompressFile.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(CompressionService.this,"Compression failed !",Toast.LENGTH_SHORT);
                            }
                        });
                    };

                    CompressFile.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(getString(R.string.completed));
                            CompressFile.setWidgetEnabled(true);
                        }
                    });

                    stopForeground(false);
                    stopSelf();
                }
            })).start();


            Log.d(TAG, "onstartcommand: end");

        }else if(intent.getAction().equals(CompressionUtils.ACTION_DECOMPRESS_LOCAL)){
            Intent tmp = new Intent(CompressionService.this,Decompressor.class);
            NotificationUtils.startNotification(CompressionService.this, tmp);
            Decompressor.setWidgetEnabled(false);

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    CompressionService.this.startForeground(NotificationUtils.NOTIFICATION_ID, NotificationUtils.notification);

                    Decompressor.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(CompressionService.this.getString(R.string.decompressing));
                        }
                    });

                    try {
                        if(CompressionUtils.decompress(intent.getStringExtra(CompressionUtils.cfile)) != 0){
                            throw new IOException("Decompression Failed");
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    Decompressor.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            NotificationUtils.updateNotification(getString(R.string.completed));
                            Decompressor.setWidgetEnabled(true);
                        }
                    });

                    stopForeground(false);
                    stopSelf();
                }
            })).start();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
