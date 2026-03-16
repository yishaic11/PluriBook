package com.example.pluribook.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.example.pluribook.data.model.Post
import com.squareup.picasso.Picasso

class ProfilePostAdapter(
    private var posts: List<Post>,
    private val onPostClicked: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagePostGrid: ImageView = view.findViewById(R.id.image_post_grid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_grid, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val post = posts[position]

        if (post.photoUrl.isNotEmpty()) {
            Picasso.get()
                .load(post.photoUrl)
                .placeholder(R.drawable.default_profile_photo)
                .fit()
                .centerCrop()
                .into(holder.imagePostGrid)
        }

        holder.itemView.setOnClickListener {
            onPostClicked(post)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}