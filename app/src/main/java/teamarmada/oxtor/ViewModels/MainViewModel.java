package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;
import static teamarmada.oxtor.Model.ProfileItem.TO_ENCRYPT;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.CipherInputStream;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Unit;
import teamarmada.oxtor.Livedata.InternetConnectionLiveData;
import teamarmada.oxtor.Livedata.MemoryLiveData;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.FileTask;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AdsRepository;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Repository.FirestoreRepository;
import teamarmada.oxtor.Repository.StorageRepository;
import teamarmada.oxtor.Utils.AES;
import teamarmada.oxtor.Utils.FileItemUtils;

@HiltViewModel
public class MainViewModel extends ViewModel implements OnCompleteListener<Unit> {

    public static final String TAG = MainViewModel.class.getSimpleName();
    public final MutableLiveData<List<FileTask<UploadTask>>> mutableUploadList;
    public final MutableLiveData<List<FileTask<StreamDownloadTask>>> mutableStreamDownloadList;
    public final MutableLiveData<List<FileTask<FileDownloadTask>>> mutableFileDownloadList;
    private final List<FileTask<UploadTask>> uploadList;
    private final List<FileTask<StreamDownloadTask>> streamDownloadList;
    private final List<FileTask<FileDownloadTask>> fileDownloadList;
    private final FirestoreRepository firestoreRepository;
    private final StorageRepository storageRepository;
    private final AdsRepository adsRepository;
    private final MutableLiveData<ProfileItem> profileItem;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final MutableLiveData<Long> usedSpace;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final SharedPreferences sharedPreferences;
    private MemoryLiveData memoryLiveData;
    private InternetConnectionLiveData internetConnectionLiveData;

    @Inject
    public MainViewModel(@ApplicationContext Context context){
        storageRepository = StorageRepository.getInstance();
        adsRepository=new AdsRepository(context);
        firestoreRepository=FirestoreRepository.getInstance();
        AuthRepository authRepository = new AuthRepository();
        profileItem=new MutableLiveData<>(new AuthRepository().getProfileItem());
        uploadList=new ArrayList<>();
        streamDownloadList=new ArrayList<>();
        fileDownloadList=new ArrayList<>();
        mutableUploadList = new MutableLiveData<>(uploadList);
        mutableStreamDownloadList = new MutableLiveData<>(streamDownloadList);
        mutableFileDownloadList = new MutableLiveData<>(fileDownloadList);
        isTaskRunning=new MutableLiveData<>(false);
        sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        usedSpace = new MutableLiveData<>(sharedPreferences.getLong(USED_SPACE, 0L));
    }

    public LiveData<Boolean> getInternetConnectionLiveData(Context context) {
        if(internetConnectionLiveData==null){
            internetConnectionLiveData = new InternetConnectionLiveData(context.getApplicationContext());
        }
        return internetConnectionLiveData;
    }

    public LiveData<Boolean> getMemoryLiveData(Context context) {
        if(memoryLiveData==null) {
            memoryLiveData = new MemoryLiveData(context.getApplicationContext());
        }
        return memoryLiveData;
    }

    public LiveData<ProfileItem> getProfileItem() {
        return profileItem;
    }

    private Task<Unit> uploadUnencryptedFile(FileItem item) {
        setIsTaskRunning(true);
        UploadTask uploadTask = storageRepository.UploadFile(item, getProfileItem().getValue());
        FileTask<UploadTask> fileTask = new FileTask<>(item, uploadTask);
        addUploadItem(fileTask);
        return uploadTask
                .continueWithTask(executor, task -> storageRepository.getDownloadUrl(item, getProfileItem().getValue()))
                .onSuccessTask(executor, task -> firestoreRepository.fetchUsedSpace(getProfileItem().getValue())
                        .continueWith(executor, innerTask -> {
                            setIsTaskRunning(!innerTask.isComplete());
                            sharedPreferences.edit().putLong(USED_SPACE, innerTask.getResult()).apply();
                            return Unit.INSTANCE;
                        }));

    }

