package com.example.pluribook.ui.post.create

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
import kotlinx.coroutines.launch

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val app = (application as PluribookApplication)
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val repository = PostRepository(
        postDao, userDao
    )

    private val _postState = MutableLiveData<ResourceState<Unit>>()
    val postState: LiveData<ResourceState<Unit>> = _postState

    private val _searchResultsState = MutableLiveData<ResourceState<List<BookItem>>>()
    val searchResultsState: LiveData<ResourceState<List<BookItem>>> = _searchResultsState

    var selectedBook: BookItem? = null
    var defaultImageUrl: String? = null

    fun searchBooks(query: String) {
        if (query.isBlank()) return

        _searchResultsState.value = ResourceState.Loading

        val request = BookNetworkClient.bookApi.fetchBooks(query)

        request.enqueue(object : retrofit2.Callback<BookSearchResponse> {

            override fun onResponse(
                call: retrofit2.Call<BookSearchResponse>,
                response: retrofit2.Response<BookSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val books = response.body()?.items ?: emptyList()
                    _searchResultsState.value = ResourceState.Success(books)
                } else {
                    _searchResultsState.value =
                        ResourceState.Error("Error response: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<BookSearchResponse>, t: Throwable) {
                _searchResultsState.value =
                    ResourceState.Error("Failed to search books: ${t.message}")
            }
        })
    }

    fun createPost(imageUri: Uri?, description: String) {
        val book = selectedBook
        if (book == null) {
            _postState.value = ResourceState.Error("Please search and select a book.")
            return
        }

        if (imageUri == null && defaultImageUrl.isNullOrEmpty()) {
            _postState.value = ResourceState.Error("Please select a photo.")
            return
        }

        if (description.isBlank()) {
            _postState.value = ResourceState.Error("Please enter your thoughts.")
            return
        }

        _postState.value = ResourceState.Loading

        val title = book.volumeInfo.title ?: "Unknown Title"
        val author = book.volumeInfo.authors?.firstOrNull() ?: ""
        val summary = book.volumeInfo.description ?: ""
        val rating = book.volumeInfo.averageRating ?: Post.DEFAULT_RATING

        viewModelScope.launch {
            try {
                val success = repository.createPost(
                    imageUri,
                    defaultImageUrl,
                    description,
                    title,
                    author,
                    summary,
                    rating
                )
                if (success) _postState.value = ResourceState.Success(Unit)
                else _postState.value = ResourceState.Error("Failed to publish post.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create book post: ${e.message}")
                _postState.value = ResourceState.Error(e.message ?: "An error occurred")
            }
        }
    }
}