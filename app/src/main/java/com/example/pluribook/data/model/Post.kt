package com.example.pluribook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val photoUrl: String,
    val description: String,
    val senderId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val likedBy: List<String> = emptyList()
) {
    constructor () : this("", "", "", "", 0L, emptyList())
}