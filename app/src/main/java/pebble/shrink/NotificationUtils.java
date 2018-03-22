package pebble.shrink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";

    private static NotificationCompat.Builder nbuilder;
    private static NotificationManager nmanager;

    private static Service service;
    private static PendingIntent pendingIntent;

    private static final String CHANNEL_ID = "SHRINK_NOTIFICATION_CHANNEL";

    public static Notification notification;
    public static final int NOTIFICATION_ID = 24;

    public static void startNotification(Service s, Intent nintent, String content) {
        service = s;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        nmanager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        pendingIntent = PendingIntent.getActivity(service, 0, nintent, 0);

        notification = createNotification(content);
        notification.flags |= Notification.FLAG_NO_CLEAR;

        service.startForeground(NOTIFICATION_ID,notification);
    }

    public static void updateNotification(String content) {
        Notification not = createNotification(content);
        if (not != null) {
            nmanager.notify(NOTIFICATION_ID, not);
        }
    }


    public static Notification createNotification(final String content) {

        nbuilder = new NotificationCompat.Builder(service)
                .setChannel(CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher_round))
                .setSmallIcon(R.drawable.ic_stat_notify_status)
                .setContentTitle(service.getResources().getString(R.string.app_name))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false);

        return nbuilder.build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void createNotificationChannel() {
        if (nmanager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            service.getString(R.string.nt_channel),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(
                    service.getString(R.string.nt_channel_desc));

            nmanager.createNotificationChannel(notificationChannel);
        }
    }


    public static void removeNotification() {
        if (nmanager != null) {
            nmanager.cancel(NOTIFICATION_ID);
        }
    }
}
