package com.example.pluribook.ui.post.edit

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.TAG
import com.example.pluribook.data.api.BookItem
import com.example.pluribook.data.api.BookNetworkClient
import com.example.pluribook.data.api.BookSearchResponse
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditPostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(
        (application as PluribookApplication).database.postDao(),
        application.database.userDao()
    )

    private val _originalPost = MutableLiveData<ResourceState<Post>>()
    val originalPost: LiveData<ResourceState<Post>> = _originalPost

    private val _updateState = MutableLiveData<ResourceState<Unit>>()
    val updateState: LiveData<ResourceState<Unit>> = _updateState

    private val _searchResults = MutableLiveData<ResourceState<List<BookItem>>>()
    val searchResults: LiveData<ResourceState<List<BookItem>>> = _searchResults

    var selectedBook: BookItem? = null
    var defaultImageUrl: String? = null
    var loadedPostData: Post? = null

    fun loadPost(postId: String) {
        _originalPost.value = ResourceState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val post = (getApplication() as PluribookApplication).database.postDao()
                    .getPostById(postId)
                if (post != null) {
                    loadedPostData = post
                    _originalPost.postValue(ResourceState.Success(post))
                } else {
                    _originalPost.postValue(ResourceState.Error("Failed to load post data"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading original post", e)
                _originalPost.postValue(
                    ResourceState.Error(
                        e.message ?: "Failed to load post data"
                    )
                )
            }
        }
    }

    fun searchBooks(query: String) {
        if (query.isBlank()) return
        _searchResults.value = ResourceState.Loading

        val request = BookNetworkClient.bookApi.fetchBooks(query)
        request.enqueue(object : retrofit2.Callback<BookSearchResponse> {
            override fun onResponse(
                call: retrofit2.Call<BookSearchResponse>,
                response: retrofit2.Response<BookSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val books = response.body()?.items ?: emptyList()
                    _searchResults.value = ResourceState.Success(books)
                } else {
                    _searchResults.value = ResourceState.Error("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<BookSearchResponse>, t: Throwable) {
                _searchResults.value = ResourceState.Error("Failed to search books")
            }
        })
    }

    fun updatePost(postId: String, imageUri: Uri?, description: String) {
        val oldPost = loadedPostData ?: return
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
                val success = repository.updatePost(
                    postId,
                    imageUri,
                    imageUrlToSave,
                    description,
                    title,
                    author,
                    summary,
                    rating
                )
                if (success) _updateState.value = ResourceState.Success(Unit)
                else _updateState.value = ResourceState.Error("Failed to update post.")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating post", e)
                _updateState.value = ResourceState.Error(e.message ?: "An error occurred")
            }
        }
    }
}