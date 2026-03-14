package com.example.pluribook.ui.post

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.R
import com.example.pluribook.utils.ResourceState
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {

    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView

    private val createPostViewModel: CreatePostViewModel by viewModels()
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            displaySelectedImage(uri)
        } else {
            Toast.makeText(requireContext(), R.string.create_post_no_media_selected_toast_text, Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = getBitmapUri(bitmap)
            selectedImageUri = uri
            displaySelectedImage(uri)
        } else {
            Toast.makeText(requireContext(), R.string.create_post_camera_error_toast_text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageCard = view.findViewById<MaterialCardView>(R.id.material_card_view_post_image)
        imageView = view.findViewById(R.id.image_view_create_post)

        val descriptionEditText = view.findViewById<TextInputEditText>(R.id.edit_text_description)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

        imageCard.setOnClickListener {
            showImageSourceDialog()
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            val description = descriptionEditText.text.toString().trim()

            if (selectedImageUri == null || description.isBlank()) {
                Toast.makeText(
                    requireContext(), R.string.create_post_missing_input_toast_text, Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.setText(R.string.create_post_save_button_uploading_text)

            createPostViewModel.createPost(selectedImageUri, description)
        }

        createPostViewModel.postState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    btnSave.isEnabled = false
                    btnSave.setText(R.string.create_post_save_button_loading_text)
                }
                is ResourceState.Success -> {
                    btnSave.isEnabled = true
                    Toast.makeText(requireContext(), R.string.create_post_success_toast_text, Toast.LENGTH_SHORT).show()
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

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add a Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> pickMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            .show()
    }

    private fun displaySelectedImage(uri: Uri) {
        imageView.setImageURI(uri)
        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
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