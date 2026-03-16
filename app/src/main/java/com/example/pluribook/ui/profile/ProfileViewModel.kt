package com.example.pluribook.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.TAG
import com.example.pluribook.data.model.Post
import com.example.pluribook.data.model.User
import com.example.pluribook.data.repository.PostRepository
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val postDao = (application as PluribookApplication).database.postDao()
    private val userDao = (application as PluribookApplication).database.userDao()

    private val repository = PostRepository(
        postDao,
        userDao
    )
    public val userRepository = UserRepository(userDao)

    private val _profilePostsState = MutableLiveData<ResourceState<List<Post>>>()
    val profilePostsState: LiveData<ResourceState<List<Post>>> = _profilePostsState

    private val _userProfileState = MutableLiveData<ResourceState<User>>()
    val userProfileState: LiveData<ResourceState<User>> = _userProfileState

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
        _profilePostsState.value = ResourceState.Loading

        viewModelScope.launch {
            try {
                val posts = repository.getPostsBySender(targetUserId)

                _profilePostsState.value = ResourceState.Success(posts)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile posts", e)
                _profilePostsState.value = ResourceState.Error(e.message ?: "Failed to load posts")
            }
        }
    }

    fun loadLikedPosts(targetUserId: String) {
        _profilePostsState.value = ResourceState.Loading

        viewModelScope.launch {
            try {
                val likedPosts = repository.getLikedPosts(targetUserId)
                _profilePostsState.value = ResourceState.Success(likedPosts)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading liked posts", e)
                _profilePostsState.value =
                    ResourceState.Error(e.message ?: "Failed to load liked posts")
            }
        }
    }
}