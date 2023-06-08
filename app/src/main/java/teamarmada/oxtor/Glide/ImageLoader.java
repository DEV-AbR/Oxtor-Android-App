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
                                    byte[] bytes=new byte[fileItem.getFileSize().intValue()];
                                    InputStream is=taskSnapshot.getStream();
                                    is.read(bytes);
                                    bytes=AES.decrypt(bytes,fileItem,new AuthRepository().getProfileItem());
                                    bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                                    callback.onDataReady(bitmap);
                                    is.close();
                                } catch (Exception e) {
                                    callback.onLoadFailed(e);
                                }
                            }
                            else {
                                callback.onDataReady(BitmapFactory.decodeStream(taskSnapshot.getStream()));
                            }
                        }
                        else callback.onLoadFailed(new Exception("Found item is not image"));
                    }).addOnFailureListener(executor,callback::onLoadFailed);

        }

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