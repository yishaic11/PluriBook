package com.example.pluribook.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pluribook.data.repository.AuthRepository
import com.example.pluribook.utils.AuthState
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        repository.getFirebaseAuth().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    viewModelScope.launch {
                        user?.uid?.let { repository.syncUserToLocalDatabase(it) }
                        _authState.value = AuthState.Success(user)
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun signup(email: String, password: String, username: String, imageUri: Uri?) {
        if (imageUri == null) {
            _authState.value = AuthState.Error("Please select a profile photo")
            return
        }

        _authState.value = AuthState.Loading

        repository.getFirebaseAuth().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    viewModelScope.launch {
                        val success = firebaseUser?.let {
                            repository.registerUser(it.uid, email, username, imageUri)
                        } ?: false

                        if (success) {
                            _authState.value = AuthState.Success(firebaseUser)
                        } else {
                            _authState.value = AuthState.Error("Failed to save user profile.")
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}