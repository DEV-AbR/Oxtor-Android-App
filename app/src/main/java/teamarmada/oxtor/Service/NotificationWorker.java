package teamarmada.oxtor.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import teamarmada.oxtor.R;

public class NotificationWorker extends Worker {

    public static final String SUPPORT_NOTIFICATION_CHANNEL_ID="1";
    private final NotificationManager notificationManager;
    private final Notification notification;
    private static String title=null;
    private static String body=null;

    public static void setRemoteMessage(String title,String body) {
        NotificationWorker.title=title;
        NotificationWorker.body=body;
    }

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager=context.getSystemService(NotificationManager.class);
        NotificationChannel supportNotificationChannel = new NotificationChannel(SUPPORT_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.support_notification_channel), NotificationManager.IMPORTANCE_HIGH);
        supportNotificationChannel.setDescription("Support notification channel of oxtor app");
        notificationManager.createNotificationChannel(supportNotificationChannel);
        PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                .setGraph(R.navigation.main_navigation)
                .addDestination(R.id.navigation_home)
                .createPendingIntent();
        Icon icon = Icon.createWithResource(context, R.mipmap.ic_launcher);
        notification=new Notification
                .Builder(context, SUPPORT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(icon)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_ALARM)
                .build();
    }


    @NonNull
    @Override
    public Result doWork() {
        if(title==null)
            return Result.failure();
        notificationManager.notify(1,notification);
        title=null;
        body=null;
        return Result.success();
    }

}
