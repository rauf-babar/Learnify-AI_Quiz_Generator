package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class ResultFragment extends Fragment {

    private QuizResult quizResult;

    private TextView tvTopic, tvScorePercent, tvCorrect, tvWrong, tvTotal;
    private TextView tvAiFeedback;
    private LinearLayout llAiLoading;
    private ProgressBar pbScore;
    private QuizGenerator quizGenerator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizResult = (QuizResult) getArguments().getSerializable("QUIZ_RESULT");
        }

        // Handle system back button
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finish();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        quizGenerator = new QuizGenerator();

        // Init Views
        tvTopic = view.findViewById(R.id.tv_result_topic);
        tvScorePercent = view.findViewById(R.id.tv_score_percentage);
        pbScore = view.findViewById(R.id.pb_result_score);

        tvCorrect = view.findViewById(R.id.tv_stat_correct);
        tvWrong = view.findViewById(R.id.tv_stat_wrong);
        tvTotal = view.findViewById(R.id.tv_stat_total);

        tvAiFeedback = view.findViewById(R.id.tv_ai_feedback);
        llAiLoading = view.findViewById(R.id.ll_ai_loading);

        Button btnBackToHome = view.findViewById(R.id.btn_back_to_home);
        btnBackToHome.setOnClickListener(v -> requireActivity().finish());

        if (quizResult != null) {
            populateUI();
            generateAiAnalysis();
        }
    }

    private void populateUI() {
        QuizRecord record = quizResult.getQuizRecord();

        tvTopic.setText(record.getTopicName());

        int total = record.getTotalQuestions();
        int correct = record.getCorrectAnswers();
        int wrong = total - correct;
        int percent = (int) record.getAccuracyPercentage();

        tvCorrect.setText(String.valueOf(correct));
        tvWrong.setText(String.valueOf(wrong));
        tvTotal.setText(String.valueOf(total));

        tvScorePercent.setText(percent + "%");
        pbScore.setProgress(percent);
    }

    private void generateAiAnalysis() {
        // Call AI
        quizGenerator.generateFeedback(quizResult, new QuizGenerator.FeedbackCallback() {
            @Override
            public void onSuccess(String feedback) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    llAiLoading.setVisibility(View.GONE);
                    tvAiFeedback.setText(feedback);
                    tvAiFeedback.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    llAiLoading.setVisibility(View.GONE);
                    tvAiFeedback.setText("Could not generate feedback at this time.");
                    tvAiFeedback.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}