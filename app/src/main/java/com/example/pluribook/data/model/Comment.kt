package com.example.pluribook.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val postId: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", "", 0L)
}