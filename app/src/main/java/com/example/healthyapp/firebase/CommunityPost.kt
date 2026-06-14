// 🔥 Firestore：社区帖子数据模型（映射到集合 community_posts）
package com.example.healthyapp.firebase

import java.util.Date

data class CommunityPost(
    val id: String = "",
    val studentId: String = "",
    val tipText: String = "",
    val source: String = "HealthyAPP",
    val createdAt: Date? = null
)
