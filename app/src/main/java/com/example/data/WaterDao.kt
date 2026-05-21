package com.example.healthyapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Insert
    suspend fun insert(record: WaterRecord)

    @Query("SELECT * FROM water_table ORDER BY id DESC")
    fun getAllRecords(): Flow<List<WaterRecord>>
}