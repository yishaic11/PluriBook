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
import com.example.pluribook.data.local.CommentWithSender
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class CommentAdapter(
    private val currentUserId: String,
    private val onOptionsClicked: (CommentWithSender) -> Unit
) : PagingDataAdapter<CommentWithSender, CommentAdapter.CommentViewHolder>(CommentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item, currentUserId, onOptionsClicked)
        }
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageProfile: ShapeableImageView = view.findViewById(R.id.image_comment_profile)
        private val textUsername: TextView = view.findViewById(R.id.text_view_comment_username)
        private val textBody: TextView = view.findViewById(R.id.text_view_comment_body)
        private val btnOptions: ImageButton = view.findViewById(R.id.image_button_comment_options)

        fun bind(
            item: CommentWithSender,
            currentUserId: String,
            onOptionsClicked: (CommentWithSender) -> Unit
        ) {
            textUsername.text = item.senderName ?: "Unknown User"
            textBody.text = item.comment.text

            val photoUrl = item.senderPhotoUrl
            if (!photoUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.default_profile_photo)
                    .error(R.drawable.default_profile_photo)
                    .fit()
                    .centerCrop()
                    .into(imageProfile)
            } else {
                imageProfile.setImageResource(R.drawable.default_profile_photo)
            }

            if (item.comment.senderId == currentUserId) {
                btnOptions.visibility = View.VISIBLE
                btnOptions.setOnClickListener { onOptionsClicked(item) }
            } else {
                btnOptions.visibility = View.GONE
            }
        }
    }

    class CommentDiff : DiffUtil.ItemCallback<CommentWithSender>() {
        override fun areItemsTheSame(old: CommentWithSender, new: CommentWithSender) =
            old.comment.id == new.comment.id

        override fun areContentsTheSame(old: CommentWithSender, new: CommentWithSender) = old == new
    }
}