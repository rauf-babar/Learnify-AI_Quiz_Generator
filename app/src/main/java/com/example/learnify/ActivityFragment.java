package com.example.learnify;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class ActivityFragment extends Fragment {

    public static boolean shouldScrollToHistory = false;

    private ProgressBar pbAccuracyFill;
    private TextView tvAccuracyValue;
    private RecyclerView rvHistoryList;
    private QuizAdapter quizAdapter;
    private final List<QuizRecord> allQuizRecords = new ArrayList<>();

    private NestedScrollView nsvStatsScrollView;
    private View noHistoryView;
    private Button btnLoadFromFirebase;

    private SessionManager sessionManager;
    private QuizDatabase quizDatabase;

    private TextView tvTotalQuizzesValue, tvTotalQuestionsValue, tvCorrectValue, tvWrongValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        quizDatabase = QuizDatabase.getInstance(requireContext());

        View backButton = view.findViewById(R.id.back_button_container);
        backButton.setOnClickListener(v -> {
        });

        pbAccuracyFill = view.findViewById(R.id.pb_accuracy_fill);
        tvAccuracyValue = view.findViewById(R.id.tv_accuracy_value);
        rvHistoryList = view.findViewById(R.id.rv_history_list);
        nsvStatsScrollView = view.findViewById(R.id.nsv_stats_scrollview);
        noHistoryView = view.findViewById(R.id.ll_no_history);
        btnLoadFromFirebase = view.findViewById(R.id.btn_load_from_firebase);

        tvTotalQuizzesValue = view.findViewById(R.id.tv_total_quizzes_value);
        tvTotalQuestionsValue = view.findViewById(R.id.tv_total_questions_value);
        tvCorrectValue = view.findViewById(R.id.tv_correct_value);
        tvWrongValue = view.findViewById(R.id.tv_wrong_value);

        setupHistoryRecyclerView();
        refreshData();

        btnLoadFromFirebase.setOnClickListener(v -> syncWithFirebase());
    }

    private void refreshData() {
        loadFullHistory();
        updateStats();
    }

    private void updateStats() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        quizDatabase.getStats(uid, stats -> {
            if (!isAdded() || getView() == null) return;

            int quizzes = stats.get("totalQuizzes");
            int questions = stats.get("totalQuestions");
            int correct = stats.get("totalCorrect");
            int wrong = stats.get("totalWrong");
            int accuracy = stats.get("averageAccuracy");

            requireActivity().runOnUiThread(() -> {
                tvTotalQuizzesValue.setText(String.valueOf(quizzes));
                tvTotalQuestionsValue.setText(String.valueOf(questions));
                tvCorrectValue.setText(String.valueOf(correct));
                tvWrongValue.setText(String.valueOf(wrong));

                pbAccuracyFill.setProgress(accuracy);
                tvAccuracyValue.setText(getString(R.string.score_percentage_format, accuracy));
            });
        });
    }

    private void syncWithFirebase() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showErrorDialog(getString(R.string.no_internet), getString(R.string.check_connection));
            return;
        }

        btnLoadFromFirebase.setEnabled(false);
        btnLoadFromFirebase.setText(getString(R.string.syncing));

        FirestoreManager.getInstance().getAllQuizHistory(uid, new FirestoreManager.OnHistoryLoadedListener() {
            @Override
            public void onHistoryLoaded(List<QuizRecord> records, List<String> rawJsons) {
                if (!isAdded()) return;

                for (int i = 0; i < records.size(); i++) {
                    quizDatabase.addQuizRecord(records.get(i), rawJsons.get(i));
                }

                requireActivity().runOnUiThread(() -> {
                    refreshData();
                    btnLoadFromFirebase.setEnabled(true);
                    btnLoadFromFirebase.setText(getString(R.string.load_all_quizzes));
                    Snackbar.make(btnLoadFromFirebase, getString(R.string.sync_success), Snackbar.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnLoadFromFirebase.setEnabled(true);
                    btnLoadFromFirebase.setText(getString(R.string.load_all_quizzes));
                    showErrorDialog(getString(R.string.sync_failed), error);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();

        if (shouldScrollToHistory) {
            shouldScrollToHistory = false;
            nsvStatsScrollView.postDelayed(() -> {
                if (!isAdded() || getView() == null) return;
                TextView tvHistoryTitle = getView().findViewById(R.id.tv_history_title);
                if (tvHistoryTitle != null) {
                    nsvStatsScrollView.smoothScrollTo(0, tvHistoryTitle.getTop());
                }
            }, 100);
        }
    }

    private void setupHistoryRecyclerView() {
        quizAdapter = new QuizAdapter(requireContext(), allQuizRecords, quizId -> requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.home_fragment_host, QuizReviewFragment.newInstance(quizId))
                .addToBackStack(null)
                .commit());

        rvHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistoryList.setAdapter(quizAdapter);
        rvHistoryList.setNestedScrollingEnabled(false);
    }

    private void loadFullHistory() {
        String currentUid = sessionManager.getUserId();
        if (currentUid == null) return;

        quizDatabase.getAllQuizzes(currentUid, quizzes -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (quizzes.isEmpty()) {
                    rvHistoryList.setVisibility(View.GONE);
                    noHistoryView.setVisibility(View.VISIBLE);
                } else {
                    rvHistoryList.setVisibility(View.VISIBLE);
                    noHistoryView.setVisibility(View.GONE);

                    int oldSize = allQuizRecords.size();
                    allQuizRecords.clear();
                    quizAdapter.notifyItemRangeRemoved(0, oldSize);
                    allQuizRecords.addAll(quizzes);
                    quizAdapter.notifyItemRangeInserted(0, quizzes.size());
                }
            });
        });
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

        btnAction.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}