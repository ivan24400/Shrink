package pebble.shrink;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;


public class CompressionService extends Service {

    private static final String TAG = "CompressionUtils";

    /**
     * Starts a local compression or decompression service
     *
     * @param intent  specifies to compress or decompress
     * @param flag    Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     */
    @Override
    public int onStartCommand(final Intent intent, int flag, int startId) {
        Log.d(TAG, "onstartcommand: start");
        if (intent.getAction().equals(CompressionUtils.ACTION_COMPRESS_LOCAL)) {

            Intent tmp = new Intent(CompressionService.this, CompressFile.class);

            NotificationUtils.initNotification(CompressionService.this, tmp);
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

                    final int ret = CompressionUtils.compress(intent.getIntExtra(CompressionUtils.cmethod, 0)
                            , true, intent.getStringExtra(CompressionUtils.cfile));
                    CompressFile.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (ret != 0) {
                                NotificationUtils.updateNotification(getString(R.string.err_cmp_failed));
                            } else {
                                NotificationUtils.updateNotification(getString(R.string.completed));
                            }
                            CompressFile.setWidgetEnabled(true);
                        }
                    });

                    CompressionService.this.stopForeground(false);
                    CompressionService.this.stopSelf();
                }
            })).start();


            Log.d(TAG, "onstartcommand: end");

        } else if (intent.getAction().equals(CompressionUtils.ACTION_DECOMPRESS_LOCAL)) {
            Intent tmp = new Intent(CompressionService.this, Decompressor.class);
            NotificationUtils.initNotification(CompressionService.this, tmp);
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
                        if (CompressionUtils.decompress(intent.getStringExtra(CompressionUtils.cfile)) != 0) {
                            throw new IOException("Decompression Failed");
                        }

                        Decompressor.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationUtils.updateNotification(getString(R.string.completed));
                                Decompressor.setWidgetEnabled(true);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        Decompressor.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                NotificationUtils.updateNotification(getString(R.string.err_dcmp_failed));
                                Decompressor.setWidgetEnabled(true);
                            }
                        });
                    }
                    CompressionService.this.stopForeground(false);
                    CompressionService.this.stopSelf();
                }
            })).start();
        }
        return START_NOT_STICKY;
    }

    /**
     * Not required
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
