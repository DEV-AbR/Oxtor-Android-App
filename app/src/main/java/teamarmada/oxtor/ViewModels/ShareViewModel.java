package teamarmada.oxtor.ViewModels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Model.SharedItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;

@HiltViewModel
public class ShareViewModel extends ViewModel implements OnCompleteListener<HttpsCallableResult> {
    public static final String TAG=ShareViewModel.class.getSimpleName();
    private final FirestoreRepository firestoreRepository;
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final MutableLiveData<ProfileItem> profileItem;
    private final Executor executor= Executors.newCachedThreadPool();

    @Inject
    public ShareViewModel() {
        this.firestoreRepository = FirestoreRepository.getInstance();
        this.authRepository = new AuthRepository();
        this.profileItem=new MutableLiveData<>(authRepository.getProfileItem());
        isTaskRunning = new MutableLiveData<>(false);
    }

    public void deleteExpiredPosts(){
        Calendar c=Calendar.getInstance();
        firestoreRepository.sortSharedPostByTimestamp(authRepository.getProfileItem())
                .get().addOnCompleteListener(task->{
                    if(task.isSuccessful())
                        task.getResult().getDocuments()
                                .forEach(documentSnapshot -> {
                                    Date exp=documentSnapshot.get("expiryDate",Date.class);
                                    Log.d(TAG, "deleteExpiredPosts: "+exp);
                                     if(documentSnapshot.get("readable",Boolean.class)
                                             &&
                                        documentSnapshot.get("emailOfReceiver",String.class)
                                                .equals(authRepository.getProfileItem().getEmail())
                                            &&
                                        documentSnapshot.get("expiryDate",Date.class)
                                                .compareTo(c.getTime())<0) {
                                         SharedItem sharedItem=new SharedItem();
                                         sharedItem.setUid(documentSnapshot.get("uid",String.class));
                                         deleteSharedPosts(sharedItem);
                                     }
                                });
                });
    }

    public void deleteSharedPosts(SharedItem item) {
        setIsTaskRunning(true);
        firestoreRepository.deleteSharedPost(item, profileItem.getValue())
                .addOnCompleteListener(executor,task->{
                    setIsTaskRunning(!task.isComplete());
                    if(!task.isSuccessful()) Log.e(TAG, "deleteSharedPosts: ", task.getException() );
                });
    }

    public LiveData<ProfileItem> getProfileItem() {
        return profileItem;
    }

    public LiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }

    public void setIsTaskRunning(boolean isItRunning){
        isTaskRunning.postValue(isItRunning);
    }

    @Override
    public void onComplete(@NonNull Task<HttpsCallableResult> task) {
        setIsTaskRunning(!task.isComplete());
    }

    public Query queryToSortSharedItemByTimestamp() {
        return firestoreRepository.sortSharedPostByTimestamp(profileItem.getValue());
    }

    public Query queryToSortSharedItemBySize() {
        return firestoreRepository.sortSharedPostBySize(profileItem.getValue());
    }

    public Query queryToSortSharedItemByName() {
        return firestoreRepository.sortSharedPostByName(profileItem.getValue());
    }

}
