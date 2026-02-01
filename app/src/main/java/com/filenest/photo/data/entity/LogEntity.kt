package com.filenest.photo.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log")
data class LogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "time")
    val time: Long = System.currentTimeMillis(),
)
