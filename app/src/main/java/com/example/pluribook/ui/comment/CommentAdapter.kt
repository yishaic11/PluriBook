package com.example.pluribook.ui.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.example.pluribook.data.model.Comment
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class CommentAdapter(
    private val currentUserId: String,
    private val onOptionsClicked: (Comment) -> Unit
) : PagingDataAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)

        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)

        if (comment != null) {
            holder.bind(comment, currentUserId, onOptionsClicked)
        }
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageProfile: ShapeableImageView = view.findViewById(R.id.image_comment_profile)
        private val textUsername: TextView = view.findViewById(R.id.text_view_comment_username)
        private val textBody: TextView = view.findViewById(R.id.text_view_comment_body)
        private val btnOptions: ImageButton = view.findViewById(R.id.image_button_comment_options)

        fun bind(comment: Comment, currentUserId: String, onOptionsClicked: (Comment) -> Unit) {
            textUsername.text = comment.senderName
            textBody.text = comment.text

            if (comment.senderPhotoUrl.isNotEmpty()) {
                Picasso.get().load(comment.senderPhotoUrl)
                    .placeholder(R.drawable.default_profile_photo).into(imageProfile)
            } else {
                imageProfile.setImageResource(R.drawable.default_profile_photo)
            }

            if (comment.senderId == currentUserId) {
                btnOptions.visibility = View.VISIBLE
                btnOptions.setOnClickListener { onOptionsClicked(comment) }
            } else {
                btnOptions.visibility = View.GONE
            }
        }
    }

    class CommentDiff : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(old: Comment, new: Comment) = old.id == new.id
        override fun areContentsTheSame(old: Comment, new: Comment) = old == new
    }
}