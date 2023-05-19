package teamarmada.oxtor.Utils;

import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Model.ProfileItem.TO_ENCRYPT;


import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.CipherInputStream;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.R;

public class FileItemUtils {

    public static final String TAG= FileItemUtils.class.getSimpleName();

    public static long ONE_KILOBYTE=1024;
    public static long ONE_MEGABYTE=ONE_KILOBYTE*1024;
    public static long ONE_GIGABYTE=ONE_MEGABYTE*1024;
    public static long TWELVE_HOURS=43200000;
    public static long TWENTY_FOUR_HOURS=TWELVE_HOURS*2;
    public static final String IMAGES="images/";
    public static final String VIDEOS="videos/";
    public static final String AUDIOS="audios/";
    public static final String FILES="files/";

    public static String byteToString(long enterByte){
        String size;
        if(enterByte<ONE_KILOBYTE){
            size=enterByte+" bytes";
        }
        else if(enterByte<ONE_MEGABYTE){
            enterByte/=ONE_KILOBYTE;
            size=enterByte+" KB";
        }
        else if(enterByte<ONE_GIGABYTE){
            enterByte/=ONE_MEGABYTE;
            size=enterByte+" MB";
        }
        else {
            enterByte/=ONE_GIGABYTE;
            size=enterByte+" GB";
        }
            return size;
    }

    private static String getTimeString(Date timestamp){
        Calendar calendar = Calendar.getInstance();
        if(timestamp==null)return null;
        calendar.setTimeInMillis(timestamp.getTime());
        return DateFormat.format("hh:mm aa", calendar).toString();
    }

    private static String getDateString(Date timestamp){
        Calendar calendar = Calendar.getInstance();
        if(timestamp==null) return null;
        calendar.setTimeInMillis(timestamp.getTime());
        return DateFormat.format("dd-mm-yyyy", calendar).toString();
    }

    public static String getTimestampString(Date postDate){
        Calendar calendar=Calendar.getInstance();
        int currentDay=calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTime(postDate);
        int postDay=calendar.get(Calendar.DAY_OF_YEAR);
        if(currentDay==postDay)
            return "Today "+getTimeString(postDate);
        else if(postDay-currentDay==1)
            return "Yesterday "+getTimeString(postDate);
        else
            return getTimeString(postDate)+"  "+getDateString(postDate);
    }

    public static String getTimestampString(long timeStamp){
        return getTimestampString(new Date(timeStamp));
    }

    public static String getFileTypeString(String extension){
        if(extension.contains("image")) return IMAGES;
        else if(extension.contains("audio")) return AUDIOS;
        else if(extension.contains("video")) return VIDEOS;
        else if(extension.contains("file")) return FILES;
        else return extension;
    }

    public static long getSizeLong(Context context, Uri path){
        Cursor returnCursor =
            context.getContentResolver().query(path, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long l= returnCursor.getLong(sizeIndex);
        returnCursor.close();
        return l;
    }

    public static String getNameString(Context context,Uri path){
        Cursor returnCursor =  context.getContentResolver().query(path, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String l= returnCursor.getString(sizeIndex);
        returnCursor.close();
        return l;
    }

    public static String generateFileNameWithoutExtension() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            return "OXT_" + timeStamp;
    }

    public static void loadPhoto(FileItem fileItem,@NonNull ImageView imageView) {
        String s= FileItemUtils.getFileTypeString(fileItem.getFileType());
        switch (s) {
            case "audio/":
                imageView.setImageResource(R.drawable.ic_baseline_audio_file_24);
                break;
            case "video/":
                imageView.setImageResource(R.drawable.ic_baseline_ondemand_video_24);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_baseline_file_present_24);
                break;
        }
    }

    public static FileItem getFileItemFromPath(Context context,Uri path){
        FileItem fileItem=new FileItem();
        String type=context.getContentResolver().getType(path);
        String name;
        try {
            name = getNameString(context, path);
        }
        catch (Exception e){
            name=FilenameUtils.getName(path.toString());
        }
        fileItem.setFileExtension(FilenameUtils.getExtension(name))
                .setFileType(type)
                .setFilePath(path.toString())
                .setFileName(name)
                .setEncrypted(false)
                .setUid(UUID.randomUUID().toString())
                .setFileSize(getSizeLong(context,path));
        return fileItem;
    }


