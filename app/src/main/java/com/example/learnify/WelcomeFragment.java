package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View signInButton = view.findViewById(R.id.btn_sign_in);
        View signUpButton = view.findViewById(R.id.btn_sign_up);
        View backButton = view.findViewById(R.id.back_button_container);

        signInButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.auth_fragment_host, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        signUpButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.auth_fragment_host, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        backButton.setOnClickListener(v -> {
            requireActivity().finish();
        });
    }
}