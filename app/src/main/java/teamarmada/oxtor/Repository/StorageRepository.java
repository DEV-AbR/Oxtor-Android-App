package teamarmada.oxtor.Repository;

import static teamarmada.oxtor.Model.FileItem.FILENAME;
import static teamarmada.oxtor.Model.FileItem.UID;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;

public class StorageRepository  {

    public static final String TAG= StorageRepository.class.getSimpleName();
    private static StorageRepository storageRepository=null;
    private final FirestoreRepository firestoreRepository;
    private final FirebaseStorage storage;

    private StorageRepository(){
        storage=FirebaseStorage.getInstance();
        firestoreRepository =FirestoreRepository.getInstance();
    }

    public synchronized static StorageRepository getInstance(){
        if(storageRepository==null)
            storageRepository=new StorageRepository();
        return storageRepository;
    }

    public UploadTask UploadFile(FileItem fileItem, byte[] bytes, ProfileItem profileItem) {
        fileItem.setStorageReference(profileItem.getStorageReference()
                + fileItem.getFileType() +"/" + fileItem.getUid() + "/" + fileItem.getFileName()+"/");
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(fileItem.getFileType())
                .setCustomMetadata(UID, fileItem.getUid()).setCustomMetadata(FILENAME, fileItem.getFileName())
                .setCustomMetadata("uploader_email",profileItem.getEmail()).build();
        return storageReference.putBytes(bytes,metadata);
    }

    public UploadTask UploadFile(FileItem fileItem, InputStream cis, ProfileItem profileItem) {
        fileItem.setStorageReference(profileItem.getStorageReference()
                + fileItem.getFileType() +"/" + fileItem.getUid() + "/" + fileItem.getFileName()+"/");
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(fileItem.getFileType())
                .setCustomMetadata(UID, fileItem.getUid()).setCustomMetadata(FILENAME, fileItem.getFileName())
                .setCustomMetadata("uploader_email",profileItem.getEmail()).build();
        return storageReference.putStream(cis,metadata);
    }

    public UploadTask UploadFile(FileItem fileItem, ProfileItem profileItem) {
        fileItem.setStorageReference(profileItem.getStorageReference()
                + fileItem.getFileType() +"/" + fileItem.getUid() + "/" + fileItem.getFileName()+"/");
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(fileItem.getFileType())
                .setCustomMetadata(UID, fileItem.getUid()).setCustomMetadata(FILENAME, fileItem.getFileName())
                .setCustomMetadata("uploader_email",profileItem.getEmail()).build();
        return storageReference.putFile(Uri.parse(fileItem.getFilePath()),metadata);
    }

    public Task<Void> RenameFile(String s,FileItem fileItem,ProfileItem profileItem){
        StorageMetadata metadata=new StorageMetadata.Builder().setCustomMetadata(FILENAME,s).build();
       return storage.getReference().child(fileItem.getStorageReference()).updateMetadata(metadata)
               .onSuccessTask(task -> {
                   Map<String,Object> map=new HashMap<>();
                   map.put(UID,fileItem.getUid());
                   String s2=s.concat(".").concat(fileItem.getFileExtension());
                   map.put(FILENAME,s2);
                   return firestoreRepository.updateFile(map,profileItem);
               });
    }

    public Task<Void> deleteFileByUrl(String s) {
        return storage.getReferenceFromUrl(s).delete();
    }

    public Task<Void> getDownloadUrl(FileItem fileitem, ProfileItem profileItem) {
        StorageReference storageReference= storage.getReference().child(fileitem.getStorageReference());
        return storageReference.getDownloadUrl().onSuccessTask(task -> {
            fileitem.setDownloadUrl(task.toString());
            return firestoreRepository.createFile(fileitem,profileItem);
        });
    }

    public Task<Void> deleteFile(FileItem fileItem,ProfileItem profileItem){
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        return storageReference.delete().onSuccessTask(task -> {
            FileItem fileItem1=new FileItem(fileItem.getStorageReference(),
                    null, null,
                    fileItem.getFileName(),fileItem.getUid(),
                    fileItem.getFileType(), fileItem.getFileExtension(),
                    fileItem.getFileSize(), fileItem.getTimeStamp());
            return firestoreRepository.updateFile(fileItem1.toHashmap(), profileItem);
        });
    }

    public Task<Void> deleteAllFiles(ProfileItem profileItem){
        StorageReference storageReference=storage.getReference().child(profileItem.getStorageReference());
        return storageReference.listAll().continueWithTask(listResult -> {
                    List<StorageReference> items = listResult.getResult().getItems();
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (StorageReference item : items) {
                        Task<Void> deleteTask = item.delete();
                        deleteTasks.add(deleteTask);
                    }
                    return Tasks.whenAll(deleteTasks);
                });
    }

    public FileDownloadTask downloadFile(FileItem fileItem, Uri uri) {
        StorageReference storageReference=storage.getReference().child(fileItem.getStorageReference());
        return storageReference.getFile(uri);
    }

    public Task<Void> getTaskOfTasks() {
        Task<Void> downloadTasks= Tasks.whenAll(storage.getReference().getActiveDownloadTasks());
        Task<Void> uploadTasks=Tasks.whenAll(storage.getReference().getActiveUploadTasks());
        return Tasks.whenAll(uploadTasks,downloadTasks);
    }

}
