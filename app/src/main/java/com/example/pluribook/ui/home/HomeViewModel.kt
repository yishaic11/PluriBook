package com.example.pluribook.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val repository = PostRepository(
        postDao,
        userDao,
        app.sharedPreferences
    )

    val postsFlow: Flow<PagingData<Post>> = repository.getPostStream().cachedIn(viewModelScope)

    init {
        refreshPosts()
    }

    fun refreshPosts() {
        viewModelScope.launch {
            repository.syncPostsFromFirebase(isRefresh = true)
        }
    }

    fun loadMorePosts() {
        viewModelScope.launch {
            repository.syncPostsFromFirebase(isRefresh = false)
        }
    }

    fun savePostToLocal(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            postDao.insertPost(post)
        }
    }

    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            app.database.clearAllTables()

            app.sharedPreferences.edit().remove("last_sync_time").apply()

            FirebaseAuth.getInstance().signOut()

            withContext(Dispatchers.Main) {
                onLogoutComplete()
            }
        }
    }
}