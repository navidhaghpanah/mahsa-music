package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val dastgah: String,
    val durationSeconds: Long,
    val avgBpm: Int,
    val notesCount: Int,
    val accuracyScore: Int, // 0 to 100%
    val timestamp: Long = System.currentTimeMillis(),
    val userNotes: String = "",
    val meter: String = "4/4"
)
