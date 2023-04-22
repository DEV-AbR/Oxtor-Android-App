package teamarmada.oxtor.Model;

import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ProfileItem {

    private String displayName,username,email,uid,phoneNumber,photoUrl,storageReference,messagingToken;
//    private boolean toEncrypt;
    @ServerTimestamp
    private Date timeStamp;

    public static final String UID="uid";
    public static final String EMAIL="email";
    public static final String USERNAME="username";
    public static final String PHOTO_URL="photoUrl";
    public static final String TIMESTAMP="timeStamp";
    public static final String DISPLAY_NAME="displayName";
    public static final String PHONE_NUMBER="phoneNumber";
    public static final String MESSAGING_TOKEN="messagingToken";
    public static final String STORAGE_REFERENCE="storageReference";
    public static final String TO_ENCRYPT="toEncrypt";


    public ProfileItem() {}

    public ProfileItem(DocumentSnapshot documentSnapshot) {
        this.uid = documentSnapshot.getString(UID);
        this.email = documentSnapshot.getString(EMAIL);
        this.username = documentSnapshot.getString(USERNAME);
        this.photoUrl = documentSnapshot.getString(PHOTO_URL);
        this.timeStamp = documentSnapshot.getDate(TIMESTAMP);
        this.displayName = documentSnapshot.getString(DISPLAY_NAME);
        this.phoneNumber = documentSnapshot.getString(PHONE_NUMBER);
        this.messagingToken = documentSnapshot.getString(MESSAGING_TOKEN);
        this.storageReference = documentSnapshot.getString(STORAGE_REFERENCE);

    }

    public ProfileItem(FirebaseUser user){
        if(user!=null) {
            uid = user.getUid();
            email = user.getEmail();
            displayName = user.getDisplayName();
            phoneNumber = user.getPhoneNumber();
            storageReference = "users/" + uid + "/";
            if (user.getPhotoUrl() != null)
                photoUrl = user.getPhotoUrl().toString();
            else photoUrl = null;
        }
    }

    public String getStorageReference() {
        return storageReference;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public String getMessagingToken() {
        return messagingToken;
    }

    public void setMessagingToken(String messagingToken) {
        this.messagingToken = messagingToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUid() {
        return uid;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timestamp) {
        this.timeStamp = timestamp;
    }


}
