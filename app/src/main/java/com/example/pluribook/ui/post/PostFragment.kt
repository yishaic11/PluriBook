package com.example.pluribook.ui.post

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.R
import com.example.pluribook.utils.ResourceState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class PostFragment : Fragment(R.layout.fragment_post) {

    private val viewModel: PostViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar_post)
        val scrollView = view.findViewById<ScrollView>(R.id.scroll_view_post)

        val buttonBack = view.findViewById<ImageButton>(R.id.button_back)
        val imagePostPhoto = view.findViewById<ImageView>(R.id.image_post_photo)
        val textPostDescription = view.findViewById<TextView>(R.id.text_post_description)
        val buttonPostOptions = view.findViewById<ImageButton>(R.id.button_post_options)

        val buttonLike = view.findViewById<ImageButton>(R.id.button_like)
        val buttonComment = view.findViewById<ImageButton>(R.id.button_comment)
        val textLikesCount = view.findViewById<TextView>(R.id.text_likes_count)
        val textViewAllComments = view.findViewById<TextView>(R.id.text_view_comments)

        val textSenderName = view.findViewById<TextView>(R.id.text_post_sender_name)
        val imageSenderProfile =
            view.findViewById<ShapeableImageView>(R.id.image_post_sender_profile)

        val postId = arguments?.getString("postId")
        if (postId != null) {
            viewModel.loadPost(postId)
        }

        buttonBack.setOnClickListener { findNavController().navigateUp() }

        viewModel.authorName.observe(viewLifecycleOwner) { name ->
            textSenderName.text = name
        }

        viewModel.authorPhotoUrl.observe(viewLifecycleOwner) { photoUrl ->
            if (photoUrl.isNotEmpty()) {
                Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.drawable.default_profile_photo)
                    .error(R.drawable.default_profile_photo)
                    .fit()
                    .centerCrop()
                    .into(imageSenderProfile)
            } else {
                imageSenderProfile.setImageResource(R.drawable.default_profile_photo)
            }
        }

        viewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    scrollView.visibility = View.GONE
                }

                is ResourceState.Success -> {
                    val post = state.data
                    textPostDescription.text = post.description

                    if (post.photoUrl.isNotEmpty()) {
                        progressBar.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE

                        Picasso.get()
                            .load(post.photoUrl)
                            .into(imagePostPhoto, object : com.squareup.picasso.Callback {
                                override fun onSuccess() {
                                    progressBar.visibility = View.GONE
                                    scrollView.visibility = View.VISIBLE
                                }

                                override fun onError(e: Exception?) {
                                    progressBar.visibility = View.GONE
                                    scrollView.visibility = View.VISIBLE
                                }
                            })
                    } else {
                        progressBar.visibility = View.GONE
                        scrollView.visibility = View.VISIBLE
                    }

                    val isLiked = post.likedBy.contains(viewModel.currentUserId)
                    if (isLiked) {
                        buttonLike.setImageResource(R.drawable.ic_heart_filled)
                        buttonLike.colorFilter = null
                    } else {
                        buttonLike.setImageResource(R.drawable.ic_heart_empty)
                        buttonLike.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.pluri_darkest)
                        )
                    }

                    val count = post.likedBy.size
                    textLikesCount.text =
                        resources.getQuantityString(R.plurals.likes_plural, count, count)
                }

                is ResourceState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        buttonLike.setOnClickListener {
            if (postId != null) viewModel.toggleLike(postId)
        }

        val openComments = View.OnClickListener {
            val bundle = Bundle().apply { putString("postId", postId) }
            findNavController().navigate(R.id.action_post_fragment_to_comment_fragment, bundle)
        }

        buttonComment.setOnClickListener(openComments)
        textViewAllComments.setOnClickListener(openComments)

        viewModel.isOwner.observe(viewLifecycleOwner) { isOwner ->
            buttonPostOptions.visibility = if (isOwner) View.VISIBLE else View.GONE
        }

        buttonPostOptions.setOnClickListener { showBottomSheetMenu(postId) }
    }

    private fun showBottomSheetMenu(postId: String?) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_post_options)

        val textEdit = bottomSheetDialog.findViewById<TextView>(R.id.text_edit_post)
        textEdit?.setOnClickListener {
            bottomSheetDialog.dismiss()
            Toast.makeText(requireContext(), "Edit coming soon", Toast.LENGTH_SHORT).show()
        }

        val textDelete = bottomSheetDialog.findViewById<TextView>(R.id.text_delete_post)
        textDelete?.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (postId != null) {
                viewModel.deletePost(postId)
                setFragmentResult("post_request", bundleOf("post_deleted" to true))
                findNavController().navigateUp()
            }
        }

        bottomSheetDialog.show()
    }
}