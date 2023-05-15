package teamarmada.oxtor.Model;

import static teamarmada.oxtor.Model.FileItem.DOWNLOAD_URL;
import static teamarmada.oxtor.Model.FileItem.ENCRYPTED;
import static teamarmada.oxtor.Model.FileItem.FILENAME;
import static teamarmada.oxtor.Model.FileItem.FILETYPE;
import static teamarmada.oxtor.Model.FileItem.FILE_EXTENSION;
import static teamarmada.oxtor.Model.FileItem.FILE_SIZE;
import static teamarmada.oxtor.Model.FileItem.IV;
import static teamarmada.oxtor.Model.FileItem.STORAGE_REFERENCE;
import static teamarmada.oxtor.Model.FileItem.TIMESTAMP;
import static teamarmada.oxtor.Model.FileItem.UID;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

public class SharedItem {

    private String emailOfSender;
    private String usernameOfSender;
    private String phoneNumberOfSender;

    private String emailOfReceiver;
    private String usernameOfReceiver;
    private String phoneNumberOfReceiver;

    private FileItem fileItem;

    public SharedItem(){}

    public SharedItem(DocumentSnapshot documentSnapshot) {
        emailOfSender = documentSnapshot.getString("emailOfSender");
        emailOfReceiver = documentSnapshot.getString("emailOfReceiver");
        usernameOfSender = documentSnapshot.getString("usernameOfSender");
        usernameOfReceiver = documentSnapshot.getString("usernameOfReceiver");
        phoneNumberOfSender = documentSnapshot.getString("phoneNumberOfSender");
        phoneNumberOfReceiver = documentSnapshot.getString("phoneNumberOfReceiver");

        String iv=documentSnapshot.getString(IV);
        String uid = documentSnapshot.getString(UID);
        Long timeStamp = documentSnapshot.getLong(TIMESTAMP);
        Long fileSize = documentSnapshot.getLong(FILE_SIZE);
        String fileName = documentSnapshot.getString(FILENAME);
        String fileType = documentSnapshot.getString(FILETYPE);
        String downloadUrl = documentSnapshot.getString(DOWNLOAD_URL);
        String fileExtension = documentSnapshot.getString(FILE_EXTENSION);
        String storageReference = documentSnapshot.getString(STORAGE_REFERENCE);
        Boolean encrypted=documentSnapshot.getBoolean(ENCRYPTED);

        fileItem=new FileItem(storageReference,downloadUrl,null,fileName,uid,
                fileType,fileExtension,fileSize,encrypted,iv,new Date(timeStamp));
    }

    public FileItem getFileItem() {
        return fileItem;
    }

    public String getUsernameOfSender() {
        return usernameOfSender;
    }

    public String getUsernameOfReceiver() {
        return usernameOfReceiver;
    }

    public String getEmailOfSender() {
        return emailOfSender;
    }

    public String getEmailOfReceiver() {
        return emailOfReceiver;
    }


    public String getPhoneNumberOfSender() {
        return phoneNumberOfSender;
    }

    public String getPhoneNumberOfReceiver() {
        return phoneNumberOfReceiver;
    }

}
