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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuizDownloaderFragment extends Fragment {

    private RecyclerView recyclerView;
    private QuizDownloadAdapter adapter;
    private Button btnLoadAll;
    private LinearLayout loadingOverlay;
    private TextView tvEmptyState;
    private EditText etSearch;
    private TextInputLayout tilSearch;

    private SessionManager sessionManager;
    private QuizDatabase quizDatabase;

    private final List<CloudQuiz> allCloudQuizzes = new ArrayList<>();
    private int currentSortMode = R.id.sort_latest;
    private boolean isCrossIconState = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_downloader, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        quizDatabase = QuizDatabase.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.rv_download_list);
        btnLoadAll = view.findViewById(R.id.btn_load_all_quizzes);
        loadingOverlay = view.findViewById(R.id.ll_loading_overlay);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        etSearch = view.findViewById(R.id.et_search_download);
        tilSearch = view.findViewById(R.id.til_search_download);
        ImageButton btnSort = view.findViewById(R.id.btn_sort_download);
        View backBtn = view.findViewById(R.id.back_button_container);

        setupRecyclerView();
        setupSearchInteractions();

        backBtn.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnSort.setOnClickListener(this::showSortMenu);
        btnLoadAll.setOnClickListener(v -> saveAllQuizzes());

        loadData();
    }

    private void setupRecyclerView() {
        adapter = new QuizDownloadAdapter(requireContext(), new ArrayList<>(), this::downloadSingleQuiz);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showErrorDialog(getString(R.string.no_internet), getString(R.string.check_connection));
            return;
        }

        setLoading(true);

        quizDatabase.getAllQuizzes(uid, localQuizzes -> {
            Set<String> localIds = new HashSet<>();
            for (QuizRecord r : localQuizzes) localIds.add(r.getQuizId());

            FirestoreManager.getInstance().getAllQuizHistory(uid, new FirestoreManager.OnHistoryLoadedListener() {
                @Override
                public void onHistoryLoaded(List<QuizRecord> records, List<String> rawJsons) {
                    if (!isAdded()) return;

                    allCloudQuizzes.clear();
                    for (int i = 0; i < records.size(); i++) {
                        if (!localIds.contains(records.get(i).getQuizId())) {
                            allCloudQuizzes.add(new CloudQuiz(records.get(i), rawJsons.get(i)));
                        }
                    }

                    requireActivity().runOnUiThread(() -> {
                        setLoading(false);
                        applyFilterAndSort();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        setLoading(false);
                        showErrorDialog("Error", error);
                    });
                }
            });
        });
    }

    private void downloadSingleQuiz(CloudQuiz cloudQuiz) {
        quizDatabase.addQuizRecord(cloudQuiz.getRecord(), cloudQuiz.getRawJson());

        allCloudQuizzes.remove(cloudQuiz);
        applyFilterAndSort();

     }

    private void saveAllQuizzes() {
        if (allCloudQuizzes.isEmpty()) {
            return;
        }

        setLoading(true);
        for (CloudQuiz q : allCloudQuizzes) {
            quizDatabase.addQuizRecord(q.getRecord(), q.getRawJson());
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            setLoading(false);
            allCloudQuizzes.clear();
            applyFilterAndSort();
            Snackbar.make(btnLoadAll, getString(R.string.sync_success), Snackbar.LENGTH_LONG).show();
        }, 1000);
    }

    private void setupSearchInteractions() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSort();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilSearch.setEndIconDrawable(R.drawable.ic_search);
                isCrossIconState = false;
            }
        });

        tilSearch.setEndIconOnClickListener(v -> {
            if (isCrossIconState) {
                etSearch.setText("");
                tilSearch.setEndIconDrawable(R.drawable.ic_search);
                isCrossIconState = false;
                etSearch.clearFocus();
            } else {
                hideKeyboard();
                etSearch.clearFocus();
                if (etSearch.getText().length() > 0) {
                    tilSearch.setEndIconDrawable(R.drawable.ic_clear_white_circle_purple);
                    isCrossIconState = true;
                }
            }
        });
    }

    private void applyFilterAndSort() {
        String query = etSearch.getText().toString();
        List<CloudQuiz> filteredList = new ArrayList<>();

        if (query.trim().isEmpty()) {
            filteredList.addAll(allCloudQuizzes);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (CloudQuiz item : allCloudQuizzes) {
                if (item.getRecord().getTopicName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(item);
                }
            }
        }

        filteredList.sort((q1, q2) -> {
            QuizRecord r1 = q1.getRecord();
            QuizRecord r2 = q2.getRecord();

            if (currentSortMode == R.id.sort_alphabetical) {
                return r1.getTopicName().compareToIgnoreCase(r2.getTopicName());
            } else if (currentSortMode == R.id.sort_accuracy_low) {
                return Double.compare(r1.getAccuracyPercentage(), r2.getAccuracyPercentage());
            } else if (currentSortMode == R.id.sort_accuracy_high) {
                return Double.compare(r2.getAccuracyPercentage(), r1.getAccuracyPercentage());
            } else if (currentSortMode == R.id.sort_oldest) {
                return Long.compare(r1.getCompletedAt(), r2.getCompletedAt());
            } else {
                return Long.compare(r2.getCompletedAt(), r1.getCompletedAt());
            }
        });

        adapter.updateList(filteredList);

        if (filteredList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(allCloudQuizzes.isEmpty() ? "All quizzes are up to date!" : "No matching quizzes found.");
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenuInflater().inflate(R.menu.menu_sort_options, popup.getMenu());
        popup.getMenu().findItem(currentSortMode).setChecked(true);
        popup.setOnMenuItemClickListener(item -> {
            currentSortMode = item.getItemId();
            item.setChecked(true);
            applyFilterAndSort();
            return true;
        });
        popup.show();
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            loadingOverlay.setVisibility(View.VISIBLE);
            btnLoadAll.setEnabled(false);
        } else {
            loadingOverlay.setVisibility(View.GONE);
            btnLoadAll.setEnabled(true);
        }
    }

    private void showErrorDialog(String title, String message) {
        if (!isAdded() || getActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_error, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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