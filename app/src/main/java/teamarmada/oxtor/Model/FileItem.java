package teamarmada.oxtor.Model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;


public class FileItem {


    private String storageReference;
    private String downloadUrl,filePath;
    private String fileName,uid,fileType;
    private String fileExtension;
    private Long fileSize;
    private boolean encrypted;
    private String iv,encryptionPassword;
    @ServerTimestamp private Date timeStamp;
    public static final String STORAGE_REFERENCE="storageReference";
    public static final String DOWNLOAD_URL="downloadUrl";
    public static final String ENCRYPTED="encrypted";
    public static final String FILEEXTENSION="fileExtension";
    public static final String TIMESTAMP="timeStamp";
    public static final String FILETYPE="fileType";
    public static final String FILENAME="fileName";
    public static final String FILEPATH="filePath";
    public static final String FILESIZE="fileSize";
    public static final String UID="uid";
    public static final String IV="iv";
    public static final String ENCRYPTION_PASSWORD="encryptionPassword";

    public FileItem(){}
    public FileItem(DocumentSnapshot documentSnapshot) {

        this.uid = documentSnapshot.getString(UID);
        this.fileSize = documentSnapshot.getLong(FILESIZE);
        this.filePath = documentSnapshot.getString(FILEPATH);
        this.fileName = documentSnapshot.getString(FILENAME);
        this.fileType = documentSnapshot.getString(FILETYPE);
        this.timeStamp = documentSnapshot.getDate(TIMESTAMP);
        try {
            this.encrypted=documentSnapshot.get(ENCRYPTED,Boolean.class);
        }
        catch (Exception e){
            encrypted=true;
        }
        this.downloadUrl = documentSnapshot.getString(DOWNLOAD_URL);
        this.fileExtension = documentSnapshot.getString(FILEEXTENSION);
        this.storageReference = documentSnapshot.getString(STORAGE_REFERENCE);
        this.iv=documentSnapshot.getString(IV);
        this.encryptionPassword=documentSnapshot.getString(ENCRYPTION_PASSWORD);
    }



    public FileItem(String storageReference,
                    String downloadUrl, String filePath,
                    String fileName, String uid,
                    String fileType, String fileExtension,
                    Long fileSize, boolean encrypted, Date timeStamp) {

        this.storageReference = storageReference;
        this.downloadUrl = downloadUrl;
        this.filePath = filePath;
        this.fileName = fileName;
        this.uid = uid;
        this.fileType = fileType;
        this.fileExtension = fileExtension;
        this.fileSize = fileSize;
        this.encrypted = encrypted;
        this.timeStamp = timeStamp;
    }

    public String getStorageReference() {
        return storageReference;
    }

    public FileItem setStorageReference(String storageReference) {
        this.storageReference = storageReference;
        return this;
    }

    public String getIv() {
        return iv;
    }

    public FileItem setIv(String iv) {
        this.iv = iv;
        return this;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public FileItem setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
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

    @Override
    public String toString() {
        return "FileItem{" +
                "storageReference='" + storageReference + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", uid='" + uid + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileExtension='" + fileExtension + '\'' +
                ", fileSize=" + fileSize +
                ", encrypted=" + encrypted +
                ", timeStamp=" + timeStamp +
                '}';
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public FileItem setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword;
        return this;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public FileItem setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }
}
