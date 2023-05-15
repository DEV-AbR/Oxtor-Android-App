package teamarmada.oxtor.Model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class FileItem {

    private String storageReference;
    private String downloadUrl,filePath;
    private String fileName,uid,fileType;
    private String fileExtension;
    private Long fileSize;
    private Boolean encrypted;
    private String iv,encryptionPassword;
    @ServerTimestamp private Date timeStamp;

    public static final String STORAGE_REFERENCE="storageReference";
    public static final String DOWNLOAD_URL="downloadUrl";
    public static final String ENCRYPTED="encrypted";
    public static final String FILE_EXTENSION="fileExtension";
    public static final String TIMESTAMP="timeStamp";
    public static final String FILETYPE="fileType";
    public static final String FILENAME="fileName";
    public static final String FILEPATH="filePath";
    public static final String FILE_SIZE="fileSize";
    public static final String UID="uid";
    public static final String IV="iv";
    public static final String ENCRYPTION_PASSWORD="encryptionPassword";

    public FileItem(){}

    public FileItem(DocumentSnapshot documentSnapshot) {
        this.iv=documentSnapshot.getString(IV);
        this.uid = documentSnapshot.getString(UID);
        this.fileSize = documentSnapshot.getLong(FILE_SIZE);
        this.filePath = documentSnapshot.getString(FILEPATH);
        this.fileName = documentSnapshot.getString(FILENAME);
        this.fileType = documentSnapshot.getString(FILETYPE);
        this.timeStamp = documentSnapshot.getDate(TIMESTAMP);
        this.downloadUrl = documentSnapshot.getString(DOWNLOAD_URL);
        this.fileExtension = documentSnapshot.getString(FILE_EXTENSION);
        this.storageReference = documentSnapshot.getString(STORAGE_REFERENCE);
        this.encryptionPassword=documentSnapshot.getString(ENCRYPTION_PASSWORD);
        try {
            encrypted=iv!=null?documentSnapshot.get(ENCRYPTED,Boolean.class):false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public FileItem(String storageReference, String downloadUrl, String filePath,
                    String fileName, String uid, String fileType, String fileExtension,
                    Long fileSize, boolean encrypted,String iv, Date timeStamp) {
        this.storageReference = storageReference;
        this.fileExtension = fileExtension;
        this.downloadUrl = downloadUrl;
        this.encrypted = encrypted;
        this.timeStamp = timeStamp;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uid = uid;
        this.iv=iv;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFilePath() {
        return filePath;
    }

    public FileItem setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public FileItem setFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public FileItem setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public FileItem setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public String getFileType() {
        return fileType;
    }

    public FileItem setFileType(String fileType) {
        this.fileType = fileType;
        return this;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public FileItem setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public FileItem setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public void setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public FileItem setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }

    public Map<String,Object> toHashmap() {
        Map<String,Object> hashMap=new HashMap<>();
        hashMap.put(STORAGE_REFERENCE,storageReference);
        hashMap.put(DOWNLOAD_URL,downloadUrl);
        hashMap.put(FILEPATH,filePath);
        hashMap.put(FILENAME,fileName);
        hashMap.put(UID,uid);
        hashMap.put(FILETYPE,fileType);
        hashMap.put(FILE_EXTENSION,fileExtension);
        hashMap.put(FILE_SIZE,fileSize);
        hashMap.put(ENCRYPTED,encrypted);
        hashMap.put(IV,iv);
        hashMap.put(ENCRYPTION_PASSWORD,encryptionPassword);
        hashMap.put(TIMESTAMP,timeStamp);
        return hashMap;
    }

}
