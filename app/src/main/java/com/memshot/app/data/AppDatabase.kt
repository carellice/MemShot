package com.memshot.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(SEP)

    @TypeConverter
    fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEP)

    @TypeConverter
    fun fromStatus(status: ShotStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ShotStatus = ShotStatus.valueOf(value)

    private companion object {
        const val SEP = "\u001F"
    }
}

@Database(entities = [Shot::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shots(): ShotDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "memshot.db")
                .build()
                .also { instance = it }
        }
    }
}
