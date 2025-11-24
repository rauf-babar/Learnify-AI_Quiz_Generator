package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OnboardingSlideFragment extends Fragment {

    private static final String ARG_IMAGE_RES = "imageRes";
    private static final String ARG_TITLE = "title";
    private static final String ARG_BODY = "body";

    public static OnboardingSlideFragment newInstance(int imageRes, String title, String body) {
        OnboardingSlideFragment fragment = new OnboardingSlideFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_RES, imageRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_BODY, body);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_slide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView illustration = view.findViewById(R.id.onboarding_illustration);
        TextView title = view.findViewById(R.id.tv_slide_title);
        TextView body = view.findViewById(R.id.tv_slide_body);

        if (getArguments() != null) {
            illustration.setImageResource(getArguments().getInt(ARG_IMAGE_RES));
            title.setText(getArguments().getString(ARG_TITLE));
            body.setText(getArguments().getString(ARG_BODY));
        }
    }
}