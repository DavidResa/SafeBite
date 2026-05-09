package com.example.safebite.data



data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val allergens: String = "",
    val favoriteFoods: String = "",
    val friends: List<String> = emptyList(),
    val profileImageUrl: String? = null,
    val isPrivate: Boolean = false,
    val isAdmin: Boolean = false,
    val bannedUntil: Long? = null,
    val warnings: List<Long> = emptyList()
)
