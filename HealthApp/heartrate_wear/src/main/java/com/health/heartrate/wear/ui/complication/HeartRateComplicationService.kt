package com.health.heartrate.wear.ui.complication

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.health.heartrate.wear.data.db.WearHeartRateDatabase
import kotlinx.coroutines.*

/**
 * Watch face complication: shows live BPM value.
 * Supported types: RANGED_VALUE (arc), SHORT_TEXT (small slot), LONG_TEXT (wide slot).
 * Register in AndroidManifest under <service> with complication intent-filter.
 */
class HeartRateComplicationService : ComplicationDataSourceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.RANGED_VALUE -> buildRanged(72, "72 BPM")
        ComplicationType.SHORT_TEXT   -> buildShortText("72", "BPM")
        ComplicationType.LONG_TEXT    -> buildLongText("Nhịp tim: 72 BPM", "Theo dõi")
        else                          -> null
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        scope.launch {
            val db     = WearHeartRateDatabase.getInstance(applicationContext)
            val latest = db.heartRateDao().getUnsynced().lastOrNull()
            val bpm    = latest?.bpm ?: 0
            val data   = when (request.complicationType) {
                ComplicationType.RANGED_VALUE -> buildRanged(bpm, "$bpm BPM")
                ComplicationType.SHORT_TEXT   -> buildShortText("$bpm", "BPM")
                ComplicationType.LONG_TEXT    -> buildLongText("Nhịp tim: $bpm BPM", "Vừa đo")
                else                          -> null
            }
            data?.let { listener.onComplicationData(it) }
        }
    }

    private fun buildRanged(bpm: Int, desc: String) = RangedValueComplicationData.Builder(
        value      = bpm.toFloat(),
        min        = 40f,
        max        = 200f,
        contentDescription = PlainComplicationText.Builder(desc).build()
    ).setText(PlainComplicationText.Builder("$bpm").build())
     .setTitle(PlainComplicationText.Builder("BPM").build())
     .build()

    private fun buildShortText(text: String, title: String) = ShortTextComplicationData.Builder(
        text  = PlainComplicationText.Builder(text).build(),
        contentDescription = PlainComplicationText.Builder("$text $title").build()
    ).setTitle(PlainComplicationText.Builder(title).build()).build()

    private fun buildLongText(text: String, title: String) = LongTextComplicationData.Builder(
        text  = PlainComplicationText.Builder(text).build(),
        contentDescription = PlainComplicationText.Builder(text).build()
    ).setTitle(PlainComplicationText.Builder(title).build()).build()

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
