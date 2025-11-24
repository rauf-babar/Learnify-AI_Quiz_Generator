package com.example.learnify;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.Map;

public class ReviewQuestionAdapter extends RecyclerView.Adapter<ReviewQuestionAdapter.ViewHolder> {

    private final List<QuizQuestion> questions;
    private final Map<Integer, Integer> userAnswers;

    public ReviewQuestionAdapter(List<QuizQuestion> questions, Map<Integer, Integer> userAnswers) {
        this.questions = questions;
        this.userAnswers = userAnswers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        QuizQuestion q = questions.get(position);
        int userAnsIndex = userAnswers.getOrDefault(position, -1);

        holder.tvQuestion.setText(context.getString(R.string.review_question_format, position + 1, q.getQuestionText()));

        int correctIndex = -1;
        for (int i = 0; i < q.getAnswers().size(); i++) {
            if (q.getAnswers().get(i).isCorrect()) correctIndex = i;
        }

        setupOption(context, holder.tvOpt1, holder.cardOpt1, 0, userAnsIndex, correctIndex, q);
        setupOption(context, holder.tvOpt2, holder.cardOpt2, 1, userAnsIndex, correctIndex, q);
        setupOption(context, holder.tvOpt3, holder.cardOpt3, 2, userAnsIndex, correctIndex, q);
        setupOption(context, holder.tvOpt4, holder.cardOpt4, 3, userAnsIndex, correctIndex, q);

        String prefix = context.getString(R.string.label_explanation);
        String body = q.getExplanation();

        SpannableStringBuilder spannable = new SpannableStringBuilder(prefix + body);

        int darkColor = ContextCompat.getColor(context, R.color.illustration_black);
        spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(darkColor), 0, prefix.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int greyColor = ContextCompat.getColor(context, R.color.text_secondary_gray);
        spannable.setSpan(new ForegroundColorSpan(greyColor), prefix.length(), spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.tvExplanation.setText(spannable);
        holder.tvExplanation.setMaxLines(2);

        holder.tvExplanation.setOnClickListener(v -> {
            if (holder.tvExplanation.getMaxLines() == 2) {
                holder.tvExplanation.setMaxLines(Integer.MAX_VALUE);
            } else {
                holder.tvExplanation.setMaxLines(2);
            }
        });
    }

    private void setupOption(Context context, TextView tv, MaterialCardView card, int index, int userIdx, int correctIdx, QuizQuestion q) {
        if (index >= q.getAnswers().size()) return;
        tv.setTextSize(18);
        tv.setText(q.getAnswers().get(index).getAnswerText());

        card.setStrokeColor(ContextCompat.getColor(context, R.color.illustration_check_light));
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
        tv.setTextColor(ContextCompat.getColor(context, R.color.illustration_black));

        if (index == correctIdx) {
            card.setStrokeColor(ContextCompat.getColor(context, R.color.green_success));
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.otp_box_background));
            tv.setTextColor(ContextCompat.getColor(context, R.color.green_success));
        }

        if (index == userIdx && userIdx != correctIdx) {
            card.setStrokeColor(ContextCompat.getColor(context, R.color.red_warning));
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.illustration_bg_pink_light));
            tv.setTextColor(ContextCompat.getColor(context, R.color.red_warning));
        }
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvExplanation;
        TextView tvOpt1, tvOpt2, tvOpt3, tvOpt4;
        MaterialCardView cardOpt1, cardOpt2, cardOpt3, cardOpt4;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tv_question_text);
            tvExplanation = itemView.findViewById(R.id.tv_explanation);

            tvOpt1 = itemView.findViewById(R.id.tv_opt_1); cardOpt1 = itemView.findViewById(R.id.card_opt_1);
            tvOpt2 = itemView.findViewById(R.id.tv_opt_2); cardOpt2 = itemView.findViewById(R.id.card_opt_2);
            tvOpt3 = itemView.findViewById(R.id.tv_opt_3); cardOpt3 = itemView.findViewById(R.id.card_opt_3);
            tvOpt4 = itemView.findViewById(R.id.tv_opt_4); cardOpt4 = itemView.findViewById(R.id.card_opt_4);
        }
    }
}