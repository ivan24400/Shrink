package pebble.shrink;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public class Notification {

    private static NotificationCompat.Builder nbuilder;
    private static NotificationManagerCompat nmanager;

    private static Context context;
    private static final String CHANNEL_ID = "SHRINK_NOTIFICATION_CHANNEL";
    private static final int NOTIFICATION_ID = 24;

    public static void createNotification(Context contxt,String content){
        context = contxt;
        Intent intent = new Intent(context,context.getClass());
        PendingIntent pintent = PendingIntent.getActivity(context,0,intent,0);

        nbuilder = new NotificationCompat.Builder(context)
                .setChannel(CHANNEL_ID)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pintent)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false);

        nmanager = NotificationManagerCompat.from(context);
        nmanager.notify(NOTIFICATION_ID,nbuilder.build());

    }

    public void updateNotification(String content){
        if(nbuilder != null){
            nbuilder.setContentText(content);
        }
    }

    public void closeNotification(){
        if(nmanager != null){
            nmanager.cancel(NOTIFICATION_ID);
        }
    }
}
