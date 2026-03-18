package com.example.pluribook.data.api

data class BookSearchResponse(
    val items: List<BookItem>?
)

data class BookItem(
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val description: String?,
    val averageRating: Double?,
    val imageLinks: ImageLinks?
)

data class ImageLinks(
    val thumbnail: String?
)