package com.yourapp.healthapp.models;

import java.util.Date;

/**
 * Model đại diện cho một điểm dữ liệu giấc ngủ
 * Mỗi object lưu thông tin tại một thời điểm cụ thể
 */
public class SleepData {
    
    private long id;                    // ID trong database
    private long sessionId;             // ID của phiên ngủ (foreign key)
    private long timestamp;             // Thời điểm đo (milliseconds)
    private double movementX;           // Gia tốc trục X (m/s²)
    private double movementY;           // Gia tốc trục Y (m/s²)
    private double movementZ;           // Gia tốc trục Z (m/s²)
    private double movementMagnitude;   // Độ lớn vector = sqrt(x²+y²+z²)
    private SleepStage stage;           // Giai đoạn ngủ
    
    /**
     * Constructor mặc định
     */
    public SleepData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor đầy đủ
     */
    public SleepData(long sessionId, double x, double y, double z) {
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
        this.movementX = x;
        this.movementY = y;
        this.movementZ = z;
        this.movementMagnitude = calculateMagnitude(x, y, z);
        this.stage = SleepStage.classifyFromMovement(movementMagnitude);
    }
    
    /**
     * Tính độ lớn vector chuyển động
     * Công thức: magnitude = √(x² + y² + z²)
     */
    private double calculateMagnitude(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    /**
     * Cập nhật giai đoạn ngủ dựa trên độ lớn chuyển động
     */
    public void updateStage() {
        this.stage = SleepStage.classifyFromMovement(this.movementMagnitude);
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Date getDate() {
        return new Date(timestamp);
    }
    
    public double getMovementX() {
        return movementX;
    }
    
    public void setMovementX(double movementX) {
        this.movementX = movementX;
        this.movementMagnitude = calculateMagnitude(movementX, movementY, movementZ);
        updateStage();
    }
    
    public double getMovementY() {
        return movementY;
    }
    
    public void setMovementY(double movementY) {
        this.movementY = movementY;
        this.movementMagnitude = calculateMagnitude(movementX, movementY, movementZ);
        updateStage();
    }
    
    public double getMovementZ() {
        return movementZ;
    }
    
    public void setMovementZ(double movementZ) {
        this.movementZ = movementZ;
        this.movementMagnitude = calculateMagnitude(movementX, movementY, movementZ);
        updateStage();
    }
    
    public double getMovementMagnitude() {
        return movementMagnitude;
    }
    
    public SleepStage getStage() {
        return stage;
    }
    
    public void setStage(SleepStage stage) {
        this.stage = stage;
    }
    
    /**
     * Định dạng thông tin để debug
     */
    @Override
    public String toString() {
        return "SleepData{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", timestamp=" + timestamp +
                ", magnitude=" + String.format("%.2f", movementMagnitude) +
                ", stage=" + stage.getDisplayName() +
                '}';
    }
    
    /**
     * Kiểm tra xem dữ liệu có hợp lệ không
     */
    public boolean isValid() {
        return sessionId > 0 && 
               timestamp > 0 && 
               !Double.isNaN(movementMagnitude) &&
               stage != null;
    }
}
