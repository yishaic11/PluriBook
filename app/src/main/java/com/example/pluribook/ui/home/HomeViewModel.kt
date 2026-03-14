package com.example.pluribook.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.repository.PostPagingSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val postDao = (application as PluribookApplication).database.postDao()

    val postsFlow: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { PostPagingSource(firestore) }
    ).flow.cachedIn(viewModelScope)


    fun savePostToLocal(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            postDao.insertPost(post)
        }
    }
}