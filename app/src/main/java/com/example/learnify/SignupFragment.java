package com.example.learnify;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
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
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class SignupFragment extends Fragment {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private TextInputLayout tilName, tilEmail, tilPassword;
    private EditText etName, etEmail, etPassword;
    private Button btnSignUp;
    private View backButtonContainer;
    private TextView tvLoginLink;
    private LinearProgressIndicator progressBar;

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        tilName = view.findViewById(R.id.til_name);
        tilEmail = view.findViewById(R.id.til_email);
        tilPassword = view.findViewById(R.id.til_password);
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnSignUp = view.findViewById(R.id.btn_sign_up);
        backButtonContainer = view.findViewById(R.id.back_button_container);
        tvLoginLink = view.findViewById(R.id.tv_log_in_link);
        progressBar = view.findViewById(R.id.progress_bar_signup);

        setupClickListeners();
        setupTextWatchers();
    }

    private void setupClickListeners() {
        backButtonContainer.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        tvLoginLink.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.auth_fragment_host, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnSignUp.setOnClickListener(v -> validateAndSignUp());
    }

    private void setupTextWatchers() {
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilName.setError(null);
                tilEmail.setError(null);
                tilPassword.setError(null);
                tilName.setEndIconMode(TextInputLayout.END_ICON_NONE);
                tilEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etName.addTextChangedListener(clearErrorWatcher);
        etEmail.addTextChangedListener(clearErrorWatcher);
        etPassword.addTextChangedListener(clearErrorWatcher);
    }

    private boolean isValidEmail(CharSequence email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void validateAndSignUp() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;

        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_name_required));
            hasError = true;
        }

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
            hasError = true;
        } else if (!isValidEmail(email)) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            hasError = true;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.error_password_required));
            hasError = true;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError(getString(R.string.error_password_length));
            hasError = true;
        }

        if (hasError) return;

        performSignUp(name, email, password);
    }

    private void performSignUp(String name, String email, String password) {
        btnSignUp.setEnabled(false);
        btnSignUp.setText(getString(R.string.signing_up));
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (!isAdded()) return;

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            onSignupSucceed(user, name);
                        }
                    } else {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText(getString(R.string.sign_up));
                        progressBar.setVisibility(View.GONE);

                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthUserCollisionException) {
                            showErrorDialog("Account Exists", "This email is already registered.\nPlease log in instead.");
                        } else if (exception instanceof FirebaseNetworkException) {
                            showErrorDialog("No Internet", "Please check your connection and try again.");
                        } else {
                            String msg = exception != null ? exception.getMessage() : "An unknown error occurred.";
                            showErrorDialog("Sign Up Failed", msg);
                        }
                    }
                });
    }

    private void onSignupSucceed(FirebaseUser user, String name) {
        tilName.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilName.setEndIconDrawable(R.drawable.ic_done_check_solid);
        tilName.setEndIconTintList(null);

        tilEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilEmail.setEndIconDrawable(R.drawable.ic_done_check_solid);
        tilEmail.setEndIconTintList(null);

        FirestoreManager.getInstance().saveUserProfile(name, user.getEmail());

        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (!isAdded()) return;

            mAuth.signOut();
            progressBar.setVisibility(View.GONE);
            btnSignUp.setText(getString(R.string.sign_up));

            if (task.isSuccessful()) {
                showSuccessDialog(user.getEmail());
            } else {
                btnSignUp.setEnabled(true);
                showErrorDialog("Verification Failed", "Account created, but we couldn't send the email. Please try logging in to resend.");
            }
        });
    }

    private void showSuccessDialog(String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Account Created!")
                .setMessage("We have sent a verification link to " + email + ".\n\nPlease verify your email before logging in.")
                .setCancelable(false)
                .setPositiveButton("Go to Login", (dialog, which) -> {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.auth_fragment_host, new LoginFragment())
                            .commit();
                })
                .show();
    }

    private void showErrorDialog(String title, String message) {
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