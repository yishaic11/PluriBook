package com.example.pluribook.ui.comment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.TAG
import com.example.pluribook.data.model.Comment
import com.example.pluribook.data.repository.CommentRepository
import com.example.pluribook.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val commentDao = (application as PluribookApplication).database.commentDao()
    private val repository = CommentRepository(commentDao)
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var currentUserName = "Unknown"
    private var currentUserPhoto = ""

    private var currentPostId: String? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc =
                    FirebaseFirestore.getInstance().collection(UserRepository.USERS_COLLECTION).document(currentUserId)
                        .get().await()
                currentUserName = doc.getString("username") ?: "Unknown"
                currentUserPhoto = doc.getString("photoUrl") ?: ""
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sender", e)
            }
        }
    }

    fun getCommentsFlow(postId: String): Flow<PagingData<Comment>> {
        if (currentPostId != postId) {
            currentPostId = postId
            repository.syncComments(postId)
        }

        return repository.getCommentStream(postId).cachedIn(viewModelScope)
    }

    fun addComment(postId: String, text: String, onSuccess: () -> Unit) {
        if (text.isBlank()) return
        val comment = Comment(
            postId = postId,
            senderId = currentUserId,
            senderName = currentUserName,
            senderPhotoUrl = currentUserPhoto,
            text = text.trim()
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.addComment(comment)
            launch(Dispatchers.Main) { onSuccess() }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSyncingComments()
    }
}