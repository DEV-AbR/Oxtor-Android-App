package teamarmada.oxtor.Repository;


import static teamarmada.oxtor.Model.FileItem.DOWNLOAD_URL;
import static teamarmada.oxtor.Model.FileItem.FILENAME;
import static teamarmada.oxtor.Model.FileItem.FILETYPE;
import static teamarmada.oxtor.Model.FileItem.FILE_SIZE;
import static teamarmada.oxtor.Model.FileItem.TIMESTAMP;
import static teamarmada.oxtor.Model.FileItem.UID;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.Map;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;



public class FirestoreRepository {

    public static final String TAG= FirestoreRepository.class.getSimpleName();
    private static FirestoreRepository firestoreRepository=null;
    private final FirebaseFirestore db;
    public static final String USERS="users";
    public static final String POSTS="posts";
    private Long usedSpace;


    private FirestoreRepository(){
        FirebaseFirestoreSettings settings=new FirebaseFirestoreSettings
                .Builder().setPersistenceEnabled(true).build();
        db=FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);
    }

    public synchronized static FirestoreRepository getInstance(){
        if(firestoreRepository==null)
            firestoreRepository=new FirestoreRepository();
        return firestoreRepository;
    }

    public Task<Void> createAccount(ProfileItem profileItem){
        DocumentReference documentReference=db.collection(USERS).document(profileItem.getUid());
        return db.runBatch(batch -> batch.set(documentReference,profileItem));
    }

    public Task<Void> updateAccount(Map<String,Object> map) {
        String uid =(String) map.get(UID);
        if(uid==null) {
            return Tasks.forException(new Exception("No UID found"));
        }
        DocumentReference documentReference=db.collection(USERS).document(uid);
        return db.batch().update(documentReference,map).commit();
    }

    public Task<Void> deleteAccount(ProfileItem  profileItem){
        DocumentReference docRef=db.collection(USERS).document(profileItem.getUid());
        DocumentReference postsRef=docRef.collection(POSTS).document();
        return postsRef.delete().onSuccessTask(task->docRef.delete());
    }

    public Task<Void> createFile(FileItem fileItem, ProfileItem profileItem){
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid())
                .collection(POSTS)
                .document(fileItem.getUid());
        return db.batch().set(docRef,fileItem).commit();
    }

    public Task<Void> updateFile(Map<String,Object> map, ProfileItem profileItem) {
        String uid =(String) map.get(UID);
        if(uid==null){
           return Tasks.forException(new Exception("No UID found"));
        }
        DocumentReference docRef=db.collection(USERS).document(profileItem.getUid()).collection(POSTS).document(uid);
        return db.batch().update(docRef,map).commit();
    }

    public Task<Void> deleteFile( FileItem fileItem,  ProfileItem profileItem) {
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid())
                .collection(POSTS)
                .document(fileItem.getUid());
        return  db.batch().delete(docRef).commit();
    }

    public Task<Long> fetchUsedSpace(ProfileItem profileItem) {
        usedSpace=0L;
        return firestoreRepository.sortByTimestamp(profileItem).continueWith(task-> {
            Query query=task.getResult();
            if(query!=null){
                query.get().addOnSuccessListener(task1 -> {
                    for(DocumentSnapshot snapshot:task1.getDocuments()){
                        Long g;
                        try{
                            if(snapshot.getString(DOWNLOAD_URL)!=null)
                                g = snapshot.getLong("fileSize");
                            else
                                g=0L;
                        }catch(Exception e){
                            g=0L;
                        }
                        usedSpace += g;
                    }
                });
            }
            return usedSpace;
        });
    }

    public Task<Void> clearCache(){
       return db.clearPersistence();
    }

    public Task<Query> sortByTimestamp(ProfileItem profileItem){
        TaskCompletionSource<Query> queryTaskCompletionSource=new TaskCompletionSource<>();
        if(profileItem.getUid()!=null)
            queryTaskCompletionSource.setResult( db.collection(USERS).document(profileItem.getUid())
                    .collection(POSTS).orderBy(TIMESTAMP,Query.Direction.ASCENDING));
        else
            queryTaskCompletionSource.setException(new Exception("No UID found, Invalid session detected"));
        return queryTaskCompletionSource.getTask();
    }

    public Task<Query> sortBySize(ProfileItem profileItem){
        TaskCompletionSource<Query> queryTaskCompletionSource=new TaskCompletionSource<>();
        try {
            queryTaskCompletionSource.setResult(db.collection(USERS).document(profileItem.getUid())
                    .collection(POSTS).orderBy(FILE_SIZE, Query.Direction.DESCENDING));
        }catch (Exception e) {
            queryTaskCompletionSource.setException(e);
        }
        return queryTaskCompletionSource.getTask();
    }

    public Task<Query> sortByName(ProfileItem profileItem){
        TaskCompletionSource<Query> queryTaskCompletionSource=new TaskCompletionSource<>();
        try {
            queryTaskCompletionSource.setResult(db.collection(USERS).document(profileItem.getUid())
                    .collection(POSTS).orderBy(FILENAME, Query.Direction.ASCENDING));
        }catch (Exception e) {
            queryTaskCompletionSource.setException(e);
        }
        return queryTaskCompletionSource.getTask();
    }

    public Query queryDeletedFiles(ProfileItem profileItem) {
        return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS)
                .whereNotEqualTo(DOWNLOAD_URL,null)
                .whereEqualTo(DOWNLOAD_URL,null);
    }

    public Query queryImageFiles(ProfileItem profileItem){
        return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS)
                .whereNotEqualTo(DOWNLOAD_URL,null)
                .whereEqualTo(DOWNLOAD_URL,null);
    }

    public Query queryVideoFiles(ProfileItem profileItem){
        return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS)
                .whereNotEqualTo(DOWNLOAD_URL,null)
                .whereEqualTo(FILETYPE,"video");
    }

    public Query queryAudioFiles(ProfileItem profileItem){
        return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS)
                .whereNotEqualTo(DOWNLOAD_URL,null)
                .whereEqualTo(FILETYPE,"audio");
    }

    public Query queryDocumentFiles(ProfileItem profileItem){
        return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS)
                .whereNotEqualTo(DOWNLOAD_URL,null)
                .whereNotEqualTo(FILETYPE,"video")
                .whereNotEqualTo(FILETYPE,"image")
                .whereNotEqualTo(FILETYPE,"audio");
    }

}
