package com.example.learnify;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private LinearLayout youtubeOptionsContainer;
    private TextInputLayout youtubeUrlInput;
    private TextInputEditText etYoutubeUrl;
    private ConstraintLayout homeContainer;
    private RecyclerView recyclerViewRecentQuizzes;
    private View noRecentQuizzesView;
    private QuizAdapter quizAdapter;
    private final List<QuizRecord> recentQuizRecords = new ArrayList<>();
    private LinearProgressIndicator progressBar;

    private SessionManager sessionManager;
    private QuizDatabase quizDatabase;

    private FileExtractor fileExtractor;
    private YouTubeHelper youTubeHelper;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isOptionsVisible = false;
    private Runnable hideErrorRunnable;
    private boolean isProcessing = false;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        processDocument(uri);
                    } else {
                        isProcessing = false;
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    isProcessing = false;
                    progressBar.setVisibility(View.GONE);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        quizDatabase = QuizDatabase.getInstance(requireContext());

        fileExtractor = new FileExtractor(requireContext());
        youTubeHelper = new YouTubeHelper();

        youtubeOptionsContainer = view.findViewById(R.id.ll_youtube_options);
        youtubeUrlInput = view.findViewById(R.id.til_youtube_url);
        etYoutubeUrl = view.findViewById(R.id.et_youtube_url);
        homeContainer = view.findViewById(R.id.cl_home_container);
        recyclerViewRecentQuizzes = view.findViewById(R.id.recycler_view_recent_quizzes);
        noRecentQuizzesView = view.findViewById(R.id.ll_no_recent_quizzes);
        TextView tvViewMoreRecent = view.findViewById(R.id.tv_view_more_recent);
        progressBar = view.findViewById(R.id.progress_bar_home);

        setupProfileCard(view);
        setupYoutubeInput();
        setupRecentQuizzes();
        setupQuizCreationButtons(view);

        tvViewMoreRecent.setOnClickListener(v -> {
            ActivityFragment.shouldScrollToHistory = true;
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation_view);
            bottomNav.setSelectedItemId(R.id.nav_activity);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        isProcessing = false;
        progressBar.setVisibility(View.GONE);
        loadRecentQuizzes();
    }

    private void setupQuizCreationButtons(View view) {
        View createFromDocButton = view.findViewById(R.id.btn_create_from_document);
        View createFromYtButton = view.findViewById(R.id.btn_generate_from_youtube);
        ImageButton btnOpenYoutube = view.findViewById(R.id.btn_youtube_icon);

        createFromDocButton.setOnClickListener(v -> {
            if (isProcessing) return;
            checkNetworkAndProceed(() -> {
                isProcessing = true;
                progressBar.setVisibility(View.VISIBLE);
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimetypes = {"application/pdf", "image/*", "text/plain"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                filePickerLauncher.launch(intent);
            });
        });

        createFromYtButton.setOnClickListener(v -> {
            if (isProcessing) return;
            String url = Objects.requireNonNull(etYoutubeUrl.getText()).toString().trim();

            if (!url.toLowerCase().contains("youtube.com") && !url.toLowerCase().contains("youtu.be")) {
                showUrlError("Invalid YouTube URL");
                return;
            }

            if(url.isEmpty()) {
                showUrlError(getString(R.string.error_url_required));
                return;
            }
            checkNetworkAndProceed(() -> {
                isProcessing = true;
                processYouTube(url);
            });
        });

        btnOpenYoutube.setOnClickListener(v -> {
            String url = Objects.requireNonNull(etYoutubeUrl.getText()).toString().trim();
            if(url.isEmpty()) {
                showUrlError(getString(R.string.error_url_required));
            } else {
                if (!url.toLowerCase().contains("youtube.com") && !url.toLowerCase().contains("youtu.be")) {
                    showUrlError("Invalid YouTube URL");
                    return;
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    showErrorDialog("Error", "Could not open YouTube Video");
                }
            }
        });
    }

    private void checkNetworkAndProceed(Runnable action) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            action.run();
        } else {
            showErrorDialog("No Internet", "You need an internet connection to generate quizzes.");
            isProcessing = false;
            progressBar.setVisibility(View.GONE);
        }
    }

    private void processDocument(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        fileExtractor.extractText(uri, new FileExtractor.ExtractionCallback() {
            @Override
            public void onSuccess(String text) {
                QuizDataHolder.getInstance().setExtractedText(text);
                launchQuizConfig("DOCUMENT", "File: " + getFileName(uri));
            }

            @Override
            public void onError(Throwable t) {
                isProcessing = false;
                progressBar.setVisibility(View.GONE);
                showErrorDialog("File Error", "Could not read this file. Please try a standard PDF or Image.");
            }
        });
    }

    private void processYouTube(String url) {
        progressBar.setVisibility(View.VISIBLE);
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        youTubeHelper.getTranscript(url, new YouTubeHelper.TranscriptCallback() {
            @Override
            public void onSuccess(String transcriptText, String videoId) {
                QuizDataHolder.getInstance().setExtractedText(transcriptText);
                launchQuizConfig("YOUTUBE", "https://youtu.be/" + videoId);
            }

            @Override
            public void onError(Throwable t) {
                isProcessing = false;
                progressBar.setVisibility(View.GONE);
                showErrorDialog("YouTube Error", "Could not get video info. Ensure video has captions.");
            }
        });
    }

    private void launchQuizConfig(String type, String dataName) {
        Intent intent = new Intent(requireActivity(), QuizActivity.class);
        intent.putExtra("SOURCE_TYPE", type);
        intent.putExtra("SOURCE_DATA", dataName);
        startActivity(intent);
    }

    private String getFileName(Uri uri) {
        String result = "Document";
        if (Objects.equals(uri.getScheme(), "content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if(index >= 0) result = cursor.getString(index);
                }
            } catch (Exception e) {
                return "Document";
            }
        }
        return result;
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

    private void setupRecentQuizzes() {
        quizAdapter = new QuizAdapter(requireContext(), recentQuizRecords,
                quizId -> {
                    if (isProcessing) return;
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.home_fragment_host, QuizReviewFragment.newInstance(quizId))
                            .addToBackStack(null)
                            .commit();
                },
                this::showDeleteDialog
        );
        recyclerViewRecentQuizzes.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewRecentQuizzes.setAdapter(quizAdapter);
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
            requireActivity().runOnUiThread(this::loadRecentQuizzes);
        });
    }

    private void loadRecentQuizzes() {
        String currentUid = sessionManager.getUserId();
        if (currentUid == null) return;

        quizDatabase.getRecentQuizzes(currentUid, quizzes -> {
            if (isAdded()) {
                if (quizzes.isEmpty()) {
                    recyclerViewRecentQuizzes.setVisibility(View.GONE);
                    noRecentQuizzesView.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewRecentQuizzes.setVisibility(View.VISIBLE);
                    noRecentQuizzesView.setVisibility(View.GONE);

                    if (quizAdapter != null) {
                        quizAdapter.updateList(quizzes);
                    }
                }
            }
        });
    }

    private void setupProfileCard(View view) {
        View profileCard = view.findViewById(R.id.cv_profile_card);
        View editIcon = view.findViewById(R.id.iv_edit_profile);
        TextView tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserName.setText(sessionManager.getUserName());

        View.OnClickListener goToProfileListener = v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation_view);
            bottomNav.setSelectedItemId(R.id.nav_profile);
        };

        profileCard.setOnClickListener(goToProfileListener);
        editIcon.setOnClickListener(goToProfileListener);
    }

    private void setupYoutubeInput() {
        youtubeUrlInput.setEndIconOnClickListener(v -> {
            if (isOptionsVisible) {
                etYoutubeUrl.setText("");
            } else {
                String url = Objects.requireNonNull(etYoutubeUrl.getText()).toString().trim();
                if (url.isEmpty()) {
                    showUrlError(getString(R.string.error_url_required));
                } else {
                    showYoutubeOptions(true);
                }
            }
        });

        etYoutubeUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                youtubeUrlInput.setError(null);
                handler.removeCallbacks(hideErrorRunnable);
                if (isOptionsVisible) showYoutubeOptions(false);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showUrlError(String message) {
        handler.removeCallbacks(hideErrorRunnable);
        youtubeUrlInput.setError(message);
        hideErrorRunnable = () -> youtubeUrlInput.setError(null);
        handler.postDelayed(hideErrorRunnable, 3000);
    }

    private void showYoutubeOptions(boolean show) {
        TransitionManager.beginDelayedTransition(homeContainer);
        if (show) {
            youtubeOptionsContainer.setVisibility(View.VISIBLE);
            youtubeUrlInput.setEndIconDrawable(R.drawable.ic_clear_white_circle_purple);
            isOptionsVisible = true;
        } else {
            youtubeOptionsContainer.setVisibility(View.GONE);
            youtubeUrlInput.setEndIconDrawable(R.drawable.ic_arrow_forward);
            isOptionsVisible = false;
        }
    }
}