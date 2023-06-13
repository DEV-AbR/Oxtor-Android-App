package teamarmada.oxtor.Glide;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.InputStream;
import java.nio.ByteBuffer;

import teamarmada.oxtor.Model.FileItem;
import teamarmada.oxtor.R;

@GlideModule
public class GlideImageModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDefaultRequestOptions(() -> new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.ic_baseline_photo_24)
                        .error(R.drawable.ic_baseline_error_24)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                )
               .addGlobalRequestListener(new RequestListener<Object>() {
                   @Override
                   public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Object> target, boolean isFirstResource) {
                       if(e!=null)
                           e.printStackTrace();
                       return e!=null;
                   }
                   @Override
                   public boolean onResourceReady(Object resource, Object model, Target<Object> target, DataSource dataSource, boolean isFirstResource) {
                       return false;
                   }
               }).setIsActiveResourceRetentionAllowed(true);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry
                .prepend(FileItem.class, InputStream.class,new GlideImageLoader.Factory())
                .append(FileItem.class, ByteBuffer.class,new NewImageLoader.Factory(context))
                .append(FileItem.class,Bitmap.class,new ImageLoader.Factory(context))
                .append(InputStream.class,new StreamEncoder(context))
                .append(Bitmap.class,new CustomBitmapEncoder(context));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

}
