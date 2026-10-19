package com.example.core.data.database.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPlayDao {
    @Query("SELECT * FROM recent_plays ORDER BY playedAt DESC")
    fun observeRecent(): Flow<List<RecentPlayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RecentPlayEntity): Long

    @Query("DELETE FROM recent_plays WHERE playedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM recent_plays WHERE songId NOT IN (SELECT songId FROM recent_plays ORDER BY playedAt DESC LIMIT 50)")
    suspend fun trimToMax50(): Int

    @Query("DELETE FROM recent_plays")
    suspend fun clearAll(): Int
}
