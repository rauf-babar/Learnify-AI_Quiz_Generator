package com.example.learnify;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingSlideAdapter extends FragmentStateAdapter {

    private final String[] titles;
    private final String[] bodies;
    private final int[] imageResources = {
            R.drawable.onboarding_illustration_one,
            R.drawable.onboarding_illustration_two,
            R.drawable.onboarding_illustration_three
    };

    public OnboardingSlideAdapter(@NonNull Fragment fragment) {
        super(fragment);
        Context context = fragment.requireContext();
        titles = context.getResources().getStringArray(R.array.onboarding_titles);
        bodies = context.getResources().getStringArray(R.array.onboarding_bodies);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingSlideFragment.newInstance(
                imageResources[position],
                titles[position],
                bodies[position]
        );
    }

    @Override
    public int getItemCount() {
        return imageResources.length;
    }
}