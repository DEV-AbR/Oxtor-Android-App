package teamarmada.oxtor.Main;

import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.ViewModels.MainViewModel;

public class ActivityLifecycleObserver extends FullScreenContentCallback implements DefaultLifecycleObserver {

    private static ActivityLifecycleObserver activityLifecycleObserver;
    private final List<FileItem> fileItems = new ArrayList<>();
    private final MainViewModel mainViewModel;
    private final AppCompatActivity activity;
    private enum RequestCode{UPLOAD_TASK, DOWNLOAD_TASK}
    private RequestCode requestCode;
    private AdView adView;

    public static ActivityLifecycleObserver getInstance(@NonNull AppCompatActivity activity) {
        if (activityLifecycleObserver == null)
            activityLifecycleObserver = new ActivityLifecycleObserver(activity);
        return activityLifecycleObserver;
    }

    private ActivityLifecycleObserver(@NonNull AppCompatActivity activity) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.activity = activity;
        mainViewModel = new ViewModelProvider(activity).get(MainViewModel.class);
        mainViewModel.getMemoryLiveData(activity).observe(activity, lowMemory -> {
            if(lowMemory){
                makeToast("App is running low on memory");
                try {
                    makeToast("Clearing all pending tasks");
                    mainViewModel.mutableUploadList.getValue().clear();
                    mainViewModel.mutableFileDownloadList.getValue().clear();
                    mainViewModel.abortAllTasks();
                }catch (Exception e){
                    activity.finish();
                    activity.startActivity(activity.getIntent());
                    activity.overridePendingTransition(0, 0);
                }
            }
        });
    }


    public void startUpload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode = RequestCode.UPLOAD_TASK;
        makeToast("Upload started");
        continueAction(fileItems, requestCode);
    }

    public void startDownload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode = RequestCode.DOWNLOAD_TASK;
        makeToast("Download started");
        continueAction(fileItems, requestCode);
    }

    private void continueAction(@NonNull List<FileItem> fileItems, RequestCode requestCode) {
        try {
            switch (requestCode) {
                case UPLOAD_TASK:
                    continueUpload(fileItems);
                    break;
                case DOWNLOAD_TASK:
                    continueDownload(fileItems);
                    break;
            }
        } catch (Exception e) {
            fileItems.clear();
            this.fileItems.clear();
        }
    }

    private void continueUpload(List<FileItem> fileItems) {
        List<Task<Unit>> tasks=new ArrayList<>();
        for (int i = 0; i < fileItems.size(); i++) {
            final FileItem fileItem = fileItems.get(i);
            tasks.add(uploadFile(fileItem));
        }
        Tasks.whenAll(tasks)
                .addOnCompleteListener(task-> mainViewModel.setIsTaskRunning(!task.isComplete()))
                .addOnSuccessListener(result-> makeToast("All uploads will be deleted after 24 hours"))
                .addOnFailureListener(e-> makeToast(e.toString()));
    }

    private void continueDownload(List<FileItem> fileItems) {
        List<Task<Unit>> tasks=new ArrayList<>();
        for (int i = 0; i < fileItems.size(); i++) {
            final FileItem fileItem = fileItems.get(i);
            tasks.add(downloadFile(fileItem));
        }
        Tasks.whenAll(tasks)
                .addOnCompleteListener(task-> mainViewModel.setIsTaskRunning(!task.isComplete()))
                .addOnSuccessListener(result-> makeToast("Items saved at Oxtor/download/"))
                .addOnFailureListener(e-> makeToast(e.toString()));
    }

    private Task<Unit> uploadFile(FileItem fileItem) {
        return mainViewModel.uploadUsingInputStream(fileItem,activity)
                .continueWithTask(task-> task.isSuccessful()?Tasks.forResult(task.getResult()):mainViewModel.uploadUsingByteArray(fileItem,activity))
                .continueWithTask(task -> task.isSuccessful()? Tasks.forResult(task.getResult()):Tasks.forException(task.getException()));
    }

    private Task<Unit> downloadFile(FileItem fileItem) {
        return mainViewModel.downloadUsingInputStream(fileItem,activity)
                .continueWithTask(task -> task.isSuccessful()?Tasks.forResult(task.getResult()):mainViewModel.downloadUsingDownloadManager(fileItem,activity))
                .continueWithTask(task -> task.isSuccessful()?Tasks.forResult(task.getResult()):Tasks.forException(task.getException()));
    }

    public void loadBanner(FrameLayout container) {
        AdSize adSize = getAdSize(container);
        mainViewModel.loadAdView(adSize);
        adView = mainViewModel.getBannerAd();
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

    private void makeToast(String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        if (!fileItems.isEmpty())
            makeToast("Resuming...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
    }

    @Override
    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
    }

    @Override
    public void onAdShowedFullScreenContent() {
    }

    @Override
    public void onAdClicked() {
        mainViewModel.setIsTaskRunning(false);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mainViewModel != null && mainViewModel.getAppOpenAd() != null) {
            mainViewModel.getAppOpenAd().setFullScreenContentCallback(this);
            mainViewModel.getAppOpenAd().show(activity);
        } else if (mainViewModel != null) {
            mainViewModel.loadAppOpenAd();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.pause();
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.destroy();
        }
    }

}


