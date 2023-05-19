package teamarmada.oxtor.Main;

import static teamarmada.oxtor.Main.ActivityLifecycleObserver.RequestCode.DOWNLOAD_TASK;
import static teamarmada.oxtor.Main.ActivityLifecycleObserver.RequestCode.UPLOAD_TASK;

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
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.Unit;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
import teamarmada.oxtor.Utils.FileItemUtils;
import teamarmada.oxtor.ViewModels.MainViewModel;

public class ActivityLifecycleObserver extends FullScreenContentCallback implements DefaultLifecycleObserver {

    public static final String TAG = ActivityLifecycleObserver.class.getSimpleName();
    private static ActivityLifecycleObserver activityLifecycleObserver;
    private final List<FileItem> fileItems = new ArrayList<>();
    private final MainViewModel mainViewModel;
    private final AppCompatActivity activity;
    private RequestCode requestCode;
    private AdView adView;
    private Thread thread;
    private boolean isThreadRunning = false;
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
        if (!isThreadRunning) {
            thread = new Thread(() -> {
                isThreadRunning = true;
                while (!Thread.currentThread().isInterrupted()) {
                    if (!isPaused) {
                        continueAction(fileItems, requestCode);
                    } else {
                        // Execution is paused, wait for resume signal
                        synchronized (threadLock) {
                            try {
                                threadLock.wait();
                            } catch (InterruptedException e) {
                                // Handle interruption if needed
                            }
                        }
                    }
                }
                isThreadRunning = false;
            });
            thread.start();
        }
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
            uploadFile(fileItems.get(i));
        }
        fileItems.clear();
    }

    private void continueDownload(List<FileItem> fileItems) {
        for (int i = 0; i < fileItems.size(); i++) {
            downloadFile(fileItems.get(i));
        }
        fileItems.clear();
    }

    private void uploadFile(FileItem fileItem) {
        Task<Unit> uploadTask;
        try {
            uploadTask = mainViewModel.uploadUsingByteArray(fileItem,activity);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                uploadTask = mainViewModel.uploadUsingInputStream(fileItem,activity);
            } catch (Exception ex) {
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(ex.toString());
                return;
            }
        }
        uploadTask.addOnSuccessListener(activity, unit -> {
            if (fileItems.indexOf(fileItem) == fileItems.size() - 1) {
                makeToast("Upload Completed");
            }
        }).addOnFailureListener(activity, e -> makeToast(e.toString()));
    }

    private void downloadFile(FileItem fileItem) {
        Task<Unit> downloadTask;
        try {
            downloadTask = mainViewModel.downloadUsingInputStream(fileItem,activity);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                downloadTask = mainViewModel.downloadUsingDownloadManager(fileItem, activity);
            } catch (Exception ex) {
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(ex.toString());
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
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAdDismissedFullScreenContent() {
        if (!fileItems.isEmpty())
            makeToast("Resuming...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
        if (!isThreadRunning) {
            startThread();
        }
        super.onAdDismissedFullScreenContent();
    }

    @Override
    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
        makeToast("Showing Ad failed...");
        mainViewModel.setAppOpenAd(null);
        mainViewModel.setIsTaskRunning(false);
        if (!isThreadRunning) {
            startThread();
        }
        super.onAdFailedToShowFullScreenContent(adError);
    }

    @Override
    public void onAdShowedFullScreenContent() {
        if (!isThreadRunning) {
            startThread();
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
            mainViewModel.getAppOpenAd().setFullScreenContentCallback(this);
            mainViewModel.getAppOpenAd().show(activity);
        } else if (mainViewModel != null) {
            if (!fileItems.isEmpty()) {
                startThread();
            }
            mainViewModel.loadAppOpenAd();
        }
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.pause();
        }
        onPauseExecution();
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.resume();
        }
        onResumeExecution();
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (adView != null) {
            adView.destroy();
        }
        if (thread != null) {
            thread.interrupt();
            thread = null; // Release the reference to the thread
        }
        DefaultLifecycleObserver.super.onDestroy(owner);
    }

    private void onPauseExecution() {
        synchronized (threadLock) {
            isPaused = true;
        }
    }

    private void onResumeExecution() {
        synchronized (threadLock) {
            isPaused = false;
            try {
                threadLock.notifyAll();
            } catch (IllegalMonitorStateException e) {
                makeToast("_");
            }
        }
    }


    public enum RequestCode {
        UPLOAD_TASK(1001),
        DOWNLOAD_TASK(1002);

        private final int requestCode;

        RequestCode(int requestCode) {
            this.requestCode = requestCode;
        }

        public int getRequestCode() {
            return requestCode;
        }
    }
}

