package teamarmada.oxtor.Model;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;


public class FileTask<T extends StorageTask> {

    public static final String TAG= FileTask.class.getSimpleName();
    private final FileItem fileItem;
    private final T t;
    private Callback callback;
    private Context context;


    public FileTask(FileItem fileItem, T t){
        this.t=t;
        this.fileItem=fileItem;
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    public T getTask() {
        return t;
    }



    public void beginTask(Context context, Callback callback){
        this.context=context;
        this.callback=callback;

        if(t instanceof UploadTask){
            UploadTask uploadTask=(UploadTask) t;
            bindUploadTask(uploadTask);
        }
        if(t instanceof FileDownloadTask){
            FileDownloadTask fileDownloadTask=(FileDownloadTask) t;
            bindDownloadTask(fileDownloadTask);
        }
        if(t instanceof StreamDownloadTask){
            StreamDownloadTask streamDownloadTask=(StreamDownloadTask) t;
            bindStreamDownload(streamDownloadTask);
        }
    }

    public boolean pause() {
       try {
           return t.pause();
       }catch (Exception e){
           Log.e(TAG, "pause: ", e);
           return false;
       }
    }

    public boolean resume(){
        try{
            return t.resume();
        }catch (Exception e){
            Log.e(TAG, "pause: ", e);
            return false;
        }
    }

    public boolean cancel(){
        try{
            return t.cancel();
        }catch (Exception e){
            Log.e(TAG, "pause: ", e);
            return false;
        }
    }

    private void bindUploadTask(UploadTask uploadTask){
        uploadTask
                .addOnProgressListener(snapshot -> {
                    if(uploadTask.isInProgress()) {
                        if(getAvailableMemory(context).lowMemory){
                            makeToast("Task paused due to low memory");
                            pause();
                        }else{
                            makeToast("Task resumed");
                            resume();
                            double d=(100.0*snapshot.getBytesTransferred())/fileItem.getFileSize();
                            callback.onTaskProgress((int) d);
                        }
                }
                })
                .addOnCanceledListener(()->callback.onTaskCancel())
                .addOnSuccessListener(snapshot->callback.onTaskSuccess())
                .addOnFailureListener(callback::onTaskFailed);
    }

    private void bindDownloadTask(FileDownloadTask fileDownloadTask){
        fileDownloadTask
                .addOnProgressListener(snapshot -> {
                    if(fileDownloadTask.isInProgress()) {
                        if(getAvailableMemory(context).lowMemory){

                            pause();
                        }else{
                            resume();
                            double d=(100.0*snapshot.getBytesTransferred())/fileItem.getFileSize();
                            callback.onTaskProgress((int) d);
                        }
                    }
                })
                .addOnCanceledListener(()->callback.onTaskCancel())
                .addOnSuccessListener(snapshot->callback.onTaskSuccess())
                .addOnFailureListener(callback::onTaskFailed);
    }

    private void bindStreamDownload(StreamDownloadTask streamDownloadTask){
        streamDownloadTask
                .addOnProgressListener(snapshot ->{
                    if(streamDownloadTask.isInProgress()) {
                        if(getAvailableMemory(context).lowMemory){
                            pause();
                        }else{
                            resume();
                            double d=(100.0*snapshot.getBytesTransferred())/fileItem.getFileSize();
                            callback.onTaskProgress((int) d);
                        }

                    }
                })
                .addOnCanceledListener(()->callback.onTaskCancel())
                .addOnSuccessListener(snapshot->callback.onTaskSuccess())
                .addOnFailureListener(callback::onTaskFailed);
    }

    private ActivityManager.MemoryInfo getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    private void makeToast(String msg){
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    public interface Callback{
        void onTaskProgress(int progress);
        void onTaskSuccess();
        void onTaskCancel();
        void onTaskFailed(@Nullable Exception ex);
    }

}
