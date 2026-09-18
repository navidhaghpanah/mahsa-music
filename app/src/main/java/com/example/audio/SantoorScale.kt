package com.example.audio

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

enum class SantoorRegister(val labelFa: String, val labelEn: String, val colorHex: Long) {
    ZARD("سیم زرد (بم)", "Yellow Strings (Bass)", 0xFFF59E0B),
    SEFID("سیم سفید (میانی)", "White Strings (Mid)", 0xFF38BDF8),
    POSHT_E_KHARAK("پشت خرک (زیر)", "Behind Bridge (Treble)", 0xFFA855F7)
}

data class SantoorString(
    val kharak: Int, // 1 to 9
    val register: SantoorRegister,
    val noteNameFa: String,
    val noteNameEn: String,
    val frequency: Float
)

data class DastgahTuning(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val description: String,
    val strings: List<SantoorString>
)

data class DetectedNote(
    val noteNameFa: String,
    val noteNameEn: String,
    val frequency: Float,
    val targetFrequency: Float,
    val cents: Float, // -50.0 to +50.0
    val isInTune: Boolean,
    val octave: Int,
    val matchedSantoorString: SantoorString? = null
)

object SantoorScale {

    // Standard 24-EDO Persian & Western notes table
    // A4 = 440 Hz
    // In 24-EDO, there are 24 quarter-tones per octave: step = 2^(1/24)
    private val NOTE_NAMES = listOf(
        Pair("دو", "C"),
        Pair("دو سُری", "C𝄰"),
        Pair("دو دیز", "C#"),
        Pair("رِ کُرُن", "D𝄳"),
        Pair("رِ", "D"),
        Pair("رِ سُری", "D𝄰"),
        Pair("رِ دیز / می بمل", "D#/E♭"),
        Pair("می کُرُن", "E𝄳"),
        Pair("می", "E"),
        Pair("فا کُرُن", "F𝄳"),
        Pair("فا", "F"),
        Pair("فا سُری", "F𝄰"),
        Pair("فا دیز", "F#"),
        Pair("سل کُرُن", "G𝄳"),
        Pair("سل", "G"),
        Pair("سل سُری", "G𝄰"),
        Pair("سل دیز / لا بمل", "G#/A♭"),
        Pair("لا کُرُن", "A𝄳"),
        Pair("لا", "A"),
        Pair("لا سُری", "A𝄰"),
        Pair("لا دیز / سی بمل", "A#/B♭"),
        Pair("سی کُرُن", "B𝄳"),
        Pair("سی", "B"),
        Pair("دو کُرُن", "C𝄳")
    )

    fun frequencyToNote(freq: Float): DetectedNote {
        if (freq < 40f || freq > 2500f) {
            return DetectedNote(
                noteNameFa = "--",
                noteNameEn = "--",
                frequency = freq,
                targetFrequency = 0f,
                cents = 0f,
                isInTune = false,
                octave = 0
            )
        }

        // Formula for 24-EDO note number relative to A4 (440 Hz)
        // A4 is index 18 in octave 4.
        // n = 24 * log2(f / 440)
        val stepsFromA4 = 24.0 * (ln(freq / 440.0) / ln(2.0))
        val roundedStep = stepsFromA4.roundToInt()

        // Difference in quarter-tones, convert to cents (1 quarter-tone = 50 cents)
        val cents = ((stepsFromA4 - roundedStep) * 50.0).toFloat().coerceIn(-50f, 50f)
        val targetFreq = (440.0 * 2.0.pow(roundedStep / 24.0)).toFloat()

        // Base A4 is note index 18 in octave 4:
        // Total quarter-tone index from C0:
        // A4 = 4 * 24 + 18 = 114
        val absoluteQuarterIndex = 114 + roundedStep
        val octave = (absoluteQuarterIndex / 24).coerceIn(0, 8)
        val noteInOctave = ((absoluteQuarterIndex % 24) + 24) % 24

        val (nameFa, nameEn) = NOTE_NAMES[noteInOctave]
        val isInTune = kotlin.math.abs(cents) <= 7.0f

        return DetectedNote(
            noteNameFa = "$nameFa $octave",
            noteNameEn = "$nameEn$octave",
            frequency = freq,
            targetFrequency = targetFreq,
            cents = cents,
            isInTune = isInTune,
            octave = octave
        )
    }

