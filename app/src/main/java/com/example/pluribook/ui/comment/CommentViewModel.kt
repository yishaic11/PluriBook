package com.example.pluribook.ui.comment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.local.CommentWithSender
import com.example.pluribook.data.model.Comment
import com.example.pluribook.data.repository.CommentRepository
import com.example.pluribook.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PluribookApplication

    private val userDao = app.database.userDao()
    private val commentDao = app.database.commentDao()

    private val userRepository = UserRepository(userDao)
    private val repository = CommentRepository(commentDao, userRepository)

    val currentUserId = userRepository.getCurrentUserId() ?: ""
    private var currentPostId: String? = null

    fun getCommentsFlow(postId: String): Flow<PagingData<CommentWithSender>> {
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