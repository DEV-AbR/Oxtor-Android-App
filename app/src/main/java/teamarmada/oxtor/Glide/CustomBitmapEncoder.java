package teamarmada.oxtor.Glide;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.EncodeStrategy;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import com.bumptech.glide.util.pool.GlideTrace;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


public class CustomBitmapEncoder implements ResourceEncoder<Bitmap> {

    public static final Option<Integer> COMPRESSION_QUALITY = Option.memory("com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionQuality", 50);
    public static final Option<Bitmap.CompressFormat> COMPRESSION_FORMAT = Option.memory("com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionFormat");
    private static final String TAG =CustomBitmapEncoder.class.getSimpleName();
    private final Context context;

    public CustomBitmapEncoder(Context context) {
        Log.d(TAG, "CustomBitmapEncoder: ");
        this.context=context;
    }

    @Override
    public boolean encode(@NonNull Resource<Bitmap> resource, @NonNull File file, @NonNull Options options) {
        final Bitmap bitmap = resource.get();
        Bitmap.CompressFormat format = getFormat(bitmap, options);
        GlideTrace.beginSectionFormat("encode: [%dx%d] %s", bitmap.getWidth(), bitmap.getHeight(), format);
        try {
            long start = LogTime.getLogTime();
            int quality = options.get(COMPRESSION_QUALITY);
            boolean success = false;
            OutputStream os = null;
            try {
                Uri uri= Uri.fromFile(file);
                os = context.getContentResolver().openOutputStream(uri);
                bitmap.compress(format, quality, os);
                os.close();
                success = true;
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Failed to encode Bitmap", e);
                }
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // Do nothing.
                    }
                }
            }

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(
                        TAG,
                        "Compressed with type: "
                                + format
                                + " of size "
                                + Util.getBitmapByteSize(bitmap)
                                + " in "
                                + LogTime.getElapsedMillis(start)
                                + ", options format: "
                                + options.get(COMPRESSION_FORMAT)
                                + ", hasAlpha: "
                                + bitmap.hasAlpha());
            }
            Log.d(TAG, "encode: "+success);
            return success;
        } finally {
            GlideTrace.endSection();
        }
    }

    private Bitmap.CompressFormat getFormat(Bitmap bitmap, Options options) {
        Bitmap.CompressFormat format = options.get(COMPRESSION_FORMAT);
        if (format != null) {
            return format;
        } else if (bitmap.hasAlpha()) {
            return Bitmap.CompressFormat.PNG;
        } else {
            return Bitmap.CompressFormat.JPEG;
        }
    }

    @NonNull
    @Override
    public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
        return EncodeStrategy.TRANSFORMED;
    }

}