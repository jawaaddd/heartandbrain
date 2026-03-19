package com.heartandbrain.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vlogs")
data class Vlog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val title: String? = null,
)
