package com.yourapp.healthapp.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Model đại diện cho một phiên ngủ hoàn chỉnh
 * Từ lúc bắt đầu ngủ đến lúc thức dậy
 */
public class SleepSession {
    
    private long id;                        // ID trong database
    private long startTime;                 // Thời gian bắt đầu (milliseconds)
    private long endTime;                   // Thời gian kết thúc (milliseconds)
    private int totalDuration;              // Tổng thời gian ngủ (phút)
    private int deepSleepDuration;          // Thời gian ngủ sâu (phút)
    private int lightSleepDuration;         // Thời gian ngủ nhẹ (phút)
    private int remSleepDuration;           // Thời gian REM (phút)
    private int awakeDuration;              // Thời gian thức (phút)
    private double sleepQuality;            // Chất lượng giấc ngủ (0-100)
    private int numberOfAwakenings;         // Số lần thức giấc
    private List<SleepData> sleepDataList;  // Danh sách dữ liệu chi tiết
    
    /**
     * Constructor mặc định
     */
    public SleepSession() {
        this.startTime = System.currentTimeMillis();
        this.sleepDataList = new ArrayList<>();
    }
    
    /**
     * Constructor với thời gian bắt đầu
     */
    public SleepSession(long startTime) {
        this.startTime = startTime;
        this.sleepDataList = new ArrayList<>();
    }
    
    /**
     * Kết thúc phiên ngủ và tính toán thống kê
     */
    public void endSession() {
        this.endTime = System.currentTimeMillis();
        calculateStatistics();
    }
    
    /**
     * Tính toán các thống kê từ dữ liệu chi tiết
     */
    private void calculateStatistics() {
        if (sleepDataList == null || sleepDataList.isEmpty()) {
            return;
        }
        
        // Tính tổng thời gian
        long durationMillis = endTime - startTime;
        this.totalDuration = (int) TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        
        // Đếm số lượng mỗi giai đoạn
        int deepCount = 0;
        int lightCount = 0;
        int remCount = 0;
        int awakeCount = 0;
        
        SleepStage previousStage = null;
        int awakenings = 0;
        
        for (SleepData data : sleepDataList) {
            SleepStage stage = data.getStage();
            
            switch (stage) {
                case DEEP:
                    deepCount++;
                    break;
                case LIGHT:
                    lightCount++;
                    break;
                case REM:
                    remCount++;
                    break;
                case AWAKE:
                    awakeCount++;
                    // Đếm số lần thức giấc (chuyển từ ngủ sang thức)
                    if (previousStage != null && previousStage != SleepStage.AWAKE) {
                        awakenings++;
                    }
                    break;
            }
            
            previousStage = stage;
        }
        
        // Giả sử mỗi điểm dữ liệu cách nhau 5 giây
        int intervalSeconds = 5;
        
        this.deepSleepDuration = (deepCount * intervalSeconds) / 60;
        this.lightSleepDuration = (lightCount * intervalSeconds) / 60;
        this.remSleepDuration = (remCount * intervalSeconds) / 60;
        this.awakeDuration = (awakeCount * intervalSeconds) / 60;
        this.numberOfAwakenings = awakenings;
        
        // Tính chất lượng giấc ngủ
        calculateSleepQuality();
    }
    
    /**
     * Tính chất lượng giấc ngủ (0-100)
     * Công thức: Quality = (Deep% × 0.4) + (Light% × 0.3) + (REM% × 0.3) - (Awake% × 0.2)
     */
    private void calculateSleepQuality() {
        if (totalDuration <= 0) {
            this.sleepQuality = 0;
            return;
        }
        
        double deepPercent = (deepSleepDuration * 100.0) / totalDuration;
        double lightPercent = (lightSleepDuration * 100.0) / totalDuration;
        double remPercent = (remSleepDuration * 100.0) / totalDuration;
        double awakePercent = (awakeDuration * 100.0) / totalDuration;
        
        double quality = (deepPercent * 0.4) + 
                        (lightPercent * 0.3) + 
                        (remPercent * 0.3) - 
                        (awakePercent * 0.2);
        
        // Điều chỉnh dựa trên số lần thức giấc
        quality -= (numberOfAwakenings * 2); // Mỗi lần thức trừ 2 điểm
        
        // Giới hạn trong khoảng 0-100
        this.sleepQuality = Math.max(0, Math.min(100, quality));
    }
    
    /**
     * Thêm dữ liệu vào phiên ngủ
     */
    public void addSleepData(SleepData data) {
        if (sleepDataList == null) {
            sleepDataList = new ArrayList<>();
        }
        sleepDataList.add(data);
    }
    
    /**
     * Lấy đánh giá chất lượng giấc ngủ bằng chữ
     */
    public String getQualityRating() {
        if (sleepQuality >= 80) {
            return "Rất tốt";
        } else if (sleepQuality >= 60) {
            return "Tốt";
        } else if (sleepQuality >= 40) {
            return "Trung bình";
        } else {
            return "Kém";
        }
    }
    
    /**
     * Định dạng thời gian ngủ
     */
    public String getFormattedDuration() {
        int hours = totalDuration / 60;
        int minutes = totalDuration % 60;
        return String.format(Locale.getDefault(), "%d giờ %d phút", hours, minutes);
    }
    
    /**
     * Định dạng ngày tháng
     */
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(startTime));
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public int getTotalDuration() {
        return totalDuration;
    }
    
    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }
    
    public int getDeepSleepDuration() {
        return deepSleepDuration;
    }
    
    public void setDeepSleepDuration(int deepSleepDuration) {
        this.deepSleepDuration = deepSleepDuration;
    }
    
    public int getLightSleepDuration() {
        return lightSleepDuration;
    }
    
    public void setLightSleepDuration(int lightSleepDuration) {
        this.lightSleepDuration = lightSleepDuration;
    }
    
    public int getRemSleepDuration() {
        return remSleepDuration;
    }
    
    public void setRemSleepDuration(int remSleepDuration) {
        this.remSleepDuration = remSleepDuration;
    }
    
    public int getAwakeDuration() {
        return awakeDuration;
    }
    
    public void setAwakeDuration(int awakeDuration) {
        this.awakeDuration = awakeDuration;
    }
    
    public double getSleepQuality() {
        return sleepQuality;
    }
    
    public void setSleepQuality(double sleepQuality) {
        this.sleepQuality = sleepQuality;
    }
    
    public int getNumberOfAwakenings() {
        return numberOfAwakenings;
    }
    
    public void setNumberOfAwakenings(int numberOfAwakenings) {
        this.numberOfAwakenings = numberOfAwakenings;
    }
    
    public List<SleepData> getSleepDataList() {
        return sleepDataList;
    }
    
    public void setSleepDataList(List<SleepData> sleepDataList) {
        this.sleepDataList = sleepDataList;
    }
    
    /**
     * Kiểm tra phiên ngủ đang hoạt động
     */
    public boolean isActive() {
        return endTime == 0 || endTime < startTime;
    }
    
    @Override
    public String toString() {
        return "SleepSession{" +
                "id=" + id +
                ", date=" + getFormattedDate() +
                ", duration=" + getFormattedDuration() +
                ", quality=" + String.format("%.1f", sleepQuality) +
                '}';
    }
}
