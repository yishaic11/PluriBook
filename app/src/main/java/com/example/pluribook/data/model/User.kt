package com.example.pluribook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String,
    val username: String,
    val photoUrl: String,
    val email: String,
    val likedPosts: List<String> = emptyList()
)