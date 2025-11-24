package com.example.learnify;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class QuizActivity extends AppCompatActivity {

    private String sourceType;
    private String sourceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sourceType = getIntent().getStringExtra("SOURCE_TYPE");
        sourceData = getIntent().getStringExtra("SOURCE_DATA");

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.quiz_fragment_host, new QuizConfigFragment())
                    .commit();
        }
    }

    public void navigateToGame(int numQuestions, String difficulty, long timeLimitMs) {
        QuizGameFragment gameFragment = new QuizGameFragment();
        Bundle args = new Bundle();
        args.putInt("NUM_QUESTIONS", numQuestions);
        args.putString("DIFFICULTY", difficulty);
        args.putLong("TIME_LIMIT_MS", timeLimitMs);

        args.putString("SOURCE_TYPE", sourceType);
        args.putString("SOURCE_DATA", sourceData);

        gameFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.quiz_fragment_host, gameFragment)
                .commit();
    }

     public void onAnswerSelected(int index) {
        androidx.fragment.app.Fragment navHost = getSupportFragmentManager().findFragmentById(R.id.quiz_fragment_host);
        if (navHost instanceof QuizGameFragment) {
            ((QuizGameFragment) navHost).onAnswerSelected(index);
        }
    }
}