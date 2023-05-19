package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
import teamarmada.oxtor.Utils.FileItemUtils;

@HiltViewModel
public class MainViewModel extends ViewModel implements OnCompleteListener<Unit> {

    public static final String TAG = MainViewModel.class.getSimpleName();
    public final MutableLiveData<List<FileTask<UploadTask>>> mutableUploadList;
    public final MutableLiveData<List<FileTask<StreamDownloadTask>>> mutableDownloadList;
    private final List<FileTask<UploadTask>> uploadList;
    private final List<FileTask<StreamDownloadTask>> downloadList;
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
        downloadList=new ArrayList<>();
        mutableUploadList = new MutableLiveData<>(uploadList);
        mutableDownloadList = new MutableLiveData<>(downloadList);
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
    
    public Task<Unit> uploadUsingInputStream(Context context,FileItem item) throws Exception {
        setIsTaskRunning(true);
        InputStream inputStream= FileItemUtils.uploadFromInputStream(item,getProfileItem().getValue(), context);
        UploadTask uploadTask = storageRepository.UploadFile(item, inputStream, getProfileItem().getValue());
        FileTask<UploadTask> fileTask=new FileTask<>(item,uploadTask);
        addUploadItem(fileTask);
        return fileTask.getTask()
                .continueWithTask(executor,task->{
                    if(task.isComplete())
                        inputStream.close();
                    return storageRepository.getDownloadUrl(item, getProfileItem().getValue());
                })
                .onSuccessTask(executor, task ->firestoreRepository.fetchUsedSpace(getProfileItem().getValue()))
                .continueWith(executor, task -> {
                    setIsTaskRunning(!task.isComplete());
                    sharedPreferences.edit().putLong(USED_SPACE, task.getResult()).apply();
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> uploadUsingByteArray(Context context, FileItem item) throws Exception {
        setIsTaskRunning(true);
        UploadTask uploadTask = storageRepository.UploadFile(item,
                FileItemUtils.readIntoByteArray(item, getProfileItem().getValue(), context),
                getProfileItem().getValue());
        FileTask<UploadTask> fileTask=new FileTask<>(item,uploadTask);
        addUploadItem(fileTask);
        return fileTask.getTask()
                .continueWithTask(executor,task-> storageRepository.getDownloadUrl(item, getProfileItem().getValue()))
                .onSuccessTask(executor, task ->firestoreRepository.fetchUsedSpace(getProfileItem().getValue()))
                .continueWith(executor, task -> {
                    setIsTaskRunning(!task.isComplete());
                    sharedPreferences.edit().putLong(USED_SPACE, task.getResult()).apply();
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> downloadUsingInputStream(Context context,FileItem fileItem) throws Exception {
        setIsTaskRunning(true);
        File output= FileItemUtils.createDownloadFile(fileItem);
        StreamDownloadTask streamDownloadTask =storageRepository.downloadFile(fileItem);
        FileTask<StreamDownloadTask> fileTask=new FileTask<>(fileItem,streamDownloadTask);
        addDownloadItem(fileTask);
        return fileTask.getTask()
                .continueWith(executor,task ->{
                    InputStream inputStream=task.getResult().getStream();
                    inputStream= FileItemUtils.downloadFromInputStream(fileItem, getProfileItem().getValue(), context,inputStream);
                    OutputStream outputStream=context.getContentResolver().openOutputStream(Uri.fromFile(output));
                    byte[] bytes=new byte[fileItem.getFileSize().intValue()];
                    int read;
                    while ((read=inputStream.read(bytes))!=-1){
                        outputStream.write(bytes,0,read);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    fileItem.setFilePath(output.getAbsolutePath());
                    setIsTaskRunning(!task.isComplete());
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> downloadUsingDownloadManager(Context context, FileItem item) throws Exception {
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
                        taskCompletionSource.setResult(null);
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

    public void addDownloadItem(FileTask<StreamDownloadTask> fileTask){
        downloadList.add(fileTask);
        try{
            mutableDownloadList.setValue(downloadList);
        }catch (Exception e){
            mutableDownloadList.postValue(downloadList);
        }
    }

    public void removeDownloadItem(FileTask<StreamDownloadTask> fileTask){
        downloadList.remove(fileTask);
        try{
            mutableDownloadList.setValue(downloadList);
        }catch (Exception e){
            mutableDownloadList.postValue(downloadList);
        }
    }

    @Override
    public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
    }

}
