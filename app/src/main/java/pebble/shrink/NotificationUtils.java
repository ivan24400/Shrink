package pebble.shrink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

    /**
     * Setup notification
     * @param s foreground service
     * @param nintent activity to resume/start when tapped on notification
     */
    public static void initNotification(Service s, Intent nintent) {
        removeNotification();
        service = s;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        nmanager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        pendingIntent = PendingIntent.getActivity(service, 0, nintent, 0);

        notification = createNotification(service.getString(R.string.initializing));
    }

    /**
     * Create a notification
     * @param content Content of notification
     * @return Notification object
     */
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

    /**
     * Creates a notification channel
     */
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


    public static Context getContext() {
        return service.getBaseContext();
    }

    /**
     * Updates content of notification.
     * @param content text
     */
    public static void updateNotification(String content) {
        if(service == null){
            return;
        }
        Notification not = null;
        if (content.equals(service.getString(R.string.completed))) {
            not = createNotification(content);
        } else {
            not = createNotification(content);
        }
        if (not != null) {
            nmanager.notify(NOTIFICATION_ID, not);
        }
    }

    /**
     * Remove notification
     */
    public static void removeNotification() {
        if (service != null) {
            nmanager.cancel(NOTIFICATION_ID);
            service.stopSelf();
        }
    }

    /**
     * Display a permission error dialog and quit application
     * @param c Current context
     */

    public static void errorDialog(final Context c, final String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(c.getString(R.string.app_name))
                .setMessage(text)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ((AppCompatActivity)c).finish();
                    }
                });
        builder.show();
    }
}
