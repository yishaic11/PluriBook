package com.example.pluribook.ui.post

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.repository.PostsRepository
import com.example.pluribook.utils.ResourceState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import com.example.pluribook.TAG

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val postsDao = (application as PluribookApplication).database.postsDao()

    private val repository = PostsRepository(
        FirebaseFirestore.getInstance(),
        FirebaseStorage.getInstance(),
        FirebaseAuth.getInstance(),
        postsDao
    )

    private val _postState = MutableLiveData<ResourceState<Unit>>()
    val postState: LiveData<ResourceState<Unit>> = _postState

    fun createPost(imageUri: Uri?, description: String) {
        if (imageUri == null) {
            _postState.value = ResourceState.Error("Please select a photo.")
            return
        }
        if (description.isBlank()) {
            _postState.value = ResourceState.Error("Please enter a description.")
            return
        }

        _postState.value = ResourceState.Loading

        viewModelScope.launch {
            try {
                val success = repository.createPost(imageUri, description)

                if (success) {
                    _postState.value = ResourceState.Success(Unit)
                } else {
                    _postState.value = ResourceState.Error("Failed to publish post.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in createPost", e)
                _postState.value = ResourceState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}