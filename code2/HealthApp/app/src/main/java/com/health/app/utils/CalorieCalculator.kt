package com.health.app.utils

object CalorieCalculator {
    private const val MET_WALKING = 3.5
    private const val MET_RUNNING = 8.0

    fun fromGps(durationSeconds: Long, weightKg: Float, activityType: String): Float {
        val met = if (activityType == "running") MET_RUNNING else MET_WALKING
        return (met * weightKg * durationSeconds / 3600.0).toFloat()
    }

    fun fromSteps(steps: Int, heightCm: Float, weightKg: Float): Float {
        val distanceKm = steps * heightCm * 0.00414f / 1000f
        return distanceKm * weightKg * 0.57f
    }

    fun stepsToDistance(steps: Int, heightCm: Float): Float = steps * heightCm * 0.00414f

    fun formatDistance(meters: Float): String =
        if (meters >= 1000) "%.2f km".format(meters / 1000f) else "${meters.toInt()} m"

    fun formatCalories(kcal: Float): String = "%.1f kcal".format(kcal)
}