    public static File createDownloadFile(FileItem fileItem) throws Exception {
        File folder=new File(Environment.getExternalStorageDirectory(),"Oxtor/Download");
        File innerFolder=new File(folder,getFileTypeString(fileItem.getFileType()));
        if(!innerFolder.exists())
            innerFolder.mkdirs();
        String nameWithExtension=fileItem.getFileName();
        File output=new File(innerFolder,nameWithExtension);
        boolean created= output.createNewFile();
        if(created)
            return output;
        else
            throw new Exception("Couldn't create said file");
    }

    public static File createUploadFile(String type,String extension) throws Exception {
        Log.d(TAG, "createUploadFile: ");
        File folder=new File(Environment.getExternalStorageDirectory(),"Oxtor/Upload");
        File innerFolder=new File(folder,type);
        if(!innerFolder.exists())
            innerFolder.mkdirs();
        String nameWithExtension=generateFileNameWithoutExtension()+extension;
        File output=new File(innerFolder,nameWithExtension);
        boolean created= output.createNewFile() && output.setWritable(true);
        if(created)
            return output;
        else
            throw new Exception("Couldn't create said file");
    }

//    public static byte[] readIntoByteArray(FileItem item, ProfileItem profileItem,Context context) throws Exception {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
//        Uri uri=Uri.parse(item.getFilePath());
//        byte[] bytes=new byte[item.getFileSize().intValue()];
//        ByteArrayOutputStream outputStream=new ByteArrayOutputStream(bytes.length);
//        File file=new File(uri.toString());
//        try (FileReader fileReader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(fileReader)) {
//            int read;
//            while ((read = bufferedReader.read()) != -1) {
//                outputStream.write(bytes, 0, read);
//            }
//        } catch (Exception e) {
//            InputStream inputStream = context.getContentResolver().openInputStream(uri);
//            try {
//                inputStream = new BufferedInputStream(inputStream, item.getFileSize().intValue());
//                outputStream = new ByteArrayOutputStream(bytes.length);
//                int read;
//                while ((read = inputStream.read(bytes)) != -1) {
//                    outputStream.write(bytes, 0, read);
//                }
//            } finally {
//                outputStream.close();
//                inputStream.close();
//            }
//        }
//        if(sharedPreferences.getBoolean(TO_ENCRYPT,false))
//            return AES.encrypt(outputStream.toByteArray(),item,profileItem);
//        else
//            return outputStream.toByteArray();
//    }

//    public static InputStream uploadFromInputStream(FileItem item, ProfileItem profileItem, Context context) throws Exception {
//        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
//        Uri uri = Uri.parse(item.getFilePath());
//        InputStream inputStream = context.getContentResolver().openInputStream(uri);
//        if (sharedPreferences.getBoolean(TO_ENCRYPT, false))
//            return new CipherInputStream(inputStream, AES.getEncryptCipher(item, profileItem));
//
//        int bufferSize = calculateBufferSize(context, item.getFileSize());
//        byte[] buffer = new byte[bufferSize];
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
//            int bytesRead;
//            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//        }catch (Exception e){
//            return inputStream;
//        }
//        finally {
//            outputStream.close();
//            inputStream.close();
//        }
//        return new ByteArrayInputStream(outputStream.toByteArray());
//    }

//    public static InputStream downloadFromInputStream(FileItem fileItem, ProfileItem profileItem, Context context, InputStream inputStream) throws Exception {
//        if (fileItem.isEncrypted()) {
//            return new CipherInputStream(inputStream, AES.getDecryptionCipher(fileItem, profileItem));
//        } else {
//            int bufferSize = calculateBufferSize(context, fileItem.getFileSize());
//            byte[] buffer = new byte[bufferSize];
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            try {
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//            }catch (Exception e){
//                return inputStream;
//            }
//            finally {
//                outputStream.close();
//                inputStream.close();
//            }
//            return new ByteArrayInputStream(outputStream.toByteArray());
//        }
//    }

    public static int calculateBufferSize(Context context, long fileSize) {
        int maxBufferSize = 8192; // 8KB (adjust as needed)
        int minBufferSize = 1024; // 1KB (adjust as needed)

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = powerManager.isInteractive();

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int availableMemory = isScreenOn ? activityManager.getMemoryClass() * 1024 * 1024 : activityManager.getLargeMemoryClass() * 1024 * 1024;

        // Check if available memory is below a certain threshold
        int minimumRequiredMemory = 128 * 1024 * 1024; // 128MB (adjust as needed)
        if (availableMemory < minimumRequiredMemory) {
            return minBufferSize;
        }

        // Calculate the buffer size based on available memory and file size
        int bufferSize = (int) Math.min(Math.max(availableMemory / 4, fileSize / 100), maxBufferSize);
        return Math.max(bufferSize, minBufferSize);
    }

}
