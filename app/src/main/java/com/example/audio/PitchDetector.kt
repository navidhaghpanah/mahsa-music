package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class PitchDetector {

    private val sampleRate = 44100
    private val bufferSize = 4096
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _pitchState = MutableStateFlow<PitchState>(PitchState.Idle)
    val pitchState: StateFlow<PitchState> = _pitchState.asStateFlow()

    private val _onsetEvent = MutableStateFlow(0L)
    val onsetEvent: StateFlow<Long> = _onsetEvent.asStateFlow()

    // Energy tracking for onset/tempo detection
    private var previousRms = 0f
    private var lastOnsetTime = 0L

    sealed class PitchState {
        object Idle : PitchState()
        object ListeningSilence : PitchState()
        data class NoteDetected(
            val frequency: Float,
            val detectedNote: DetectedNote,
            val rms: Float
        ) : PitchState()
    }

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, activeTuning: DastgahTuning) {
        if (recordingJob != null) return

        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val readSize = bufferSize.coerceAtLeast(minBufSize)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                readSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            return
        }

        recordingJob = scope.launch(Dispatchers.Default) {
            val audioBuffer = ShortArray(bufferSize)
            val floatBuffer = FloatArray(bufferSize)
            val pitchHistory = FloatArray(3) { 0f }
            var historyIndex = 0

            while (isActive) {
                val read = audioRecord?.read(audioBuffer, 0, bufferSize) ?: -1
                if (read <= 0) continue

                // Convert to normalized float array & calculate RMS
                var sumSquares = 0.0
                for (i in 0 until read) {
                    val sample = audioBuffer[i] / 32768.0f
                    floatBuffer[i] = sample
                    sumSquares += sample * sample
                }
                val rms = sqrt(sumSquares / read).toFloat()

                // Check for mezrab strike / onset transient
                val now = System.currentTimeMillis()
                val deltaRms = rms - previousRms
                if (deltaRms > 0.035f && (now - lastOnsetTime) > 140) {
                    lastOnsetTime = now
                    _onsetEvent.value = now
                }
                previousRms = rms

                if (rms < 0.015f) {
                    _pitchState.value = PitchState.ListeningSilence
                    continue
                }

                // YIN pitch estimation algorithm
                val detectedFreq = estimatePitchYin(floatBuffer, read, sampleRate)
                if (detectedFreq > 50f && detectedFreq < 2200f) {
                    pitchHistory[historyIndex % 3] = detectedFreq
                    historyIndex++

                    // Median smoothing of 3 samples
                    val sorted = pitchHistory.sorted()
                    val smoothFreq = sorted[1]

                    val note = SantoorScale.frequencyToNote(smoothFreq)
                    val matchedString = SantoorScale.findMatchingString(smoothFreq, activeTuning)
                    val enrichedNote = note.copy(matchedSantoorString = matchedString)

                    _pitchState.value = PitchState.NoteDetected(
                        frequency = smoothFreq,
                        detectedNote = enrichedNote,
                        rms = rms
                    )
                } else {
                    _pitchState.value = PitchState.ListeningSilence
                }
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _pitchState.value = PitchState.Idle
    }

    // YIN Pitch Estimator implementation
    private fun estimatePitchYin(buffer: FloatArray, size: Int, sampleRate: Int): Float {
        val halfSize = size / 2
        val yinBuffer = FloatArray(halfSize)

        // Step 1 & 2: Squared difference function
        for (tau in 0 until halfSize) {
            var sum = 0.0f
            for (i in 0 until halfSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }

        // Step 3: Cumulative mean normalized difference
        yinBuffer[0] = 1.0f
        var runningSum = 0.0f
        for (tau in 1 until halfSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum > 0f) {
                yinBuffer[tau] * tau / runningSum
            } else {
                1.0f
            }
        }

        // Step 4: Absolute threshold
        val threshold = 0.15f
        var tau = 2
        while (tau < halfSize) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfSize && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                break
            }
            tau++
        }

        if (tau >= halfSize || yinBuffer[tau] >= threshold) {
            // Find global minimum if no threshold dip
            var minTau = 2
            var minVal = yinBuffer[2]
            for (t in 3 until halfSize) {
                if (yinBuffer[t] < minVal) {
                    minVal = yinBuffer[t]
                    minTau = t
                }
            }
            if (minVal > 0.40f) return -1f
            tau = minTau
        }

        // Step 5: Parabolic peak interpolation
        val s0 = if (tau > 0) yinBuffer[tau - 1] else yinBuffer[tau]
        val s1 = yinBuffer[tau]
        val s2 = if (tau + 1 < halfSize) yinBuffer[tau + 1] else yinBuffer[tau]

        val delta = if (2 * s1 - s0 - s2 != 0f) {
            (s2 - s0) / (2 * (2 * s1 - s0 - s2))
        } else 0f

        val refinedTau = tau + delta
        if (refinedTau <= 0f) return -1f

        return sampleRate / refinedTau
    }
}
