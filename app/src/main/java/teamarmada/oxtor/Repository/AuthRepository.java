package teamarmada.oxtor.Repository;

import static teamarmada.oxtor.Model.ProfileItem.DISPLAY_NAME;
import static teamarmada.oxtor.Model.ProfileItem.PHOTO_URL;
import static teamarmada.oxtor.Model.ProfileItem.UID;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.HashMap;
import java.util.Map;

import teamarmada.oxtor.Model.ProfileItem;


public class AuthRepository  {

    public static final String TAG= AuthRepository.class.getSimpleName();
    private final FirestoreRepository firestoreRepository;
    private final FirebaseAuth auth;
    private FirebaseUser user;

    public AuthRepository(){
        firestoreRepository=FirestoreRepository.getInstance();
        auth=FirebaseAuth.getInstance();
        user=auth.getCurrentUser();
    }

    public FirebaseUser getUser() {
        return user;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public ProfileItem getProfileItem() {
        return new ProfileItem(user);
    }

    public Task<ProfileItem> signIn(AuthCredential credential){
        return auth.signInWithCredential(credential)
                .onSuccessTask(result->{
                    user=result.getUser();
                    return Tasks.forResult(getProfileItem());
                });
    }

    public Task<Void> UpdateProfileDisplayName(String name){
        UserProfileChangeRequest profileChangeRequest=new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();
       return user.updateProfile(profileChangeRequest).onSuccessTask(task -> {
           Map<String,Object> map=new HashMap<>();
           map.put(UID,user.getUid());
           map.put(DISPLAY_NAME,name);
           return firestoreRepository.updateAccount(map);
       });
    }

    public Task<Void>  UpdateProfileDisplayPicture(Uri pictureUri){
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                .setPhotoUri(pictureUri).build();
        return user.updateProfile(profileChangeRequest).onSuccessTask(task -> {
            Map<String,Object> map=new HashMap<>();
            map.put(UID,user.getUid());
            map.put(PHOTO_URL,pictureUri.toString());
            return firestoreRepository.updateAccount(map);
    });
    }

}
