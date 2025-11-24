package com.example.learnify;

import java.io.Serializable;

public class QuizAnswer implements Serializable {
    private final String answerText;
    private final boolean isCorrect;

    public QuizAnswer(String answerText, boolean isCorrect) {
        this.answerText = answerText;
        this.isCorrect = isCorrect;
    }

    public String getAnswerText() { return answerText; }
    public boolean isCorrect() { return isCorrect; }
}