package com.example.pluribook.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pluribook.data.model.Post

@Dao
interface PostsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC") // Get newest posts first
    suspend fun getAllPosts(): List<Post>

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
}