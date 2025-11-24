package com.example.learnify;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileExtractor {

    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public FileExtractor(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        PDFBoxResourceLoader.init(context);
    }

    public void extractText(Uri uri, ExtractionCallback callback) {
        executor.execute(() -> {
            try {
                if (getFileSize(uri) > MAX_FILE_SIZE) {
                    mainHandler.post(() -> callback.onError(new Exception("File is too large (>10MB).")));
                    return;
                }

                String mimeType = context.getContentResolver().getType(uri);
                if (mimeType == null) {
                    String name = getFileName(uri);
                    if (name.endsWith(".pdf")) mimeType = "application/pdf";
                    else if (name.endsWith(".txt")) mimeType = "text/plain";
                    else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) mimeType = "image/jpeg";
                }

                if (mimeType != null) {
                    if (mimeType.equals("application/pdf")) {
                        extractPdf(uri, callback);
                    } else if (mimeType.startsWith("image/")) {
                        extractImage(uri, callback);
                    } else if (mimeType.equals("text/plain")) {
                        extractTxt(uri, callback);
                    } else {
                        mainHandler.post(() -> callback.onError(new Exception("Unsupported file type. Please use PDF, Image, or Text.")));
                    }
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Could not determine file type.")));
                }

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private void extractPdf(Uri uri, ExtractionCallback callback) {
        PDDocument document = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            document = PDDocument.load(inputStream);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(30);

            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                mainHandler.post(() -> callback.onError(new Exception("PDF appears empty or is a scan. Try Image option.")));
            } else {
                mainHandler.post(() -> callback.onSuccess(text.trim()));
            }
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError(e));
        } finally {
            try {
                if (document != null) document.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void extractTxt(Uri uri, ExtractionCallback callback) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            mainHandler.post(() -> callback.onSuccess(text.toString().trim()));
        } catch (Exception e) {
            mainHandler.post(() -> callback.onError(e));
        }
    }

    private void extractImage(Uri uri, ExtractionCallback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, uri);

            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String result = visionText.getText();
                        if (result.isEmpty()) {
                            callback.onError(new Exception("No text found in image."));
                        } else {
                            callback.onSuccess(result);
                        }
                    })
                    .addOnFailureListener(callback::onError);
        } catch (IOException e) {
            mainHandler.post(() -> callback.onError(new Exception("Failed to load image: " + e.getMessage())));
        }
    }

    private long getFileSize(Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private String getFileName(Uri uri) {
        String result = "";
        if (Objects.equals(uri.getScheme(), "content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) result = cursor.getString(index);
                }
            }
        }
        return result.toLowerCase();
    }

    public interface ExtractionCallback {
        void onSuccess(String text);
        void onError(Throwable t);
    }
}