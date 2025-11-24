package com.example.learnify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizDatabase {

    private static final String TABLE_QUIZ_HISTORY = "quiz_history";
    private static final String COL_HISTORY_ID = "historyId";
    private static final String COL_UID = "uid";
    private static final String COL_TOPIC_NAME = "topicName";
    private static final String COL_SOURCE_TYPE = "sourceType";
    private static final String COL_SOURCE_DATA = "sourceData";
    private static final String COL_TOTAL_QUESTIONS = "totalQuestions";
    private static final String COL_CORRECT_ANSWERS = "correctAnswers";
    private static final String COL_ACCURACY = "accuracy";
    private static final String COL_TIME_TAKEN_MS = "timeTakenMs";
    private static final String COL_COMPLETED_AT = "completedAt";
    private static final String COL_QUIZ_REVIEW_DATA = "quizReviewData";
    private static final String COL_DIFFICULTY = "difficulty";

    private static QuizDatabase instance;
    private final QuizDbHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private QuizDatabase(Context context) {
        dbHelper = new QuizDbHelper(context.getApplicationContext());
    }

    public static synchronized QuizDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new QuizDatabase(context);
        }
        return instance;
    }

    public void addQuizRecord(QuizRecord record, String quizReviewJson) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_HISTORY_ID, record.getQuizId());
            values.put(COL_UID, record.getUid());
            values.put(COL_TOPIC_NAME, record.getTopicName());
            values.put(COL_SOURCE_TYPE, record.getSource());
            values.put(COL_SOURCE_DATA, record.getSourceData());
            values.put(COL_TOTAL_QUESTIONS, record.getTotalQuestions());
            values.put(COL_CORRECT_ANSWERS, record.getCorrectAnswers());
            values.put(COL_ACCURACY, record.getAccuracyPercentage());
            values.put(COL_TIME_TAKEN_MS, record.getTimeTakenMs());
            values.put(COL_COMPLETED_AT, record.getCompletedAt());
            values.put(COL_DIFFICULTY, record.getDifficulty());
            values.put(COL_QUIZ_REVIEW_DATA, quizReviewJson);

            db.insertWithOnConflict(TABLE_QUIZ_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        });
    }

    public void addQuizRecord(QuizRecord record, QuizResult result) {
        String json = gson.toJson(result);
        addQuizRecord(record, json);
    }

    public void getRecentQuizzes(String uid, DatabaseCallback<List<QuizRecord>> callback) {
        getQuizzes(uid, "3", callback);
    }

    public void getAllQuizzes(String uid, DatabaseCallback<List<QuizRecord>> callback) {
        getQuizzes(uid, null, callback);
    }

    private void getQuizzes(String uid, String limit, DatabaseCallback<List<QuizRecord>> callback) {
        executor.execute(() -> {
            List<QuizRecord> quizList = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String selection = COL_UID + " = ?";
            String[] selectionArgs = { uid };
            String orderBy = COL_COMPLETED_AT + " DESC";

            Cursor cursor = db.query(
                    TABLE_QUIZ_HISTORY,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    orderBy,
                    limit
            );

            if (cursor.moveToFirst()) {
                do {
                    String diff = "Medium";
                    int diffIndex = cursor.getColumnIndex(COL_DIFFICULTY);
                    if (diffIndex != -1) {
                        String val = cursor.getString(diffIndex);
                        if (val != null) diff = val;
                    }

                    QuizRecord record = new QuizRecord(
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_UID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_TOPIC_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_QUESTIONS)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_CORRECT_ANSWERS)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_TAKEN_MS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_TYPE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_DATA)),
                            cursor.getLong(cursor.getColumnIndexOrThrow(COL_COMPLETED_AT)),
                            diff
                    );
                    quizList.add(record);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();

            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(quizList));
        });
    }

    public void getStats(String uid, DatabaseCallback<Map<String, Integer>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Map<String, Integer> stats = new HashMap<>();

            String selection = COL_UID + " = ?";
            String[] selectionArgs = { uid };

            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUIZ_HISTORY + " WHERE " + COL_UID + " = ?", selectionArgs);
            if (cursor.moveToFirst()) stats.put("totalQuizzes", cursor.getInt(0));
            else stats.put("totalQuizzes", 0);
            cursor.close();

            cursor = db.rawQuery("SELECT SUM(" + COL_TOTAL_QUESTIONS + ") FROM " + TABLE_QUIZ_HISTORY + " WHERE " + COL_UID + " = ?", selectionArgs);
            if (cursor.moveToFirst()) stats.put("totalQuestions", cursor.getInt(0));
            else stats.put("totalQuestions", 0);
            cursor.close();

            cursor = db.rawQuery("SELECT SUM(" + COL_CORRECT_ANSWERS + ") FROM " + TABLE_QUIZ_HISTORY + " WHERE " + COL_UID + " = ?", selectionArgs);
            if (cursor.moveToFirst()) stats.put("totalCorrect", cursor.getInt(0));
            else stats.put("totalCorrect", 0);
            cursor.close();

            int totalQs = stats.get("totalQuestions") != null ? stats.get("totalQuestions") : 0;
            int totalCorrect = stats.get("totalCorrect") != null ? stats.get("totalCorrect") : 0;
            stats.put("totalWrong", totalQs - totalCorrect);

            cursor = db.rawQuery("SELECT AVG(" + COL_ACCURACY + ") FROM " + TABLE_QUIZ_HISTORY + " WHERE " + COL_UID + " = ?", selectionArgs);
            if (cursor.moveToFirst()) stats.put("averageAccuracy", (int) cursor.getDouble(0));
            else stats.put("averageAccuracy", 0);
            cursor.close();

            db.close();
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(stats));
        });
    }

    public void getQuizResult(String historyId, DatabaseCallback<QuizResult> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] columns = {COL_QUIZ_REVIEW_DATA};
            String selection = COL_HISTORY_ID + " = ?";
            String[] selectionArgs = {historyId};

            Cursor cursor = db.query(TABLE_QUIZ_HISTORY, columns, selection, selectionArgs, null, null, null);

            QuizResult result = null;
            if (cursor.moveToFirst()) {
                String json = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUIZ_REVIEW_DATA));
                if (json != null && !json.isEmpty()) {
                    try {
                        result = gson.fromJson(json, QuizResult.class);
                    } catch (Exception e) {
                        android.util.Log.e("QuizDatabase", "JSON Parsing Failed", e);
                    }
                }
            }
            cursor.close();
            db.close();

            QuizResult finalResult = result;
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(finalResult));
        });
    }

    public void clearUserData(String uid) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(TABLE_QUIZ_HISTORY, COL_UID + " = ?", new String[]{uid});
            db.close();
        });
    }

    public interface DatabaseCallback<T> {
        void onComplete(T result);
    }

    private static class QuizDbHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "learnify.db";
        private static final int DATABASE_VERSION = 1;

        private static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_QUIZ_HISTORY + " (" +
                        COL_HISTORY_ID + " TEXT PRIMARY KEY," +
                        COL_UID + " TEXT," +
                        COL_TOPIC_NAME + " TEXT," +
                        COL_SOURCE_TYPE + " TEXT," +
                        COL_SOURCE_DATA + " TEXT," +
                        COL_TOTAL_QUESTIONS + " INTEGER," +
                        COL_CORRECT_ANSWERS + " INTEGER," +
                        COL_ACCURACY + " REAL," +
                        COL_TIME_TAKEN_MS + " INTEGER," +
                        COL_COMPLETED_AT + " INTEGER," +
                        COL_DIFFICULTY + " TEXT," +
                        COL_QUIZ_REVIEW_DATA + " TEXT)";

        public QuizDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_HISTORY);
            onCreate(db);
        }
    }
}