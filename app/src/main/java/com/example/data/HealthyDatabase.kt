package com.example.healthyapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WaterRecord::class], version = 1, exportSchema = false)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var Instance: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, HealthDatabase::class.java, "health_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}