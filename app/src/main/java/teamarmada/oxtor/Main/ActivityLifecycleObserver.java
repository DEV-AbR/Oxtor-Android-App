package teamarmada.oxtor.Main;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;
import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.ViewModels.MainViewModel;

public class ActivityLifecycleObserver extends FullScreenContentCallback implements DefaultLifecycleObserver {
    private static final String TAG = ActivityLifecycleObserver.class.getSimpleName();
    private static ActivityLifecycleObserver activityLifecycleObserver;
    private final List<FileItem> fileItems = new ArrayList<>();
    private final MainViewModel mainViewModel;
    private final AppCompatActivity activity;
    private RequestCode requestCode;
    private AdView adView;
    private ExecutorService executorService;
    private boolean isPaused = false;
    private final Object threadLock = new Object();

    public static ActivityLifecycleObserver getInstance(@NonNull AppCompatActivity activity) {
        if (activityLifecycleObserver == null)
            activityLifecycleObserver = new ActivityLifecycleObserver(activity);
        return activityLifecycleObserver;
    }

    private ActivityLifecycleObserver(@NonNull AppCompatActivity activity) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.activity = activity;
        mainViewModel = new ViewModelProvider(activity).get(MainViewModel.class);
        mainViewModel.getMemoryLiveData(activity).observe(activity, this::onLowMemory);
    }

    private void onLowMemory(boolean lowMemory) {
        if (lowMemory) {
            onPauseExecution();
        } else {
            onResumeExecution();
        }
    }

    public void startUpload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode = RequestCode.UPLOAD_TASK;
        startThread();
    }

    public void startDownload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode = RequestCode.DOWNLOAD_TASK;
        startThread();
    }

    private void startThread() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        executorService.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (!isPaused) {
                    continueAction(fileItems, requestCode);
                } else {
                    synchronized (threadLock) {
                        try {
                            threadLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        });
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
        for (int i = 0; i < fileItems.size(); i++) {
            final FileItem fileItem = fileItems.get(i);
            executorService.execute(() -> uploadFile(fileItem));
        }
        fileItems.clear();
    }

    private void continueDownload(List<FileItem> fileItems) {
        for (int i = 0; i < fileItems.size(); i++) {
            final FileItem fileItem = fileItems.get(i);
            executorService.execute(() -> downloadFile(fileItem));
        }
        fileItems.clear();
    }

    private void uploadFile(FileItem fileItem) {
        Task<Unit> uploadTask;
        try {
            uploadTask = mainViewModel.uploadUsingByteArray(fileItem, activity, executorService);
        } catch (Exception e) {
            try {
                uploadTask = mainViewModel.uploadUsingInputStream(fileItem, activity, executorService);
            } catch (Exception ex) {
                mainViewModel.setIsTaskRunning(false);
                makeToast(ex.toString());
                return;
            }
        }
        uploadTask.addOnSuccessListener(activity, unit -> {
            if (fileItems.indexOf(fileItem) == fileItems.size() - 1) {
                makeToast("Upload Completed");
                makeToast("All files will be destroyed after 24 hours");
            }
        }).addOnFailureListener(activity, e -> makeToast(e.toString()));
    }

    private void downloadFile(FileItem fileItem) {
        Task<Unit> downloadTask;
        try {
            downloadTask = mainViewModel.downloadUsingInputStream(fileItem, activity, executorService);
        } catch (Exception e) {
            try {
                downloadTask = mainViewModel.downloadUsingDownloadManager(fileItem, activity);
            } catch (Exception ex) {
                mainViewModel.setIsTaskRunning(false);
                makeToast(ex.toString());
                makeToast("Viewing in web browser");
                Uri url = Uri.parse(fileItem.getDownloadUrl());
                Intent i= new Intent(Intent.ACTION_VIEW,url);
                activity.startActivity(i);
                return;
            }
        }
        downloadTask.addOnSuccessListener(activity, unit -> {
            if (fileItems.indexOf(fileItem) == fileItems.size() - 1) {
                makeToast("Items saved at Oxtor/download/");
            }
        }).addOnFailureListener(activity, ex -> makeToast(ex.toString()));
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
        if (executorService == null || executorService.isShutdown()) {
            startThread();
        }
    }

    @Override
    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
        makeToast("Showing Ad failed...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
        if (executorService == null || executorService.isShutdown()) {
            startThread();
        }
    }

    @Override
    public void onAdShowedFullScreenContent() {
        if (executorService == null || executorService.isShutdown()) {
            startThread();
        }
    }

    @Override
    public void onAdClicked() {
        mainViewModel.setIsTaskRunning(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mainViewModel != null && mainViewModel.getAppOpenAd() != null) {
            mainViewModel.getAppOpenAd().setFullScreenContentCallback(this);
            mainViewModel.getAppOpenAd().show(activity);
        } else if (mainViewModel != null) {
            if (!fileItems.isEmpty()) {
                startThread();
            }
            mainViewModel.loadAppOpenAd();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.pause();
        }
        onPauseExecution();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.resume();
        }
        onResumeExecution();
    }

    private void onPauseExecution() {
        isPaused = true;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void onResumeExecution() {
        isPaused = false;
        synchronized (threadLock) {
            threadLock.notifyAll();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.destroy();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private enum RequestCode {
        UPLOAD_TASK,
        DOWNLOAD_TASK
    }
}


