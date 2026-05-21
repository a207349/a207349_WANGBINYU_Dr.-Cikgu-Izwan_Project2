package com.example.healthyapp.data

import kotlinx.coroutines.flow.Flow

class HealthRepository(private val waterDao: WaterDao) {
    val allRecords: Flow<List<WaterRecord>> = waterDao.getAllRecords()

    suspend fun insert(record: WaterRecord) {
        waterDao.insert(record)
    }
}