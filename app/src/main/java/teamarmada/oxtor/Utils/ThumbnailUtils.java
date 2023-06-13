package teamarmada.oxtor.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import androidx.transition.Slide;

import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.internal.util.HalfSerializer;
import teamarmada.oxtor.R;

public class ThumbnailUtils {

    public static Bitmap generateThumbnail(Context context, File file) {
        String mimeType = getMimeType(file);
        ContentResolver contentResolver = context.getContentResolver();
        if (mimeType != null && mimeType.startsWith("image")) {
            return MediaStore.Images.Thumbnails.getThumbnail(contentResolver, getFileId(file), MediaStore.Images.Thumbnails.MINI_KIND, null);
        } else if (mimeType != null && mimeType.startsWith("video")) {
            return android.media.ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        }  else if (mimeType != null && mimeType.startsWith("audio")) {
            return getAudioThumbnail(context,Uri.parse(file.getAbsolutePath()));
        }  else if (mimeType != null && mimeType.equals("text/plain")) {
            return generateTXTThumbnail(context,file);
        } else  if (mimeType != null && mimeType.equals("application/pdf")) {
            return generatePDFThumbnail(context,file,0);
        }else  if (mimeType != null && mimeType.startsWith("application")) {
            return generateDocumentThumbnail(context,file);
        } else{
            return getDefaultThumbnail(context);
        }
    }

    private static String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
    }

    public static Bitmap generateDocumentThumbnail(Context context, File file) {
        String mimeType = getMimeType(file);
        if (mimeType != null && mimeType.equals("application/pdf")) {
            return generatePDFThumbnail(context,file,0);
        }
        return null;
    }

    public static Bitmap generatePDFThumbnail(Context context,File file, int pageIndex) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);

            PdfRenderer.Page page = pdfRenderer.openPage(pageIndex);

            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            pdfRenderer.close();
            parcelFileDescriptor.close();

            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getDefaultDocumentThumbnail(context);
    }

    public static Bitmap getAudioThumbnail(Context context, Uri audioUri) {
        try(MediaMetadataRetriever retriever=new MediaMetadataRetriever()) {
            retriever.setDataSource(context, audioUri);
            byte[] embeddedPicture = retriever.getEmbeddedPicture();
            if (embeddedPicture != null) {
                return BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getDefaultAudioThumbnail(context);
    }

    public static Bitmap generateTXTThumbnail(Context context, File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            int line;
            while ((line = reader.read()) != -1) {
                content.append(line).append("\n");
            }
            reader.close();
            return createTextThumbnail(context, content.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap createTextThumbnail(Context context, String text) {
        int thumbnailSize = (int) context.getResources().getDimension(R.dimen.text_snippet);
        Bitmap bitmap = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(context.getResources().getDimension(R.dimen.text_size));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawColor(Color.BLACK);
        canvas.drawText(text, 0, 0, paint);
        return bitmap;
    }

    private static Bitmap getDefaultThumbnail(Context context) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_file_present_24);
    }

    private static long getFileId(File file) {
        return file.getAbsolutePath().hashCode();
    }

    private static Bitmap getDefaultAudioThumbnail(Context context) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_audio_file_24);
    }

    private static Bitmap getDefaultDocumentThumbnail(Context context) {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_text_snippet_24);
    }

}

