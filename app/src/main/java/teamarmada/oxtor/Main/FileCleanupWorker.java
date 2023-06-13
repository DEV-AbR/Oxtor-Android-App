package teamarmada.oxtor.Main;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import java.util.Date;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;
import teamarmada.oxtor.Repository.StorageRepository;

public class FileCleanupWorker extends Worker {

    public static final String TAG=FileCleanupWorker.class.getSimpleName();
    Result result=Result.retry();

    public FileCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        AuthRepository authRepository=new AuthRepository();
        FirestoreRepository firestoreRepository=FirestoreRepository.getInstance();
        authRepository.getAuth().addAuthStateListener(firebaseAuth -> {
            if(firebaseAuth==null){
                return;
            }
            firestoreRepository.sortByTimestamp(authRepository.getProfileItem())
                    .continueWithTask(task->task.getResult().get())
                    .addOnSuccessListener(results-> results.getDocuments().forEach(snapshot -> {
                        FileItem fileItem;
                        try{
                            fileItem=snapshot.toObject(FileItem.class);

                        }catch (Exception e){
                            fileItem=new FileItem(snapshot);
                        }
                        FileItem finalFileItem = fileItem;
                        if(shouldDeleteFileFromBucket(fileItem.getTimeStamp())){
                            try {
                                StorageRepository.getInstance().deleteFile(fileItem, authRepository.getProfileItem())
                                        .addOnSuccessListener(task1 -> {
                                            result=Result.success();
                                        })
                                        .addOnFailureListener(e -> result=Result.failure());
                            }catch (Exception e){
                                result=Result.failure();
                            }
                        }
                        else if(shouldDeleteFileFromDatabase(fileItem.getTimeStamp())){
                            firestoreRepository.deleteFile(fileItem,authRepository.getProfileItem())
                                    .addOnSuccessListener(task1->{
                                        result=Result.success();
                                    })
                                    .addOnFailureListener(e->result=Result.failure());
                        }
                        else{
                            result=Result.failure();
                        }
                    }));

        });
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

    @NonNull
    @Override
    public Result doWork() {
        return result;
    }

}
