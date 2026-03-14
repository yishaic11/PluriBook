package com.example.pluribook.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pluribook.data.model.Post
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getPagedPosts(): PagingSource<Int, Post>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC") // Get newest posts first
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE senderId = :id")
    suspend fun getPostsBySenderId(id: String): List<Post>

    @Query(
        """
        UPDATE posts 
        SET 
            description = COALESCE(:description, description),
            photoUrl = COALESCE(:photoUrl, photoUrl)
        WHERE id = :postId
    """
    )
    suspend fun updatePost(postId: String, description: String?, photoUrl: String?)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): Post?

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
}