package teamarmada.oxtor.Glide;


import android.content.Context;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Utils.FileItemUtils;

public class NewImageLoader implements ModelLoader<FileItem, ByteBuffer> {
    private static final String TAG = GlideImageLoader.class.getSimpleName();
    private final Context context;
    public NewImageLoader(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull FileItem item, int height, int width, @NonNull Options options) {
        return new LoadData<>(new StorageKey(item), new StorageFetcher(item,context));
    }

    @Override
    public boolean handles(@NonNull FileItem item) {
        return item.getFileType().contains("image");
    }

    public static class Factory implements ModelLoaderFactory<FileItem, ByteBuffer> {
        private final Context context;
        public Factory(Context context) {
            this.context=context;
        }

        @NonNull
        @Override
        public ModelLoader<FileItem, ByteBuffer> build(@NonNull MultiModelLoaderFactory factory) {
            return new NewImageLoader(context);
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

    private static class StorageFetcher implements DataFetcher<ByteBuffer>, StreamDownloadTask.StreamProcessor {

        private final Executor executor;
        private final FileItem fileItem;
        private ByteBuffer byteBuffer;
        private StreamDownloadTask task;
        private final Context context;

        public StorageFetcher(FileItem fileItem, Context context) {
            this.fileItem=fileItem;
            this.context=context;
            executor= Executors.newSingleThreadExecutor();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull final DataCallback<? super ByteBuffer> callback) {
            task=FirebaseStorage.getInstance().getReference().child(fileItem.getStorageReference()).getStream(this);
            task.addOnSuccessListener(executor,taskSnapshot -> {
                if(fileItem.getFileType().contains("image")){
                    try {
//                        if (fileItem.isEncrypted()) {
//                            taskSnapshot.getStream().read(bytes);
//                            byteBuffer = ByteBuffer.wrap(AES.decrypt(bytes, fileItem, new AuthRepository().getProfileItem()));
//                        } else {
//                            taskSnapshot.getStream().read(bytes);
//                        }
                        byteBuffer=ByteBuffer.wrap(FileItemUtils.readIntoByteArray(fileItem,new AuthRepository().getProfileItem(),context));
                        callback.onDataReady(byteBuffer);
                    }catch (Exception e){
                        callback.onLoadFailed(e);
                    }
                }
                else callback.onLoadFailed(new Exception("Found item is not image"));
            }).addOnFailureListener(executor, callback::onLoadFailed);

        }

        @Override
        public void cleanup() {
            if(byteBuffer!=null) {
                byteBuffer.clear();
            }
        }

        @Override
        public void cancel() {
            cleanup();
            byteBuffer=null;
        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }


        @Override
        public void doInBackground(@NonNull StreamDownloadTask.TaskSnapshot state, @NonNull InputStream stream) throws IOException {
            if(task.isSuccessful())
                stream.close();
        }
    }

}
