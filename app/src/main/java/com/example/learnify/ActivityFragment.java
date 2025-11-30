package com.example.learnify;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

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
    private TextView tvNoSearchResults;
    private Button btnLoadFromFirebase;
    private EditText etSearchHistory;
    private TextInputLayout tilSearchHistory;

    private SessionManager sessionManager;
    private QuizDatabase quizDatabase;

    private TextView tvTotalQuizzesValue, tvTotalQuestionsValue, tvCorrectValue, tvWrongValue;
    private boolean isCrossIconState = false;
    private int currentSortMode = R.id.sort_latest;

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
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });

        pbAccuracyFill = view.findViewById(R.id.pb_accuracy_fill);
        tvAccuracyValue = view.findViewById(R.id.tv_accuracy_value);
        rvHistoryList = view.findViewById(R.id.rv_history_list);
        nsvStatsScrollView = view.findViewById(R.id.nsv_stats_scrollview);

        noHistoryView = view.findViewById(R.id.ll_no_history);
        tvNoSearchResults = view.findViewById(R.id.tv_no_search_results);

        btnLoadFromFirebase = view.findViewById(R.id.btn_load_from_firebase);

        tilSearchHistory = view.findViewById(R.id.til_search_history);
        etSearchHistory = view.findViewById(R.id.et_search_history);
        ImageButton btnSortHistory = view.findViewById(R.id.btn_sort_history);

        tvTotalQuizzesValue = view.findViewById(R.id.tv_total_quizzes_value);
        tvTotalQuestionsValue = view.findViewById(R.id.tv_total_questions_value);
        tvCorrectValue = view.findViewById(R.id.tv_correct_value);
        tvWrongValue = view.findViewById(R.id.tv_wrong_value);

        setupHistoryRecyclerView();
        refreshData();

        btnLoadFromFirebase.setOnClickListener(v -> syncWithFirebase());
        btnSortHistory.setOnClickListener(this::showSortMenu);

        setupSearchInteractions();
    }

    private void setupSearchInteractions() {
        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearchHistory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilSearchHistory.setEndIconDrawable(R.drawable.ic_search);
                isCrossIconState = false;
            }
        });

        tilSearchHistory.setEndIconOnClickListener(v -> {
            if (isCrossIconState) {
                etSearchHistory.setText("");
                tilSearchHistory.setEndIconDrawable(R.drawable.ic_search);
                isCrossIconState = false;
                etSearchHistory.clearFocus();
            } else {
                hideKeyboard();
                etSearchHistory.clearFocus();
                if (etSearchHistory.getText().length() > 0) {
                    tilSearchHistory.setEndIconDrawable(R.drawable.ic_clear_white_circle_purple);
                    isCrossIconState = true;
                }
            }
        });
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenuInflater().inflate(R.menu.menu_sort_options, popup.getMenu());

        // Restore checked state
        popup.getMenu().findItem(currentSortMode).setChecked(true);

        popup.setOnMenuItemClickListener(item -> {
            currentSortMode = item.getItemId();
            item.setChecked(true);
            applyFilterAndSort();
            return true;
        });

        popup.show();
    }

    private void applyFilterAndSort() {
        String query = etSearchHistory.getText().toString();
        List<QuizRecord> filteredList = new ArrayList<>();

        if (query.trim().isEmpty()) {
            filteredList.addAll(allQuizRecords);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (QuizRecord record : allQuizRecords) {
                if (record.getTopicName() != null && record.getTopicName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(record);
                }
            }
        }

        filteredList.sort((r1, r2) -> {
            if (currentSortMode == R.id.sort_alphabetical) {
                return r1.getTopicName().compareToIgnoreCase(r2.getTopicName());
            } else if (currentSortMode == R.id.sort_accuracy_low) {
                return Double.compare(r1.getAccuracyPercentage(), r2.getAccuracyPercentage());
            } else if (currentSortMode == R.id.sort_accuracy_high) {
                return Double.compare(r2.getAccuracyPercentage(), r1.getAccuracyPercentage());
            } else if (currentSortMode == R.id.sort_oldest) {
                return Long.compare(r1.getCompletedAt(), r2.getCompletedAt());
            } else {
                // Default: Latest (Descending)
                return Long.compare(r2.getCompletedAt(), r1.getCompletedAt());
            }
        });

        if (quizAdapter != null) {
            quizAdapter.updateList(filteredList);
        }

        if (filteredList.isEmpty()) {
            rvHistoryList.setVisibility(View.GONE);
            if (allQuizRecords.isEmpty()) {
                noHistoryView.setVisibility(View.VISIBLE);
                tvNoSearchResults.setVisibility(View.GONE);
            } else {
                noHistoryView.setVisibility(View.GONE);
                tvNoSearchResults.setVisibility(View.VISIBLE);
            }
        } else {
            rvHistoryList.setVisibility(View.VISIBLE);
            noHistoryView.setVisibility(View.GONE);
            tvNoSearchResults.setVisibility(View.GONE);
        }
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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

            int quizzes = stats.get("totalQuizzes") != null ? stats.get("totalQuizzes") : 0;
            int questions = stats.get("totalQuestions") != null ? stats.get("totalQuestions") : 0;
            int correct = stats.get("totalCorrect") != null ? stats.get("totalCorrect") : 0;
            int wrong = stats.get("totalWrong") != null ? stats.get("totalWrong") : 0;
            int accuracy = stats.get("averageAccuracy") != null ? stats.get("averageAccuracy") : 0;

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
        quizAdapter = new QuizAdapter(requireContext(), allQuizRecords,
                quizId -> requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.home_fragment_host, QuizReviewFragment.newInstance(quizId))
                        .addToBackStack(null)
                        .commit(),
                this::showDeleteDialog
        );

        rvHistoryList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistoryList.setAdapter(quizAdapter);
        rvHistoryList.setNestedScrollingEnabled(false);
    }

    private void showDeleteDialog(String quizId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Quiz")
                .setMessage("Remove this quiz from your local history? It will remain in the cloud backup.")
                .setPositiveButton("Delete", (dialog, which) -> deleteQuiz(quizId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuiz(String quizId) {
        quizDatabase.deleteQuiz(quizId, () -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(this::refreshData);
        });
    }

    private void loadFullHistory() {
        String currentUid = sessionManager.getUserId();
        if (currentUid == null) return;

        quizDatabase.getAllQuizzes(currentUid, quizzes -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                allQuizRecords.clear();
                if (quizzes.isEmpty()) {
                    rvHistoryList.setVisibility(View.GONE);
                    noHistoryView.setVisibility(View.VISIBLE);
                    tvNoSearchResults.setVisibility(View.GONE);
                } else {
                    rvHistoryList.setVisibility(View.VISIBLE);
                    noHistoryView.setVisibility(View.GONE);
                    tvNoSearchResults.setVisibility(View.GONE);

                    allQuizRecords.addAll(quizzes);

                    if (etSearchHistory != null) {
                        applyFilterAndSort();
                    } else {
                        quizAdapter.updateList(allQuizRecords);
                    }
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