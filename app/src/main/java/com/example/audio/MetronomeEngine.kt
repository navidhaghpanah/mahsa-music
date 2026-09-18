package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class MetronomeEngine {

    private val sampleRate = 44100
    private var isRunning = false
    private var metronomeJob: Job? = null

    private val _currentBeat = MutableStateFlow(1)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var bpm = 100
    private var beatsPerBar = 4
    private var isMuted = false

    // Pre-synthesized click waveforms for low latency
    private val highClick: ShortArray = generateTone(1600.0, 35) // Accent beat (Beat 1)
    private val lowClick: ShortArray = generateTone(900.0, 25)   // Sub-beats

    private var audioTrack: AudioTrack? = null

    init {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (_: Exception) {}
    }

    fun start(scope: CoroutineScope, targetBpm: Int, meter: Int = 4, muted: Boolean = false) {
        if (isRunning) stop()

        bpm = targetBpm.coerceIn(30, 260)
        beatsPerBar = meter
        isMuted = muted
        isRunning = true
        _isPlaying.value = true
        _currentBeat.value = 1

        metronomeJob = scope.launch(Dispatchers.Default) {
            var beat = 1
            while (isActive && isRunning) {
                _currentBeat.value = beat

                if (!isMuted) {
                    val tone = if (beat == 1) highClick else lowClick
                    try {
                        audioTrack?.write(tone, 0, tone.size, AudioTrack.WRITE_NON_BLOCKING)
                    } catch (_: Exception) {}
                }

                val intervalMs = (60000L / bpm)
                delay(intervalMs)

                beat = (beat % beatsPerBar) + 1
            }
        }
    }

    fun updateBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(30, 260)
    }

    fun updateMeter(newMeter: Int) {
        beatsPerBar = newMeter
        _currentBeat.value = 1
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun stop() {
        isRunning = false
        _isPlaying.value = false
        metronomeJob?.cancel()
        metronomeJob = null
        _currentBeat.value = 1
    }

    fun release() {
        stop()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun generateTone(frequency: Double, durationMs: Int): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Percussive woodblock decay envelope
            val decay = (1.0 - (i.toDouble() / numSamples))
            val sample = (sin(2.0 * Math.PI * frequency * t) * decay * 32000.0).toInt()
            buffer[i] = sample.coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }
}
