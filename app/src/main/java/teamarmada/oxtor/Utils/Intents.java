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

    public static Intent getMediaChooserIntent(Context context, CharSequence title) {
        List<Intent> allIntents = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        if (!isExplicitCameraPermissionRequired(context) ) {
            allIntents.addAll(getImageCaptureIntents(packageManager));
            allIntents.addAll(getVideoCaptureIntents(packageManager));
        }

//        List<Intent> videoIntents=getVideoGalleryIntents(packageManager,Intent.ACTION_GET_CONTENT);
//        if(videoIntents.isEmpty()) {
//            videoIntents=getVideoGalleryIntents(packageManager,Intent.ACTION_PICK);
//        }
//        allIntents.addAll(videoIntents);
//
//        List<Intent> imageIntents=getImageGalleryIntents(packageManager,Intent.ACTION_GET_CONTENT);
//        if(videoIntents.isEmpty()) {
//            imageIntents=getImageGalleryIntents(packageManager,Intent.ACTION_PICK);
//        }
//        allIntents.addAll(imageIntents);
//
//        List<Intent> fileIntents=getContentIntents(packageManager,Intent.ACTION_GET_CONTENT);
//        if(fileIntents.isEmpty()) {
//            fileIntents=getContentIntents(packageManager,Intent.ACTION_PICK);
//        }
//        allIntents.addAll(fileIntents);

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

    private static List<Intent> getImageCaptureIntents(PackageManager packageManager) {
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

    private static List<Intent> getVideoCaptureIntents(PackageManager packageManager){
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

    private static List<Intent> getImageGalleryIntents(PackageManager packageManager, String action) {
        List<Intent> intents = new ArrayList<>();
        Intent galleryIntent = action.equals(Intent.ACTION_GET_CONTENT)
                ? new Intent(action)
                : new Intent(action, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intents.add(intent);
        }
        return intents;
    }

    private static List<Intent> getVideoGalleryIntents(PackageManager packageManager, String action) {
        List<Intent> intents = new ArrayList<>();
        Intent galleryIntent = action.equals(Intent.ACTION_GET_CONTENT)
                ? new Intent(action)
                : new Intent(action, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("video/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intents.add(intent);
        }
        return intents;
    }

    private static List<Intent> getContentIntents(PackageManager packageManager, String action) {
        List<Intent> intents = new ArrayList<>();
        Intent galleryIntent = action.equals(Intent.ACTION_GET_CONTENT)
                ? new Intent(action)
                : new Intent(action, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("*/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intents.add(intent);
        }
        return intents;
    }

    private static boolean isExplicitCameraPermissionRequired(Context context) {
        return hasPermissionInManifest(context, "android.permission.CAMERA") && context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
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


    private static Uri getCaptureImageOutputUri() {
        String name= FileItemUtils.generateFileName(".jpeg");
        try {
            return Uri.fromFile(FileItemUtils.createUploadFile(name));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Uri getCaptureVideoOutputUri() {
        String name= FileItemUtils.generateFileName(".mp4");
        try {
            return Uri.fromFile(FileItemUtils.createUploadFile(name));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}