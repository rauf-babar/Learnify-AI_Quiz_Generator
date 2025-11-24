package com.example.learnify;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment {

    private TextInputLayout tilEmail;
    private EditText etEmail;
    private Button btnSendEmail;
    private LinearProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        tilEmail = view.findViewById(R.id.til_email);
        etEmail = view.findViewById(R.id.et_email);
        btnSendEmail = view.findViewById(R.id.btn_send_email);
        View backButtonContainer = view.findViewById(R.id.back_button_container);
        progressBar = view.findViewById(R.id.progress_bar_forgot_pass);

        backButtonContainer.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnSendEmail.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                tilEmail.setError("Email is required");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Invalid email");
                return;
            }

            tilEmail.setError(null);
            sendResetEmail(email);
        });
    }

    private void sendResetEmail(String email) {
        btnSendEmail.setEnabled(false);
        btnSendEmail.setText(getString(R.string.sending));
        progressBar.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;

                    btnSendEmail.setEnabled(true);
                    btnSendEmail.setText(getString(R.string.send_email));
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        showSuccessDialog(email);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Error sending email";
                        showErrorDialog("Failed", error);
                    }
                });
    }

    private void showSuccessDialog(String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Check Your Email")
                .setMessage("We have sent a password reset link to " + email)
                .setCancelable(false)
                .setPositiveButton("Back to Login", (dialog, which) -> requireActivity().getSupportFragmentManager().popBackStack())
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