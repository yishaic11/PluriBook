package com.example.pluribook.ui.post

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
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.launch

class CreatePostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(
        (application as PluribookApplication).database.postDao(),
        application.database.userDao()
    )

    private val _postState = MutableLiveData<ResourceState<Unit>>()
    val postState: LiveData<ResourceState<Unit>> = _postState

    private val _searchResults = MutableLiveData<ResourceState<List<BookItem>>>()
    val searchResults: LiveData<ResourceState<List<BookItem>>> = _searchResults

    var selectedBook: BookItem? = null
    var defaultImageUrl: String? = null

    fun searchBooks(query: String) {
        if (query.isBlank()) return

        _searchResults.value = ResourceState.Loading

        val request = BookNetworkClient.bookApi.searchBooks(query)

        request.enqueue(object : retrofit2.Callback<BookSearchResponse> {

            override fun onResponse(
                call: retrofit2.Call<BookSearchResponse>,
                response: retrofit2.Response<BookSearchResponse>
            ) {
                if (response.isSuccessful) {
                    val books = response.body()?.items ?: emptyList()
                    _searchResults.value = ResourceState.Success(books)
                } else {
                    _searchResults.value = ResourceState.Error("Error response: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<BookSearchResponse>, t: Throwable) {
                _searchResults.value = ResourceState.Error("Failed to search books: ${t.message}")
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
        val rating = book.volumeInfo.averageRating ?: 0.0

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