package com.example.pluribook.utils

sealed class ResourceState<out T> {
    data class Success<out T>(val data: T) : ResourceState<T>()
    data class Error(val message: String) : ResourceState<Nothing>()
    data object Loading : ResourceState<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading
}