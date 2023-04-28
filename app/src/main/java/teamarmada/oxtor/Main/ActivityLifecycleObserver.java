package teamarmada.oxtor.Main;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.MainViewModel;

public class ActivityLifecycleObserver extends FullScreenContentCallback implements DefaultLifecycleObserver {

    public static final String TAG= ActivityLifecycleObserver.class.getSimpleName();
    private static ActivityLifecycleObserver activityLifecycleObserver;
    private final ExecutorService executor= Executors.newSingleThreadExecutor();
    public static final int UPLOAD_TASK=1;
    public static final int DOWNLOAD_TASK=2;
    private final MainViewModel mainViewModel;
    private final AppCompatActivity activity;
    private final List<FileItem> fileItems=new ArrayList<>();
    private int requestCode;
    private AdView adView;
    private AlertDialog alertDialog=null;
    public static ActivityLifecycleObserver getInstance(@NonNull AppCompatActivity activity) {
        if(activityLifecycleObserver ==null)
            activityLifecycleObserver =new ActivityLifecycleObserver(activity);
        return activityLifecycleObserver;
    }

    private ActivityLifecycleObserver(@NonNull AppCompatActivity activity) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.activity=activity;
        mainViewModel=new ViewModelProvider(activity).get(MainViewModel.class);
        alertDialog=new MaterialAlertDialogBuilder(activity, R.style.Theme_Oxtor_AlertDialog)
                .setTitle("Caution !")
                .setMessage("App is running low on memory")
                .setCancelable(false)
                .setPositiveButton("Continue with rest of the items", (dialogInterface,i)-> continueAction(fileItems,requestCode))
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();
    }

    private void showAlertDialogForFileItem(String message) {
        alertDialog.setMessage(message);
        try {
            if (!alertDialog.isShowing())
                alertDialog.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private final Thread thread=new Thread(() -> continueAction(fileItems,requestCode));

    public void startUpload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode=UPLOAD_TASK;
        executor.execute(thread);
    }

    public void startDownload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode=DOWNLOAD_TASK;
        executor.execute(thread);
    }

    private void continueAction(@NonNull List<FileItem> fileItems,int requestCode){
        switch (requestCode){
            case UPLOAD_TASK:
                continueUpload(fileItems);
                break;
            case DOWNLOAD_TASK:
                continueDownload(fileItems);
                break;
        }
        fileItems.clear();
    }

    private void continueUpload(List<FileItem> fileItems) {
        for (int i=0;i<fileItems.size();i++) {
            final FileItem fileItem=fileItems.get(i);
            if (mainViewModel.getUsedSpace().getValue() <= FileItemUtils.ONE_GIGABYTE || fileItem.getFileSize()<= FileItemUtils.ONE_GIGABYTE) {
                if(canContinueAction(fileItem)) {
                    uploadFile(fileItem);
                }
                else{
                    fileItems.remove(fileItem);
                    requestCode=UPLOAD_TASK;
                    showAlertDialogForFileItem("Can't upload "+fileItem.getFileName()+" right now as the app will run out of memory and crash");
                }
            }
            else {
                makeToast("Can't upload "+fileItem.getFileName()+"as you are only permitted 1GB of space on this account");
            }
        }
    }

    private void continueDownload(List<FileItem> fileItems){
        for (int i=0;i<fileItems.size();i++) {
            final FileItem fileItem=fileItems.get(i);
            if(canContinueAction(fileItem)) {
                downloadFile(fileItem);
            }
            else{
                fileItems.remove(fileItem);
                requestCode=DOWNLOAD_TASK;
                showAlertDialogForFileItem("Can't download "+fileItem.getFileName()+" right now as the app will run out of memory and crash");
            }
        }
    }

    private void uploadFile(FileItem fileItem) {
        try {
            mainViewModel.uploadFile(activity, fileItem)
                    .addOnSuccessListener(unit -> {
                        if(fileItems.indexOf(fileItem)==fileItems.size()-1){
                            makeToast("Upload Completed");
                        }
                    })
                    .addOnFailureListener(e -> makeToast(e.toString()));
        } catch (Exception e) {
            try {
                mainViewModel.uploadByteArray(activity, fileItem)
                        .addOnSuccessListener(unit -> {
                            if(fileItems.indexOf(fileItem)==fileItems.size()-1){
                                makeToast("Upload Completed");
                            }
                        })
                        .addOnFailureListener(ex -> makeToast(ex.toString()));
            } catch (Exception ex) {
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(e.toString());
            }
        }
    }

    private void downloadFile(FileItem fileItem) {
        try {
            mainViewModel.downloadFile(activity, fileItem)
                    .addOnSuccessListener(unit -> {
                        if (fileItems.indexOf(fileItem) == fileItems.size() - 1) {
                            makeToast("Items saved at Oxtor/download/");
                        }
                    })
                    .addOnFailureListener(e -> makeToast(e.toString()));
        }catch (Exception e){
            try {
                mainViewModel.downloadViaDownloadManager(activity, fileItem);
            } catch (Exception ex) {
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(e.toString());
            }
        }
    }

    private boolean canContinueAction(FileItem fileItem){
        return fileItem.getFileSize()<getAvailableMemory(activity).availMem;
    }

    public void loadBanner(FrameLayout container){
        AdSize adSize = getAdSize(container);
        mainViewModel.loadAdView(adSize);
        adView=mainViewModel.getBannerAd();
        container.removeAllViews();
        container.addView(adView);
    }

    private AdSize getAdSize(FrameLayout container) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        float adWidthPixels = container.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }
        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    private void makeToast(String msg){
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    private ActivityManager.MemoryInfo getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        if(!fileItems.isEmpty())
            makeToast("Resuming...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
        if (!thread.isAlive()) {
            executor.execute(thread);
        }
        super.onAdDismissedFullScreenContent();
    }

    @Override
    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
        makeToast("Showing Ad failed...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
        if (!thread.isAlive()) {
            executor.execute(thread);
        }
        super.onAdFailedToShowFullScreenContent(adError);
    }

    @Override
    public void onAdShowedFullScreenContent() {
        if (!thread.isAlive()) {
            executor.execute(thread);
        }
        super.onAdShowedFullScreenContent();
    }

    @Override
    public void onAdClicked() {
        mainViewModel.setIsTaskRunning(false);
        thread.interrupt();
        super.onAdClicked();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mainViewModel != null && mainViewModel.getAppOpenAd() != null) {
            if (!getAvailableMemory(activity).lowMemory&&!fileItems.isEmpty()) {
                mainViewModel.getAppOpenAd().setFullScreenContentCallback(this);
                mainViewModel.getAppOpenAd().show(activity);
            }
        }
        else if(mainViewModel!=null){
            if(!fileItems.isEmpty()) {
                executor.execute(thread);
            }
            mainViewModel.loadAppOpenAd();
        }
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if(adView!=null)
            adView.pause();
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if(adView!=null)
            adView.resume();
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if(adView!=null)
            adView.destroy();
        DefaultLifecycleObserver.super.onDestroy(owner);
    }

}
