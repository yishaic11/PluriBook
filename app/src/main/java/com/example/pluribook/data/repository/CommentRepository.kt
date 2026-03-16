package com.example.pluribook.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pluribook.data.local.CommentDao
import com.example.pluribook.data.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class CommentRepository(
    private val commentDao: CommentDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun getCommentsRef(postId: String) =
        firestore.collection("posts").document(postId).collection("comments")

    fun getCommentStream(postId: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { commentDao.getPagedComments(postId) }
        ).flow
    }

    suspend fun syncComments(postId: String) {
        try {
            val snapshot = getCommentsRef(postId).get().await()
            val comments = snapshot.toObjects(Comment::class.java)
            commentDao.insertComments(comments)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addComment(comment: Comment): Boolean {
        return try {
            getCommentsRef(comment.postId).document(comment.id).set(comment).await()
            commentDao.insertComment(comment)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        try {
            getCommentsRef(postId).document(commentId).delete().await()
            commentDao.deleteComment(commentId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}