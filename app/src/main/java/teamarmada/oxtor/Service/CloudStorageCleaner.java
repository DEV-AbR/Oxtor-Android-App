package teamarmada.oxtor.Service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class CloudStorageCleaner extends Service  {

    private static final String FILE_CLEANUP_WORK_TAG = "file_cleanup_work";
    private static final long CLEANUP_INTERVAL = 6 * 60 * 60 * 1000; // 6 hours

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Constraints constraints=new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest fileCleanupWorkRequest = new PeriodicWorkRequest.Builder(FileCleanupWorker.class, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS)
                        .addTag(FILE_CLEANUP_WORK_TAG)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                FILE_CLEANUP_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                fileCleanupWorkRequest);

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
