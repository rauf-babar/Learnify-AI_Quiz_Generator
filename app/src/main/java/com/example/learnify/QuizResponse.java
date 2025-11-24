package com.example.learnify;

import java.util.List;

public class QuizResponse {
    private String topic;
    private List<QuizQuestion> questions;

    public String getTopic() {
        return topic;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}