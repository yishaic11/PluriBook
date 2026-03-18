package com.example.pluribook.ui.post.create

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.R
import com.example.pluribook.data.model.Post
import com.example.pluribook.utils.ResourceState
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView
    private val createPostViewModel: CreatePostViewModel by viewModels()

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                displaySelectedImage(uri)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val uri = getBitmapUri(bitmap)
                selectedImageUri = uri
                displaySelectedImage(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutPostForm = view.findViewById<LinearLayout>(R.id.layout_post_form)
        val imageCard = view.findViewById<MaterialCardView>(R.id.material_card_view_post_image)
        imageView = view.findViewById(R.id.image_view_create_post)
        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        val layoutBookSearch = view.findViewById<TextInputLayout>(R.id.text_input_book_search)
        val editTextBookSearch = view.findViewById<TextInputEditText>(R.id.edit_text_book_search)
        val progressBarSearch = view.findViewById<ProgressBar>(R.id.progress_bar_search)

        val textSelectedTitle = view.findViewById<TextView>(R.id.text_selected_book_title)
        val textSelectedAuthor = view.findViewById<TextView>(R.id.text_selected_book_author)
        val textSelectedRating = view.findViewById<TextView>(R.id.text_selected_book_rating)
        val textSelectedSummary = view.findViewById<TextView>(R.id.text_selected_book_summary)
        val scrollSummary = view.findViewById<View>(R.id.scroll_selected_summary)

        layoutBookSearch.setEndIconOnClickListener {
            val query = editTextBookSearch.text.toString()
            createPostViewModel.searchBooks(query)
        }

        createPostViewModel.searchResultsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    progressBarSearch.visibility = View.VISIBLE
                    layoutBookSearch.isEndIconVisible = false
                }

                is ResourceState.Success -> {
                    progressBarSearch.visibility = View.GONE
                    layoutBookSearch.isEndIconVisible = true

                    val books = state.data
                    if (books.isEmpty()) {
                        Toast.makeText(requireContext(), "No books found.", Toast.LENGTH_SHORT)
                            .show()
                        return@observe
                    }

                    val bookTitles = books.map { it.volumeInfo.title ?: "Unknown" }.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Select your Book")
                        .setItems(bookTitles) { _, which ->
                            val selected = books[which]
                            createPostViewModel.selectedBook = selected

                            selectedImageUri = null

                            layoutPostForm.visibility = View.VISIBLE

                            textSelectedTitle.text = selected.volumeInfo.title ?: "Unknown Title"

                            val author = selected.volumeInfo.authors?.firstOrNull()
                            if (author.isNullOrEmpty()) textSelectedAuthor.visibility = View.GONE
                            else {
                                textSelectedAuthor.visibility = View.VISIBLE
                                textSelectedAuthor.text = author
                            }

                            val rating = selected.volumeInfo.averageRating
                            if (rating == null || rating == Post.DEFAULT_RATING) textSelectedRating.visibility =
                                View.GONE
                            else {
                                textSelectedRating.visibility = View.VISIBLE
                                textSelectedRating.text =
                                    getString(R.string.post_book_rating_format, rating.toString())

                            }

                            val summary = selected.volumeInfo.description
                            if (summary.isNullOrEmpty()) scrollSummary.visibility = View.GONE
                            else {
                                scrollSummary.visibility = View.VISIBLE
                                textSelectedSummary.text = summary
                            }

                            val thumbUrl = selected.volumeInfo.imageLinks?.thumbnail?.replace(
                                "http:",
                                "https:"
                            )
                            if (!thumbUrl.isNullOrEmpty()) {
                                createPostViewModel.defaultImageUrl = thumbUrl
                                Picasso.get().load(thumbUrl).into(imageView)
                            } else {
                                createPostViewModel.defaultImageUrl = null
                                imageView.setImageResource(R.drawable.create_post_image_upload_icon)
                            }
                        }.show()
                }

                is ResourceState.Error -> {
                    progressBarSearch.visibility = View.GONE
                    layoutBookSearch.isEndIconVisible = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        imageCard.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Photo")
                .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                    if (which == 0) cameraLauncher.launch(null) else pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }.show()
        }

        btnCancel.setOnClickListener { findNavController().popBackStack() }

        btnSave.setOnClickListener {
            val description = descriptionEditText.text.toString().trim()
            createPostViewModel.createPost(selectedImageUri, description)
        }

        createPostViewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    btnSave.isEnabled = false
                    btnSave.setText(R.string.create_post_save_button_loading_text)
                }

                is ResourceState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.create_post_success_toast_text,
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.home_fragment)
                }

                is ResourceState.Error -> {
                    btnSave.isEnabled = true
                    btnSave.setText(R.string.create_post_save_button_error_text)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displaySelectedImage(uri: Uri) {
        imageView.setImageURI(uri)
    }

    private fun getBitmapUri(bitmap: Bitmap): Uri {
        val tempFile =
            File(requireContext().cacheDir, "temp_post_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(tempFile)
    }
}