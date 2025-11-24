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

    private int selectedQCount = 5;
    private String selectedDifficulty = "Medium";

    private TextView btnQ5, btnQ10, btnQ15;
    private TextView btnEasy, btnMedium, btnHard;
    private TextView tvEstimatedTime;

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

        tvEstimatedTime = view.findViewById(R.id.tv_estimated_time);
        MaterialButton btnStart = view.findViewById(R.id.btn_start_quiz);
        view.findViewById(R.id.back_button_container).setOnClickListener(v -> requireActivity().finish());

        updateQCountUI();
        updateDifficultyUI();
        calculateTime();

        btnQ5.setOnClickListener(v -> { selectedQCount = 5; updateQCountUI(); calculateTime(); });
        btnQ10.setOnClickListener(v -> { selectedQCount = 10; updateQCountUI(); calculateTime(); });
        btnQ15.setOnClickListener(v -> { selectedQCount = 15; updateQCountUI(); calculateTime(); });

        btnEasy.setOnClickListener(v -> { selectedDifficulty = "Easy"; updateDifficultyUI(); calculateTime(); });
        btnMedium.setOnClickListener(v -> { selectedDifficulty = "Medium"; updateDifficultyUI(); calculateTime(); });
        btnHard.setOnClickListener(v -> { selectedDifficulty = "Hard"; updateDifficultyUI(); calculateTime(); });

        btnStart.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).navigateToGame(selectedQCount, selectedDifficulty, calculateTimeMs());
            }
        });
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

    private void setButtonState(TextView btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_button_solid_purple);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.youtube_white));
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