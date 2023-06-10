package teamarmada.oxtor.Service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Date;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;
import teamarmada.oxtor.Repository.StorageRepository;

public class FileCleanupWorker extends Worker {

    public static final String TAG=FileCleanupWorker.class.getSimpleName();
    public final String NOTIFICATION_WORK="Share Notification Work";
    private final WorkManager workManager;
    private StatusCode statusCode=null;
    private Context context;

    public FileCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context=context;
        workManager=WorkManager.getInstance(context);
        AuthRepository authRepository=new AuthRepository();
        FirestoreRepository firestoreRepository=FirestoreRepository.getInstance();
        authRepository.getAuth().addAuthStateListener(firebaseAuth -> {
            if(firebaseAuth==null){
                return;
            }
            firestoreRepository.sortByTimestamp(authRepository.getProfileItem())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> queryDocumentSnapshots.getDocuments().forEach(snapshot -> {
                        FileItem fileItem;
                        try{
                            fileItem=snapshot.toObject(FileItem.class);

                        }catch (Exception e){
                            fileItem=new FileItem(snapshot);
                        }
                        FileItem finalFileItem = fileItem;
                        if(shouldDeleteFileFromBucket(fileItem.getTimeStamp())){
                            statusCode=StatusCode.TO_DELETE;
                            makeToast();
                            StorageRepository.getInstance().deleteFile(fileItem, authRepository.getProfileItem())
                                    .addOnSuccessListener(task->{
                                        statusCode=StatusCode.DELETED;
                                        initNotificationWork(finalFileItem.getFileName());
                                    })
                                    .addOnFailureListener(e-> statusCode=StatusCode.IT_FAILED);
                        }
                        else if(shouldDeleteFileFromDatabase(fileItem.getTimeStamp())){
                            firestoreRepository.deleteFile(fileItem,authRepository.getProfileItem())
                                    .addOnSuccessListener(task->{
                                        statusCode=StatusCode.DELETED;
                                        initNotificationWork(finalFileItem.getFileName());
                                    })
                                    .addOnFailureListener(e->statusCode=StatusCode.IT_FAILED);
                        }
                        else{
                            statusCode=StatusCode.NOT_TO_DELETE;
                        }
                    }));
        });
    }

    public void makeToast(){
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(()->Toast.makeText(context,"Some deleted",Toast.LENGTH_SHORT).show());

    }

    public boolean shouldDeleteFileFromBucket(Date uploadDate) {
        long currentTimeMillis = System.currentTimeMillis();
        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;
        long elapsedTimeMillis = currentTimeMillis - uploadDate.getTime();
        return elapsedTimeMillis >= twentyFourHoursInMillis;
    }

    public boolean shouldDeleteFileFromDatabase(Date uploadDate) {
        long currentTimeMillis = System.currentTimeMillis();
        long twentyFourHoursInMillis = 7*24 * 60 * 60 * 1000;
        long elapsedTimeMillis = currentTimeMillis - uploadDate.getTime();
        return elapsedTimeMillis >= twentyFourHoursInMillis;
    }

    public void initNotificationWork(String fileName){
        Constraints constraints=new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest notificationWork= new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .addTag(NOTIFICATION_WORK)
                .setConstraints(constraints)
                .build();
        NotificationWorker.setRemoteMessage("File Deleted",fileName+" has been deleted after 24 hr of uploading");
        workManager.enqueueUniqueWork(NOTIFICATION_WORK,
                ExistingWorkPolicy.APPEND,
                notificationWork);
    }

    @NonNull
    @Override
    public Result doWork() {
        switch(statusCode){
            default:
            case IT_FAILED:return Result.retry();
            case TO_DELETE:
            case DELETED:return Result.success();
        }
    }

    public enum StatusCode {
        TO_DELETE, DELETED, IT_FAILED, NOT_TO_DELETE;
    }

}
