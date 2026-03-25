# HƯỚNG DẪN XÂY DỰNG TÍNH NĂNG THEO DÕI GIẤC NGỦ - ANDROID STUDIO (JAVA)

## 📋 MỤC LỤC
1. [Tổng quan hệ thống](#tổng-quan)
2. [Cấu trúc thư mục](#cấu-trúc-thư-mục)
3. [Cài đặt ban đầu](#cài-đặt-ban-đầu)
4. [Các thành phần chính](#các-thành-phần-chính)
5. [Hướng dẫn triển khai](#hướng-dẫn-triển-khai)
6. [Giải thích thuật toán](#giải-thích-thuật-toán)

---

## 🎯 TỔNG QUAN

### Tính năng chính:
- ✅ Theo dõi giấc ngủ bằng cảm biến gia tốc kế
- ✅ Phát hiện 4 giai đoạn: Deep Sleep, Light Sleep, REM, Awake
- ✅ Thống kê chất lượng giấc ngủ
- ✅ Lưu trữ lịch sử giấc ngủ
- ✅ Hiển thị biểu đồ trực quan

### Nguyên lý hoạt động:
1. **Thu thập dữ liệu**: Đọc giá trị từ accelerometer mỗi 5 giây
2. **Xử lý dữ liệu**: Tính toán độ lớn vector chuyển động
3. **Phân loại giai đoạn**: So sánh với ngưỡng để xác định giai đoạn ngủ
4. **Lưu trữ**: Ghi vào SQLite database
5. **Hiển thị**: Tạo biểu đồ và thống kê

---

## 📁 CẤU TRÚC THỬ MỤC

```
app/src/main/java/com/yourapp/healthapp/
├── models/
│   ├── SleepSession.java          # Model phiên ngủ
│   ├── SleepData.java              # Model dữ liệu từng giai đoạn
│   └── SleepStage.java             # Enum các giai đoạn ngủ
├── database/
│   ├── SleepDatabaseHelper.java    # SQLite helper
│   └── SleepDataDAO.java           # Data Access Object
├── services/
│   └── SleepTrackingService.java   # Background service
├── utils/
│   ├── SleepAnalyzer.java          # Thuật toán phân tích
│   └── SleepStatistics.java        # Tính toán thống kê
└── activities/
    ├── SleepTrackingActivity.java  # Màn hình theo dõi
    └── SleepHistoryActivity.java   # Màn hình lịch sử
```

---

## 🛠️ CÀI ĐẶT BAN ĐẦU

### 1. Thêm quyền vào AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.yourapp.healthapp">

    <!-- Quyền cần thiết -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Yêu cầu thiết bị phải có accelerometer -->
    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <!-- Đăng ký Service -->
        <service
            android:name=".services.SleepTrackingService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="health" />

        <!-- Activities -->
        <activity android:name=".activities.SleepTrackingActivity" />
        <activity android:name=".activities.SleepHistoryActivity" />
        
    </application>
</manifest>
```

### 2. Thêm dependencies vào build.gradle (Module: app)

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Để vẽ biểu đồ
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Room Database (tùy chọn, nếu muốn dùng thay SQLite)
    // implementation "androidx.room:room-runtime:2.5.2"
    // annotationProcessor "androidx.room:room-compiler:2.5.2"
}
```

### 3. Thêm repository vào build.gradle (Project)

```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

---

## 🔧 CÁC THÀNH PHẦN CHÍNH

### Danh sách file cần tạo:
1. **SleepStage.java** - Enum định nghĩa các giai đoạn ngủ
2. **SleepData.java** - Model lưu dữ liệu mỗi lần đo
3. **SleepSession.java** - Model lưu cả phiên ngủ
4. **SleepDatabaseHelper.java** - Quản lý SQLite database
5. **SleepDataDAO.java** - Truy vấn database
6. **SleepAnalyzer.java** - Thuật toán phân tích giai đoạn ngủ
7. **SleepStatistics.java** - Tính toán thống kê
8. **SleepTrackingService.java** - Service chạy nền
9. **SleepTrackingActivity.java** - Giao diện theo dõi
10. **SleepHistoryActivity.java** - Giao diện xem lịch sử

---

## 🚀 HƯỚNG DẪN TRIỂN KHAI

### Bước 1: Tạo các Model cơ bản
### Bước 2: Thiết lập Database
### Bước 3: Xây dựng thuật toán phân tích
### Bước 4: Tạo Background Service
### Bước 5: Xây dựng giao diện
### Bước 6: Kiểm thử và tối ưu

(Chi tiết từng bước xem trong các file code đi kèm)

---

## 📊 GIẢI THÍCH THUẬT TOÁN

### 1. Công thức tính độ lớn chuyển động:

```
magnitude = √(x² + y² + z²)
```

Trong đó:
- x, y, z là giá trị gia tốc theo 3 trục
- magnitude là độ lớn vector chuyển động

### 2. Ngưỡng phân loại giai đoạn ngủ:

| Giai đoạn | Mức chuyển động | Ngưỡng (m/s²) |
|-----------|-----------------|---------------|
| **Deep Sleep** | Rất thấp | < 0.5 |
| **Light Sleep** | Thấp - Trung bình | 0.5 - 1.5 |
| **REM Sleep** | Nhỏ nhưng thường xuyên | 1.5 - 3.0 |
| **Awake** | Cao | > 3.0 |

### 3. Tính chất lượng giấc ngủ:

```
Sleep Quality = (Deep% × 0.4) + (Light% × 0.3) + (REM% × 0.3) - (Awake% × 0.2)
```

Chất lượng từ 0-100:
- 80-100: Rất tốt
- 60-79: Tốt
- 40-59: Trung bình
- 0-39: Kém

---

## 💡 GHI CHÚ QUAN TRỌNG

### Lưu ý khi sử dụng:
1. **Đặt điện thoại trên giường**: Gần người ngủ để cảm biến hoạt động tốt
2. **Sạc pin đầy**: Tracking chạy suốt đêm tiêu tốn pin
3. **Tắt chế độ tiết kiệm pin**: Tránh service bị kill
4. **Khóa màn hình**: Giảm tiêu thụ pin

### Tối ưu hóa:
- Thu thập dữ liệu mỗi 5 giây (có thể điều chỉnh)
- Sử dụng WakeLock để service không bị ngủ
- Dọn dẹp dữ liệu cũ (> 30 ngày)
- Nén database định kỳ

---

## 🎓 HỌC THÊM

### Thuật toán nâng cao (có thể thêm sau):
1. **Machine Learning**: Sử dụng TensorFlow Lite để phân loại chính xác hơn
2. **Heart Rate**: Tích hợp theo dõi nhịp tim (nếu có sensor)
3. **Sound Detection**: Phát hiện tiếng ngáy, tiếng động
4. **Smart Alarm**: Báo thức vào thời điểm ngủ nhẹ

### Tài liệu tham khảo:
- Android Sensor Framework: https://developer.android.com/guide/topics/sensors
- Sleep Science: https://sleepfoundation.org/sleep-topics/stages-of-sleep
- Android Services: https://developer.android.com/guide/components/services

---

**Tác giả**: Claude AI Assistant  
**Ngày tạo**: 2026  
**Phiên bản**: 1.0  
