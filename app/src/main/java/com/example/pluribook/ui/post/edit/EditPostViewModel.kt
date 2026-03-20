package com.example.pluribook.ui.post.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.api.BookItem
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditPostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = (application as PluribookApplication)
    private val repository = PostRepository(app.database.postDao(), app.database.userDao())

    private val _originalPost = MutableLiveData<ResourceState<Post>>()
    val originalPost: LiveData<ResourceState<Post>> = _originalPost

    private val _updateState = MutableLiveData<ResourceState<Unit>>()
    val updateState: LiveData<ResourceState<Unit>> = _updateState

    private val _searchResults = MutableLiveData<ResourceState<List<BookItem>>>()
    val searchResults: LiveData<ResourceState<List<BookItem>>> = _searchResults

    var selectedBook: BookItem? = null
    var defaultImageUrl: String? = null

    fun loadPost(postId: String) {
        _originalPost.value = ResourceState.Loading
        viewModelScope.launch {
            try {
                val post = repository.fetchPost(postId)
                if (post != null) _originalPost.postValue(ResourceState.Success(post))
                else _originalPost.postValue(ResourceState.Error("Post not found"))
            } catch (e: Exception) {
                _originalPost.postValue(ResourceState.Error(e.message ?: "An error occurred"))
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        _searchResults.value = ResourceState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val books = repository.searchBooks(query)
                _searchResults.postValue(ResourceState.Success(books))
            } catch (e: Exception) {
                _searchResults.postValue(ResourceState.Error("Failed to search books"))
            }
        }
    }

    fun updatePost(postId: String, imageUri: Uri?, description: String) {
        val currentState = _originalPost.value
        if (currentState !is ResourceState.Success) return
        val oldPost = currentState.data

        if (description.isBlank()) {
            _updateState.value = ResourceState.Error("Description cannot be empty.")
            return
        }

        _updateState.value = ResourceState.Loading

        val book = selectedBook
        val title = book?.volumeInfo?.title ?: oldPost.bookTitle
        val author = book?.volumeInfo?.authors?.firstOrNull() ?: oldPost.bookAuthor
        val summary = book?.volumeInfo?.description ?: oldPost.bookSummary
        val rating = book?.volumeInfo?.averageRating ?: oldPost.bookRating

        val imageUrlToSave = if (imageUri == null && book != null) {
            defaultImageUrl ?: ""
        } else if (imageUri == null) {
            oldPost.photoUrl
        } else {
            defaultImageUrl ?: oldPost.photoUrl
        }

        viewModelScope.launch {
            try {
                val success = repository.updatePost(postId, imageUri, imageUrlToSave, description, title, author, summary, rating)
                if (success) _updateState.value = ResourceState.Success(Unit)
                else _updateState.value = ResourceState.Error("Failed to update post.")
            } catch (e: Exception) {
                _updateState.value = ResourceState.Error(e.message ?: "An error occurred")
            }
        }
    }
}