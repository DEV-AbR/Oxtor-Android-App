package teamarmada.oxtor.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import teamarmada.oxtor.BuildConfig;
import teamarmada.oxtor.R;

public class ForceUpdate {

    private static ForceUpdate forceUpdate=null;
    private final AppCompatActivity activity;
    private final FirebaseRemoteConfig frc;

    public static ForceUpdate getInstance(AppCompatActivity activity) {
        if(forceUpdate==null)
            forceUpdate=new ForceUpdate(activity);
        return forceUpdate;
    }

    public ForceUpdate(AppCompatActivity activity){
        this.activity=activity;
        frc = FirebaseRemoteConfig.getInstance();
    }

    public void checkForUpdate() {
        String appVersion = getAppVersion();
        String currentVersion = frc.getString("min_version_of_app");
        String minVersion = frc.getString("latest_version_of_app");
        if (!TextUtils.isEmpty(minVersion) && !TextUtils.isEmpty(appVersion) && checkMandateVersionApplicable(
                getAppVersionWithoutAlphaNumeric(minVersion), getAppVersionWithoutAlphaNumeric(appVersion))) {
            onUpdateNeeded(true);
        }
        else if (!TextUtils.isEmpty(currentVersion) &&
                !TextUtils.isEmpty(appVersion) &&
                !TextUtils.equals(currentVersion, appVersion)) {
            onUpdateNeeded(false);
        }
        else {
            moveForward();
        }
    }

    private Boolean checkMandateVersionApplicable(String minVersion,String appVersion) {
        try {
            int minVersionInt = Integer.parseInt(minVersion);
            int appVersionInt = Integer.parseInt(appVersion);
            return minVersionInt > appVersionInt;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private String getAppVersionWithoutAlphaNumeric(String result) {
        String version_str = "";
        version_str = result.replace(".", "");
        return version_str;
    }

    private void onUpdateNeeded(Boolean isMandatoryUpdate) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity,R.style.Theme_Oxtor_AlertDialog)
                .setTitle(activity.getString(R.string.update_app))
                .setCancelable(false)
                .setMessage(isMandatoryUpdate?
                    activity.getString(R.string.dialog_update_available_message):
                        "A new version is found on Play store, please update for better usage.")
            .setPositiveButton(R.string.update_now,((dialogInterface, i) -> openAppOnPlayStore()));

        if (!isMandatoryUpdate) {
            dialogBuilder.setNegativeButton(R.string.later,(dialogInterface, i) -> {
                moveForward();
                dialogInterface.dismiss();
            }).create();
        }
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void moveForward() {
        makeToast("Next Page Intent");
    }

    private void makeToast(String msg){
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    void openAppOnPlayStore() {
        Uri uri = Uri.parse("market://details?id=teamarmada.oxtor");
        openURI(activity, uri, "Play Store not found in your device");
    }

    void openURI(Context ctx, Uri uri,String error_msg) {
        Intent i =new Intent(Intent.ACTION_VIEW, uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if (ctx.getPackageManager().queryIntentActivities(i, 0).size() > 0) {
            ctx.startActivity(i);
        } else if (error_msg != null) {
            makeToast(error_msg);
        }
    }

}
