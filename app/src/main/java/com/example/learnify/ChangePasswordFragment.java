package com.example.learnify;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class ChangePasswordFragment extends Fragment {

    private TextInputLayout tilOldPass, tilNewPass, tilConfirmPass;
    private EditText etOldPass, etNewPass, etConfirmPass;
    private Button btnResetPassword;
    private LinearProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        tilOldPass = view.findViewById(R.id.til_old_password);
        tilNewPass = view.findViewById(R.id.til_new_password);
        tilConfirmPass = view.findViewById(R.id.til_confirm_password);
        etOldPass = view.findViewById(R.id.et_old_password);
        etNewPass = view.findViewById(R.id.et_new_password);
        etConfirmPass = view.findViewById(R.id.et_confirm_password);
        btnResetPassword = view.findViewById(R.id.btn_reset_password);
        View backButtonContainer = view.findViewById(R.id.back_button_container);
        progressBar = view.findViewById(R.id.progress_bar_change_pass);

        backButtonContainer.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnResetPassword.setOnClickListener(v -> validateAndChangePassword());

        setupTextWatchers();
    }

    private void setupTextWatchers() {
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilOldPass.setError(null);
                tilNewPass.setError(null);
                tilConfirmPass.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etOldPass.addTextChangedListener(clearErrorWatcher);
        etNewPass.addTextChangedListener(clearErrorWatcher);
        etConfirmPass.addTextChangedListener(clearErrorWatcher);
    }

    private void validateAndChangePassword() {
        String oldPass = etOldPass.getText().toString().trim();
        String newPass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        boolean hasError = false;

        if (oldPass.isEmpty()) {
            tilOldPass.setError("Current password required");
            hasError = true;
        }
        if (newPass.length() < 6) {
            tilNewPass.setError("Password must be at least 6 chars");
            hasError = true;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirmPass.setError("Passwords do not match");
            hasError = true;
        }

        if (hasError) return;

        changePassword(oldPass, newPass);
    }

    private void changePassword(String oldPass, String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText(getString(R.string.Updating));
        progressBar.setVisibility(View.VISIBLE);

        // 1. Re-authenticate User
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Update Password
                user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    if (!isAdded()) return;

                    btnResetPassword.setEnabled(true);
                    btnResetPassword.setText(getString(R.string.reset_password));
                    progressBar.setVisibility(View.GONE);

                    if (updateTask.isSuccessful()) {
                        showSuccessDialog();
                    } else {
                        showErrorDialog("Update Failed", "Could not update password. " + Objects.requireNonNull(updateTask.getException()).getMessage());
                    }
                });
            } else {
                if (!isAdded()) return;

                btnResetPassword.setEnabled(true);
                btnResetPassword.setText(getString(R.string.reset_password));
                progressBar.setVisibility(View.GONE);

                Exception e = task.getException();
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    tilOldPass.setError("Incorrect current password");
                } else {
                    showErrorDialog("Authentication Error", "Could not verify your account. Please try again.");
                }
            }
        });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Password Updated")
                .setMessage("Your password has been changed successfully.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> requireActivity().getSupportFragmentManager().popBackStack())
                .show();
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