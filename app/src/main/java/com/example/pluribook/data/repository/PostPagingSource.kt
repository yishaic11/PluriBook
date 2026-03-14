package com.example.pluribook.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.pluribook.data.model.Post
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostPagingSource(
    private val firestore: FirebaseFirestore
) : PagingSource<DocumentSnapshot, Post>() {

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Post>): DocumentSnapshot? {
        return null
    }

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Post> {
        return try {
            val baseQuery = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())

            val currentPageQuery = params.key?.let { lastDoc ->
                baseQuery.startAfter(lastDoc)
            } ?: baseQuery

            val snapshot = currentPageQuery.get().await()

            val posts = snapshot.toObjects(Post::class.java)

            val lastVisibleDoc = if (snapshot.isEmpty) null else snapshot.documents.lastOrNull()

            LoadResult.Page(
                data = posts,
                prevKey = null,
                nextKey = if (snapshot.size() < params.loadSize) null else lastVisibleDoc
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}