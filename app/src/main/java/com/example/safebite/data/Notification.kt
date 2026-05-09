package com.example.safebite.data

data class Notification(
    val id: String = "",
    val toId: String = "",
    val message: String = "",
    val type: String = "warning", // "warning" or "info"
    val timestamp: Long = 0L,
    val read: Boolean = false
)
