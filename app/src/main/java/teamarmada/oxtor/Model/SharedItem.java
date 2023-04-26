package teamarmada.oxtor.Model;

import static teamarmada.oxtor.Model.FileItem.TIMESTAMP;
import static teamarmada.oxtor.Model.FileItem.UID;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Date;

public class SharedItem {

    private String emailOfSender;
    private String usernameOfSender;
    private String phoneNumberOfSender;

    private String emailOfReceiver;
    private String usernameOfReceiver;
    private String phoneNumberOfReceiver;

    private FileItem fileItem;

    private String uid;
    @ServerTimestamp
    private Date timeStamp;

    public SharedItem(){

    }

    public SharedItem(DocumentSnapshot documentSnapshot) {
        emailOfSender = documentSnapshot.getString("emailOfSender");
        emailOfReceiver = documentSnapshot.getString("emailOfReceiver");
        usernameOfSender = documentSnapshot.getString("usernameOfSender");
        usernameOfReceiver = documentSnapshot.getString("usernameOfReceiver");
        phoneNumberOfSender = documentSnapshot.getString("phoneNumberOfSender");
        phoneNumberOfReceiver = documentSnapshot.getString("phoneNumberOfReceiver");
        uid = documentSnapshot.getString(UID);
        timeStamp = documentSnapshot.getDate(TIMESTAMP);
        try {
            JSONObject jsonObject=documentSnapshot.get("fileItem", JSONObject.class);
            fileItem=new FileItem(jsonObject);
        }catch (Exception e){
            fileItem = documentSnapshot.get("fileItem", FileItem.class);
        }
    }


    public String getUsernameOfSender() {
        return usernameOfSender;
    }

    public String getUsernameOfReceiver() {
        return usernameOfReceiver;
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    public void setFileItem(FileItem fileItem) {
        this.fileItem = fileItem;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public String getEmailOfSender() {
        return emailOfSender;
    }

    public void setEmailOfSender(String emailOfSender) {
        this.emailOfSender = emailOfSender;
    }

    public String getEmailOfReceiver() {
        return emailOfReceiver;
    }

    public void setEmailOfReceiver(String emailOfReceiver) {
        this.emailOfReceiver = emailOfReceiver;
    }

    public String getPhoneNumberOfSender() {
        return phoneNumberOfSender;
    }

    public String getPhoneNumberOfReceiver() {
        return phoneNumberOfReceiver;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

}
