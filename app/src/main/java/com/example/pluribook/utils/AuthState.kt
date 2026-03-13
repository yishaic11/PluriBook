package com.example.pluribook.utils

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
}