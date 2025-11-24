package com.example.learnify;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private EditText etProfileName;
    private TextView tvProfileEmail, tvRankTitle, tvRankScore;
    private ImageView ivRankIcon;
    private ImageButton btnEditName;
    private LinearLayout rowChangePassword, rowLogout, rowDeleteAccount;
    private SwitchMaterial switchDarkMode;

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private QuizDatabase quizDatabase;
    private boolean isEditingName = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        quizDatabase = QuizDatabase.getInstance(requireContext());
        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        setupClickListeners();
        loadUserData();
        setupDarkModeSwitch();
    }

    private void initializeViews(View view) {
        etProfileName = view.findViewById(R.id.et_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        btnEditName = view.findViewById(R.id.btn_edit_name);

        tvRankTitle = view.findViewById(R.id.tv_rank_title);
        tvRankScore = view.findViewById(R.id.tv_rank_score);
        ivRankIcon = view.findViewById(R.id.iv_rank_icon);

        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        rowChangePassword = view.findViewById(R.id.row_change_password);
        rowLogout = view.findViewById(R.id.row_logout);
        rowDeleteAccount = view.findViewById(R.id.row_delete_account);
    }

    private void setupClickListeners() {
        btnEditName.setOnClickListener(v -> toggleEditName());

        rowChangePassword.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.home_fragment_host, new ChangePasswordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        rowLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        rowDeleteAccount.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadUserData() {
        etProfileName.setText(sessionManager.getUserName());
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvProfileEmail.setText(user.getEmail());
        }

        String uid = sessionManager.getUserId();
        if (uid != null) {
            quizDatabase.getStats(uid, stats -> {
                if (!isAdded()) return;

                int totalPoints = stats.get("totalCorrect");

                requireActivity().runOnUiThread(() -> {
                    updateRankUI(totalPoints);
                    sessionManager.saveUserScore(totalPoints);
                });
            });
        }
    }

    private void updateRankUI(long totalCorrect) {
        tvRankScore.setText(getString(R.string.score_points_format, totalCorrect));

        String rankTitle;
        int rankIconRes;

        if (totalCorrect < 100) {
            rankTitle = "Novice Learner";
            rankIconRes = R.drawable.ic_rank_novice;
        } else if (totalCorrect < 500) {
            rankTitle = "Quiz Enthusiast";
            rankIconRes = R.drawable.ic_rank_enthusiast;
        } else if (totalCorrect < 1000) {
            rankTitle = "Knowledge Seeker";
            rankIconRes = R.drawable.ic_rank_seeker;
        } else {
            rankTitle = "Quiz Master";
            rankIconRes = R.drawable.ic_crown;
        }

        tvRankTitle.setText(rankTitle);
        ivRankIcon.setImageResource(rankIconRes);
        ivRankIcon.setColorFilter(null);
    }

    private void setupDarkModeSwitch() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switchDarkMode.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    private void toggleEditName() {
        if (!isEditingName) {
            startEditing();
        } else {
            saveNameChange();
        }
    }

    private void startEditing() {
        isEditingName = true;
        etProfileName.setEnabled(true);
        etProfileName.requestFocus();
        etProfileName.setSelection(etProfileName.getText().length());

        btnEditName.setImageResource(R.drawable.ic_done_check);
        btnEditName.setColorFilter(null);

        showKeyboard(etProfileName);
    }

    private void saveNameChange() {
        String newName = etProfileName.getText().toString().trim();
        String currentName = sessionManager.getUserName();

        if (newName.isEmpty()) {
            showErrorDialog("Invalid Name", "Name cannot be empty.");
            return;
        }

        if (newName.equals(currentName)) {
            finishEditing();
            return;
        }

        FirestoreManager.getInstance().updateUserName(newName, new FirestoreManager.OnUpdateListener() {
            @Override
            public void onSuccess() {
                sessionManager.saveSession(sessionManager.getUserToken(), sessionManager.getUserId(), newName);
                finishEditing();
            }

            @Override
            public void onFailure(String error) {
                finishEditing();
                etProfileName.setText(sessionManager.getUserName());
                showErrorDialog("Update Failed", "Could not update name. Please check your connection.");
            }
        });
    }

    private void finishEditing() {
        isEditingName = false;
        etProfileName.setEnabled(false);

        btnEditName.setImageResource(R.drawable.ic_edit);
        btnEditName.setColorFilter(getResources().getColor(R.color.figma_purple_main, null));

        hideKeyboard(etProfileName);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        sessionManager.clearSession();
        mAuth.signOut();
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? This action cannot be undone and all your data will be lost.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        String uid = sessionManager.getUserId();
        FirestoreManager.getInstance().deleteUserAccount(new FirestoreManager.OnUpdateListener() {
            @Override
            public void onSuccess() {
                if (uid != null) {
                    quizDatabase.clearUserData(uid);
                }
                performLogout();
            }

            @Override
            public void onFailure(String error) {
                showErrorDialog("Delete Failed", "Could not delete account. Please re-login and try again.");
            }
        });
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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