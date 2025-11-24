package com.example.learnify;

import android.content.Context;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class QuizRecord implements Serializable {

    private final String topicName;
    private final int totalQuestions;
    private final int correctAnswers;
    private final long timeTakenMs;
    private final String source;
    private final String quizId;
    private final String uid;
    private final long completedAt;
    private final String sourceData;
    private final String difficulty;

    public QuizRecord(String quizId, String uid, String topicName, int totalQuestions, int correctAnswers, long timeTakenMs, String source, String sourceData, long completedAt, String difficulty) {
        this.quizId = quizId;
        this.uid = uid;
        this.topicName = topicName;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.timeTakenMs = timeTakenMs;
        this.source = source;
        this.sourceData = sourceData;
        this.completedAt = completedAt;
        this.difficulty = difficulty;
    }

    public String getTopicName() { return topicName; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public long getTimeTakenMs() { return timeTakenMs; }
    public String getSource() { return source; }
    public String getQuizId() { return quizId; }
    public String getUid() { return uid; }
    public long getCompletedAt() { return completedAt; }
    public String getSourceData() { return sourceData; }
    public String getDifficulty() { return difficulty; }

    public double getAccuracyPercentage() {
        if (totalQuestions == 0) return 0.0;
        return ((double) correctAnswers / totalQuestions) * 100;
    }

    public String getTimeFormatted(Context context) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeTakenMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeTakenMs) % 60;

        if (seconds > 0) {
            return context.getString(R.string.time_min_sec, minutes, seconds);
        } else {
            return context.getString(R.string.time_min_only, minutes);
        }
    }
}