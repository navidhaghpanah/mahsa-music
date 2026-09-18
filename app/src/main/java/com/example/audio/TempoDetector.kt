package com.example.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

data class DetectedTempo(
    val bpm: Int,
    val confidence: Float,
    val categoryFa: String,
    val categoryEn: String,
    val isLiveDetected: Boolean
)

class TempoDetector {

    private val onsetTimestamps = mutableListOf<Long>()
    private val maxOnsets = 20

    private val _tempoState = MutableStateFlow(
        DetectedTempo(
            bpm = 0,
            confidence = 0f,
            categoryFa = "در انتظار مضراب یا ریتم...",
            categoryEn = "Waiting for rhythm...",
            isLiveDetected = false
        )
    )
    val tempoState: StateFlow<DetectedTempo> = _tempoState.asStateFlow()

    // Tap tempo history
    private val tapTimestamps = mutableListOf<Long>()

    fun registerOnset(timestamp: Long) {
        synchronized(onsetTimestamps) {
            // Drop stale onsets (> 6 seconds ago)
            onsetTimestamps.removeAll { timestamp - it > 6000 }
            onsetTimestamps.add(timestamp)
            if (onsetTimestamps.size > maxOnsets) {
                onsetTimestamps.removeAt(0)
            }

            if (onsetTimestamps.size >= 4) {
                analyzeTempo(onsetTimestamps, isLive = true)
            }
        }
    }

    fun registerTap(timestamp: Long = System.currentTimeMillis()): Int {
        synchronized(tapTimestamps) {
            tapTimestamps.removeAll { timestamp - it > 4000 }
            tapTimestamps.add(timestamp)
            if (tapTimestamps.size > 8) {
                tapTimestamps.removeAt(0)
            }

            if (tapTimestamps.size >= 2) {
                val intervals = mutableListOf<Long>()
                for (i in 1 until tapTimestamps.size) {
                    intervals.add(tapTimestamps[i] - tapTimestamps[i - 1])
                }
                val avgInterval = intervals.average()
                if (avgInterval > 0) {
                    val bpm = (60000.0 / avgInterval).roundToInt().coerceIn(30, 260)
                    updateTempo(bpm, 1.0f, isLive = false)
                    return bpm
                }
            }
        }
        return _tempoState.value.bpm
    }

    fun reset() {
        synchronized(onsetTimestamps) { onsetTimestamps.clear() }
        synchronized(tapTimestamps) { tapTimestamps.clear() }
        _tempoState.value = DetectedTempo(
            bpm = 0,
            confidence = 0f,
            categoryFa = "در انتظار مضراب یا ریتم...",
            categoryEn = "Waiting for rhythm...",
            isLiveDetected = false
        )
    }

    private fun analyzeTempo(timestamps: List<Long>, isLive: Boolean) {
        val intervals = mutableListOf<Long>()
        for (i in 1 until timestamps.size) {
            val diff = timestamps[i] - timestamps[i - 1]
            if (diff in 150..2000) { // between 30 and 400 BPM
                intervals.add(diff)
            }
        }

        if (intervals.size < 3) return

        // Cluster intervals to find dominant beat period
        val sortedIntervals = intervals.sorted()
        val medianInterval = sortedIntervals[sortedIntervals.size / 2]

        // Candidate intervals near median
        val closeIntervals = intervals.filter { abs(it - medianInterval) < medianInterval * 0.25 }
        if (closeIntervals.isEmpty()) return

        var chosenInterval = closeIntervals.average()

        // Normalize if playing rapid subdivisions (e.g. 16th notes / riz)
        // If interval is less than 270ms (> 220 BPM), it might be 8th notes or fast subdivision
        if (chosenInterval < 270) {
            chosenInterval *= 2.0
        }

        val calculatedBpm = (60000.0 / chosenInterval).roundToInt().coerceIn(40, 240)
        val confidence = (closeIntervals.size.toFloat() / intervals.size.toFloat()).coerceIn(0.2f, 0.95f)

        updateTempo(calculatedBpm, confidence, isLive)
    }

    private fun updateTempo(bpm: Int, confidence: Float, isLive: Boolean) {
        val (catFa, catEn) = getTempoCategory(bpm)
        _tempoState.value = DetectedTempo(
            bpm = bpm,
            confidence = confidence,
            categoryFa = catFa,
            categoryEn = catEn,
            isLiveDetected = isLive
        )
    }

    companion object {
        fun getTempoCategory(bpm: Int): Pair<String, String> {
            return when {
                bpm < 60 -> Pair("سنگین و کشیده (Lento)", "Slow / Lento")
                bpm in 60..76 -> Pair("آرام و ملایم (Adagio)", "Adagio")
                bpm in 77..108 -> Pair("ضرب پیوسته و روان (Andante)", "Walking Pace / Andante")
                bpm in 109..120 -> Pair("چهارمضراب معتدل (Moderato)", "Moderate / Moderato")
                bpm in 121..168 -> Pair("رنگ و چهارمضراب سریع (Allegro)", "Fast / Allegro (Reng)")
                else -> Pair("بسیار تند و شتابان (Presto)", "Very Fast / Presto")
            }
        }
    }
}
