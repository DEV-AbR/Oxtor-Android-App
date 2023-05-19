package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;
import static teamarmada.oxtor.Model.ProfileItem.USERNAME;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.storage.FileDownloadTask;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Function;
import kotlin.Unit;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;
import teamarmada.oxtor.Repository.FunctionsRepository;

@HiltViewModel
public class LoginViewModel extends ViewModel implements OnCompleteListener<Unit> {

    public static final String TAG=LoginViewModel.class.getSimpleName();
    private final AuthRepository authRepository;
    private final FirestoreRepository firestoreRepository;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final MutableLiveData<FirebaseUser> user;
    private SharedPreferences sharedPreferences;

    @Inject
    public LoginViewModel(@ApplicationContext Context context){
        this.authRepository = new AuthRepository();
        this.firestoreRepository=FirestoreRepository.getInstance();
        isTaskRunning=new MutableLiveData<>(false);
        user=new MutableLiveData<>(authRepository.getUser());
        sharedPreferences=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }

    public Unit signIn(AuthCredential credential){
        setIsTaskRunning(true);
        authRepository.signIn(credential)
                .continueWithTask(task->{       
                    task.getResult().setUsername(sharedPreferences.getString(USERNAME,null));
                    return firestoreRepository.createAccount(task.getResult());
                })
                .addOnSuccessListener(task->{
                    addMessageToken(authRepository.getProfileItem());
                    try{
                        user.setValue(authRepository.getUser());
                    }catch (Exception e){
                        user.postValue(authRepository.getUser());
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> setIsTaskRunning(!task.isComplete()));
        return Unit.INSTANCE;
    }

    private void addMessageToken(ProfileItem profileItem){
        firestoreRepository.addMessageToken(profileItem).addOnFailureListener(Throwable::printStackTrace);
    }

    public Task<Unit> checkPendingSignIn() {
        Task<AuthResult> authResultTask = authRepository.getAuth().getPendingAuthResult();
        if (authResultTask != null) {
            setIsTaskRunning(true);
            return authResultTask.continueWithTask(task -> {
                if(task.isSuccessful())
                    return Tasks.forResult(signIn(task.getResult().getCredential()));
                else
                    return Tasks.forException(new Exception("Couldn't sign in with credentials found using email and dynamic link"));
            });
        }
        else
            return Tasks.forException(new Exception("No pending sign in session found"));
    }

    public Task<Unit> signInWithEmail(String email,String link){
        setIsTaskRunning(true);
        return getAuthInstance().signInWithEmailLink(email,link)
                .continueWith(task -> {
                    setIsTaskRunning(!task.isComplete());
                    signIn(task.getResult().getCredential());
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
        this.isTaskRunning.setValue(isItRunning);
    }

    @Override
    public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
    }

    public LiveData<FirebaseUser> getUser(){
        return user;
    }

}