    public Task<Unit> uploadUsingInputStream(FileItem item, Context context) throws Exception {
        setIsTaskRunning(true);
        boolean b=!sharedPreferences.getBoolean(TO_ENCRYPT, false);
        if(b) return uploadUnencryptedFile(item);
        Uri uri = Uri.parse(item.getFilePath());
        try (InputStream bufferedInputStream = new CipherInputStream(context.getContentResolver().openInputStream(uri), AES.getEncryptCipher(item, profileItem.getValue()))) {
            UploadTask uploadTask = storageRepository.UploadFile(item, bufferedInputStream, getProfileItem().getValue());
            FileTask<UploadTask> fileTask = new FileTask<>(item, uploadTask);
            addUploadItem(fileTask);
            return uploadTask
                    .continueWithTask(executor, task -> storageRepository.getDownloadUrl(item, getProfileItem().getValue()))
                    .onSuccessTask(executor, task -> firestoreRepository.fetchUsedSpace(getProfileItem().getValue())
                    .continueWith(executor, innerTask -> {
                                setIsTaskRunning(!innerTask.isComplete());
                                sharedPreferences.edit().putLong(USED_SPACE, innerTask.getResult()).apply();
                                return Unit.INSTANCE;
                            }));
        }
    }

    public Task<Unit> uploadUsingByteArray(FileItem item,Context context) throws Exception {
        setIsTaskRunning(true);
        Uri uri=Uri.parse(item.getFilePath());
        byte[] buffer=new byte[FileItemUtils.calculateBufferSize(context,item.getFileSize())];
        File file=new File(uri.toString());
        byte[] bytes1;
        try (FileReader fileReader = new FileReader(file);
             BufferedReader bufferedReader = new BufferedReader(fileReader);
             ByteArrayOutputStream outputStream=new ByteArrayOutputStream()){
            int read;
            while ((read = bufferedReader.read()) != -1) {
                outputStream.write(buffer, 0, read);
            }
            bytes1=outputStream.toByteArray();
        }
        catch (Exception e) {
            try(InputStream inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream()) {
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                bytes1=outputStream.toByteArray();
            }
        }
        if(sharedPreferences.getBoolean(TO_ENCRYPT,false))
            bytes1= AES.encrypt(bytes1,item,profileItem.getValue());
        UploadTask uploadTask = storageRepository.UploadFile(item, bytes1, getProfileItem().getValue());
        FileTask<UploadTask> fileTask=new FileTask<>(item,uploadTask);
        addUploadItem(fileTask);
        return uploadTask
                .continueWithTask(executor,task-> storageRepository.getDownloadUrl(item, getProfileItem().getValue()))
                .onSuccessTask(executor, task ->firestoreRepository.fetchUsedSpace(getProfileItem().getValue()))
                .continueWith(executor, task -> {
                    setIsTaskRunning(!task.isComplete());
                    sharedPreferences.edit().putLong(USED_SPACE, task.getResult()).apply();
                    return Unit.INSTANCE;
                });
    }

