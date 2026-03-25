package com.yourapp.healthapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yourapp.healthapp.R;
import com.yourapp.healthapp.database.SleepDataDAO;
import com.yourapp.healthapp.models.SleepSession;
import com.yourapp.healthapp.services.SleepTrackingService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity chính để theo dõi giấc ngủ
 * Hiển thị thông tin real-time và điều khiển tracking
 */
public class SleepTrackingActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // UI Components
    private Button btnStartStop;
    private Button btnHistory;
    private TextView tvStatus;
    private TextView tvDuration;
    private TextView tvCurrentStage;
    private TextView tvStartTime;
    private View layoutTracking;
    
    // Database
    private SleepDataDAO dataDAO;
    
    // Tracking state
    private boolean isTracking = false;
    private long trackingStartTime = 0;
    
    // Timer để cập nhật UI
    private Timer updateTimer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_tracking);
        
        // Khởi tạo database
        dataDAO = new SleepDataDAO(this);
        
        // Khởi tạo UI
        initializeViews();
        
        // Kiểm tra quyền
        checkPermissions();
        
        // Kiểm tra xem có session đang chạy không
        checkActiveSession();
    }
    
    /**
     * Khởi tạo các view
     */
    private void initializeViews() {
        btnStartStop = findViewById(R.id.btnStartStop);
        btnHistory = findViewById(R.id.btnHistory);
        tvStatus = findViewById(R.id.tvStatus);
        tvDuration = findViewById(R.id.tvDuration);
        tvCurrentStage = findViewById(R.id.tvCurrentStage);
        tvStartTime = findViewById(R.id.tvStartTime);
        layoutTracking = findViewById(R.id.layoutTracking);
        
        // Ẩn layout tracking ban đầu
        layoutTracking.setVisibility(View.GONE);
        
        // Set listeners
        btnStartStop.setOnClickListener(v -> toggleTracking());
        btnHistory.setOnClickListener(v -> openHistory());
    }
    
    /**
     * Kiểm tra và xin quyền cần thiết
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ cần quyền POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE
                );
            }
        }
    }
    
    /**
     * Kiểm tra xem có session đang chạy không
     */
    private void checkActiveSession() {
        SleepSession activeSession = dataDAO.getActiveSession();
        if (activeSession != null) {
            isTracking = true;
            trackingStartTime = activeSession.getStartTime();
            updateUIForTracking();
            startUIUpdateTimer();
        }
    }
    
    /**
     * Bật/tắt tracking
     */
    private void toggleTracking() {
        if (isTracking) {
            stopTracking();
        } else {
            startTracking();
        }
    }
    
    /**
     * Bắt đầu theo dõi giấc ngủ
     */
    private void startTracking() {
        // Khởi động service
        Intent serviceIntent = new Intent(this, SleepTrackingService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Cập nhật state
        isTracking = true;
        trackingStartTime = System.currentTimeMillis();
        
        // Cập nhật UI
        updateUIForTracking();
        
        // Bắt đầu timer cập nhật UI
        startUIUpdateTimer();
        
        Toast.makeText(this, "Bắt đầu theo dõi giấc ngủ", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Dừng theo dõi giấc ngủ
     */
    private void stopTracking() {
        // Dừng service
        Intent serviceIntent = new Intent(this, SleepTrackingService.class);
        stopService(serviceIntent);
        
        // Cập nhật state
        isTracking = false;
        
        // Dừng timer
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        
        // Cập nhật UI
        updateUIForStopped();
        
        Toast.makeText(this, "Đã dừng theo dõi", Toast.LENGTH_SHORT).show();
        
        // Hiển thị kết quả
        showSessionResult();
    }
    
    /**
     * Cập nhật UI khi đang tracking
     */
    private void updateUIForTracking() {
        btnStartStop.setText("Dừng theo dõi");
        btnStartStop.setBackgroundColor(getColor(R.color.red));
        tvStatus.setText("Đang theo dõi...");
        layoutTracking.setVisibility(View.VISIBLE);
        
        // Hiển thị thời gian bắt đầu
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
        tvStartTime.setText("Bắt đầu: " + sdf.format(new Date(trackingStartTime)));
    }
    
    /**
     * Cập nhật UI khi dừng tracking
     */
    private void updateUIForStopped() {
        btnStartStop.setText("Bắt đầu theo dõi");
        btnStartStop.setBackgroundColor(getColor(R.color.green));
        tvStatus.setText("Nhấn để bắt đầu");
        layoutTracking.setVisibility(View.GONE);
    }
    
    /**
     * Bắt đầu timer cập nhật UI
     */
    private void startUIUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
        
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateTrackingInfo());
            }
        }, 0, 1000); // Cập nhật mỗi giây
    }
    
    /**
     * Cập nhật thông tin tracking
     */
    private void updateTrackingInfo() {
        if (!isTracking) {
            return;
        }
        
        // Tính thời gian đã tracking
        long elapsedMillis = System.currentTimeMillis() - trackingStartTime;
        long hours = elapsedMillis / (1000 * 60 * 60);
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long seconds = (elapsedMillis / 1000) % 60;
        
        String duration = String.format(Locale.getDefault(), 
            "%02d:%02d:%02d", hours, minutes, seconds);
        tvDuration.setText(duration);
        
        // Lấy thông tin session hiện tại
        SleepSession activeSession = dataDAO.getActiveSession();
        if (activeSession != null) {
            // Có thể hiển thị giai đoạn ngủ hiện tại nếu muốn
            // Cần implement thêm logic để lấy dữ liệu mới nhất
        }
    }
    
    /**
     * Hiển thị kết quả session vừa kết thúc
     */
    private void showSessionResult() {
        // Lấy session vừa kết thúc
        // (trong production nên truyền sessionId qua Intent)
        // Ở đây đơn giản hóa bằng cách mở màn hình lịch sử
        openHistory();
    }
    
    /**
     * Mở màn hình lịch sử
     */
    private void openHistory() {
        Intent intent = new Intent(this, SleepHistoryActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTimer != null) {
            updateTimer.cancel();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, 
                    "Cần cấp quyền để hiển thị thông báo", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
