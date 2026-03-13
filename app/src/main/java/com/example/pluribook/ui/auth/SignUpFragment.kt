package com.example.pluribook.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.R
import com.example.pluribook.utils.AuthState
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso

class SignUpFragment : Fragment(R.layout.fragment_signup) {
    private val viewModel: AuthViewModel by viewModels()

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

        val editTextUsername = view.findViewById<TextInputEditText>(R.id.edit_text_signup_username)
        val editTextEmail = view.findViewById<TextInputEditText>(R.id.edit_text_signup_email)
        val editTextPassword = view.findViewById<TextInputEditText>(R.id.edit_text_signup_password)
        val buttonSignup = view.findViewById<MaterialButton>(R.id.button_signup)
        val textViewGoToSignup = view.findViewById<TextView>(R.id.text_view_go_to_login)
        val buttonSelectPhoto =
            view.findViewById<ExtendedFloatingActionButton>(R.id.button_signup_select_photo)
        imageProfile = view.findViewById(R.id.image_view_signup_profile)

        buttonSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        buttonSignup.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signup(email, password, username, selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        textViewGoToSignup.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    buttonSignup.isEnabled = false
                    buttonSignup.setText(R.string.loading_creating_account)
                }

                is AuthState.Success -> {
                    buttonSignup.isEnabled = true
                    buttonSignup.setText(R.string.signup_button)
                    Toast.makeText(
                        requireContext(),
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                }

                is AuthState.Error -> {
                    buttonSignup.isEnabled = true
                    buttonSignup.setText(R.string.signup_button)
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