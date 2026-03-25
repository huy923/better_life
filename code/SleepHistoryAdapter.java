package com.yourapp.healthapp.activities;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.yourapp.healthapp.R;
import com.yourapp.healthapp.models.SleepSession;

import java.util.List;

/**
 * Adapter cho RecyclerView hiển thị danh sách sessions
 */
public class SleepHistoryAdapter extends RecyclerView.Adapter<SleepHistoryAdapter.ViewHolder> {
    
    private List<SleepSession> sessions;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(SleepSession session);
    }
    
    public SleepHistoryAdapter(List<SleepSession> sessions, OnItemClickListener listener) {
        this.sessions = sessions;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sleep_session, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SleepSession session = sessions.get(position);
        holder.bind(session);
    }
    
    @Override
    public int getItemCount() {
        return sessions.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private CardView cardView;
        private TextView tvDate;
        private TextView tvDuration;
        private TextView tvQuality;
        private TextView tvQualityRating;
        private TextView tvDeepSleep;
        private TextView tvLightSleep;
        private TextView tvRemSleep;
        private View qualityIndicator;
        
        ViewHolder(View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.cardView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvQuality = itemView.findViewById(R.id.tvQuality);
            tvQualityRating = itemView.findViewById(R.id.tvQualityRating);
            tvDeepSleep = itemView.findViewById(R.id.tvDeepSleep);
            tvLightSleep = itemView.findViewById(R.id.tvLightSleep);
            tvRemSleep = itemView.findViewById(R.id.tvRemSleep);
            qualityIndicator = itemView.findViewById(R.id.qualityIndicator);
        }
        
        void bind(SleepSession session) {
            // Ngày tháng
            tvDate.setText(session.getFormattedDate());
            
            // Thời gian ngủ
            tvDuration.setText(session.getFormattedDuration());
            
            // Chất lượng
            double quality = session.getSleepQuality();
            tvQuality.setText(String.format("%.1f/100", quality));
            tvQualityRating.setText(session.getQualityRating());
            
            // Màu chỉ báo chất lượng
            int indicatorColor;
            if (quality >= 80) {
                indicatorColor = Color.parseColor("#4CAF50"); // Green
            } else if (quality >= 60) {
                indicatorColor = Color.parseColor("#8BC34A"); // Light Green
            } else if (quality >= 40) {
                indicatorColor = Color.parseColor("#FFC107"); // Amber
            } else {
                indicatorColor = Color.parseColor("#F44336"); // Red
            }
            qualityIndicator.setBackgroundColor(indicatorColor);
            
            // Thời gian từng giai đoạn
            tvDeepSleep.setText(formatMinutes(session.getDeepSleepDuration()));
            tvLightSleep.setText(formatMinutes(session.getLightSleepDuration()));
            tvRemSleep.setText(formatMinutes(session.getRemSleepDuration()));
            
            // Click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(session);
                }
            });
        }
        
        private String formatMinutes(int minutes) {
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (hours > 0) {
                return String.format("%dh %dm", hours, mins);
            } else {
                return String.format("%dm", mins);
            }
        }
    }
}
