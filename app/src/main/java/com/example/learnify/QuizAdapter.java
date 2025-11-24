package com.example.learnify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private final List<QuizRecord> quizRecords;
    private final Context context;
    private final OnQuizClickListener listener;

    public QuizAdapter(Context context, List<QuizRecord> quizRecords, OnQuizClickListener listener) {
        this.context = context;
        this.quizRecords = quizRecords;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.quiz_record_item, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        QuizRecord record = quizRecords.get(position);

        holder.tvQuizTopic.setText(record.getTopicName());
        holder.tvQuizTime.setText(record.getTimeFormatted(context));
        holder.tvQuizAccuracy.setText(String.format(Locale.getDefault(), "%.0f%% Accuracy", record.getAccuracyPercentage()));

        int sourceIconRes;
        String source = record.getSource();

        if ("YOUTUBE".equals(source)) {
            sourceIconRes = R.drawable.ic_video_source;
        } else if ("REGENERATE".equals(source)) {
            sourceIconRes = R.drawable.ic_revision;
        } else {
            sourceIconRes = R.drawable.ic_document_file;
        }

        holder.ivQuizSource.setImageResource(sourceIconRes);

        if (record.getAccuracyPercentage() >= 80) {
            holder.tvQuizAccuracy.setTextColor(ContextCompat.getColor(context, R.color.green_success));
        } else if (record.getAccuracyPercentage() <= 40) {
            holder.tvQuizAccuracy.setTextColor(ContextCompat.getColor(context, R.color.red_warning));
        } else {
            holder.tvQuizAccuracy.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_gray));
        }

        holder.btnGoToQuiz.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuizClick(record.getQuizId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return quizRecords.size();
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        final TextView tvQuizTopic, tvQuizAccuracy, tvQuizTime;
        final ImageView ivQuizSource;
        final FloatingActionButton btnGoToQuiz;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizTopic = itemView.findViewById(R.id.tv_quiz_topic);
            tvQuizAccuracy = itemView.findViewById(R.id.tv_quiz_accuracy);
            tvQuizTime = itemView.findViewById(R.id.tv_quiz_time);
            ivQuizSource = itemView.findViewById(R.id.iv_quiz_source);
            btnGoToQuiz = itemView.findViewById(R.id.btn_go_to_quiz);
        }
    }

    public interface OnQuizClickListener {
        void onQuizClick(String quizId);
    }
}