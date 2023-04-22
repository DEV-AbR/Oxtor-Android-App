package teamarmada.oxtor.Main;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.messaging.RemoteMessage;

import teamarmada.oxtor.R;

public class NotificationWorker extends Worker {

    public static final String SHARE_NOTIFICATION_CHANNEL_ID = "1";
    public static final String SUPPORT_NOTIFICATION_CHANNEL_ID="2";
    NotificationManager notificationManager;
    NotificationChannel shareNotificationChannel;
    NotificationChannel supportNotificationChannel;
    Notification notification;
    PendingIntent pendingIntent;

    private static RemoteMessage remoteMessage=null;

    public static void setRemoteMessage(RemoteMessage remoteMessage) {
        NotificationWorker.remoteMessage = remoteMessage;
    }

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        shareNotificationChannel=new NotificationChannel(SHARE_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.share_notification_channel), NotificationManager.IMPORTANCE_HIGH);
        shareNotificationChannel.setDescription("Share notification channel of Oxtor app");
        supportNotificationChannel=new NotificationChannel(SUPPORT_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.support_notification_channel),NotificationManager.IMPORTANCE_HIGH);
        supportNotificationChannel.setDescription("Support notification channel of oxtor app");
        notificationManager=context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(shareNotificationChannel);
        notificationManager.createNotificationChannel(supportNotificationChannel);
        pendingIntent=new NavDeepLinkBuilder(context)
                .setGraph(R.navigation.main_navigation)
                .addDestination(R.id.navigation_shared)
                .createPendingIntent();
        if("from admin".equals(remoteMessage.getNotification().getTitle()))
            notification=new Notification
                    .Builder(context, SUPPORT_NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(remoteMessage.getNotification().getTitle())
                    .setContentText(remoteMessage.getNotification().getBody())
                    .setCategory(Notification.CATEGORY_ALARM)
                    .build();
        else
            notification=new Notification
                .Builder(context, SHARE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody())
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_ALARM)
                .build();
    }

    @NonNull
    @Override
    public Result doWork() {
        if(remoteMessage.getData()==null)
            return Result.failure();
        notificationManager.notify(1,notification);
        return Result.success();

    }

}
