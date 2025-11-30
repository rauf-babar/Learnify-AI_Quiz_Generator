package com.example.learnify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizDownloadAdapter extends RecyclerView.Adapter<QuizDownloadAdapter.ViewHolder> {

    private final Context context;
    private final List<CloudQuiz> quizList;
    private final OnDownloadClickListener listener;

    public QuizDownloadAdapter(Context context, List<CloudQuiz> quizList, OnDownloadClickListener listener) {
        this.context = context;
        this.quizList = new ArrayList<>(quizList);
        this.listener = listener;
    }

    public void updateList(List<CloudQuiz> newList) {
        quizList.clear();
        quizList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_quiz, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CloudQuiz item = quizList.get(position);
        QuizRecord record = item.getRecord();

        holder.tvTopic.setText(record.getTopicName());

        String dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(record.getCompletedAt()));
        holder.tvDate.setText(dateStr);

        holder.tvScore.setText(String.format(Locale.getDefault(), "%d%%", (int)record.getAccuracyPercentage()));
        holder.tvQuestions.setText(record.getTotalQuestions() + " Qs");

        String source = record.getSource();
        String sourceText = "Document";
        int iconRes = R.drawable.ic_document_file;

        if ("YOUTUBE".equals(source)) {
            sourceText = "YouTube";
            iconRes = R.drawable.ic_video_source;
        } else if ("REGENERATE".equals(source) || "RETAKE".equals(source)) {
            sourceText = "Practice";
            iconRes = R.drawable.ic_revision;
        }

        holder.tvSource.setText(sourceText);
        holder.btnDownload.setIconResource(iconRes);

        holder.btnDownload.setOnClickListener(v -> listener.onDownloadClick(item));
    }

    @Override
    public int getItemCount() {
        return quizList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDate, tvScore, tvQuestions, tvSource;
        MaterialButton btnDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tv_topic);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvQuestions = itemView.findViewById(R.id.tv_questions);
            tvSource = itemView.findViewById(R.id.tv_source);
            btnDownload = itemView.findViewById(R.id.btn_download);
        }
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(CloudQuiz cloudQuiz);
    }
}