    private Task<Unit> downloadUnencryptedFile(FileItem fileItem) throws Exception {
        setIsTaskRunning(true);
        File output= FileItemUtils.createDownloadFile(fileItem);
        Uri uri=Uri.parse(output.getAbsolutePath());
        FileDownloadTask fileDownloadTask =storageRepository.downloadFile(fileItem,uri);
        FileTask<FileDownloadTask> fileTask=new FileTask<>(fileItem,fileDownloadTask);
        addFileDownloadItem(fileTask);
        return fileDownloadTask
                .continueWith(executor,task ->{
                    setIsTaskRunning(!task.isComplete());
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> downloadUsingInputStream(FileItem fileItem,Context context) throws Exception {
        setIsTaskRunning(true);
        boolean b=!fileItem.isEncrypted() ;
        if(b)
            return downloadUnencryptedFile(fileItem);
        File output= FileItemUtils.createDownloadFile(fileItem);
        StreamDownloadTask streamDownloadTask =storageRepository.downloadFile(fileItem);
        FileTask<StreamDownloadTask> fileTask=new FileTask<>(fileItem,streamDownloadTask);
        addStreamDownloadItem(fileTask);
        return streamDownloadTask
                .continueWith(executor,task ->{
                    int bufferSize = FileItemUtils.calculateBufferSize(context, fileItem.getFileSize());
                    byte[] buffer = new byte[bufferSize];
                    try (InputStream inputStream = new CipherInputStream(task.getResult().getStream(), AES.getDecryptionCipher(fileItem, profileItem.getValue()));
                         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        fileItem.setFilePath(output.getAbsolutePath());
                        setIsTaskRunning(!task.isComplete());
                    }
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> downloadUsingDownloadManager(FileItem item, Context context) throws Exception {
        TaskCompletionSource<Unit> taskCompletionSource = new TaskCompletionSource<>();

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.getDownloadUrl()));
        request.setTitle("Downloading...")
                .setDescription(item.getFileName())
                .setMimeType(item.getFileExtension())
                .setDestinationUri(Uri.fromFile(FileItemUtils.createDownloadFile(item)))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Enqueue the download request
        long downloadId = downloadManager.enqueue(request);

        // Periodically check the download status
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = cursor.getInt(statusIndex);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        // Download completed successfully
                        taskCompletionSource.setResult(Unit.INSTANCE);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        // Download failed
                        taskCompletionSource.setException(new Exception("Download failed"));
                    }
                    cursor.close();
                }

                if (!taskCompletionSource.getTask().isComplete()) {
                    // Schedule the next check
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(runnable);
        return taskCompletionSource.getTask();
    }

    public InterstitialAd getInterstitialAd(){
        return adsRepository.getInterstitialAd();
    }

    public void setInterstitialAd(InterstitialAd ad){
        adsRepository.setInterstitialAd(ad);
    }

    public void loadInterstitialAd() {
        adsRepository.loadInterstitialAd();
    }

    public void loadAppOpenAd() {
        adsRepository.loadAppOpenAd();
    }

    public void loadAdView(AdSize adSize) {
        adsRepository.loadBannerAd(adSize);
    }

    public AppOpenAd getAppOpenAd(){
        return adsRepository.getAppOpenAd();
    }

    public void setAppOpenAd(AppOpenAd ad){
        adsRepository.setAppOpenAd(ad);
    }

    public AdView getBannerAd() {
        return adsRepository.getBannerAd();
    }

    public LiveData<Long> getUsedSpace() {
        return usedSpace;
    }

    public MutableLiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }

    public void setIsTaskRunning(boolean isTaskRunning){
        this.isTaskRunning.postValue(isTaskRunning);
    }

    public void addUploadItem(FileTask<UploadTask> fileTask){
        uploadList.add(fileTask);
        try {
            mutableUploadList.setValue(uploadList);
        }catch (Exception e){
            mutableUploadList.postValue(uploadList);
        }
    }

    public void removeUploadItem(FileTask<UploadTask> fileTask){
        uploadList.remove(fileTask);
        try {
            mutableUploadList.setValue(uploadList);
        }catch (Exception e){
            mutableUploadList.postValue(uploadList);
        }
    }

    public void addStreamDownloadItem(FileTask<StreamDownloadTask> fileTask){
        streamDownloadList.add(fileTask);
        try{
            mutableStreamDownloadList.setValue(streamDownloadList);
        }catch (Exception e){
            mutableStreamDownloadList.postValue(streamDownloadList);
        }
    }

    public void removeStreamDownloadItem(FileTask<StreamDownloadTask> fileTask){
        streamDownloadList.remove(fileTask);
        try{
            mutableStreamDownloadList.setValue(streamDownloadList);
        }catch (Exception e){
            mutableStreamDownloadList.postValue(streamDownloadList);
        }
    }

    public void addFileDownloadItem(FileTask<FileDownloadTask> fileTask){
        fileDownloadList.add(fileTask);
        try{
            mutableFileDownloadList.setValue(fileDownloadList);
        }catch (Exception e){
            mutableFileDownloadList.postValue(fileDownloadList);
        }
    }

    public void removeFileDownloadItem(FileTask<FileDownloadTask> fileTask){
        fileDownloadList.remove(fileTask);
        try{
            mutableFileDownloadList.setValue(fileDownloadList);
        }catch (Exception e){
            mutableFileDownloadList.postValue(fileDownloadList);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
    }

}
