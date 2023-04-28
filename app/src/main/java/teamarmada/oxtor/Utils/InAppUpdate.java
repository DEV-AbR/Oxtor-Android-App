package teamarmada.oxtor.Utils;

import static android.graphics.Color.WHITE;
import static com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE;
import static com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE;

import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import teamarmada.oxtor.Main.App;
import teamarmada.oxtor.R;


public class InAppUpdate implements InstallStateUpdatedListener, DefaultLifecycleObserver {

    private final AppUpdateManager appUpdateManager;
    private final int MY_REQUEST_CODE = 500;
    private final AppCompatActivity parentActivity;
    private int currentType = FLEXIBLE;
    private static InAppUpdate inAppUpdate=null;

    public static InAppUpdate getInstance(AppCompatActivity activity) {
        if(inAppUpdate==null)
            inAppUpdate=new InAppUpdate(activity);
        return inAppUpdate;
    }

    private InAppUpdate(AppCompatActivity activity) {
        parentActivity=activity;
        activity.getLifecycle().addObserver(this);
        appUpdateManager = AppUpdateManagerFactory.create(parentActivity);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener ( info -> {
            // Check if update is available
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (info.updatePriority() == 5) { // Priority: 5 (Immediate update flow)
                    if (info.isUpdateTypeAllowed(IMMEDIATE)) {
                        startUpdate(info, IMMEDIATE);
                    }
                } else if (info.updatePriority() == 4) { // Priority: 4
                    Integer clientVersionStalenessDays = info.clientVersionStalenessDays();
                    if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 5 && info.isUpdateTypeAllowed(IMMEDIATE)) {
                        // Trigger IMMEDIATE flow
                        startUpdate(info, IMMEDIATE);
                    } else if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 3 && info.isUpdateTypeAllowed(FLEXIBLE)) {
                        // Trigger FLEXIBLE flow
                        startUpdate(info, FLEXIBLE);
                    }
                } else if (info.updatePriority() == 3) { // Priority: 3
                    Integer clientVersionStalenessDays = info.clientVersionStalenessDays();
                    if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 30 && info.isUpdateTypeAllowed(IMMEDIATE)) {
                        // Trigger IMMEDIATE flow
                        startUpdate(info, IMMEDIATE);
                    } else if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 15 && info.isUpdateTypeAllowed(FLEXIBLE)) {
                        // Trigger FLEXIBLE flow
                        startUpdate(info, FLEXIBLE);
                    }
                } else if (info.updatePriority() == 2) { // Priority: 2

                    Integer clientVersionStalenessDays = info.clientVersionStalenessDays();
                    if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 90 && info.isUpdateTypeAllowed(IMMEDIATE)) {
                        // Trigger IMMEDIATE flow
                        startUpdate(info, IMMEDIATE);
                    } else if (clientVersionStalenessDays != null && clientVersionStalenessDays >= 30 && info.isUpdateTypeAllowed(FLEXIBLE)) {
                        // Trigger FLEXIBLE flow
                        startUpdate(info, FLEXIBLE);
                    }
                } else if (info.updatePriority() == 1) { // Priority: 1
                    // Trigger FLEXIBLE flow
                    startUpdate(info, FLEXIBLE);
                } else { // Priority: 0
                    // Do not show in-app update
                }
            } else {
                // UPDATE IS NOT AVAILABLE
            }
        });
        appUpdateManager.registerListener(this);
    }


    private void startUpdate(AppUpdateInfo info,int type) {
        try {
            appUpdateManager.startUpdateFlowForResult(info, type, parentActivity, MY_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
        currentType = type;
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != AppCompatActivity.RESULT_OK) {
                // If the update is cancelled or fails, you can request to start the update again.
                Log.e("ERROR", "Update flow failed! Result code: $resultCode");
            }
        }
    }

    private void flexibleUpdateDownloadCompleted() {
        Snackbar snackbar=Snackbar.make(
            parentActivity.findViewById(R.id.container),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        );
        snackbar.setAction("RESTART",v->{
            appUpdateManager.completeUpdate();
            snackbar.dismiss();
        }).setActionTextColor(WHITE).show();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener (info -> {
                    if (currentType == FLEXIBLE) {
                        // If the update is downloaded but not installed, notify the user to complete the update.
                        if (info.installStatus() == InstallStatus.DOWNLOADED)
                            flexibleUpdateDownloadCompleted();
                    } else if (currentType == IMMEDIATE) {
                        // for AppUpdateType.IMMEDIATE only, already executing updater
                        if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                            startUpdate(info, IMMEDIATE);
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        appUpdateManager.unregisterListener(this);

    }

    @Override
    public void onStateUpdate(@NonNull InstallState installState) {
        if (installState.installStatus() == InstallStatus.DOWNLOADED) {
            flexibleUpdateDownloadCompleted();
        }
    }

}