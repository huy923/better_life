package com.example.better_life.sensor

import android.content.Context
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

/**
 * PPG (Photoplethysmography) heart rate estimation using rear camera flash + sensor.
 */
class CameraHeartRateSensor(private val context: Context){

    private val cameraManager = context.getSystemService(CameraManager::class.java)
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private val handlerThread = HandlerThread("CameraHR").also { it.start() }
    private val handler = Handler(handlerThread.looper)

    private val intensityBuffer = ArrayDeque<Double>(BUFFER_SIZE)
    private val timestampBuffer = ArrayDeque<Long>(BUFFER_SIZE)
    private var peakTimestamps  = mutableListOf<Long>()
    private var lastPeakTime    = 0L

    private val _bpmFlow = MutableSharedFlow<Int>(replay = 1)
    val bpmFlow: SharedFlow<Int> = _bpmFlow.asSharedFlow()

    private val _statusFlow = MutableSharedFlow<SensorStatus>(replay = 1)
    val statusFlow: SharedFlow<SensorStatus> = _statusFlow.asSharedFlow()

    private var isRunning = false

    enum class SensorStatus { IDLE, STARTING, MEASURING, FINGER_NOT_DETECTED, ERROR }

    suspend fun start() {
        if (isRunning) return
        isRunning = true
        _statusFlow.emit(SensorStatus.STARTING)

        val cameraId = findRearCameraWithFlash() ?: run {
            _statusFlow.emit(SensorStatus.ERROR)
            return
        }

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCapture(camera)
                }
                override fun onDisconnected(camera: CameraDevice) { cleanup() }
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    handler.post { kotlinx.coroutines.runBlocking { _statusFlow.emit(SensorStatus.ERROR) } }
                }
            }, handler)
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            _statusFlow.emit(SensorStatus.ERROR)
        }
    }

    private fun startCapture(camera: CameraDevice) {
        val imageReader = android.media.ImageReader.newInstance(
            FRAME_WIDTH, FRAME_HEIGHT,
            android.graphics.ImageFormat.YUV_420_888, 3
        )
        imageReader.setOnImageAvailableListener({ reader ->
            reader.acquireLatestImage()?.use { image ->
                val intensity = extractRedChannelIntensity(image)
                processIntensity(intensity, System.currentTimeMillis())
            }
        }, handler)

        val surface = imageReader.surface
        camera.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(surface)
                        set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                        set(CaptureRequest.SENSOR_EXPOSURE_TIME, EXPOSURE_NS)
                    }
                    session.setRepeatingRequest(request.build(), null, handler)
                    handler.post { kotlinx.coroutines.runBlocking { _statusFlow.emit(SensorStatus.MEASURING) } }
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    handler.post { kotlinx.coroutines.runBlocking { _statusFlow.emit(SensorStatus.ERROR) } }
                }
            }, handler
        )
    }

    private fun extractRedChannelIntensity(image: android.media.Image): Double {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var sum = 0L
        for (b in data) sum += (b.toInt() and 0xFF)
        return sum.toDouble() / data.size
    }

    private fun processIntensity(value: Double, timestamp: Long) {
        intensityBuffer.addLast(value)
        timestampBuffer.addLast(timestamp)
        if (intensityBuffer.size > BUFFER_SIZE) { intensityBuffer.removeFirst(); timestampBuffer.removeFirst() }
        if (intensityBuffer.size < MIN_SAMPLES_FOR_DETECTION) return

        val mean = intensityBuffer.average()
        val std  = stdDev(intensityBuffer, mean)

        if (std < FINGER_DETECTION_THRESHOLD) {
            kotlinx.coroutines.runBlocking { _statusFlow.emit(SensorStatus.FINGER_NOT_DETECTED) }
            return
        }

        val peakThreshold = mean + 0.3 * std
        val midIdx = intensityBuffer.size / 2
        val window = intensityBuffer.toList()
        if (window[midIdx] > peakThreshold &&
            window[midIdx] > window[midIdx - 1] &&
            window[midIdx] > window[midIdx + 1]) {

            val peakTime = timestampBuffer.toList()[midIdx]
            if (peakTime - lastPeakTime > MIN_PEAK_GAP_MS) {
                lastPeakTime = peakTime
                peakTimestamps.add(peakTime)
                if (peakTimestamps.size > MAX_PEAKS) peakTimestamps.removeAt(0)
                if (peakTimestamps.size >= 4) {
                    val bpm = calculateBpm()
                    if (bpm in 30..220) {
                        kotlinx.coroutines.runBlocking {
                            _statusFlow.emit(SensorStatus.MEASURING)
                            _bpmFlow.emit(bpm)
                        }
                    }
                }
            }
        }
    }

    private fun calculateBpm(): Int {
        val intervals = (1 until peakTimestamps.size).map {
            peakTimestamps[it] - peakTimestamps[it - 1]
        }
        val avgInterval = intervals.average()
        return (60_000.0 / avgInterval).toInt()
    }

    fun stop() {
        isRunning = false
        cleanup()
    }

    private fun cleanup() {
        captureSession?.close()
        cameraDevice?.close()
        captureSession = null
        cameraDevice   = null
        intensityBuffer.clear()
        timestampBuffer.clear()
        peakTimestamps.clear()
        lastPeakTime = 0L
    }

    private fun findRearCameraWithFlash(): String? =
        cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

    private fun stdDev(data: ArrayDeque<Double>, mean: Double): Double {
        val variance = data.sumOf { (it - mean) * (it - mean) } / data.size
        return sqrt(variance)
    }

    companion object {
        private const val TAG                      = "CameraHR"
        private const val BUFFER_SIZE              = 128
        private const val MIN_SAMPLES_FOR_DETECTION = 32
        private const val FINGER_DETECTION_THRESHOLD = 2.0
        private const val MIN_PEAK_GAP_MS          = 300L
        private const val MAX_PEAKS                = 10
        private const val FRAME_WIDTH              = 320
        private const val FRAME_HEIGHT             = 240
        private const val EXPOSURE_NS              = 8_000_000L
    }
}
