package com.heartandbrain.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VlogDao {
    @Insert
    suspend fun insert(vlog: Vlog): Long

    @Query("SELECT * FROM vlogs WHERE id = :id")
    suspend fun getById(id: Long): Vlog?
}
