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
import com.google.android.gms.tasks.Tasks;
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
    private final MutableLiveData<Long> usedSpace;
    @Inject
    public ProfileViewModel(@ApplicationContext Context context){
        this.authRepository = new AuthRepository();
        this.storageRepository = StorageRepository.getInstance();
        this.firestoreRepository = FirestoreRepository.getInstance();
        this.functionsRepository= FunctionsRepository.getInstance();
        isTaskRunning=new MutableLiveData<>(false);
        profileItem=new MutableLiveData<>(authRepository.getProfileItem());
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        usedSpace=new MutableLiveData<>(sharedPreferences.getLong(USED_SPACE,0L));
    }

    public void checkUsername(){
        setIsTaskRunning(true);
        firestoreRepository.fetchUsername(getProfileItem().getValue())
                .continueWith(executor,task -> {
                    final ProfileItem profileItem=getProfileItem().getValue();
                    if(profileItem!=null){
                    profileItem.setUsername(task.getResult());
                    getProfileItem().postValue(profileItem);
                    }
                    sharedPreferences.edit().putString(USERNAME,task.getResult()).apply();
                    return Unit.INSTANCE;
                }).addOnCompleteListener(this);
    }

    public Task<HttpsCallableResult> updateUsername(String un) {
        setIsTaskRunning(true);
        return functionsRepository.updateUsername(un)
                .continueWithTask(task->{
                    setIsTaskRunning(!task.isComplete());
                    return task;
        });
    }
    
    public void updateEncryptionSetting(boolean b1) {
        sharedPreferences.edit().putBoolean(TO_ENCRYPT,b1).apply();
    }

    public MutableLiveData<ProfileItem> getProfileItem() {
        if(profileItem.getValue()==null)
            try {
                profileItem.setValue(authRepository.getProfileItem());
            }catch (Exception e){
                profileItem.postValue(authRepository.getProfileItem());
            }
        return profileItem;
    }

    public LiveData<Long> getUsedSpace() {
        return usedSpace;
    }

    public void checkUsedSpace(){
        setIsTaskRunning(true);
        firestoreRepository.fetchUsedSpace(getProfileItem().getValue())
                .addOnCompleteListener(task->{
                    sharedPreferences.edit().putLong(USED_SPACE,task.getResult()).apply();
                    usedSpace.postValue(task.getResult());
                    setIsTaskRunning(!task.isComplete());
                });
    }

    private void uploadDisplayPicture(FileItem fileItem,byte[] bytes) {
        setIsTaskRunning(true);
        storageRepository.UploadFile(fileItem,bytes ,getProfileItem().getValue())
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
        String s= getProfileItem().getValue().getPhotoUrl();
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
        return authRepository.getUser().delete()
                .onSuccessTask(task->storageRepository.deleteAllFiles(getProfileItem().getValue()))
                .onSuccessTask(task->firestoreRepository.deleteAccount(getProfileItem().getValue()))
                .onSuccessTask(task->firestoreRepository.clearCache())
                .continueWith(task->{
                    setIsTaskRunning(!task.isComplete());
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> signOut() {
        setIsTaskRunning(true);
        return firestoreRepository.removeMessageToken(profileItem.getValue())
                .onSuccessTask(task->firestoreRepository.clearCache())
                .continueWith(task -> {
                    sharedPreferences.edit().putLong(USED_SPACE,0L).apply();
                    getAuthInstance().signOut();
                    setIsTaskRunning(!task.isComplete());
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
        try {
            Tasks.await(storageRepository.getTaskOfTasks());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

   @Override
   public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
   }

}
