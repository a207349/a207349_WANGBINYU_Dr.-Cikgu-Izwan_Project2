package com.example.healthyapp.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private val postsRef = db.collection("community_posts")

    fun listenPosts(onUpdate: (List<CommunityPost>) -> Unit): ListenerRegistration {
        return postsRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val posts = snapshot?.documents?.map { doc ->
                    CommunityPost(
                        id = doc.id,
                        studentId = doc.getString("studentId") ?: "",
                        tipText = doc.getString("tipText") ?: "",
                        source = doc.getString("source") ?: "",
                        createdAt = doc.getTimestamp("createdAt")?.toDate()
                    )
                } ?: emptyList()
                onUpdate(posts)
            }
    }

    suspend fun addPost(studentId: String, tipText: String, source: String): String {
        val data = hashMapOf(
            "studentId" to studentId,
            "tipText" to tipText,
            "source" to source,
            "createdAt" to FieldValue.serverTimestamp()
        )
        val docRef = postsRef.add(data).await()
        return docRef.id
    }
}
