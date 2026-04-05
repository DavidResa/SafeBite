package com.example.safebite.data



data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val allergens: String = "",
    val friends: List<String> = emptyList(),
    val profileImageUrl: String? = null
)
