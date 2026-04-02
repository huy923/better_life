package com.health.heartrate.phone.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.health.heartrate.shared.constants.DataLayerConstants
import kotlinx.coroutines.tasks.await

/**
 * Sends control commands from phone to watch.
 * The phone is the controller; the watch is the sensor worker.
 */
class PhoneCommandSender(context: Context) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient    = Wearable.getNodeClient(context)

    suspend fun startWatchMonitoring() =
        sendCommand(DataLayerConstants.CMD_START_MONITORING)

    suspend fun stopWatchMonitoring() =
        sendCommand(DataLayerConstants.CMD_STOP_MONITORING)

    private suspend fun sendCommand(command: String) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) { Log.w(TAG, "No connected watch nodes"); return }
            nodes.forEach { node ->
                messageClient.sendMessage(
                    node.id,
                    DataLayerConstants.PATH_COMMAND,
                    command.toByteArray(Charsets.UTF_8)
                ).await()
                Log.d(TAG, "Sent command '$command' to ${node.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: $command", e)
        }
    }

    companion object { private const val TAG = "PhoneCmdSender" }
}
