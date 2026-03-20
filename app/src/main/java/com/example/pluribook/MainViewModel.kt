package com.example.pluribook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pluribook.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository((application as PluribookApplication).database.userDao())

    fun syncCurrentUser(uid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.fetchAndCacheUser(uid)
        }
    }
}