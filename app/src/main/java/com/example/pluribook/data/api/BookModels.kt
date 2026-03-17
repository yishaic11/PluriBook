package com.example.pluribook.data.api

import com.google.gson.annotations.SerializedName

data class BookSearchResponse(
    @SerializedName("items") val items: List<BookItem>?
)

data class BookItem(
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    @SerializedName("title") val title: String?,
    @SerializedName("authors") val authors: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("averageRating") val averageRating: Double?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnail: String?
)