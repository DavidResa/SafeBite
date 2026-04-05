package com.example.safebite.data

data class Post(
    val id: String = "",
    val category: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val textContent: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "image" or "video"
    val timestamp: Long = 0L
)
