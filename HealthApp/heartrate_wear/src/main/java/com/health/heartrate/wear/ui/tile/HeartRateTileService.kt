package com.health.heartrate.wear.ui.tile

import androidx.wear.protolayout.*
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.tiles.*
import androidx.wear.tiles.tooling.preview.TilePreviewData
import com.google.common.util.concurrent.ListenableFuture
import com.health.heartrate.wear.data.db.WearHeartRateDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.future

/**
 * Wear OS Tile showing current BPM.
 * Appears in the watch's tile carousel (swipe left from watch face).
 * Refreshes every 30 seconds when monitoring is active.
 */
class HeartRateTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest):
            ListenableFuture<TileBuilders.Tile> = scope.future {
        val db       = WearHeartRateDatabase.getInstance(applicationContext)
        val latest   = db.heartRateDao().getUnsynced().lastOrNull()
        val bpmText  = if (latest != null) "${latest.bpm}" else "--"
        val timeText = if (latest != null) "Vừa cập nhật" else "Chưa có dữ liệu"

        TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(30_000L)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(buildLayout(bpmText, timeText))
                            .build()
                    ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest):
            ListenableFuture<ResourceBuilders.Resources> = scope.future {
        ResourceBuilders.Resources.Builder().setVersion("1").build()
    }

    private fun buildLayout(bpm: String, sub: String): LayoutElementBuilders.Layout =
        LayoutElementBuilders.Layout.Builder()
            .setRoot(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(bpm)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.SpProp.Builder().setValue(52f).build())
                                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                                    .build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText("BPM")
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.SpProp.Builder().setValue(14f).build())
                                    .build()
                            ).build()
                    )
                    .addContent(
                        LayoutElementBuilders.Text.Builder()
                            .setText(sub)
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.SpProp.Builder().setValue(12f).build())
                                    .build()
                            ).build()
                    )
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .build()
            ).build()

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
