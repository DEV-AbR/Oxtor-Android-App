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
import teamarmada.oxtor.ViewModels.MainViewModel;

public class ActivityLifecycleObserver extends FullScreenContentCallback implements DefaultLifecycleObserver {

    public static final String TAG= ActivityLifecycleObserver.class.getSimpleName();
    private static ActivityLifecycleObserver activityLifecycleObserver;
    private final ExecutorService executor= Executors.newSingleThreadExecutor();
    private final List<FileItem> fileItems=new ArrayList<>();
    private RequestCode requestCode;
    private final MainViewModel mainViewModel;
    private final AppCompatActivity activity;
    private AdView adView;
    private final AlertDialog alertDialog;

    public static ActivityLifecycleObserver getInstance(@NonNull AppCompatActivity activity) {
        if(activityLifecycleObserver ==null)
            activityLifecycleObserver =new ActivityLifecycleObserver(activity);
        return activityLifecycleObserver;
    }

    private ActivityLifecycleObserver(@NonNull AppCompatActivity activity) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        this.activity=activity;
        alertDialog=new MaterialAlertDialogBuilder(activity, R.style.Theme_Oxtor_AlertDialog)
                .setTitle("Caution !")
                .setMessage("Running low memory, App might crash if continued")
                .setCancelable(false)
                .setPositiveButton("Continue anyway", (dialogInterface,i)-> executor.execute(thread))
                .setNegativeButton("Abort task", (dialogInterface, i) -> thread.interrupt())
                .create();
        mainViewModel=new ViewModelProvider(activity).get(MainViewModel.class);
        mainViewModel.getMemoryLiveData(activity).observe(activity, aBoolean -> {
            try {
                if (aBoolean)
                    showAlertDialog();
                else if (alertDialog.isShowing())
                    dismissAlertDialog();
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    private final Thread thread=new Thread(() -> continueAction(fileItems,requestCode));

    public void startUpload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode= UPLOAD_TASK;
        executor.execute(thread);
    }

    public void startDownload(List<FileItem> fileItems) {
        this.fileItems.addAll(fileItems);
        requestCode= DOWNLOAD_TASK;
        executor.execute(thread);
    }

    private void continueAction(@NonNull List<FileItem> fileItems,RequestCode requestCode){
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
            uploadFile(fileItems.get(i));
        }
    }

    private void continueDownload(List<FileItem> fileItems){
        for (int i=0;i<fileItems.size();i++) {
            downloadFile(fileItems.get(i));
        }
    }

    private void uploadFile(FileItem fileItem) {
        Task<Unit> uploadTask;
        try{
            uploadTask= mainViewModel.uploadUsingInputStream(activity,fileItem);
        }catch (Exception e){
            e.printStackTrace();
            try{
                uploadTask= mainViewModel.uploadUsingByteArray(activity,fileItem);
            }catch (Exception ex){
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(ex.toString());
                return;
            }
        }
        uploadTask.addOnSuccessListener(activity,unit -> {
                        if(fileItems.indexOf(fileItem)==fileItems.size()-1){
                            makeToast("Upload Completed");
                        }
                    })
                    .addOnFailureListener(activity,e -> makeToast(e.toString()));
    }

    private void downloadFile(FileItem fileItem) {
        Task<Unit> downloadTask;
        try{
            downloadTask= mainViewModel.downloadUsingInputStream(activity,fileItem);
        }catch (Exception e){
            e.printStackTrace();
            try {
                mainViewModel.downloadViaDownloadManager(activity,fileItem);
            } catch (Exception ex) {
                ex.printStackTrace();
                mainViewModel.setIsTaskRunning(false);
                makeToast(e.toString());
            }
            return;
        }
        downloadTask.addOnSuccessListener(activity,unit -> {
            if (fileItems.indexOf(fileItem) == fileItems.size() - 1) {
                makeToast("Items saved at Oxtor/download/");
            }
        }).addOnFailureListener(activity,ex -> makeToast(ex.toString()));
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

    private void showAlertDialog() {
        new Handler(Looper.getMainLooper()).post(alertDialog::show);
    }

    private void dismissAlertDialog() {
        new Handler(Looper.getMainLooper()).post(alertDialog::dismiss);
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
            mainViewModel.getAppOpenAd().setFullScreenContentCallback(this);
            mainViewModel.getAppOpenAd().show(activity);
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
        if(adView!=null) {
            adView.pause();
        }
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if(adView!=null) {
            adView.resume();
        }
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if(adView!=null) {
            adView.destroy();
        }
        DefaultLifecycleObserver.super.onDestroy(owner);
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
