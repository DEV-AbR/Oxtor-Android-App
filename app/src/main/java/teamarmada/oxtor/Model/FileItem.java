package teamarmada.oxtor.Model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class FileItem implements Serializable {

    private String storageReference;
    private String downloadUrl;
    private String filePath;
    private String fileName;
    private String uid;
    private String fileType;
    private String fileExtension;
    private long fileSize;
    private @ServerTimestamp Date timeStamp;

    public static final String STORAGE_REFERENCE="storageReference";
    public static final String DOWNLOAD_URL="downloadUrl";
    public static final String FILE_EXTENSION="fileExtension";
    public static final String TIMESTAMP="timeStamp";
    public static final String FILETYPE="fileType";
    public static final String FILENAME="fileName";
    public static final String FILEPATH="filePath";
    public static final String FILE_SIZE="fileSize";
    public static final String UID="uid";


    public FileItem(){}

    public FileItem(DocumentSnapshot documentSnapshot) {
        this.uid = documentSnapshot.getString(UID);
        this.filePath = documentSnapshot.getString(FILEPATH);
        this.fileName = documentSnapshot.getString(FILENAME);
        this.fileType = documentSnapshot.getString(FILETYPE);
        this.timeStamp = documentSnapshot.getDate(TIMESTAMP);
        this.downloadUrl = documentSnapshot.getString(DOWNLOAD_URL);
        this.fileExtension = documentSnapshot.getString(FILE_EXTENSION);
        this.storageReference = documentSnapshot.getString(STORAGE_REFERENCE);
        try{
            this.fileSize = documentSnapshot.getLong(FILE_SIZE);
        }catch(Exception e){
            this.fileSize=0;
        }

    }

    public FileItem(String storageReference, String downloadUrl, String filePath,
                    String fileName, String uid, String fileType, String fileExtension,
                    Long fileSize, Date timeStamp) {
        this.storageReference = storageReference;
        this.fileExtension = fileExtension;
        this.downloadUrl = downloadUrl;

        this.timeStamp = timeStamp;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uid = uid;

    }

    public String getStorageReference() {
        return storageReference;
    }

    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
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

        hashMap.put(TIMESTAMP,timeStamp);
        return hashMap;
    }

}
