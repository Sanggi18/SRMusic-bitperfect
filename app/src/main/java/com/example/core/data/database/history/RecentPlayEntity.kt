package com.example.core.data.database.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_plays")
data class RecentPlayEntity(
    @PrimaryKey val songId: Long,
    val playedAt: Long
)
