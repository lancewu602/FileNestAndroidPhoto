package com.filenest.photo.data

import android.content.Context
import androidx.room.*
import com.filenest.photo.data.dao.LogDao
import com.filenest.photo.data.entity.LogEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Singleton

@Database(
    entities = [
        LogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
}

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "db_001"
        ).build()
    }

    @Provides
    fun provideLogDao(db: AppDatabase): LogDao {
        return db.logDao()
    }

}

/**
 * 自定义类型转换器
 */
class CustomTypeConverters {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, formatter)
        }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.ofEpochMilli(it) }
    }

}