package teamarmada.oxtor.Glide;


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
import com.google.firebase.storage.StreamDownloadTask;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.CipherInputStream;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Utils.AES;

public class GlideImageLoader implements ModelLoader<FileItem, InputStream> {

    private static final String TAG = GlideImageLoader.class.getSimpleName();

    public GlideImageLoader() {}

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull FileItem item, int height, int width, @NonNull Options options) {
        return new LoadData<>(new StorageKey(item), new StorageFetcher(item));
    }

    @Override
    public boolean handles(@NonNull FileItem item) {
        return item.getFileType().contains("image");
    }

    public static class Factory implements ModelLoaderFactory<FileItem, InputStream> {

        public Factory() {
        }

        @NonNull
        @Override
        public ModelLoader<FileItem, InputStream> build(@NonNull MultiModelLoaderFactory factory) {
            return new GlideImageLoader();
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

    private static class StorageFetcher implements DataFetcher<InputStream>, StreamDownloadTask.StreamProcessor {

        private final Executor executor;
        private final FileItem fileItem;
        private InputStream inputStream;
        private StreamDownloadTask task;

        public StorageFetcher(FileItem fileItem) {
            this.fileItem=fileItem;
            inputStream=null;
            executor= Executors.newCachedThreadPool();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull final DataCallback<? super InputStream> callback) {
            try{
                FirebaseStorage.getInstance().getReference().child(fileItem.getStorageReference()).getStream(this)
                .addOnSuccessListener(executor,taskSnapshot -> {
                    if(fileItem.getFileType().contains("image")){
                        try {
                            if(fileItem.isEncrypted()){
                                inputStream=new CipherInputStream(taskSnapshot.getStream(), AES.getDecryptionCipher(fileItem,new AuthRepository().getProfileItem()));
                            }else{
                                inputStream=new BufferedInputStream(taskSnapshot.getStream(),(int)taskSnapshot.getTotalByteCount());
                            }
                            callback.onDataReady(inputStream);
                        } catch (Exception e) {
                            callback.onLoadFailed(e);
                        }
                    }
                    else callback.onLoadFailed(new Exception("Found item is not image"));
                }).addOnFailureListener(executor, callback::onLoadFailed);
            }
            catch (Exception e){
                callback.onLoadFailed(new Exception("File does not exist anymore"));
            }
        }

        @Override
        public void cleanup() {
            if(inputStream!=null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void cancel() {
            inputStream=null;
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }


        @Override
        public void doInBackground(@NonNull StreamDownloadTask.TaskSnapshot state, @NonNull InputStream stream) throws IOException {
            if(task.isSuccessful()){
                inputStream.close();
            }
        }

    }

}
