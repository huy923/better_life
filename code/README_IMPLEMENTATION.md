# HƯỚNG DẪN TRIỂN KHAI TÍNH NĂNG THEO DÕI GIẤC NGỦ

## 📝 CHECKLIST TRIỂN KHAI

### Bước 1: Cấu trúc thư mục
Tạo các thư mục sau trong dự án của bạn:

```
app/src/main/java/com/yourapp/healthapp/
├── models/         ← Tạo thư mục này
├── database/       ← Tạo thư mục này
├── services/       ← Tạo thư mục này
├── activities/     ← Có thể đã có
└── utils/          ← Tạo thư mục này (tùy chọn)

app/src/main/res/
├── layout/         ← Đã có
├── drawable/       ← Đã có
└── values/         ← Đã có
```

### Bước 2: Copy files vào đúng thư mục

**Models:**
- `SleepStage.java` → `models/`
- `SleepData.java` → `models/`
- `SleepSession.java` → `models/`

**Database:**
- `SleepDatabaseHelper.java` → `database/`
- `SleepDataDAO.java` → `database/`

**Services:**
- `SleepTrackingService.java` → `services/`

**Activities:**
- `SleepTrackingActivity.java` → `activities/`
- `SleepHistoryActivity.java` → `activities/`
- `SleepHistoryAdapter.java` → `activities/`

**Layouts (res/layout/):**
- `activity_sleep_tracking.xml`
- `activity_sleep_history.xml` (cần tạo thêm)
- `item_sleep_session.xml`

**Values (res/values/):**
- `colors.xml` (merge vào file có sẵn)

**Drawables (res/drawable/):**
- `button_green.xml`
- `button_outline_blue.xml`

### Bước 3: Sửa package name
Đổi tất cả `com.yourapp.healthapp` thành package name của bạn trong tất cả các file Java.

Cách nhanh:
1. Mở Find & Replace (Ctrl+Shift+R trong Android Studio)
2. Tìm: `com.yourapp.healthapp`
3. Thay bằng: `com.tenban.tenapp` (package thật của bạn)
4. Replace All

### Bước 4: Thêm vào AndroidManifest.xml

Mở `app/src/main/AndroidManifest.xml` và thêm:

```xml
<!-- Trước tag <application> -->
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="true" />

<!-- Trong tag <application> -->
<service
    android:name=".services.SleepTrackingService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="health" />

<activity android:name=".activities.SleepTrackingActivity" />
<activity android:name=".activities.SleepHistoryActivity" />
```

### Bước 5: Thêm dependencies vào build.gradle

Mở `app/build.gradle` và thêm:

```gradle
dependencies {
    // ... các dependencies có sẵn ...
    
    // Để vẽ biểu đồ
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // CardView và RecyclerView (có thể đã có)
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
}
```

Thêm vào `settings.gradle` hoặc `build.gradle` (Project level):

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // Thêm dòng này
    }
}
```

Sau đó: **Sync Now**

### Bước 6: Tạo icons còn thiếu

Cần tạo các icon sau trong `res/drawable/`:

**Cách 1: Sử dụng Vector Asset (Khuyến nghị)**
1. Right-click vào `res/drawable` → New → Vector Asset
2. Chọn icon từ Material Icons:
   - `ic_sleep` → Icon: `bedtime`
   - `ic_sleep_large` → Icon: `bedtime` (kích thước lớn hơn)
   - `ic_clock` → Icon: `schedule`
   - `ic_timer` → Icon: `timer`
   - `ic_stage` → Icon: `show_chart`
   - `ic_stop` → Icon: `stop`

**Cách 2: Sử dụng icon đơn giản**
Tạo file XML đơn giản cho từng icon:

```xml
<!-- ic_sleep.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36c-0.98,1.37 -2.58,2.26 -4.4,2.26c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4C12.92,3.04 12.46,3 12,3L12,3z"/>
</vector>
```

### Bước 7: Tạo layout còn thiếu

Cần tạo `activity_sleep_history.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#F5F5F5">

    <!-- Statistics Summary -->
    <androidx.cardview.widget.CardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="16dp"
        app:cardCornerRadius="12dp"
        app:cardElevation="4dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="Thống Kê Tổng Quan"
                android:textSize="18sp"
                android:textStyle="bold"
                android:textColor="#333333"
                android:layout_marginBottom="12dp"/>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Trung bình"
                        android:textSize="12sp"
                        android:textColor="#666666"/>

                    <TextView
                        android:id="@+id/tvAverageDuration"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="7h 30m"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#1976D2"/>

                </LinearLayout>

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical"
                    android:gravity="center">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Chất lượng TB"
                        android:textSize="12sp"
                        android:textColor="#666666"/>

                    <TextView
                        android:id="@+id/tvAverageQuality"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="75.0/100"
                        android:textSize="16sp"
                        android:textStyle="bold"
                        android:textColor="#4CAF50"/>

                </LinearLayout>

            </LinearLayout>

        </LinearLayout>

    </androidx.cardview.widget.CardView>

    <!-- Charts -->
    <androidx.cardview.widget.CardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:layout_marginEnd="16dp"
        android:layout_marginBottom="16dp"
        app:cardCornerRadius="12dp"
        app:cardElevation="4dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <com.github.mikephil.charting.charts.PieChart
                android:id="@+id/pieChart"
                android:layout_width="match_parent"
                android:layout_height="200dp"/>

            <com.github.mikephil.charting.charts.BarChart
                android:id="@+id/barChart"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:layout_marginTop="16dp"/>

        </LinearLayout>

    </androidx.cardview.widget.CardView>

    <!-- Sessions List -->
    <TextView
        android:id="@+id/tvNoData"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Chưa có dữ liệu giấc ngủ"
        android:textSize="16sp"
        android:textColor="#999999"
        android:gravity="center"
        android:padding="32dp"
        android:visibility="gone"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerViewHistory"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:paddingBottom="8dp"/>

