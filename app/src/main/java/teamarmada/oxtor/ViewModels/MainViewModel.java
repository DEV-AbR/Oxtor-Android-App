package teamarmada.oxtor.ViewModels;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Main.MainActivity.USED_SPACE;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FileDownloadTask;
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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlin.Unit;
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
    public final MutableLiveData<List<FileTask<FileDownloadTask>>> mutableFileDownloadList;
    private final List<FileTask<UploadTask>> uploadList;
    private final List<FileTask<FileDownloadTask>> fileDownloadList;
    private final FirestoreRepository firestoreRepository;
    private final StorageRepository storageRepository;
    private final AdsRepository adsRepository;
    private final MutableLiveData<ProfileItem> profileItem;
    private final MutableLiveData<Boolean> isTaskRunning;
    private final MutableLiveData<Long> usedSpace;
    private final SharedPreferences sharedPreferences;
    private MemoryLiveData memoryLiveData;
    private InternetConnectionLiveData internetConnectionLiveData;
    private final AuthRepository authRepository;
    private final MutableLiveData<FirebaseUser> user;

    @Inject
    public MainViewModel(@ApplicationContext Context context){
        storageRepository = StorageRepository.getInstance();
        authRepository=new AuthRepository();
        user=new MutableLiveData<>(authRepository.getUser());
        profileItem=new MutableLiveData<>(authRepository.getProfileItem());
        adsRepository=new AdsRepository(context);
        firestoreRepository=FirestoreRepository.getInstance();
        uploadList=new ArrayList<>();
        fileDownloadList=new ArrayList<>();
        mutableUploadList = new MutableLiveData<>(uploadList);
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

    public Task<Unit> uploadFile(FileItem item) {
        setIsTaskRunning(true);
        Executor executor=Executors.newSingleThreadExecutor();
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

    public Task<Unit> downloadFile(FileItem fileItem) {
        Executor executor=Executors.newSingleThreadExecutor();
        setIsTaskRunning(true);
        File output= FileItemUtils.createNewDownloadFile(fileItem);
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

    public void renameFile(String s, FileItem fileItem) {
        setIsTaskRunning(true);
        storageRepository.RenameFile(s, fileItem, profileItem.getValue())
                .continueWith(task->Unit.INSTANCE)
                .addOnCompleteListener(this);
    }

    public Task<Unit> deleteFiles(List<FileItem> fileItems) {
        setIsTaskRunning(true);
        List<Task<Void>> tasks=new ArrayList<>();
        for (FileItem fileItem : fileItems) {
            tasks.add(storageRepository.deleteFile(fileItem, profileItem.getValue()));
        }
        return Tasks.whenAll(tasks)
                .continueWithTask(task -> firestoreRepository.fetchUsedSpace(profileItem.getValue()))
                .continueWith(task -> {
                    setIsTaskRunning(!task.isComplete());
                    sharedPreferences.edit().putLong(USED_SPACE, task.getResult()).apply();
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> uploadUsingByteArray(FileItem item, Context context) {
        setIsTaskRunning(true);
        TaskCompletionSource<Unit> taskCompletionSource = new TaskCompletionSource<>();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            byte[] bytes1;
            try {
                Uri uri = Uri.parse(item.getFilePath());
                File file = new File(uri.toString());
                byte[] buffer = new byte[FileItemUtils.calculateBufferSize(context, item.getFileSize())];
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    int read;
                    while ((read = bufferedReader.read()) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    bytes1 = outputStream.toByteArray();
                } catch (Exception e) {
                    try (InputStream inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
                         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }
                        bytes1 = outputStream.toByteArray();
                    }
                }

                UploadTask uploadTask = storageRepository.UploadFile(item, bytes1, getProfileItem().getValue());
                FileTask<UploadTask> fileTask = new FileTask<>(item, uploadTask);
                addUploadItem(fileTask);

                try {
                    Task<Unit> task = uploadTask
                            .continueWithTask(executor, t -> storageRepository.getDownloadUrl(item, getProfileItem().getValue()))
                            .onSuccessTask(executor, t -> firestoreRepository.fetchUsedSpace(getProfileItem().getValue()))
                            .continueWith(executor, t -> {
                                setIsTaskRunning(!t.isComplete());
                                sharedPreferences.edit().putLong(USED_SPACE, t.getResult()).apply();
                                return Unit.INSTANCE;
                            });
                    taskCompletionSource.setResult(task.getResult());
                } catch (Exception e) {
                    taskCompletionSource.setException(e);
                }
            }catch (Exception e){
                taskCompletionSource.setException(e);
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Unit> downloadUsingDownloadManager(FileItem item, Context context) {
        TaskCompletionSource<Unit> taskCompletionSource = new TaskCompletionSource<>();
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(item.getDownloadUrl()));
        request.setTitle("Downloading...")
                .setDescription(item.getFileName())
                .setMimeType(item.getFileExtension())
                .setDestinationUri(Uri.fromFile(FileItemUtils.createNewDownloadFile(item)))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        long downloadId = downloadManager.enqueue(request);
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
                        taskCompletionSource.setResult(Unit.INSTANCE);
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        taskCompletionSource.setException(new Exception("Download failed"));
                    }
                    cursor.close();
                }
                if (!taskCompletionSource.getTask().isComplete()) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(runnable);
        return taskCompletionSource.getTask();
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

    public void addFileDownloadItem(FileTask<FileDownloadTask> fileTask){
        fileDownloadList.add(fileTask);
        try{
            mutableFileDownloadList.setValue(fileDownloadList);
        }catch (Exception e){
            mutableFileDownloadList.postValue(fileDownloadList);
        }
    }

    public void removeFileDownloadItem(FileTask<FileDownloadTask> fileTask) {
        fileDownloadList.remove(fileTask);
        try {
            mutableFileDownloadList.setValue(fileDownloadList);
        } catch (Exception e) {
            mutableFileDownloadList.postValue(fileDownloadList);
        }
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

    public void setIsTaskRunning(boolean isTaskRunning){
        this.isTaskRunning.postValue(isTaskRunning);
    }

    public Unit signIn(AuthCredential credential){
        setIsTaskRunning(true);
        authRepository.signIn(credential)
                .continueWithTask(task->{
                    return firestoreRepository.createAccount(task.getResult());
                })
                .addOnSuccessListener(task->{
                    try{
                        user.setValue(authRepository.getUser());
                    }catch (Exception e){
                        user.postValue(authRepository.getUser());
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace)
                .addOnCompleteListener(task -> setIsTaskRunning(!task.isComplete()));
        return Unit.INSTANCE;
    }

    public Task<Unit> checkPendingSignIn() {
        Task<AuthResult> authResultTask = authRepository.getAuth().getPendingAuthResult();
        if (authResultTask != null) {
            setIsTaskRunning(true);
            return authResultTask.continueWithTask(task -> {
                if(task.isSuccessful())
                    return Tasks.forResult(signIn(task.getResult().getCredential()));
                else
                    return Tasks.forException(new Exception("Couldn't sign in with credentials found using email and dynamic link"));
            });
        }
        else
            return Tasks.forException(new Exception("No pending sign in session found"));
    }

    public Task<Unit> signInWithEmail(String email,String link){
        setIsTaskRunning(true);
        return getAuthInstance().signInWithEmailLink(email,link)
                .continueWith(task -> {
                    setIsTaskRunning(!task.isComplete());
                    signIn(task.getResult().getCredential());
                    return Unit.INSTANCE;
                });
    }

    public LiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }

    public void onComplete(@NonNull Task<Unit> task) {
        setIsTaskRunning(!task.isComplete());
    }

    public LiveData<FirebaseUser> getUser(){
        return user;
    }

    public Task<Query> queryToSortByTimestamp() {
        return firestoreRepository.sortByTimestamp(profileItem.getValue());
    }

    public Task<Query> queryToSortBySize() {
        return firestoreRepository.sortBySize(profileItem.getValue());
    }

    public Task<Query> queryToSortByName() {
        return firestoreRepository.sortByName(profileItem.getValue());
    }

    public void checkUsedSpace(){
        setIsTaskRunning(true);
        firestoreRepository.fetchUsedSpace(getProfileItem().getValue())
                .addOnCompleteListener(task->{
                    sharedPreferences.edit().putLong(USED_SPACE,task.getResult()).apply();
                    usedSpace.postValue(task.getResult());
                    setIsTaskRunning(!task.isComplete());
                });
    }

    private void uploadDisplayPicture(FileItem fileItem,byte[] bytes) {
        setIsTaskRunning(true);
        storageRepository.UploadFile(fileItem,bytes ,getProfileItem().getValue())
                .onSuccessTask(task -> {
                    String s=task.getMetadata().getReference().toString();
                    fileItem.setStorageReference(s);
                    return task.getMetadata().getReference().getDownloadUrl();
                })
                .onSuccessTask(task -> {
                    try {
                        final ProfileItem profileItem = getProfileItem().getValue();
                        profileItem.setPhotoUrl(task.toString());
                        this.profileItem.postValue(profileItem);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return authRepository.UpdateProfileDisplayPicture(task);
                })
                .continueWith(task->Unit.INSTANCE)
                .addOnCompleteListener(this);
    }

    public void updateDisplayPicture(FileItem fileItem,byte[] bytes) {
        String s= getProfileItem().getValue().getPhotoUrl();
        boolean b=s.contains("https://firebasestorage.googleapis.com");
        Log.d(TAG, "updateDisplayPicture: picture found in bucket : "+b);
        if(b) {
            setIsTaskRunning(true);
            storageRepository.deleteFileByUrl(s)
                    .addOnSuccessListener(unused -> uploadDisplayPicture(fileItem,bytes))
                    .addOnFailureListener(Throwable::printStackTrace);
        }
        else uploadDisplayPicture(fileItem,bytes);
    }

    public void updateDisplayName(String toString){
        setIsTaskRunning(true);
        authRepository.UpdateProfileDisplayName(toString)
                .continueWith(task -> {
                    try {
                        final ProfileItem profileItem = getProfileItem().getValue();
                        profileItem.setDisplayName(toString);
                        this.profileItem.postValue(profileItem);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return Unit.INSTANCE;})
                .addOnCompleteListener(this);
    }

    public Task<Unit> deleteAccount(){
        setIsTaskRunning(true);
        return authRepository.getUser().delete()
                .onSuccessTask(task->storageRepository.deleteAllFiles(getProfileItem().getValue()))
                .onSuccessTask(task->firestoreRepository.deleteAccount(getProfileItem().getValue()))
                .onSuccessTask(task->firestoreRepository.clearCache())
                .continueWith(task->{
                    setIsTaskRunning(!task.isComplete());
                    return Unit.INSTANCE;
                });
    }

    public Task<Unit> signOut() {
        setIsTaskRunning(true);
        return firestoreRepository.clearCache().continueWith(task -> {
            sharedPreferences.edit().putLong(USED_SPACE,0L).apply();
            getAuthInstance().signOut();
            setIsTaskRunning(!task.isComplete());
            return Unit.INSTANCE;
        });
    }

    public FirebaseAuth getAuthInstance(){
        return authRepository.getAuth();
    }

    public void abortAllTasks() {
        try {
            Tasks.await(storageRepository.getTaskOfTasks());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}