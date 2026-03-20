package com.example.pluribook.ui.profile.edit

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.R
import com.example.pluribook.utils.ResourceState
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val viewModel: EditProfileViewModel by viewModels()

    private var selectedImageUri: Uri? = null
    private var imageProfile: ShapeableImageView? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                imageProfile?.let { view ->
                    Picasso.get().load(it).fit().centerCrop().into(view)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageProfile = view.findViewById(R.id.image_view_edit_profile)
        val buttonSelectPhoto =
            view.findViewById<ExtendedFloatingActionButton>(R.id.button_edit_profile_select_photo)
        val editUsername = view.findViewById<TextInputEditText>(R.id.edit_text_edit_username)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_profile)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_profile)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar_edit_profile)

        viewModel.loadCurrentUser()

        viewModel.userProfile.observe(viewLifecycleOwner) { state ->
            if (state is ResourceState.Success) {
                val user = state.data
                editUsername.setText(user.username)
                if (user.photoUrl.isNotEmpty()) {
                    imageProfile?.let { view ->
                        Picasso.get().load(user.photoUrl).fit().centerCrop().into(view)
                    }
                }
            }
        }

        buttonSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnCancel.setOnClickListener {
            val action =
                EditProfileFragmentDirections.actionEditProfileFragmentToProfileFragment(null)
            findNavController().navigate(action)
        }

        btnSave.setOnClickListener {
            val newName = editUsername.text.toString().trim()
            viewModel.updateProfile(newName, selectedImageUri)
        }

        viewModel.updateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    btnSave.isEnabled = false
                    progressBar.visibility = View.VISIBLE
                }

                is ResourceState.Success -> {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show()

                    val action =
                        EditProfileFragmentDirections.actionEditProfileFragmentToProfileFragment(
                            null
                        )
                    findNavController().navigate(action)
                }

                is ResourceState.Error -> {
                    btnSave.isEnabled = true
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageProfile = null
    }
}