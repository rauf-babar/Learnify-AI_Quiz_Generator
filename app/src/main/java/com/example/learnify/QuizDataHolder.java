package com.example.learnify;

public class QuizDataHolder {
    private static QuizDataHolder instance;
    private String extractedText;

    private QuizDataHolder() {}

    public static synchronized QuizDataHolder getInstance() {
        if (instance == null) {
            instance = new QuizDataHolder();
        }
        return instance;
    }

    public void setExtractedText(String text) {
        this.extractedText = text;
    }

    public String getExtractedText() {
        return extractedText;
    }
}