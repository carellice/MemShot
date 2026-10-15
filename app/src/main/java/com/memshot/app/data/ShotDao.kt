package com.memshot.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShotDao {
    @Query("SELECT * FROM shots ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Shot>>

    @Query("SELECT * FROM shots ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<Shot>

    @Query("SELECT * FROM shots WHERE id = :id")
    fun observe(id: Long): Flow<Shot?>

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun get(id: Long): Shot?

    @Query("SELECT * FROM shots WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun pending(limit: Int): List<Shot>

    @Query("SELECT COUNT(*) FROM shots WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM shots WHERE sourceKey = :key)")
    suspend fun exists(key: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(shot: Shot): Long

    @Update
    suspend fun update(shot: Shot)

    @Query("UPDATE shots SET status = 'PENDING'")
    suspend fun markAllPending()

    @Query("DELETE FROM shots WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT * FROM shots WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Shot>
}
