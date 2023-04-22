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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Intents {
    private int PICK_IMAGE_CHOOSER_REQUEST_CODE = 200;

    /**
     * Create a chooser intent to select the source to get image from.<br></br>
     * The source can be camera's (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br></br>
     * All possible sources are added to the intent chooser.
     *
     * @param context used to access Android APIs, like content resolve, it is your
     * activity/fragment/widget.
     * @param title the title to use for the chooser UI
     * @param includeDocuments if to include KitKat documents activity containing all sources
     * @param includeCamera if to include camera intents
     */
    public static Intent getPickImageChooserIntent(Context context, CharSequence title, boolean includeDocuments, boolean includeCamera) {
        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        if (!isExplicitCameraPermissionRequired(context) && includeCamera) {
            allIntents.addAll(getCameraIntents(packageManager));
            allIntents.addAll(getVideoIntents(packageManager));
        }
        List<Intent> galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_GET_CONTENT, includeDocuments);
        if (galleryIntents.isEmpty()) {
            galleryIntents = getGalleryIntents(packageManager, Intent.ACTION_PICK, includeDocuments);
        }
        allIntents.addAll(galleryIntents);
        Intent target;
        if (allIntents.isEmpty()) {
            target = new Intent();
        } else {
            target = allIntents.get(allIntents.size() - 1);
            allIntents.remove(allIntents.size() - 1);
        }
        Intent chooserIntent = Intent.createChooser(target, title);
        Parcelable[] parcelables=new Parcelable[allIntents.size()];
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(parcelables));
        return chooserIntent;
    }

    private static List<Intent> getCameraIntents(PackageManager packageManager) {
        List<Intent> allIntents = new ArrayList<>();
        Uri outputFileUri = getCaptureImageOutputUri();
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

    private static List<Intent> getVideoIntents(PackageManager packageManager){
        List<Intent> allIntents = new ArrayList<>();
        Uri outputFileUri = getCaptureVideoOutputUri();
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


    /**
     * Get all Gallery intents for getting image from one of the apps of the device that handle
     * images.
     */
    private static List<Intent> getGalleryIntents(PackageManager packageManager, String action, boolean includeDocuments) {
        List<Intent> intents = new ArrayList<>();
        Intent galleryIntent = action.equals(Intent.ACTION_GET_CONTENT)
                ? new Intent(action)
                : new Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*").setType("video/*");

        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intents.add(intent);
        }
        return intents;
    }

    /**
     * Check if explicetly requesting camera permission is required.<br></br>
     * It is required in Android Marshmellow and above if "CAMERA" permission is requested in the
     * manifest.<br></br>
     * See [StackOverflow
     * question](http://stackoverflow.com/questions/32789027/android-m-camera-intent-permission-bug).
     */
    private static boolean isExplicitCameraPermissionRequired(Context context) {
        return hasPermissionInManifest(context, "android.permission.CAMERA") && context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if the app requests a specific permission in the manifest.
     *
     * @param permissionName the permission to check
     * @return true - the permission in requested in manifest, false - not.
     */
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


    private static Uri getCaptureImageOutputUri() {
        File folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Oxtor");
        File innerFolder=new File(folder, "image");
        if(!innerFolder.exists())
            innerFolder.mkdirs();
        return Uri.fromFile(new File(innerFolder.getPath(), FileItemUtils.generateFileName(".jpeg")));
    }

    private static Uri getCaptureVideoOutputUri() {
        File folder=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Oxtor");
        File innerFolder=new File(folder, "video");
        if(!innerFolder.exists())
            innerFolder.mkdirs();
        return Uri.fromFile(new File(innerFolder.getPath(), FileItemUtils.generateFileName(".mp4")));
    }


}