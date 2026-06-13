package com.example.healthyapp.network

import com.google.gson.annotations.SerializedName

data class GenericResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

data class ProfileResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val profile: ProfileData? = null
)

data class ProfileData(
    val id: Int = 0,
    @SerializedName("user_name") val userName: String = "",
    @SerializedName("student_id") val studentId: String = "",
    val age: Int = 0,
    @SerializedName("height_cm") val heightCm: Int = 0,
    @SerializedName("weight_kg") val weightKg: Int = 0,
    @SerializedName("daily_water_goal_ml") val dailyWaterGoalMl: Int = 2000,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class AddWaterResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val id: Int? = null
)

data class WaterHistoryResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val items: List<WaterLogItem>? = null
)

data class WaterLogItem(
    val id: Int = 0,
    @SerializedName("student_id") val studentId: String = "",
    @SerializedName("amount_ml") val amountMl: Int = 0,
    val note: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DashboardResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val profile: ProfileData? = null,
    @SerializedName("today_water_ml") val todayWaterMl: Int = 0
)
