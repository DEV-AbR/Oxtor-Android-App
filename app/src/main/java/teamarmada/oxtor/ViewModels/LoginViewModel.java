package teamarmada.oxtor.ViewModels;

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
import kotlin.Unit;
import teamarmada.oxtor.Repository.AuthRepository;

@HiltViewModel
public class LoginViewModel extends ViewModel implements OnCompleteListener<Unit> {

    public static final String TAG=LoginViewModel.class.getSimpleName();
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final MutableLiveData<FirebaseUser> user;

    @Inject
    public LoginViewModel(){
        this.authRepository = new AuthRepository();
        isTaskRunning=new MutableLiveData<>(false);
        user=new MutableLiveData<>(authRepository.getUser());
    }

    public Task<Unit> signIn(AuthCredential credential){
        setIsTaskRunning(true);
        return authRepository.signIn(credential)
                .continueWith(task->{
                    setIsTaskRunning(!task.isComplete());
                    if(task.isSuccessful()) {
                        try{
                            user.setValue(authRepository.getUser());
                        }catch (Exception e) {
                            user.postValue(authRepository.getUser());
                        }
                    }
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> checkPendingSignIn() {
        Task<AuthResult> authResultTask = authRepository.getAuth().getPendingAuthResult();
        if (authResultTask != null) {
            setIsTaskRunning(true);
            return authResultTask.continueWithTask(task -> {
                if(task.isSuccessful())
                    return signIn(task.getResult().getCredential());
                else
                    return Tasks.forResult(Unit.INSTANCE);
            });
        }
        else
            return Tasks.forException(new Exception("No pending sign in session found"));
    }

    public Task<Unit> getGoogleSignInAccount(Task<GoogleSignInAccount> googleSignInAccountTask) {
        setIsTaskRunning(true);
        return googleSignInAccountTask.continueWith(task->{
            setIsTaskRunning(!task.isComplete());
            GoogleSignInAccount gsa=task.getResult();
            AuthCredential authCredential=GoogleAuthProvider.getCredential(gsa.getIdToken(),null);
            signIn(authCredential);
            return Unit.INSTANCE;
        });
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