package com.example.safebite.data

data class FriendRequest(
    val id: String = "",
    val fromId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String? = null,
    val toId: String = "",
    val status: String = "pending", // "pending", "accepted", "rejected"
    val timestamp: Long = System.currentTimeMillis()
)
