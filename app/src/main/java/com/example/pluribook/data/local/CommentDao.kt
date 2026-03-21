package com.example.pluribook.data.local

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pluribook.data.model.Comment
import kotlinx.coroutines.flow.Flow

data class CommentWithSender(
    @Embedded val comment: Comment,
    @ColumnInfo(name = "username") val senderName: String?,
    @ColumnInfo(name = "photoUrl") val senderPhotoUrl: String?
)

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<Comment>)

    @Query("""
        SELECT c.*, u.username, u.photoUrl 
        FROM comments c 
        LEFT JOIN users u ON c.senderId = u.uid 
        WHERE c.postId = :postId 
        ORDER BY c.createdAt ASC
    """)
    fun getPagedComments(postId: String): PagingSource<Int, CommentWithSender>

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    fun getCommentCount(postId: String): Flow<Int>

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)
}