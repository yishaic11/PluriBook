package com.example.pluribook.ui.profile

import android.app.Application
import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PluribookApplication
    private val postDao = app.database.postDao()
    private val userDao = app.database.userDao()
    private val repository = PostRepository(postDao, userDao)
    val userRepository = UserRepository(app.database.userDao())

    private val _userProfileState = MutableLiveData<ResourceState<User>>()
    val userProfileState: LiveData<ResourceState<User>> = _userProfileState

    private val _likedPostIds = MutableLiveData<List<String>>()
    val likedPostIds: LiveData<List<String>> = _likedPostIds

    private var profileSyncJob: Job? = null
    private var profileListener: ListenerRegistration? = null

    fun getPostsFlow(userId: String): Flow<PagingData<Post>> =
        repository.getPostsBySenderStream(userId).cachedIn(viewModelScope)

    fun getLikedPostsFlow(postIds: List<String>): Flow<PagingData<Post>> =
        repository.getLikedPostsStream(postIds).cachedIn(viewModelScope)

    fun loadUserProfile(uid: String) {
        _userProfileState.value = ResourceState.Loading
        profileListener?.remove()
        profileListener = userRepository.startRealtimeUserSync(uid)
        profileSyncJob?.cancel()
        profileSyncJob = viewModelScope.launch {
            userRepository.fetchAndCacheUser(uid)
            userRepository.getUserStream(uid).collect { user ->
                if (user != null) _userProfileState.value = ResourceState.Success(user)
            }
        }
    }

    fun refreshUserPosts(targetUserId: String) {
        viewModelScope.launch { repository.syncUserPosts(targetUserId) }
    }

    fun refreshLikedPosts(targetUserId: String) {
        viewModelScope.launch {
            val ids = repository.syncAndGetLikedPostIds(targetUserId)
            _likedPostIds.value = ids
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }
}