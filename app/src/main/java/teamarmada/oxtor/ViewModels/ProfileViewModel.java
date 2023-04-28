package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;
import static teamarmada.oxtor.Model.ProfileItem.TO_ENCRYPT;
import static teamarmada.oxtor.Model.ProfileItem.UID;
import static teamarmada.oxtor.Model.ProfileItem.USERNAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.HttpsCallableResult;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Unit;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;
import teamarmada.oxtor.Repository.FunctionsRepository;
import teamarmada.oxtor.Repository.StorageRepository;


@HiltViewModel
public class ProfileViewModel extends ViewModel implements OnCompleteListener<Unit> {

    private static final String TAG = ProfileViewModel.class.getSimpleName();
    private final AuthRepository authRepository;
    private final StorageRepository storageRepository;
    private final FirestoreRepository firestoreRepository;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final Executor executor= Executors.newCachedThreadPool();
    private final FunctionsRepository functionsRepository;
    private final MutableLiveData<ProfileItem> profileItem;
    private final SharedPreferences sharedPreferences;

    @Inject
    public ProfileViewModel(@ApplicationContext Context context){
        this.authRepository = new AuthRepository();
        this.storageRepository = StorageRepository.getInstance();
        this.firestoreRepository = FirestoreRepository.getInstance();
        this.functionsRepository= FunctionsRepository.getInstance();
        isTaskRunning=new MutableLiveData<>(false);
        profileItem=new MutableLiveData<>(authRepository.getProfileItem());
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void checkUsername(){
        setIsTaskRunning(true);
        firestoreRepository.fetchUsername(profileItem.getValue())
                .continueWith(executor,task -> {
                    final ProfileItem profileItem=getProfileItem().getValue();
                    profileItem.setUsername(task.getResult());
                    getProfileItem().postValue(profileItem);
                    return Unit.INSTANCE;
                }).addOnCompleteListener(this);
    }

    public Task<HttpsCallableResult> updateUsername(String un) {
        setIsTaskRunning(true);
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put(UID, profileItem.getValue().getUid());
            jsonObject.put(USERNAME, un);
        }catch (Exception e){
            e.printStackTrace();
        }
        return functionsRepository.updateUsername(jsonObject).continueWithTask(task->{
            //firestoreRepository.logToDB(jsonObject);
            setIsTaskRunning(!task.isComplete());
            return task;
        });
    }
    
    public void updateEncryptionSetting(boolean b1) {
        sharedPreferences.edit().putBoolean(TO_ENCRYPT,b1).apply();
    }
    
    public MutableLiveData<ProfileItem> getProfileItem() {
        return profileItem;
    }

    public void fetchUsedSpace(){
        firestoreRepository.fetchUsedSpace(profileItem.getValue())
                .addOnSuccessListener(aLong -> sharedPreferences.edit().putLong(USED_SPACE,aLong).apply());
    }

    private void uploadDisplayPicture(FileItem fileItem,byte[] bytes) {
        setIsTaskRunning(true);
        storageRepository.UploadFile(fileItem,bytes ,profileItem.getValue())
                .getTask()
                .onSuccessTask(executor, task -> {
                    String s=task.getMetadata().getReference().toString();
                    fileItem.setStorageReference(s);
                    return task.getMetadata().getReference().getDownloadUrl();
                })
                .onSuccessTask(executor, task -> {
                    try {
                        final ProfileItem profileItem = getProfileItem().getValue();
                        profileItem.setPhotoUrl(task.toString());
                        getProfileItem().postValue(profileItem);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return authRepository.UpdateProfileDisplayPicture(task);
                })
                .continueWith(executor,task->Unit.INSTANCE)
                .addOnCompleteListener(this);
    }

    public void updateDisplayPicture(FileItem fileItem,byte[] bytes) {
        String s= profileItem.getValue().getPhotoUrl();
        boolean b=s.contains("https://firebasestorage.googleapis.com");
        Log.d(TAG, "updateDisplayPicture: picture found in bucket : "+b);
        if(b) {
            setIsTaskRunning(true);
            storageRepository.deleteFileByUrl(s)
                    .addOnSuccessListener(executor, unused -> uploadDisplayPicture(fileItem,bytes))
                    .addOnFailureListener(Throwable::printStackTrace);
        }
        else uploadDisplayPicture(fileItem,bytes);
    }

    public void updateDisplayName(String toString){
        setIsTaskRunning(true);
        authRepository.UpdateProfileDisplayName(toString)
                .continueWith(executor,task -> {
                    try {
                        final ProfileItem profileItem = getProfileItem().getValue();
                        profileItem.setDisplayName(toString);
                        getProfileItem().postValue(profileItem);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return Unit.INSTANCE;})
                .addOnCompleteListener(this);
    }

    public Task<Unit> deleteAccount(){
        setIsTaskRunning(true);
        return authRepository.getUser()
                .delete()
                .continueWith(task->Unit.INSTANCE);
    }

    public Task<Unit> signOut() {
        setIsTaskRunning(true);
        return firestoreRepository.removeMessageToken(profileItem.getValue())
                .continueWith(task -> {
                    setIsTaskRunning(!task.isComplete());
                    getAuthInstance().signOut();
                    return Unit.INSTANCE;
                });
    }

    public FirebaseAuth getAuthInstance(){
        return authRepository.getAuth();
    }

    public LiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }

    public void setIsTaskRunning(boolean isItRunning) {
        this.isTaskRunning.postValue(isItRunning);
    }

    public void abortAllTasks() {
        storageRepository.abortAllTasks();
    }

   @Override
   public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
   }

   
}
