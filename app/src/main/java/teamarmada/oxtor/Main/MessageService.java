package teamarmada.oxtor.Main;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;


public class MessageService extends FirebaseMessagingService implements OnCompleteListener<Void>, FirebaseAuth.AuthStateListener {

    public static final String TAG = MessageService.class.getSimpleName();
    public final String NOTIFICATION_WORK="Share Notification Work";
    private final AuthRepository authRepository;
    private final FirestoreRepository firestoreRepository;
    private ProfileItem profileItem;

    public MessageService(){
        authRepository=new AuthRepository();
        firestoreRepository=FirestoreRepository.getInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        authRepository.getAuth().addAuthStateListener(this);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        if(firebaseAuth.getCurrentUser()==null){
            WorkManager.getInstance(this).cancelUniqueWork(NOTIFICATION_WORK);
        }
        else {
            profileItem=new ProfileItem(firebaseAuth.getCurrentUser());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        if (profileItem != null){
            firestoreRepository.updateMessageToken(token, authRepository.getProfileItem())
                    .addOnCompleteListener(this);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            NotificationWorker.setRemoteMessage(remoteMessage);
            scheduleJob();
        }
    }

    private void scheduleJob() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .addTag(NOTIFICATION_WORK)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(this).beginWith(work).enqueue();
    }

}