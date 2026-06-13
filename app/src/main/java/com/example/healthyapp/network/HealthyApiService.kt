package com.example.healthyapp.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface HealthyApiService {

    @POST("api.php")
    @FormUrlEncoded
    suspend fun saveProfile(
        @Query("action") action: String = "save_profile",
        @Field("user_name") userName: String,
        @Field("student_id") studentId: String,
        @Field("age") age: Int,
        @Field("height_cm") heightCm: Int,
        @Field("weight_kg") weightKg: Int,
        @Field("daily_water_goal_ml") dailyGoal: Int
    ): GenericResponse

    @GET("api.php")
    suspend fun getProfile(
        @Query("action") action: String = "get_profile",
        @Query("student_id") studentId: String
    ): ProfileResponse

    @POST("api.php")
    @FormUrlEncoded
    suspend fun addWater(
        @Query("action") action: String = "add_water",
        @Field("student_id") studentId: String,
        @Field("amount_ml") amountMl: Int,
        @Field("note") note: String? = null
    ): AddWaterResponse

    @GET("api.php")
    suspend fun getWaterHistory(
        @Query("action") action: String = "get_water_history",
        @Query("student_id") studentId: String
    ): WaterHistoryResponse

    @GET("api.php")
    suspend fun getDashboard(
        @Query("action") action: String = "get_dashboard",
        @Query("student_id") studentId: String
    ): DashboardResponse

    @POST("api.php")
    @FormUrlEncoded
    suspend fun deleteWater(
        @Query("action") action: String = "delete_water",
        @Field("id") id: Int
    ): GenericResponse

}
