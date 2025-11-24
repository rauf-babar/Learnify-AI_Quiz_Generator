package com.example.learnify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeHelper {

    private static final String TAG = "YouTubeHelper";

    private static final String RAPID_API_KEY = BuildConfig.RAPID_API_KEY;
    private static final String RAPID_API_HOST = BuildConfig.RAPID_API_HOST;

    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public YouTubeHelper() {
        this.client = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getTranscript(String videoUrl, TranscriptCallback callback) {
        executor.execute(() -> {
            String videoId = extractVideoId(videoUrl);
            if (videoId == null) {
                postError(callback, "Invalid YouTube URL.");
                return;
            }
            fetchTranscriptData(videoId, null, callback);
        });
    }

    private void fetchTranscriptData(String videoId, String params, TranscriptCallback callback) {
        try {
            String url = "https://" + RAPID_API_HOST + "/get_transcript?id=" + videoId;
            if (params != null && !params.isEmpty()) {
                url += "&params=" + params;
            }

            Log.d(TAG, "Requesting: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("x-rapidapi-key", RAPID_API_KEY)
                    .addHeader("x-rapidapi-host", RAPID_API_HOST)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 403 || response.code() == 429) {
                        postError(callback, "Server busy. Please try again later.");
                    } else {
                        postError(callback, "Captions unavailable for this video.");
                    }
                    return;
                }

                String jsonResponse = response.body().string();

                if (jsonResponse.trim().startsWith("[")) {
                    processSuccessArray(jsonResponse, videoId, callback);
                    return;
                }

                JSONObject root = new JSONObject(jsonResponse);

                if (root.has("transcript")) {
                    JSONArray array = root.getJSONArray("transcript");
                    processTranscriptArray(array, videoId, callback);
                    return;
                }

                if (params == null && root.has("languageMenu")) {
                    JSONArray menu = root.getJSONArray("languageMenu");
                    String foundParams = null;

                    for (int i = 0; i < menu.length(); i++) {
                        JSONObject item = menu.getJSONObject(i);
                        String title = item.optString("title", "");

                        if (title.contains("English")) {
                            foundParams = item.optString("params", null);
                            break;
                        }
                    }

                    if (foundParams == null && menu.length() > 0) {
                        foundParams = menu.getJSONObject(0).optString("params", null);
                    }

                    if (foundParams != null) {
                        Log.d(TAG, "Retrying with params: " + foundParams);
                        fetchTranscriptData(videoId, foundParams, callback);
                        return;
                    }
                }

                if (root.has("msg")) {
                    postError(callback, root.getString("msg"));
                } else if (root.has("message")) {
                    postError(callback, root.getString("message"));
                } else {
                    postError(callback, "No speech text found in this video.");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            postError(callback, "Connection failed. Check internet.");
        }
    }

    private void processSuccessArray(String jsonArrayString, String videoId, TranscriptCallback callback) {
        try {
            JSONArray array = new JSONArray(jsonArrayString);
            processTranscriptArray(array, videoId, callback);
        } catch (Exception e) {
            postError(callback, "Failed to parse transcript.");
        }
    }

    private void processTranscriptArray(JSONArray array, String videoId, TranscriptCallback callback) {
        try {
            StringBuilder fullText = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                JSONObject segment = array.getJSONObject(i);
                if (segment.has("text")) {
                    fullText.append(segment.getString("text")).append(" ");
                } else if (segment.has("snippet")) {
                    fullText.append(segment.getString("snippet")).append(" ");
                }
            }

            String finalText = fullText.toString().trim();
            if (finalText.isEmpty()) {
                postError(callback, "Transcript was empty.");
                return;
            }

            if (finalText.length() > 100000) {
                finalText = finalText.substring(0, 100000);
            }

            String result = finalText;
            mainHandler.post(() -> callback.onSuccess(result, videoId));

        } catch (Exception e) {
            postError(callback, "Error processing text.");
        }
    }

    private String extractVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        if (matcher.find()) return matcher.group();
        return null;
    }

    private void postError(TranscriptCallback callback, String msg) {
        mainHandler.post(() -> callback.onError(new Exception(msg)));
    }

    public interface TranscriptCallback {
        void onSuccess(String transcriptText, String videoId);
        void onError(Throwable t);
    }
}