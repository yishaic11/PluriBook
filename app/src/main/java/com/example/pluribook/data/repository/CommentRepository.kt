package com.example.pluribook.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.pluribook.TAG
import com.example.pluribook.data.local.CommentDao
import com.example.pluribook.data.model.Comment
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentRepository(
    private val commentDao: CommentDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val COMMENTS_COLLECTION = "comments"
        private const val PAGE_SIZE = 10
    }

    private var commentListener: ListenerRegistration? = null
    private val commentsCollection = firestore.collection(COMMENTS_COLLECTION)

    private fun getPostCommentsQuery(postId: String) =
        commentsCollection.whereEqualTo("postId", postId)

    fun getCommentStream(postId: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { commentDao.getPagedComments(postId) }
        ).flow
    }

    fun getCommentStreamCount(postId: String): Flow<Int> {
        return commentDao.getCommentCount(postId)
    }

    fun syncComments(postId: String) {
        commentListener?.remove()

        commentListener = getPostCommentsQuery(postId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Listen failed for comments.", e)
                return@addSnapshotListener
            }

            snapshot?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    for (dc in it.documentChanges) {
                        val comment = dc.document.toObject(Comment::class.java)
                        when (dc.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                commentDao.insertComment(comment)
                            }
                            DocumentChange.Type.REMOVED -> {
                                commentDao.deleteComment(comment.id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopSyncingComments() {
        commentListener?.remove()
        commentListener = null
    }

    suspend fun addComment(comment: Comment): Boolean {
        return try {
            commentsCollection.document(comment.id).set(comment).await()
            commentDao.insertComment(comment)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating comment: ${e.message}", e)
            false
        }
    }

    suspend fun deleteComment(commentId: String) {
        try {
            commentsCollection.document(commentId).delete().await()
            commentDao.deleteComment(commentId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment: ${e.message}", e)
        }
    }
}