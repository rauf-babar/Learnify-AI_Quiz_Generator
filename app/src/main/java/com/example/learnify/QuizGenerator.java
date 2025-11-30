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

    public void generateQuiz(String text, int numQuestions, String difficulty, String language, QuizCallback callback) {
        String langInstruction = language.equals("English")
                ? "Generate the quiz strictly in English. If the input text is in another language, translate the concepts and generate English questions."
                : "Generate the quiz in the same language as the input text.";

        String prompt = "Analyze the text below and generate a " + difficulty + " level quiz with " + numQuestions + " multiple-choice questions.\n" +
                "You must also generate a short, descriptive 'topic' title (max 5 words) that summarizes what this text is about.\n" +
                langInstruction + "\n\n" +
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

    public void regenerateQuiz(String oldQuestionsJson, int numQuestions, String difficulty, String language, QuizCallback callback) {
        String langInstruction = language.equals("English")
                ? "Generate the quiz strictly in English."
                : "Generate the quiz in the same language as the input context.";

        String prompt = "You are a Quiz Re-mastering Engine. I will provide a list of old quiz questions in JSON format.\n" +
                "Your task is to generate a NEW " + difficulty + " level quiz with " + numQuestions + " questions based strictly on the concepts covered in the input questions.\n\n" +
                "RULES:\n" +
                "1. Do NOT simply copy the old questions. Rephrase them, turn them into scenario-based questions, or ask about the same concept from a different angle.\n" +
                "2. Do NOT introduce new external topics.\n" +
                "3. Keep the same 'topic' title as the input context.\n" +
                "   - If the topic is originally in a different language than the required output language (Instruction 4), TRANSLATE the topic into the output language.\n" +
                "4. " + langInstruction + "\n" +
                "5. Return strictly raw JSON Object (no Markdown) matching this schema:\n" +
                "{\n" +
                "  \"topic\": \"string\",\n" +
                "  \"questions\": [\n" +
                "    { \"questionText\": \"string\", \"explanation\": \"string\", \"answers\": [ { \"answerText\": \"string\", \"isCorrect\": boolean } ] }\n" +
                "  ]\n" +
                "}\n\n" +
                "Ensure exactly 4 options per question.\n" +
                "Input Questions JSON: " + oldQuestionsJson;

        sendRequest(prompt, callback);
    }

    private void sendRequest(String prompt, QuizCallback callback) {
        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<>() {
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
        promptBuilder.append("1. LANGUAGE DETECTION: Read the quiz questions and answers provided above. Write your entire response strictly in the SAME language as the questions and answers.\n");
        promptBuilder.append("2. FORMATTING: Do NOT use Markdown (no asterisks, no hashes). Do NOT use any headings or titles. Use only simple bullet points (•) and clear short paragraphs.\n");
        promptBuilder.append("3. WEAK AREAS: Analyze the [WRONG] answers. Explain the core concepts the user missed.\n");
        promptBuilder.append("4. STRENGTHS: Briefly validate what the user understands well based on [CORRECT] answers.\n");
        promptBuilder.append("5. NEXT STEPS: Suggest minimum 2 specific topics or keywords to study next. You may suggest names of popular YouTube channels or websites known for this topic, but do NOT generate specific URLs as they may be broken.\n");

        if (accuracy < 40) {
            promptBuilder.append("6. ADVICE: Offer encouraging advice to review the fundamentals before retaking.\n");
        }

        Content content = new Content.Builder()
                .addText(promptBuilder.toString())
                .build();

        ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(content);

        Futures.addCallback(responseFuture, new FutureCallback<>() {
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