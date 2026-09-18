package com.example

import com.example.audio.SantoorRegister
import com.example.audio.SantoorScale
import com.example.audio.TempoDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SantoorScaleTest {

    @Test
    fun testA440Frequency() {
        val note = SantoorScale.frequencyToNote(440.0f)
        assertEquals("A", note.noteNameEn.substring(0, 1))
        assertTrue("A440 should be close to 0 cents offset", Math.abs(note.cents) < 2f)
        assertTrue(note.isInTune)
    }

    @Test
    fun testDastgahTuningsAvailable() {
        val tunings = SantoorScale.getDastgahTunings()
        assertTrue("Must support Persian Dastgah tunings", tunings.size >= 4)
        val shur = tunings.find { it.id == "shur_sol" }
        assertNotNull(shur)
        assertEquals(27, shur!!.strings.size)
    }

    @Test
    fun testFindMatchingString() {
        val shur = SantoorScale.getDastgahTunings().first { it.id == "shur_sol" }
        // G3 (Sol Zard, kharak 3) is ~196 Hz
        val matched = SantoorScale.findMatchingString(196.0f, shur)
        assertNotNull(matched)
        assertEquals(SantoorRegister.ZARD, matched!!.register)
        assertEquals(3, matched.kharak)
    }

    @Test
    fun testTempoDetectorTap() {
        val detector = TempoDetector()
        val now = System.currentTimeMillis()
        // Simulate tapping at 120 BPM (every 500ms)
        detector.registerTap(now)
        val bpm = detector.registerTap(now + 500)
        assertEquals(120, bpm)
    }
}
