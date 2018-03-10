package pebble.shrink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class NotificationUtil {

    private static final String TAG = "NotificationUtil";

    private static NotificationCompat.Builder nbuilder;
    private static NotificationManager nmanager;

    private static Service service;
    private static PendingIntent pendingIntent;

    private static boolean sStarted = false;
    private static final String CHANNEL_ID = "SHRINK_NOTIFICATION_CHANNEL";
    private static final int NOTIFICATION_ID = 24;

    public static void startNotification(Service s,Class className, String content){
        service = s;

        Log.d(TAG,"MY SDK version: "+Build.VERSION.SDK_INT +" oreo: "+Build.VERSION_CODES.O);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
        nmanager = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent nintent = new Intent(service,className);
        nintent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(service,0,nintent,0);

        Notification notification = createNotification(content);
        s.startForeground(NOTIFICATION_ID,notification);
        sStarted = true;
    }

    public static void updateNotification(String content){
        Notification not = createNotification(content);
        if(not != null){
            nmanager.notify(NOTIFICATION_ID,not);
        }
    }


    public static Notification createNotification(final String content){

        nbuilder = new NotificationCompat.Builder(service)
                .setChannel(CHANNEL_ID)
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

    public static void stopNotification(){
        if(sStarted){
            sStarted = false;
            nmanager.cancel(NOTIFICATION_ID);
            service.stopForeground(true);
        }
    }
}
