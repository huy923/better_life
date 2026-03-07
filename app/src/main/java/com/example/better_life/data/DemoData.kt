package com.example.better_life.data

import com.example.better_life.data.database.AppDatabase
import com.example.better_life.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DemoData {
    suspend fun insertSampleData(database: AppDatabase) {
        withContext(Dispatchers.IO) {
            // 1. Sample User
            database.userDao().insertOrUpdate(
                User(name = "Nguyễn Văn A", age = 25, height = 170, weight = 65.5)
            )

            // 2. Sample Heart Rate Records
            val hrDao = database.heartRateDao()
            val now = System.currentTimeMillis()
            hrDao.insert(HeartRateRecord(bpm = 110, timestamp = now - 3600000, status = "Tập luyện"))
            hrDao.insert(HeartRateRecord(bpm = 95, timestamp = now - 7200000, status = "Hoạt động"))
            hrDao.insert(HeartRateRecord(bpm = 88, timestamp = now - 10800000, status = "Bình thường"))
            hrDao.insert(HeartRateRecord(bpm = 92, timestamp = now - 14400000, status = "Hoạt động"))
            hrDao.insert(HeartRateRecord(bpm = 85, timestamp = now - 18000000, status = "Bình thường"))

            // 3. Sample Meal Records
            val mealDao = database.mealDao()
            mealDao.insert(MealRecord(name = "Phở bò tái", calories = 420, timestamp = now - 7200000, mealType = "Sáng"))
            mealDao.insert(MealRecord(name = "Cơm gà xối mỡ", calories = 650, timestamp = now - 14400000, mealType = "Trưa"))
            mealDao.insert(MealRecord(name = "Sinh tố bơ", calories = 180, timestamp = now - 86400000, mealType = "Chiều"))
            mealDao.insert(MealRecord(name = "Bún chả Hà Nội", calories = 520, timestamp = now - 90000000, mealType = "Trưa"))
            mealDao.insert(MealRecord(name = "Salad cá hồi", calories = 350, timestamp = now - 172800000, mealType = "Tối"))

            // 4. Sample Running Records
            val runDao = database.runningDao()
            runDao.insert(RunningRecord(date = "Hôm nay", activityType = "Chạy bộ", duration = "00:30:00", steps = 3200, calories = 280, distance = 3.2, timestamp = now))
            runDao.insert(RunningRecord(date = "Hôm qua", activityType = "Đi bộ", duration = "00:40:00", steps = 4100, calories = 180, distance = 2.8, timestamp = now - 86400000))
            runDao.insert(RunningRecord(date = "2 ngày trước", activityType = "Chạy bộ", duration = "00:20:00", steps = 2000, calories = 190, distance = 2.1, timestamp = now - 172800000))
            runDao.insert(RunningRecord(date = "3 ngày trước", activityType = "Chạy bộ", duration = "00:45:00", steps = 5000, calories = 400, distance = 5.0, timestamp = now - 259200000))
            runDao.insert(RunningRecord(date = "4 ngày trước", activityType = "Đi bộ", duration = "00:15:00", steps = 1500, calories = 70, distance = 1.2, timestamp = now - 345600000))

            // 5. Sample Goals
            val goalDao = database.goalDao()
            goalDao.insert(Goal(title = "Đi bộ mỗi ngày", targetValue = 10000.0, currentValue = 7200.0, unit = "bước", streak = 15, iconName = "ic_run"))
            goalDao.insert(Goal(title = "Uống đủ nước", targetValue = 2.5, currentValue = 1.8, unit = "lít", streak = 8, iconName = "ic_water_drop"))
            goalDao.insert(Goal(title = "Ngủ đủ giấc", targetValue = 8.0, currentValue = 7.5, unit = "giờ", streak = 5, iconName = "ic_moon", isCompleted = true))
            goalDao.insert(Goal(title = "Không ăn vặt sau 8h tối", targetValue = 1.0, currentValue = 1.0, unit = "ngày", streak = 22, iconName = "ic_no_entry", isCompleted = true))
            goalDao.insert(Goal(title = "Tập thể dục 30 phút", targetValue = 30.0, currentValue = 20.0, unit = "phút", streak = 3, iconName = "ic_muscle"))
        }
    }
}
