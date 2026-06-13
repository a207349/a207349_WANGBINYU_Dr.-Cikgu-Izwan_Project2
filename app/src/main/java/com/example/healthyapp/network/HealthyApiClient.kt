package com.example.healthyapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object HealthyApiClient {
    private const val BASE_URL = "http://lrgs.ftsm.ukm.my/users/a207349/healthyapp/"

    val api: HealthyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HealthyApiService::class.java)
    }
}
