package com.example.learnify;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class QuizQuestionAdapter extends FragmentStateAdapter {

    private final List<QuizQuestion> questions;

    public QuizQuestionAdapter(@NonNull Fragment fragment, List<QuizQuestion> questions) {
        super(fragment);
        this.questions = questions;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        QuizQuestion question = questions.get(position);
        return QuestionFragment.newInstance(question);
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }
}