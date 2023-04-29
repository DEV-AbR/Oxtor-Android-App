package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class HomeViewModel extends ViewModel implements OnCompleteListener<Unit> {
    public static final String TAG = HomeViewModel.class.getSimpleName();
    private final FirestoreRepository firestoreRepository;
    private final StorageRepository storageRepository;
    private final FunctionsRepository functionsRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final Executor executor = Executors.newCachedThreadPool();
    private final MutableLiveData<ProfileItem> profileItem;
    private final SharedPreferences sharedPreferences;

    @Inject
    public HomeViewModel(@ApplicationContext Context context) {
        this.storageRepository = StorageRepository.getInstance();
        this.firestoreRepository = FirestoreRepository.getInstance();
        this.functionsRepository = FunctionsRepository.getInstance();
        this.authRepository = new AuthRepository();
        isTaskRunning = new MutableLiveData<>(false);
        profileItem=new MutableLiveData<>(authRepository.getProfileItem());
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void renameFile(String s, FileItem fileItem) {
        setIsTaskRunning(true);
        storageRepository.RenameFile(s, fileItem, profileItem.getValue())
                .continueWith(executor,task->Unit.INSTANCE)
                .addOnCompleteListener(executor,this);
    }

    public void deleteFile(FileItem fileItem) {
        setIsTaskRunning(true);
        storageRepository.deleteFile(fileItem, profileItem.getValue())
                .onSuccessTask(executor, task -> firestoreRepository.fetchUsedSpace(profileItem.getValue()))
                .continueWith(executor, task -> {
                    sharedPreferences.edit().putLong(USED_SPACE, task.getResult()).apply();
                    return Unit.INSTANCE;
                    })
                .addOnCompleteListener(executor, this);
    }

    public Task<HttpsCallableResult> shareFile(@NonNull List<FileItem> fileItems,
                                               @NonNull String senderUsername,
                                               @NonNull String receiverUsername) throws Exception {
        setIsTaskRunning(true);
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("senderUsername",senderUsername);
        jsonObject.put("receiverUsername",receiverUsername);
        JSONArray jsonArray=new JSONArray();
        for (int i = 0; i < fileItems.size(); i++) {
            jsonArray.put(fileItems.get(i));
        }
        jsonObject.put("fileItems",jsonArray);
        return functionsRepository.shareByEmail(jsonObject).continueWithTask(task -> {
            setIsTaskRunning(!task.isComplete());
            return task;
        });
    }

    public void checkUsername() {
        setIsTaskRunning(true);
        firestoreRepository.fetchUsername(profileItem.getValue())
                .continueWith(executor,task -> {
                    if(task.isSuccessful()){
                        final ProfileItem profileItem1=profileItem.getValue();
                        profileItem1.setUsername(task.getResult());
                        profileItem.setValue(profileItem1);
                    }
                    return Unit.INSTANCE;
                })
                .addOnCompleteListener(executor,this);
    }

    public Task<String> fetchUsername() {
        return firestoreRepository.fetchUsername(profileItem.getValue());
    }
    
    public LiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }

    public void setIsTaskRunning(boolean isItRunning) {
        isTaskRunning.postValue(isItRunning);
    }

    public Query queryToSortByTimestamp() {
        return firestoreRepository.sortByTimestamp(profileItem.getValue());
    }

    public Query queryToSortBySize() {
        return firestoreRepository.sortBySize(profileItem.getValue());
    }

    public Query queryToSortByName() {
        return firestoreRepository.sortByName(profileItem.getValue());
    }

    public LiveData<ProfileItem> getProfileItem() {
        if(profileItem.getValue()==null)
            try {
                profileItem.setValue(authRepository.getProfileItem());
            }catch (Exception e){
                profileItem.postValue(authRepository.getProfileItem());
            }
        return profileItem;
    }

    @Override
    public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
    }

}
