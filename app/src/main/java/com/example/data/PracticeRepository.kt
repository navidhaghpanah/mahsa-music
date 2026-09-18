package com.example.data

import kotlinx.coroutines.flow.Flow

class PracticeRepository(private val dao: PracticeDao) {
    val allSessions: Flow<List<PracticeSession>> = dao.getAllSessions()
    val sessionCount: Flow<Int> = dao.getSessionCount()
    val totalSeconds: Flow<Long?> = dao.getTotalPracticeSeconds()

    suspend fun insert(session: PracticeSession): Long {
        return dao.insertSession(session)
    }

    suspend fun delete(session: PracticeSession) {
        dao.deleteSession(session)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteSessionById(id)
    }
}
