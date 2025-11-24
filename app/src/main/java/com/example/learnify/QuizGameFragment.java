package com.example.learnify;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QuizGameFragment extends Fragment {

    private ViewPager2 viewPager;
    private List<QuizQuestion> questions = new ArrayList<>();

    private TextView tvTimer, tvQuestionNumber;
    private ProgressBar pbQuizProgress;
    private View viewCorrectProgress, viewWrongProgress;
    private MaterialButton btnSubmitNext;
    private ConstraintLayout quizUiContainer;
    private LinearLayout loadingContainer;
    private FragmentContainerView resultFragmentHost;

    private CountDownTimer countDownTimer;
    private long timeTakenMs = 0;
    private long totalTimeLimitMs;

    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private boolean isQuizSubmitted = false;
    private final Map<Integer, Integer> userAnswers = new HashMap<>();

    private SessionManager sessionManager;
    private QuizDatabase quizDatabase;
    private QuizGenerator quizGenerator;

    private String difficultyLevel;
    private int numQuestions;
    private String sourceType;
    private String sourceData;

    private String aiGeneratedTopic = "Generated Quiz";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        quizDatabase = QuizDatabase.getInstance(requireContext());
        quizGenerator = new QuizGenerator();

        if (getArguments() != null) {
            difficultyLevel = getArguments().getString("DIFFICULTY", "Medium");
            totalTimeLimitMs = getArguments().getLong("TIME_LIMIT_MS", 600000);
            numQuestions = getArguments().getInt("NUM_QUESTIONS", 5);
            sourceType = getArguments().getString("SOURCE_TYPE", "UNKNOWN");
            sourceData = getArguments().getString("SOURCE_DATA", "");
        }

        quizUiContainer = view.findViewById(R.id.quiz_ui_container);
        loadingContainer = view.findViewById(R.id.ll_loading_container);
        resultFragmentHost = view.findViewById(R.id.result_fragment_host);

        viewPager = view.findViewById(R.id.vp_quiz_questions);
        tvTimer = view.findViewById(R.id.tv_timer);
        tvQuestionNumber = view.findViewById(R.id.tv_question_number);
        pbQuizProgress = view.findViewById(R.id.pb_quiz_progress);
        viewCorrectProgress = view.findViewById(R.id.view_correct_progress);
        viewWrongProgress = view.findViewById(R.id.view_wrong_progress);
        btnSubmitNext = view.findViewById(R.id.btn_submit_next);

        view.findViewById(R.id.back_button_container).setOnClickListener(v -> showExitDialog());

        btnSubmitNext.setOnClickListener(v -> onSubmitNextClicked());

        startAIGeneration();
    }

    private void startAIGeneration() {
        String extractedText = QuizDataHolder.getInstance().getExtractedText();

        if (extractedText == null || extractedText.isEmpty()) {
            showErrorDialog("Content Error", "No text found to generate quiz.");
            return;
        }

        QuizGenerator.QuizCallback callback = new QuizGenerator.QuizCallback() {
            @Override
            public void onSuccess(QuizResponse response) {
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (response.getTopic() != null && !response.getTopic().isEmpty()) {
                        aiGeneratedTopic = response.getTopic();
                    } else {
                        aiGeneratedTopic = getString(R.string.quiz_topic);
                    }

                    questions = response.getQuestions();

                    loadingContainer.setVisibility(View.GONE);
                    quizUiContainer.setVisibility(View.VISIBLE);

                    setupViewPager();
                    setupBackButton();
                    startTimer();
                    updateScoreboard();
                });
            }

            @Override
            public void onError(Throwable t) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showErrorDialog("AI Error", "Could not generate quiz. Please try again."));
            }
        };

        if ("REGENERATE".equals(sourceType)) {
            quizGenerator.regenerateQuiz(extractedText, numQuestions, difficultyLevel, callback);
        } else {
            quizGenerator.generateQuiz(extractedText, numQuestions, difficultyLevel, callback);
        }
    }

    private void setupViewPager() {
        viewPager.setUserInputEnabled(false);
        QuizQuestionAdapter questionAdapter = new QuizQuestionAdapter(this, questions);
        viewPager.setAdapter(questionAdapter);
        viewPager.setOffscreenPageLimit(questions.size());
        pbQuizProgress.setMax(questions.size());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateQuestionProgress(position);

                androidx.fragment.app.Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);
                if (fragment instanceof QuestionFragment) {
                    QuestionFragment qf = (QuestionFragment) fragment;
                    if (qf.isAnswered()) setButtonToNext();
                    else setButtonToSubmit(qf.isAnswerSelected());
                }
            }
        });
        updateQuestionProgress(0);
    }

    private void updateQuestionProgress(int position) {
        tvQuestionNumber.setText(String.valueOf(position + 1));
        pbQuizProgress.setProgress(position + 1);
        isQuizSubmitted = false;
        setButtonToSubmit(false);
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(totalTimeLimitMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeTakenMs = (totalTimeLimitMs - millisUntilFinished);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeTakenMs);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeTakenMs) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText(getString(R.string.default_time));
                timeTakenMs = totalTimeLimitMs;
                finishQuiz();
            }
        }.start();
    }

    public void onAnswerSelected(int answerIndex) {
        if (!isQuizSubmitted) {
            userAnswers.put(viewPager.getCurrentItem(), answerIndex);
            btnSubmitNext.setEnabled(true);
        }
    }

    private void onSubmitNextClicked() {
        QuestionFragment fragment = (QuestionFragment) getChildFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (fragment == null) return;

        if (isQuizSubmitted) {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < questions.size() - 1) {
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                finishQuiz();
            }
        } else {
            if (!fragment.isAnswerSelected()) {
                return;
            }
            boolean isCorrect = fragment.isSelectedAnswerCorrect();
            fragment.showResult();
            if (isCorrect) correctAnswers++; else wrongAnswers++;
            updateScoreboard();
            isQuizSubmitted = true;
            setButtonToNext();
        }
    }

    private void setButtonToSubmit(boolean isEnabled) {
        btnSubmitNext.setText(getString(R.string.submit));
        btnSubmitNext.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.figma_purple_main));
        btnSubmitNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.youtube_white));
        btnSubmitNext.setStrokeWidth(0);
        btnSubmitNext.setEnabled(isEnabled);
    }

    private void setButtonToNext() {
        btnSubmitNext.setEnabled(true);
        if (viewPager.getCurrentItem() == questions.size() - 1) {
            btnSubmitNext.setText(getString(R.string.finish));
        } else {
            btnSubmitNext.setText(getString(R.string.next));
        }
        btnSubmitNext.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.illustration_bg_lavender_medium));
        btnSubmitNext.setTextColor(ContextCompat.getColor(requireContext(), R.color.figma_purple_main));
        btnSubmitNext.setStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.figma_purple_main));
        btnSubmitNext.setStrokeWidth(4);
    }

    private void updateScoreboard() {
        int totalAnswered = correctAnswers + wrongAnswers;
        if (totalAnswered == 0) {
            viewCorrectProgress.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
            viewWrongProgress.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
            return;
        }
        LinearLayout.LayoutParams correctParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, correctAnswers);
        LinearLayout.LayoutParams wrongParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, wrongAnswers);
        viewCorrectProgress.setLayoutParams(correctParams);
        viewWrongProgress.setLayoutParams(wrongParams);
    }

    private void setupBackButton() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    private void handleBackPress() {
        if (resultFragmentHost.getVisibility() == View.VISIBLE) {
            requireActivity().finish();
            return;
        }
        int currentItem = viewPager.getCurrentItem();
        if (currentItem > 0) {
            viewPager.setCurrentItem(currentItem - 1);
        } else {
            showExitDialog();
        }
    }

    private void showExitDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_exit_quiz);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button btnExit = dialog.findViewById(R.id.btn_exit_confirm);
        Button btnCancel = dialog.findViewById(R.id.btn_exit_cancel);
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            countDownTimer.cancel();
            requireActivity().finish();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void finishQuiz() {
        countDownTimer.cancel();
        String currentUid = sessionManager.getUserId();
        if (currentUid == null) return;

        String quizId = "quiz_" + UUID.randomUUID().toString();
        long completedAt = System.currentTimeMillis();

        QuizRecord record = new QuizRecord(
                quizId, currentUid, aiGeneratedTopic, questions.size(), correctAnswers,
                timeTakenMs, sourceType, sourceData, completedAt, difficultyLevel
        );

        QuizResult result = new QuizResult(record, questions, userAnswers);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> quizDatabase.addQuizRecord(record, result));
        FirestoreManager.getInstance().saveQuizResult(record, result);

        Bundle args = new Bundle();
        args.putSerializable("QUIZ_RESULT", result);
        ResultFragment resultFragment = new ResultFragment();
        resultFragment.setArguments(args);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.result_fragment_host, resultFragment)
                .commit();

        quizUiContainer.setVisibility(View.GONE);
        resultFragmentHost.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
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
            requireActivity().finish();
        });

        dialog.show();
    }
}