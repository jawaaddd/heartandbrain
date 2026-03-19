package com.heartandbrain.app.data

import kotlinx.coroutines.flow.Flow

class ClipRepository(private val dao: ClipDao) {

    fun pinnedClips(): Flow<List<Clip>> = dao.pinnedClips()

    fun recentClips(): Flow<List<Clip>> {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        return dao.recentClips(sevenDaysAgo)
    }

    suspend fun saveClips(clips: List<Clip>) = dao.insertAll(clips)

    suspend fun setPinned(clip: Clip, pinned: Boolean) = dao.update(clip.copy(isPinned = pinned))

    suspend fun randomClip(): Clip? = dao.randomClip()
}
