package com.example.pluribook.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.example.pluribook.data.model.Post
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso

class ProfilePostAdapter(
    private val onImageLoaded: () -> Unit,
    private val onPostClick: (Post) -> Unit
) : PagingDataAdapter<Post, ProfilePostAdapter.ProfileViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_grid, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val post = getItem(position)
        if (post != null) {
            holder.bind(post, onPostClick, onImageLoaded)
        }
    }

    class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.image_post_grid)

        fun bind(post: Post, onPostClick: (Post) -> Unit, onImageLoaded: () -> Unit) {
            if (post.photoUrl.isNotEmpty()) {
                Picasso.get()
                    .load(post.photoUrl)
                    .placeholder(R.drawable.default_profile_photo)
                    .fit()
                    .centerCrop()
                    .into(ivPhoto, object : Callback {
                        override fun onSuccess() {
                            onImageLoaded()
                        }

                        override fun onError(e: Exception?) {
                            onImageLoaded()
                        }
                    })
            } else {
                onImageLoaded()
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
            oldItem == newItem
    }
}