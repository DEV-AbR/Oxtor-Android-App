package teamarmada.oxtor.Model;

import com.google.firebase.storage.StorageTask;

public class FileTask <T extends StorageTask> {
    private FileItem fileItem;
    private T task;

    public FileTask(FileItem fileItem,T task) {
        this.fileItem=fileItem;
        this.task = task;
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    public T getTask() {
        return task;
    }

}
