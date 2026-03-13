package com.example.pluribook.ui.post

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.pluribook.R
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class CreatePostFragment : Fragment(R.layout.fragment_create_post) {
    private var capturedImageBitmap: Bitmap? = null
    private lateinit var imageView: ImageView

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedImageBitmap = bitmap
            imageView.setImageBitmap(bitmap)

            imageView.setPadding(0, 0, 0, 0)
            imageView.imageTintList = null
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            Toast.makeText(requireContext(), "Failed to capture photo", Toast.LENGTH_SHORT).show()
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
            cameraLauncher.launch(null)
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            val description = descriptionEditText.text.toString().trim()

            if (capturedImageBitmap == null || description.isBlank()) {
                Toast.makeText(
                    requireContext(), "Please add a photo and a description", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // TODO: Upload capturedImageBitmap to Firebase Storage
            // TODO: Save Post data (Image URL + Description) to Room and Firestore
            Toast.makeText(requireContext(), "Saving new post!", Toast.LENGTH_SHORT).show()
        }
    }
}
