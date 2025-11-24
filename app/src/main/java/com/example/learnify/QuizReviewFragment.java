package com.example.learnify;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

public class QuizReviewFragment extends Fragment {

    private static final String ARG_QUIZ_ID = "quiz_id";
    private String quizId;
    private QuizDatabase quizDatabase;
    private QuizResult currentQuizResult;

    private TextView tvTopic, tvDiff, tvQs, tvScore, tvTime;
    private RecyclerView rvQuestions;

    public static QuizReviewFragment newInstance(String quizId) {
        QuizReviewFragment fragment = new QuizReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUIZ_ID, quizId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizId = getArguments().getString(ARG_QUIZ_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        quizDatabase = QuizDatabase.getInstance(requireContext());

        tvTopic = view.findViewById(R.id.tv_review_topic);
        tvDiff = view.findViewById(R.id.tv_stat_difficulty);
        tvQs = view.findViewById(R.id.tv_stat_questions);
        tvScore = view.findViewById(R.id.tv_stat_score);
        tvTime = view.findViewById(R.id.tv_stat_time);
        rvQuestions = view.findViewById(R.id.rv_review_questions);

        view.findViewById(R.id.back_button_container).setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btn_retake_quiz).setOnClickListener(v -> handleRetake());

        loadData();
    }

    private void loadData() {
        if (quizId == null) {
            showErrorDialog("Error", "Quiz ID is missing.");
            return;
        }

        quizDatabase.getQuizResult(quizId, result -> {
            if (!isAdded() || getActivity() == null) return;

            if (result == null || result.getQuizRecord() == null) {
                showErrorDialog("Error", "Could not load quiz details.");
                return;
            }

            currentQuizResult = result;

            requireActivity().runOnUiThread(() -> {
                QuizRecord record = result.getQuizRecord();

                tvTopic.setText(record.getTopicName());

                String diff = record.getDifficulty();
                tvDiff.setText(diff != null && !diff.isEmpty() ? diff : "Medium");

                tvQs.setText(String.valueOf(record.getTotalQuestions()));

                tvScore.setText(getString(R.string.score_percentage_format, (int) record.getAccuracyPercentage()));

                tvTime.setText(record.getTimeFormatted(getContext()));

                ReviewQuestionAdapter adapter = new ReviewQuestionAdapter(
                        result.getQuestions(),
                        result.getUserAnswers()
                );
                rvQuestions.setLayoutManager(new LinearLayoutManager(getContext()));
                rvQuestions.setAdapter(adapter);
            });
        });
    }

    private void handleRetake() {
        if (currentQuizResult == null) {
            showErrorDialog("Error", "Quiz data not loaded yet.");
            return;
        }

        Gson gson = new Gson();
        String contextJson = gson.toJson(currentQuizResult.getQuestions());

        QuizDataHolder.getInstance().setExtractedText(contextJson);

        Intent intent = new Intent(requireActivity(), QuizActivity.class);
        intent.putExtra("SOURCE_TYPE", "REGENERATE");
        intent.putExtra("SOURCE_DATA", currentQuizResult.getQuizRecord().getSourceData());

        startActivity(intent);
    }

    private void showErrorDialog(String title, String message) {
        if (!isAdded() || getActivity() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_error, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_error_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_error_message);
        Button btnAction = dialogView.findViewById(R.id.btn_error_action);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnAction.setText(getString(R.string.okay));

        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            if (title.equals("Error")) {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        dialog.show();
    }
}