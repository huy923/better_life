package com.health.heartrate.wear.sync

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.health.heartrate.shared.constants.DataLayerConstants
import com.health.heartrate.wear.service.WearHeartRateService

/**
 * Listens for control commands from the phone app.
 * Phone sends CMD_START_MONITORING → starts WearHeartRateService.
 */
class WearCommandListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != DataLayerConstants.PATH_COMMAND) return
        val command = String(event.data, Charsets.UTF_8)
        Log.d(TAG, "Received command from phone: $command")

        val action = when (command) {
            DataLayerConstants.CMD_START_MONITORING -> WearHeartRateService.ACTION_START
            DataLayerConstants.CMD_STOP_MONITORING  -> WearHeartRateService.ACTION_STOP
            else -> return
        }
        startService(Intent(this, WearHeartRateService::class.java).apply {
            this.action = action
        })
    }

    companion object { private const val TAG = "WearCmdListener" }
}
