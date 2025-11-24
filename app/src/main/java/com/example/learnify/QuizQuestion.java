package com.example.learnify;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    private final String questionText;
    private final List<QuizAnswer> answers;
    private final String explanation;

    public QuizQuestion(String questionText, List<QuizAnswer> answers, String explanation) {
        this.questionText = questionText;
        this.answers = answers;
        this.explanation = explanation;
    }

    public String getQuestionText() { return questionText; }
    public List<QuizAnswer> getAnswers() { return answers; }
    public String getExplanation() { return explanation; }
}