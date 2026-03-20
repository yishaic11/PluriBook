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
import androidx.navigation.fragment.navArgs
import com.example.pluribook.R
import com.example.pluribook.data.model.Post
import com.example.pluribook.utils.ResourceState
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class PostFragment : Fragment(R.layout.fragment_post) {

    private val postViewModel: PostViewModel by viewModels()

    private val args: PostFragmentArgs by navArgs()

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

        val textBookTitle = view.findViewById<TextView>(R.id.text_book_title)
        val textBookAuthor = view.findViewById<TextView>(R.id.text_book_author)
        val textBookRating = view.findViewById<TextView>(R.id.text_book_rating)
        val textBookSummary = view.findViewById<TextView>(R.id.text_book_summary)
        val scrollSummary = view.findViewById<View>(R.id.scroll_book_summary)

        val postId = args.postId

        postViewModel.loadPost(postId)

        postViewModel.getCommentCount(postId).observe(viewLifecycleOwner) { count ->
            textViewAllComments.text =
                if (count > 0) "View all $count comments" else "Be the first to comment!"
        }

        buttonBack.setOnClickListener { findNavController().navigateUp() }

        postViewModel.senderName.observe(viewLifecycleOwner) { name ->
            textSenderName.text = name
        }

        postViewModel.senderPhotoUrl.observe(viewLifecycleOwner) { photoUrl ->
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

        postViewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    scrollView.alpha = 0.5f
                    buttonPostOptions.isEnabled = false
                }
                is ResourceState.Success -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    setFragmentResult("post_request", bundleOf("post_deleted" to true))
                    findNavController().navigateUp()
                }
                is ResourceState.Error -> {
                    progressBar.visibility = View.GONE
                    scrollView.alpha = 1.0f
                    buttonPostOptions.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        postViewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    if (postViewModel.deleteState.value !is ResourceState.Loading) {
                        progressBar.visibility = View.VISIBLE
                        scrollView.visibility = View.GONE
                    }
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

                    val isLiked = post.likedBy.contains(postViewModel.currentUserId)
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

                    textBookTitle.text = post.bookTitle

                    if (post.bookAuthor.isEmpty()) textBookAuthor.visibility = View.GONE
                    else {
                        textBookAuthor.visibility = View.VISIBLE
                        textBookAuthor.text =
                            getString(R.string.post_book_author_format, post.bookAuthor)
                    }

                    if (post.bookRating == Post.DEFAULT_RATING) textBookRating.visibility =
                        View.GONE
                    else {
                        textBookRating.visibility = View.VISIBLE
                        textBookRating.text =
                            getString(R.string.post_book_rating_format, post.bookRating.toString())
                    }

                    if (post.bookSummary.isEmpty()) scrollSummary.visibility = View.GONE
                    else {
                        scrollSummary.visibility = View.VISIBLE
                        textBookSummary.text = post.bookSummary
                    }


                    val navigateToProfile = View.OnClickListener {
                        if (post.senderId == postViewModel.currentUserId) {

                            val action =
                                PostFragmentDirections.actionPostFragmentToProfileFragment(post.senderId)
                            findNavController().navigate(action)
                        } else {

                            val action =
                                PostFragmentDirections.actionPostFragmentToOtherProfileFragment(post.senderId)
                            findNavController().navigate(action)
                        }
                    }

                    textSenderName.setOnClickListener(navigateToProfile)
                    imageSenderProfile.setOnClickListener(navigateToProfile)
                }

                is ResourceState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }

        buttonLike.setOnClickListener {
            postViewModel.toggleLike(postId)
        }

        val openComments = View.OnClickListener {
            val action = PostFragmentDirections.actionPostFragmentToCommentFragment(postId)
            findNavController().navigate(action)
        }

        buttonComment.setOnClickListener(openComments)
        textViewAllComments.setOnClickListener(openComments)

        postViewModel.isOwner.observe(viewLifecycleOwner) { isOwner ->
            buttonPostOptions.visibility = if (isOwner) View.VISIBLE else View.GONE
        }

        buttonPostOptions.setOnClickListener { showBottomSheetMenu(postId) }
    }

    private fun showBottomSheetMenu(postId: String) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_post_options)

        val textEdit = bottomSheetDialog.findViewById<TextView>(R.id.text_edit_post)
        textEdit?.setOnClickListener {
            bottomSheetDialog.dismiss()
            val action = PostFragmentDirections.actionPostFragmentToEditPostFragment(postId)
            findNavController().navigate(action)
        }

        val textDelete = bottomSheetDialog.findViewById<TextView>(R.id.text_delete_post)
        textDelete?.setOnClickListener {
            bottomSheetDialog.dismiss()
            postViewModel.deletePost(postId)
        }

        bottomSheetDialog.show()
    }
}