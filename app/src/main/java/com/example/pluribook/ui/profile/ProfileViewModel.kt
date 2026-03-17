package com.example.pluribook.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.TAG
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()

    private val repository = PostRepository(postDao, userDao)
    val userRepository = UserRepository(userDao)

    private val _userProfileState = MutableLiveData<ResourceState<User>>()
    val userProfileState: LiveData<ResourceState<User>> = _userProfileState

    private val _likedPostIds = MutableLiveData<List<String>>()
    val likedPostIds: LiveData<List<String>> = _likedPostIds

    fun getPostsFlow(userId: String): Flow<PagingData<Post>> {
        return repository.getPostsBySenderStream(userId).cachedIn(viewModelScope)
    }

    fun getLikedPostsFlow(postIds: List<String>): Flow<PagingData<Post>> {
        return repository.getLikedPostsStream(postIds).cachedIn(viewModelScope)
    }

    fun loadUserProfile(uid: String) {
        _userProfileState.value = ResourceState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.getUserProfile(uid)
                if (user != null) {
                    _userProfileState.value = ResourceState.Success(user)
                } else {
                    _userProfileState.value = ResourceState.Error("User not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _userProfileState.value = ResourceState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun refreshUserPosts(targetUserId: String) {
        viewModelScope.launch {
            repository.syncUserPosts(targetUserId)
        }
    }

    fun refreshLikedPosts(targetUserId: String) {
        viewModelScope.launch {
            val ids = repository.syncAndGetLikedPostIds(targetUserId)
            _likedPostIds.value = ids
        }
    }
}