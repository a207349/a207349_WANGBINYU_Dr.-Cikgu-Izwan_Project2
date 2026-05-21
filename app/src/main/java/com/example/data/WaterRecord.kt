package com.example.healthyapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_table")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Int,
    val logMessage: String
)