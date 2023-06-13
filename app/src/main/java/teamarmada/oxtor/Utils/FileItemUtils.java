package teamarmada.oxtor.Utils;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;
public class FileItemUtils {

    public static final String TAG = FileItemUtils.class.getSimpleName();

    public static final long ONE_KILOBYTE = 1024;
    public static final long ONE_MEGABYTE = ONE_KILOBYTE * 1024;
    public static final long ONE_GIGABYTE = ONE_MEGABYTE * 1024;
    public static final long TWELVE_HOURS = 43200000;
    public static final long TWENTY_FOUR_HOURS = TWELVE_HOURS * 2;
    public static final String IMAGES_FOLDER = "images/";
    public static final String VIDEOS_FOLDER = "videos/";
    public static final String AUDIOS_FOLDER = "audios/";
    public static final String FILES_FOLDER = "files/";

    public static String formatByteSize(long bytes) {
        String size;
        if (bytes < ONE_KILOBYTE) {
            size = bytes + " bytes";
        } else if (bytes < ONE_MEGABYTE) {
            long kilobytes = bytes / ONE_KILOBYTE;
            size = kilobytes + " KB";
        } else if (bytes < ONE_GIGABYTE) {
            long megabytes = bytes / ONE_MEGABYTE;
            size = megabytes + " MB";
        } else {
            long gigabytes = bytes / ONE_GIGABYTE;
            size = gigabytes + " GB";
        }
        return size;
    }

    private static String getTimeString(Date timestamp) {
        Calendar calendar = Calendar.getInstance();
        if (timestamp == null) {
            return null;
        }
        calendar.setTime(timestamp);
        return DateFormat.format("hh:mm aa", calendar).toString();
    }

    private static String getDateString(Date timestamp) {
        Calendar calendar = Calendar.getInstance();
        if (timestamp == null) {
            return null;
        }
        calendar.setTime(timestamp);
        return DateFormat.format("dd-MM-yyyy", calendar).toString();
    }

    public static String getFormattedTimestamp(Date postDate) {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(postDate);
        int postDay = calendar.get(Calendar.DAY_OF_YEAR);
        if (currentDay == postDay) {
            return "Today"+" "+getTimeString(postDate);
        } else if (postDay - currentDay == 1) {
            return "Yesterday"+" "+getTimeString(postDate);
        } else {
            return getDateString(postDate)+" "+getTimeString(postDate);
        }
    }

    public static String getFileTypeFolder(String extension) {
        if (extension.contains("image")) {
            return IMAGES_FOLDER;
        } else if (extension.contains("audio")) {
            return AUDIOS_FOLDER;
        } else if (extension.contains("video")) {
            return VIDEOS_FOLDER;
        } else if (extension.contains("file")) {
            return FILES_FOLDER;
        } else {
            return extension;
        }
    }

    public static long getFileSize(Context context, Uri fileUri) {
        Cursor returnCursor = context.getContentResolver().query(fileUri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long fileSize = returnCursor.getLong(sizeIndex);
        returnCursor.close();
        return fileSize;
    }

    public static String getFileName(Context context, Uri fileUri) {
        Cursor returnCursor = context.getContentResolver().query(fileUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String fileName = returnCursor.getString(nameIndex);
        returnCursor.close();
        return fileName;
    }

    public static void setFileItemIcon(FileItem fileItem, @NonNull ImageView imageView) {
        String fileTypeFolder = FileItemUtils.getFileTypeFolder(fileItem.getFileType());
        switch (fileTypeFolder) {
            case AUDIOS_FOLDER:
                imageView.setImageResource(R.drawable.ic_baseline_audio_file_24);
                break;
            case VIDEOS_FOLDER:
                imageView.setImageResource(R.drawable.ic_baseline_ondemand_video_24);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_baseline_file_present_24);
                break;
        }
    }

    public static FileItem getFileItemFromPath(Context context, Uri fileUri) {
        String fileType = context.getContentResolver().getType(fileUri);
        String fileName;
        try {
            fileName = getFileName(context, fileUri);
        } catch (Exception e) {
            fileName = FilenameUtils.getName(fileUri.toString());
        }
        return new FileItem(null,
                null,
                fileUri.toString(),
                fileName,
                UUID.randomUUID().toString(),
                fileType,
                FilenameUtils.getExtension(fileName),
                getFileSize(context, fileUri),null);
    }

    public static File createNewDownloadFile(FileItem fileItem) {
        File folder = new File(Environment.getExternalStorageDirectory(), "Oxtor/Download");
        File innerFolder = new File(folder, getFileTypeFolder(fileItem.getFileType()));
        if (!innerFolder.exists()) {
            innerFolder.mkdirs();
        }
        String fileNameWithExtension = fileItem.getFileName();
        File output = new File(innerFolder, fileNameWithExtension);
        boolean created = false;
        try {
            created = output.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!created) {
            File file1= new File(Environment.getDownloadCacheDirectory(), fileNameWithExtension);
            try{
            if(Objects.requireNonNull(file1.getParentFile()).mkdirs())
                if(file1.createNewFile())
                    return file1;
            else
                return null;
            }catch (Exception e){
                return null;
            }
        }
        return output;
    }

    public static int calculateBufferSize(Context context, long fileSize) throws Exception {
        int maxBufferSize = 8192; // 8KB (adjust as needed)
        int minBufferSize = 1024; // 1KB (adjust as needed)

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = powerManager.isInteractive();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int availableMemory = isScreenOn ? activityManager.getMemoryClass() * 1024 * 1024 : activityManager.getLargeMemoryClass() * 1024 * 1024;

        // Check if available memory is below the file size
        if (availableMemory < fileSize) {
            throw new Exception("Insufficient memory to calculate buffer size");
        }

        // Calculate the buffer size based on available memory and file size
        int bufferSize = (int) Math.min(Math.max(availableMemory / 4, fileSize / 100), maxBufferSize);
        return Math.max(bufferSize, minBufferSize);
    }
}
