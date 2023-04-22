package teamarmada.oxtor.Glide;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An {@link Encoder} that can write an {@link InputStream} to disk.
 */
public class StreamEncoder implements Encoder<InputStream> {
    private static final String TAG = "StreamEncoder";
    private final Context context;

    public StreamEncoder(Context context) {
        this.context=context;
    }

    @Override
    public boolean encode(@NonNull InputStream data, @NonNull File file, @NonNull Options options) {
        byte[] buffer=new byte[(int)file.length()];
        try {
            data.read(buffer);
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to read from InputStream", e);
            }
        }
        boolean success = false;
        OutputStream os = null;
        try {
            Uri uri=Uri.parse(file.getPath());
            os = context.getContentResolver().openOutputStream(uri);
            int read;
            while ((read = data.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.close();
            success = true;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to encode data onto the OutputStream", e);
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
        return success;
    }

}