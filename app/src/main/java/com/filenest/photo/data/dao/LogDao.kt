package com.filenest.photo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.filenest.photo.data.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun save(entity: LogEntity): Long

    @Query("SELECT * FROM log")
    fun listUseFlow(): Flow<List<LogEntity>>
}