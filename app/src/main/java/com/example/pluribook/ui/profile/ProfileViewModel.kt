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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val postDao = (application as PluribookApplication).database.postDao()
    private val userDao = (application as PluribookApplication).database.userDao()

    private val repository = PostRepository(postDao, userDao)
    val userRepository = UserRepository(userDao)

    private val _userProfileState = MutableLiveData<ResourceState<User>>()
    val userProfileState: LiveData<ResourceState<User>> = _userProfileState

    private val _targetUserForPosts = MutableStateFlow<String?>(null)
    private val _likedPostIds = MutableStateFlow<List<String>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val userPostsFlow: Flow<PagingData<Post>> = _targetUserForPosts.flatMapLatest { uid ->
        if (uid == null) flowOf(PagingData.empty())
        else repository.getPostsBySenderStream(uid)
    }.cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val likedPostsFlow: Flow<PagingData<Post>> = _likedPostIds.flatMapLatest { ids ->
        if (ids.isEmpty()) flowOf(PagingData.empty())
        else repository.getLikedPostsStream(ids)
    }.cachedIn(viewModelScope)


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

    fun loadUserPosts(targetUserId: String) {
        _targetUserForPosts.value = targetUserId
        viewModelScope.launch {
            repository.syncUserPosts(targetUserId)
        }
    }

    fun loadLikedPosts(targetUserId: String) {
        viewModelScope.launch {
            val ids = repository.syncAndGetLikedPostIds(targetUserId)
            _likedPostIds.value = ids
        }
    }
}