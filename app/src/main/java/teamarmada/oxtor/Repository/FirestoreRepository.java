package teamarmada.oxtor.Repository;

import static teamarmada.oxtor.Model.FileItem.FILENAME;
import static teamarmada.oxtor.Model.FileItem.FILE_SIZE;
import static teamarmada.oxtor.Model.FileItem.TIMESTAMP;
import static teamarmada.oxtor.Model.FileItem.UID;

import android.util.Base64;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firestore.v1.FirestoreGrpc;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Model.SharedItem;


public class FirestoreRepository {

    public static final String TAG= FirestoreRepository.class.getSimpleName();
    private static FirestoreRepository firestoreRepository=null;
    private final FirebaseFirestore db;
    public static final String USERS="users";
    public static final String POSTS="posts";
    public static final String MESSAGING_TOKEN="messagingToken";
    public static final String SHARED_POSTS="shared-posts";
    public static final String SALT="salt";
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
        return FirebaseMessaging.getInstance().getToken()
                .onSuccessTask(task->db.runBatch(batch -> batch.update(documentReference,profileItem.toMap()).update(documentReference,MESSAGING_TOKEN,task)));
    }

    public Task<Void> updateAccount(Map<String,Object> map) throws NullPointerException {
        String uid =(String) map.get(UID);
        if(uid==null) throw new NullPointerException();
        else{
            DocumentReference documentReference=db.collection(USERS).document(uid);
            return db.batch().update(documentReference,map).commit();
        }
    }

    public Task<Void> deleteAccount(ProfileItem  profileItem){
        DocumentReference docRef=db.collection(USERS).document(profileItem.getUid()),
                posts=docRef.collection(POSTS).document(),
                sharedPosts=docRef.collection(SHARED_POSTS).document();
        return db.runBatch(batch -> batch.delete(posts).delete(sharedPosts).delete(docRef));
    }

    public Task<Void> deleteAllFiles(ProfileItem profileItem){
        DocumentReference docRef=db.collection(USERS).document(profileItem.getUid()),
                posts=docRef.collection("posts").document();
        return db.runBatch(batch -> batch.delete(posts));
    }

    public Task<Void> createFile( FileItem fileItem, ProfileItem profileItem){
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid())
                .collection(POSTS)
                .document(fileItem.getUid());
        return db.batch().set(docRef,fileItem).commit();
    }

    public Task<Void> updateFile(Map<String,Object> map, ProfileItem profileItem) throws NullPointerException{
        String uid =(String) map.get(UID);
        if(uid==null)
            throw new NullPointerException("Hashmap does not have any uid");
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid()).collection(POSTS).document(uid);
        return db.batch().update(docRef,map).commit();
    }

    public Task<Void> deleteFile( FileItem fileItem,  ProfileItem profileItem) {
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid())
                .collection(POSTS)
                .document(fileItem.getUid());
        return  db.batch().delete(docRef).commit();
    }

    public Task<Void> updateMessageToken(String token,ProfileItem profileItem){
       return db.collection(USERS).document(profileItem.getUid()).update(MESSAGING_TOKEN,token);
    }

    public Task<Void> removeMessageToken(ProfileItem profileItem) {
        return db.collection(USERS).document(profileItem.getUid()).update(MESSAGING_TOKEN,null);
    }

    public Task<Void> deleteSharedPost(SharedItem sharedItem, ProfileItem profileItem) {
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid())
                .collection(SHARED_POSTS)
                .document(sharedItem.getUid());
        return  db.batch().delete(docRef).commit();
    }

    public Task<Void> updateSharedItem(Map<String,Object> map, ProfileItem profileItem) {
        String uid =(String) map.get(UID);
        if(uid==null) throw new NullPointerException("Hashmap does not have any uid");
        DocumentReference docRef=db.collection(USERS)
                .document(profileItem.getUid()).collection(POSTS).document(uid);
        return db.batch().update(docRef,map).commit();
    }

    public Task<ProfileItem> fetchProfileItem(ProfileItem profileItem) {
        return db.collection(USERS).document(profileItem.getUid()).get()
                .continueWith(task -> task.getResult().toObject(ProfileItem.class));
    }

    public Task<Long> fetchUsedSpace(ProfileItem profileItem) {
        usedSpace=0L;
        Query query= firestoreRepository.sortByTimestamp(profileItem);
        if(query!=null)
            return query.get().continueWith(task -> {
                    for(DocumentSnapshot snapshot:task.getResult()){
                        Long g = snapshot.getLong("fileSize");
                        usedSpace+=g;
                    }
                    return usedSpace;
                });
        else
            return Tasks.forResult(0L);
    }

    public Task<String> fetchUsername(ProfileItem profileItem){
        return fetchProfileItem(profileItem)
                .continueWith(task -> task.getResult().getUsername());
    }

    public Task<byte[]> fetchSALT(ProfileItem item){
        return db.collection(USERS).document(item.getUid()).get()
                .continueWith(task -> {
                    String salt=task.getResult().getString(SALT);
                    byte[] bytes=new byte[16];
                    if(salt==null){
                        (new SecureRandom()).nextBytes(bytes);
                        String sa=Base64.encodeToString(bytes,Base64.DEFAULT);
                        db.collection(USERS).document(item.getUid()).update(SALT,sa);
                    }
                    else bytes=salt.getBytes(StandardCharsets.UTF_8);
                    return bytes;
                });
    }

    public Task<Void> clearCache(){
       return db.clearPersistence();
    }

    public Query sortByTimestamp(ProfileItem profileItem){
        if(profileItem.getUid()!=null)
            return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS).orderBy(TIMESTAMP,Query.Direction.ASCENDING);
        else return null;
    }

    public Query sortBySize(ProfileItem profileItem){
        if(profileItem.getUid()!=null)
            return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS).orderBy(FILE_SIZE, Query.Direction.DESCENDING);
        else return null;
    }

    public Query sortByName(ProfileItem profileItem){
        if(profileItem.getUid()!=null)
            return db.collection(USERS).document(profileItem.getUid())
                .collection(POSTS).orderBy(FILENAME, Query.Direction.ASCENDING);
        else return null;
    }

    public Query sortSharedPostByTimestamp(ProfileItem profileItem) {
        if(profileItem.getUid()!=null)
            return db.collection(USERS).document(profileItem.getUid())
                .collection(SHARED_POSTS).orderBy(TIMESTAMP, Query.Direction.ASCENDING);
        else return null;
    }

    public Query sortSharedPostBySize(ProfileItem profileItem) {
        if(profileItem.getUid()!=null)
            return db.collection(USERS).document(profileItem.getUid())
                .collection(SHARED_POSTS).orderBy(FILE_SIZE, Query.Direction.ASCENDING);
        else return null;
    }

    public Query sortSharedPostByName(ProfileItem profileItem) {
        if(profileItem.getUid()!=null)
        return db.collection(USERS).document(profileItem.getUid())
                .collection(SHARED_POSTS).orderBy(FILENAME, Query.Direction.ASCENDING);
        else return null;
    }
    
}
