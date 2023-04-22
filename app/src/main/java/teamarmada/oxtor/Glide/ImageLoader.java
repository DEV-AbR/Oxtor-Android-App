package teamarmada.oxtor.Glide;


import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.google.firebase.storage.FirebaseStorage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Utils.AES;

public class ImageLoader implements ModelLoader<FileItem, Bitmap> {

    private static final String TAG = ImageLoader.class.getSimpleName();
    private final Context context;

    public ImageLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<Bitmap> buildLoadData(@NonNull FileItem item, int height, int width, @NonNull Options options) {

        return new LoadData<>(new StorageKey(item), new StorageFetcher(item,context));
    }

    @Override
    public boolean handles(@NonNull FileItem item) {
        return true;
    }

    /**
     * Factory to create {@link ImageLoader}.
     */
    public static class Factory implements ModelLoaderFactory<FileItem,Bitmap> {
        private final Context context;

        public Factory(Context context) {
            this.context = context;
        }

        @NonNull
        @Override
        public ModelLoader<FileItem, Bitmap> build(@NonNull MultiModelLoaderFactory factory) {
            return new ImageLoader(context);
        }

        @Override
        public void teardown() {
            // No-op
        }

    }

    private static class StorageKey implements Key {

        private final FileItem fileItem;

        public StorageKey(FileItem item) {
            fileItem=item;
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest digest) {
            digest.update(fileItem.getStorageReference().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StorageKey key = (StorageKey) o;
            return fileItem.equals(key.fileItem);
        }

        @Override
        public int hashCode() {
            return fileItem.hashCode();
        }
    }

    private static class StorageFetcher implements DataFetcher<Bitmap> {

        private final FileItem fileItem;
        private Bitmap bitmap;
        private final Context context;
        private final DownloadManager dm;
        private final BitmapFactory.Options options;
        private long downloadID;
        private DataSource dataSource;
        private final Executor executor;

        public StorageFetcher(FileItem fileItem, Context context) {
            this.fileItem=fileItem;
            this.context=context;
            dataSource=DataSource.REMOTE;
            dm=context.getSystemService(DownloadManager.class);
            options=new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            executor= Executors.newSingleThreadExecutor();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull final DataCallback<? super Bitmap> callback) {
            FirebaseStorage.getInstance().getReference().child(fileItem.getStorageReference())
                    .getStream().addOnSuccessListener(executor,taskSnapshot -> {
                        if(fileItem.getFileType().contains("image")){
                            Log.d(TAG, "loadData: image found: "+fileItem.getFileName());
                            if(fileItem.isEncrypted()){
                                try {
                                    Log.d(TAG, "loadData: image is encrypted");
                                    byte[] bytes=new byte[fileItem.getFileSize().intValue()];
                                    InputStream is=taskSnapshot.getStream();
                                    is.read(bytes);
                                    bytes=AES.decrypt(bytes,fileItem,new AuthRepository().getProfileItem());
                                    bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                                    callback.onDataReady(bitmap);
                                } catch (Exception e) {
                                    callback.onLoadFailed(e);
                                }
                            }
                            else {
                                Log.d(TAG, "loadData: image is not encrypted");
                                callback.onDataReady(BitmapFactory.decodeStream(taskSnapshot.getStream()));
                            }
                        }
                        else callback.onLoadFailed(new Exception("Found item is not image"));
                    }).addOnFailureListener(executor,callback::onLoadFailed);

        }


//        private void downloadViaDownloadManager(@NonNull final DataCallback<? super Bitmap> callback) throws Exception {
//            Log.d(TAG, "downloadViaDownloadManager: "+fileItem);
//            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(fileItem.getDownloadUrl()));
//            request.setTitle("Downloading...").setDescription(fileItem.getFileName()).setMimeType(fileItem.getFileExtension())
//            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "Oxtor/"+fileItem.getFileType()+"/")
//            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN).setVisibleInDownloadsUi(false);
//            downloadID= dm.enqueue(request);
//            AtomicBoolean downloading=new AtomicBoolean(true);
//            Cursor cursor = dm.query(new DownloadManager.Query().setFilterById(downloadID));
//            while (downloading.get()) {
//                cursor.moveToFirst();
//                @SuppressLint("Range")
//                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                    switch (status) {
//                        case DownloadManager.STATUS_FAILED:
//                            downloading.set(false);
//                            cursor.close();
////                            try{
////                                callback.onDataReady(getBitmapFromRes());
////                            }catch (Exception e){
////                                callback.onLoadFailed(e);
////                            }
//                            callback.onLoadFailed(new Exception("Couldn't download the item"));
//                            break;
//                        case DownloadManager.STATUS_SUCCESSFUL:
//                            downloading.set(false);
//                            byte[] bytes = new byte[fileItem.getFileSize().intValue()];
//                            Uri uri= dm.getUriForDownloadedFile(downloadID);
//                            FileInputStream fis=new FileInputStream(uri.toString());
//                            fis.read(bytes);
//                            fis.close();
//                            try{
//                                bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
//                                callback.onDataReady(bitmap);
//                            }catch (Exception e){
////                                try {
////                                    callback.onDataReady(getBitmapFromRes());
////                                }catch (Exception ex){
////                                    callback.onLoadFailed(ex);
////                                }
//                                callback.onLoadFailed(e);
//                            }
//                            cursor.close();
//                            break;
//                    }
//            }
//        }
//
//        public Bitmap getBitmapFromRes() throws Exception {
//            Log.d(TAG, "getBitmapFromRes: "+fileItem);
//            dataSource=DataSource.LOCAL;
//            @DrawableRes int res;
//            switch (fileItem.getFileType()){
//                default: case "file/": res= R.drawable.ic_baseline_file_present_24;
//                    break;
//                case "image/":
//                    res=R.drawable.ic_baseline_photo_24;
//                    break;
//                case "video/":
//                    res=R.drawable.ic_baseline_ondemand_video_24;
//                    break;
//                case "audio/":
//                    res=R.drawable.ic_baseline_audio_file_24;
//                    break;
//            }
//
//            Drawable drawable= AppCompatResources.getDrawable(context,res);
//            try {
//                Bitmap bitmap;
//                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//                Canvas canvas = new Canvas(bitmap);
//                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
//                drawable.draw(canvas);
//                return bitmap;
//            } catch (OutOfMemoryError e) {
//                // Handle the error
//                throw new Exception("Couldn't generate bitmap from vector drawable");
//            }
//        }


        @Override
        public void cleanup() {
            try {
              dm.remove(downloadID);
            } catch (Exception e) {
                Log.w(TAG, "download manager throwing exception", e);
            }
        }

        @Override
        public void cancel() {
            cleanup();
        }

        @NonNull
        @Override
        public Class<Bitmap> getDataClass() {
            return Bitmap.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return dataSource;
        }


    }

}