    // Standard Dastgah tunings for Santoor Sol-Kook (سنتور سل‌کوک ۹ خرک)
    // Kharak positions 1 to 9:
    // Kharak 1: Mi
    // Kharak 2: Fa
    // Kharak 3: Sol
    // Kharak 4: La (or La Koron)
    // Kharak 5: Si (or Si Bemol / Si Koron)
    // Kharak 6: Do
    // Kharak 7: Re
    // Kharak 8: Mi (or Mi Koron)
    // Kharak 9: Fa
    fun getDastgahTunings(): List<DastgahTuning> {
        return listOf(
            buildShurSolTuning(),
            buildMahurDoTuning(),
            buildHomayounSolTuning(),
            buildEsfahanDoTuning(),
            buildSegahTuning(),
            buildChahargahTuning()
        )
    }

    private fun noteFrequency(name: String, octave: Int): Float {
        // Look up in 24-EDO
        val baseIndex = NOTE_NAMES.indexOfFirst { it.second == name }
        if (baseIndex == -1) return 440f
        val absoluteIndex = octave * 24 + baseIndex
        val roundedStep = absoluteIndex - 114
        return (440.0 * 2.0.pow(roundedStep / 24.0)).toFloat()
    }

    private fun buildShurSolTuning(): DastgahTuning {
        // Shur Sol (شور سل):
        // Kharak 1: Mi (E), 2: Fa (F), 3: Sol (G), 4: La Koron (A𝄳), 5: Si Bemol (B♭), 6: Do (C), 7: Re (D), 8: Mi Koron (E𝄳), 9: Fa (F)
        val zardNotes = listOf(
            Pair("E", 3), Pair("F", 3), Pair("G", 3), Pair("A𝄳", 3),
            Pair("A#/B♭", 3), Pair("C", 4), Pair("D", 4), Pair("E𝄳", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E", 4), Pair("F", 4), Pair("G", 4), Pair("A𝄳", 4),
            Pair("A#/B♭", 4), Pair("C", 5), Pair("D", 5), Pair("E𝄳", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E", 5), Pair("F", 5), Pair("G", 5), Pair("A𝄳", 5),
            Pair("A#/B♭", 5), Pair("C", 6), Pair("D", 6), Pair("E𝄳", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "shur_sol",
            nameFa = "دستگاه شور (سل)",
            nameEn = "Dastgah Shur (Sol)",
            desc = "مادر دستگاه‌های ایرانی - سیم ۴: لا کُرُن، سیم ۵: سی بمل، سیم ۸: می کُرُن",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun buildMahurDoTuning(): DastgahTuning {
        // Mahur Do (ماهور دو): Natural notes, major scale
        val zardNotes = listOf(
            Pair("E", 3), Pair("F", 3), Pair("G", 3), Pair("A", 3),
            Pair("B", 3), Pair("C", 4), Pair("D", 4), Pair("E", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E", 4), Pair("F", 4), Pair("G", 4), Pair("A", 4),
            Pair("B", 4), Pair("C", 5), Pair("D", 5), Pair("E", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E", 5), Pair("F", 5), Pair("G", 5), Pair("A", 5),
            Pair("B", 5), Pair("C", 6), Pair("D", 6), Pair("E", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "mahur_do",
            nameFa = "دستگاه ماهور (دو)",
            nameEn = "Dastgah Mahur (Do)",
            desc = "دستگاه شاد و باشکوه - تمام نت‌ها بکار (طبیعی)",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun buildHomayounSolTuning(): DastgahTuning {
        // Homayoun Sol (همایون سل):
        // Kharak 4: La Koron, Kharak 5: Si natural, Kharak 8: Mi Bemol
        val zardNotes = listOf(
            Pair("E", 3), Pair("F", 3), Pair("G", 3), Pair("A𝄳", 3),
            Pair("B", 3), Pair("C", 4), Pair("D", 4), Pair("D#/E♭", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E", 4), Pair("F", 4), Pair("G", 4), Pair("A𝄳", 4),
            Pair("B", 4), Pair("C", 5), Pair("D", 5), Pair("D#/E♭", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E", 5), Pair("F", 5), Pair("G", 5), Pair("A𝄳", 5),
            Pair("B", 5), Pair("C", 6), Pair("D", 6), Pair("D#/E♭", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "homayoun_sol",
            nameFa = "دستگاه همایون (سل)",
            nameEn = "Dastgah Homayoun (Sol)",
            desc = "نوستالژیک و مجلل - سیم ۴: لا کُرُن، سیم ۵: سی بکار، سیم ۸: می بمل",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun buildEsfahanDoTuning(): DastgahTuning {
        // Esfahan Do (آواز بیات اصفهان دو):
        // Kharak 4: La natural, Kharak 5: Si Bemol, Kharak 8: Mi Koron
        val zardNotes = listOf(
            Pair("E", 3), Pair("F", 3), Pair("G", 3), Pair("A", 3),
            Pair("A#/B♭", 3), Pair("C", 4), Pair("D", 4), Pair("E𝄳", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E", 4), Pair("F", 4), Pair("G", 4), Pair("A", 4),
            Pair("A#/B♭", 4), Pair("C", 5), Pair("D", 5), Pair("E𝄳", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E", 5), Pair("F", 5), Pair("G", 5), Pair("A", 5),
            Pair("A#/B♭", 5), Pair("C", 6), Pair("D", 6), Pair("E𝄳", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "esfahan_do",
            nameFa = "آواز بیات اصفهان (دو)",
            nameEn = "Bayat-e Esfahan (Do)",
            desc = "آهنگین و عاطفی - سیم ۴: لا بکار، سیم ۵: سی بمل، سیم ۸: می کُرُن",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun buildSegahTuning(): DastgahTuning {
        // Segah (سه‌گاه):
        // Kharak 1: Mi Koron, Kharak 4: La Koron, Kharak 5: Si Koron, Kharak 8: Mi Koron
        val zardNotes = listOf(
            Pair("E𝄳", 3), Pair("F", 3), Pair("G", 3), Pair("A𝄳", 3),
            Pair("B𝄳", 3), Pair("C", 4), Pair("D", 4), Pair("E𝄳", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E𝄳", 4), Pair("F", 4), Pair("G", 4), Pair("A𝄳", 4),
            Pair("B𝄳", 4), Pair("C", 5), Pair("D", 5), Pair("E𝄳", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E𝄳", 5), Pair("F", 5), Pair("G", 5), Pair("A𝄳", 5),
            Pair("B𝄳", 5), Pair("C", 6), Pair("D", 6), Pair("E𝄳", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "segah",
            nameFa = "دستگاه سه‌گاه",
            nameEn = "Dastgah Segah",
            desc = "شورآفرین و عمیق - سیم ۱ و ۸: می کُرُن، سیم ۴: لا کُرُن، سیم ۵: سی کُرُن",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun buildChahargahTuning(): DastgahTuning {
        // Chahargah (چهارگاه):
        // Kharak 4: La Koron, Kharak 7: Re Koron
        val zardNotes = listOf(
            Pair("E", 3), Pair("F", 3), Pair("G", 3), Pair("A𝄳", 3),
            Pair("B", 3), Pair("C", 4), Pair("D𝄳", 4), Pair("E", 4), Pair("F", 4)
        )
        val sefidNotes = listOf(
            Pair("E", 4), Pair("F", 4), Pair("G", 4), Pair("A𝄳", 4),
            Pair("B", 4), Pair("C", 5), Pair("D𝄳", 5), Pair("E", 5), Pair("F", 5)
        )
        val poshtNotes = listOf(
            Pair("E", 5), Pair("F", 5), Pair("G", 5), Pair("A𝄳", 5),
            Pair("B", 5), Pair("C", 6), Pair("D𝄳", 6), Pair("E", 6), Pair("F", 6)
        )

        return createDastgah(
            id = "chahargah",
            nameFa = "دستگاه چهارگاه",
            nameEn = "Dastgah Chahargah",
            desc = "حماسی و سرزنده - سیم ۴: لا کُرُن، سیم ۵: سی بکار، سیم ۷: رِ کُرُن",
            zard = zardNotes,
            sefid = sefidNotes,
            posht = posNotesList(poshtNotes)
        )
    }

    private fun posNotesList(list: List<Pair<String, Int>>) = list

    private fun createDastgah(
        id: String,
        nameFa: String,
        nameEn: String,
        desc: String,
        zard: List<Pair<String, Int>>,
        sefid: List<Pair<String, Int>>,
        posht: List<Pair<String, Int>>
    ): DastgahTuning {
        val strings = mutableListOf<SantoorString>()

        zard.forEachIndexed { index, (note, oct) ->
            val freq = noteFrequency(note, oct)
            val noteFa = persianNoteName(note)
            strings.add(
                SantoorString(
                    kharak = index + 1,
                    register = SantoorRegister.ZARD,
                    noteNameFa = "$noteFa $oct",
                    noteNameEn = "$note$oct",
                    frequency = freq
                )
            )
        }

        sefid.forEachIndexed { index, (note, oct) ->
            val freq = noteFrequency(note, oct)
            val noteFa = persianNoteName(note)
            strings.add(
                SantoorString(
                    kharak = index + 1,
                    register = SantoorRegister.SEFID,
                    noteNameFa = "$noteFa $oct",
                    noteNameEn = "$note$oct",
                    frequency = freq
                )
            )
        }

        posht.forEachIndexed { index, (note, oct) ->
            val freq = noteFrequency(note, oct)
            val noteFa = persianNoteName(note)
            strings.add(
                SantoorString(
                    kharak = index + 1,
                    register = SantoorRegister.POSHT_E_KHARAK,
                    noteNameFa = "$noteFa $oct",
                    noteNameEn = "$note$oct",
                    frequency = freq
                )
            )
        }

        return DastgahTuning(id, nameFa, nameEn, desc, strings)
    }

    private fun persianNoteName(western: String): String {
        return when (western) {
            "C" -> "دو"
            "C𝄰" -> "دو سُری"
            "C#" -> "دو دیز"
            "D𝄳" -> "رِ کُرُن"
            "D" -> "رِ"
            "D𝄰" -> "رِ سُری"
            "D#/E♭" -> "می بمل"
            "E𝄳" -> "می کُرُن"
            "E" -> "می"
            "F𝄳" -> "فا کُرُن"
            "F" -> "فا"
            "F𝄰" -> "فا سُری"
            "F#" -> "فا دیز"
            "G𝄳" -> "سل کُرُن"
            "G" -> "سل"
            "G𝄰" -> "سل سُری"
            "G#/A♭" -> "سل دیز"
            "A𝄳" -> "لا کُرُن"
            "A" -> "لا"
            "A𝄰" -> "لا سُری"
            "A#/B♭" -> "سی بمل"
            "B𝄳" -> "سی کُرُن"
            "B" -> "سی"
            "C𝄳" -> "دو کُرُن"
            else -> western
        }
    }

    fun findMatchingString(freq: Float, tuning: DastgahTuning): SantoorString? {
        if (freq <= 0f) return null
        return tuning.strings.minByOrNull { kotlin.math.abs(it.frequency - freq) }
            ?.takeIf {
                val ratio = freq / it.frequency
                // Within about 120 cents of target
                ratio in 0.93f..1.07f
            }
    }
}
