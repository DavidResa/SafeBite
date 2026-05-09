package com.example.safebite.data

data class Report(
    val id: String = "",
    val postId: String = "",
    val reporterId: String = "",
    val authorId: String = "",
    val reason: String = "",
    val postText: String = "",
    val timestamp: Long = 0L,
    val status: String = "pending" // "pending", "resolved"
)
