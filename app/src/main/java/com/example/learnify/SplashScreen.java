package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_SCREEN_TIMEOUT = 2000;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash_screen);

        sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkSession, SPLASH_SCREEN_TIMEOUT);
    }

    private void checkSession() {
        if (sessionManager.isLoggedIn()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkFirestoreProfile(user.getUid());
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException || e instanceof FirebaseAuthInvalidCredentialsException) {
                            handleInvalidSession();
                        } else {
                           navigateToHome();
                        }
                    }
                });
            } else {
                handleInvalidSession();
            }
        } else {
            navigateToOnboarding();
        }
    }

    private void checkFirestoreProfile(String uid) {
        FirestoreManager.getInstance().checkUserExists(uid, exists -> {
            if (exists) {
                navigateToHome();
            } else {
                handleInvalidSession();
            }
        });
    }

    private void handleInvalidSession() {
        sessionManager.clearSession();
        FirebaseAuth.getInstance().signOut();
        navigateToOnboarding();
    }

    private void navigateToHome() {
        Intent intent = new Intent(SplashScreen.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToOnboarding() {
        Intent intent = new Intent(SplashScreen.this, onBoardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}