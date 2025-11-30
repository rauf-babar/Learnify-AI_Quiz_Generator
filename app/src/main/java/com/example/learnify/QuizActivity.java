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
    private String passedTopic;

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
        passedTopic = getIntent().getStringExtra("TOPIC_NAME");

        if (savedInstanceState == null) {
            if ("RETAKE".equals(sourceType)) {
                int numQ = getIntent().getIntExtra("NUM_QUESTIONS", 5);
                String diff = getIntent().getStringExtra("DIFFICULTY");
                if (diff == null) diff = "Medium";
                String lang = getIntent().getStringExtra("LANGUAGE");
                if (lang == null) lang = "English";

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.quiz_fragment_host,
                                QuizConfigFragment.newInstance(numQ, diff, lang, true, passedTopic))
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.quiz_fragment_host, new QuizConfigFragment())
                        .commit();
            }
        }
    }

    public void navigateToGame(int numQuestions, String difficulty, long timeLimitMs, String language, String topicName) {
        QuizGameFragment gameFragment = new QuizGameFragment();
        Bundle args = new Bundle();
        args.putInt("NUM_QUESTIONS", numQuestions);
        args.putString("DIFFICULTY", difficulty);
        args.putLong("TIME_LIMIT_MS", timeLimitMs);
        args.putString("LANGUAGE", language);

        args.putString("SOURCE_TYPE", sourceType);
        args.putString("SOURCE_DATA", sourceData);

        if (topicName != null) {
            args.putString("TOPIC_NAME", topicName);
        } else if (passedTopic != null) {
            args.putString("TOPIC_NAME", passedTopic);
        }

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