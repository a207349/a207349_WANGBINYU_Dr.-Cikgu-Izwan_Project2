package com.example.healthyapp.firebase

import java.util.Date

data class CommunityPost(
    val id: String = "",
    val studentId: String = "",
    val tipText: String = "",
    val source: String = "HealthyAPP",
    val createdAt: Date? = null
)
