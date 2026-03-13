package com.example.pluribook.ui.auth

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pluribook.PluribookApplication
import com.example.pluribook.data.repository.AuthRepository
import com.example.pluribook.utils.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.pluribook.TAG

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = (application as PluribookApplication).database.userDao()
    private val repository = AuthRepository(
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance(),
        FirebaseStorage.getInstance(),
        userDao
    )

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val result =
                    repository.getFirebaseAuth().signInWithEmailAndPassword(email, password).await()
                val user = result.user

                user?.uid?.let { repository.syncUserToLocalDatabase(it) }
                _authState.value = AuthState.Success(user)

            } catch (e: Exception) {
                Log.e(TAG, "Error in login")
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signup(email: String, password: String, username: String, imageUri: Uri?) {
        if (imageUri == null) {
            _authState.value = AuthState.Error("Please select a profile photo")
            return
        }

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            try {
                val result =
                    repository.getFirebaseAuth().createUserWithEmailAndPassword(email, password)
                        .await()
                val firebaseUser = result.user

                val success = firebaseUser?.let {
                    repository.registerUser(it.uid, email, username, imageUri)
                } ?: false

                if (success) {
                    _authState.value = AuthState.Success(firebaseUser)
                } else {
                    _authState.value = AuthState.Error("Failed to save user profile.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in signup")
                _authState.value = AuthState.Error(e.message ?: "Signup failed")
            }
        }
    }
}