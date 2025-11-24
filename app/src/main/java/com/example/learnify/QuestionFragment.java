package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuestionFragment extends Fragment {

    private static final String ARG_QUESTION = "arg_question";
    private QuizQuestion question;
    private LinearLayout llAnswerContainer;
    private MaterialCardView cvExplanation;
    private final List<MaterialCardView> answerCards = new ArrayList<>();

    private MaterialCardView selectedAnswerCard = null;
    private boolean isAnswered = false;

    public static QuestionFragment newInstance(QuizQuestion question) {
        QuestionFragment fragment = new QuestionFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_QUESTION, (Serializable) question);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            question = (QuizQuestion) getArguments().getSerializable(ARG_QUESTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_question, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (question == null) return;

        TextView tvQuestionText = view.findViewById(R.id.tv_question_text);
        llAnswerContainer = view.findViewById(R.id.ll_answer_container);
        cvExplanation = view.findViewById(R.id.cv_explanation);
        TextView tvExplanationBody = view.findViewById(R.id.tv_explanation_body);

        tvQuestionText.setText(question.getQuestionText());
        tvExplanationBody.setText(question.getExplanation());

        setupAnswerViews();
    }

    private void setupAnswerViews() {
        llAnswerContainer.removeAllViews();
        answerCards.clear();
        String[] letters = {"A", "B", "C", "D"};

        for (int i = 0; i < question.getAnswers().size(); i++) {
            QuizAnswer answer = question.getAnswers().get(i);

            MaterialCardView card = (MaterialCardView) LayoutInflater.from(getContext()).inflate(R.layout.item_answer, llAnswerContainer, false);

            TextView tvLetter = card.findViewById(R.id.tv_answer_letter);
            TextView tvText = card.findViewById(R.id.tv_answer_text);

            if (i < letters.length) tvLetter.setText(letters[i]);
            tvText.setText(answer.getAnswerText());

            card.setTag(answer.isCorrect());
            answerCards.add(card);

            final int answerIndex = i;
            card.setOnClickListener(v -> {
                if (!isAnswered) {
                    selectAnswer(card, answerIndex);
                }
            });

            llAnswerContainer.addView(card);
        }
    }

    private void selectAnswer(MaterialCardView selectedCard, int answerIndex) {
        for (MaterialCardView card : answerCards) {
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.profile_image_background));
        }
        selectedCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.figma_purple_main));

        selectedAnswerCard = selectedCard;

        if (getParentFragment() instanceof QuizGameFragment) {
            ((QuizGameFragment) getParentFragment()).onAnswerSelected(answerIndex);
        } else if (getActivity() instanceof QuizActivity) {
            ((QuizActivity) getActivity()).onAnswerSelected(answerIndex);
        }
    }

    public void showResult() {
        if (selectedAnswerCard == null) return;
        isAnswered = true;

        boolean isSelectedCorrect = (boolean) selectedAnswerCard.getTag();

        for (MaterialCardView card : answerCards) {
            card.setClickable(false);
            boolean isThisCardCorrect = (boolean) card.getTag();
            ImageView icon = card.findViewById(R.id.iv_answer_icon);
            TextView tvLetter = card.findViewById(R.id.tv_answer_letter);

            if (card == selectedAnswerCard) {
                if (isSelectedCorrect) {
                    card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_success));
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.otp_box_background));
                    tvLetter.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success));
                    icon.setImageResource(R.drawable.ic_answer_correct);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.red_warning));
                    card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.illustration_bg_pink_light));
                    tvLetter.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_warning));
                    icon.setImageResource(R.drawable.ic_answer_wrong);
                    icon.setVisibility(View.VISIBLE);
                }
            } else if (isThisCardCorrect) {
                card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.green_success));
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.otp_box_background));
                tvLetter.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_success));
                icon.setImageResource(R.drawable.ic_answer_correct);
                icon.setVisibility(View.VISIBLE);
            } else {
                card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.profile_image_background));
                card.setAlpha(0.5f);
            }
        }

        if (cvExplanation != null) {
            cvExplanation.setVisibility(View.VISIBLE);
        }
    }

    public boolean isAnswered() {
        return isAnswered;
    }

    public boolean isAnswerSelected() {
        return selectedAnswerCard != null;
    }

    public boolean isSelectedAnswerCorrect() {
        if (selectedAnswerCard == null) return false;
        return (boolean) selectedAnswerCard.getTag();
    }
}