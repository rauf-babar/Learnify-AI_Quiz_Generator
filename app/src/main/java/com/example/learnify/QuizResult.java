package com.example.learnify;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class QuizResult implements Serializable {
    private final QuizRecord quizRecord;
    private final List<QuizQuestion> questions;
    private final Map<Integer, Integer> userAnswers;


    public QuizResult(QuizRecord quizRecord, List<QuizQuestion> questions, Map<Integer, Integer> userAnswers) {
        this.quizRecord = quizRecord;
        this.questions = questions;
        this.userAnswers = userAnswers;
    }

    public QuizRecord getQuizRecord() { return quizRecord; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public Map<Integer, Integer> getUserAnswers() { return userAnswers; }
}