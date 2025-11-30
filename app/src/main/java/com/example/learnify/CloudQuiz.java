package com.example.learnify;

public class CloudQuiz {
    private final QuizRecord record;
    private final String rawJson;

    public CloudQuiz(QuizRecord record, String rawJson) {
        this.record = record;
        this.rawJson = rawJson;
    }

    public QuizRecord getRecord() {
        return record;
    }

    public String getRawJson() {
        return rawJson;
    }
}