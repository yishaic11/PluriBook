package com.example.pluribook.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pluribook.TAG
import com.example.pluribook.data.local.CommentDao
import com.example.pluribook.data.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class CommentRepository(
    private val commentDao: CommentDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val COMMENTS_COLLECTION = "posts"
        private const val PAGE_SIZE = 10
    }

    private fun getCommentsRef(postId: String) =
        firestore.collection(PostRepository.POSTS_COLLECTION).document(postId)
            .collection(COMMENTS_COLLECTION)

    fun getCommentStream(postId: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { commentDao.getPagedComments(postId) }
        ).flow
    }

    suspend fun syncComments(postId: String) {
        try {
            val snapshot = getCommentsRef(postId).get().await()
            val comments = snapshot.toObjects(Comment::class.java)
            commentDao.insertComments(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments from Firebase", e)
            e.printStackTrace()
        }
    }

    suspend fun addComment(comment: Comment): Boolean {
        return try {
            getCommentsRef(comment.postId).document(comment.id).set(comment).await()
            commentDao.insertComment(comment)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating comment: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteComment(postId: String, commentId: String) {
        try {
            getCommentsRef(postId).document(commentId).delete().await()
            commentDao.deleteComment(commentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment: ${e.message}", e)
            e.printStackTrace()
        }
    }
}