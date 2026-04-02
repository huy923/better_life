package com.health.heartrate.shared.constants

object DataLayerConstants {
    const val PATH_HEART_RATE_LIVE    = "/heart_rate/live"
    const val PATH_HEART_RATE_SESSION = "/heart_rate/session"
    const val PATH_COMMAND            = "/command"
    const val PATH_STATUS             = "/status"

    const val KEY_BPM           = "bpm"
    const val KEY_TIMESTAMP     = "timestamp"
    const val KEY_ACCURACY      = "accuracy"
    const val KEY_SESSION_ID    = "session_id"
    const val KEY_SOURCE        = "source"
    const val KEY_COMMAND       = "command"
    const val KEY_AVG_BPM       = "avg_bpm"
    const val KEY_MAX_BPM       = "max_bpm"
    const val KEY_MIN_BPM       = "min_bpm"
    const val KEY_DURATION_SEC  = "duration_sec"
    const val KEY_SAMPLE_COUNT  = "sample_count"

    const val CMD_START_MONITORING = "start_monitoring"
    const val CMD_STOP_MONITORING  = "stop_monitoring"
    const val CMD_REQUEST_SYNC     = "request_sync"

    const val SOURCE_WATCH  = "wear_os"
    const val SOURCE_PHONE  = "phone_camera"

    const val ALERT_HIGH_BPM      = 120
    const val ALERT_LOW_BPM       = 45
    const val ALERT_RESTING_HIGH  = 100
    const val RETENTION_DAYS      = 30L
}
