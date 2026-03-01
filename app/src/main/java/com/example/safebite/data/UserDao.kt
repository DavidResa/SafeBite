package com.example.safebite.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE name = :name AND password = :password LIMIT 1")
    suspend fun login(name: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun signup(user: User)

    @Query("SELECT EXISTS(SELECT * FROM users WHERE name = :name)")
    suspend fun userExists(name: String): Boolean

    @Query("SELECT * FROM users WHERE name = :name LIMIT 1")
    suspend fun getUserByName(name: String): User?
}
