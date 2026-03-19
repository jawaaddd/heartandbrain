package com.heartandbrain.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ClipType { CLIP, QUOTE }

enum class Category { GOAL, COMMITMENT, EMOTIONAL, REMINDER, REFLECTION }

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = Vlog::class,
            parentColumns = ["id"],
            childColumns = ["vlogId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("vlogId")],
)
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vlogId: Long,
    val filePath: String,  // denormalized from Vlog for easy playback access
    val startTime: Float,
    val endTime: Float,
    val title: String,
    val type: ClipType,
    val quoteText: String? = null,
    val category: Category,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
