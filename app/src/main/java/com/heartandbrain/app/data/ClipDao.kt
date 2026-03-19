package com.heartandbrain.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    @Insert
    suspend fun insertAll(clips: List<Clip>)

    @Query("SELECT * FROM clips WHERE isPinned = 1 ORDER BY createdAt DESC")
    fun pinnedClips(): Flow<List<Clip>>

    @Query("""
        SELECT * FROM clips
        WHERE isPinned = 0 AND createdAt >= :since
        ORDER BY createdAt DESC
    """)
    fun recentClips(since: Long): Flow<List<Clip>>

    @Update
    suspend fun update(clip: Clip)

    @Query("SELECT * FROM clips ORDER BY RANDOM() LIMIT 1")
    suspend fun randomClip(): Clip?
}
