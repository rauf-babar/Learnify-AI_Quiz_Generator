package com.example.learnify;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class QuizGenerator {

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private final GenerativeModelFutures model;
    private final Executor executor;
    private final Gson gson;

    public QuizGenerator() {
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", API_KEY);
        this.model = GenerativeModelFutures.from(gm);
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }

    public void generateQuiz(String text, int numQuestions, String difficulty, QuizCallback callback) {
        String prompt = "Analyze the text below and generate a " + difficulty + " level quiz with " + numQuestions + " multiple-choice questions.\n" +
                "You must also generate a short, descriptive 'topic' title (max 5 words) that summarizes what this text is about.\n\n" +
                "Return the output strictly as a raw JSON Object (no Markdown). The schema is:\n" +
                "{\n" +
                "  \"topic\": \"string\",\n" +
                "  \"questions\": [\n" +
                "    { \"questionText\": \"string\", \"explanation\": \"string\", \"answers\": [ { \"answerText\": \"string\", \"isCorrect\": boolean } ] }\n" +
                "  ]\n" +
                "}\n\n" +
                "Ensure exactly 4 options per question.\n" +
                "Text to analyze: " + text;

        sendRequest(prompt, callback);
    }

    public void regenerateQuiz(String oldQuestionsJson, int numQuestions, String difficulty, QuizCallback callback) {
        String prompt = "You are a Quiz Re-mastering Engine. I will provide a list of old quiz questions in JSON format.\n" +
                "Your task is to generate a NEW " + difficulty + " level quiz with " + numQuestions + " questions based strictly on the concepts covered in the input questions.\n\n" +
                "RULES:\n" +
                "1. Do NOT simply copy the old questions. Rephrase them, turn them into scenario-based questions, or ask about the same concept from a different angle.\n" +
                "2. Do NOT introduce new external topics. If the input covers BGP, stick to BGP.\n" +
                "3. Keep the same 'topic' title as the input context.\n" +
                "4. Return strictly raw JSON Object (no Markdown) matching this schema:\n" +
                "{\n" +
                "  \"topic\": \"string\",\n" +
                "  \"questions\": [\n" +
                "    { \"questionText\": \"string\", \"explanation\": \"string\", \"answers\": [ { \"answerText\": \"string\", \"isCorrect\": boolean } ] }\n" +
                "  ]\n" +
                "}\n\n" +
                "Input Questions JSON: " + oldQuestionsJson;

        sendRequest(prompt, callback);
    }

    private void sendRequest(String prompt, QuizCallback callback) {
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String jsonResponse = result.getText();

                    if (jsonResponse != null && jsonResponse.startsWith("```json")) {
                        jsonResponse = jsonResponse.replace("```json", "").replace("```", "");
                    }
                    if (jsonResponse != null && jsonResponse.endsWith("```")) {
                        jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
                    }

                    QuizResponse response = gson.fromJson(jsonResponse, QuizResponse.class);

                    if (response == null || response.getQuestions() == null || response.getQuestions().isEmpty()) {
                        callback.onError(new Exception("AI returned empty quiz data."));
                    } else {
                        callback.onSuccess(response);
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(t);
            }
        }, executor);
    }

    public void generateFeedback(QuizResult result, FeedbackCallback callback) {
        double accuracy = result.getQuizRecord().getAccuracyPercentage();
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("Analyze this quiz performance.\n");
        promptBuilder.append("Topic: ").append(result.getQuizRecord().getTopicName()).append("\n");
        promptBuilder.append("Score: ").append((int)accuracy).append("%\n\n");
        promptBuilder.append("Here is the detailed breakdown of every question:\n");

        for (int i = 0; i < result.getQuestions().size(); i++) {
            int userAnsIndex = result.getUserAnswers().getOrDefault(i, -1);
            QuizQuestion q = result.getQuestions().get(i);

            int correctIndex = -1;
            for(int j=0; j<q.getAnswers().size(); j++) {
                if(q.getAnswers().get(j).isCorrect()) correctIndex = j;
            }

            boolean isCorrect = (userAnsIndex == correctIndex);

            if (isCorrect) {
                promptBuilder.append("[CORRECT] ");
            } else {
                promptBuilder.append("[WRONG] ");
            }
            promptBuilder.append("Q: ").append(q.getQuestionText()).append("\n");
        }

        promptBuilder.append("\nINSTRUCTIONS FOR FEEDBACK:\n");
        promptBuilder.append("1. Analyze the [WRONG] answers to identify specific weak areas or concepts the user misunderstands.\n");
        promptBuilder.append("2. Acknowledge the [CORRECT] answers to validate what they already know well.\n");
        promptBuilder.append("3. Give a short, encouraging summary of their overall performance.\n");

        if (accuracy >= 20) {
            promptBuilder.append("4. Suggest 2 specific sub-topics or related concepts they should learn NEXT to advance based on what they got right.\n");
        } else {
            promptBuilder.append("4. Give encouraging advice to restart the basics. Do NOT suggest advanced next steps yet.\n");
        }

        promptBuilder.append("\nKeep the tone helpful, professional, and concise. Return plain text, no markdown.");

        Content content = new Content.Builder()
                .addText(promptBuilder.toString())
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                callback.onSuccess(text);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onError(t);
            }
        }, executor);
    }

    public interface QuizCallback {
        void onSuccess(QuizResponse response);
        void onError(Throwable t);
    }

    public interface FeedbackCallback {
        void onSuccess(String feedback);
        void onError(Throwable t);
    }
}