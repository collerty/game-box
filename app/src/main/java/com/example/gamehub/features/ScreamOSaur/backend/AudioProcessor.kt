package com.example.gamehub.features.ScreamOSaur.backend

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class AudioProcessor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude

    companion object {
        const val MIN_AMPLITUDE_THRESHOLD = 8000
        private const val TAG = "ScreamOSaurDebug"
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun start() {
        if (!hasPermission() || recordJob?.isActive == true) {
            Log.d(TAG, "Audio processor not started: hasPermission=${hasPermission()}, recordJob?.isActive=${recordJob?.isActive}")
            return
        }

        Log.d(TAG, "Starting audio processor")
        val bufferSize = AudioRecord.getMinBufferSize(
            8000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to initialize AudioRecord due to SecurityException: ${e.message}")
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Failed to initialize AudioRecord")
            return
        }

        recordJob = scope.launch(Dispatchers.IO) {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)
                Log.d(TAG, "Audio recording started successfully")

                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val maxAmplitudeRaw = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0
                        if (maxAmplitudeRaw >= MIN_AMPLITUDE_THRESHOLD) {
                            Log.d(TAG, "High amplitude detected: $maxAmplitudeRaw >= $MIN_AMPLITUDE_THRESHOLD")
                        }
                        _amplitude.value = maxAmplitudeRaw
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio processing: ${e.message}")
                _amplitude.value = 0
            } finally {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord?.stop()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Error stopping audio recording: ${e.message}")
            }
        }
    }

    fun stop() {
        recordJob?.cancel()
        recordJob = null
        stopRecording()
        audioRecord?.release()
        audioRecord = null
        _amplitude.value = 0
        Log.d(TAG, "Audio processor stopped")
    }
}

