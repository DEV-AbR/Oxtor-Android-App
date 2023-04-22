package teamarmada.oxtor.Repository;

import static teamarmada.oxtor.Model.FileItem.ENCRYPTED;
import static teamarmada.oxtor.Model.FileItem.FILENAME;
import static teamarmada.oxtor.Model.FileItem.UID;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.FileTask;
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

    public FileTask<UploadTask> UploadFile(FileItem fileItem, byte[] bytes, ProfileItem profileItem) {
        fileItem.setStorageReference(profileItem.getStorageReference()
                + fileItem.getFileType() +"/" + fileItem.getUid() + "/" + fileItem.getFileName()+"/");
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(fileItem.getFileType())
                .setCustomMetadata(UID, fileItem.getUid()).setCustomMetadata(FILENAME, fileItem.getFileName())
                .setCustomMetadata(ENCRYPTED,String.valueOf(fileItem.isEncrypted())).build();
        UploadTask uploadTask=storageReference.putBytes(bytes,metadata);
        return new FileTask<>(fileItem,uploadTask);
    }

    public FileTask<UploadTask> UploadFile(FileItem fileItem, InputStream cis, ProfileItem profileItem) {
        fileItem.setStorageReference(profileItem.getStorageReference()
                + fileItem.getFileType() +"/" + fileItem.getUid() + "/" + fileItem.getFileName()+"/");
        StorageReference storageReference= storage.getReference().child(fileItem.getStorageReference());
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType(fileItem.getFileType())
                .setCustomMetadata(UID, fileItem.getUid()).setCustomMetadata(FILENAME, fileItem.getFileName())
                .setCustomMetadata(ENCRYPTED,String.valueOf(fileItem.isEncrypted())).build();
        UploadTask uploadTask=storageReference.putStream(cis,metadata);
        return new FileTask<>(fileItem,uploadTask);
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
        return storageReference.delete().onSuccessTask(task -> firestoreRepository.deleteFile(fileItem, profileItem));
    }

    public Task<Void> deleteAllFiles(ProfileItem profileItem){
        StorageReference storageReference=storage.getReference().child(profileItem.getStorageReference());
        return storageReference.delete();
    }

    public FileTask<StreamDownloadTask> downloadFile(FileItem fileItem) {
        StorageReference storageReference=storage.getReference().child(fileItem.getStorageReference());
        StreamDownloadTask task=storageReference.getStream((state, stream) -> {
            if(state.getBytesTransferred()==state.getTotalByteCount())
                stream.close();
        });
        return new FileTask<>(fileItem,task);
    }

    public void abortAllTasks() {
        try {
            storage.getReference().getActiveUploadTasks().forEach(StorageTask::cancel);
            storage.getReference().getActiveDownloadTasks().forEach(StorageTask::cancel);
        }catch (Exception ignored){}
    }

}
