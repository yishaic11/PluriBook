package com.example.pluribook.ui.profile.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.model.User
import com.example.pluribook.data.repository.UserRepository
import com.example.pluribook.utils.ResourceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository((application as PluribookApplication).database.userDao())

    private val currentUserId = userRepository.getCurrentUserId()

    private val _userProfile = MutableLiveData<ResourceState<User>>()
    val userProfile: LiveData<ResourceState<User>> = _userProfile

    private val _updateState = MutableLiveData<ResourceState<Unit>>()
    val updateState: LiveData<ResourceState<Unit>> = _updateState

    var loadedUser: User? = null

    fun loadCurrentUser() {
        val uid = currentUserId ?: return
        _userProfile.value = ResourceState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            userRepository.fetchAndCacheUser(uid)
            val user = userRepository.getUserById(uid)

            if (user != null) {
                loadedUser = user
                _userProfile.postValue(ResourceState.Success(user))
            } else {
                _userProfile.postValue(ResourceState.Error("Failed to load profile"))
            }
        }
    }

    fun updateProfile(newUsername: String, newImageUri: Uri?) {
        val uid = currentUserId ?: return
        val oldUser = loadedUser ?: return

        if (newUsername.isBlank()) {
            _updateState.value = ResourceState.Error("Username cannot be empty")
            return
        }

        _updateState.value = ResourceState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val success = userRepository.updateUserProfile(uid, newUsername, newImageUri, oldUser.photoUrl)
            if (success) _updateState.postValue(ResourceState.Success(Unit))
            else _updateState.postValue(ResourceState.Error("Failed to update profile"))
        }
    }
}