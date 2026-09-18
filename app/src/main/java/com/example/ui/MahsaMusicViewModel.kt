package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.DastgahTuning
import com.example.audio.DetectedNote
import com.example.audio.DetectedTempo
import com.example.audio.MetronomeEngine
import com.example.audio.PitchDetector
import com.example.audio.SantoorScale
import com.example.audio.TempoDetector
import com.example.data.AiAdvisor
import com.example.data.AppDatabase
import com.example.data.PracticeRepository
import com.example.data.PracticeSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String, val titleEn: String) {
    TUNER("تیونر سنتور", "Santoor Tuner"),
    METRONOME("مترونوم و تمپو", "Metronome & Tempo"),
    PRACTICE("ثبت تمرین", "Practice Tracker"),
    COACH("مشاور هوشمند", "AI Coach")
}

class MahsaMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PracticeRepository
    private val pitchDetector = PitchDetector()
    private val tempoDetector = TempoDetector()
    private val metronomeEngine = MetronomeEngine()

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.TUNER)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Tuner & Audio State
    val tunings: List<DastgahTuning> = SantoorScale.getDastgahTunings()
    private val _selectedTuning = MutableStateFlow(tunings.first())
    val selectedTuning: StateFlow<DastgahTuning> = _selectedTuning.asStateFlow()

    private val _isMicrophoneActive = MutableStateFlow(false)
    val isMicrophoneActive: StateFlow<Boolean> = _isMicrophoneActive.asStateFlow()

    val pitchState: StateFlow<PitchDetector.PitchState> = pitchDetector.pitchState
    val liveTempo: StateFlow<DetectedTempo> = tempoDetector.tempoState

    // Metronome
    private val _metronomeBpm = MutableStateFlow(100)
    val metronomeBpm: StateFlow<Int> = _metronomeBpm.asStateFlow()

    private val _metronomeMeter = MutableStateFlow(4)
    val metronomeMeter: StateFlow<Int> = _metronomeMeter.asStateFlow()

    val isMetronomePlaying: StateFlow<Boolean> = metronomeEngine.isPlaying
    val currentMetronomeBeat: StateFlow<Int> = metronomeEngine.currentBeat

    private val _isMetronomeMuted = MutableStateFlow(false)
    val isMetronomeMuted: StateFlow<Boolean> = _isMetronomeMuted.asStateFlow()

    // Practice Tracker State
    private val _isPracticing = MutableStateFlow(false)
    val isPracticing: StateFlow<Boolean> = _isPracticing.asStateFlow()

    private val _practiceSeconds = MutableStateFlow(0L)
    val practiceSeconds: StateFlow<Long> = _practiceSeconds.asStateFlow()

    private val _practiceNotesCount = MutableStateFlow(0)
    val practiceNotesCount: StateFlow<Int> = _practiceNotesCount.asStateFlow()

    private val _practiceInTuneCount = MutableStateFlow(0)
    val practiceInTuneCount: StateFlow<Int> = _practiceInTuneCount.asStateFlow()

    private val practiceBpmSamples = mutableListOf<Int>()
    private var practiceTimerJob: Job? = null

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    // Database Flows
    val allSessions: StateFlow<List<PracticeSession>>
    val totalSeconds: StateFlow<Long?>
    val sessionCount: StateFlow<Int>

    // AI Coach State
    private val _aiAdviceText = MutableStateFlow("")
    val aiAdviceText: StateFlow<String> = _aiAdviceText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _selectedSessionForCoach = MutableStateFlow<PracticeSession?>(null)
    val selectedSessionForCoach: StateFlow<PracticeSession?> = _selectedSessionForCoach.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PracticeRepository(db.practiceDao())

        allSessions = repository.allSessions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        totalSeconds = repository.totalSeconds.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0L
        )
        sessionCount = repository.sessionCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        // Listen to onset events for tempo estimation
        viewModelScope.launch {
            pitchDetector.onsetEvent.collect { timestamp ->
                if (timestamp > 0) {
                    tempoDetector.registerOnset(timestamp)
                }
            }
        }

        // Track notes during active practice
        viewModelScope.launch {
            pitchDetector.pitchState.collect { state ->
                if (_isPracticing.value && state is PitchDetector.PitchState.NoteDetected) {
                    _practiceNotesCount.value += 1
                    if (state.detectedNote.isInTune) {
                        _practiceInTuneCount.value += 1
                    }
                }
            }
        }

        // Collect tempo samples during active practice
        viewModelScope.launch {
            tempoDetector.tempoState.collect { tempo ->
                if (_isPracticing.value && tempo.bpm > 0) {
                    synchronized(practiceBpmSamples) {
                        practiceBpmSamples.add(tempo.bpm)
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectTuning(tuning: DastgahTuning) {
        _selectedTuning.value = tuning
        if (_isMicrophoneActive.value) {
            pitchDetector.stop()
            pitchDetector.start(viewModelScope, tuning)
        }
    }

    fun toggleMicrophone(enabled: Boolean) {
        _isMicrophoneActive.value = enabled
        if (enabled) {
            pitchDetector.start(viewModelScope, _selectedTuning.value)
        } else {
            pitchDetector.stop()
            tempoDetector.reset()
        }
    }

    // Metronome Controls
    fun toggleMetronome() {
        if (metronomeEngine.isPlaying.value) {
            metronomeEngine.stop()
        } else {
            metronomeEngine.start(
                viewModelScope,
                _metronomeBpm.value,
                _metronomeMeter.value,
                _isMetronomeMuted.value
            )
        }
    }

    fun setMetronomeBpm(newBpm: Int) {
        val clamped = newBpm.coerceIn(30, 260)
        _metronomeBpm.value = clamped
        metronomeEngine.updateBpm(clamped)
    }

    fun setMetronomeMeter(newMeter: Int) {
        _metronomeMeter.value = newMeter
        metronomeEngine.updateMeter(newMeter)
    }

    fun toggleMetronomeMute() {
        val newVal = !_isMetronomeMuted.value
        _isMetronomeMuted.value = newVal
        metronomeEngine.setMuted(newVal)
    }

    fun tapTempo() {
        val detected = tempoDetector.registerTap()
        if (detected > 0) {
            setMetronomeBpm(detected)
        }
    }

    fun adoptDetectedTempo() {
        val detected = liveTempo.value.bpm
        if (detected in 40..240) {
            setMetronomeBpm(detected)
        }
    }

    // Practice Session Controls
    fun startPracticeSession() {
        if (!_isMicrophoneActive.value) {
            toggleMicrophone(true)
        }
        _isPracticing.value = true
        _practiceSeconds.value = 0L
        _practiceNotesCount.value = 0
        _practiceInTuneCount.value = 0
        synchronized(practiceBpmSamples) { practiceBpmSamples.clear() }

        practiceTimerJob?.cancel()
        practiceTimerJob = viewModelScope.launch {
            while (isActive && _isPracticing.value) {
                delay(1000L)
                _practiceSeconds.value += 1
            }
        }
    }

    fun finishPracticeSession() {
        _isPracticing.value = false
        practiceTimerJob?.cancel()
        practiceTimerJob = null
        _showSaveDialog.value = true
    }

    fun dismissSaveDialog() {
        _showSaveDialog.value = false
    }

    fun savePracticeSession(title: String, notes: String) {
        val duration = _practiceSeconds.value
        val notesCount = _practiceNotesCount.value
        val inTuneCount = _practiceInTuneCount.value
        val accuracy = if (notesCount > 0) {
            ((inTuneCount.toFloat() / notesCount.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val avgBpm = synchronized(practiceBpmSamples) {
            if (practiceBpmSamples.isNotEmpty()) {
                practiceBpmSamples.average().toInt().coerceIn(40, 240)
            } else {
                _metronomeBpm.value
            }
        }

        val session = PracticeSession(
            title = title.ifBlank { "تمرین ${_selectedTuning.value.nameFa}" },
            dastgah = _selectedTuning.value.nameFa,
            durationSeconds = duration,
            avgBpm = avgBpm,
            notesCount = notesCount,
            accuracyScore = accuracy,
            userNotes = notes,
            meter = "${_metronomeMeter.value}/4"
        )

        viewModelScope.launch {
            repository.insert(session)
            _showSaveDialog.value = false
            // Reset counters
            _practiceSeconds.value = 0L
            _practiceNotesCount.value = 0
            _practiceInTuneCount.value = 0
        }
    }

    fun deleteSession(session: PracticeSession) {
        viewModelScope.launch {
            repository.delete(session)
            if (_selectedSessionForCoach.value?.id == session.id) {
                _selectedSessionForCoach.value = null
                _aiAdviceText.value = ""
            }
        }
    }

    // AI Coach
    fun requestAiAdvice(session: PracticeSession?, customQuestion: String = "") {
        val targetSession = session ?: PracticeSession(
            title = "تمرین ردیف سنتور",
            dastgah = _selectedTuning.value.nameFa,
            durationSeconds = _practiceSeconds.value.coerceAtLeast(300L),
            avgBpm = _metronomeBpm.value,
            notesCount = _practiceNotesCount.value.coerceAtLeast(45),
            accuracyScore = if (_practiceNotesCount.value > 0) {
                ((_practiceInTuneCount.value.toFloat() / _practiceNotesCount.value.toFloat()) * 100f).toInt()
            } else 85,
            userNotes = customQuestion
        )

        _selectedSessionForCoach.value = targetSession
        _isAiLoading.value = true
        _aiAdviceText.value = ""

        viewModelScope.launch {
            val response = AiAdvisor.analyzePractice(targetSession, customQuestion)
            _aiAdviceText.value = response
            _isAiLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        pitchDetector.stop()
        metronomeEngine.release()
        practiceTimerJob?.cancel()
    }
}
