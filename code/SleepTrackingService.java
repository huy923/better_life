package com.yourapp.healthapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import com.yourapp.healthapp.R;
import com.yourapp.healthapp.activities.SleepTrackingActivity;
import com.yourapp.healthapp.database.SleepDataDAO;
import com.yourapp.healthapp.models.SleepData;
import com.yourapp.healthapp.models.SleepSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Service chạy nền để theo dõi giấc ngủ
 * Sử dụng Accelerometer để phát hiện chuyển động
 */
public class SleepTrackingService extends Service implements SensorEventListener {
    
    private static final String TAG = "SleepTrackingService";
    private static final String CHANNEL_ID = "SleepTrackingChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Cấu hình
    private static final int SAMPLING_INTERVAL_MS = 5000; // Đo mỗi 5 giây
    private static final int BATCH_SAVE_SIZE = 20; // Lưu 20 điểm dữ liệu một lần
    
    // Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // Wake Lock để service không bị ngủ
    private PowerManager.WakeLock wakeLock;
    
    // Database
    private SleepDataDAO dataDAO;
    
    // Session hiện tại
    private SleepSession currentSession;
    private long currentSessionId = -1;
    
    // Buffer để lưu dữ liệu trước khi ghi vào database
    private List<SleepData> dataBuffer;
    
    // Handler để lấy mẫu định kỳ
    private Handler samplingHandler;
    private Runnable samplingRunnable;
    
    // Biến lưu giá trị sensor gần nhất
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;
    private boolean hasInitialReading = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Khởi tạo database
        dataDAO = new SleepDataDAO(this);
        dataBuffer = new ArrayList<>();
        
        // Khởi tạo sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        
        // Khởi tạo WakeLock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SleepTracker::WakeLock"
            );
        }
        
        // Khởi tạo Handler cho sampling
        samplingHandler = new Handler();
        samplingRunnable = new Runnable() {
            @Override
            public void run() {
                if (hasInitialReading) {
                    recordSleepData();
                }
                samplingHandler.postDelayed(this, SAMPLING_INTERVAL_MS);
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Tạo notification channel (Android 8.0+)
        createNotificationChannel();
        
        // Tạo notification
        Notification notification = buildNotification("Đang theo dõi giấc ngủ...");
        
        // Chạy service ở foreground
        startForeground(NOTIFICATION_ID, notification);
        
        // Bắt đầu tracking
        startTracking();
        
        // Service sẽ tự động restart nếu bị kill
        return START_STICKY;
    }
    
    /**
     * Bắt đầu theo dõi giấc ngủ
     */
    private void startTracking() {
        // Acquire wake lock
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 60 * 1000L); // 10 giờ
        }
        
        // Tạo session mới
        currentSession = new SleepSession(System.currentTimeMillis());
        currentSessionId = dataDAO.createSession(currentSession);
        currentSession.setId(currentSessionId);
        
        // Đăng ký sensor listener
        if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            );
        }
        
        // Bắt đầu sampling
        samplingHandler.post(samplingRunnable);
    }
    
    /**
     * Dừng theo dõi giấc ngủ
     */
    private void stopTracking() {
        // Hủy sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Dừng sampling
        samplingHandler.removeCallbacks(samplingRunnable);
        
        // Lưu dữ liệu còn lại trong buffer
        if (!dataBuffer.isEmpty()) {
            dataDAO.addSleepDataBatch(dataBuffer);
            dataBuffer.clear();
        }
        
        // Kết thúc session
        if (currentSession != null) {
            currentSession.endSession();
            
            // Load tất cả dữ liệu để tính toán thống kê
            List<SleepData> allData = dataDAO.getSleepDataForSession(currentSessionId);
            currentSession.setSleepDataList(allData);
            currentSession.endSession(); // Tính toán lại sau khi có đủ dữ liệu
            
            // Cập nhật vào database
            dataDAO.updateSession(currentSession);
        }
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    /**
     * Ghi lại dữ liệu ngủ tại thời điểm hiện tại
     */
    private void recordSleepData() {
        if (currentSessionId < 0) {
            return;
        }
        
        // Tạo SleepData object
        SleepData data = new SleepData(currentSessionId, lastX, lastY, lastZ);
        
        // Thêm vào buffer
        dataBuffer.add(data);
        
        // Nếu buffer đầy, lưu vào database
        if (dataBuffer.size() >= BATCH_SAVE_SIZE) {
            dataDAO.addSleepDataBatch(new ArrayList<>(dataBuffer));
            dataBuffer.clear();
        }
        
        // Cập nhật notification
        updateNotification(data);
    }
    
    /**
     * Xử lý sự kiện sensor thay đổi
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Lưu giá trị gia tốc
            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];
            hasInitialReading = true;
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }
    
    /**
     * Tạo Notification Channel (Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Theo dõi giấc ngủ",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hiển thị thông tin theo dõi giấc ngủ");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Tạo notification
     */
    private Notification buildNotification(String contentText) {
        // Intent để mở app khi click vào notification
        Intent notificationIntent = new Intent(this, SleepTrackingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Action để dừng tracking
        Intent stopIntent = new Intent(this, SleepTrackingService.class);
        stopIntent.setAction("STOP_TRACKING");
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Theo dõi giấc ngủ")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_sleep) // Bạn cần tạo icon này
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop, "Dừng", stopPendingIntent)
                .setOngoing(true) // Không thể swipe để xóa
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
    
    /**
     * Cập nhật notification với thông tin mới nhất
     */
    private void updateNotification(SleepData data) {
        String stage = data.getStage().getDisplayName();
        String text = "Giai đoạn: " + stage;
        
        Notification notification = buildNotification(text);
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service không cần bind
    }
    
    /**
     * Xử lý action từ notification
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_TRACKING".equals(intent.getAction())) {
            stopSelf(); // Dừng service
            return START_NOT_STICKY;
        }
        
        return super.onStartCommand(intent, flags, startId);
    }
}
