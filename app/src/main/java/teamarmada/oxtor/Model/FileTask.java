package teamarmada.oxtor.Model;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;


public class FileTask<T extends StorageTask> {

    public static final String TAG= FileTask.class.getSimpleName();
    private final FileItem fileItem;
    private final T t;

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

}
