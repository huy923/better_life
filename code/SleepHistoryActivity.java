package com.yourapp.healthapp.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.yourapp.healthapp.R;
import com.yourapp.healthapp.database.SleepDataDAO;
import com.yourapp.healthapp.models.SleepSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity hiển thị lịch sử giấc ngủ
 * Bao gồm danh sách sessions và biểu đồ thống kê
 */
public class SleepHistoryActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private SleepHistoryAdapter adapter;
    private SleepDataDAO dataDAO;
    private List<SleepSession> sessionList;
    
    private TextView tvNoData;
    private TextView tvAverageDuration;
    private TextView tvAverageQuality;
    private PieChart pieChart;
    private BarChart barChart;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_history);
        
        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lịch Sử Giấc Ngủ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize
        dataDAO = new SleepDataDAO(this);
        initializeViews();
        loadData();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewHistory);
        tvNoData = findViewById(R.id.tvNoData);
        tvAverageDuration = findViewById(R.id.tvAverageDuration);
        tvAverageQuality = findViewById(R.id.tvAverageQuality);
        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionList = new ArrayList<>();
        adapter = new SleepHistoryAdapter(sessionList, this::onSessionClick);
        recyclerView.setAdapter(adapter);
    }
    
    private void loadData() {
        // Load all sessions
        sessionList.clear();
        sessionList.addAll(dataDAO.getAllSessions());
        
        if (sessionList.isEmpty()) {
            tvNoData.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            pieChart.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
        } else {
            tvNoData.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.VISIBLE);
            
            adapter.notifyDataSetChanged();
            calculateStatistics();
            setupCharts();
        }
    }
    
    /**
     * Tính toán thống kê tổng quát
     */
    private void calculateStatistics() {
        if (sessionList.isEmpty()) {
            return;
        }
        
        // Tính trung bình
        int totalDuration = 0;
        double totalQuality = 0;
        int validSessions = 0;
        
        for (SleepSession session : sessionList) {
            if (session.getEndTime() > 0) { // Chỉ tính sessions đã kết thúc
                totalDuration += session.getTotalDuration();
                totalQuality += session.getSleepQuality();
                validSessions++;
            }
        }
        
        if (validSessions > 0) {
            int avgDuration = totalDuration / validSessions;
            double avgQuality = totalQuality / validSessions;
            
            int hours = avgDuration / 60;
            int minutes = avgDuration % 60;
            
            tvAverageDuration.setText(String.format("%d giờ %d phút", hours, minutes));
            tvAverageQuality.setText(String.format("%.1f/100", avgQuality));
        }
    }
    
    /**
     * Thiết lập biểu đồ
     */
    private void setupCharts() {
        setupPieChart();
        setupBarChart();
    }
    
    /**
     * Biểu đồ tròn - Phân bố giai đoạn ngủ trung bình
     */
    private void setupPieChart() {
        // Tính tổng thời gian mỗi giai đoạn
        int totalDeep = 0;
        int totalLight = 0;
        int totalRem = 0;
        int totalAwake = 0;
        int validSessions = 0;
        
        for (SleepSession session : sessionList) {
            if (session.getEndTime() > 0) {
                totalDeep += session.getDeepSleepDuration();
                totalLight += session.getLightSleepDuration();
                totalRem += session.getRemSleepDuration();
                totalAwake += session.getAwakeDuration();
                validSessions++;
            }
        }
        
        if (validSessions == 0) {
            return;
        }
        
        // Tạo entries
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(totalDeep, "Ngủ Sâu"));
        entries.add(new PieEntry(totalLight, "Ngủ Nhẹ"));
        entries.add(new PieEntry(totalRem, "REM"));
        entries.add(new PieEntry(totalAwake, "Thức"));
        
        // Tạo dataset
        PieDataSet dataSet = new PieDataSet(entries, "Phân Bố Giai Đoạn Ngủ");
        dataSet.setColors(new int[]{
            Color.parseColor("#1976D2"), // Deep
            Color.parseColor("#64B5F6"), // Light
            Color.parseColor("#FFB74D"), // REM
            Color.parseColor("#EF5350")  // Awake
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        
        // Customize chart
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setDescription(null);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
    
    /**
     * Biểu đồ cột - Chất lượng giấc ngủ 7 ngày gần nhất
     */
    private void setupBarChart() {
        // Lấy 7 sessions gần nhất
        List<SleepSession> recentSessions = new ArrayList<>();
        for (int i = 0; i < Math.min(7, sessionList.size()); i++) {
            if (sessionList.get(i).getEndTime() > 0) {
                recentSessions.add(sessionList.get(i));
            }
        }
        
        if (recentSessions.isEmpty()) {
            return;
        }
        
        // Tạo entries (đảo ngược để mới nhất ở bên phải)
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = recentSessions.size() - 1; i >= 0; i--) {
            float quality = (float) recentSessions.get(i).getSleepQuality();
            entries.add(new BarEntry(recentSessions.size() - 1 - i, quality));
        }
        
        // Tạo dataset
        BarDataSet dataSet = new BarDataSet(entries, "Chất Lượng Giấc Ngủ");
        dataSet.setColor(Color.parseColor("#1976D2"));
        dataSet.setValueTextSize(12f);
        
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        
        barChart.setData(data);
        
        // Customize chart
        barChart.setDescription(null);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.getLegend().setEnabled(false);
        
        // X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = recentSessions.size() - 1 - (int) value;
                if (index >= 0 && index < recentSessions.size()) {
                    // Hiển thị ngày
                    return recentSessions.get(index).getFormattedDate().substring(0, 5);
                }
                return "";
            }
        });
        
        // Y Axis
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisRight().setEnabled(false);
        
        barChart.animateY(1000);
        barChart.invalidate();
    }
    
    /**
     * Xử lý khi click vào session
     */
    private void onSessionClick(SleepSession session) {
        // Có thể mở màn hình chi tiết session
        Toast.makeText(this, "Chi tiết: " + session.getFormattedDate(), Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
