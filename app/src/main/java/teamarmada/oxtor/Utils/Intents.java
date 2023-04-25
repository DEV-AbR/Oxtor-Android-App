package teamarmada.oxtor.Utils;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Intents {

    public static final String TAG=Intents.class.getSimpleName();

    public static Intent getMediaChooserIntent(Context context) {
        List<Intent> allIntents = new ArrayList<>();
        if (!isExplicitCameraPermissionRequired(context) ) {
            allIntents.addAll(getImageCaptureIntents(context));
            allIntents.addAll(getVideoCaptureIntents(context));
        }
        Intent target;
        if (allIntents.isEmpty()) {
            target = new Intent();
        }
        else {
            target = allIntents.get(allIntents.size() - 1);
            allIntents.remove(allIntents.size() - 1);
        }
        Intent chooserIntent = Intent.createChooser(target, "Select source");
        Parcelable[] parcelables=new Parcelable[allIntents.size()];
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(parcelables));
        return chooserIntent;
    }

    private static List<Intent> getImageCaptureIntents(Context context) {
        PackageManager packageManager=context.getPackageManager();
        List<Intent> allIntents = new ArrayList<>();
        Uri outputFileUri = null;
        try {
            outputFileUri = getCaptureImageOutputUri();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }
        return allIntents;
    }

    private static List<Intent> getVideoCaptureIntents(Context context){
        PackageManager packageManager=context.getPackageManager();
        List<Intent> allIntents = new ArrayList<>();
        Uri outputFileUri=null;
        try {
            outputFileUri = getCaptureVideoOutputUri();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            if (outputFileUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            }
            allIntents.add(intent);
        }
        return allIntents;
    }

    private static boolean isExplicitCameraPermissionRequired(Context context) {
        return hasPermissionInManifest(context, "android.permission.CAMERA") &&
                context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermissionInManifest(Context context, String permissionName) {
        String packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_PERMISSIONS);
            String[] declaredPermissions = packageInfo.requestedPermissions;
            if (declaredPermissions != null && declaredPermissions.length > 0) {
                for (String p : declaredPermissions) {
                    if (p.equalsIgnoreCase(permissionName)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    private static Uri getCaptureImageOutputUri() throws IOException {
        Log.d(TAG, "getCaptureImageOutputUri: ");
        try {
            String uri=FileItemUtils.createUploadFile("images",".jpeg").getAbsolutePath();
            return Uri.parse(uri);
        } catch (Exception e) {
            e.printStackTrace();
            File dir=new File(Environment.getExternalStorageDirectory(),"Oxtor/Upload");
            if(!dir.exists())
                dir.mkdirs();
            File file=File.createTempFile(FileItemUtils.generateFileNameWithoutExtension(),".jpeg",dir);
            return Uri.parse(file.getAbsolutePath());
        }
    }

    private static Uri getCaptureVideoOutputUri() throws IOException {
        Log.d(TAG, "getCaptureVideoOutputUri: ");
        try {
            String uri=FileItemUtils.createUploadFile("videos",".mp4").getAbsolutePath();
            return Uri.parse(uri);
        } catch (Exception e) {
            e.printStackTrace();
            File dir=new File(Environment.getExternalStorageDirectory(),"Oxtor/Upload");
            if(!dir.exists())
                dir.mkdirs();
            File file=File.createTempFile(FileItemUtils.generateFileNameWithoutExtension(),".mp4",dir);
            return Uri.parse(file.getAbsolutePath());
        }
    }

}