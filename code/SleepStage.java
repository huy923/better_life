package com.yourapp.healthapp.models;

/**
 * Enum định nghĩa các giai đoạn giấc ngủ
 * Dựa trên nghiên cứu khoa học về chu kỳ ngủ
 */
public enum SleepStage {
    
    /**
     * DEEP SLEEP (Ngủ sâu) - Giai đoạn 3-4 của NREM
     * - Rất ít chuyển động
     * - Cơ thể nghỉ ngơi và phục hồi
     * - Chiếm khoảng 15-25% tổng thời gian ngủ
     */
    DEEP(0, "Ngủ Sâu", "#1976D2"),
    
    /**
     * LIGHT SLEEP (Ngủ nhẹ) - Giai đoạn 1-2 của NREM
     * - Chuyển động vừa phải
     * - Dễ thức giấc
     * - Chiếm khoảng 50-60% tổng thời gian ngủ
     */
    LIGHT(1, "Ngủ Nhẹ", "#64B5F6"),
    
    /**
     * REM SLEEP (Ngủ mơ)
     * - Chuyển động mắt nhanh
     * - Mơ và xử lý ký ức
     * - Chiếm khoảng 20-25% tổng thời gian ngủ
     */
    REM(2, "Ngủ Mơ", "#FFB74D"),
    
    /**
     * AWAKE (Thức)
     * - Nhiều chuyển động
     * - Tỉnh táo hoặc sắp thức giấc
     */
    AWAKE(3, "Thức", "#EF5350");

    private final int value;
    private final String displayName;
    private final String color; // Màu sắc cho biểu đồ (hex color)

    /**
     * Constructor
     */
    SleepStage(int value, String displayName, String color) {
        this.value = value;
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Lấy giá trị số của giai đoạn (để lưu vào database)
     */
    public int getValue() {
        return value;
    }

    /**
     * Lấy tên hiển thị tiếng Việt
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Lấy mã màu hex
     */
    public String getColor() {
        return color;
    }

    /**
     * Chuyển đổi từ giá trị số sang SleepStage
     */
    public static SleepStage fromValue(int value) {
        for (SleepStage stage : SleepStage.values()) {
            if (stage.value == value) {
                return stage;
            }
        }
        return AWAKE; // Mặc định
    }

    /**
     * Phân loại giai đoạn ngủ dựa trên độ lớn chuyển động
     * @param movementMagnitude Độ lớn vector chuyển động (m/s²)
     * @return Giai đoạn ngủ tương ứng
     */
    public static SleepStage classifyFromMovement(double movementMagnitude) {
        // Ngưỡng phân loại dựa trên nghiên cứu thực nghiệm
        if (movementMagnitude < 0.5) {
            return DEEP;        // Rất ít chuyển động
        } else if (movementMagnitude < 1.5) {
            return LIGHT;       // Chuyển động nhẹ
        } else if (movementMagnitude < 3.0) {
            return REM;         // Chuyển động vừa phải
        } else {
            return AWAKE;       // Nhiều chuyển động
        }
    }

    /**
     * Lấy mô tả chi tiết về giai đoạn ngủ
     */
    public String getDescription() {
        switch (this) {
            case DEEP:
                return "Giai đoạn ngủ sâu, cơ thể phục hồi và tái tạo năng lượng. " +
                       "Rất khó đánh thức và rất quan trọng cho sức khỏe.";
            case LIGHT:
                return "Giai đoạn ngủ nhẹ, chuyển tiếp giữa tỉnh và ngủ. " +
                       "Dễ bị đánh thức bởi tiếng động hoặc ánh sáng.";
            case REM:
                return "Giai đoạn ngủ mơ, mắt chuyển động nhanh. " +
                       "Não bộ xử lý thông tin và củng cố ký ức.";
            case AWAKE:
                return "Đang thức hoặc sắp thức giấc. " +
                       "Có thể do tiếng động, ánh sáng hoặc chu kỳ ngủ tự nhiên.";
            default:
                return "";
        }
    }

    /**
     * Kiểm tra xem có phải là giai đoạn ngủ tốt không
     * @return true nếu là DEEP hoặc REM
     */
    public boolean isQualitySleep() {
        return this == DEEP || this == REM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
