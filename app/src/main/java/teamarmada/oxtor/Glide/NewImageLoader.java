package teamarmada.oxtor.Glide;


import static teamarmada.oxtor.Main.MainActivity.PREFS;
import static teamarmada.oxtor.Model.ProfileItem.TO_ENCRYPT;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.Model.ProfileItem;
import teamarmada.oxtor.Repository.AuthRepository;
import teamarmada.oxtor.Utils.AES;

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
            if(fileItem.getDownloadUrl()!=null) {
                FirebaseStorage.getInstance().getReference().child(fileItem.getStorageReference()).getStream(this).addOnSuccessListener(executor, taskSnapshot -> {
                    if (fileItem.getFileType().contains("image") && fileItem.getDownloadUrl() != null) {
                        try {
                            byteBuffer = ByteBuffer.wrap(readIntoByteArray(fileItem, new AuthRepository().getProfileItem(), context));
                            callback.onDataReady(byteBuffer);
                        } catch (Exception e) {
                            callback.onLoadFailed(e);
                        }
                    } else callback.onLoadFailed(new Exception("Found item is not image"));
                }).addOnFailureListener(executor, callback::onLoadFailed);
            }
            else{
                callback.onLoadFailed(new Exception("File does not exist anymore"));
            }
        }

        public byte[] readIntoByteArray(FileItem item, ProfileItem profileItem, Context context) throws Exception {
            SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            Uri uri=Uri.parse(item.getFilePath());
            byte[] bytes=new byte[item.getFileSize().intValue()];
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream(bytes.length);
            File file=new File(uri.toString());
            try (FileReader fileReader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                int read;
                while ((read = bufferedReader.read()) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } catch (Exception e) {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                try {
                    inputStream = new BufferedInputStream(inputStream, item.getFileSize().intValue());
                    outputStream = new ByteArrayOutputStream(bytes.length);
                    int read;
                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                } finally {
                    outputStream.close();
                    inputStream.close();
                }
            }
            if(sharedPreferences.getBoolean(TO_ENCRYPT,false))
                return AES.encrypt(outputStream.toByteArray(),item,profileItem);
            else
                return outputStream.toByteArray();
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
