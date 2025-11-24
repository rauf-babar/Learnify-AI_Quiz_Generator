package com.example.learnify;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "LearnifyPrefs";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
    private static final String KEY_TOTAL_SCORE = "user_total_score";

    private static final long SESSION_EXPIRATION_DAYS = 14;
    private static final long EXPIRATION_TIME_MS = SESSION_EXPIRATION_DAYS * 24 * 60 * 60 * 1000;

    private final SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String userToken, String userId, String userName) {
        if (sharedPreferences == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_TOKEN, userToken);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public void saveUserScore(long totalScore) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putLong(KEY_TOTAL_SCORE, totalScore).apply();
    }

    public long getUserScore() {
        if (sharedPreferences == null) return 0;
        return sharedPreferences.getLong(KEY_TOTAL_SCORE, 0);
    }

    public boolean isLoggedIn() {
        if (sharedPreferences == null) return false;

        String token = sharedPreferences.getString(KEY_USER_TOKEN, null);
        if (token == null) {
            return false;
        }

        long loginTime = sharedPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - loginTime > EXPIRATION_TIME_MS) {
            clearSession();
            return false;
        }

        return true;
    }

    public void clearSession() {
        if (sharedPreferences == null) return;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public String getUserToken() {
        if (sharedPreferences == null) return null;
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    public String getUserId() {
        if (sharedPreferences == null) return null;
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        if (sharedPreferences == null) return "User";
        return sharedPreferences.getString(KEY_USER_NAME, "User");
    }
}