package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class QuizConfigFragment extends Fragment {

    private static final String ARG_Q_COUNT = "q_count";
    private static final String ARG_DIFF = "diff";
    private static final String ARG_LANG = "lang";
    private static final String ARG_READ_ONLY = "read_only";
    private static final String ARG_TOPIC = "topic_name";

    private int selectedQCount = 5;
    private String selectedDifficulty = "Medium";
    private String selectedLanguage = "English";
    private boolean isReadOnly = false;
    private String passedTopic = null;

    private TextView btnQ5, btnQ10, btnQ15;
    private TextView btnEasy, btnMedium, btnHard;
    private TextView btnLangEnglish, btnLangSource;
    private TextView tvEstimatedTime;

    public static QuizConfigFragment newInstance(int qCount, String diff, String lang, boolean readOnly, String topicName) {
        QuizConfigFragment fragment = new QuizConfigFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_Q_COUNT, qCount);
        args.putString(ARG_DIFF, diff);
        args.putString(ARG_LANG, lang);
        args.putBoolean(ARG_READ_ONLY, readOnly);
        args.putString(ARG_TOPIC, topicName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedQCount = getArguments().getInt(ARG_Q_COUNT, 5);
            selectedDifficulty = getArguments().getString(ARG_DIFF, "Medium");
            selectedLanguage = getArguments().getString(ARG_LANG, "English");
            isReadOnly = getArguments().getBoolean(ARG_READ_ONLY, false);
            passedTopic = getArguments().getString(ARG_TOPIC);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnQ5 = view.findViewById(R.id.btn_q5);
        btnQ10 = view.findViewById(R.id.btn_q10);
        btnQ15 = view.findViewById(R.id.btn_q15);

        btnEasy = view.findViewById(R.id.btn_easy);
        btnMedium = view.findViewById(R.id.btn_medium);
        btnHard = view.findViewById(R.id.btn_hard);

        btnLangEnglish = view.findViewById(R.id.btn_lang_english);
        btnLangSource = view.findViewById(R.id.btn_lang_source);

        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time);
        MaterialButton btnStart = view.findViewById(R.id.btn_start_quiz);
        view.findViewById(R.id.back_button_container).setOnClickListener(v -> requireActivity().finish());

        updateQCountUI();
        updateDifficultyUI();
        updateLanguageUI();
        calculateTime();

        if (isReadOnly) {
            disableInputs();
            btnStart.setText(R.string.retake_quiz);
        } else {
            setupListeners();
        }

        btnStart.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).navigateToGame(selectedQCount, selectedDifficulty, calculateTimeMs(), selectedLanguage, passedTopic);
            }
        });
    }

    private void setupListeners() {
        btnQ5.setOnClickListener(v -> { selectedQCount = 5; updateQCountUI(); calculateTime(); });
        btnQ10.setOnClickListener(v -> { selectedQCount = 10; updateQCountUI(); calculateTime(); });
        btnQ15.setOnClickListener(v -> { selectedQCount = 15; updateQCountUI(); calculateTime(); });

        btnEasy.setOnClickListener(v -> { selectedDifficulty = "Easy"; updateDifficultyUI(); calculateTime(); });
        btnMedium.setOnClickListener(v -> { selectedDifficulty = "Medium"; updateDifficultyUI(); calculateTime(); });
        btnHard.setOnClickListener(v -> { selectedDifficulty = "Hard"; updateDifficultyUI(); calculateTime(); });

        btnLangEnglish.setOnClickListener(v -> { selectedLanguage = "English"; updateLanguageUI(); });
        btnLangSource.setOnClickListener(v -> { selectedLanguage = "Source"; updateLanguageUI(); });
    }

    private void disableInputs() {
        View[] views = {btnQ5, btnQ10, btnQ15, btnEasy, btnMedium, btnHard, btnLangEnglish, btnLangSource};
        for (View v : views) {
            v.setEnabled(false);
            v.setAlpha(0.7f);
        }
    }

    private void updateQCountUI() {
        setButtonState(btnQ5, selectedQCount == 5);
        setButtonState(btnQ10, selectedQCount == 10);
        setButtonState(btnQ15, selectedQCount == 15);
    }

    private void updateDifficultyUI() {
        setButtonState(btnEasy, selectedDifficulty.equals("Easy"));
        setButtonState(btnMedium, selectedDifficulty.equals("Medium"));
        setButtonState(btnHard, selectedDifficulty.equals("Hard"));
    }

    private void updateLanguageUI() {
        setButtonState(btnLangEnglish, selectedLanguage.equals("English"));
        setButtonState(btnLangSource, selectedLanguage.equals("Source"));
    }

    private void setButtonState(TextView btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_button_solid_purple);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            btn.setBackgroundResource(R.drawable.bg_button_stroked_gray);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary_dark));
        }
    }

    private void calculateTime() {
        long ms = calculateTimeMs();
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;

        String timeStr;
        if (seconds > 0) {
            timeStr = getString(R.string.time_min_sec, minutes, seconds);
        } else {
            timeStr = getString(R.string.time_min_only, minutes);
        }

        tvEstimatedTime.setText(getString(R.string.estimated_time_result, timeStr));
    }

    private long calculateTimeMs() {
        long baseTimePerQ = 60000;
        if (selectedDifficulty.equals("Easy")) baseTimePerQ = 45000;
        if (selectedDifficulty.equals("Hard")) baseTimePerQ = 90000;
        return selectedQCount * baseTimePerQ;
    }
}