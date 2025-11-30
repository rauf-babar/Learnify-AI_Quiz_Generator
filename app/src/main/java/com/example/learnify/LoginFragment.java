package com.example.learnify;

import android.app.AlertDialog;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginFragment extends Fragment {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private View backButtonContainer;
    private TextView tvSignUpLink, tvForgotPassword;
    private LinearProgressIndicator progressBar;

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        mAuth = FirebaseAuth.getInstance();

        tilEmail = view.findViewById(R.id.til_email);
        tilPassword = view.findViewById(R.id.til_password);
        etEmail = view.findViewById(R.id.et_email);
        etPassword = view.findViewById(R.id.et_password);
        btnLogin = view.findViewById(R.id.btn_log_in);
        backButtonContainer = view.findViewById(R.id.back_button_container);
        tvSignUpLink = view.findViewById(R.id.tv_sign_up_link);
        tvForgotPassword = view.findViewById(R.id.tv_forgot_password_link);
        progressBar = view.findViewById(R.id.progress_bar_login);

        setupClickListeners();
        setupTextWatchers();
    }

    private void setupClickListeners() {
        backButtonContainer.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        tvSignUpLink.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.auth_fragment_host, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnLogin.setOnClickListener(v -> validateAndLogin());

        tvForgotPassword.setOnClickListener(v -> requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_host, new ForgotPasswordFragment())
                .addToBackStack(null)
                .commit());
    }

    private void setupTextWatchers() {
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
                tilPassword.setError(null);
                tilEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };
        etEmail.addTextChangedListener(clearErrorWatcher);
        etPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void validateAndLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean hasError = false;

        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.error_email_required));
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

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText(getString(R.string.signing_in));
        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            onLoginSucceed(user);
                        }
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText(getString(R.string.log_in));
                        progressBar.setVisibility(View.GONE);

                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthInvalidUserException || exception instanceof FirebaseAuthInvalidCredentialsException) {
                            showErrorDialog("Login Failed", "Incorrect email or password.");
                        } else {
                            String msg = exception != null ? exception.getMessage() : "Login failed.";
                            showErrorDialog("Login Failed", msg);
                        }
                    }
                });
    }

    private void onLoginSucceed(FirebaseUser user) {
        if (!user.isEmailVerified()) {
            btnLogin.setEnabled(true);
            btnLogin.setText(getString(R.string.log_in));
            progressBar.setVisibility(View.GONE);

            showVerificationDialog(user);
            mAuth.signOut();
            return;
        }

        tilEmail.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        tilEmail.setEndIconDrawable(R.drawable.ic_done_check_solid);
        tilEmail.setEndIconTintList(null);

        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult().getToken();
                String uid = user.getUid();

                FirestoreManager.getInstance().getUserName(uid, name -> FirestoreManager.getInstance().getUserScore(uid, score -> {
                    sessionManager.saveSession(token, uid, name);
                    sessionManager.saveUserScore(score);

                    Intent intent = new Intent(requireActivity(), HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                }));
            }
        });
    }

    private void showVerificationDialog(FirebaseUser user) {
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

        tvTitle.setText(getString(R.string.email_not_verified));
        tvMessage.setText(getString(R.string.verify_email));
        btnAction.setText(getString(R.string.resend_link));

        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showErrorDialog("Link Sent", "A new verification link has been sent to " + user.getEmail());
                } else {
                    showErrorDialog("Error", "Failed to send verification link. Please try again.");
                }
            });
        });

        dialog.show();
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