</LinearLayout>
```

### Bước 8: Test ứng dụng

1. **Build project**: Build → Make Project
2. **Chạy trên thiết bị thật** (khuyến nghị) hoặc emulator
3. Kiểm tra:
   - ✅ App mở được không lỗi
   - ✅ Nhấn "Bắt đầu theo dõi" → Notification hiện ra
   - ✅ Để chạy 1-2 phút rồi "Dừng theo dõi"
   - ✅ Xem lịch sử → Có session vừa tạo

---

## ⚠️ TROUBLESHOOTING - XỬ LÝ LỖI THƯỜNG GẶP

### Lỗi 1: Cannot resolve symbol 'R'
**Nguyên nhân:** Resource chưa được sinh ra
**Giải pháp:**
1. Build → Clean Project
2. Build → Rebuild Project
3. Kiểm tra lỗi trong file XML

### Lỗi 2: Service bị kill khi khóa màn hình
**Nguyên nhân:** Hệ điều hành kill service để tiết kiệm pin
**Giải pháp:**
1. Tắt Battery Optimization cho app
2. Settings → Apps → Your App → Battery → Unrestricted

### Lỗi 3: MPAndroidChart không tìm thấy
**Nguyên nhân:** Chưa thêm repository JitPack
**Giải pháp:**
1. Mở `settings.gradle` hoặc `build.gradle` (Project)
2. Thêm `maven { url 'https://jitpack.io' }` vào repositories
3. Sync lại

### Lỗi 4: Sensor không hoạt động
**Nguyên nhân:** Thiết bị không có accelerometer hoặc chưa cấp quyền
**Giải pháp:**
1. Test trên thiết bị thật (emulator có thể không có sensor)
2. Kiểm tra quyền trong AndroidManifest.xml

### Lỗi 5: Notification không hiện
**Nguyên nhân:** Android 13+ cần quyền POST_NOTIFICATIONS
**Giải pháp:**
1. Thêm quyền vào AndroidManifest.xml
2. Request runtime permission trong code

---

## 🎯 TỐI ƯU HÓA

### 1. Tiết kiệm pin
```java
// Trong SleepTrackingService.java
private static final int SAMPLING_INTERVAL_MS = 10000; // Tăng lên 10 giây thay vì 5
```

### 2. Xóa dữ liệu cũ tự động
```java
// Gọi hàm này định kỳ
dataDAO.deleteOldData(30); // Xóa dữ liệu cũ hơn 30 ngày
```

### 3. Nâng cao độ chính xác
- Điều chỉnh ngưỡng phân loại trong `SleepStage.java`
- Thử nghiệm với nhiều người để tìm ngưỡng tối ưu

---

## 📱 HƯỚNG DẪN SỬ DỤNG CHO NGƯỜI DÙNG

1. **Chuẩn bị:**
   - Sạc điện thoại đầy hoặc gắn sạc
   - Đặt điện thoại gần giường (trong bán kính 50cm)
   - Tắt chế độ tiết kiệm pin

2. **Bắt đầu tracking:**
   - Mở app → Nhấn "Bắt đầu theo dõi"
   - Khóa màn hình
   - Đi ngủ bình thường

3. **Kết thúc:**
   - Khi thức dậy, mở app
   - Nhấn "Dừng theo dõi"
   - Xem kết quả trong lịch sử

---

## 🚀 CẢI TIẾN TRONG TƯƠNG LAI

### Tính năng có thể thêm:
1. **Smart Alarm** - Báo thức thông minh
2. **Sound Detection** - Phát hiện tiếng ngáy
3. **Integration với Google Fit**
4. **Machine Learning** - Dự đoán chính xác hơn
5. **Export PDF Report** - Xuất báo cáo
6. **Cloud Sync** - Đồng bộ dữ liệu

---

## 📚 TÀI LIỆU THAM KHẢO

- Android Sensors: https://developer.android.com/guide/topics/sensors
- Foreground Services: https://developer.android.com/guide/components/foreground-services
- MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
- Sleep Science: https://sleepfoundation.org

---

**Chúc bạn thành công! 🎉**

Nếu có lỗi, hãy kiểm tra lại từng bước theo checklist. 
Đa số lỗi là do sai package name hoặc thiếu file.
