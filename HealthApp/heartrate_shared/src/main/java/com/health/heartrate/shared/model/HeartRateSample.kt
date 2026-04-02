package com.health.heartrate.shared.model

data class HeartRateSample(
    val bpm: Int,
    val timestamp: Long,
    val accuracy: Int,
    val source: String,
    val sessionId: String
) {
    fun toByteArray(): ByteArray =
        "$bpm,$timestamp,$accuracy,$source,$sessionId".toByteArray(Charsets.UTF_8)

    companion object {
        fun fromByteArray(bytes: ByteArray): HeartRateSample {
            val p = bytes.toString(Charsets.UTF_8).split(',')
            return HeartRateSample(p[0].toInt(), p[1].toLong(), p[2].toInt(), p[3], p[4])
        }
    }
}
